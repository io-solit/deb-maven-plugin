package io.solit.deb.man;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * @author yaga
 * @since 27.02.18
 */
@SuppressWarnings("SynchronizeOnNonFinalField")
public class RoffWriter extends Writer {
    private final Writer writer;
    private boolean newLine = true;
    // Theoretically this chars may change during execution, but this is not implemented
    private char breakingRequestChar = '.';
    private char nonBreakingRequestChar = '\'';
    private char escapeChar = '\\';

    public RoffWriter(Writer destination) {
        super(destination);
        this.writer = destination;
    }

    private void startRequest() throws IOException {
        newLine();
        writer.write(breakingRequestChar);
        newLine = false;
    }

    private void newLine() throws IOException {
        if (!newLine) {
            writer.write('\n');
            newLine = true;
        }
    }

    private void writeMacroArgument(CharSequence sequence) throws IOException {
        writer.write(' ');
        writer.write('"');
        for (int i = 0, l = sequence.length(); i < l; i++) {
            char c = sequence.charAt(i);
            if (c == '\n')
                writer.write("\\n");
            else if (c == '"')
                writer.write("\\\"");
            else if (!writeSpecial(c))
                writer.write(c);
        }
        writer.write('"');
    }

    public void writeStructureLine() throws IOException {
        synchronized (lock) {
            startRequest();
            newLine();
        }
    }

