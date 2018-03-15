package io.solit.deb.changes;

import io.solit.deb.Version;

import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.time.temporal.ChronoField.*;

/**
 * @author yaga
 * @since 13.03.18
 */
public class ChangeSet {
    private static final DateTimeFormatter RFC_5322_DATE_TIME;
    private final String packageName;
    private final Version version;
    private final String maintainer;
    private final String maintainerEmail;
    private String distribution = "stable";
    private Urgency urgency = Urgency.medium;
    private String urgencyCommentary;
    private ZonedDateTime date;
    private final Changes changes;

    public ChangeSet(String packageName, Version version, String maintainer, String maintainerEmail, Changes changes) {
        this.packageName = Objects.requireNonNull(packageName, "");
        if (packageName.contains("\n"))
            throw new IllegalArgumentException("Package name should not contain line feed");
        this.version = Objects.requireNonNull(version);
        this.maintainer = Objects.requireNonNull(maintainer);
        if (maintainer.contains("\n"))
            throw new IllegalArgumentException("Maintainer should not contain line feed");
        this.maintainerEmail = maintainerEmail;
        if (maintainerEmail.contains("\n"))
            throw new IllegalArgumentException("Maintainer email should not contain line feed");
        this.date = ZonedDateTime.now();
        this.changes = Objects.requireNonNull(changes, "Changes must not be null");
    }

    public ChangeSet setDate(ZonedDateTime date) {
        this.date = Objects.requireNonNull(date, "Date should not be null");
        return this;
    }

    public ChangeSet setDistribution(String distribution) {
        Objects.requireNonNull(distribution, "Distribution should not be null");
        if (distribution.contains(";") || distribution.contains("\n"))
            throw new IllegalArgumentException("Distribution should not contain commas and line breaks");
        this.distribution = distribution;
        return this;
    }

    public ChangeSet setUrgency(Urgency urgency) {
        this.urgency = Objects.requireNonNull(urgency, "Urgency should not be null");
        return this;
    }

    public ChangeSet setUrgencyCommentary(String commentary) {
        if (commentary != null && (commentary.contains(",") || commentary.contains("\n")))
            throw new IllegalArgumentException("Commentary should not contain commas and line breaks");
        this.urgencyCommentary = commentary;
        return this;
    }

    public void write(Writer writer) throws IOException {
        writer.write(packageName);
        writer.write(" (");
        writer.write(version.getValidatedString());
        writer.write(")");
            writer.write(" ");
            writer.write(distribution);
        writer.write("; urgency=");
        writer.write(urgency.toString());
        if (urgencyCommentary != null) {
            writer.write(" (");
            writer.write(urgencyCommentary);
            writer.write(')');
        }
        writer.write('\n');
        for (String change: changes) {
            writer.write("  ");
            writer.write(change);
            writer.write('\n');
        }
        writer.write(" -- ");
        writer.write(maintainer);
        writer.write(" <");
        writer.write(maintainerEmail);
        writer.write(">  ");
        writer.write(RFC_5322_DATE_TIME.format(date));
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public String getPackageName() {
        return packageName;
    }

    public Version getVersion() {
        return version;
    }

    public String getMaintainer() {
        return maintainer;
    }

    public String getMaintainerEmail() {
        return maintainerEmail;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getUrgencyCommentary() {
        return urgencyCommentary;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public Changes getChanges() {
        return changes;
    }

    static {
        Map<Long, String> dow = new HashMap<>();
        dow.put(1L, "Mon");
        dow.put(2L, "Tue");
        dow.put(3L, "Wed");
        dow.put(4L, "Thu");
        dow.put(5L, "Fri");
        dow.put(6L, "Sat");
        dow.put(7L, "Sun");
        Map<Long, String> moy = new HashMap<>();
        moy.put(1L, "Jan");
        moy.put(2L, "Feb");
        moy.put(3L, "Mar");
        moy.put(4L, "Apr");
        moy.put(5L, "May");
        moy.put(6L, "Jun");
        moy.put(7L, "Jul");
        moy.put(8L, "Aug");
        moy.put(9L, "Sep");
        moy.put(10L, "Oct");
        moy.put(11L, "Nov");
        moy.put(12L, "Dec");
        RFC_5322_DATE_TIME = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .parseLenient()
                .optionalStart()
                .appendText(DAY_OF_WEEK, dow)
                .appendLiteral(", ")
                .optionalEnd()
                .appendValue(DAY_OF_MONTH, 1, 2, SignStyle.NOT_NEGATIVE)
                .appendLiteral(' ')
                .appendText(MONTH_OF_YEAR, moy)
                .appendLiteral(' ')
                .appendValue(YEAR, 4)  // 2 digit year not handled
                .appendLiteral(' ')
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalEnd()
                .appendLiteral(' ')
                .appendOffset("+HHMM", "+0000")  // should handle UT/Z/EST/EDT/CST/CDT/MST/MDT/PST/MDT
                .toFormatter();
    }

    public enum Urgency {
        low, medium, high, emergency, critical
    }
}
