package io.solit.deb.changes;

import io.solit.deb.Version;
import org.commonmark.node.BulletList;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;

import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.solit.deb.MarkdownUtils.extractText;

/**
 * @author yaga
 * @since 14.03.18
 */
public class KeepChangelogParser {
    private static final Pattern CHANGE_HEADING = Pattern.compile(
            // [ version ] - 2000-01-01[T00:00:00][+01:00]
            "\\s*\\[([\\w.:+~-]+)]\\s*-+\\s*(\\d{4}-\\d{2}-\\d{2})(?:T(\\d{2}:\\d{2}:\\d{2}))?([A-Z]*(?:[+-][\\d:]+)?)?\\s*(\\[YANKED])?"
    );
    private static final Pattern COLON_PAIR = Pattern.compile("^([^:]+):(.+)$");
    private static final String RELEASE_HEADER = "release";
    private static final String SECURITY_HEADER = "security";
    private static final Pattern URGENCY_PARSER = Pattern.compile(
            "(low|medium|high|emergency|critical)(?:\\s*\\((.*)\\))?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MAINTAINER_PARSER = Pattern.compile(
            "(.*)[\\s<]+([a-zA-Z0-9_.+-]+@[a-zA-Z0-9.-]+)>?"
    );
    public static final String UNRELEASED = "[Unreleased]";
    private final String packageName;
    private final String defaultMaintainer;
    private final String defaultMaintainerEmail;
    private String defaultDistribution;
    private Version unreleasedVersion;

    public KeepChangelogParser(String packageName, String defaultMaintainer, String defaultMaintainerEmail) {
        this.packageName = Objects.requireNonNull(packageName, "Package name should be specified");
        this.defaultMaintainer = Objects.requireNonNull(defaultMaintainer, "Default maintainer should not be null");
        this.defaultMaintainerEmail = Objects.requireNonNull(defaultMaintainerEmail, "Defualt maintainer email should not be null");
    }

    public void setDefaultDistribution(String defaultDistribution) {
        this.defaultDistribution = defaultDistribution;
    }

    public void setUnreleasedVersion(Version unreleasedVersion) {
        this.unreleasedVersion = unreleasedVersion;
    }

    public Changelog parse(Reader changelogReader) throws IOException {
        Node document = Parser.builder() .build().parseReader(changelogReader);
        List<ChangeSet> changeSets = new ArrayList<>();
        ChangeAccumulator accumulator = null;
        for (Node n = document.getFirstChild(); n != null;) {
            if (n instanceof Heading) {
                Heading h = (Heading) n;
                if (h.getLevel() <= 2) {
                    if (accumulator != null) {
                        ChangeSet set = accumulator.createSet();
                        if (set != null)
                            changeSets.add(set);
                    }
                    if (h.getLevel() == 2)
                        accumulator = parseChangeSet(h);
                    else
                        accumulator = null;
                    n = n.getNext();
                    continue;
                }
            }
            if (accumulator != null)
                n = accumulator.accept(n);
            else
                n = n.getNext();
        }
        if (accumulator != null) {
            ChangeSet set = accumulator.createSet();
            if (set != null)
                changeSets.add(set);
        }
        return changeSets.isEmpty() ? null : new Changelog(changeSets);
    }

    private ChangeAccumulator parseChangeSet(Heading heading) {
        String headingText = extractText(heading);
        Matcher matcher = CHANGE_HEADING.matcher(headingText);
        if (matcher.matches()) {
            Version version = Version.parseVersion(matcher.group(1));
            ZonedDateTime dateTime;
            dateTime = getZonedDateTime(matcher.group(2), matcher.group(3), matcher.group(4));
            return new ChangeAccumulator(version, dateTime, matcher.group(5) != null);
        } else if (unreleasedVersion != null && UNRELEASED.equalsIgnoreCase(headingText)) {
            return new ChangeAccumulator(unreleasedVersion, ZonedDateTime.now(), false);
        } else
            return null;
    }

