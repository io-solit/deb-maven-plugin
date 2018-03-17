package io.solit.plugin.maven.deb.changelog;

import io.solit.deb.Version;
import io.solit.deb.changes.ChangeSet;
import io.solit.deb.changes.Changelog;
import io.solit.deb.changes.KeepChangelogParser;
import io.solit.deb.changes.StringChanges;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.Deflater;

/**
 * This mojo creates and/or copies a changelog file as requested by debian policy
 * <p>
 *     If <code>copyOriginalChangelog</code> is set to <code>true</code> <code>upstreamChangelogSource</code> file
 *     will be gzipped and copied into <code>changelogDestinationDirectory</code> as <code>changelog.gz</code>
 * <p>
 *     If <code>convertToDebianChangelog</code> is set, plugin will try to parse <code>changelogSource</code> as
 *     markdown file written according to keepachangelog.com recommendations and write a changelog in a debian
 *     format as <code>changelog.gz</code> os as <code>changelog.Debian.gz</code> if <code>copyOriginalChangelog</code>
 *     is also set or current version has a revision part
 * @author yaga
 * @since 3/15/18
 * @see <a href="https://keepachangelog.com/en/1.0.0">Keep a changelog</a>
 */
@Mojo(name = "changelog", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ChangelogMojo extends AbstractMojo {
    private static final String SNAPSHOT = "-SNAPSHOT";
    public static final String DEBIAN_CHANGELOG = "changelog.Debian.gz";
    public static final String UPSTREAM_CHANGELOG = "changelog.gz";

    /**
     * Whether to copy a gzipped original changelog to a destination folder
     */
    @Parameter()
    private boolean copyOriginalChangelog = false;

    /**
     * Whether to try parsing changelog and converting it to a form
     * specified by debian policy
     */
    @Parameter()
    private boolean convertToDebianChangelog = true;

    /**
     * Whether to append unreleased or yanked change set, if current version does not match version
     * in last change set.
     */
    @Parameter()
    private boolean appendCurrentVersionChangeSet = true;

    /**
     * A source changelog file.
     * <p>
     *     If <code>convertToDebianChangelog</code> is set, plugin will try to parse it as
     *     markdown file written according to keepachangelog.com recommendations and
     *     write a changelog in a debian format as <code>changelog.Debian.gz</code>
     *
     */
    @Parameter(defaultValue = "${project.basedir}/changelog.md")
    private File changelogSource;

    /**
     * An upstream changelog file
     * <p>
     *     If <code>copyOriginalChangelog</code> is set to <code>true</code> this file
     *     will be gzipped and copied into destination directory as <code>changelog.gz</code>
     * <p>
     *     If both <code>copyOriginalChangelog</code> and <code>convertToDebianChangelog</code> are set,
     *     or current version has a revision part plugin write a changelog in a debian format as
     *     <code>changelog.Debian.gz</code>
     */
    @Parameter(defaultValue = "${project.basedir}/changelog.md")
    private File upstreamChangelogSource;

    /**
     * Upstream part of a package version
     */
    @Parameter(property = "deb.version", defaultValue = "${project.version}")
    private String version;

    /**
     * Revision part of a package version
     */
    @Parameter(property = "deb.revision")
    private String revision;


    /**
     * Directory to write changelog to.
     * <p>
     *     If not specified <code>${stageDir}/usr/share/doc/${packageName}</code> will be used.
     */
    @Parameter()
    private File changelogDestinationDirectory;

    /**
     * Stage directory, containing files to be included into a deb package
     */
    @Parameter(defaultValue = "${project.build.directory}/deb")
    private File stageDir;

    /**
     * Name of debian package
     */
    @Parameter(property = "deb.name", defaultValue = "${project.artifactId}")
    private String packageName;

    /**
     * Package maintainer's name
     */
    @Parameter(property = "deb.maintainer", defaultValue = "${project.developers[0].name}")
    private String maintainer;

    /**
     * Package maintainer's email address
     */
    @Parameter(property = "deb.maintainer", defaultValue = "${project.developers[0].email}")
    private String maintainerEmail;

    /**
     * A distribution to write to changelog
     */
    @Parameter(defaultValue = "stable")
    private String targetDistribution = "stable";

    /**
     * Encoding used to read source file
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String sourceEncoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (changelogSource == null || !changelogSource.isFile())
            return;
        Path destinationDirectory;
        if (changelogDestinationDirectory== null)
            destinationDirectory = stageDir.toPath().resolve("usr").resolve("share").resolve("doc").resolve(packageName);
        else
            destinationDirectory = changelogDestinationDirectory.toPath();
        try {
            Files.createDirectories(destinationDirectory);
            if (copyOriginalChangelog)
                createUpstreamChangelog(destinationDirectory);
            if (convertToDebianChangelog)
                createDebianChangelog(destinationDirectory);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while creating changelog", e);
        }
    }

    private void createUpstreamChangelog(Path destinationDirectory) throws IOException {
        Path changelogFile = destinationDirectory.resolve(UPSTREAM_CHANGELOG);
        if (Files.exists(changelogFile))
            return;
        GzipParameters gp = new GzipParameters();
        gp.setCompressionLevel(Deflater.BEST_COMPRESSION);
        try(
                OutputStream os = new FileOutputStream(changelogFile.toFile());
                OutputStream gos = new GzipCompressorOutputStream(os, gp)
        ) {
            Files.copy(changelogSource.toPath(), gos);
        }
    }

    private void createDebianChangelog(Path destinationDirectory) throws MojoFailureException, IOException {
        Version unreleasedVersion = getUnreleasedVersion();
        Path changelogFile = destinationDirectory.resolve(
                copyOriginalChangelog || unreleasedVersion.getRevision() != null ? DEBIAN_CHANGELOG : UPSTREAM_CHANGELOG
        );
        if (Files.exists(changelogFile))
            return;
        if (maintainer == null || maintainerEmail == null)
            throw new MojoFailureException("Maintainer and email should not be null");
        Charset charset;
        if (this.sourceEncoding == null) {
            charset = Charset.defaultCharset();
            getLog().warn(
                    "File encoding has not been set, using platform encoding " + charset.name() +
                    " i.e. build is platform dependent!"
            );
        } else
            charset = Charset.forName(this.sourceEncoding);
        KeepChangelogParser parser = new KeepChangelogParser(packageName, maintainer, maintainerEmail);
        parser.setDefaultDistribution(targetDistribution);
        parser.setUnreleasedVersion(unreleasedVersion);
        Changelog changelog;
        try (
                InputStream is = new FileInputStream(changelogSource);
                Reader reader = new InputStreamReader(is, charset)
        ) {
            changelog = parser.parse(reader);
        }
        if (changelog == null)
            return;
        if (unreleasedVersion != null)
            createUnreleasedVersionChangeSet(unreleasedVersion, changelog);
        GzipParameters gp = new GzipParameters();
        gp.setCompressionLevel(Deflater.BEST_COMPRESSION);
        try (
                OutputStream os = new FileOutputStream(changelogFile.toFile());
                GzipCompressorOutputStream gos = new GzipCompressorOutputStream(os, gp);
                Writer wr = new OutputStreamWriter(gos, StandardCharsets.UTF_8)
        ) {
            changelog.write(wr);
        }
    }

    private Version getUnreleasedVersion() {
        if (!appendCurrentVersionChangeSet)
            return null;
        String version = this.version, revision = this.revision;
        if (revision == null && version.endsWith(SNAPSHOT)) {
            version = version.substring(0, version.length() - SNAPSHOT.length());
            revision = "b" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
        return new Version(version, revision);
    }

    private void createUnreleasedVersionChangeSet(Version version, Changelog changelog) {
        boolean currentPresent = changelog.getChanges().stream().anyMatch(c -> version.equals(c.getVersion()));
        if (currentPresent)
            return;
        changelog.addChangeSet(
                new ChangeSet(packageName, version, maintainer, maintainerEmail, StringChanges.YANKED)
                    .setDistribution(this.targetDistribution)
        );
    }
}
