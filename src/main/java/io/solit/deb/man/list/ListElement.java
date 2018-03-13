package io.solit.deb.man.list;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;
import io.solit.deb.man.block.ManParagraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yaga
 * @since 06.03.18
 */
public class ListElement {
    private final List<ManPart> parts = new ArrayList<>();

    public ListElement() {}

    public ListElement(ManPart part) {
        getParts().add(part);
    }

    public List<ManPart> getParts() {
        return parts;
    }

    public void write(RoffWriter writer) throws IOException {
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i) instanceof ManParagraph) {
                if (i > 0)
                    writer.startIndentedParagraph(ManPart.INDENT_STEP);
                parts.get(i).write(writer);
            } else {
                writer.openBlock();
                parts.get(i).write(writer);
                writer.completeBlock();
            }

        }
    }

}
