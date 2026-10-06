package ua.lpnu.kzp.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes the report text to the output file.
 */
public final class ReportWriter {

    private ReportWriter() {
    }

    /**
     * Writes {@code text} to {@code output} as UTF-8, creating missing parent directories and
     * overwriting an existing file.
     *
     * @param output path of the report file
     * @param text   report text
     * @throws IOException if a directory or the file cannot be created or written
     */
    public static void write(Path output, String text) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(output, text, StandardCharsets.UTF_8);
    }
}
