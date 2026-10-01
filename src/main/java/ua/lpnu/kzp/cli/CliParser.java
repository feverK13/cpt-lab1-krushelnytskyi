package ua.lpnu.kzp.cli;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Parses command line arguments into a {@link CliParseResult}.
 *
 * <p>Recognised options: {@code --help}, {@code --version}, {@code --input <path>},
 * {@code --output <path>}. {@code --help} takes precedence over every other argument, and
 * {@code --version} takes precedence over any parsing error.</p>
 */
public final class CliParser {

    /** Default input CSV path used when {@code --input} is not given. */
    public static final Path DEFAULT_INPUT = Path.of("data", "input.csv");

    /** Default report output path used when {@code --output} is not given. */
    public static final Path DEFAULT_OUTPUT = Path.of("out", "report.txt");

    private static final String HELP_OPTION = "--help";
    private static final String VERSION_OPTION = "--version";
    private static final String INPUT_OPTION = "--input";
    private static final String OUTPUT_OPTION = "--output";

    private CliParser() {
    }

    /**
     * Parses the given command line arguments.
     *
     * @param args raw command line arguments
     * @return the parsed result: help, version, a runnable configuration, or an error
     */
    public static CliParseResult parse(String[] args) {
        for (String arg : args) {
            if (HELP_OPTION.equals(arg)) {
                return new CliParseResult.Help();
            }
        }
        for (String arg : args) {
            if (VERSION_OPTION.equals(arg)) {
                return new CliParseResult.Version();
            }
        }

        Path input = DEFAULT_INPUT;
        Path output = DEFAULT_OUTPUT;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case INPUT_OPTION -> {
                    if (i + 1 >= args.length) {
                        return missingValueError(INPUT_OPTION);
                    }
                    input = Path.of(args[++i]);
                }
                case OUTPUT_OPTION -> {
                    if (i + 1 >= args.length) {
                        return missingValueError(OUTPUT_OPTION);
                    }
                    output = Path.of(args[++i]);
                }
                default -> {
                    return unknownArgumentError(arg);
                }
            }
        }

        return new CliParseResult.Run(input, output);
    }

    /**
     * Renders the Ukrainian usage text shown for {@code --help}.
     *
     * @return usage text terminated with a platform-independent line separator
     */
    public static String usage() {
        return String.format(
                Locale.ROOT,
                """
                Використання: lab01 [опції]%n
                %n
                Опції:%n
                  --help              показати цю довідку та завершити роботу%n
                  --version           показати версію програми та завершити роботу%n
                  --input <шлях>      шлях до вхідного CSV-файлу (типово: data/input.csv)%n
                  --output <шлях>     шлях до файлу звіту (типово: out/report.txt)%n
                """);
    }

    private static CliParseResult.Error unknownArgumentError(String argument) {
        return new CliParseResult.Error(String.format(
                Locale.ROOT,
                "Невідомий аргумент: %s. Використайте --help для довідки.%n",
                argument));
    }

    private static CliParseResult.Error missingValueError(String option) {
        return new CliParseResult.Error(String.format(
                Locale.ROOT,
                "Відсутнє значення для параметра %s. Використайте --help для довідки.%n",
                option));
    }
}
