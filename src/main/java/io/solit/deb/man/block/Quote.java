package io.solit.deb.man.block;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yaga
 * @since 05.03.18
 */
public class Quote implements ManPart {
    private final List<ManPart> parts = new ArrayList<>();

    public List<ManPart> getParts() {
        return parts;
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.writeStructureLine();
        writer.openBlock(INDENT_STEP);
        for (ManPart p: parts) {
            if (p instanceof ManParagraph)
                writer.startIndentedParagraph(0);
            p.write(writer);
        }
        writer.completeBlock();
    }
}
