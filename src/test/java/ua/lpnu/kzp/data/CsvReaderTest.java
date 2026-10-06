package ua.lpnu.kzp.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CsvReader}: encoding, line endings, BOM and failure modes.
 */
class CsvReaderTest {

    private static final String BOM = String.valueOf((char) 0xFEFF);

    private static Path write(Path dir, String content) throws IOException {
        Path file = dir.resolve("input.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    @Test
    void readsLinesSeparatedByLineFeed(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a;b\nc;d\n");

        assertEquals(List.of("a;b", "c;d"), CsvReader.readLines(file));
    }

    @Test
    void readsLinesSeparatedByCarriageReturnLineFeed(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a;b\r\nc;d\r\n");

        assertEquals(List.of("a;b", "c;d"), CsvReader.readLines(file));
    }

    @Test
    void readsLastLineWithoutTrailingSeparator(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a;b\nc;d");

        assertEquals(List.of("a;b", "c;d"), CsvReader.readLines(file));
    }

    @Test
    void stripsLeadingByteOrderMarkFromFirstLineOnly(@TempDir Path dir) throws IOException {
        Path file = write(dir, BOM + "Ноутбук;Ноутбуки\nМонітор;Монітори\n");

        assertEquals(List.of("Ноутбук;Ноутбуки", "Монітор;Монітори"), CsvReader.readLines(file));
    }

    @Test
    void keepsByteOrderMarkThatIsNotAtTheStart(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a" + BOM + "b;c\n");

        assertEquals(List.of("a" + BOM + "b;c"), CsvReader.readLines(file));
    }

    @Test
    void preservesBlankLinesSoLineNumbersStayPhysical(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a;b\n\n   \nc;d\n");

        assertEquals(List.of("a;b", "", "   ", "c;d"), CsvReader.readLines(file));
    }

    @Test
    void preservesCyrillicContent(@TempDir Path dir) throws IOException {
        Path file = write(dir, "Пральна машина;Побутова техніка;1.00;1;1\n");

        assertEquals(
                List.of("Пральна машина;Побутова техніка;1.00;1;1"), CsvReader.readLines(file));
    }

    @Test
    void emptyFileYieldsNoLines(@TempDir Path dir) throws IOException {
        Path file = write(dir, "");

        assertTrue(CsvReader.readLines(file).isEmpty());
    }

    @Test
    void fileWithOnlyByteOrderMarkYieldsSingleEmptyLine(@TempDir Path dir) throws IOException {
        Path file = write(dir, BOM);

        assertEquals(List.of(""), CsvReader.readLines(file));
    }

    @Test
    void fileWithOnlyBlankLinesKeepsEveryLine(@TempDir Path dir) throws IOException {
        Path file = write(dir, "\n \n\t\n");

        assertEquals(List.of("", " ", "\t"), CsvReader.readLines(file));
    }

    @Test
    void directoryInsteadOfFileThrowsIoException(@TempDir Path dir) {
        assertThrows(IOException.class, () -> CsvReader.readLines(dir));
    }

    @Test
    void missingFileThrowsNoSuchFileException(@TempDir Path dir) {
        Path file = dir.resolve("absent.csv");

        assertThrows(NoSuchFileException.class, () -> CsvReader.readLines(file));
    }

    @Test
    void malformedUtf8ThrowsMalformedInputException(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("broken.csv");
        Files.write(file, new byte[] {(byte) 0xC3, (byte) 0x28, (byte) 0x0A});

        assertThrows(MalformedInputException.class, () -> CsvReader.readLines(file));
    }

    @Test
    void returnedListIsImmutable(@TempDir Path dir) throws IOException {
        Path file = write(dir, "a;b\n");
        List<String> lines = CsvReader.readLines(file);

        assertThrows(UnsupportedOperationException.class, () -> lines.add("x"));
    }
}
