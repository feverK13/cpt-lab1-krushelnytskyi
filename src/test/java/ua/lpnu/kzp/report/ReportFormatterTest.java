package ua.lpnu.kzp.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ua.lpnu.kzp.metrics.Metrics;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReportFormatter}.
 */
class ReportFormatterTest {

    private static final Path INPUT = Path.of("data", "input.csv");
    private static final String NL = System.lineSeparator();

    private static String row(String label, String value) {
        return label + " ".repeat(34 - label.length()) + " " + " ".repeat(16 - value.length()) + value;
    }

    private static String lines(String... lines) {
        return String.join(NL, lines) + NL;
    }

    @Test
    void reportForSmallDatasetMatchesExactly() {
        String report = ReportFormatter.format(
                INPUT,
                Optional.of(new Metrics(3, 200.25, 24, 15L)),
                List.of("Рядок 2: порожній рядок", "Рядок 5: ціна: не є числом: \"абв\""));

        assertEquals(
                lines(
                        "Звіт: електронний магазин",
                        "Вхідний файл: " + INPUT,
                        "",
                        row("Показник", "Значення"),
                        "-".repeat(51),
                        row("Кількість коректних записів", "3"),
                        row("Середня ціна", "200.25 грн"),
                        row("Найдовша гарантія, міс.", "24"),
                        row("Загальний запас, шт.", "15"),
                        "",
                        "Пропущено рядків: 2",
                        "",
                        "Пропущені рядки:",
                        "Рядок 2: порожній рядок",
                        "Рядок 5: ціна: не є числом: \"абв\""),
                report);
    }

    @Test
    void reportWithoutSkippedLinesHasNoSkippedSection() {
        String report = ReportFormatter.format(
                INPUT, Optional.of(new Metrics(1, 6499.0, 36, 12L)), List.of());

        assertTrue(report.endsWith(NL + "Пропущено рядків: 0" + NL), report);
        assertFalse(report.contains("Пропущені рядки:"));
    }

    @Test
    void reportWithoutValidRecordsReplacesTableWithMessage() {
        String report = ReportFormatter.format(
                INPUT, Optional.empty(), List.of("Рядок 1: порожній рядок"));

        assertEquals(
                lines(
                        "Звіт: електронний магазин",
                        "Вхідний файл: " + INPUT,
                        "",
                        "Немає жодного коректного запису.",
                        "",
                        "Пропущено рядків: 1",
                        "",
                        "Пропущені рядки:",
                        "Рядок 1: порожній рядок"),
                report);
    }

    @ParameterizedTest
    @CsvSource({
        "1.3333333333, 1.33 грн",
        "200.25,       200.25 грн",
        "14030.62375,  14030.62 грн",
        "0.005,        0.01 грн"
    })
    void averagePriceHasTwoDecimals(double average, String expected) {
        String report = ReportFormatter.format(
                INPUT, Optional.of(new Metrics(1, average, 0, 0L)), List.of());

        assertTrue(report.contains(row("Середня ціна", expected)), report);
    }

    @Test
    void formattingIgnoresDefaultLocale() {
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
        try {
            String report = ReportFormatter.format(
                    INPUT, Optional.of(new Metrics(3, 200.25, 24, 15L)), List.of());

            assertTrue(report.contains("200.25 грн"), report);
            assertFalse(report.contains("200,25"));
        } finally {
            Locale.setDefault(original);
        }
    }
}
