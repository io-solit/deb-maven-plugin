package io.solit.plugin.maven.deb.dependencies;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;

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

    public boolean exclude(Artifact artifact) {
        if (pattern != null) {
            if (artifact.toString().matches(pattern))
                return true;
        }
        if (dependencies != null) {
            for (Dependency d: dependencies) {
                if (d.getGroupId() != null && !d.getGroupId().equals(artifact.getGroupId()))
                    continue;
                if (d.getArtifactId() != null && !d.getArtifactId().equals(artifact.getArtifactId()))
                    continue;
                if (d.getVersion() != null && !d.getVersion().equals(artifact.getVersion()))
                    continue;
                if (d.getType() != null && !d.getType().equals(artifact.getType()))
                    continue;
                if (d.getClassifier() != null && !d.getClassifier().equals(artifact.getClassifier()))
                    continue;
                return true;
            }
        }
        return false;
    }

}
