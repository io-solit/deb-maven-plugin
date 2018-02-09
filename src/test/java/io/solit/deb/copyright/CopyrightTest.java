package io.solit.deb.copyright;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author yaga
 * @since 24.01.18
 */
public class CopyrightTest {

    @Test
    public void minimalCopyright() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        StringWriter stringWriter = new StringWriter();
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
    }

    @Test
    public void testFileListCopyright() throws IOException {
        String as = String.join("", Collections.nCopies(90, "a"));
        Copyright cpr = new Copyright(new LinkedHashSet<>(Arrays.asList("*.sh", as, "*.bat", "*.js")), "foo", "bar");
        StringWriter stringWriter = new StringWriter();
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*.sh");
        assertMatches(sc.nextLine(), "\\s+" + as);
        assertMatches(sc.nextLine(), "\\s+\\*.bat \\*.js");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
    }

    @Test
    public void twoFilesCopyright() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        StringWriter stringWriter = new StringWriter();
        cpr.addFiles(Collections.singleton("some/*\\?"), "baz", "qux");
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*some/\\*\\\\\\?");
        assertMatches(sc.nextLine(), "Copyright:\\s*baz");
        assertMatches(sc.nextLine(), "Licence:\\s*qux");
    }

    @Test
    public void additionalFilesInfoCopyright() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar", "baz", "qux");
        StringWriter stringWriter = new StringWriter();
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
        assertMatches(sc.nextLine(), "\\s+qux");
        assertMatches(sc.nextLine(), "Comment:\\s*baz");
    }

    @Test
    public void additionalCopyrightInfo() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        cpr.setCopyright("xyzzy");
        cpr.setSource("git@example.com");
        cpr.setUpstreamName("example");
        cpr.addUpstreamContact("Alice <alice@example.com>");
        cpr.setDisclaimer("ABSOLUTELY NO WARRANTY!!!");
        cpr.setLicence("NFR", "No familiar rules apply");
        cpr.setComment("111");
        StringWriter stringWriter = new StringWriter();
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "Upstream-Name:\\s*example");
        assertMatches(sc.nextLine(), "Upstream-Contact:\\s*Alice <alice@example.com>");
        assertMatches(sc.nextLine(), "Source:\\s*git@example.com");
        assertMatches(sc.nextLine(), "Disclaimer:\\s*ABSOLUTELY NO WARRANTY!!!");
        assertMatches(sc.nextLine(), "Comment:\\s*111");
        assertMatches(sc.nextLine(), "Licence:\\s*NFR");
        assertMatches(sc.nextLine(), "\\s+No familiar rules apply");
        assertMatches(sc.nextLine(), "Copyright:\\s*xyzzy");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
    }

    @Test
    public void testSeveralUpstreamContacts() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        cpr.addUpstreamContact("Alice <alice@example.com>");
        cpr.addUpstreamContact("Bob <bob@example.com>");
        StringWriter stringWriter = new StringWriter();
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "Upstream-Contact:\\s*Alice <alice@example.com>");
        assertMatches(sc.nextLine(), "\\s+Bob <bob@example.com>");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
    }

    @Test
    public void testLicences() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        StringWriter stringWriter = new StringWriter();
        cpr.addStandAloneLicence("baz", "qux").setComment("xyzzy");
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Licence:\\s*baz");
        assertMatches(sc.nextLine(), "\\s+qux");
        assertMatches(sc.nextLine(), "Comment:\\s*xyzzy");
    }

    @Test
    public void testLicencesWithNoComment() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        StringWriter stringWriter = new StringWriter();
        cpr.addStandAloneLicence("baz", "qux");
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Licence:\\s*baz");
        assertMatches(sc.nextLine(), "\\s+qux");
    }

    @Test
    public void testSeveralLicences() throws IOException {
        Copyright cpr = new Copyright(Collections.singleton("*"), "foo", "bar");
        StringWriter stringWriter = new StringWriter();
        cpr.addStandAloneLicence("baz", "qux").setComment("xyzzy");
        cpr.addStandAloneLicence("WTFPL", "Do whatever you want").setComment("anything");
        cpr.writeCopyright(stringWriter);
        Scanner sc = new Scanner(stringWriter.toString());
        assertMatches(sc.nextLine(), "Format:\\s*https?://www.debian.org/doc/packaging-manuals/copyright-format/1\\.0/");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Files:\\s*\\*");
        assertMatches(sc.nextLine(), "Copyright:\\s*foo");
        assertMatches(sc.nextLine(), "Licence:\\s*bar");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Licence:\\s*baz");
        assertMatches(sc.nextLine(), "\\s+qux");
        assertMatches(sc.nextLine(), "Comment:\\s*xyzzy");
        assertMatches(sc.nextLine(), "");
        assertMatches(sc.nextLine(), "Licence:\\s*WTFPL");
        assertMatches(sc.nextLine(), "\\s+Do whatever you want");
        assertMatches(sc.nextLine(), "Comment:\\s*anything");
    }


    private void assertMatches(String string, String pattern) {
        assertTrue(string.matches(pattern), () -> "'" + string + "' does not match '" + pattern + "'");
    }

}