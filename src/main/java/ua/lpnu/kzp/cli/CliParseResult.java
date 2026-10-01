package ua.lpnu.kzp.cli;

import java.nio.file.Path;

/**
 * Outcome of parsing the command line arguments.
 */
public sealed interface CliParseResult {

    /** The user requested usage help ({@code --help}). */
    record Help() implements CliParseResult {
    }

    /** The user requested the application version ({@code --version}). */
    record Version() implements CliParseResult {
    }

    /**
     * Arguments were parsed successfully into a runnable configuration.
     *
     * @param input  resolved input CSV path
     * @param output resolved report output path
     */
    record Run(Path input, Path output) implements CliParseResult {
    }

    /**
     * Arguments could not be parsed.
     *
     * @param message Ukrainian error message, ready to print to stderr
     */
    record Error(String message) implements CliParseResult {
    }
}