    public void writeManHeader(String title, int manSection, LocalDate date, String source, String manual) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("TH");
            writeMacroArgument(title.toUpperCase());
            writeMacroArgument(Integer.toString(manSection));
            writeMacroArgument(date.format(DateTimeFormatter.ISO_DATE));
            writeMacroArgument(source);
            writeMacroArgument(manual);
            newLine();
        }
    }

    public void writeCommentLine(String comment) throws IOException {
        synchronized (lock) {
            if (comment.indexOf('\n') >= 0)
                throw new IllegalArgumentException("Comment line should not contain line breaks");
            startRequest();
            writer.write("\\\" ");
            writer.write(comment);
            newLine();
        }
    }

    public void writeHeader(String header) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("SH");
            writeMacroArgument(header);
            newLine();
        }
    }

    public void writeSubHeader(String subHeader) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("SS");
            writeMacroArgument(subHeader);
            newLine();
        }
    }

    public void startParagraph() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("P");
            newLine();
        }
    }

    public void startTaggedParagraph(int indentation) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("TP ");
            writer.write(Integer.toString(indentation));
            newLine();
        }
    }

    public void startIndentedParagraph(String hangingTag, int indentation) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("IP");
            writeMacroArgument(hangingTag == null ? "" : hangingTag);
            writer.write(" ");
            writer.write(Integer.toString(indentation));
            newLine();
        }
    }

    public void startIndentedParagraph(int indentation) throws IOException {
        startIndentedParagraph(null, indentation);
    }

    public void startURL(String url) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("UR");
            writeMacroArgument(url);
            newLine();
        }
    }

    public void completeURL(String trailer) throws IOException{
        synchronized (lock) {
            startRequest();
            writer.write("UE");
            if (trailer != null)
                writeMacroArgument(trailer);
            newLine();
        }
    }

    public void completeURL() throws IOException{
        completeURL(null);
    }

    public void openBlock() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("RS");
            newLine();
        }
    }

    public void openBlock(int indentation) throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("RS ");
            writer.write(Integer.toString(indentation));
            newLine();
        }
    }

    public void completeBlock() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("RE");
            newLine();
        }
    }

    public void breakLine() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("br");
            newLine();
        }
    }

    public void disableFilling() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("nf");
            newLine();
        }
    }

    public void enableFilling() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("fi");
            newLine();
        }
    }

    public void disableHyphenation() throws IOException {
        synchronized (lock) {
            startRequest();
            writer.write("nh");
            newLine();
        }
    }

    public void switchFont(boolean bold, boolean italic, boolean monospace) throws IOException {
        synchronized (lock) {
            char[] c = new char[3];
            int i = 0;
            if (monospace)
                c[i++] = 'C';
            if (bold)
                c[i++] = 'B';
            if (italic)
                c[i++] = 'I';
            if (!bold && !italic)
                c[i++] = 'R';
            writer.write("\\f");
            switch (i) {
                case 1:
                    writer.write(c, 0, i);
                    break;
                case 2:
                    writer.write('(');
                    writer.write(c, 0, i);
                    break;
                case 3:
                    writer.write('[');
                    writer.write(c, 0, i);
                    writer.write(']');
            }
            newLine = false;
        }
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        append(new ArraySequence(Objects.requireNonNull(cbuf), off, len));
    }

    @Override
    public void write(char[] cbuf) throws IOException {
        append(new ArraySequence(cbuf, 0, cbuf.length));
    }

    @Override
    public void write(String str) throws IOException {
        append(Objects.requireNonNull(str), 0, str.length());
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        append(Objects.requireNonNull(str), off, off + len);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        if (csq == null)
            append("null", 0, 4);
        else
            append(csq, 0, csq.length());
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IOException {
        synchronized (lock) {
            if (csq == null)
                csq = "null";
            if (start < 0 || end > csq.length() || start > end)
                throw new IndexOutOfBoundsException(String.format("Illegal stat or end '%d-%d' of [%d, %d)", start, end, 0, csq.length()));
            for (int i = start; i < end; i++) {
                char c = csq.charAt(i);
                if (c == '\n' && !newLine) {
                    writer.write(c);
                    newLine = true;
                    continue;
                }
                if ((c == breakingRequestChar || c == nonBreakingRequestChar) && newLine) {
                    writer.write("\\&");
                    writer.write(c);
                } else if (!writeSpecial(c)) {
                    writer.write(c);
                }
                newLine = false;
            }
        }
        return this;
    }

    public void flush() throws IOException {
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    private boolean writeSpecial(char c) throws IOException {
        switch (c) {
            case '\u0024': writer.write(escapeChar); writer.write("(Do"); break; // Dollar
            case '\u20ac': writer.write(escapeChar); writer.write("(Eu"); break; // Euro
            case '\u00a3': writer.write(escapeChar); writer.write("(Po"); break; // British pound sterling
            case '\'':     writer.write(escapeChar); writer.write("(aq"); break; // Apostrophe quote
            case '\u00b7': writer.write(escapeChar); writer.write("(bu"); break; // Bullet sign
            case '\u00a9': writer.write(escapeChar); writer.write("(co"); break; // Copyright
            case '\u2019': writer.write(escapeChar); writer.write("(cq"); break; // Single closing quote (right)
            case '\u00a2': writer.write(escapeChar); writer.write("(ct"); break; // Cent
            case '\u2021': writer.write(escapeChar); writer.write("(dd"); break; // Double dagger
            case '\u00b0': writer.write(escapeChar); writer.write("(de"); break; // Degree
            case '\u2020': writer.write(escapeChar); writer.write("(dg"); break; // Dagger
            case '\u0022': writer.write(escapeChar); writer.write("(dq"); break; // Double quote (ASCII 34)
            case '\u2014': writer.write(escapeChar); writer.write("(em"); break; // Em-dash
            case '\u2013': writer.write(escapeChar); writer.write("(en"); break; // En-dash
            case '\u2010': writer.write(escapeChar); writer.write("(hy"); break; // Hyphen
            case '\u201c': writer.write(escapeChar); writer.write("(lq"); break; // Double quote left
            case '\u2018': writer.write(escapeChar); writer.write("(oq"); break; // Single opening quote (left)
            case '\u00ae': writer.write(escapeChar); writer.write("(rg"); break; // Registered sign
            case '\u201d': writer.write(escapeChar); writer.write("(rq"); break; // Double quote right
            case '\\':     writer.write(escapeChar); writer.write("(rs"); break; // Printable backslash character
            case '\u00a7': writer.write(escapeChar); writer.write("(sc"); break; // Section sign
            case '\u2122': writer.write(escapeChar); writer.write("(tm"); break; // Trademark symbol
            case '\u005f': writer.write(escapeChar); writer.write("(ul"); break; // Underline character
            case '\u2261': writer.write(escapeChar); writer.write("(=="); break; // Identical
            case '\u2265': writer.write(escapeChar); writer.write("(>="); break; // Larger or equal
            case '\u2264': writer.write(escapeChar); writer.write("(<="); break; // Less or equal
            case '\u2260': writer.write(escapeChar); writer.write("(!="); break; // Not equal
            case '\u2192': writer.write(escapeChar); writer.write("(->"); break; // Right arrow
            case '\u2190': writer.write(escapeChar); writer.write("(<-"); break; // Left arrow
            case '\u00b1': writer.write(escapeChar); writer.write("(+-"); break; // Plus-minus sign
            case '\u002d': writer.write(escapeChar); writer.write("-");   break; // Minus sign
            default:
                return false;
        }
        return true;
    }

    private class ArraySequence implements CharSequence {
        private final char[] chars;
        private final int len;
        private final int offset;

        private ArraySequence(char[] chars, int len, int offset) {
            this.chars = chars;
            this.len = len;
            this.offset = offset;
        }

        @Override
        public int length() {
            return len;
        }

        @Override
        public char charAt(int index) {
            if (index < 0 || index >= len)
                throw new IndexOutOfBoundsException(index + " is not in [0," + len + ")");
            return chars[index + offset];
        }

        @Override
        public String toString() {
            return new String(chars, offset, len);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            if (start < 0) {
                throw new IndexOutOfBoundsException(start + " is not in [0," + len + ")");
            }
            if (end > len) {
                throw new IndexOutOfBoundsException(end + " is not in [0," + len + ")");
            }
            int subLen = end - start;
            if (subLen < 0)
                throw new IndexOutOfBoundsException("End is before start");
            return new ArraySequence(chars, offset + start, subLen);
        }
    }

}
