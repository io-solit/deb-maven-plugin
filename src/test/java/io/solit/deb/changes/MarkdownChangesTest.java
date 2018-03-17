package io.solit.deb.changes;

import org.commonmark.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author yaga
 * @since 3/14/18
 */
public class MarkdownChangesTest {

    @Test
    public void testParagraphAndBreaks() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "Lorem ipsum",
                "dolor sit amet",
                "",
                "consectetur  ",
                "adipiscing"
        ))));
        checkLines(c,
                "Lorem ipsum",
                "dolor sit amet",
                "",
                "consectetur  ",
                "adipiscing"
        );
    }

    @Test
    public void testHeadings() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "# Foo",
                "## Bar",
                "Baz",
                "======",
                "Qux",
                "------",
                "### Xyzzy"
        ))));
        checkLines(c,
                "# Foo",
                "## Bar",
                "# Baz",
                "## Qux",
                "### Xyzzy"
        );
    }

    @Test
    public void testUnorderedLists() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "* foo",
                "* bar",
                "",
                "- baz",
                "- qux"
        ))));
        checkLines(c,
                "*   foo",
                "*   bar",
                "-   baz",
                "-   qux"
        );
    }

    @Test
    public void testOrderedLists() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "1. foo",
                "2. bar",
                "",
                "5. baz",
                "6. qux"
        ))));
        checkLines(c,
                "1.  foo",
                "2.  bar",
                "3.  baz",
                "4.  qux"
        );
    }

    @Test
    public void testFencedCode() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "````md",
                "_i'm_ *not* a `markdown`",
                "````"
        ))));
        checkLines(c,
                "````md",
                "_i'm_ *not* a `markdown`",
                "````"
        );
    }

    @Test
    public void testIndentedCode() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "    actually",
                "    _i'm_ *not* a `markdown`",
                "    i'm a code"
        ))));
        checkLines(c,
                "    actually",
                "    _i'm_ *not* a `markdown`",
                "    i'm a code"
        );
    }

    @Test
    public void testHtmlBlocks() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "<table>",
                "    <tr>",
                "        <td>Hello!</td>",
                "    </tr>",
                "</table>"
        ))));
        checkLines(c,
                "<table>",
                "    <tr>",
                "        <td>Hello!</td>",
                "    </tr>",
                "</table>"
        );
    }


    @Test
    public void testThematicBreaks() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "topic",
                "",
                "-------",
                "theme",
                "",
                "*****",
                "subject",
                "",
                "___"
        ))));
        checkLines(c,
                "topic",
                "",
                "---",
                "theme",
                "",
                "---",
                "subject",
                "",
                "---"
        );
    }

    @Test
    public void testInlines() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "*strong* _emphasized_",
                "__very emphasized__ **quite strong**",
                "`code part` <inline/>",
                "[*__decorated__ link*](/to/some/place \"here\")",
                "![just an image](/something.jpg)"
        ))));
        checkLines(c,
                "*strong* _emphasized_",
                "__very emphasized__ **quite strong**",
                "`code part` <inline/>",
                "[*__decorated__ link*](/to/some/place \"here\")",
                "![just an image](/something.jpg)"
        );
    }

    @Test
    public void testComplexMarkdown() throws IOException {
        MarkdownChanges c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "### Heading _with_ *decorations*",
                "-   list item 1",
                "-   list item 2",
                "1.  ord item 1",
                "2.  ord item 2",
                "    ```java",
                "    public static class Foo {",
                "        public static final int i = 1;",
                "    }",
                "    ```",
                "    *   ast item 1",
                "    *   ast item 2",
                "        continuation",
                "",
                "        New paragraph",
                "> quote",
                "> more quote",
                ">",
                "> even more quote",
                "> > quote in `quote`"
        ))));
        checkLines(c,
                "### Heading _with_ *decorations*",
                "-   list item 1",
                "-   list item 2",
                "1.  ord item 1",
                "2.  ord item 2",
                "    ```java",
                "    public static class Foo {",
                "        public static final int i = 1;",
                "    }",
                "    ```",
                "    *   ast item 1",
                "    *   ast item 2",
                "        continuation",
                "",
                "        New paragraph",
                "> quote",
                "> more quote",
                ">",
                "> even more quote",
                "> > quote in `quote`"
        );
    }

    public static void checkLines(Changes changes, String... lines) {
        int i = 0;
        Iterator<String> scanner = changes.iterator();
        while (scanner.hasNext() && i < lines.length) {
            assertEquals(lines[i], scanner.next(), "Line " + i + " mismatch");
            i++;
        }
        if (!scanner.hasNext() && i < lines.length) {
            fail("Only " + i + " lines of " +  lines.length + " is present");
        }
        if (scanner.hasNext() && i >= lines.length) {
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNext()) {
                builder.append(scanner.next()).append("\n");
            }
            assertEquals("", builder.toString(), "Unexpected text");
        }
    }

}