package io.solit.deb.copyright;

import io.solit.deb.ControlFileWriter;

import java.io.IOException;

/**
 * @author yaga
 * @since 23.01.18
 */
public class CopyrightLicence {
    private static final String LICENCE_HEADER = "Licence";
    private static final String COMMENT_HEADER = "Comment";
    private final String name;
    private final String licence;
    private String comment;

    CopyrightLicence(String name, String licence) {
        this.name = name.trim();
        this.licence = licence;
        if (this.name.isEmpty())
            throw new IllegalArgumentException("Licence name should not be empty");
        if (this.name.contains("\n"))
            throw new IllegalArgumentException("Licence name should not contain line separators");
    }

    public String getName() {
        return name;
    }

    public String getLicence() {
        return licence;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void writeLicences(ControlFileWriter controlWriter) throws IOException {
        controlWriter.writeFormattedField(LICENCE_HEADER, name, licence);
        if (comment != null)
            controlWriter.writeFormattedField(COMMENT_HEADER, null, comment);
    }
}
