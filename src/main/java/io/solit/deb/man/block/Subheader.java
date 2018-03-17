package io.solit.deb.man.block;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author yaga
 * @since 05.03.18
 */
public class Subheader implements ManPart {
    private final String text;

    public Subheader(String text) {
        this.text = Objects.requireNonNull(text, "Text should not be null");
    }

    public String getText() {
        return text;
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.writeSubHeader(text);
    }
}
