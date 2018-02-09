package io.solit.plugin.maven.deb.copyright;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yaga
 * @since 23.01.18
 */
public class CopyrightPatterns {
    private Set<String> files = new HashSet<>();
    private String copyright;
    private String licence;
    private String licenceContent;
    private String comment;

    public Set<String> getFiles() {
        return files;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public String getLicence() {
        return licence;
    }

    public void setLicence(String licence) {
        this.licence = licence;
    }

    public String getLicenceContent() {
        return licenceContent;
    }

    public void setLicenceContent(String licenceContent) {
        this.licenceContent = licenceContent;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
