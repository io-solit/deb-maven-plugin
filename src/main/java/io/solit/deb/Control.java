package io.solit.deb;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yaga
 * @since 19.01.18
 */
public class Control {
    private final static String PACKAGE_HEADER = "Package", VERSION_HEADER = "Version", SECTION_HEADER = "Section",
            PRIORITY_HEADER = "Priority", ARCHITECTURE_HEADER = "Architecture", MAINTAINER_HEADER = "Maintainer",
            DESCRIPTION_HEADER = "Description", SOURCE_HEADER = "Source", ESSENTIAL_HEADER = "Essential",
            INSTALLED_SIZE_HEADER = "Installed-Size", HOMEPAGE_HEADER = "Homepage", BUILT_USING_HEADER = "Build-Using",
            DEPENDS_HEADER = "Depends", PRE_DEPENDS_HEADER = "Pre-Depends", RECOMMENDS_HEADER = "Recommends",
            SUGGESTS_HEADER = "Suggests", ENHANCES_HEADER = "Enhances", BREAKS_HEADER = "Breaks",
            CONFLICTS_HEADER = "Conflicts", PROVIDES_HEADER = "Provides";
    private final static Pattern PACKAGE_PATTERN = Pattern.compile("^[a-z0-9][a-z0-9+.-]+$");
    private final static Pattern PACKAGE_REFERENCE = Pattern.compile("^[a-z0-9][a-z0-9+.-]+(?:\\s*\\(([^)]+)\\))?$");
    private static final List<String> VERSION_RELATIONS = Collections.unmodifiableList(Arrays.asList("<<", "<=", "=", ">=", ">>"));

    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Set<String>> relationshipHeaders = new HashMap<>();
    private final Version version;
    private final String synopsis;
    private String description;

    public Control(String packageName, Version version, String maintainer, String architecture, String synopsis) {
        if (!PACKAGE_PATTERN.matcher(Objects.requireNonNull(packageName, "Package name should not be null")).matches())
            throw new IllegalArgumentException("Package name should match '" + PACKAGE_PATTERN.pattern() + "', was '" + packageName + "'");
        if (Objects.requireNonNull(architecture, "Architecture should not be null").trim().isEmpty())
            throw new IllegalArgumentException("Architecture should not be blank");
        if (Objects.requireNonNull(maintainer, "Maintainer should not be null").trim().isEmpty())
            throw new IllegalArgumentException("Maintainer should not be blank");
        if (Objects.requireNonNull(synopsis, "Synopsis should not be null").trim().isEmpty())
            throw new IllegalArgumentException("Synopsis should not be blank");
        headers.put(PACKAGE_HEADER, packageName);
        this.version = Objects.requireNonNull(version, "Version should not be null");
        headers.put(VERSION_HEADER, version.getValidatedString());
        headers.put(MAINTAINER_HEADER, maintainer.trim());
        headers.put(ARCHITECTURE_HEADER, architecture.trim());
        this.synopsis = Objects.requireNonNull(synopsis, "Synopsis should not be null");
    }

    public String getPackageName() {
        return headers.get(PACKAGE_HEADER);
    }

    public Version getVersion() {
        return version;
    }

    public String getMaintainer() {
        return headers.get(MAINTAINER_HEADER);
    }

    public String getArchitecture() {
        return headers.get(ARCHITECTURE_HEADER);
    }

    public String getSource() {
        return headers.get(SOURCE_HEADER);
    }

    public void setSource(String source) {
        if (source == null || source.isEmpty())
            headers.remove(SOURCE_HEADER);
        else if (PACKAGE_PATTERN.matcher(source).matches())
            headers.put(SOURCE_HEADER, source);
        else
            throw new IllegalArgumentException("Source package name should match " + PACKAGE_PATTERN.pattern());
    }

    public String getSection() {
        return headers.get(SECTION_HEADER);
    }

    public void setSection(String section) {
        if (section == null || section.isEmpty())
            headers.remove(SECTION_HEADER);
        else
            headers.put(SECTION_HEADER, section);
    }

    public PackagePriority getPriority() {
        return Optional.ofNullable(headers.get(PRIORITY_HEADER)).map(PackagePriority::valueOf).orElse(null);
    }

    public void setPriority(PackagePriority priority) {
        if (priority == null)
            headers.remove(PRIORITY_HEADER);
        else
            headers.put(PRIORITY_HEADER, priority.name());
    }

