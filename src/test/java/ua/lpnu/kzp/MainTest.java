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
 * Integration tests for {@link Main#run}: argument handling, input processing, reported messages
 * and exit codes.
 */
class MainTest {

    private static final String VALID_RECORD = "Монітор Dell S2425H;Монітори;6499.00;36;12";
    private static final String NO_RECORDS_MESSAGE = "Немає жодного коректного запису.";
    private static final String SKIPPED_HEADER = "Пропущені рядки:";
    private static final String BOM = String.valueOf((char) 0xFEFF);

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

    private static Path input(Path dir, String content) throws IOException {
        Path file = dir.resolve("input.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private int runWith(Path input, Path logFile, String... extraArgs) {
        return runWith(input, input.toAbsolutePath().getParent().resolve("report.txt"), logFile, extraArgs);
    }

    private int runWith(Path input, Path output, Path logFile, String... extraArgs) {
        String[] args = new String[extraArgs.length + 4];
        args[0] = "--input";
        args[1] = input.toString();
        args[2] = "--output";
        args[3] = output.toString();
        System.arraycopy(extraArgs, 0, args, 4, extraArgs.length);
        return Main.run(args, out, err, logFile);
    }

    private static String metricValue(String report, String label) {
        return report.lines()
                .filter(line -> line.startsWith(label + " "))
                .map(line -> line.substring(label.length()).strip())
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row " + label + " in: " + report));
    }

    private static List<String> skippedSection(String report) {
        List<String> lines = report.lines().toList();
        int header = lines.indexOf(SKIPPED_HEADER);
        return header < 0 ? List.of() : lines.subList(header + 1, lines.size());
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
        int exitCode =
                Main.run(new String[] {"--version", "--help"}, out, err, tempDir.resolve("app.log"));

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
    void validInputPrintsReportWithoutSkippedSectionAndExitsZero(@TempDir Path tempDir)
            throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n");

        int exitCode = runWith(file, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertEquals("1", metricValue(out(), "Кількість коректних записів"));
        assertTrue(out().contains("Пропущено рядків: 0"), "out was: " + out());
        assertFalse(out().contains(SKIPPED_HEADER));
        assertTrue(err().isEmpty());
    }

    @Test
    void bomAndCrlfInputIsAccepted(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, BOM + VALID_RECORD + "\r\n" + VALID_RECORD + "\r\n");

        int exitCode = runWith(file, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertEquals("2", metricValue(out(), "Кількість коректних записів"));
        assertTrue(out().contains("Пропущено рядків: 0"), "out was: " + out());
    }

    @Test
    void emptyFileExitsWithOne(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, "");

        int exitCode = runWith(file, tempDir.resolve("app.log"));

        assertEquals(1, exitCode);
        assertTrue(out().contains(NO_RECORDS_MESSAGE));
    }

    @Test
    void fileWithOnlyInvalidLinesExitsWithOne(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, "абв\n\nМонітор;Монітори;абв;1;1\n");

        int exitCode = runWith(file, tempDir.resolve("app.log"));

        assertEquals(1, exitCode);
        assertTrue(out().contains(NO_RECORDS_MESSAGE));
        assertTrue(out().contains("Рядок 1: очікується 5 полів, отримано 1"));
        assertTrue(out().contains("Рядок 2: порожній рядок"));
        assertTrue(out().contains("Рядок 3: ціна: не є числом"));
    }

    @Test
    void mixedFileReportsSkippedLinesInFileOrderAndExitsZero(@TempDir Path tempDir)
            throws IOException {
        Path file = input(tempDir, String.join("\n",
                VALID_RECORD,
                ";Монітори;1;1;1",
                "",
                "Ноутбук;Ноутбуки;24999.00;24;7",
                "Колонка;Аудіо;3499.00;12.5;8",
                "Пилосос;Побутова техніка;29999.00;24;-3",
                "Фотоапарат;Фотоапарати;39999.00;24",
                "Клавіатура;Аксесуари;3899.00;24;25") + "\n");

        int exitCode = runWith(file, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertEquals(
                List.of(
                        "Рядок 2: назва: порожнє поле",
                        "Рядок 3: порожній рядок",
                        "Рядок 5: гарантія: має бути цілим числом: \"12.5\"",
                        "Рядок 6: запас: від'ємне значення: \"-3\"",
                        "Рядок 7: очікується 5 полів, отримано 4"),
                skippedSection(out()));
        assertTrue(out().contains("Пропущено рядків: 5"));
        assertFalse(out().contains(NO_RECORDS_MESSAGE));
    }

    @Test
    void missingInputFileExitsWithTwo(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("absent.csv");
        Path logFile = tempDir.resolve("app.log");

        int exitCode = runWith(file, logFile);

        assertEquals(2, exitCode);
        assertTrue(err().contains("Файл не знайдено"));
        assertTrue(out().isEmpty());
        assertTrue(logContent(logFile).contains("| ERROR | Main.run |"));
        assertTrue(logContent(logFile).contains("NoSuchFileException"));
    }

    @Test
    void malformedUtf8InputExitsWithTwo(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("broken.csv");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28, (byte) 0x0A});
        Path logFile = tempDir.resolve("app.log");

        int exitCode = runWith(file, logFile);

        assertEquals(2, exitCode);
        assertFalse(err().isEmpty());
        assertTrue(logContent(logFile).contains("MalformedInputException"));
    }

    @Test
    void skippedLinesAreLoggedAsWarnings(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\nМонітор;Монітори;1;1;-3\n");
        Path logFile = tempDir.resolve("app.log");

        int exitCode = runWith(file, logFile);

        assertEquals(0, exitCode);
        String content = logContent(logFile);
        assertTrue(
                content.contains("| WARN | InputProcessor.process | line=2 field=stock | "
                        + "Skipped line: negative value: \"-3\""),
                "log was: " + content);
        assertTrue(content.contains("Validation summary: 1 valid, 1 skipped"));
    }

    @Test
    void verboseMirrorsLogToStderr(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n");

        int exitCode = runWith(file, tempDir.resolve("app.log"), "--verbose");

        assertEquals(0, exitCode);
        assertTrue(err().contains("Application started"));
        assertTrue(err().contains("Shutting down with exit code 0"));
    }

    @Test
    void withoutVerboseStderrStaysEmptyOnNormalRun(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\nМонітор;Монітори;абв;1;1\n");

        runWith(file, tempDir.resolve("app.log"));

        assertTrue(err().isEmpty(), "err was: " + err());
    }

    @Test
    void normalRunWritesStartupAndShutdownToLogFile(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n");
        Path logFile = tempDir.resolve("app.log");

        int exitCode = runWith(file, logFile);

        assertEquals(0, exitCode);
        String content = logContent(logFile);
        assertTrue(content.contains("Application started"));
        assertTrue(content.contains("Shutting down with exit code 0"));
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
            assertTrue(line.contains(" | INFO | "), "line was: " + line);
        }
    }

    @Test
    void normalRunSurvivesUnwritableLogPath(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n");
        Path blockingFile = tempDir.resolve("not-a-directory");
        Files.writeString(blockingFile, "x", StandardCharsets.UTF_8);

        int exitCode = runWith(file, blockingFile.resolve("app.log"));

        assertEquals(0, exitCode);
        assertFalse(err().isEmpty());
    }

    @Test
    void consoleTextEqualsReportFileText(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n\nМонітор;Монітори;абв;1;1\n");
        Path output = tempDir.resolve("reports").resolve("report.txt");

        int exitCode = runWith(file, output, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        assertEquals(out(), Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void noValidRecordsStillWritesReportAndExitsWithOne(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, "\n");
        Path output = tempDir.resolve("report.txt");

        int exitCode = runWith(file, output, tempDir.resolve("app.log"));

        assertEquals(1, exitCode);
        String report = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals(out(), report);
        assertTrue(report.contains(NO_RECORDS_MESSAGE));
        assertEquals(List.of("Рядок 1: порожній рядок"), skippedSection(report));
        assertFalse(report.contains("Середня ціна"));
    }

    @Test
    void unwritableOutputExitsWithTwo(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n");
        Path blockingFile = tempDir.resolve("not-a-directory");
        Files.writeString(blockingFile, "x", StandardCharsets.UTF_8);
        Path logFile = tempDir.resolve("app.log");

        int exitCode = runWith(file, blockingFile.resolve("report.txt"), logFile);

        assertEquals(2, exitCode);
        assertTrue(err().contains("Не вдалося записати звіт у файл"), "err was: " + err());
        String content = logContent(logFile);
        assertTrue(content.contains("| ERROR | Main.run |"), "log was: " + content);
        assertTrue(content.contains("\tat "), "log has no stack trace: " + content);
        assertTrue(content.contains("Shutting down with exit code 2"));
    }

    @Test
    void metricsSummaryIsLogged(@TempDir Path tempDir) throws IOException {
        Path file = input(tempDir, VALID_RECORD + "\n");
        Path logFile = tempDir.resolve("app.log");

        runWith(file, logFile);

        assertTrue(logContent(logFile).contains(
                "Metrics: valid=1 averagePrice=6499.00 longestWarrantyMonths=36 totalStock=12"));
    }

    @Test
    void sampleInputProducesExpectedReport(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.txt");

        int exitCode = runWith(Path.of("data", "input.csv"), output, tempDir.resolve("app.log"));

        assertEquals(0, exitCode);
        String report = out();
        assertEquals("8", metricValue(report, "Кількість коректних записів"));
        assertEquals("14030.62 грн", metricValue(report, "Середня ціна"));
        assertEquals("36", metricValue(report, "Найдовша гарантія, міс."));
        assertEquals("75", metricValue(report, "Загальний запас, шт."));
        assertTrue(report.contains("Пропущено рядків: 6"), report);
        assertEquals(6, skippedSection(report).size());
        assertEquals(report, Files.readString(output, StandardCharsets.UTF_8));
    }

    private static String logContent(Path logFile) throws IOException {
        return Files.readString(logFile, StandardCharsets.UTF_8);
    }
}
