package io.solit.deb.man.parse;

import io.solit.deb.man.ManPage;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

/**
 * @author yaga
 * @since 3/11/18
 */
public class MarkdownParser {
    private String source;
    private String manual;
    private Consumer<String> defaultWarningHandler = w -> {};

    public MarkdownParser setSource(String source) {
        this.source = source;
        return this;
    }

    public MarkdownParser setDefaultWarningHandler(Consumer<String> defaultWarningHandler) {
        this.defaultWarningHandler = defaultWarningHandler;
        return this;
    }

    public MarkdownParser setManual(String manual) {
        this.manual = manual;
        return this;
    }

    public ManPage parse(Reader reader) throws IOException {
        return new MarkdownParserWorker(this.source, manual, null, -1, null, defaultWarningHandler).parse(reader);
    }

    public ManPage parse(
            Reader reader, String defaultName, int defaultSection, String defaultDescription
    ) throws IOException {
        return new MarkdownParserWorker(this.source, manual, defaultName, defaultSection, defaultDescription, defaultWarningHandler)
                .parse(reader);
    }

    public ManPage parse(Reader reader, Consumer<String> warningHandler) throws IOException {
        return new MarkdownParserWorker(this.source, manual, null, -1, null, warningHandler).parse(reader);
    }

    public ManPage parse(
            Reader reader, Consumer<String> warningHandler, String defaultName, int defaultSection, String defaultDescription
    ) throws IOException {
        return new MarkdownParserWorker(this.source, manual, defaultName, defaultSection, defaultDescription, warningHandler)
                .parse(reader);
    }
}
