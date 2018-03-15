package io.solit.plugin.maven.deb.man;

import io.solit.deb.man.ManPage;
import io.solit.deb.man.Section;
import io.solit.deb.man.parse.ManParseException;
import io.solit.deb.man.parse.MarkdownParser;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;

/**
 * Goal generates linux man pages from markdown source. Generation process is similar to one used by <code>ruby-ronn</code> utility.
 * <p>
 *     Goal processes every file in source directory with <code>.md</code> extension (case-insensitive), and converts it to a man page.
 *     Files with other extensions are silently ignored.
 * <p>
 *     Each processed file must contain a level one header specifying man title, section and short description used by man-db to
 *     generate apropos database. File content before header which follows this format will be ignored
 * <p>
 *     For instance, the following code will create man page with title <code>foo</code> in section <code>5</code>
 *     with short description <code>bar baz qux</code>
 *     <pre><code>
 *         # foo(5) - bar baz qux
 *     </code></pre>
 * <p>
 *     Other headers in file will be turned to a sections of man file, if header of level one or two and, to a subheader for other levels
 * <p>
 *     Generation supports, lists (inc. nested), definition, code blocks, quotes, emphasis and other markdown feature described in
 *     commonmark markdown specification.
 * @author yaga
 * @since 06.03.18
 */
@Mojo(name = "man", defaultPhase = LifecyclePhase.COMPILE)
public class ManMojo extends AbstractMojo {
    private static final Pattern MAN_FILE_PATTERN = Pattern.compile("(.+)\\.(\\d+)\\.md", Pattern.CASE_INSENSITIVE);
    private static final Pattern MAN_DIRECTORY_PATTERN = Pattern.compile("man(\\d+)");

    /**
     * Directory to read markdown files for man page generation.
     * <p>
     *     Only files with <code>.md</code> (case-insensitive) will be processed
     * <p>
     *     Subdirectory structure will be preserved, with exception for <code>.md</code> files
     *     in directory itself. Such files will be placed to a directory named <code>man[sectionNumber]</code>
     * <p>
     *     Defaults to <code>${project.basedir}/src/deb/doc</code>
     */
    @Parameter(defaultValue = "${project.basedir}/src/deb/doc")
    private File manSourceDirectory;

    /**
     * Directory to write generated man pages to.
     * <p>
     *     Subdirectory structure of source directory will be preserved, with exception for <code>.md</code> files
     *     in directory itself. Such files will be placed to a directory named <code>man[sectionNumber]</code>
     * <p>
     *     If not specified <code>/usr/share/man</code> in stage directory will be used.
     */
    @Parameter()
    private File manDestinationDirectory;

    /**
     * Whether to try to create man title and description, if no proper heading found inside markdown file.
     * <p>
     *     Title will be deduced the following way:
     *     <ol>
     *         <li>
     *             if file name matches <code>title.[number].md</code> pattern, then <code>title</code> is used as man title,
     *             and <code>[number]</code> as man section
     *         </li>
     *         <li>
     *             else if file is in directory <code>>man[number]</code>, than file name (without extension) is used as man title and,
     *             <code>[number]</code> as man section
     *         </li>
     *         <li>
     *             otherwise file name (without extension) is used as title and section is assumed to be <code>1</code>
     *         </li>
     *     </ol>
     * <p>
     *     For short description required by man page specification combination of title and section name will be used
     */
    @Parameter(defaultValue = "true")
    private boolean deduceManTitle = true;

    /**
     * Treat parser and validation warnings as failures and abort generation.
     */
    @Parameter(defaultValue = "false")
    private boolean strictManParsing = false;

    /**
     * Manual sources specified in a manual header. Package name will be used by default.
     */
    @Parameter()
    private String manSource;

    /**
     * Manual name specified in a manual header.
     */
    @Parameter()
    private String manName;

    /**
     * Name of debian package
     */
    @Parameter(property = "deb.name", defaultValue = "${project.artifactId}")
    private String packageName;

    /**
     * Stage directory, containing files to be included into a deb package
     */
    @Parameter(defaultValue = "${project.build.directory}/deb")
    private File stageDir;

