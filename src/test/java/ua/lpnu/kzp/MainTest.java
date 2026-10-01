package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link Main#run}: argument handling, exit codes and side effects.
 */
class MainTest {

    private final ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
    private final PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

    private String out() {
        return outBuffer.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBuffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void noArgumentsExitsWithZero(@TempDir Path tempDir) {
        int exitCode = Main.run(new String[0], out, err, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
    }

    @Test
    void helpPrintsUsageAndExitsZero(@TempDir Path tempDir) {
        int exitCode = Main.run(new String[] {"--help"}, out, err, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertTrue(out().contains("--help"));
        assertTrue(err().isEmpty());
    }

    @Test
    void versionPrintsVersionAndExitsZero(@TempDir Path tempDir) {
        int exitCode = Main.run(new String[] {"--version"}, out, err, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertEquals(AppVersion.get(), out().strip());
        assertTrue(err().isEmpty());
    }

    @Test
    void helpTakesPrecedenceOverVersion(@TempDir Path tempDir) {
        int exitCode = Main.run(new String[] {"--version", "--help"}, out, err, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertTrue(out().contains("--help"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--bogus", "-x"})
    void unknownArgumentExitsWithTwo(String argument, @TempDir Path tempDir) {
        int exitCode = Main.run(new String[] {argument}, out, err, tempDir.resolve("app.log"));

        assertEquals(2, exitCode);
        assertTrue(err().contains("--help"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"--input", "--output"})
    void missingValueExitsWithTwo(String option, @TempDir Path tempDir) {
        int exitCode = Main.run(new String[] {option}, out, err, tempDir.resolve("app.log"));

        assertEquals(2, exitCode);
        assertTrue(err().contains("--help"));
    }

    @Test
    void helpCreatesNoFiles(@TempDir Path tempDir) {
        Path logFile = tempDir.resolve("app.log");

        Main.run(new String[] {"--help"}, out, err, logFile);

        assertFalse(Files.exists(logFile));
    }

    @Test
    void versionCreatesNoFiles(@TempDir Path tempDir) {
        Path logFile = tempDir.resolve("app.log");

        Main.run(new String[] {"--version"}, out, err, logFile);

        assertFalse(Files.exists(logFile));
    }

    @Test
    void normalRunWritesStartupAndShutdownToLogFile(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");

        int exitCode = Main.run(new String[0], out, err, logFile);

        assertEquals(0, exitCode);
        List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
        String content = String.join(System.lineSeparator(), lines);
        assertTrue(content.contains("Application started"));
        assertTrue(content.contains("Shutting down with exit code 0"));
        for (String line : lines) {
            assertTrue(line.contains(" | INFO | Main.run | line=- field=- | "), "line was: " + line);
        }
    }

    @Test
    void verboseMirrorsLogToStderr(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");

        int exitCode = Main.run(new String[] {"--verbose"}, out, err, logFile);

        assertEquals(0, exitCode);
        assertTrue(err().contains("Application started"));
        assertTrue(err().contains("Shutting down with exit code 0"));
    }

    @Test
    void withoutVerboseStderrStaysEmptyOnNormalRun(@TempDir Path tempDir) {
        Path logFile = tempDir.resolve("app.log");

        Main.run(new String[0], out, err, logFile);

        assertTrue(err().isEmpty());
    }

    @Test
    void normalRunSurvivesUnwritableLogPath(@TempDir Path tempDir) throws IOException {
        Path blockingFile = tempDir.resolve("not-a-directory");
        Files.writeString(blockingFile, "x", StandardCharsets.UTF_8);
        Path logFile = blockingFile.resolve("app.log");

        int exitCode = Main.run(new String[0], out, err, logFile);

        assertEquals(0, exitCode);
        assertFalse(err().isEmpty());
    }
}
