package io.solit.plugin.maven.deb.pack;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author yaga
 * @since 22.01.18
 */
public class PermissionModification {
    private static final Path ROOT = Paths.get(File.separator);
    private static final int TYPE_MASK = 0xf000;
    private Pattern PERMISSIONS_PATTERN = Pattern.compile("[0-7]{3}");

    private String permissions;

    private Set<String> include;

    private Set<String> exclude;

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Set<String> getInclude() {
        if (include == null)
            include = new HashSet<>();
        return include;
    }

    public Set<String> getExclude() {
        if (exclude == null)
            exclude = new HashSet<>();
        return exclude;
    }

    public CompiledPermissions compile() {
        if (!PERMISSIONS_PATTERN.matcher(permissions.trim()).matches())
            throw new IllegalArgumentException("Please use octal permissions format, instead of '" + permissions + "'");
        FileSystem fs = FileSystems.getDefault();
        return new CompiledPermissions(
            Integer.parseInt(this.permissions.trim(), 8),
            getInclude().stream().map(s -> fs.getPathMatcher("glob:" + s.trim())).collect(Collectors.toSet()),
            getExclude().stream().map(s -> fs.getPathMatcher("glob:" + s.trim())).collect(Collectors.toSet())
        );
    }

    public class CompiledPermissions {
        private int permissions;
        private Set<PathMatcher> include, exclude;

        CompiledPermissions(int permissions, Set<PathMatcher> include, Set<PathMatcher> exclude) {
            this.permissions = permissions;
            this.include = Objects.requireNonNull(include);
            this.exclude = Objects.requireNonNull(exclude);
        }

        public boolean apply(TarArchiveEntry entry, Path path) {
            Path absPath = ROOT.resolve(path);
            if (!include.isEmpty()) {
                boolean included = false;
                for (PathMatcher inc : this.include) {
                    if (inc.matches(absPath)) {
                        included = true;
                        break;
                    }
                }
                if (!included)
                    return false;
            }
            if (!exclude.isEmpty()) {
                for (PathMatcher ex : this.exclude)
                    if (ex.matches(absPath))
                        return false;
            }

            entry.setMode((entry.getMode() & TYPE_MASK) | permissions);
            return true;
        }

    }

}
