package ua.lpnu.kzp.data;

import ua.lpnu.kzp.logging.AppLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the input lines and splits them into valid records and skipped-line messages.
 */
public final class InputProcessor {

    private InputProcessor() {
    }

    /**
     * Outcome of processing all input lines.
     *
     * @param records         trimmed fields of every valid record, in file order
     * @param skippedMessages Ukrainian messages for every skipped line, in file order
     */
    public record ProcessedInput(List<String[]> records, List<String> skippedMessages) {

        /**
         * Creates the outcome with defensive copies of both lists.
         */
        public ProcessedInput {
            records = List.copyOf(records);
            skippedMessages = List.copyOf(skippedMessages);
        }
    }

    /**
     * Validates every line; a skipped line never stops processing.
     *
     * @param lines  raw input lines, in file order
     * @param logger logger receiving one warning per skipped line and the validation summary
     * @return the valid records and the skipped-line messages
     */
    public static ProcessedInput process(List<String> lines, AppLogger logger) {
        List<String[]> records = new ArrayList<>();
        List<String> skippedMessages = new ArrayList<>();

        for (int index = 0; index < lines.size(); index++) {
            LineResult result = RecordValidator.validate(index + 1, lines.get(index));
            switch (result) {
                case LineResult.Valid valid -> records.add(valid.fields().toArray(new String[0]));
                case LineResult.Invalid invalid -> {
                    skippedMessages.add(invalid.message());
                    logger.warn(
                            "InputProcessor.process",
                            invalid.lineNumber(),
                            invalid.logField(),
                            "Skipped line: " + invalid.logReason());
                }
            }
        }

        logger.info(
                "InputProcessor.process",
                "Validation summary: " + records.size() + " valid, " + skippedMessages.size() + " skipped");
        return new ProcessedInput(records, skippedMessages);
    }
}
