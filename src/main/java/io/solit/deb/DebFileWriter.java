package io.solit.deb;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.zip.GZIPOutputStream;

/**
 * @author yaga
 * @since 18.01.18
 */
public class DebFileWriter implements AutoCloseable {
    private static final String AR_HEADER = "!<arch>\n";
    private static final String DEB_VERSION = "2.0";
    private static final int BEFORE_CONTROL_STAGE = 2, BEFORE_DATA_SAGE = 4, FINAL_STAGE = 6;
    private static final int HEADER_LENGTH = 60, FILENAME_LENGTH = 16, MOD_TIME_LENGTH = 12;
    private static final long SIZE_OFFSET = 48;
    private static final String OWNER_ID = "0     ", GROUP_ID = "0     ", FILE_MODE = "100644  ", SIZE_PLACEHOLDER = "          ";
    private static final Charset CHARSET = StandardCharsets.ISO_8859_1;
    private final RandomAccessFile _randomAccessFile;
    private int _stage;

    public DebFileWriter(File file) throws IOException {
        _randomAccessFile = new RandomAccessFile(file, "rw");
        _randomAccessFile.write(AR_HEADER.getBytes(CHARSET));
        try (Writer os = new OutputStreamWriter(new DebOutputStream("debian-binary", "debian-binary"), CHARSET)) {
            os.write(DEB_VERSION + "\n");
        }
    }

    public TarArchiveOutputStream openControl() throws IOException {
        if (_stage != BEFORE_CONTROL_STAGE)
            throw new IOException("Control stream was previously open");
        TarArchiveOutputStream control = new TarArchiveOutputStream(
                new GZIPOutputStream(new DebOutputStream("control", "control.tar.gz")),
                StandardCharsets.UTF_8.name()
        );
        control.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        return control;
    }

    public TarArchiveOutputStream openData() throws IOException {
        if (_stage != BEFORE_DATA_SAGE)
            throw new IOException("Data stream was previously open");
        TarArchiveOutputStream data = new TarArchiveOutputStream(
                new GZIPOutputStream(new DebOutputStream("data", "data.tar.gz")),
                StandardCharsets.UTF_8.name()
        );
        data.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        return data;
    }

    @Override
    public void close() throws IOException {
        try {
            if (_stage > FILENAME_LENGTH)
                return;
            if (_stage < FINAL_STAGE)
                throw new IOException("Deb file is incomplete");
            _stage++;
        } finally {
            _randomAccessFile.close();
        }
    }

    private class DebOutputStream extends OutputStream {
        private final long _startOffset;
        private final int _activeState;
        private final String _name;

        public DebOutputStream(String name, String fileName) throws IOException {
            _activeState = ++_stage;
            _startOffset = _randomAccessFile.getFilePointer();
            ByteArrayOutputStream buffer = createHeader(fileName);
            buffer.writeTo(this);
            _name = name;
        }

        private ByteArrayOutputStream createHeader(String fileName) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(HEADER_LENGTH);
            try(Writer w = new OutputStreamWriter(buffer, CHARSET)) {
                w.write(fileName);
                for (int i = fileName.length(); i < FILENAME_LENGTH; i++)
                    w.write(' ');
                String modTime = Long.toString(Instant.now().getEpochSecond());
                w.write(modTime);
                for (int i = modTime.length(); i < MOD_TIME_LENGTH; i++)
                    w.write(' ');
                w.write(OWNER_ID);
                w.write(GROUP_ID);
                w.write(FILE_MODE);
                w.write(SIZE_PLACEHOLDER);
            }
            buffer.write(0x60);
            buffer.write(0x0A);
            return buffer;
        }

        private void checkStage() throws IOException {
            if (_stage != _activeState)
                throw new IOException("Unable to write to " + _name + ", since it was previously closed");
        }

        @Override
        public void write(int b) throws IOException {
            checkStage();
            _randomAccessFile.write(b);
        }

        @Override
        public void write(byte[] b) throws IOException {
            checkStage();
            _randomAccessFile.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkStage();
            _randomAccessFile.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            if (_stage != _activeState)
                return;
            long offset = _randomAccessFile.getFilePointer();
            long size = offset - _startOffset - HEADER_LENGTH;
            if (size % 2 != 0) {
                write(0x0A); // Pad to even size;
                offset = _randomAccessFile.getFilePointer();
            }
            _randomAccessFile.seek(_startOffset + SIZE_OFFSET);
            if (size >= 10_000_000_000L) // 10 Gigabytes
                throw new IOException("Content size too large: " + size);
            _randomAccessFile.write(Long.toString(size).getBytes(CHARSET));
            _randomAccessFile.seek(offset);
            _stage++;
        }
    }
}
