package io.solit.deb.changes;

import io.solit.deb.Version;
import org.commonmark.parser.Parser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yaga
 * @since 13.03.18
 */
public class ChangelogTest {


    @Test
    public void testEmptyChangelog() {
        assertThrows(IllegalArgumentException.class, Changelog::new);
    }

    @Test
    public void testSimplestChangelog() throws IOException {
        Changelog changelog = new Changelog(
                new ChangeSet("foo", new Version("1.0.0"), "bar", "baz@example.com", new StringChanges("   xyzzy\n   qux"))
                    .setDate(ZonedDateTime.of(2010, 10, 10, 10, 10, 10, 0, ZoneId.of("Z")))
        );
        try(StringWriter writer = new StringWriter()) {
            changelog.write(writer);
            checkLines(writer.toString(),
                    "foo (1.0.0) stable; urgency=medium",
                    "  xyzzy",
                    "  qux",
                    " -- bar <baz@example.com>  Sun, 10 Oct 2010 10:10:10 +0000"
            );
        }
    }

    @Test
    public void testFilledChangelog() throws IOException {
        Changelog changelog = new Changelog(
                new ChangeSet("foo", new Version("1.0.0"), "bar", "baz@example.com", new StringChanges("   xyzzy\n   qux"))
                        .setDistribution("stable-security")
                        .setUrgency(ChangeSet.Urgency.critical)
                        .setUrgencyCommentary("q q q")
                        .setDate(ZonedDateTime.of(2010, 10, 10, 10, 10, 10, 0, ZoneId.of("Z")))
        );
        try(StringWriter writer = new StringWriter()) {
            changelog.write(writer);
            checkLines(writer.toString(),
                    "foo (1.0.0) stable-security; urgency=critical (q q q)",
                    "  xyzzy",
                    "  qux",
                    " -- bar <baz@example.com>  Sun, 10 Oct 2010 10:10:10 +0000"
            );
        }
    }

    @Test
    public void testMultipleChangeSets() throws IOException {
        Changelog changelog = new Changelog(
                new ChangeSet("foo", new Version("1.0.0"), "bar", "baz@example.com", new StringChanges("   xyzzy\n   qux"))
                        .setDate(ZonedDateTime.of(2010, 10, 10, 10, 10, 10, 0, ZoneId.of("Z"))),

                new ChangeSet("oof", new Version("2.0.0"), "rab", "zab@example.com", new StringChanges("   yzzyx\n   xuq"))
                        .setDate(ZonedDateTime.of(2011, 11, 11, 11, 11, 11, 0, ZoneId.of("Z")))
        );
        try(StringWriter writer = new StringWriter()) {
            changelog.write(writer);
            checkLines(writer.toString(),
                    "oof (2.0.0) stable; urgency=medium",
                    "  yzzyx",
                    "  xuq",
                    " -- rab <zab@example.com>  Fri, 11 Nov 2011 11:11:11 +0000",
                    "",
                    "foo (1.0.0) stable; urgency=medium",
                    "  xyzzy",
                    "  qux",
                    " -- bar <baz@example.com>  Sun, 10 Oct 2010 10:10:10 +0000"
            );
        }
    }

    @Test
    public void testAppendedChangeSets() throws IOException {
        Changelog changelog = new Changelog(
                new ChangeSet("foo", new Version("1.0.0"), "bar", "baz@example.com", new StringChanges("   xyzzy\n   qux"))
                        .setDate(ZonedDateTime.of(2010, 10, 10, 10, 10, 10, 0, ZoneId.of("Z")))
        );
        changelog.addChangeSet(
                new ChangeSet("oof", new Version("2.0.0"), "rab", "zab@example.com", new StringChanges("   yzzyx\n   xuq"))
                        .setDate(ZonedDateTime.of(2011, 11, 11, 11, 11, 11, 0, ZoneId.of("Z")))
        );
        try(StringWriter writer = new StringWriter()) {
            changelog.write(writer);
            checkLines(writer.toString(),
                    "oof (2.0.0) stable; urgency=medium",
                    "  yzzyx",
                    "  xuq",
                    " -- rab <zab@example.com>  Fri, 11 Nov 2011 11:11:11 +0000",
                    "",
                    "foo (1.0.0) stable; urgency=medium",
                    "  xyzzy",
                    "  qux",
                    " -- bar <baz@example.com>  Sun, 10 Oct 2010 10:10:10 +0000"
            );
        }
    }

    @Test
    public void testMarkdownChanges() throws IOException {
        Changes c = new MarkdownChanges(Parser.builder() .build().parseReader(new StringReader(String.join("\n",
                "### Added",
                "-   feature 1",
                "-   feature 2",
                "### Removed",
                "1.  feature 3",
                "2.  feature 4"
        ))));
        Changelog changelog = new Changelog(
                new ChangeSet("foo", new Version("1.0.0"), "bar", "baz@example.com", c)
                        .setDate(ZonedDateTime.of(2010, 1, 1, 1, 1, 1, 1, ZoneId.ofOffset("UT", ZoneOffset.ofHours(5)))
        ));
        try(StringWriter writer = new StringWriter()) {
            changelog.write(writer);
            checkLines(writer.toString(),
                    "foo (1.0.0) stable; urgency=medium",
                    "  ### Added",
                    "  -   feature 1",
                    "  -   feature 2",
                    "  ### Removed",
                    "  1.  feature 3",
                    "  2.  feature 4",
                    " -- bar <baz@example.com>  Fri, 1 Jan 2010 01:01:01 +0500"
            );
        }
    }

    private void checkLines(String string, String... lines) {
        Scanner scanner = new Scanner(string);
        int i = 0;
        while (scanner.hasNextLine() && i < lines.length) {
            assertEquals(lines[i], scanner.nextLine(), "Line " + i + " mismatch");
            i++;
        }
        if (!scanner.hasNextLine() && i < lines.length) {
            fail("Only " + i + " lines of " +  lines.length + " is present");
        }
        if (scanner.hasNextLine() && i >= lines.length) {
            StringBuilder builder = new StringBuilder();
            while (scanner.hasNextLine()) {
                builder.append(scanner.nextLine()).append("\n");
            }
            assertEquals("", builder.toString(), "Unexpected text");
        }
    }

}