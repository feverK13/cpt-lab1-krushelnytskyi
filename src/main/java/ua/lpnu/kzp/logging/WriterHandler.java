package ua.lpnu.kzp.logging;

import java.io.IOException;
import java.io.Writer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A {@link Handler} that writes formatted log records to a {@link Writer}, flushing after every
 * record so content is visible immediately in files and mirrored streams.
 */
final class WriterHandler extends Handler {

    private final Writer writer;
    private final boolean closeWriter;

    /**
     * Creates a handler writing through the given writer.
     *
     * @param writer      destination for formatted log records
     * @param formatter   formatter used to render each record
     * @param closeWriter whether {@link #close()} must also close {@code writer}; false when the
     *                    writer wraps a stream owned by the caller (e.g. stderr)
     */
    WriterHandler(Writer writer, Formatter formatter, boolean closeWriter) {
        this.writer = writer;
        this.closeWriter = closeWriter;
        setFormatter(formatter);
    }

    /**
     * Writes and flushes the formatted record if it passes the level and filter checks. A write
     * failure is reported to the error manager instead of being thrown.
     *
     * @param record the record to write
     */
    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }
        try {
            writer.write(getFormatter().format(record));
            writer.flush();
        } catch (IOException e) {
            reportError(null, e, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * Flushes the writer; a failure is reported to the error manager instead of being thrown.
     */
    @Override
    public void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
    }

    /**
     * Flushes the writer and closes it only if this handler owns it. A close failure is reported
     * to the error manager instead of being thrown.
     */
    @Override
    public void close() {
        flush();
        if (closeWriter) {
            try {
                writer.close();
            } catch (IOException e) {
                reportError(null, e, ErrorManager.CLOSE_FAILURE);
            }
        }
    }
}
