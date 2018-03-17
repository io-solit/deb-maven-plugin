package io.solit.deb.man.block;

import io.solit.deb.man.ManPart;
import io.solit.deb.man.RoffWriter;
import io.solit.deb.man.text.TextPart;
import io.solit.deb.man.text.TextRun;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author yaga
 * @since 05.03.18
 */
public class ManParagraph implements ManPart {
    private final List<TextPart> parts = new ArrayList<>();

    public ManParagraph() {}

    public ManParagraph(TextPart... text) {
        parts.addAll(Arrays.asList(text));
    }

    public ManParagraph(String... text) {
        this(Arrays.stream(text).map(TextRun::new).toArray(TextPart[]::new));
    }


    public List<TextPart> getParts() {
        return parts;
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        for (TextPart part : parts) {
            part.write(writer);
        }
    }
}
