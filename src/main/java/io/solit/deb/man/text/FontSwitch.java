package io.solit.deb.man.text;

import io.solit.deb.man.RoffWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author yaga
 * @since 3/11/18
 */
public class FontSwitch implements Literal {
    private final boolean bold;
    private final boolean monospace;
    private final boolean italic;

    public FontSwitch(boolean bold, boolean italic, boolean monospace) {
        this.bold = bold;
        this.monospace = monospace;
        this.italic = italic;
    }

    @Override
    public void write(RoffWriter writer) throws IOException {
        writer.switchFont(bold, italic, monospace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FontSwitch)) return false;
        FontSwitch that = (FontSwitch) o;
        return bold == that.bold &&
                monospace == that.monospace &&
                italic == that.italic;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bold, monospace, italic);
    }

    @Override
    public String toString() {
        return "FontSwitch{" +
                bold +
                "," + italic +
                "," + monospace +
                '}';
    }
}
