package ua.lpnu.kzp;

import ua.lpnu.kzp.cli.CliParseResult;
import ua.lpnu.kzp.cli.CliParser;
import ua.lpnu.kzp.logging.AppLogger;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

/**
 * Entry point of the console application for the "Electronics store" domain.
 *
 * <p>The class is final and has a private constructor: it only hosts {@code main} and is never
 * instantiated.</p>
 */
public final class Main {

    private static final int EXIT_OK = 0;
    private static final int EXIT_ARGUMENT_ERROR = 2;

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
     * @param logFile path to the log file used once processing is implemented
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
            case CliParseResult.Run run -> runApplication(run, args, err, logFile);
        };
    }

    private static int runApplication(CliParseResult.Run run, String[] args, PrintStream err, Path logFile) {
        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", "Application started, version " + AppVersion.get());
            logger.info("Main.run", "Arguments: " + Arrays.toString(args));
            logger.info("Main.run", "Resolved input path: " + run.input());
            logger.info("Main.run", "Resolved output path: " + run.output());

            int exitCode = EXIT_OK;

            logger.info("Main.run", "Shutting down with exit code " + exitCode);
            return exitCode;
        }
    }
}
