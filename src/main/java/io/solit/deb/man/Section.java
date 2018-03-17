package io.solit.deb.man;

import io.solit.deb.man.block.ManParagraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yaga
 * @since 26.02.18
 */
public class Section {
    private final String name;
    private final List<ManPart> parts = new ArrayList<>();

    public Section(String name) {
        this.name = Objects.requireNonNull(name, "Name should not be null");
    }

    public List<ManPart> getParts() {
        return parts;
    }

    public void write(RoffWriter roffWriter) throws IOException {
        roffWriter.writeStructureLine();
        roffWriter.writeHeader(name.toUpperCase());
        for (ManPart part: parts) {
            if (part instanceof ManParagraph)
                roffWriter.startParagraph();
            part.write(roffWriter);
        }
    }

    public String getName() {
        return name;
    }
}
