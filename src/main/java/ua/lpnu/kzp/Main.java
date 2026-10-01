package ua.lpnu.kzp;

import ua.lpnu.kzp.cli.CliParseResult;
import ua.lpnu.kzp.cli.CliParser;
import ua.lpnu.kzp.data.CsvReader;
import ua.lpnu.kzp.data.LineResult;
import ua.lpnu.kzp.data.RecordValidator;
import ua.lpnu.kzp.logging.AppLogger;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Entry point of the console application for the "Electronics store" domain.
 *
 * <p>The class is final and has a private constructor: it only hosts {@code main} and is never
 * instantiated.</p>
 */
public final class Main {

    private static final int EXIT_OK = 0;
    private static final int EXIT_NO_VALID_RECORDS = 1;
    private static final int EXIT_ARGUMENT_ERROR = 2;
    private static final int EXIT_IO_ERROR = 2;

    private Main() {
    }

    /**
     * Application entry point.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        int exitCode = run(args, System.out, System.err, Path.of("out", "app.log"));
        if (exitCode != EXIT_OK) {
            System.exit(exitCode);
        }
    }

    /**
     * Runs the application logic against the given streams, without ever calling
     * {@link System#exit(int)}.
     *
     * @param args    command line arguments
     * @param out     stream for normal program output
     * @param err     stream for error messages
     * @param logFile path to the log file
     * @return the process exit code
     */
    public static int run(String[] args, PrintStream out, PrintStream err, Path logFile) {
        CliParseResult result = CliParser.parse(args);
        return switch (result) {
            case CliParseResult.Help ignored -> {
                out.print(CliParser.usage());
                yield EXIT_OK;
            }
            case CliParseResult.Version ignored -> {
                out.printf(Locale.ROOT, "%s%n", AppVersion.get());
                yield EXIT_OK;
            }
            case CliParseResult.Error error -> {
                err.print(error.message());
                yield EXIT_ARGUMENT_ERROR;
            }
            case CliParseResult.Run run -> runApplication(run, args, out, err, logFile);
        };
    }

    private static int runApplication(
            CliParseResult.Run run, String[] args, PrintStream out, PrintStream err, Path logFile) {
        try (AppLogger logger = AppLogger.create(logFile, run.verbose(), err)) {
            logger.info("Main.run", "Application started, version " + AppVersion.get());
            logger.info("Main.run", "Arguments: " + Arrays.toString(args));
            logger.info("Main.run", "Resolved input path: " + run.input());
            logger.info("Main.run", "Resolved output path: " + run.output());

            List<String> lines;
            try {
                lines = CsvReader.readLines(run.input());
            } catch (IOException e) {
                logger.error("Main.run", "Failed to read input file " + run.input(), e);
                err.print(readErrorMessage(run.input(), e));
                logger.info("Main.run", "Shutting down with exit code " + EXIT_IO_ERROR);
                return EXIT_IO_ERROR;
            }
            logger.info("Main.run", "Read " + lines.size() + " lines from " + run.input());

            int exitCode = processLines(lines, out, logger);
            logger.info("Main.run", "Shutting down with exit code " + exitCode);
            return exitCode;
        }
    }

    /**
     * Validates every line, reports the skipped ones and collects the valid records.
     *
     * @param lines  raw input lines, in file order
     * @param out    stream the Ukrainian skipped-line messages are printed to
     * @param logger logger receiving one warning per skipped line and the final summary
     * @return {@link #EXIT_OK}, or {@link #EXIT_NO_VALID_RECORDS} if no record passed validation
     */
    private static int processLines(List<String> lines, PrintStream out, AppLogger logger) {
        List<String[]> records = new ArrayList<>();
        int skipped = 0;

        for (int index = 0; index < lines.size(); index++) {
            LineResult result = RecordValidator.validate(index + 1, lines.get(index));
            switch (result) {
                case LineResult.Valid valid -> records.add(valid.fields().toArray(new String[0]));
                case LineResult.Invalid invalid -> {
                    skipped++;
                    out.printf(Locale.ROOT, "%s%n", invalid.message());
                    logger.warn(
                            "Main.processLines",
                            invalid.lineNumber(),
                            invalid.logField(),
                            "Skipped line: " + invalid.logReason());
                }
            }
        }

        logger.info(
                "Main.processLines",
                "Validation summary: " + records.size() + " valid, " + skipped + " skipped");

        if (records.isEmpty()) {
            out.printf(Locale.ROOT, "Немає жодного коректного запису.%n");
            return EXIT_NO_VALID_RECORDS;
        }
        return EXIT_OK;
    }

    private static String readErrorMessage(Path input, IOException e) {
        if (e instanceof NoSuchFileException) {
            return String.format(Locale.ROOT, "Файл не знайдено: %s%n", input);
        }
        return String.format(
                Locale.ROOT, "Не вдалося прочитати файл %s: %s%n", input, e.getMessage());
    }
}
