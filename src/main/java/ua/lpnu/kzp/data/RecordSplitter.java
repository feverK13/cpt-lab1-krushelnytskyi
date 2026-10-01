package ua.lpnu.kzp.data;

/**
 * Splits a single CSV line into its raw, untrimmed fields.
 *
 * <p>The separator is {@code ;} and the split limit is {@code -1}, so empty trailing fields are
 * preserved and a trailing separator yields an extra empty field instead of being ignored.</p>
 */
public final class RecordSplitter {

    /** Number of fields a well-formed record must have. */
    public static final int FIELD_COUNT = 5;

    private static final String SEPARATOR = ";";

    private RecordSplitter() {
    }

    /**
     * Splits the given line into raw fields.
     *
     * @param line line to split
     * @return the fields in file order, never trimmed and never collapsed
     */
    public static String[] split(String line) {
        return line.split(SEPARATOR, -1);
    }
}
