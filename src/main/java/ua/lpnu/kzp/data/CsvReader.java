package ua.lpnu.kzp.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads the raw text lines of the input CSV file.
 *
 * <p>The file is decoded as UTF-8; a byte sequence that is not valid UTF-8 makes the read fail
 * instead of silently producing replacement characters. Both {@code LF} and {@code CRLF} line
 * endings are accepted, a leading byte order mark is removed from the first line, and blank lines
 * are kept so that the index of a line in the returned list always matches its physical position
 * in the file.</p>
 */
public final class CsvReader {

    private static final char BYTE_ORDER_MARK = (char) 0xFEFF;

    private CsvReader() {
    }

    /**
     * Reads every line of the given file.
     *
     * @param path path to the input file
     * @return the file lines without their line separators, in file order
     * @throws IOException if the file is missing, cannot be read, or is not valid UTF-8
     */
    public static List<String> readLines(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (!lines.isEmpty()) {
            String first = lines.get(0);
            if (!first.isEmpty() && first.charAt(0) == BYTE_ORDER_MARK) {
                lines.set(0, first.substring(1));
            }
        }
        return List.copyOf(lines);
    }
}
