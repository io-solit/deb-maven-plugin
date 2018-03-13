package io.solit.deb.man;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yaga
 * @since 26.02.18
 */
public class ManPage {
    private static final String NAME_SECTION = "NAME";
    private final String title;
    private final int manSection;
    private String source = "Linux";
    private String manual = "Manual";
    private LocalDate date;
    private String name;
    private String shortDescription;
    private final List<Section> additionalSections = new ArrayList<>();

    public ManPage(String title, String desc, int manSection) {
        this.title = Objects.requireNonNull(title, "Manual title should not be null");
        this.name = title;
        this.shortDescription = Objects.requireNonNull(desc, "Description must not be null");
        this.manSection = manSection;
        this.date = LocalDate.now();
    }

    public String getTitle() {
        return title;
    }

    public int getManSection() {
        return manSection;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = Objects.requireNonNull(date, "Date should not be null");
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = Objects.requireNonNull(source, "Source must not be null");
    }

    public String getManual() {
        return manual;
    }

    public void setManual(String manual) {
        this.manual = Objects.requireNonNull(manual, "Manual must not be null");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "Name must not be null");
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public List<Section> getAdditionalSections() {
        return additionalSections;
    }

    public void write(Writer writer) throws IOException {
        RoffWriter roffWriter = new RoffWriter(writer);
        roffWriter.writeCommentLine("Generated with deb-maven-plugin");
        roffWriter.writeStructureLine();
        roffWriter.writeManHeader(title, manSection, date, source, manual);
        roffWriter.disableHyphenation();
        roffWriter.writeStructureLine();
        roffWriter.writeHeader(NAME_SECTION);
        roffWriter.write(name);
        roffWriter.write(" - ");
        roffWriter.write(shortDescription);
        for (Section as: additionalSections) {
            as.write(roffWriter);
        }
    }
}
