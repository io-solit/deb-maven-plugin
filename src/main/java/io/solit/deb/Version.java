package io.solit.deb;

import java.util.regex.Pattern;

/**
 * @author yaga
 * @since 19.01.18
 */
public class Version {
    public static final Pattern REVISION_PATTERN = Pattern.compile("^[\\w+.~]+");
    private Integer epoch;
    private String upstream;
    private String revision;

    Version(){}

    public Version(Integer epoch, String upstream, String revision) {
        this.epoch = epoch;
        this.upstream = upstream;
        this.revision = revision;
    }

    public Version(String upstream) {
        this.upstream = upstream;
    }

    public Version(String upstream, String revision) {
        this.upstream = upstream;
        this.revision = revision;
    }

    public Integer getEpoch() {
        return epoch;
    }

    public String getUpstream() {
        return upstream;
    }

    public String getRevision() {
        return revision;
    }

    public String getValidatedString() {
        StringBuilder result = new StringBuilder();
        if (epoch != null) {
            if (epoch < 0)
                throw new IllegalArgumentException("Epoch should be an unsigned integer");
            result.append(epoch.intValue()).append(":");
        }
        if (upstream == null || upstream.trim().isEmpty())
            throw new IllegalArgumentException("Upstream version should be specified");
        if (!Character.isDigit(upstream.charAt(0)))
            throw new IllegalArgumentException("Upstream should start with a digit, was '" + upstream + "'");
        checkUpstream(upstream, revision);
        result.append(upstream);
        if (revision != null) {
            if (!REVISION_PATTERN.matcher(revision).matches())
                throw new IllegalArgumentException("Revision should match " + REVISION_PATTERN.pattern());
            result.append("-").append(revision);
        }
        return result.toString();
    }

    public static Version parseVersion(String versionString) {
        Integer epoch = null;
        String upstream = versionString, revision = null;
        int colon = upstream.indexOf(':');
        if (colon >= 0) {
            try {
                epoch = Integer.valueOf(versionString.substring(0, colon));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + versionString + "' is not a valid version");
            }
            if (epoch < 0)
                throw new IllegalArgumentException("'" + versionString + "' is not a valid version");
            upstream = upstream.substring(colon + 1);
        }
        int hyphen = upstream.lastIndexOf('-');
        if (hyphen >= 0) {
            revision = upstream.substring(hyphen + 1);
            if (!REVISION_PATTERN.matcher(revision).matches())
                throw new IllegalArgumentException("'" + versionString + "' is not a valid version");
            upstream = upstream.substring(0, hyphen);
        }
        checkUpstream(upstream, revision);
        return new Version(epoch, upstream, revision);
    }

    private static void checkUpstream(String upstream, String revision) {
        for (int i = 1, l = upstream.length(); i < l; i++) {
            if (Character.isAlphabetic(upstream.charAt(i)) || Character.isDigit(upstream.charAt(i)))
                continue;
            switch (upstream.charAt(i)) {
                case '.': case '+': case '~':
                    continue;
                case '-':
                    if (revision == null)
                        throw new IllegalArgumentException(
                                "If revision is not specified upstream should not contain hyphens, was '" + upstream + "'"
                        );
                    else
                        continue;
                default:
                    throw new IllegalArgumentException(
                            "Upstream should consist of alphanumeric characters, '+', '.', '~' and '-' if revision is specified, was '" +
                                    upstream + "'"
                    );
            }
        }
    }
}
