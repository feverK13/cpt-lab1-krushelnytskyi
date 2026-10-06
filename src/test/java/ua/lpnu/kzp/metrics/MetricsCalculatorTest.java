package ua.lpnu.kzp.metrics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MetricsCalculator}.
 */
class MetricsCalculatorTest {

    private static final double DELTA = 0.0001;

    private static String[] record(String price, String warranty, String stock) {
        return new String[] {"Товар", "Категорія", price, warranty, stock};
    }

    static Stream<Arguments> datasets() {
        return Stream.of(
                Arguments.of(
                        "known dataset",
                        List.of(
                                record("100.00", "12", "5"),
                                record("200.50", "24", "0"),
                                record("300.25", "6", "10")),
                        new Metrics(3, 200.25, 24, 15L)),
                Arguments.of(
                        "single record",
                        List.<String[]>of(record("6499.00", "36", "12")),
                        new Metrics(1, 6499.00, 36, 12L)),
                Arguments.of(
                        "average needing rounding",
                        List.of(
                                record("1.00", "0", "1"),
                                record("1.00", "0", "1"),
                                record("2.00", "0", "1")),
                        new Metrics(3, 4.0 / 3.0, 0, 3L)),
                Arguments.of(
                        "stock sum above Integer.MAX_VALUE",
                        List.of(
                                record("1", "1", String.valueOf(Integer.MAX_VALUE)),
                                record("1", "1", String.valueOf(Integer.MAX_VALUE)),
                                record("1", "1", String.valueOf(Integer.MAX_VALUE - 1))),
                        new Metrics(3, 1.0, 1, 3L * Integer.MAX_VALUE - 1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("datasets")
    void calculatesTheFourMetrics(String name, List<String[]> records, Metrics expected) {
        Metrics actual = MetricsCalculator.calculate(records).orElseThrow();

        assertEquals(expected.validCount(), actual.validCount());
        assertEquals(expected.averagePrice(), actual.averagePrice(), DELTA);
        assertEquals(expected.longestWarrantyMonths(), actual.longestWarrantyMonths());
        assertEquals(expected.totalStock(), actual.totalStock());
    }

    @Test
    void zeroValidRecordsYieldNoMetrics() {
        Optional<Metrics> metrics = MetricsCalculator.calculate(List.of());

        assertTrue(metrics.isEmpty());
    }

    @Test
    void summaryListsTheFourMetricsLocaleIndependently() {
        Metrics metrics = new Metrics(8, 14030.62375, 36, 75L);

        assertEquals(
                "valid=8 averagePrice=14030.62 longestWarrantyMonths=36 totalStock=75",
                metrics.summary());
    }
}
