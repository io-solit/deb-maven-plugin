package io.solit.deb.changes;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static io.solit.deb.changes.MarkdownChangesTest.checkLines;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author yaga
 * @since 14.03.18
 */
public class KeepChangelogParserTest {

    @Test
    public void testOneSetChangelog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "This changelog bla bla bla follows bla bla, adopted bla bla bla",
                "## Unreleased",
                " This change is still coming",
                "## [1.0.0] - 2010-10-10",
                "Some change description here"
        )));
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.medium, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals("bar", s.getMaintainer());
        assertEquals("baz@example.com", s.getMaintainerEmail());
        assertEquals("qux", s.getDistribution());
        assertEquals(null, s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(), "Some change description here");
    }

    @Test
    public void testOneSetChangelogWithZonedDate() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10UTC",
                "Some change description here"
        )));
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("UTC")), cl.getChanges().get(0).getDate());
        cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10UTC+0100",
                "Some change description here"
        )));
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("UTC+0100")), cl.getChanges().get(0).getDate());
        cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10+01:00",
                "Some change description here"
        )));
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("+01:00")), cl.getChanges().get(0).getDate());
        cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10-01:00",
                "Some change description here"
        )));
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("-01:00")), cl.getChanges().get(0).getDate());
    }

    @Test
    public void testOneSetChangelogWithTime() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10T01:01:01",
                "Some change description here"
        )));
        assertEquals(ZonedDateTime.of(2010, 10, 10, 1, 1, 1, 0, ZoneId.of("Z")), cl.getChanges().get(0).getDate());
        cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10T01:01:01UTC+0100",
                "Some change description here"
        )));
        assertEquals(ZonedDateTime.of(2010, 10, 10, 1, 1, 1, 0, ZoneId.of("UTC+0100")), cl.getChanges().get(0).getDate());
    }

    @Test
    public void testEmptyLog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10"
        )));
        assertNull(cl);
    }

    @Test
    public void testYankedLog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10 [YANKED]"
        )));
        assertNotNull(cl);
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.medium, s.getUrgency());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(), "yanked");
    }

    @Test
    public void testDetailedYankedLog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10 [YANKED]",
                "Some details"
        )));
        assertNotNull(cl);
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.medium, s.getUrgency());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(), "Some details");
    }

    @Test
    public void testSecurityUrgency() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "## [1.0.0] - 2010-10-10",
                "### Security",
                "Some hole patched"
        )));
        assertNotNull(cl);
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.high, s.getUrgency());
        checkLines(s.getChanges(), "### Security", "Some hole patched");
    }

    @Test
    public void testMultisetChangelog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "Preface",
                "## Unreleased",
                "Pending",
                "## [1.1.0] - 2010-10-10",
                "### Fixed",
                "Fixed a bug",
                "### Security",
                "Patched a hole",
                "Made another one",
                "## [1.0.0] - 2010-01-01",
                "### Added",
                "* Created some stuff",
                "* Tested it",
                "## [1.0.1] - 2010-07-07 [YANKED]",
                "## [0.5] - 2009-07-07"
        )));
        assertNotNull(cl);
        assertEquals(3, cl.getChanges().size());

        ChangeSet s = cl.getChanges().get(0);
        assertEquals("foo", s.getPackageName());
        assertEquals("1.1.0", s.getVersion().getValidatedString());
        assertEquals("bar", s.getMaintainer());
        assertEquals("baz@example.com", s.getMaintainerEmail());
        assertEquals("qux", s.getDistribution());
        assertEquals(ChangeSet.Urgency.high, s.getUrgency());
        assertEquals(null, s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(),
                "### Fixed",
                "Fixed a bug",
                "### Security",
                "Patched a hole",
                "Made another one"
        );

        s = cl.getChanges().get(1);
        assertEquals(ChangeSet.Urgency.medium, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.1", s.getVersion().getValidatedString());
        assertEquals("bar", s.getMaintainer());
        assertEquals("baz@example.com", s.getMaintainerEmail());
        assertEquals("qux", s.getDistribution());
        assertEquals(null, s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 7, 7, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(), "yanked");

        s = cl.getChanges().get(2);
        assertEquals(ChangeSet.Urgency.medium, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals("bar", s.getMaintainer());
        assertEquals("baz@example.com", s.getMaintainerEmail());
        assertEquals("qux", s.getDistribution());
        assertEquals(null, s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 1, 1, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(),
                "### Added",
                "*   Created some stuff",
                "*   Tested it"
        );
    }

    @Test
    public void testDetailedChangelog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "## Unreleased",
                " This change is still coming",
                "## [1.0.0] - 2010-10-10",
                "### Release",
                "* urgency: low (nothing happens)",
                "* maintainer: xyzzy <zap@example.org>",
                "* distribution: xuq",
                "### Added",
                "Some change description here"
        )));
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.low, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals("xyzzy", s.getMaintainer());
        assertEquals("zap@example.org", s.getMaintainerEmail());
        assertEquals("xuq", s.getDistribution());
        assertEquals("nothing happens", s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(), "### Added", "Some change description here");
    }

    @Test
    public void testDetailedChangelogUrgencyWithNoComments() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "## Unreleased",
                " This change is still coming",
                "## [1.0.0] - 2010-10-10",
                "### Release",
                "* urgency: low",
                "### Security",
                "Some change description here"
        )));
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.low, s.getUrgency());
        assertEquals(null, s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(), "### Security", "Some change description here");
    }

    @Test
    public void testUnexpectedlyDetailedChangelog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "## Unreleased",
                " This change is still coming",
                "## [1.0.0] - 2010-10-10",
                "### Release",
                "Some arbitrary text",
                "### Added",
                "Some change description here"
        )));
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.medium, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals("bar", s.getMaintainer());
        assertEquals("baz@example.com", s.getMaintainerEmail());
        assertEquals("qux", s.getDistribution());
        assertEquals(null, s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(),
                "### Release",
                "Some arbitrary text",
                "### Added",
                "Some change description here"
        );
    }

    @Test
    public void testExtensivelyDetailedChangelog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "## Unreleased",
                " This change is still coming",
                "## [1.0.0] - 2010-10-10",
                "### Release",
                "Some arbitrary text",
                "* urgency: low (nothing happens)",
                "* maintainer: xyzzy <zap@example.org>",
                "* distribution: xuq",
                "### Added",
                "Some change description here"
        )));
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.low, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals("xyzzy", s.getMaintainer());
        assertEquals("zap@example.org", s.getMaintainerEmail());
        assertEquals("xuq", s.getDistribution());
        assertEquals("nothing happens", s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(),
                "### Release",
                "Some arbitrary text",
                "### Added",
                "Some change description here"
        );
    }

    @Test
    public void testAdditionallyDetailedChangelog() throws IOException {
        KeepChangelogParser parser = new KeepChangelogParser("foo", "bar", "baz@example.com");
        parser.setDefaultDistribution("qux");
        Changelog cl = parser.parse(new StringReader(String.join("\n",
                "# Changelog",
                "## Unreleased",
                " This change is still coming",
                "## [1.0.0] - 2010-10-10",
                "### Release",
                "* Some arbitrary text",
                "* urgency: low (nothing happens)",
                "* distribution: xuq",
                "### Added",
                "Some change description here"
        )));
        assertEquals(1, cl.getChanges().size());
        ChangeSet s = cl.getChanges().iterator().next();
        assertEquals(ChangeSet.Urgency.low, s.getUrgency());
        assertEquals("foo", s.getPackageName());
        assertEquals("1.0.0", s.getVersion().getValidatedString());
        assertEquals("xuq", s.getDistribution());
        assertEquals("nothing happens", s.getUrgencyCommentary());
        assertEquals(ZonedDateTime.of(2010, 10, 10, 0, 0, 0, 0, ZoneId.of("Z")), s.getDate());
        checkLines(s.getChanges(),
                "### Release",
                "*   Some arbitrary text",
                "*   urgency: low (nothing happens)",
                "*   distribution: xuq",
                "### Added",
                "Some change description here"
        );
    }

}