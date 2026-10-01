package ua.lpnu.kzp.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
 * Tests for {@link AppLogger}: line format, level mapping, placeholders and failure handling.
 */
class AppLoggerTest {

    private static final String LINE_PATTERN =
            "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2} \\| "
                    + "(INFO|WARN|ERROR) \\| \\S+ \\| line=(-|\\d+) field=(-|\\S+) \\| .*$";

    private List<String> readLogLines(Path logFile) throws IOException {
        return Files.readAllLines(logFile, StandardCharsets.UTF_8);
    }

    @Test
    void infoLineMatchesExpectedFormat(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", "Application started");
        }

        List<String> lines = readLogLines(logFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).matches(LINE_PATTERN), "line was: " + lines.get(0));
        assertTrue(lines.get(0).contains("INFO"));
        assertTrue(lines.get(0).contains("Main.run"));
        assertTrue(lines.get(0).contains("line=- field=-"));
        assertTrue(lines.get(0).contains("Application started"));
    }

    @Test
    void warnLineIncludesLineAndField(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.warn("Reader.readLine", 7, "price", "Negative price");
        }

        List<String> lines = readLogLines(logFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("WARN"));
        assertTrue(lines.get(0).contains("line=7 field=price"));
    }

    @Test
    void errorLineMapsSevereToErrorAndIncludesStackTrace(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.error("Main.run", "Failed to read file", new IOException("boom"));
        }

        List<String> lines = readLogLines(logFile);
        String content = String.join(System.lineSeparator(), lines);
        assertTrue(content.contains("ERROR"));
        assertTrue(content.contains("Failed to read file"));
        assertTrue(content.contains("java.io.IOException: boom"));
    }

    @Test
    void ukrainianCharactersRoundTripThroughLogFile(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);
        String message = "Пропущено рядок 3: від'ємна ціна, грн";

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", message);
        }

        List<String> lines = readLogLines(logFile);
        assertTrue(lines.get(0).contains(message));
    }

    @Test
    void cyrillicPayloadRoundTripsThroughLogFile(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);
        String rawLine = "Ноутбук;Комп'ютери;abc;24;5";

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.warn("Reader.readLine", 3, "price", "Invalid line: " + rawLine);
        }

        List<String> lines = readLogLines(logFile);
        assertTrue(lines.get(0).contains(rawLine));
    }

    @Test
    void logFileIsOverwrittenOnEachRun(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", "first run");
        }
        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", "second run");
        }

        List<String> lines = readLogLines(logFile);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("second run"));
    }

    @Test
    void verboseMirrorsRecordsToErr(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, true, err)) {
            logger.info("Main.run", "mirrored message");
        }

        String errContent = errBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(errContent.contains("mirrored message"));
        String firstLine = errContent.lines().findFirst().orElseThrow();
        assertTrue(firstLine.matches(LINE_PATTERN), "line was: " + firstLine);
    }

    @Test
    void nonVerboseDoesNotWriteToErr(@TempDir Path tempDir) throws IOException {
        Path logFile = tempDir.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", "not mirrored");
        }

        assertEquals("", errBuffer.toString(StandardCharsets.UTF_8));
    }

    @Test
    void unwritableLogPathPrintsWarningToErrAndDoesNotThrow(@TempDir Path tempDir) throws IOException {
        Path blockingFile = tempDir.resolve("not-a-directory");
        Files.writeString(blockingFile, "x", StandardCharsets.UTF_8);
        Path logFile = blockingFile.resolve("app.log");
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer, true, StandardCharsets.UTF_8);

        try (AppLogger logger = AppLogger.create(logFile, false, err)) {
            logger.info("Main.run", "working despite missing file logging");
        }

        assertFalse(errBuffer.toString(StandardCharsets.UTF_8).isEmpty());
    }
}
