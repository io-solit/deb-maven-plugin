package io.solit.plugin.maven.deb.copyright;

import io.solit.deb.copyright.Copyright;
import io.solit.deb.copyright.CopyrightLicence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * @author yaga
 * @since 23.01.18
 */
public class LicenceFile {
    private String name;
    private File file;
    private String comment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void addToCopyright(String sourceEncoding, Copyright copyright) throws IOException {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), sourceEncoding);
            CopyrightLicence copyrightLicence = copyright.addStandAloneLicence(name, content);
            copyrightLicence.setComment(comment);
        } catch (IOException e) {
            throw new IOException("Error reading licence file" + name, e);
        }
    }

}
