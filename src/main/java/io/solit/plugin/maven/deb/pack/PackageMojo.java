package io.solit.plugin.maven.deb.pack;

import io.solit.deb.Control;
import io.solit.deb.DebFileWriter;
import io.solit.deb.Version;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

/**
 * Create deb package based on files situated in a stage dir.
 * <p>
 *     This mojo:
 *     <ul>
 *         <li>creates control file based on configuration</li>
 *         <li>copies control files to a control archive, following present symlinks</li>
 *         <li>writes md5sums of present data files</li>
 *         <li>copies data files from a stage dir to a data archive, following present symlinks</li>
 *         <li>creates symbolic links based on a configuration</li>
 *     </ul>
 * @author yaga
 * @since 18.01.18
 */
@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractMojo {
    private static final String UNIX_SEPARATOR = "/";
    private static final String SNAPSHOT = "-SNAPSHOT";

    /**
     * Names of a control files that should be treated as maintainer scripts
     */
    @Parameter
    private Set<String> maintainerScripts = new HashSet<>(Arrays.asList(
            "preinst", "postinst", "prerm", "postrm", "config"
    ));

    /**
     * Build directory
     */
    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDir;

    /**
     * Stage directory, containing files to be included into a deb package
     */
    @Parameter(defaultValue = "${project.build.directory}/deb")
    private File stageDir;

    /**
     * Stage directory, containing files to be inclued into a control section of a deb packages
     */
    @Parameter(defaultValue = "${project.build.directory}/control")
    private File controlDir;

    /**
     * Target file to save deb package to
     */
    @Parameter()
    private File target;

    /**
     * Name of debian package
     */
    @Parameter(property = "deb.name", defaultValue = "${project.artifactId}")
    private String packageName;

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
     * Package architecture
     */
    @Parameter(property = "deb.architecture", defaultValue = "all")
    private String architecture = "all";

    /**
     * Short one-line package description, human readable name
     */
    @Parameter(property = "deb.synopsis", defaultValue = "${project.name}")
    private String synopsis;

    /**
     * Detailed package description
     */
    @Parameter(defaultValue = "${project.description}")
    private String description;

    /**
     * Project home page
     */
    @Parameter(defaultValue = "${project.url}")
    private String homepage;

    /**
     * Permission changes to apply to packaged files.
     * <p>
     *     Each permission consists of
     *     <ol>
     *         <li><code>permissions</code> - octal permission description</li>
     *         <li><code>include</code> - list of glob patterns to apply permissions change to (if omitted, all files assumed)</li>
     *         <li>
     *             <code>exclude</code> - list of glob patterns to exclude from permission change,
     *             excludes have higher priority than includes
     *         </li>
     *     </ol>
     */
    @Parameter
    private List<PermissionModification> permissions;

    /**
     * Non-required package control fields, including:
     * <ol>
     *     <li>section</li>
     *     <li>priority</li>
     *     <li>source</li>
     *     <li>essential</li>
     *     <li>builtUsing</li>
     *     <li>depends</li>
     *     <li>preDepends</li>
     *     <li>recommends</li>
     *     <li>suggests</li>
     *     <li>enhances</li>
     *     <li>breaks</li>
     *     <li>conflicts</li>
     *     <li>provides</li>
     * </ol>
     */
    @Parameter
    private Attributes packageAttributes;

    /**
     * Symbolic links to create in a package data files. Two fields should be specified for every link:
     * <ol>
     *     <li>linkName</li>
     *     <li>linkDestination</li>
     * </ol>
     */
    @Parameter
    private List<Link> symbolicLinks;

    private List<PermissionModification> getPermissions() {
        if (permissions == null)
            permissions = new ArrayList<>();
        return permissions;
    }

    private TarArchiveEntry createTarEntry(String name) {
        TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(name);
        tarArchiveEntry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);
        tarArchiveEntry.setUserId(0);
        tarArchiveEntry.setGroupId(0);
        return tarArchiveEntry;
    }

    private void copyDataFiles(TarArchiveOutputStream dataArchive) throws IOException {
        Path start = stageDir.toPath();
        List<PermissionModification.CompiledPermissions> permissions = this.getPermissions().stream()
                .map(PermissionModification::compile).collect(Collectors.toList());
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (Files.isSameFile(dir, start))
                    return FileVisitResult.CONTINUE;
                Path relative = start.relativize(dir);
                TarArchiveEntry entry = new TarArchiveEntry(relative.toString() + "/", TarConstants.LF_DIR);
                entry.setIds(0, 0);
                entry.setModTime(TarArchiveEntry.DEFAULT_DIR_MODE);
                entry.setSize(0);
                entry.setModTime(attrs.lastModifiedTime().toMillis());
                for (PermissionModification.CompiledPermissions p: permissions)
                    if (p.apply(entry, relative))
                        break;
                dataArchive.putArchiveEntry(entry);
                dataArchive.closeArchiveEntry();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink())
                    return FileVisitResult.CONTINUE;
                Path relative = start.relativize(file);
                TarArchiveEntry entry = createTarEntry(relative.toString());
                entry.setSize(attrs.size());
                entry.setModTime(attrs.lastModifiedTime().toMillis());
                for (PermissionModification.CompiledPermissions p: permissions)
                    if (p.apply(entry, relative))
                        break;
                dataArchive.putArchiveEntry(entry);
                IOUtils.copy(new FileInputStream(file.toFile()), dataArchive);
                dataArchive.closeArchiveEntry();
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(start, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, fv);
    }

    private void writeLinks() {
        if (symbolicLinks == null || symbolicLinks.isEmpty())
            return;
        for (Link lnk: symbolicLinks) {
            if (lnk.getLinkName() == null || lnk.getLinkName().trim().isEmpty())
                throw new IllegalArgumentException("Link name is not specified");
            if (lnk.getLinkDestination() == null || lnk.getLinkDestination().trim().isEmpty())
                throw new IllegalArgumentException("Link destination is not specified");
            TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(lnk.getLinkName().trim(), TarConstants.LF_SYMLINK);
            tarArchiveEntry.setUserId(0);
            tarArchiveEntry.setGroupId(0);
            tarArchiveEntry.setSize(0);
            //noinspection OctalInteger
            tarArchiveEntry.setMode(0120777);
            tarArchiveEntry.setLinkName(lnk.getLinkDestination().trim());
        }
    }

    private Control createControl() {
        String version = this.version, revision = this.revision;
        if (revision == null && version.endsWith(SNAPSHOT)) {
            version = version.substring(0, version.length() - SNAPSHOT.length());
            revision = "b" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        }
        String maintainer = this.maintainer;
        if (maintainer == null)
            throw new IllegalArgumentException("Maintainer should be specified");
        if (maintainerEmail != null)
            maintainer += " <" + maintainerEmail + ">";
        Control control = new Control(packageName, new Version(version, revision), maintainer, architecture, synopsis);
        control.setDescription(processDescription());
        if (packageAttributes != null)
            packageAttributes.fillControl(control);
        if (homepage != null)
            control.setHomepage(homepage);
        return control;
    }

    private String processDescription() {
        if (description == null || description.trim().isEmpty())
            return null;
        List<String> lines = new ArrayList<>();
        int maxWhitespaces = Integer.MAX_VALUE;
        Scanner scanner = new Scanner(description);
        StringBuilder result = new StringBuilder(description.length());
        while(scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (result.length() == 0) { //first line is trimmed by maven
                result.append(line.trim()).append('\n');
                continue;
            }
            int ws = countWhitespaces(line);
            if (ws < line.length()) // do not count empty lines
                maxWhitespaces = Math.min(maxWhitespaces, ws);
            lines.add(line);
        }
        for (String line: lines)
            if (line.length() > maxWhitespaces) // empty lines left as is
                result.append(line.substring(maxWhitespaces)).append('\n');
        return result.toString();
    }

    private int countWhitespaces(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i)))
                return i;
        }
        return 0;
    }

    private void copyControlFiles(TarArchiveOutputStream controlArchive) throws IOException {
        Path start = controlDir.toPath();
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                TarArchiveEntry entry = createTarEntry(start.relativize(file).toString());
                entry.setSize(attrs.size());
                if (maintainerScripts.contains(entry.getName()))
                    //noinspection OctalInteger
                    entry.setMode(0100755);
                entry.setModTime(attrs.lastModifiedTime().toMillis());
                controlArchive.putArchiveEntry(entry);
                IOUtils.copy(new FileInputStream(file.toFile()), controlArchive);
                controlArchive.closeArchiveEntry();
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(start, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, fv);
    }

    private void writeControl(TarArchiveOutputStream controlArchive, Control control) throws IOException {
        TarArchiveEntry entry = createTarEntry("control");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (Writer wr = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)) {
            control.writeControlFile(wr);
        }
        entry.setSize(buffer.size());
        controlArchive.putArchiveEntry(entry);
        buffer.writeTo(controlArchive);
        controlArchive.closeArchiveEntry();
    }

    private long writeCheckSumsAndComputeSize(TarArchiveOutputStream controlArchive) throws IOException {
        long size;
        TarArchiveEntry entry = createTarEntry("md5sums");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (Writer wr = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)) {
            size = writeCheckSumsAndComputeSize(wr);
        }
        entry.setSize(buffer.size());
        controlArchive.putArchiveEntry(entry);
        buffer.writeTo(controlArchive);
        controlArchive.closeArchiveEntry();
        return size;
    }

    private long writeCheckSumsAndComputeSize(Writer writer) throws IOException {
        Path start = stageDir.toPath();
        LongAdder adder = new LongAdder();
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (attrs.isSymbolicLink())
                    return FileVisitResult.CONTINUE;
                adder.add(attrs.size());
                try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file.toFile()), MessageDigest.getInstance("MD5"))) {
                    byte[] buffer = new byte[0x2000];
                    //noinspection StatementWithEmptyBody
                    while (dis.read(buffer) >= 0);
                    for (byte b: dis.getMessageDigest().digest()) {
                        int s = b & 0xff;
                        if (s < 0x10)
                            writer.write('0');
                        writer.write(Integer.toHexString(s));
                    }
                    writer.write(' ');
                    String prefix = "";
                    for (Path p: start.relativize(file)) {
                        writer.write(prefix);
                        writer.write(p.toString());
                        prefix = UNIX_SEPARATOR;
                    }
                    writer.write('\n');
                } catch (NoSuchAlgorithmException e) {
                    throw new IOException("Unable to create md5 digest", e);
                }
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(start, fv);
        return adder.longValue();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Control control = createControl();
            File target = this.target;
            if (target == null) {
                String targetName = control.getPackageName() + "_" +
                        control.getVersion().getValidatedString() + "_" +
                        control.getArchitecture() + ".deb";
                target = new File(this.buildDir, targetName);
            }
            try (DebFileWriter deb = new DebFileWriter(target)) {
                try (TarArchiveOutputStream controlArchive = deb.openControl()) {
                    long size = writeCheckSumsAndComputeSize(controlArchive);
                    control.setInstalledSize(size);
                    writeControl(controlArchive, control);
                    copyControlFiles(controlArchive);
                }
                try (TarArchiveOutputStream dataArchive = deb.openData()) {
                    copyDataFiles(dataArchive);
                    writeLinks();
                }
            }
        } catch (IllegalArgumentException e) {
            throw new MojoFailureException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write directory", e);
        }
    }
}
