package ua.lpnu.kzp.logging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Self-documenting application logger built on {@code java.util.logging}.
 *
 * <p>Every record is written as {@code ISO-8601 time | LEVEL | Class.method | line=N field=X |
 * message}, with {@code -} standing in for an absent line or field. Records always go to the log
 * file; when {@code verbose} is enabled at creation time they are also mirrored to the given
 * stderr stream. If the log file cannot be created, a warning is printed to stderr once and the
 * application continues without file logging.</p>
 */
public final class AppLogger implements AutoCloseable {

    private static final AtomicLong INSTANCE_COUNTER = new AtomicLong();

    private final Logger logger;
    private final List<Handler> handlers;

    private AppLogger(Logger logger, List<Handler> handlers) {
        this.logger = logger;
        this.handlers = handlers;
    }

    /**
     * Creates a logger writing to {@code logFile}, optionally mirroring records to {@code err}.
     *
     * @param logFile path to the log file; parent directories are created as needed and the file
     *                is overwritten
     * @param verbose whether to also mirror every record to {@code err}
     * @param err     stream used both for the verbose mirror and for the degraded-mode warning
     * @return a ready-to-use logger; never {@code null}, even if the log file could not be created
     */
    public static AppLogger create(Path logFile, boolean verbose, PrintStream err) {
        Logger logger = Logger.getLogger(AppLogger.class.getName() + "." + INSTANCE_COUNTER.incrementAndGet());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        List<Handler> handlers = new ArrayList<>();
        try {
            Path parent = logFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Writer fileWriter = Files.newBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            Handler fileHandler = new WriterHandler(fileWriter, new AppLogFormatter(), true);
            logger.addHandler(fileHandler);
            handlers.add(fileHandler);
        } catch (IOException e) {
            err.printf(
                    Locale.ROOT,
                    "Попередження: не вдалося створити файл журналу %s (%s). Журнал файлу вимкнено.%n",
                    logFile,
                    e.getMessage());
        }

        if (verbose) {
            Writer mirrorWriter = new OutputStreamWriter(err, StandardCharsets.UTF_8);
            Handler mirrorHandler = new WriterHandler(mirrorWriter, new AppLogFormatter(), false);
            logger.addHandler(mirrorHandler);
            handlers.add(mirrorHandler);
        }

        return new AppLogger(logger, handlers);
    }

    /**
     * Logs an informational message, e.g. startup, arguments or the run summary.
     *
     * @param location "Class.method" of the caller
     * @param message  message text
     */
    public void info(String location, String message) {
        log(Level.INFO, location, null, null, message, null);
    }

    /**
     * Logs a warning about a skipped line, e.g. an invalid CSV record.
     *
     * @param location "Class.method" of the caller
     * @param line     1-based line number, or {@code null} if not applicable
     * @param field    offending field name, or {@code null} if not applicable
     * @param message  reason for the warning
     */
    public void warn(String location, Integer line, String field, String message) {
        log(Level.WARNING, location, line, field, message, null);
    }

    /**
     * Logs an error together with its stack trace.
     *
     * @param location  "Class.method" of the caller
     * @param message   message text
     * @param throwable the exception to record
     */
    public void error(String location, String message, Throwable throwable) {
        log(Level.SEVERE, location, null, null, message, throwable);
    }

    private void log(Level level, String location, Integer line, String field, String message, Throwable throwable) {
        String lineText = line == null ? "-" : String.valueOf(line);
        String fieldText = field == null ? "-" : field;
        String body = location + " | line=" + lineText + " field=" + fieldText + " | " + message;

        LogRecord record = new LogRecord(level, body);
        if (throwable != null) {
            record.setThrown(throwable);
        }
        logger.log(record);
    }

    /**
     * Closes and flushes every handler attached to this logger.
     */
    @Override
    public void close() {
        for (Handler handler : handlers) {
            handler.close();
            logger.removeHandler(handler);
        }
    }
}
