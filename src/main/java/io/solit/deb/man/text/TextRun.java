package io.solit.deb.man.text;

import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author yaga
 * @since 05.03.18
 */
public class TextRun implements Literal {
    private final String text;

    public TextRun(String text) {
        this.text = Objects.requireNonNull(text);
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.write(text);
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextRun)) return false;
        TextRun textRun = (TextRun) o;
        return Objects.equals(text, textRun.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "TextRun{" +
                "'" + text + '\'' +
                '}';
    }
}
