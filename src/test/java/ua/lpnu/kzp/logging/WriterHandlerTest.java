package ua.lpnu.kzp.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link WriterHandler}: level filtering, writer ownership and I/O failures, which must
 * be reported to the error manager instead of propagating to the application.
 */
class WriterHandlerTest {

    /** Writer whose every operation fails. */
    private static final class FailingWriter extends Writer {

        @Override
        public void write(char[] buffer, int offset, int length) throws IOException {
            throw new IOException("write failed");
        }

        @Override
        public void flush() throws IOException {
            throw new IOException("flush failed");
        }

        @Override
        public void close() throws IOException {
            throw new IOException("close failed");
        }
    }

    /** Writer that remembers whether it was closed. */
    private static final class TrackingWriter extends StringWriter {

        private boolean closed;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /** Error manager that records the reported error codes. */
    private static final class RecordingErrorManager extends ErrorManager {

        private final List<Integer> codes = new ArrayList<>();

        @Override
        public synchronized void error(String message, Exception exception, int code) {
            codes.add(code);
        }
    }

    private static LogRecord record(Level level) {
        return new LogRecord(level, "message");
    }

    @Test
    void writeFailureIsReportedNotThrown() {
        WriterHandler handler = new WriterHandler(new FailingWriter(), new SimpleFormatter(), false);
        RecordingErrorManager errors = new RecordingErrorManager();
        handler.setErrorManager(errors);

        handler.publish(record(Level.INFO));

        assertEquals(List.of(ErrorManager.WRITE_FAILURE), errors.codes);
    }

    @Test
    void flushAndCloseFailuresAreReportedNotThrown() {
        WriterHandler handler = new WriterHandler(new FailingWriter(), new SimpleFormatter(), true);
        RecordingErrorManager errors = new RecordingErrorManager();
        handler.setErrorManager(errors);

        handler.close();

        assertEquals(List.of(ErrorManager.FLUSH_FAILURE, ErrorManager.CLOSE_FAILURE), errors.codes);
    }

    @Test
    void recordBelowHandlerLevelIsNotWritten() {
        StringWriter writer = new StringWriter();
        WriterHandler handler = new WriterHandler(writer, new SimpleFormatter(), false);
        handler.setLevel(Level.WARNING);

        handler.publish(record(Level.INFO));

        assertTrue(writer.toString().isEmpty());
    }

    @Test
    void closeLeavesCallerOwnedWriterOpen() {
        TrackingWriter writer = new TrackingWriter();
        WriterHandler handler = new WriterHandler(writer, new SimpleFormatter(), false);

        handler.close();

        assertFalse(writer.closed);
    }

    @Test
    void closeClosesOwnedWriter() {
        TrackingWriter writer = new TrackingWriter();
        WriterHandler handler = new WriterHandler(writer, new SimpleFormatter(), true);

        handler.close();

        assertTrue(writer.closed);
    }
}
