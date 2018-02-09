package io.solit.deb;

import io.solit.deb.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.solit.deb.Control;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author yaga
 * @since 19.01.18
 */
public class ControlTest {

    private boolean checkStdHeader(String line) {
        return line.startsWith("Package") || line.startsWith("Version") || line.startsWith("Maintainer") ||
                line.startsWith("Description") || line.startsWith("Architecture");
    }

    @Test
    public void testSimplestControl() throws IOException {
        StringWriter writer = new StringWriter();
        new Control("package", new Version("1.0"), "me", "all", "Have fun").writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Package"))
                Assertions.assertTrue(line.matches("Package:\\s*package"), line);
            else if (line.startsWith("Version"))
                Assertions.assertTrue(line.matches("Version:\\s*1\\.0"), line);
            else if (line.startsWith("Maintainer"))
                Assertions.assertTrue(line.matches("Maintainer:\\s*me"), line);
            else if (line.startsWith("Description"))
                Assertions.assertTrue(line.matches("Description:\\s*Have fun"), line);
            else if (line.startsWith("Architecture"))
                Assertions.assertTrue(line.matches("Architecture:\\s*all"), line);
            else
                fail("Unexpected string " + line);
        }
    }

    @Test
    public void testDescription() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.setDescription("Have fun\nwhile installing this package\n\nIt's cool!");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Description")) {
                checkLine(line, "Description:\\s*Have fun");
                assertTrue(sc.hasNextLine());
                checkLine(sc, "\\s+Have fun");
                checkLine(sc, "\\s+while installing this package");
                checkLine(sc, "\\s+\\.");
                checkLine(sc, "\\s+It's cool!");
            } else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testProvides() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addProvides("foo");
        control.addProvides("bar");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Provides")) {
                checkLine(line, "Provides:\\s*(?:foo\\s*,\\s*bar|bar\\s*,\\s*foo)");
            } else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testDepends() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addDepends("foo | bar");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Depends"))
                checkLine(line, "Depends:\\s*foo \\| bar");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testRecommends() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addRecommends("foo (>= 1.0)");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Recommends"))
                checkLine(line, "Recommends:\\s*foo \\(>= 1\\.0\\)");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testSuggests() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addSuggests("foo");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Suggests"))
                checkLine(line, "Suggests:\\s*foo");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testBreaks() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addBreaks("foo");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Breaks"))
                checkLine(line, "Breaks:\\s*foo");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testConflicts() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addConflicts("foo");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Conflicts"))
                checkLine(line, "Conflicts:\\s*foo");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testPreDepends() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addPreDepends("foo");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Pre-Depends"))
                checkLine(line, "Pre-Depends:\\s*foo");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testEnhances() throws IOException {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addEnhances("foo");
        StringWriter writer = new StringWriter();
        control.writeControlFile(writer);
        Scanner sc = new Scanner(writer.toString());
        while (sc.hasNextLine()) {
            String line = sc.nextLine();
            if (line.startsWith("Enhances"))
                checkLine(line, "Enhances:\\s*foo");
            else if (!checkStdHeader(line))
                fail("Unexpected line " + line);
        }
    }

    @Test
    public void testParsings() {
        Control control = new Control("package", new Version("1.0"), "me", "all", "Have fun");
        control.addPreDepends("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
        control.addDepends("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
        control.addRecommends("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
        control.addSuggests("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
        control.addEnhances("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
        control.addBreaks("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
        control.addConflicts("foo (<= 2.1-4) | bar (= 3.4) | qux (>> 1:3.3) | baz ( << 1:4-4-5 ) | qqq (>=1)");
    }


    private void checkLine(Scanner scanner, String regex) {
        assertTrue(scanner.hasNextLine(), "No next line");
        checkLine(scanner.nextLine(), regex);
    }

    private void checkLine(String line, String regex) {
        assertTrue(line.matches(regex), () -> "'" + line + "' does not match '" + regex + "'");
    }

}
