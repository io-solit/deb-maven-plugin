package io.solit.deb.man;

import io.solit.deb.man.block.Example;
import io.solit.deb.man.block.ManParagraph;
import io.solit.deb.man.block.Quote;
import io.solit.deb.man.block.Subheader;
import io.solit.deb.man.list.DefinitionList;
import io.solit.deb.man.list.ListElement;
import io.solit.deb.man.list.ManList;
import io.solit.deb.man.list.NumberedList;
import io.solit.deb.man.list.UnorderedList;
import io.solit.deb.man.text.FontSwitch;
import io.solit.deb.man.text.Hyperlink;
import io.solit.deb.man.text.LineBreak;
import io.solit.deb.man.text.TextRun;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author yaga
 * @since 05.03.18
 */
public class ManPageTest {

    @Test
    public void testMinimalPage() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar"
            );
        }
    }

    @Test
    public void testEmptySection() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        page.getAdditionalSections().add(new Section("qux"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\""
            );
        }
    }

    @Test
    public void testSectionWithSubHeader() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(new Subheader("xyzzy"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".SS \"xyzzy\""
            );
        }
    }

    @Test
    public void testSectionExamplePart() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(new Example("SELECT\n\t1 + 1;\n"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".IP \"\" 4",
                    ".RS",
                    ".nf",
                    "\\f(CBSELECT",
                    "\t1 + 1;",
                    "\\fR",
                    ".fi",
                    ".RE"
            );
        }
    }

    @Test
    public void testSectionWithParagraph() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        ManParagraph p;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(p = new ManParagraph());
        p.getParts().add(new TextRun("Lorem "));
        p.getParts().add(new TextRun("ipsum"));
        p.getParts().add(new LineBreak());
        p.getParts().add(new TextRun("dolor "));
        p.getParts().add(new TextRun("sit "));
        p.getParts().add(new TextRun("amet"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".P",
                    "Lorem ipsum",
                    ".br",
                    "dolor sit amet"
            );
        }
    }

    @Test
    public void testSectionWithDecoratedParagraph() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        ManParagraph p;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(p = new ManParagraph());
        p.getParts().add(new FontSwitch(true, true, true));
        p.getParts().add(new TextRun("Lorem "));
        p.getParts().add(new FontSwitch(false, true, true));
        p.getParts().add(new TextRun("ipsum"));
        p.getParts().add(new LineBreak());
        p.getParts().add(new FontSwitch(true, false, true));
        p.getParts().add(new TextRun("dolor "));
        p.getParts().add(new FontSwitch(false, false, true));
        p.getParts().add(new TextRun("sit "));
        p.getParts().add(new FontSwitch(true, true, false));
        p.getParts().add(new TextRun("amet"));
        p.getParts().add(new FontSwitch(false, true, false));
        p.getParts().add(new LineBreak());
        p.getParts().add(new TextRun("consectetur "));
        p.getParts().add(new FontSwitch(true, false, false));
        p.getParts().add(new TextRun("adipiscing "));
        p.getParts().add(new FontSwitch(false, false, false));
        p.getParts().add(new TextRun("elit"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".P",
                    "\\f[CBI]Lorem \\f(CIipsum",
                    ".br",
                    "\\f(CBdolor \\f(CRsit \\f(BIamet\\fI",
                    ".br",
                    "consectetur \\fBadipiscing \\fRelit"
            );
        }
    }

    @Test
    public void testSectionWithHyperlink() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        ManParagraph p;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(p = new ManParagraph());
        p.getParts().add(new Hyperlink("https://example.com/ipsum", "Lorem"));
        p.getParts().add(new TextRun("dolor"));
        p.getParts().add(new Hyperlink("https://example.com/amet",  "sit"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".P",
                    ".UR \"https://example.com/ipsum\"",
                    "Lorem",
                    ".UE",
                    "dolor",
                    ".UR \"https://example.com/amet\"",
                    "sit",
                    ".UE"
            );
        }
    }

    @Test
    public void testSectionWithOrderedList() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        ManList l;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(l = new UnorderedList('\u00b7'));
        l.addItem(new ManParagraph(new TextRun("Lorem")));
        l.addItem(new ManParagraph("ipsum"));
        l.addItem(new ManParagraph("dolor"));
        l.addItem(new ManParagraph("sit"));
        l.addItem(new ManParagraph("amet"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".IP \"\\(bu\" 4",
                    "Lorem",
                    ".IP \"\\(bu\" 4",
                    "ipsum",
                    ".IP \"\\(bu\" 4",
                    "dolor",
                    ".IP \"\\(bu\" 4",
                    "sit",
                    ".IP \"\\(bu\" 4",
                    "amet"
            );
        }
    }

    @Test
    public void testOrderedList() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        ManList l;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(l = new NumberedList());
        l.addItem(new ManParagraph("Lorem"));
        l.addItem(new ManParagraph("ipsum"));
        l.addItem(new ManParagraph("dolor"));
        l.addItem(new ManParagraph("sit"));
        l.addItem(new ManParagraph("amet"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".IP \"1.\" 4",
                    "Lorem",
                    ".IP \"2.\" 4",
                    "ipsum",
                    ".IP \"3.\" 4",
                    "dolor",
                    ".IP \"4.\" 4",
                    "sit",
                    ".IP \"5.\" 4",
                    "amet"
            );
        }
    }

    @Test
    public void testDefinitionList() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        DefinitionList l;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(l = new DefinitionList());
        l.addItem(new ManParagraph("Lorem ipsum"), new ManParagraph("dolor sit amet"));
        l.addItem(new ManParagraph(new TextRun("consectetur "), new TextRun("adipiscing elit")),
                new ManParagraph("sed do eiusmod tempor incididunt ut"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".TP 4",
                    "Lorem ipsum",
                    "dolor sit amet",
                    ".TP 4",
                    "consectetur adipiscing elit",
                    "sed do eiusmod tempor incididunt ut"
            );
        }
    }

    @Test
    public void testNestedList() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        ManList l;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(l = new NumberedList());
        ManList ul = new UnorderedList('\u2014');
        ListElement i = new ListElement();
        l.getItems().add(i);
        i.getParts().add(new ManParagraph("xyzzy"));
        i.getParts().add(ul);
        ul.addItem(new ManParagraph("Lorem"));
        ul.addItem(new ManParagraph("ipsum"));
        ul.addItem(new ManParagraph("dolor"));
        ul.addItem(new ManParagraph("sit"));
        ul.addItem(new ManParagraph("amet"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".IP \"1.\" 4",
                    "xyzzy",
                    ".RS",
                    ".IP \"\\(em\" 4",
                    "Lorem",
                    ".IP \"\\(em\" 4",
                    "ipsum",
                    ".IP \"\\(em\" 4",
                    "dolor",
                    ".IP \"\\(em\" 4",
                    "sit",
                    ".IP \"\\(em\" 4",
                    "amet",
                    ".RE"
            );
        }
    }

    @Test
    public void testSectionWithQuotedParagraph() throws IOException {
        ManPage page = new ManPage("foo", "bar", 1);
        page.setDate(LocalDate.of(2010, 1, 1));
        Section s;
        Quote q;
        page.getAdditionalSections().add(s = new Section("qux"));
        s.getParts().add(q = new Quote());
        ManParagraph p;
        q.getParts().add(p = new ManParagraph());
        p.getParts().add(new TextRun("Lorem "));
        p.getParts().add(new TextRun("ipsum "));
        p.getParts().add(new TextRun("dolor "));
        p.getParts().add(new TextRun("sit "));
        p.getParts().add(new TextRun("amet"));
        try (StringWriter w = new StringWriter()) {
            page.write(w);
            checkLines(w.toString(),
                    ".TH \"FOO\" \"1\" \"2010\\-01\\-01\" \"Linux\" \"Manual\"",
                    ".SH \"NAME\"",
                    "foo \\- bar",
                    ".SH \"QUX\"",
                    ".RS 4",
                    ".IP \"\" 0",
                    "Lorem ipsum dolor sit amet",
                    ".RE"
            );
        }
    }

    private void checkLines(String result, String... expectedLines) {
        List<String> actualLines = new ArrayList<>();
        Scanner s = new Scanner(result);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            if (line.isEmpty())
                continue;
            if (line.equals("."))
                continue;
            if (line.startsWith(".\\\""))
                continue;
            if (line.equals(".nh"))
                continue;
            actualLines.add(line);
        }
        assertEquals(Arrays.asList(expectedLines), actualLines);
    }

}