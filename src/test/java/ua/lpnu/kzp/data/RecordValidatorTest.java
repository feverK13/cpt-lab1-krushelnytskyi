package ua.lpnu.kzp.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link RecordValidator}: every validation rule, its boundaries, the order in
 * which rules are applied and the rendered messages.
 */
class RecordValidatorTest {

    private static final String VALID_PREFIX = "Монітор;Монітори;";

    private static LineResult.Valid valid(String line) {
        return assertInstanceOf(LineResult.Valid.class, RecordValidator.validate(1, line));
    }

    private static LineResult.Invalid invalid(String line) {
        return assertInstanceOf(LineResult.Invalid.class, RecordValidator.validate(1, line));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Монітор Dell S2425H;Монітори;6499.00;36;12",
        "Монітор;Монітори;0.01;0;0",
        "Монітор;Монітори;1;1;1",
        "Монітор;Монітори;2147483647;2147483647;2147483647",
        "Монітор;Монітори;1.5;-0;0"
    })
    void acceptsWellFormedRecords(String line) {
        assertEquals(5, valid(line).fields().size());
    }

    @Test
    void trimsEveryFieldAndKeepsCyrillic() {
        LineResult.Valid result =
                valid("  Пральна машина Bosch  ;  Побутова техніка  ;  18750.00  ;  36  ;  3  ");

        assertEquals(
                List.of("Пральна машина Bosch", "Побутова техніка", "18750.00", "36", "3"),
                result.fields());
    }

    @ParameterizedTest
    @ValueSource(strings = {" 5 ", "\t5\t", " \t5\t "})
    void acceptsNumbersSurroundedBySpacesOrTabs(String number) {
        LineResult.Valid result =
                valid("Монітор\t;\tМонітори;" + number + ";" + number + ";" + number);

        assertEquals(List.of("Монітор", "Монітори", "5", "5", "5"), result.fields());
    }

    @Test
    void fieldsOfAValidRecordAreImmutable() {
        List<String> fields = valid("Монітор;Монітори;1;1;1").fields();

        assertThrows(UnsupportedOperationException.class, () -> fields.set(0, "x"));
    }

    @Test
    void keepsTheGivenLineNumber() {
        assertEquals(42, RecordValidator.validate(42, "Монітор;Монітори;1;1;1").lineNumber());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", " \t "})
    void rejectsBlankLineBeforeAnyOtherCheck(String line) {
        LineResult.Invalid result = invalid(line);

        assertEquals(ValidationError.EMPTY_LINE, result.error());
        assertNull(result.field());
        assertNull(result.detail());
    }

    static Stream<Arguments> wrongFieldCounts() {
        return Stream.of(
                Arguments.of("a", 1),
                Arguments.of("a;b;c;d", 4),
                Arguments.of("a;b;c;d;e;", 6),
                Arguments.of("Монітор;Монітори;1;1;1;", 6),
                Arguments.of("a;b;c;d;e;f;g", 7));
    }

    @ParameterizedTest
    @MethodSource("wrongFieldCounts")
    void rejectsWrongFieldCountWithActualCount(String line, int expectedCount) {
        LineResult.Invalid result = invalid(line);

        assertEquals(ValidationError.FIELD_COUNT, result.error());
        assertNull(result.field());
        assertEquals(String.valueOf(expectedCount), result.detail());
    }

    static Stream<Arguments> emptyFields() {
        return Stream.of(
                Arguments.of(";Монітори;1;1;1", RecordField.NAME),
                Arguments.of("   ;Монітори;1;1;1", RecordField.NAME),
                Arguments.of("Монітор;;1;1;1", RecordField.CATEGORY),
                Arguments.of("Монітор;   ;1;1;1", RecordField.CATEGORY),
                Arguments.of("Монітор;Монітори;;1;1", RecordField.PRICE),
                Arguments.of("Монітор;Монітори; ;1;1", RecordField.PRICE),
                Arguments.of("Монітор;Монітори;1;;1", RecordField.WARRANTY_MONTHS),
                Arguments.of("Монітор;Монітори;1;1;", RecordField.STOCK),
                Arguments.of("Монітор;Монітори;1;1;  ", RecordField.STOCK));
    }

    @ParameterizedTest
    @MethodSource("emptyFields")
    void rejectsEmptyField(String line, RecordField expectedField) {
        LineResult.Invalid result = invalid(line);

        assertEquals(ValidationError.EMPTY_FIELD, result.error());
        assertEquals(expectedField, result.field());
        assertNull(result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "абв", "12,5", "1e3", "1E3", "1e+3", "NaN", "Infinity", "-Infinity",
        "12.5f", "12.5d", "+5", ".5", "5.", "1 000", "1.2.3", "0x10", "--1"
    })
    void rejectsPriceThatIsNotAPlainDecimal(String price) {
        LineResult.Invalid result = invalid(VALID_PREFIX + price + ";1;1");

        assertEquals(ValidationError.NOT_A_NUMBER, result.error());
        assertEquals(RecordField.PRICE, result.field());
        assertEquals(price, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "0.00", "-0", "-0.0"})
    void rejectsNonPositivePrice(String price) {
        LineResult.Invalid result = invalid(VALID_PREFIX + price + ";1;1");

        assertEquals(ValidationError.NOT_POSITIVE, result.error());
        assertEquals(RecordField.PRICE, result.field());
        assertEquals(price, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-0.01", "-24999.00"})
    void rejectsNegativePrice(String price) {
        LineResult.Invalid result = invalid(VALID_PREFIX + price + ";1;1");

        assertEquals(ValidationError.NEGATIVE, result.error());
        assertEquals(RecordField.PRICE, result.field());
        assertEquals(price, result.detail());
    }

    @Test
    void rejectsPriceThatDoesNotFitIntoDouble() {
        String price = "1".repeat(400);

        LineResult.Invalid result = invalid(VALID_PREFIX + price + ";1;1");

        assertEquals(ValidationError.TOO_LARGE, result.error());
        assertEquals(RecordField.PRICE, result.field());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.5", "24.0", "0.5"})
    void rejectsNonIntegerWarranty(String warranty) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;" + warranty + ";1");

        assertEquals(ValidationError.NOT_AN_INTEGER, result.error());
        assertEquals(RecordField.WARRANTY_MONTHS, result.field());
        assertEquals(warranty, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.5", "1.0", "0.5"})
    void rejectsNonIntegerStock(String stock) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;1;" + stock);

        assertEquals(ValidationError.NOT_AN_INTEGER, result.error());
        assertEquals(RecordField.STOCK, result.field());
        assertEquals(stock, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"абв", "+5", "1e3", "NaN", "12,5", "24m"})
    void rejectsWarrantyThatIsNotANumber(String warranty) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;" + warranty + ";1");

        assertEquals(ValidationError.NOT_A_NUMBER, result.error());
        assertEquals(RecordField.WARRANTY_MONTHS, result.field());
    }

    @ParameterizedTest
    @ValueSource(strings = {"абв", "+5", "1e3", "12,5"})
    void rejectsStockThatIsNotANumber(String stock) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;1;" + stock);

        assertEquals(ValidationError.NOT_A_NUMBER, result.error());
        assertEquals(RecordField.STOCK, result.field());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-24", "-2147483649"})
    void rejectsNegativeWarranty(String warranty) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;" + warranty + ";1");

        assertEquals(ValidationError.NEGATIVE, result.error());
        assertEquals(RecordField.WARRANTY_MONTHS, result.field());
        assertEquals(warranty, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "-3", "-2147483649"})
    void rejectsNegativeStock(String stock) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;1;" + stock);

        assertEquals(ValidationError.NEGATIVE, result.error());
        assertEquals(RecordField.STOCK, result.field());
        assertEquals(stock, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2147483648", "99999999999999999999"})
    void rejectsWarrantyThatOverflowsInt(String warranty) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;" + warranty + ";1");

        assertEquals(ValidationError.TOO_LARGE, result.error());
        assertEquals(RecordField.WARRANTY_MONTHS, result.field());
        assertEquals(warranty, result.detail());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2147483648", "99999999999999999999"})
    void rejectsStockThatOverflowsInt(String stock) {
        LineResult.Invalid result = invalid(VALID_PREFIX + "1;1;" + stock);

        assertEquals(ValidationError.TOO_LARGE, result.error());
        assertEquals(RecordField.STOCK, result.field());
        assertEquals(stock, result.detail());
    }

    static Stream<Arguments> firstFailingField() {
        return Stream.of(
                Arguments.of(";;абв;абв;абв", RecordField.NAME),
                Arguments.of("Монітор;;абв;абв;абв", RecordField.CATEGORY),
                Arguments.of("Монітор;Монітори;абв;абв;абв", RecordField.PRICE),
                Arguments.of("Монітор;Монітори;1;абв;абв", RecordField.WARRANTY_MONTHS),
                Arguments.of("Монітор;Монітори;1;1;абв", RecordField.STOCK));
    }

    @ParameterizedTest
    @MethodSource("firstFailingField")
    void reportsOnlyTheFirstFailingField(String line, RecordField expectedField) {
        assertEquals(expectedField, invalid(line).field());
    }

    static Stream<Arguments> ukrainianMessages() {
        return Stream.of(
                Arguments.of(7, "   ", "Рядок 7: порожній рядок"),
                Arguments.of(2, "a;b;c;d", "Рядок 2: очікується 5 полів, отримано 4"),
                Arguments.of(3, ";Монітори;1;1;1", "Рядок 3: назва: порожнє поле"),
                Arguments.of(4, "Монітор;;1;1;1", "Рядок 4: категорія: порожнє поле"),
                Arguments.of(
                        5, "Монітор;Монітори;абв;1;1", "Рядок 5: ціна: не є числом: \"абв\""),
                Arguments.of(
                        6, "Монітор;Монітори;0;1;1", "Рядок 6: ціна: має бути більше нуля: \"0\""),
                Arguments.of(
                        8, "Монітор;Монітори;-1;1;1", "Рядок 8: ціна: від'ємне значення: \"-1\""),
                Arguments.of(
                        9,
                        "Монітор;Монітори;1;12.5;1",
                        "Рядок 9: гарантія: має бути цілим числом: \"12.5\""),
                Arguments.of(
                        10, "Монітор;Монітори;1;1;-3", "Рядок 10: запас: від'ємне значення: \"-3\""),
                Arguments.of(
                        11,
                        "Монітор;Монітори;1;1;2147483648",
                        "Рядок 11: запас: завелике значення: \"2147483648\""));
    }

    @ParameterizedTest
    @MethodSource("ukrainianMessages")
    void rendersUkrainianMessage(int lineNumber, String line, String expectedMessage) {
        LineResult.Invalid result = assertInstanceOf(
                LineResult.Invalid.class, RecordValidator.validate(lineNumber, line));

        assertEquals(expectedMessage, result.message());
    }

    static Stream<Arguments> englishReasons() {
        return Stream.of(
                Arguments.of("   ", null, "empty line"),
                Arguments.of("a;b;c;d", null, "expected 5 fields, got 4"),
                Arguments.of(";Монітори;1;1;1", "name", "empty field"),
                Arguments.of("Монітор;;1;1;1", "category", "empty field"),
                Arguments.of("Монітор;Монітори;абв;1;1", "price", "not a number: \"абв\""),
                Arguments.of("Монітор;Монітори;0;1;1", "price", "must be greater than zero: \"0\""),
                Arguments.of(
                        "Монітор;Монітори;1;12.5;1",
                        "warrantyMonths",
                        "must be an integer: \"12.5\""),
                Arguments.of("Монітор;Монітори;1;1;-3", "stock", "negative value: \"-3\""),
                Arguments.of(
                        "Монітор;Монітори;1;1;2147483648",
                        "stock",
                        "value is too large: \"2147483648\""));
    }

    @ParameterizedTest
    @MethodSource("englishReasons")
    void rendersEnglishReasonForTheLog(String line, String expectedField, String expectedReason) {
        LineResult.Invalid result = invalid(line);

        assertEquals(expectedField, result.logField());
        assertEquals(expectedReason, result.logReason());
    }
}
