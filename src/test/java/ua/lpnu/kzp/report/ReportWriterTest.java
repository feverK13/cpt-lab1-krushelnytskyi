package ua.lpnu.kzp.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ReportWriter}.
 */
class ReportWriterTest {

    private static final String TEXT = "Середня ціна: 200.25 грн" + System.lineSeparator();

    @Test
    void writesTextAsUtf8(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.txt");

        ReportWriter.write(output, TEXT);

        assertArrayEquals(TEXT.getBytes(StandardCharsets.UTF_8), Files.readAllBytes(output));
    }

    @Test
    void createsMissingParentDirectories(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("a").resolve("b").resolve("report.txt");

        ReportWriter.write(output, TEXT);

        assertTrue(Files.isDirectory(output.getParent()));
        assertEquals(TEXT, Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void overwritesExistingFile(@TempDir Path tempDir) throws IOException {
        Path output = tempDir.resolve("report.txt");
        Files.writeString(output, "old content that is longer than the new one", StandardCharsets.UTF_8);

        ReportWriter.write(output, TEXT);

        assertEquals(TEXT, Files.readString(output, StandardCharsets.UTF_8));
    }

    @Test
    void unwritableParentThrows(@TempDir Path tempDir) throws IOException {
        Path blockingFile = tempDir.resolve("not-a-directory");
        Files.writeString(blockingFile, "x", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> ReportWriter.write(blockingFile.resolve("report.txt"), TEXT));
    }
}
