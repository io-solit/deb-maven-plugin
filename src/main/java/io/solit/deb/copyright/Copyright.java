package io.solit.deb.copyright;

import io.solit.deb.ControlFileWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author yaga
 * @since 23.01.18
 */
public class Copyright {
    private static final String FORMAT = "https://www.debian.org/doc/packaging-manuals/copyright-format/1.0/";
    private static final String FORMAT_HEADER = "Format";
    private static final String UPSTREAM_NAME_HEADER = "Upstream-Name";
    private static final String UPSTREAM_CONTACT_HEADER = "Upstream-Contact";
    private static final String SOURCE_HEADER = "Source";
    private static final String DISCLAIMER_HEADER = "Disclaimer";
    private static final String COMMENT_HEADER = "Comment";
    private static final String LICENCE_HEADER = "Licence";
    private static final String COPYRIGHT_HEADER = "Copyright";
    private String upstreamName;
    private String source;
    private String disclaimer;
    private String comment;
    private String licence;
    private String licenceContent;
    private String copyright;
    private final List<String> upstreamContact = new ArrayList<>();
    private final List<CopyrightFiles> files = new ArrayList<>();
    private final List<CopyrightLicence> licences = new ArrayList<>();


    public Copyright(Set<String> files, String copyright, String licence) {
        this.files.add(new CopyrightFiles(files, copyright, licence));
    }

    public Copyright(Set<String> files, String copyright, String licence, String comment) {
        CopyrightFiles f = new CopyrightFiles(files, copyright, licence);
        this.files.add(f);
        f.setComment(comment);
    }

    public Copyright(Set<String> files, String copyright, String licence, String comment, String licenceContent) {
        CopyrightFiles f = new CopyrightFiles(files, copyright, licence);
        this.files.add(f);
        f.setComment(comment);
        f.setLicenceContent(licenceContent);
    }

    public String getUpstreamName() {
        return upstreamName;
    }

    public void setUpstreamName(String upstreamName) {
        if (upstreamName != null && upstreamName.trim().isEmpty())
            throw new IllegalArgumentException("Upstream name should not be empty");
        if (upstreamName != null && upstreamName.contains("\n"))
            throw new IllegalArgumentException("Upstream name should be one line");
        this.upstreamName = upstreamName;
    }

    public List<String> getUpstreamContact() {
        return Collections.unmodifiableList(upstreamContact);
    }

    public void addUpstreamContact(String contact) {
        if (contact == null)
            throw new NullPointerException("Upstream contact must not be null");
        contact = contact.trim();
        if (contact.isEmpty())
            throw new IllegalArgumentException("Upstream contact must not be empty");
        if (contact.contains("\n"))
            throw new IllegalArgumentException("Upstream contact should be one line");
        upstreamContact.add(contact.trim());
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getLicenceName() {
        return licence;
    }

    public String getLicenceContent() {
        return licenceContent;
    }

    public void setLicence(String name) {
        setLicence(name, null);
    }

    public void setLicence(String name, String content) {
        if (name == null && content != null)
            throw new IllegalArgumentException("Licence content can not be present if name is null");
        if (name != null) {
            name = name.trim();
            if (name.isEmpty())
                throw new IllegalArgumentException("Licence name should not be empty");
            if (name.contains("\n"))
                throw new IllegalArgumentException("Licence name should not contain line separators");
        }
        licence = name;
        licenceContent = content;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public List<CopyrightFiles> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public CopyrightFiles addFiles(Set<String> files, String copyright, String licence) {
        CopyrightFiles file = new CopyrightFiles(files, copyright, licence);
        this.files.add(file);
        return file;
    }

    public List<CopyrightLicence> getLicences() {
        return Collections.unmodifiableList(licences);
    }

    public CopyrightLicence addStandAloneLicence(String name, String licence) {
        CopyrightLicence l = new CopyrightLicence(name, licence);
        this.licences.add(l);
        return l;
    }

    public void writeCopyright(Writer writer) throws IOException {
        ControlFileWriter controlWriter = new ControlFileWriter(writer);
        controlWriter.writeSingleLineField(FORMAT_HEADER, FORMAT);

        if (upstreamName != null)
            controlWriter.writeSingleLineField(UPSTREAM_NAME_HEADER, upstreamName);

        if (!upstreamContact.isEmpty())
            controlWriter.writeLineBasedList(UPSTREAM_CONTACT_HEADER, upstreamContact);

        if (source != null)
            controlWriter.writeFormattedField(SOURCE_HEADER, null, source);

        if (disclaimer != null)
            controlWriter.writeFormattedField(DISCLAIMER_HEADER, null, disclaimer);

        if (comment != null)
            controlWriter.writeFormattedField(COMMENT_HEADER, null, comment);

        if (licence != null)
            controlWriter.writeFormattedField(LICENCE_HEADER, licence, licenceContent);

        if (copyright != null)
            controlWriter.writeFormattedField(COPYRIGHT_HEADER, null, copyright);

        for (CopyrightFiles file : files) {
            controlWriter.nextParagraph();
            file.writeFiles(controlWriter);
        }

        for (CopyrightLicence f : licences) {
            controlWriter.nextParagraph();
            f.writeLicences(controlWriter);
        }

    }
}
