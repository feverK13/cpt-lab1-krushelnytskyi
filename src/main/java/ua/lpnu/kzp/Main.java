package ua.lpnu.kzp;

import ua.lpnu.kzp.cli.CliParseResult;
import ua.lpnu.kzp.cli.CliParser;
import ua.lpnu.kzp.data.CsvReader;
import ua.lpnu.kzp.data.InputProcessor;
import ua.lpnu.kzp.logging.AppLogger;
import ua.lpnu.kzp.metrics.Metrics;
import ua.lpnu.kzp.metrics.MetricsCalculator;
import ua.lpnu.kzp.report.ReportFormatter;
import ua.lpnu.kzp.report.ReportWriter;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
     * Application entry point. Console output is always encoded as UTF-8.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        PrintStream out = ConsoleStreams.utf8(System.out);
        PrintStream err = ConsoleStreams.utf8(System.err);
        int exitCode = run(args, out, err, Path.of("out", "app.log"));
        out.flush();
        err.flush();
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

            int exitCode = process(run, out, err, logger);
            logger.info("Main.run", "Shutting down with exit code " + exitCode);
            return exitCode;
        }
    }

    private static int process(CliParseResult.Run run, PrintStream out, PrintStream err, AppLogger logger) {
        List<String> lines;
        try {
            lines = CsvReader.readLines(run.input());
        } catch (IOException e) {
            logger.error("Main.run", "Failed to read input file " + run.input(), e);
            err.print(readErrorMessage(run.input(), e));
            return EXIT_IO_ERROR;
        }
        logger.info("Main.run", "Read " + lines.size() + " lines from " + run.input());

        InputProcessor.ProcessedInput input = InputProcessor.process(lines, logger);

        Optional<Metrics> metrics = MetricsCalculator.calculate(input.records());
        logger.info("Main.run", metrics.map(m -> "Metrics: " + m.summary())
                .orElse("No valid records, metrics are not computed"));

        String report = ReportFormatter.format(run.input(), metrics, input.skippedMessages());
        out.print(report);
        try {
            ReportWriter.write(run.output(), report);
        } catch (IOException e) {
            logger.error("Main.run", "Failed to write report file " + run.output(), e);
            err.printf(Locale.ROOT, "Не вдалося записати звіт у файл %s: %s%n", run.output(), e);
            return EXIT_IO_ERROR;
        }
        logger.info("Main.run", "Report written to " + run.output());

        return metrics.isPresent() ? EXIT_OK : EXIT_NO_VALID_RECORDS;
    }

    private static String readErrorMessage(Path input, IOException e) {
        if (e instanceof NoSuchFileException) {
            return String.format(Locale.ROOT, "Файл не знайдено: %s%n", input);
        }
        return String.format(
                Locale.ROOT, "Не вдалося прочитати файл %s: %s%n", input, e.getMessage());
    }
}
