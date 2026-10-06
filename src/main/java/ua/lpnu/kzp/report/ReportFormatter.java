package ua.lpnu.kzp.report;

import ua.lpnu.kzp.metrics.Metrics;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Builds the Ukrainian report text shown on the console and written to the output file.
 */
public final class ReportFormatter {

    private static final String ROW = "%-34s %16s%n";
    private static final int TABLE_WIDTH = 51;

    private ReportFormatter() {
    }

    /**
     * Builds the whole report text.
     *
     * <p>The report holds a title, the input path, a fixed-width table with the four metrics (or
     * a "no valid records" message instead), the number of skipped lines and, if there are any,
     * the skipped-line messages in file order. Numbers use {@link Locale#ROOT} and two decimals;
     * lines end with the platform line separator.</p>
     *
     * @param input           path of the processed input file
     * @param metrics         computed metrics, or empty if there were no valid records
     * @param skippedMessages Ukrainian skipped-line messages, in file order
     * @return the report text, ending with a line separator
     */
    public static String format(Path input, Optional<Metrics> metrics, List<String> skippedMessages) {
        StringBuilder report = new StringBuilder();
        report.append(String.format(Locale.ROOT, "Звіт: електронний магазин%n"));
        report.append(String.format(Locale.ROOT, "Вхідний файл: %s%n%n", input));

        if (metrics.isPresent()) {
            Metrics m = metrics.get();
            report.append(String.format(Locale.ROOT, ROW, "Показник", "Значення"));
            report.append(String.format(Locale.ROOT, "%s%n", "-".repeat(TABLE_WIDTH)));
            report.append(String.format(
                    Locale.ROOT, ROW, "Кількість коректних записів", String.valueOf(m.validCount())));
            report.append(String.format(
                    Locale.ROOT, ROW, "Середня ціна",
                    String.format(Locale.ROOT, "%.2f грн", m.averagePrice())));
            report.append(String.format(
                    Locale.ROOT, ROW, "Найдовша гарантія, міс.",
                    String.valueOf(m.longestWarrantyMonths())));
            report.append(String.format(
                    Locale.ROOT, ROW, "Загальний запас, шт.", String.valueOf(m.totalStock())));
        } else {
            report.append(String.format(Locale.ROOT, "Немає жодного коректного запису.%n"));
        }

        report.append(String.format(Locale.ROOT, "%nПропущено рядків: %d%n", skippedMessages.size()));
        if (!skippedMessages.isEmpty()) {
            report.append(String.format(Locale.ROOT, "%nПропущені рядки:%n"));
            for (String message : skippedMessages) {
                report.append(String.format(Locale.ROOT, "%s%n", message));
            }
        }
        return report.toString();
    }
}
