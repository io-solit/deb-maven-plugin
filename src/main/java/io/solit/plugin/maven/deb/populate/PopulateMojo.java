package io.solit.plugin.maven.deb.populate;

import io.solit.plugin.maven.deb.dependencies.AbstractDependencyMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Copies filtered project runtime dependencies into a specified dir
 * @author yaga
 * @since 24.01.18
 */
@Mojo(
        name = "populate",
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        defaultPhase = LifecyclePhase.PREPARE_PACKAGE
)
public class PopulateMojo extends AbstractDependencyMojo<Void> {

    @Override
    protected void processDependency(DependencyArtifact node, Void nothing, File dependencyDir, boolean root) throws MojoExecutionException {
        try {
            File src = node.getArtifact().getFile();
            if (src == null)
                if (root)
                    return;
                else
                    throw new MojoExecutionException("Unresolved dependency: " + node.getArtifact().toString());
            Path target = new File(dependencyDir, src.getName()).toPath();
            if (Files.exists(target))
                return;
            Files.copy(
                    src.toPath(), target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to copy artifact " + node.getArtifact().toString(), e);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File dependencyDir = getDependencyDirectory();
        if (!dependencyDir.isDirectory() && !dependencyDir.mkdirs())
            throw new MojoExecutionException("Unable to create directory " + dependencyDir.toString());
        traverseDependencies(null);
    }

}
