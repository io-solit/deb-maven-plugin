package io.solit.deb.copyright;

import io.solit.deb.ControlFileWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author yaga
 * @since 23.01.18
 */
public class CopyrightFiles {
    private final static String FILES_HEADER = "Files";
    private final static String COPYRIGHT_HEADER = "Copyright";
    private final static String LICENCE_HEADER = "Licence";
    private final static String COMMENT_HEADER = "Comment";

    private final Set<String> files = new LinkedHashSet<>();
    private final String copyright;
    private final String licence;
    private String licenceContent;
    private String comment;

    CopyrightFiles(Set<String> files, String copyright, String licence) {
        if (files.isEmpty())
            throw new IllegalArgumentException("No files specified");
        files.forEach(this::addFile);
        if (copyright == null)
            throw new NullPointerException("Copyright is missing");
        copyright = copyright.trim();
        if (copyright.isEmpty())
            throw new IllegalArgumentException("Copyright should not be empty");

        if (licence == null)
            throw new NullPointerException("Licence is missing");
        licence = licence.trim();
        if (licence.isEmpty())
            throw new IllegalArgumentException("Licence is empty");
        if (licence.contains("\n"))
            throw new IllegalArgumentException("Licence name should not contain line separators");

        this.copyright = copyright;
        this.licence = licence;
    }

    public Set<String> getFiles() {
        return Collections.unmodifiableSet(files);
    }

    public void addFile(String filePattern) {
        if (filePattern == null)
            throw new IllegalArgumentException("File can not be null");
        filePattern = filePattern.trim();
        if (filePattern.isEmpty())
            throw new IllegalArgumentException("File can not be empty");
        boolean escaped = false;
        for (int i = 0; i < filePattern.length(); i++) {
            if (filePattern.charAt(i) == '\\')
                escaped = !escaped;
            else if (escaped) {
                switch (filePattern.charAt(i)) {
                    case '?': case '*':
                        break;
                    default:
                        throw new IllegalArgumentException("Illegal escape sequence at '" + filePattern + "'");
                }
                escaped = false;
             } else if (Character.isWhitespace(filePattern.charAt(i)))
                throw new IllegalArgumentException("Forbidden whitespace chars at '" + filePattern + "'");
        }
        if (escaped)
            throw new IllegalArgumentException("Illegal escape sequence at '" + filePattern + "'");
        files.add(filePattern);
    }

    public String getCopyright() {
        return copyright;
    }

    public String getLicence() {
        return licence;
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

    public void writeFiles(ControlFileWriter controlWriter) throws IOException {
        controlWriter.writeMultiLineList(FILES_HEADER, files);
        controlWriter.writeFormattedField(COPYRIGHT_HEADER, null, copyright);
        controlWriter.writeFormattedField(LICENCE_HEADER, licence, licenceContent);
        if (comment != null)
            controlWriter.writeFormattedField(COMMENT_HEADER, null, comment);
    }
}
