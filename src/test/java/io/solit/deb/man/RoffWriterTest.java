package io.solit.deb.man;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yaga
 * @since 05.03.18
 */
public class RoffWriterTest {
    private static final String LOREM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut " +
            "labore et dolore magna aliqua.";
    private static final String ENTIM = "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea " +
            "commodo consequat. ";
    private static final String DUIS = "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla " +
            "pariatur.";

    private StringWriter _target;

    private RoffWriter roff() {
        return new RoffWriter(_target = new StringWriter());
    }

    private Scanner readResult() {
        return new Scanner(_target.toString());
    }

    @Test
    public void testSimpleSentence() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testSimpleSentenceWithLeadingDot() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write("." + LOREM);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        String s = scanner.nextLine();
        assertTrue(s.endsWith(LOREM), "Unexpected prefix " + s);
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testNewLine() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.write("\n");
            roff.write(ENTIM);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testNewLineWithLeadingDot() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.write("\n");
            roff.write('.');
            roff.write(ENTIM);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        String s = scanner.nextLine();
        assertTrue(s.endsWith(ENTIM), "Unexpected prefix " + s);
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testSentenceWithSpecialChars() throws IOException {
        try (RoffWriter roff = roff()) {
            char[] chars = {
                    '\u0024', '\u20ac', '\u00a3', '\'',     '\u00b7', '\u00a9', '\u2019', '\u00a2', '\u2021', '\u00b0', '\u2020', '\u0022',
                    '\u2014', '\u2013', '\u2010', '\u201c', '\u2018', '\u00ae', '\u201d', '\\',     '\u00a7', '\u2122', '\u005f', '\u2261',
                    '\u2265', '\u2264', '\u2260', '\u2192', '\u2190', '\u00b1'
            };
            StringBuilder src = new StringBuilder();
            for (char c : chars)
                src.append(c).append(LOREM);
            roff.append(src);
        }
        StringBuilder dst = new StringBuilder();
        String[] replacements = {
                "(Do", "(Eu", "(Po", "(aq", "(bu", "(co", "(cq", "(ct", "(dd", "(de", "(dg", "(dq", "(em", "(en", "(hy", "(lq", "(oq",
                "(rg", "(rq", "(rs", "(sc", "(tm", "(ul", "(==", "(>=", "(<=", "(!=", "(->", "(<-", "(+-"
        };
        for(String d: replacements)
            dst.append("\\").append(d).append(LOREM);
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(dst.toString(), scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testStructureLine() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.writeStructureLine();
            roff.write(ENTIM);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testCommentLine() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.writeCommentLine(ENTIM);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".\\\" " + ENTIM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testIndentedParagraph() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startTaggedParagraph(4);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".TP 4", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testIndentedParagraphWithIndent() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startTaggedParagraph(10);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".TP 10", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testIndentedHangingParagraph() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startIndentedParagraph(4);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".IP \"\" 4", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testIndentedHangingParagraphWithIndent() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startIndentedParagraph(10);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".IP \"\" 10", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testIndentedHangingParagraphWithTag() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startIndentedParagraph("foo", 10);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".IP \"foo\" 10", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testParagraph() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startParagraph();
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".P", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testLineBreak() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.breakLine();
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".br", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testNewLineBreak() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.write('\n');
            roff.breakLine();
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".br", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testHeader() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.writeHeader(ENTIM);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".SH \"" + ENTIM + "\"", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testSubHeader() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.writeSubHeader(ENTIM);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".SS \"" + ENTIM + "\"", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testHyphenation() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.disableHyphenation();
            roff.write(ENTIM);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".nh", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testFilling() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.disableFilling();
            roff.write(ENTIM);
            roff.enableFilling();
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".nf", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".fi", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testBlock() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.openBlock();
            roff.write(ENTIM);
            roff.completeBlock();
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".RS", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".RE", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }


    @Test
    public void testURL() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startURL("https://example.com");
            roff.write(ENTIM);
            roff.completeURL("!");
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".UR \"https://example.com\"", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".UE \"!\"", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testURLNoTrailer() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.startURL("https://example.com");
            roff.write(ENTIM);
            roff.completeURL();
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".UR \"https://example.com\"", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(ENTIM, scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(".UE", scanner.nextLine());
        assertTrue(scanner.hasNextLine());
        assertEquals(DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testBold() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(true, false, false);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\fB" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testItalic() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(false, true, false);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\fI" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testBoldItalic() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(true, true, false);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\f(BI" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testConstantWidth() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(false, false, true);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\f(CR" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testConstantWidthItalic() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(false, true, true);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\f(CI" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testConstantWidthBold() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(true, false, true);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\f(CB" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testConstantWidthBoldItalic() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(true, true, true);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\f[CBI]" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testRoman() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.write(LOREM);
            roff.switchFont(false, false, false);
            roff.write(DUIS);
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(LOREM + "\\fR" + DUIS, scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }

    @Test
    public void testManHeader() throws IOException {
        try (RoffWriter roff = roff()) {
            roff.writeManHeader("F-o-o", 1, LocalDate.of(2010, 1, 1), "bar", "qux");
        }
        Scanner scanner = readResult();
        assertTrue(scanner.hasNextLine());
        assertEquals(".TH \"F\\-O\\-O\" \"1\" \"2010\\-01\\-01\" \"bar\" \"qux\"", scanner.nextLine());
        assertFalse(scanner.hasNextLine());
    }


}