package io.solit.deb.man.parse;

import io.solit.deb.man.ManPage;
import io.solit.deb.man.ManPart;
import io.solit.deb.man.Section;
import io.solit.deb.man.block.Example;
import io.solit.deb.man.block.ManParagraph;
import io.solit.deb.man.block.Quote;
import io.solit.deb.man.block.Subheader;
import io.solit.deb.man.list.DefinitionList;
import io.solit.deb.man.list.NumberedList;
import io.solit.deb.man.list.UnorderedList;
import io.solit.deb.man.text.FontSwitch;
import io.solit.deb.man.text.Hyperlink;
import io.solit.deb.man.text.LineBreak;
import io.solit.deb.man.text.Literal;
import io.solit.deb.man.text.TextPart;
import io.solit.deb.man.text.TextRun;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yaga
 * @since 3/7/18
 */
public class MarkdownParserWorkerTest {

    @Test
    public void testMinimalMarkdown() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "============="
                ))
        );
        assertEquals(5, page.getManSection());
        assertEquals("foo", page.getTitle());
        assertEquals("foo", page.getName());
        assertEquals("bar", page.getShortDescription());
        assertEquals("Qux", page.getSource());
        assertEquals("Xyzzy", page.getManual());
        assertTrue(page.getAdditionalSections().isEmpty());
    }

    @Test
    public void testFallbackToDefault() throws IOException {
        List<String> warnings = new ArrayList<>();
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", "foo", 5, "bar", warnings::add).parse(
                new StringReader(String.join("\n"))
        );
        assertEquals(5, page.getManSection());
        assertEquals("foo", page.getTitle());
        assertEquals("foo", page.getName());
        assertEquals("bar", page.getShortDescription());
        assertEquals("Qux", page.getSource());
        assertEquals("Xyzzy", page.getManual());
        assertTrue(page.getAdditionalSections().isEmpty());
        assertFalse(warnings.isEmpty());
    }

    @Test
    public void testIgnoredText() throws IOException {
        List<String> warnings = new ArrayList<>();
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, warnings::add).parse(
                new StringReader(String.join("\n",
                        "Some text here",
                        "## Ignored header",
                        "foo(5) -- bar",
                        "============="
                ))
        );
        assertEquals(5, page.getManSection());
        assertEquals("foo", page.getTitle());
        assertEquals("foo", page.getName());
        assertEquals("bar", page.getShortDescription());
        assertEquals("Qux", page.getSource());
        assertEquals("Xyzzy", page.getManual());
        assertTrue(page.getAdditionalSections().isEmpty());
        assertFalse(warnings.isEmpty());
    }

    @Test
    public void testMissingHeader() {
        assertThrows(ManParseException.class,
                () -> new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                        new StringReader(String.join("\n"))
                )
        );
    }

    @Test
    public void testMissingHeaderWithText() {
        assertThrows(ManParseException.class,
                () -> new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, s -> {}).parse(
                        new StringReader(String.join("\n",
                                "Some text",
                                "Some other text"
                        ))
                )
        );
    }

    @Test
    public void testWrongHeader() {
        assertThrows(ManParseException.class,
                () -> new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, s -> {}).parse(
                        new StringReader(String.join("\n",
                                "Some text",
                                "========="
                        ))
                )
        );
    }

    @Test
    public void testSection() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# *_Lorem_ ipsum*",
                        "dolor sit  ",
                        "amet"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("Lorem ipsum", s.getName());
        assertEquals(1, s.getParts().size(), "Unexpected parts");
        checkParagraphText(s.getParts().iterator().next(), t("dolor sit"), b(), t("amet"));
    }

    @Test
    public void testHangingSection() throws IOException {
        List<String> warnings = new ArrayList<>();
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, warnings::add).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "Lorem ipsum",
                        "# dolor sit"
                ))
        );
        assertEquals(2, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().get(0);
        assertNotNull(s.getName());
        assertEquals(1, s.getParts().size(), "Unexpected parts");
        checkParagraphText(s.getParts().iterator().next(), t("Lorem ipsum"));
        s = page.getAdditionalSections().get(1);
        assertEquals("dolor sit", s.getName());
        assertEquals(0, s.getParts().size(), "Unexpected parts");
        assertFalse(warnings.isEmpty());
    }

    @Test
    public void testSectionWithSubheader() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "Lorem",
                        "ipsum   ",
                        "dolor",
                        "-----",
                        "### sit `amet` [consectetur](http://example.com) <adipiscing>"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("Lorem ipsum dolor", s.getName());
        assertEquals(1, s.getParts().size(), "Unexpected parts");
        assertTrue(s.getParts().iterator().next() instanceof Subheader, "Part is not subheader");
        assertEquals("sit amet consectetur <adipiscing>", Subheader.class.cast(s.getParts().iterator().next()).getText());
    }

    @Test
    public void testSectionWithHighlights() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# *_Lorem_ ipsum*",
                        "<dolor> _sit_ *amet*",
                        "`consectetur` _**adipiscing**_  ",
                        "__elit__",
                        "",
                        "Sed"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("Lorem ipsum", s.getName());
        assertEquals(2, s.getParts().size(), "Unexpected parts");
        checkParagraphText(s.getParts().get(0),
                f(false, true, false), t("dolor"), f(false, false, false), t(" "),
                f(false, true, false), t("sit"), f(false, false, false), t(" "),
                f(true, false, false), t("amet"), f(false, false, false), t(" "),
                f(true, false, true), t("consectetur"), f(false, false, false), t(" "),
                f(true, true, false), t("adipiscing"), f(false, false, false), b(),
                f(false, true, false), t("elit"), f(false, false, false)
        );
        checkParagraphText(s.getParts().get(1), t("Sed"));

    }

    @Test
    public void testSectionWithNestedList() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "Lorem ipsum dolor sit amet",
                        "* consectetur ",
                        "* adipiscing ",
                        "    1. elit",
                        "    2. sed",
                        "* do"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("baz", s.getName());
        assertEquals(2, s.getParts().size(), "Unexpected parts count");
        checkParagraphText(s.getParts().get(0), t("Lorem ipsum dolor sit amet"));
        assertTrue(s.getParts().get(1) instanceof UnorderedList, "Content is not a paragraph");
        UnorderedList outer = (UnorderedList) s.getParts().get(1);
        assertEquals('\u00b7', outer.getMarker());
        assertEquals(3, outer.getItems().size());
        assertEquals(1, outer.getItems().get(0).getParts().size(), "Unexpected size of a first item");
        checkParagraphText(outer.getItems().get(0).getParts().get(0), t("consectetur"));
        assertEquals(2, outer.getItems().get(1).getParts().size(), "Unexpected size of a second item");
        checkParagraphText(outer.getItems().get(1).getParts().get(0), t("adipiscing"));
        assertTrue(outer.getItems().get(1).getParts().get(1) instanceof NumberedList, "Inner element is not a list");
        NumberedList inner = (NumberedList) outer.getItems().get(1).getParts().get(1);
        assertEquals(2, inner.getItems().size());
        assertEquals(1, inner.getItems().get(0).getParts().size());
        checkParagraphText(inner.getItems().get(0).getParts().get(0), t("elit"));
        assertEquals(1, inner.getItems().get(1).getParts().size());
        checkParagraphText(inner.getItems().get(1).getParts().get(0), t("sed"));
        assertEquals(1, outer.getItems().get(2).getParts().size(), "Unexpected size of a third item");
        checkParagraphText(outer.getItems().get(2).getParts().get(0), t("do"));
    }

    @Test
    public void testSectionWithQuotedParagraph() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "Lorem ipsum dolor sit amet",
                        "> consectetur",
                        "> adipiscing",
                        "> 1. elit",
                        "> 2. sed",
                        ">",
                        "> do"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("baz", s.getName());
        assertEquals(2, s.getParts().size(), "Unexpected parts count");
        checkParagraphText(s.getParts().get(0), t("Lorem ipsum dolor sit amet"));
        assertTrue(s.getParts().get(1) instanceof Quote, "Content is not a quote");
        Quote outer = (Quote) s.getParts().get(1);
        assertEquals(3, outer.getParts().size());
        checkParagraphText(outer.getParts().get(0), t("consectetur adipiscing"));
        assertTrue(outer.getParts().get(1) instanceof NumberedList, "Inner element is not a list");
        NumberedList inner = (NumberedList) outer.getParts().get(1);
        assertEquals(2, inner.getItems().size());
        assertEquals(1, inner.getItems().get(0).getParts().size());
        checkParagraphText(inner.getItems().get(0).getParts().get(0), t("elit"));
        assertEquals(1, inner.getItems().get(1).getParts().size());
        checkParagraphText(inner.getItems().get(1).getParts().get(0), t("sed"));
        checkParagraphText(outer.getParts().get(2), t("do"));
    }

    @Test
    public void testHyperlinks() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "## [](http://example.com \"baz\")",
                        "### [](http://example.com)",
                        "Lorem ipsum [dolor _sit_](amet)",
                        "",
                        "![consectetur](adipiscing)"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("baz", s.getName());
        assertEquals(3, s.getParts().size(), "Unexpected parts count");
        assertTrue(s.getParts().get(0) instanceof Subheader, "Part is not sub header");
        assertEquals("http://example.com", Subheader.class.cast(s.getParts().get(0)).getText());
        checkParagraphText(s.getParts().get(1),
                t("Lorem ipsum "), h("amet", t("dolor "), f(false, true, false), t("sit"), f(false, false, false))
        );
        checkParagraphText(s.getParts().get(2),
                h("adipiscing", t("consectetur"))
        );
    }

    @Test
    public void testExamples() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "    # some offset md",
                        "    1. with list",
                        "```",
                        "# some fenced md",
                        "1. with list",
                        "```",
                        "<div>",
                        "# some html md",
                        "1. with list",
                        "</div>"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals("baz", s.getName());
        assertEquals(3, s.getParts().size(), "Unexpected parts count");
        assertTrue(s.getParts().get(0) instanceof Example);
        assertEquals("# some offset md\n1. with list\n", Example.class.cast(s.getParts().get(0)).getText());
        assertTrue(s.getParts().get(1) instanceof Example);
        assertEquals("# some fenced md\n1. with list\n", Example.class.cast(s.getParts().get(1)).getText());
        assertTrue(s.getParts().get(2) instanceof Example);
        assertEquals("<div>\n# some html md\n1. with list\n</div>", Example.class.cast(s.getParts().get(2)).getText());
    }

    @Test
    public void testDefinitionLikeParagraph() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "Lorem:",
                        "ipsum",
                        "",
                        "Dolor:",
                        ""
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals(2, s.getParts().size(), "Unexpected parts count");
        checkParagraphText(s.getParts().get(0), t("Lorem: ipsum"));
        checkParagraphText(s.getParts().get(1), t("Dolor:"));
    }

    @Test
    public void testDefinitionList() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "*  Lorem:",
                        "   ipsum",
                        "*  Dolor:",
                        "   sit"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals(1, s.getParts().size(), "Unexpected parts count");
        assertTrue(s.getParts().get(0) instanceof DefinitionList, "Part is not a definition list");
        DefinitionList dl = (DefinitionList) s.getParts().get(0);
        assertEquals(2, dl.getItems().size(), "Unexpected items count");
        checkParagraphText(dl.getItems().get(0).getTerm(), t("Lorem:"));
        assertEquals(1, dl.getItems().get(0).getDefinition().getParts().size());
        checkParagraphText(dl.getItems().get(0).getDefinition().getParts().get(0), t("ipsum"));

        checkParagraphText(dl.getItems().get(1).getTerm(), t("Dolor:"));
        assertEquals(1, dl.getItems().get(1).getDefinition().getParts().size());
        checkParagraphText(dl.getItems().get(1).getDefinition().getParts().get(0), t("sit"));
    }

    @Test
    public void testInterleavedDefinitionList() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "*  Lorem:",
                        "*  ipsum:",
                        "   dolor",
                        "*  sit"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals(3, s.getParts().size(), "Unexpected parts count");
        assertTrue(s.getParts().get(0) instanceof UnorderedList, "Part is not an unordered list");
        UnorderedList ul = (UnorderedList) s.getParts().get(0);
        assertEquals(1, ul.getItems().size(), "Unexpected items count");
        assertEquals(1, ul.getItems().get(0).getParts().size(), "Unexpected item parts count");
        checkParagraphText(ul.getItems().get(0).getParts().get(0), t("Lorem:"));

        assertTrue(s.getParts().get(1) instanceof DefinitionList, "Part is not a definition list");
        DefinitionList dl = (DefinitionList) s.getParts().get(1);
        assertEquals(1, dl.getItems().size(), "Unexpected items count");
        checkParagraphText(dl.getItems().get(0).getTerm(), t("ipsum:"));
        assertEquals(1, dl.getItems().get(0).getDefinition().getParts().size());
        checkParagraphText(dl.getItems().get(0).getDefinition().getParts().get(0), t("dolor"));

        ul = (UnorderedList) s.getParts().get(2);
        assertEquals(1, ul.getItems().size(), "Unexpected items count");
        assertEquals(1, ul.getItems().get(0).getParts().size(), "Unexpected item parts count");
        checkParagraphText(ul.getItems().get(0).getParts().get(0), t("sit"));
    }

    @Test
    public void testDecoratedDefinitionList() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "*  _Lorem_:",
                        "   *ipsum*"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertEquals(1, s.getParts().size(), "Unexpected parts count");
        assertTrue(s.getParts().get(0) instanceof DefinitionList, "Part is not a definition list");
        DefinitionList dl = (DefinitionList) s.getParts().get(0);
        assertEquals(1, dl.getItems().size(), "Unexpected items count");
        checkParagraphText(dl.getItems().get(0).getTerm(), f(false, true, false), t("Lorem"), f(false, false, false), t(":"));
        assertEquals(1, dl.getItems().get(0).getDefinition().getParts().size());
        checkParagraphText(dl.getItems().get(0).getDefinition().getParts().get(0),
                f(true, false, false), t("ipsum"), f(false, false, false));
    }

    @Test
    public void testDecoratedDefinitionWithExample() throws IOException {
        ManPage page = new MarkdownParserWorker("Qux", "Xyzzy", null, -1, null, Assertions::fail).parse(
                new StringReader(String.join("\n",
                        "foo(5) -- bar",
                        "=============",
                        "# baz",
                        "*  Lorem:",
                        "   ```",
                        "   ipsum",
                        "   ```"
                ))
        );
        assertEquals(1, page.getAdditionalSections().size());
        Section s = page.getAdditionalSections().iterator().next();
        assertTrue(s.getParts().get(0) instanceof DefinitionList, "Part is not a definition list");
        assertEquals(1, s.getParts().size(), "Unexpected parts count");
        DefinitionList dl = (DefinitionList) s.getParts().get(0);
        assertEquals(1, dl.getItems().size(), "Unexpected items count");
        checkParagraphText(dl.getItems().get(0).getTerm(), t("Lorem:"));
        assertEquals(1, dl.getItems().get(0).getDefinition().getParts().size());
        assertTrue(dl.getItems().get(0).getDefinition().getParts().get(0) instanceof Example, "Part is not example");
        Example ex = (Example) dl.getItems().get(0).getDefinition().getParts().get(0);
        assertEquals("ipsum\n", ex.getText());
    }

    private void checkParagraphText(ManPart part, TextPart... text) {
        assertTrue(part instanceof ManParagraph, "Part is not a paragraph");
        ManParagraph paragraph = (ManParagraph) part;
        int i;
        for (i = 0; i < text.length; i++) {
            if (paragraph.getParts().size() <= i)
                fail("No run for text '" + text[i] + "' at " + i);
            assertEquals(text[i], paragraph.getParts().get(i), "Part does not match '" + text[i] + "' at " + i);
        }
        if (i < paragraph.getParts().size())
            fail("Excessive text " + paragraph.getParts().subList(i, paragraph.getParts().size())
                    .stream().map(TextRun.class::cast).map(TextRun::getText).collect(Collectors.joining()));
    }

    private FontSwitch f(boolean bold, boolean italic, boolean monospace) {
        return new FontSwitch(bold, italic, monospace);
    }

    private TextRun t(String text) {
        return new TextRun(text);
    }

    private LineBreak b() {
        return new LineBreak();
    }

    private Hyperlink h(String hyperlink, Literal... text) {
        Hyperlink h = new Hyperlink(hyperlink);
        h.getText().addAll(Arrays.asList(text));
        return h;
    }
}