    public Boolean getEssential() {
        return Optional.ofNullable(headers.get(ESSENTIAL_HEADER)).map(Boolean::valueOf).orElse(null);
    }

    public void setEssential(Boolean essential) {
        if (essential == null)
            headers.remove(ESSENTIAL_HEADER);
        else
            headers.put(ESSENTIAL_HEADER, essential ? "yes" : "no");
    }

    public Long getInstalledSize() {
        return Optional.ofNullable(headers.get(INSTALLED_SIZE_HEADER)).map(Long::valueOf).orElse(null);
    }

    public void setInstalledSize(Long installedSizeInBytes) {
        if (installedSizeInBytes == null)
            headers.remove(INSTALLED_SIZE_HEADER);
        else
            headers.put(INSTALLED_SIZE_HEADER, Long.toString(installedSizeInBytes / 1024));
    }

    public String getHomepage() {
        return headers.get(HOMEPAGE_HEADER);
    }

    public void setHomepage(String homepage) {
        if (homepage == null || homepage.isEmpty())
            headers.remove(HOMEPAGE_HEADER);
        else
            headers.put(HOMEPAGE_HEADER, homepage);
    }

    public String getBuildUsing() {
        return headers.get(BUILT_USING_HEADER);
    }

    public void setBuildUsing(String buildTool) {
        if (buildTool == null || buildTool.isEmpty())
            headers.remove(BUILT_USING_HEADER);
        else
            headers.put(BUILT_USING_HEADER, buildTool);
    }

    private void checkPackageReference(String packageReference) {
        Objects.requireNonNull(packageReference, "Package reference should not be null");
        for (String ref: packageReference.split("\\|")) {
            Matcher matcher = PACKAGE_REFERENCE.matcher(ref.trim());
            if (!matcher.matches())
                throw new IllegalArgumentException("'" + packageReference + " is not a valid package reference");
            if (matcher.group(1) == null)
                continue;
            String versionClause = matcher.group(1).trim();
            String version = null;
            for (String rel : VERSION_RELATIONS)
                if (versionClause.startsWith(rel)) {
                    version = versionClause.substring(rel.length()).trim();
                    break;
                }
            if (version == null)
                throw new IllegalArgumentException("'" + packageReference + "' is not a valid package reference");
            else
                Version.parseVersion(version);
        }
    }

    public void addDepends(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(DEPENDS_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getPreDepends() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(PRE_DEPENDS_HEADER, s -> new HashSet<>()));
    }

    public void addPreDepends(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(PRE_DEPENDS_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getRecommends() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(RECOMMENDS_HEADER, s -> new HashSet<>()));
    }

    public void addRecommends(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(RECOMMENDS_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getSuggests() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(SUGGESTS_HEADER, s -> new HashSet<>()));
    }

    public void addSuggests(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(SUGGESTS_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getEnhances() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(ENHANCES_HEADER, s -> new HashSet<>()));
    }

    public void addEnhances(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(ENHANCES_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getBreaks() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(BREAKS_HEADER, s -> new HashSet<>()));
    }

    public void addBreaks(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(BREAKS_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getConflicts() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(CONFLICTS_HEADER, s -> new HashSet<>()));
    }

    public void addConflicts(String packageReference) {
        checkPackageReference(packageReference);
        relationshipHeaders.computeIfAbsent(CONFLICTS_HEADER, s -> new HashSet<>()).add(packageReference);
    }

    public Set<String> getProvides() {
        return Collections.unmodifiableSet(relationshipHeaders.computeIfAbsent(PROVIDES_HEADER, s -> new HashSet<>()));
    }

    public void addProvides(String packageName) {
        if (!PACKAGE_PATTERN.matcher(packageName).matches())
            throw new IllegalArgumentException(
                    "Provided package name should match '" + PACKAGE_PATTERN.pattern() + "', was '" + packageName + "'"
            );
        relationshipHeaders.computeIfAbsent(PROVIDES_HEADER, s -> new HashSet<>()).add(packageName);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void writeControlFile(Writer writer) throws IOException {
        ControlFileWriter controlWriter = new ControlFileWriter(writer);
        for (Map.Entry<String, String> e: headers.entrySet()) {
            controlWriter.writeSingleLineField(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, Set<String>> e: relationshipHeaders.entrySet()) {
            controlWriter.writeSingleLineList(e.getKey(), e.getValue());
        }
        controlWriter.writeFormattedField(DESCRIPTION_HEADER, synopsis, description);
    }
}
