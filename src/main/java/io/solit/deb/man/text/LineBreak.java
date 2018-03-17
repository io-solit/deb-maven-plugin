package io.solit.deb.man.text;

import io.solit.deb.man.RoffWriter;

import java.io.IOException;

/**
 * @author yaga
 * @since 06.03.18
 */
public class LineBreak implements Literal {
    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.breakLine();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof LineBreak;
    }

    @Override
    public String toString() {
        return "LineBreak{}";
    }
}
