package io.solit.plugin.maven.deb.pack;

/**
 * @author yaga
 * @since 22.01.18
 */
public class Link {
    private String linkName;
    private String linkDestination;

    public String getLinkName() {
        return linkName;
    }

    public void setLinkName(String linkName) {
        this.linkName = linkName;
    }

    public String getLinkDestination() {
        return linkDestination;
    }

    public void setLinkDestination(String linkDestination) {
        this.linkDestination = linkDestination;
    }
}
