package io.solit.plugin.maven.deb.populate;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Recursively copies files from a <code>dataSourceDir</code> to a <code>stageDir</code>
 * and from <code>controlSourceDir</code> to <code>controlDir</code>.
 * <p>
 *     If source directory does not exist, nothing will be copied, and plugin will finish correctly
 * <p>
 *     Files are not overridden during this operation
 * <p>
 *     Symbolic links are copied as is, without following them.
 * @author yaga
 * @since 16.03.18
 */
@Mojo(name = "data", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class DataMojo extends AbstractMojo {

    /**
     * Source directory, containing files to be included into a deb package
     */
    @Parameter(defaultValue = "${project.basedir}/src/deb/data")
    private File dataSourceDir;

    /**
     * Source directory, containing files to be inclued into a control section of a deb packages
     */
    @Parameter(defaultValue = "${project.basedir}/src/deb/control")
    private File controlSourceDir;

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

    public void copyDirectory(File source, File destination) throws IOException {
        Path src = source.toPath(), dst = destination.toPath();
        if (!Files.isDirectory(src))
            return;
        Files.createDirectories(dst);
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = dst.resolve(src.relativize(dir));
                if (Files.isDirectory(target))
                    return FileVisitResult.CONTINUE;
                Files.copy(dir, target, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = dst.resolve(src.relativize(file));
                if (Files.exists(target))
                    return FileVisitResult.CONTINUE;
                Files.copy(file, target, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void execute() throws MojoExecutionException {
        try {
            copyDirectory(controlSourceDir, controlDir);
            copyDirectory(dataSourceDir, stageDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Exception while coping directory", e);
        }
    }
}
