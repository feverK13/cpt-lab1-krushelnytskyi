package ua.lpnu.kzp.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CliParser}: option parsing, defaults, precedence and error cases.
 */
class CliParserTest {

    @Test
    void noArgumentsUsesDefaults() {
        CliParseResult result = CliParser.parse(new String[0]);

        CliParseResult.Run run = assertInstanceOf(CliParseResult.Run.class, result);
        assertEquals(CliParser.DEFAULT_INPUT, run.input());
        assertEquals(CliParser.DEFAULT_OUTPUT, run.output());
        assertEquals(false, run.verbose());
    }

    @Test
    void verboseFlagIsRecognised() {
        CliParseResult result = CliParser.parse(new String[] {"--verbose"});

        CliParseResult.Run run = assertInstanceOf(CliParseResult.Run.class, result);
        assertTrue(run.verbose());
    }

    @Test
    void verboseCombinesWithInputAndOutput() {
        CliParseResult result =
                CliParser.parse(new String[] {"--input", "in.csv", "--output", "out.txt", "--verbose"});

        CliParseResult.Run run = assertInstanceOf(CliParseResult.Run.class, result);
        assertEquals(Path.of("in.csv"), run.input());
        assertEquals(Path.of("out.txt"), run.output());
        assertTrue(run.verbose());
    }

    @Test
    void inputOptionOverridesDefault() {
        CliParseResult result = CliParser.parse(new String[] {"--input", "custom-in.csv"});

        CliParseResult.Run run = assertInstanceOf(CliParseResult.Run.class, result);
        assertEquals(Path.of("custom-in.csv"), run.input());
        assertEquals(CliParser.DEFAULT_OUTPUT, run.output());
    }

    @Test
    void outputOptionOverridesDefault() {
        CliParseResult result = CliParser.parse(new String[] {"--output", "custom-out.txt"});

        CliParseResult.Run run = assertInstanceOf(CliParseResult.Run.class, result);
        assertEquals(CliParser.DEFAULT_INPUT, run.input());
        assertEquals(Path.of("custom-out.txt"), run.output());
    }

    @Test
    void inputAndOutputTogether() {
        CliParseResult result = CliParser.parse(new String[] {"--input", "in.csv", "--output", "out.txt"});

        CliParseResult.Run run = assertInstanceOf(CliParseResult.Run.class, result);
        assertEquals(Path.of("in.csv"), run.input());
        assertEquals(Path.of("out.txt"), run.output());
    }

    @Test
    void helpFlagReturnsHelp() {
        CliParseResult result = CliParser.parse(new String[] {"--help"});

        assertInstanceOf(CliParseResult.Help.class, result);
    }

    @Test
    void versionFlagReturnsVersion() {
        CliParseResult result = CliParser.parse(new String[] {"--version"});

        assertInstanceOf(CliParseResult.Version.class, result);
    }

    @Test
    void helpTakesPrecedenceOverVersion() {
        CliParseResult result = CliParser.parse(new String[] {"--version", "--help"});

        assertInstanceOf(CliParseResult.Help.class, result);
    }

    @Test
    void versionTakesPrecedenceOverUnknownArgument() {
        CliParseResult result = CliParser.parse(new String[] {"--bogus", "--version"});

        assertInstanceOf(CliParseResult.Version.class, result);
    }

    @Test
    void helpTakesPrecedenceOverMissingValue() {
        CliParseResult result = CliParser.parse(new String[] {"--input", "--help"});

        assertInstanceOf(CliParseResult.Help.class, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--bogus", "-x", "input.csv"})
    void unknownArgumentIsError(String argument) {
        CliParseResult result = CliParser.parse(new String[] {argument});

        CliParseResult.Error error = assertInstanceOf(CliParseResult.Error.class, result);
        assertTrue(error.message().contains("--help"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--input", "--output"})
    void missingValueAfterOptionIsError(String option) {
        CliParseResult result = CliParser.parse(new String[] {option});

        CliParseResult.Error error = assertInstanceOf(CliParseResult.Error.class, result);
        assertTrue(error.message().contains("--help"));
    }

    @Test
    void unknownArgumentErrorMessageIsUkrainian() {
        CliParseResult result = CliParser.parse(new String[] {"--bogus"});

        CliParseResult.Error error = assertInstanceOf(CliParseResult.Error.class, result);
        assertTrue(error.message().contains("Невідомий"));
    }

    @Test
    void missingValueErrorMessageIsUkrainian() {
        CliParseResult result = CliParser.parse(new String[] {"--input"});

        CliParseResult.Error error = assertInstanceOf(CliParseResult.Error.class, result);
        assertTrue(error.message().contains("Відсутнє"));
    }
}
