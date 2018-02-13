package io.solit.plugin.maven.deb.dependencies;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import static org.apache.maven.model.building.ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL;

/**
 * Abstract mojo for dependency traversal
 * @author yaga
 * @since 24.01.18
 */
public abstract class AbstractDependencyMojo<T> extends AbstractMojo {

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(readonly = true, defaultValue = "${session}")
    protected MavenSession session;

    @Parameter(readonly = true, defaultValue = "${project}")
    protected MavenProject project;

    /**
     * Filter to exclude dependencies from traversal.
     * If a dependency matches the filter, it is excluded, as well as it's transitive dependencies
     * <p>
     *     Filter consists of:
     *     <ul>
     *         <li>
     *             <code>dependencies</code> - list of maven gav dependencies. Only specified gav parameters are checked, e.g.
     *             if only version is specified all artifacts of that version will match
     *         </li>
     *         <li><code>pattern</code> - a regular expression applied to a gav string</li>
     *     </ul>
     * </p>
     */
    @Parameter
    private DependenciesFilter traverseExclusions;

    /**
     * Filter to exclude dependencies from packaging.
     * If a dependency matches the filter, it is excluded, but not it's transitive dependencies
     * <p>
     *     Filter consists of:
     *     <ul>
     *         <li>
     *             <code>dependencies</code> - list of maven gav dependencies. Only specified gav parameters are checked, e.g.
     *             if only version is specified all artifacts of that version will match
     *         </li>
     *         <li><code>pattern</code> - a regular expression applied to a gav string</li>
     *     </ul>
     * </p>
     */
    @Parameter
    private DependenciesFilter packageExclusions;

    /**
     * Stage directory, containing files to be included into a deb package
     */
    @Parameter(property = "deb.root", defaultValue = "${project.build.directory}/deb")
    protected File stageDir;

    /**
     * Name of debian package
     */
    @Parameter(property = "deb.name", defaultValue = "${project.artifactId}")
    protected String packageName;

    /**
     * Directory, to copy dependencies to. Defaults to <code>${stageDir}/usr/share/${packageName}</code>
     */
    @Parameter
    private File dependencyDir;

    @Component()
    private ProjectBuilder projectBuilder;

    protected void traverseDependencies(T context) throws MojoExecutionException, MojoFailureException {
        try {
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            File dependencyDirectory = getDependencyDirectory();
            request.setProject(project);
            DependencyNode root = dependencyGraphBuilder.buildDependencyGraph(request, new AndArtifactFilter(Arrays.asList(
                            new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME),
                            artifact -> traverseExclusions == null || !traverseExclusions.exclude(artifact)
            )));
            Deque<DependencyArtifact> front = new ArrayDeque<>(Collections.singletonList(new DependencyArtifact(root, project)));
            for (DependencyArtifact a = front.poll(); a != null; a = front.poll()) {
                a.getChildren().forEach(front::push);
                if (packageExclusions == null || !packageExclusions.exclude(a.getArtifact()))
                    processDependency(a, context, dependencyDirectory, a.node == root);
            }
        } catch (DependencyGraphBuilderException e) {
            throw new MojoFailureException("Unable to collect dependencies" + e.getMessage(), e);
        } catch (ProjectBuildingException e) {
            throw new MojoFailureException("Unable to build project" + e.getMessage(), e);
        }
    }

    protected abstract void processDependency(DependencyArtifact artifact, T context, File dependencyDir, boolean root)
            throws MojoExecutionException;

    protected File getDependencyDirectory() throws MojoExecutionException {
        File dependencyDir = this.dependencyDir;
        if (dependencyDir == null) {
            if (packageName == null || stageDir == null)
                throw new MojoExecutionException("No dependency dir specified");
            dependencyDir = stageDir;
            for (String s: new String[] { "usr", "share", packageName} )
                dependencyDir = new File(dependencyDir, s);
        }
        return dependencyDir;
    }

    protected class DependencyArtifact {
        private final DependencyNode node;
        private final MavenProject project;

        public DependencyArtifact(DependencyNode node, MavenProject project) {
            this.node = node;
            this.project = project;
        }

        public Artifact getArtifact() {
            return node.getArtifact();
        }

        public MavenProject getProject() {
            return project;
        }

        List<DependencyArtifact> getChildren() throws ProjectBuildingException {
            List<DependencyArtifact> result = new ArrayList<>(node.getChildren().size());
            for (DependencyNode node: node.getChildren()) {
                ProjectBuildingRequest request = new DefaultProjectBuildingRequest()
                        .setLocalRepository(session.getLocalRepository())
                        .setRemoteRepositories(project.getRemoteArtifactRepositories())
                        .setRepositorySession(session.getRepositorySession())
                        .setSystemProperties(session.getSystemProperties())
                        .setProcessPlugins(false)
                        .setResolveDependencies(false)
                        .setValidationLevel(VALIDATION_LEVEL_MINIMAL);
                MavenProject project = projectBuilder.build(node.getArtifact(), request).getProject();
                result.add(new DependencyArtifact(node, project));
            }
            return result;
        }

    }
}