    private ZonedDateTime getZonedDateTime(String date, String time, String zone) {
        return ZonedDateTime.of(
                LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE),
                time == null ? LocalTime.MIDNIGHT : LocalTime.parse(time, DateTimeFormatter.ISO_LOCAL_TIME),
                zone == null || zone.isEmpty() ? ZoneId.of("+0000") : ZoneId.of(zone)
        );
    }

    private class ChangeAccumulator {
        private final Version version;
        private final ZonedDateTime date;
        private final boolean yanked;
        private String packageName = KeepChangelogParser.this.packageName;
        private String maintainer = defaultMaintainer;
        private String maintainerEmail = defaultMaintainerEmail;
        private String distribution = defaultDistribution;
        private String urgencyComment = null;
        private ChangeSet.Urgency urgency = null;
        private List<Node> nodes = new ArrayList<>();

        private ChangeAccumulator(Version version, ZonedDateTime date, boolean yanked) {
            this.version = Objects.requireNonNull(version, "Version should not be null");
            this.date = Objects.requireNonNull(date, "Date should not be null");
            this.yanked = yanked;
        }

        public ChangeSet createSet() {
            Changes changes;
            if (!nodes.isEmpty())
                changes = new MarkdownChanges(nodes);
            else if (yanked)
                changes = StringChanges.YANKED;
            else
                return null;
            ChangeSet set = new ChangeSet(packageName, version, maintainer, maintainerEmail, changes);
            set.setDate(date);
            if (distribution != null)
                set.setDistribution(distribution);
            if (urgencyComment != null)
                set.setUrgencyCommentary(urgencyComment);
            if (urgency != null)
                set.setUrgency(urgency);
            return set;
        }

        public Node accept(Node node) {
            if (node instanceof Heading) {
                Heading h = (Heading) node;
                if (h.getLevel() <= 2)
                    throw new IllegalArgumentException("Unexpected heading");
                if (urgency == null && h.getLevel() == 3 && SECURITY_HEADER.equalsIgnoreCase(extractText(h).trim()))
                    urgency = ChangeSet.Urgency.high;
                else  if (h.getLevel() == 3 && RELEASE_HEADER.equalsIgnoreCase(extractText(h).trim()))
                    return readReleaseDetails(h);
            }
            nodes.add(node);
            return node.getNext();
        }

        private Node readReleaseDetails(Heading heading) {
            List<Node> details = new ArrayList<>(Collections.singleton(heading));
            for (Node n = heading.getNext(); n != null; n = n.getNext()) {
                if (n instanceof Heading) {
                    if (details.size() > 1)
                        this.nodes.addAll(details);
                    return n;
                }
                if (n instanceof BulletList) {
                    boolean skip = true;
                    for (Node item = n.getFirstChild(); item != null; item = item.getNext()) {
                        if (item.getFirstChild() instanceof Paragraph) {
                            Matcher m = COLON_PAIR.matcher(extractText(item.getFirstChild()));
                            if (m.matches()) {
                                switch (m.group(1).trim().toLowerCase()) {
                                    case "distribution":
                                        distribution = m.group(2).trim();
                                        break;
                                    case "urgency":
                                        skip &= parseUrgency(m.group(2).trim());
                                        break;
                                    case "maintainer":
                                        skip &= parseMaintainer(m.group(2).trim());
                                        break;
                                    default:
                                        skip = false;
                                }
                            } else
                                skip = false;
                        }
                    }
                    if (skip)
                        continue;
                }
                details.add(n);
            }
            if (details.size() > 1)
                this.nodes.addAll(details);
            return null;
        }

        private boolean parseMaintainer(String maintainer) {
            Matcher matcher = MAINTAINER_PARSER.matcher(maintainer);
            if (matcher.matches()) {
                this.maintainer = matcher.group(1);
                this.maintainerEmail = matcher.group(2);
                return true;
            }
            return false;
        }

        private boolean parseUrgency(String urgency) {
            Matcher matcher = URGENCY_PARSER.matcher(urgency);
            if (matcher.matches()) {
                this.urgency = ChangeSet.Urgency.valueOf(matcher.group(1));
                if (matcher.group(2) != null && !matcher.group(2).isEmpty())
                    this.urgencyComment = matcher.group(2);
                return true;
            }
            return false;
        }
    }

}
