package ua.lpnu.kzp;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Creates console streams that always encode text as UTF-8, regardless of the platform default
 * or the {@code stdout.encoding} chosen by the JVM.
 */
public final class ConsoleStreams {

    private ConsoleStreams() {
    }

    /**
     * Wraps {@code target} in an auto-flushing {@link PrintStream} that encodes text as UTF-8.
     *
     * @param target the byte stream to write to, e.g. {@link System#out}
     * @return a UTF-8 print stream writing to {@code target}
     */
    public static PrintStream utf8(OutputStream target) {
        return new PrintStream(target, true, StandardCharsets.UTF_8);
    }
}