    /**
     * Encoding used to read source files
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String sourceEncoding;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Charset charset;
            if (this.sourceEncoding == null) {
                charset = Charset.defaultCharset();
                getLog().warn("File encoding has not been set, using platform encoding " + charset.name() +
                        " i.e. build is platform dependent!");
            } else
                charset = Charset.forName(this.sourceEncoding);
            Path dest;
            if (this.manDestinationDirectory == null)
                dest = stageDir.toPath().resolve("usr").resolve("share").resolve("man");
            else
                dest = manDestinationDirectory.toPath();
            MarkdownParser parser = new MarkdownParser()
                    .setSource(manSource == null ? packageName : manSource)
                    .setManual(manName == null ? packageName + " manual" : manName);
            Path source = manSourceDirectory.toPath();
            if (Files.isDirectory(source))
                Files.walkFileTree(manSourceDirectory.toPath(), new MdVisitor(charset, parser, source, dest));
        } catch (IOException e) {
            if (e.getCause() instanceof ManParseException)
                throw new MojoFailureException(e.getMessage(), e);
            else
                throw new MojoExecutionException(e.getMessage(), e);
        }
    }


    private class MdVisitor extends SimpleFileVisitor<Path> {
        private final Charset charset;
        private final MarkdownParser parser;
        private final Path destination;
        private final Path source;

        public MdVisitor(Charset charset, MarkdownParser parser, Path source, Path destination) {
            this.charset = charset;
            this.parser = parser;
            this.source = source;
            this.destination = destination;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (Files.isRegularFile(file) && file.getFileName().toString().toLowerCase().endsWith(".md")) {
                Path relative = source.relativize(file);
                Path parent = relative.getParent();
                Consumer<String> warningHandler;
                if (strictManParsing)
                    warningHandler = w -> { throw new ManParseException(w); };
                else
                    warningHandler = w -> getLog().warn("File '" + file.toString() + "': " + w);
                ManPage page;
                try (
                        InputStream is = new FileInputStream(file.toFile());
                        Reader input = new InputStreamReader(is, charset)
                ) {
                    if (deduceManTitle) {
                        String title;
                        int section;
                        String description;
                        Matcher fn = MAN_FILE_PATTERN.matcher(file.getFileName().toString());
                        if (fn.matches()) {
                            title = fn.group(1);
                            section = Integer.parseInt(fn.group(2));
                        } else {
                            title = file.getFileName().toString();
                            title = title.substring(0, title.length() - 3); // removing ".md" extension
                            if (parent != null) {
                                Matcher pd = MAN_DIRECTORY_PATTERN.matcher(parent.getFileName().toString());
                                if (pd.matches())
                                    section = Integer.parseInt(pd.group(1));
                                else
                                    section = 1;
                            } else
                                section = 1;
                        }
                        description = createDefaultDescription(title, section);
                        page = parser.parse(input, warningHandler, title, section, description);
                    } else
                        page = parser.parse(input, warningHandler);
                    validateManPage(page, warningHandler);
                } catch (ManParseException e) {
                    throw new IOException("Markdown processing error: File: '" + file.toString() + "': " + e.getMessage(), e);
                }
                if (parent == null)
                    parent = Paths.get("man" + page.getManSection());
                parent = destination.resolve(parent);
                String destinationName = page.getName() + "." + page.getManSection() + ".gz";
                Files.createDirectories(parent);
                GzipParameters parameters = new GzipParameters();
                parameters.setCompressionLevel(Deflater.BEST_COMPRESSION);
                try(
                        OutputStream os = new FileOutputStream(parent.resolve(destinationName).toFile());
                        OutputStream gz = new GzipCompressorOutputStream(os, parameters);
                        Writer wr = new OutputStreamWriter(gz, StandardCharsets.UTF_8)
                ) {
                    page.write(wr);
                }
            }
            return FileVisitResult.CONTINUE;
        }

        private String createDefaultDescription(String title, int section) {
            String description;
            switch (section) {
                default:
                    description = title + " user command";
                    break;
                case 2:
                    description = title + " system call";
                    break;
                case 3:
                    description = title + " library";
                    break;
                case 4:
                    description = title + " special file";
                    break;
                case 5:
                    description = title + " file format";
                    break;
                case 6:
                    description = title + " game";
                    break;
                case 7:
                    description = title + " overview";
                    break;
                case 8:
                    description = title + " administrative utility";
                    break;
            }
            return description;
        }

        private void validateManPage(ManPage page, Consumer<String> warningHandler) {
            validateSections(page, warningHandler);
        }

        private void validateSections(ManPage page, Consumer<String> warningHandler) {
            Set<Integer> standard = new LinkedHashSet<>();
            Set<String> nonStandard = new HashSet<>();
            for (Section s: page.getAdditionalSections()) {
                switch (s.getName().toUpperCase()) {
                    case "SYNOPSIS":
                        if (!standard.add(2))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "CONFIGURATION":
                        if (!standard.add(3))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "DESCRIPTION":
                        if (!standard.add(4))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "OPTIONS":
                        if (!standard.add(5))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "EXIT STATUS":
                        if (!standard.add(6))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "RETURN VALUE":
                        if (!standard.add(7))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "ERRORS":
                        if (!standard.add(8))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "ENVIRONMENT":
                        if (!standard.add(9))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "FILES":
                        if (!standard.add(10))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "VERSIONS":
                        if (!standard.add(11))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "ATTRIBUTES":
                        if (!standard.add(12))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "CONFORMING TO":
                        if (!standard.add(13))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "NOTES":
                        if (!standard.add(14))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "BUGS":
                        if (!standard.add(15))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "EXAMPLE":
                        if (!standard.add(16))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    case "SEE ALSO":
                        if (!standard.add(17))
                            warningHandler.accept("Duplicate section name " + s.getName());
                        break;
                    default:
                        if (!nonStandard.add(s.getName().toUpperCase()))
                            warningHandler.accept("Duplicate section name " + s.getName());
                }
            }
            if (!standard.containsAll(Arrays.asList(2, 4, 17)))
                warningHandler.accept("Not all recommended sections are present: [SYNOPSIS, DESCRIPTION, SEE ALSO]");
            int last = 0;
            for (Integer i: standard) {
                if (i < last) {
                    warningHandler.accept("Standard sections should follow in recommended order");
                    break;
                }
                last = i;
            }
        }
    }
}
