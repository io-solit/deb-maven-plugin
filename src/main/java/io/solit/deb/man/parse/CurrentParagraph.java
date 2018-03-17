package io.solit.deb.man.parse;

import io.solit.deb.man.block.ManParagraph;
import io.solit.deb.man.list.DefinitionList.DefinitionItem;
import io.solit.deb.man.list.ListElement;
import io.solit.deb.man.text.FontSwitch;
import io.solit.deb.man.text.Hyperlink;
import io.solit.deb.man.text.LineBreak;
import io.solit.deb.man.text.Literal;
import io.solit.deb.man.text.TextRun;

import java.util.function.IntConsumer;

/**
 * @author yaga
 * @since 3/11/18
 */
class CurrentParagraph {
    private ManParagraph term;
    private ManParagraph paragraph;
    private StringBuilder builder;
    private int italic, bold, monospace;
    private boolean pendingFont;
    private Hyperlink hyperlink;
    private boolean expectTerm;

    public CurrentParagraph(boolean expectTerm) {
        this.paragraph = new ManParagraph();
        this.expectTerm = expectTerm;
    }

    private void addLiteral(Literal literal) {
        if (hyperlink != null)
            hyperlink.getText().add(literal);
        else
            paragraph.getParts().add(literal);
    }

    private void flush() {
        if (builder != null) {
            addLiteral(new TextRun(builder.toString()));
            builder = null;
        }
    }

    private void flushFont() {
        if (pendingFont) {
            addLiteral(new FontSwitch(this.bold > 0, this.italic > 0, this.monospace > 0));
            pendingFont = false;
        }
    }

    private boolean checkTerm() {
        if (expectTerm && builder != null && builder.length() > 0 && builder.charAt(builder.length() - 1) == ':') {
            expectTerm = false;
            flush();
            term = paragraph;
            paragraph = new ManParagraph();
            return true;
        } else
            return false;
    }

    public void appendText(String text) {
        if (text == null || text.isEmpty())
            return;
        if (builder != null) {
            builder.append(text);
        } else {
            flushFont();
            builder = new StringBuilder(text);
        }
    }

    public void monospace(boolean monospace) {
        switchFont(this.monospace, monospace, i -> this.monospace = i);
    }

    public void bold(boolean bold) {
        switchFont(this.bold, bold, i -> this.bold = i);
    }

    public void italic(boolean italic) {
        switchFont(this.italic, italic, i -> this.italic = i);
    }

    private void switchFont(int value, boolean direction, IntConsumer assignment) {
        int newVal = Math.max(0, value + (direction ? 1 : -1));
        if (newVal != value)
            assignment.accept(newVal);
        if (newVal > 0 ^ value > 0) {
            flush();
            pendingFont = true;
        }
    }

    public void beginLink(String destination) {
        if (hyperlink != null)
            throw new ManParseException("Nesting hyperlinks is not allowed");
        flush();
        hyperlink = new Hyperlink(destination);
        paragraph.getParts().add(hyperlink);
    }

    public void completeLink() {
        flush();
        flushFont();
        hyperlink = null;
    }

    public void breakLine() {
        flush();
        flushFont();
        expectTerm = false; // Only first line counts
        addLiteral(new LineBreak());
    }

    public void feedLine() {
        if (!checkTerm()) {
            expectTerm = false; // Only first line counts
            appendText(" ");
        }
    }

    public ManParagraph returnParagraph() {
        checkTerm();
        flush();
        flushFont();
        if (term != null)
            throw new ManParseException("Paragraph is separated into term and definition");
        else
            return paragraph;
    }

    public DefinitionItem returnDefinitionItem() {
        checkTerm();
        flush();
        flushFont();
        if (term != null)
            return new DefinitionItem(term, paragraph.getParts().isEmpty() ? new ListElement() : new ListElement(paragraph));
        else
            return null;
    }
}
