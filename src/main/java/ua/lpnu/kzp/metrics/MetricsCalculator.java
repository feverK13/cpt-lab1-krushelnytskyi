package ua.lpnu.kzp.metrics;

import ua.lpnu.kzp.data.RecordField;

import java.util.List;
import java.util.Optional;

/**
 * Computes the report metrics over validated records.
 */
public final class MetricsCalculator {

    private MetricsCalculator() {
    }

    /**
     * Computes the count, average price, longest warranty and total stock.
     *
     * <p>Every record must hold five trimmed fields that already passed validation. The stock is
     * summed as {@code long}, so several values near {@link Integer#MAX_VALUE} do not overflow.</p>
     *
     * @param records valid records, each a five-element field array in {@link RecordField} order
     * @return the metrics, or an empty optional if there are no records
     */
    public static Optional<Metrics> calculate(List<String[]> records) {
        if (records.isEmpty()) {
            return Optional.empty();
        }

        double priceSum = 0;
        int longestWarranty = Integer.MIN_VALUE;
        long totalStock = 0;
        for (String[] fields : records) {
            priceSum += Double.parseDouble(fields[RecordField.PRICE.index()]);
            longestWarranty = Math.max(
                    longestWarranty, Integer.parseInt(fields[RecordField.WARRANTY_MONTHS.index()]));
            totalStock += Integer.parseInt(fields[RecordField.STOCK.index()]);
        }

        int count = records.size();
        return Optional.of(new Metrics(count, priceSum / count, longestWarranty, totalStock));
    }
}
