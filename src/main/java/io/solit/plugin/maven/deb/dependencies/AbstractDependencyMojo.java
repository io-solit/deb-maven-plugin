package io.solit.plugin.maven.deb.dependencies;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;

import java.io.File;

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

    protected void traverseDependencies(T context) throws MojoExecutionException, MojoFailureException {
        try {
            ProjectBuildingRequest request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            File dependencyDirectory = getDependencyDirectory();
            request.setProject(project);
            DependencyNode root = dependencyGraphBuilder.buildDependencyGraph(request, new ScopeArtifactFilter(Artifact.SCOPE_RUNTIME));
            root.accept(new DependencyNodeVisitor() {
                        @Override
                        public boolean visit(DependencyNode node) {
                            if (traverseExclusions != null && traverseExclusions.exclude(node))
                                return false;
                            try {
                                if (packageExclusions == null || !packageExclusions.exclude(node))
                                    processDependency(node, context, dependencyDirectory, node == root);
                            } catch (MojoExecutionException e) {
                                throw new TransportException(e);
                            }
                            return true;
                        }

                        @Override
                        public boolean endVisit(DependencyNode node) {
                            return true;
                        }
                    });
        } catch (DependencyGraphBuilderException e) {
            throw new MojoFailureException("Unable to collect dependencies" + e.getMessage(), e);
        } catch (TransportException e) {
            throw e.getCause();
        }
    }

    protected abstract void processDependency(DependencyNode node, T context, File dependencyDir, boolean root) throws MojoExecutionException;

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

    private static class TransportException extends RuntimeException {

        public TransportException(MojoExecutionException cause) {
            super(cause);
        }

        @Override
        public synchronized MojoExecutionException getCause() {
            return (MojoExecutionException) super.getCause();
        }
    }
}
