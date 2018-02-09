package io.solit.deb;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author yaga
 * @since 23.01.18
 */
public class ControlFileWriter implements AutoCloseable {
    private static final String COMMA_ITEMS_SEPARATOR = ", ";
    private static final String WS_ITEMS_SEPARATOR = " ";
    private static final String VALUE_SEPARATOR = ": ";
    private static final char FIELD_SEPARATOR = '\n';
    private static final String CONTINUATION_SEPARATOR = FIELD_SEPARATOR + " ";
    private static final int MAX_LINE_LENGTH = 80;
    private final Writer writer;

    public ControlFileWriter(Writer writer) {
        this.writer = Objects.requireNonNull(writer, "Writer should not be null");
    }

    public void nextParagraph() throws IOException {
        writer.write("\n");
    }

    public void writeSingleLineField(String field, String value) throws IOException {
        writer.write(field);
        writer.write(VALUE_SEPARATOR);
        writer.write(value);
        writer.write(FIELD_SEPARATOR);
    }

    public void writeSingleLineList(String field, Iterable<String> values) throws IOException {
        writer.write(field);
        String pref = VALUE_SEPARATOR;
        for (String val: values) {
            writer.write(pref);
            writer.write(val);
            pref = COMMA_ITEMS_SEPARATOR;
        }
        writer.write(FIELD_SEPARATOR);
    }

    public void writeMultiLineList(String field, Iterable<String> values) throws IOException {
        writer.write(field);
        writer.write(VALUE_SEPARATOR);
        int length = field.length() + VALUE_SEPARATOR.length();
        String pref = "";
        for (String val : values) {
            if (length + pref.length() + val.length() < MAX_LINE_LENGTH) {
                writer.write(pref);
                writer.write(val);
                length += pref.length() + val.length();
            } else {
                writer.write(CONTINUATION_SEPARATOR);
                writer.write(val);
                length = val.length() + 1; // one for continuation
            }
            pref = WS_ITEMS_SEPARATOR;
        }
        writer.write(FIELD_SEPARATOR);
    }

    public void writeLineBasedList(String field, Iterable<String> values) throws IOException {
        writer.write(field);
        writer.write(VALUE_SEPARATOR);
        for (Iterator<String> it = values.iterator(); it.hasNext();) {
            String item = it.next();
            writer.write(item);
            if (it.hasNext())
                writer.write(CONTINUATION_SEPARATOR);
        }
        writer.write(FIELD_SEPARATOR);
    }


    public void writeFormattedField(String field, String synopsis, String content) throws IOException {
        writer.write(field);
        writer.write(VALUE_SEPARATOR);
        if (synopsis != null) {
            writer.write(synopsis);
            writer.write(FIELD_SEPARATOR);
        }
        if (content != null) {
            char[] chars = content.toCharArray();
            int last = 0;
            boolean nonSpaces = false;
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] == FIELD_SEPARATOR) {
                    if (i >= last) {
                        writer.write(' ');
                        if (nonSpaces)
                            writer.write(chars, last, i - last + 1);
                        else {
                            writer.write('.');
                            writer.write(FIELD_SEPARATOR);
                        }
                    }
                    last = i + 1;
                    nonSpaces = false;
                } else if (!nonSpaces && !Character.isWhitespace(chars[i]))
                    nonSpaces = true;
            }
            if (last < chars.length && nonSpaces) {
                writer.write(' ');
                writer.write(chars, last, chars.length - last);
            }
            writer.write(FIELD_SEPARATOR);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
