package ua.lpnu.kzp.metrics;

import java.util.Locale;

/**
 * The four report metrics computed over the valid records.
 *
 * @param validCount            number of valid records
 * @param averagePrice          average price, in UAH
 * @param longestWarrantyMonths maximum warranty length, in months
 * @param totalStock            sum of the stock of every valid record
 */
public record Metrics(int validCount, double averagePrice, int longestWarrantyMonths, long totalStock) {

    /**
     * Renders the metrics as a single English line for the log.
     *
     * @return e.g. {@code valid=8 averagePrice=14030.62 longestWarrantyMonths=36 totalStock=75}
     */
    public String summary() {
        return String.format(
                Locale.ROOT,
                "valid=%d averagePrice=%.2f longestWarrantyMonths=%d totalStock=%d",
                validCount,
                averagePrice,
                longestWarrantyMonths,
                totalStock);
    }
}
