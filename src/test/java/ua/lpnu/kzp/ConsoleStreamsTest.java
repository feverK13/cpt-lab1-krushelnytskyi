package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Tests for {@link ConsoleStreams}.
 */
class ConsoleStreamsTest {

    @Test
    void utf8StreamWritesUtf8Bytes() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream stream = ConsoleStreams.utf8(buffer);

        stream.print("Рядок 1: ціна, грн");

        assertArrayEquals("Рядок 1: ціна, грн".getBytes(StandardCharsets.UTF_8), buffer.toByteArray());
    }
}
