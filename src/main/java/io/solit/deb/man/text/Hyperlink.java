package io.solit.deb.man.text;

import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yaga
 * @since 3/7/18
 */
public class Hyperlink implements TextPart {
    private final List<Literal> text = new ArrayList<>();
    private final String destination;

    public Hyperlink(String destination) {
        this.destination = destination;
    }

    public Hyperlink(String destination, String text) {
        this.destination = destination;
        this.text.add(new TextRun(text));
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.startURL(destination);
        for (Literal t: text)
            t.write(writer);
        writer.completeURL();
    }

    public List<Literal> getText() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hyperlink)) return false;
        Hyperlink hyperlink = (Hyperlink) o;
        return Objects.equals(text, hyperlink.text) &&
                Objects.equals(destination, hyperlink.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, destination);
    }

    @Override
    public String toString() {
        return "Hyperlink{" +
                text +
                "(" + destination + ')' +
                '}';
    }
}
