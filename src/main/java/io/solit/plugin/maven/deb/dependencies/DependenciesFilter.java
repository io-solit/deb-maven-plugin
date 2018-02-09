package io.solit.plugin.maven.deb.dependencies;

import org.apache.maven.model.Dependency;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.util.List;

/**
 * @author yaga
 * @since 24.01.18
 */
public class DependenciesFilter {

    private List<Dependency> dependencies;

    private String pattern;

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean exclude(DependencyNode dependencyNode) {
        if (pattern != null) {
            if (dependencyNode.getArtifact().toString().matches(pattern))
                return true;
        }
        if (dependencies != null) {
            for (Dependency d: dependencies) {
                if (d.getGroupId() != null && !d.getGroupId().equals(dependencyNode.getArtifact().getGroupId()))
                    continue;
                if (d.getArtifactId() != null && !d.getArtifactId().equals(dependencyNode.getArtifact().getArtifactId()))
                    continue;
                if (d.getVersion() != null && !d.getVersion().equals(dependencyNode.getArtifact().getVersion()))
                    continue;
                if (d.getType() != null && !d.getType().equals(dependencyNode.getArtifact().getType()))
                    continue;
                if (d.getClassifier() != null && !d.getClassifier().equals(dependencyNode.getArtifact().getClassifier()))
                    continue;
                return true;
            }
        }
        return false;
    }

}
