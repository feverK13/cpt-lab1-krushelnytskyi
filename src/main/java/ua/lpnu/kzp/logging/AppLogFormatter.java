package ua.lpnu.kzp.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Formats log records as {@code ISO-8601 time | LEVEL | message}, where {@code message} already
 * contains the {@code Class.method | line=N field=X | text} body built by {@link AppLogger}.
 */
final class AppLogFormatter extends Formatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx", Locale.ROOT);

    /**
     * Renders one record as {@code timestamp | LEVEL | message}, followed by the stack trace of
     * the attached exception, if any.
     *
     * @param record the record to render; its message already holds the location, line and field
     * @return the rendered record, ending with the platform line separator
     */
    @Override
    public String format(LogRecord record) {
        String timestamp = TIMESTAMP_FORMATTER.format(record.getInstant().atZone(ZoneId.systemDefault()));
        String level = mapLevel(record.getLevel());

        StringBuilder line = new StringBuilder();
        line.append(timestamp).append(" | ").append(level).append(" | ").append(formatMessage(record));

        if (record.getThrown() != null) {
            line.append(System.lineSeparator());
            StringWriter stackTrace = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(stackTrace));
            line.append(stackTrace);
        }
        line.append(System.lineSeparator());
        return line.toString();
    }

    /**
     * Maps a {@code java.util.logging} level to the label used in the log file.
     *
     * @param level the record level
     * @return {@code ERROR} for SEVERE and above, {@code WARN} for WARNING, {@code INFO} otherwise
     */
    private static String mapLevel(Level level) {
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return "ERROR";
        }
        if (level.intValue() >= Level.WARNING.intValue()) {
            return "WARN";
        }
        return "INFO";
    }
}
