package io.solit.deb.man.block;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author yaga
 * @since 05.03.18
 */
public class Example implements ManPart {
    private final String text;

    public Example(String text) {
        this.text = Objects.requireNonNull(text);
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.writeStructureLine();
        writer.startIndentedParagraph(INDENT_STEP);
        writer.openBlock();
        writer.disableFilling();
        writer.switchFont(true, false, true);
        writer.write(text);
        writer.switchFont(false, false, false);
        writer.enableFilling();
        writer.completeBlock();
    }

    public String getText() {
        return text;
    }
}
