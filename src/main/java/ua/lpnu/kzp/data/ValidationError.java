package ua.lpnu.kzp.data;

import java.util.Locale;

/**
 * Reason why a line was rejected.
 *
 * <p>Every reason carries two templates: a Ukrainian one for the message printed to the user and
 * an English one for the log record. A template may contain a single {@code %s} placeholder for
 * the offending value or count; reasons without a placeholder simply ignore the detail.</p>
 */
public enum ValidationError {

    /** The line is empty or contains whitespace only. */
    EMPTY_LINE("порожній рядок", "empty line"),

    /** The line does not contain exactly five fields; the detail is the actual count. */
    FIELD_COUNT("очікується 5 полів, отримано %s", "expected 5 fields, got %s"),

    /** The field is empty after trimming. */
    EMPTY_FIELD("порожнє поле", "empty field"),

    /** The field does not match the accepted number format. */
    NOT_A_NUMBER("не є числом: \"%s\"", "not a number: \"%s\""),

    /** The field is a number but not an integer. */
    NOT_AN_INTEGER("має бути цілим числом: \"%s\"", "must be an integer: \"%s\""),

    /** The field is a negative number where only non-negative values are allowed. */
    NEGATIVE("від'ємне значення: \"%s\"", "negative value: \"%s\""),

    /** The field is zero where a strictly positive value is required. */
    NOT_POSITIVE("має бути більше нуля: \"%s\"", "must be greater than zero: \"%s\""),

    /** The field is a number that does not fit into the target type. */
    TOO_LARGE("завелике значення: \"%s\"", "value is too large: \"%s\"");

    private final String ukrainianTemplate;
    private final String englishTemplate;

    ValidationError(String ukrainianTemplate, String englishTemplate) {
        this.ukrainianTemplate = ukrainianTemplate;
        this.englishTemplate = englishTemplate;
    }

    /**
     * Renders the Ukrainian reason shown to the user.
     *
     * @param detail offending value or count, may be {@code null} for reasons without a detail
     * @return the rendered reason
     */
    public String ukrainian(String detail) {
        return String.format(Locale.ROOT, ukrainianTemplate, detail);
    }

    /**
     * Renders the English reason written to the log.
     *
     * @param detail offending value or count, may be {@code null} for reasons without a detail
     * @return the rendered reason
     */
    public String english(String detail) {
        return String.format(Locale.ROOT, englishTemplate, detail);
    }
}
