package ua.lpnu.kzp.data;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates one line of the input file against the record format
 * {@code name;category;price;warrantyMonths;stock}.
 *
 * <p>Checks run in a fixed order and the first failing one wins, so every rejected line carries
 * exactly one reason: blank line, field count, empty name, empty category, then price, warranty
 * and stock. Numbers must be written in plain decimal notation: no exponent, no type suffix, no
 * {@code NaN} or {@code Infinity}, no thousands separators.</p>
 */
public final class RecordValidator {

    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern INTEGER = Pattern.compile("-?\\d+");
    private static final BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final List<RecordField> INTEGER_FIELDS =
            List.of(RecordField.WARRANTY_MONTHS, RecordField.STOCK);

    private RecordValidator() {
    }

    /**
     * Validates a single line.
     *
     * @param lineNumber 1-based physical line number, used in the reported message
     * @param line       raw line, without its line separator
     * @return a {@link LineResult.Valid} with the trimmed fields, or a {@link LineResult.Invalid}
     *         describing the first failing check
     */
    public static LineResult validate(int lineNumber, String line) {
        if (line.isBlank()) {
            return new LineResult.Invalid(lineNumber, null, ValidationError.EMPTY_LINE, null);
        }

        String[] raw = RecordSplitter.split(line);
        if (raw.length != RecordSplitter.FIELD_COUNT) {
            return new LineResult.Invalid(
                    lineNumber, null, ValidationError.FIELD_COUNT, String.valueOf(raw.length));
        }

        List<String> fields = new ArrayList<>(raw.length);
        for (String field : raw) {
            fields.add(field.strip());
        }

        for (RecordField field : List.of(RecordField.NAME, RecordField.CATEGORY)) {
            if (fields.get(field.index()).isEmpty()) {
                return new LineResult.Invalid(lineNumber, field, ValidationError.EMPTY_FIELD, null);
            }
        }

        String price = fields.get(RecordField.PRICE.index());
        ValidationError priceError = validatePrice(price);
        if (priceError != null) {
            return new LineResult.Invalid(lineNumber, RecordField.PRICE, priceError, detail(priceError, price));
        }

        for (RecordField field : INTEGER_FIELDS) {
            String value = fields.get(field.index());
            ValidationError error = validateNonNegativeInt(value);
            if (error != null) {
                return new LineResult.Invalid(lineNumber, field, error, detail(error, value));
            }
        }

        return new LineResult.Valid(lineNumber, fields);
    }

    /**
     * Checks a trimmed price: plain decimal, finite as {@code double}, strictly positive.
     * {@code -0} and {@code -0.0} are rejected as not positive, not as negative.
     *
     * @param value the trimmed price field
     * @return the first failing reason, or {@code null} if the price is valid
     */
    private static ValidationError validatePrice(String value) {
        if (value.isEmpty()) {
            return ValidationError.EMPTY_FIELD;
        }
        if (!DECIMAL.matcher(value).matches()) {
            return ValidationError.NOT_A_NUMBER;
        }
        double price = Double.parseDouble(value);
        if (!Double.isFinite(price)) {
            return ValidationError.TOO_LARGE;
        }
        if (price < 0) {
            return ValidationError.NEGATIVE;
        }
        if (price <= 0) {
            return ValidationError.NOT_POSITIVE;
        }
        return null;
    }

    /**
     * Checks a trimmed integer field: plain integer, not negative, at most
     * {@link Integer#MAX_VALUE}. The value is parsed as {@link BigInteger} so overflow is
     * detected instead of wrapping. A decimal such as {@code 12.5} is reported as not an integer
     * rather than not a number.
     *
     * @param value the trimmed warranty or stock field
     * @return the first failing reason, or {@code null} if the value is valid
     */
    private static ValidationError validateNonNegativeInt(String value) {
        if (value.isEmpty()) {
            return ValidationError.EMPTY_FIELD;
        }
        if (!INTEGER.matcher(value).matches()) {
            return DECIMAL.matcher(value).matches()
                    ? ValidationError.NOT_AN_INTEGER
                    : ValidationError.NOT_A_NUMBER;
        }
        BigInteger parsed = new BigInteger(value);
        if (parsed.signum() < 0) {
            return ValidationError.NEGATIVE;
        }
        if (parsed.compareTo(MAX_INT) > 0) {
            return ValidationError.TOO_LARGE;
        }
        return null;
    }

    private static String detail(ValidationError error, String value) {
        return error == ValidationError.EMPTY_FIELD ? null : value;
    }
}
