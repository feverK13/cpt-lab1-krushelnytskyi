package ua.lpnu.kzp.data;

import java.util.List;
import java.util.Locale;

/**
 * Outcome of validating a single line of the input file.
 *
 * <p>A valid line yields its five trimmed fields; an invalid line yields everything needed to
 * report it: the physical line number, the offending field (absent for whole-line problems), the
 * reason, and the offending value.</p>
 */
public sealed interface LineResult {

    /**
     * Returns the 1-based physical line number this result describes.
     *
     * @return the line number
     */
    int lineNumber();

    /**
     * A line that passed every validation rule.
     *
     * @param lineNumber 1-based physical line number
     * @param fields     the five trimmed fields, in record order
     */
    record Valid(int lineNumber, List<String> fields) implements LineResult {

        /**
         * Creates a valid result with a defensive copy of the fields.
         */
        public Valid {
            fields = List.copyOf(fields);
        }
    }

    /**
     * A line that was rejected by the first failing validation rule.
     *
     * @param lineNumber 1-based physical line number
     * @param field      the offending field, or {@code null} for a whole-line problem
     * @param error      the reason the line was rejected
     * @param detail     the offending value or count, or {@code null} if there is none
     */
    record Invalid(int lineNumber, RecordField field, ValidationError error, String detail)
            implements LineResult {

        /**
         * Renders the Ukrainian message for the skipped line.
         *
         * @return {@code Рядок N: <поле>: <причина>}, without the field part for whole-line
         *         problems
         */
        public String message() {
            String reason = error.ukrainian(detail);
            if (field == null) {
                return String.format(Locale.ROOT, "Рядок %d: %s", lineNumber, reason);
            }
            return String.format(Locale.ROOT, "Рядок %d: %s: %s", lineNumber, field.label(), reason);
        }

        /**
         * Returns the English reason for the log record.
         *
         * @return the rendered English reason
         */
        public String logReason() {
            return error.english(detail);
        }

        /**
         * Returns the English field name for the log record.
         *
         * @return the CSV field name, or {@code null} for a whole-line problem
         */
        public String logField() {
            return field == null ? null : field.csvName();
        }
    }
}
