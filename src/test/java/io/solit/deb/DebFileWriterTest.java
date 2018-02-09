package io.solit.deb;

import io.solit.deb.DebFileWriter;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.Charsets;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yaga
 * @since 19.01.18
 */
public class DebFileWriterTest {

    @Test
    public void testPrematureClose() throws IOException {
        File f = File.createTempFile("test", ".deb");
        try {
            assertThrows(IOException.class, () -> {
                try (DebFileWriter ignore = new DebFileWriter(f)) {}
            });
        } finally {
            if (!f.delete())
                f.deleteOnExit();
        }
    }

    @Test
    public void testPrematureDataOpen() throws IOException {
        File f = File.createTempFile("test", ".deb");
        try {
            try (DebFileWriter debFileWriter = new DebFileWriter(f)) {
                assertThrows(IOException.class, debFileWriter::openData);
            } catch (IOException e) {
                // do nothing
            }
        } finally {
            if (!f.delete())
                f.deleteOnExit();
        }
    }

    @Test
    public void testDataOpenWithoutControlClose() throws IOException {
        File f = File.createTempFile("test", ".deb");
        try {
            try (DebFileWriter debFileWriter = new DebFileWriter(f)) {
                debFileWriter.openControl();
                assertThrows(IOException.class, debFileWriter::openData);
            } catch (IOException e) {
                // do nothing
            }
        } finally {
            if (!f.delete())
                f.deleteOnExit();
        }
    }

    @Test
    public void testControlReopen() throws IOException {
        File f = File.createTempFile("test", ".deb");
        try {
            try (DebFileWriter debFileWriter = new DebFileWriter(f)) {
                debFileWriter.openControl().close();
                assertThrows(IOException.class, debFileWriter::openControl);
            } catch (IOException e) {
                // do nothing
            }
        } finally {
            if (!f.delete())
                f.deleteOnExit();
        }
    }

    @Test
    public void testDataReopen() throws IOException {
        File f = File.createTempFile("test", ".deb");
        try {
            try (DebFileWriter debFileWriter = new DebFileWriter(f)) {
                debFileWriter.openControl().close();
                debFileWriter.openData().close();
                assertThrows(IOException.class, debFileWriter::openData);
            } catch (IOException e) {
                // do nothing
            }
        } finally {
            if (!f.delete())
                f.deleteOnExit();
        }
    }

    @Test
    public void testEmptyDebFile() throws IOException {
        File f = File.createTempFile("test", ".deb");
        try {
            try (DebFileWriter debFileWriter = new DebFileWriter(f)) {
                try(TarArchiveOutputStream tarArchiveOutputStream = debFileWriter.openControl()) {
                    TarArchiveEntry control = new TarArchiveEntry("control");
                    control.setSize(3);
                    tarArchiveOutputStream.putArchiveEntry(control);
                    try {
                        tarArchiveOutputStream.write("foo".getBytes(Charsets.ISO_8859_1));
                    } finally {
                        tarArchiveOutputStream.closeArchiveEntry();
                    }
                }
                try(TarArchiveOutputStream tarArchiveOutputStream = debFileWriter.openData()) {
                    TarArchiveEntry archiveEntry = new TarArchiveEntry("/etc/some-data", true);
                    archiveEntry.setSize(3);
                    tarArchiveOutputStream.putArchiveEntry(archiveEntry);
                    try {
                        tarArchiveOutputStream.write("bar".getBytes(Charsets.ISO_8859_1));
                    } finally {
                        tarArchiveOutputStream.closeArchiveEntry();
                    }
                }
            }
            try (ArArchiveInputStream ar = new ArArchiveInputStream(new FileInputStream(f))) {
                {
                    ArArchiveEntry debBinary = ar.getNextArEntry();
                    assertNotNull(debBinary);
                    assertEquals("debian-binary", debBinary.getName());
                    assertEquals(0, debBinary.getUserId());
                    assertEquals(0, debBinary.getGroupId());
                    assertEquals(4, debBinary.getLength());
                    // 100644
                    assertEquals(0b1000000110100100, debBinary.getMode());
                    byte[] buffer = new byte[4];
                    assertEquals(4, ar.read(buffer));
                    assertEquals("2.0\n", new String(buffer, Charsets.ISO_8859_1));
                }
                {
                    ArArchiveEntry control = ar.getNextArEntry();
                    assertNotNull(control);
                    assertEquals("control.tar.gz", control.getName());
                    assertEquals(0, control.getUserId());
                    assertEquals(0, control.getGroupId());
                    // 100644
                    assertEquals(0b1000000110100100, control.getMode());
                    byte[] buffer = new byte[(int) control.getSize()];
                    assertTrue(ar.read(buffer) > 0);
                    TarArchiveInputStream controlArchive = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(buffer)));
                    TarArchiveEntry nextTarEntry = controlArchive.getNextTarEntry();
                    assertEquals("control", nextTarEntry.getName());
                    assertEquals(3, nextTarEntry.getSize());
                    buffer = new byte[3];
                    assertEquals(3, controlArchive.read(buffer));
                    assertEquals("foo", new String(buffer, Charsets.ISO_8859_1));
                }

                {
                    ArArchiveEntry data = ar.getNextArEntry();
                    assertNotNull(data);
                    assertEquals("data.tar.gz", data.getName());
                    assertEquals(0, data.getUserId());
                    assertEquals(0, data.getGroupId());
                    // 100644
                    assertEquals(0b1000000110100100, data.getMode());
                    byte[] buffer = new byte[(int) data.getSize()];
                    assertTrue(ar.read(buffer) > 0);
                    TarArchiveInputStream controlArchive = new TarArchiveInputStream(new GZIPInputStream(new ByteArrayInputStream(buffer)));
                    TarArchiveEntry nextTarEntry = controlArchive.getNextTarEntry();
                    assertEquals("/etc/some-data", nextTarEntry.getName());
                    assertEquals(3, nextTarEntry.getSize());
                    buffer = new byte[3];
                    assertEquals(3, controlArchive.read(buffer));
                    assertEquals("bar", new String(buffer, Charsets.ISO_8859_1));
                }
            }
        } finally {
            if (!f.delete())
                f.deleteOnExit();
        }

    }

}