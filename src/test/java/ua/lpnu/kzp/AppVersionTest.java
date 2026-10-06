package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link AppVersion}.
 */
class AppVersionTest {

    @Test
    void getReturnsVersionMatchingSemverPattern() {
        String version = AppVersion.get();

        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"),
                "version should match \\d+.\\d+.\\d+ but was: " + version);
    }

    @Test
    void buildNumberIsLocalOrCiRunNumber() {
        String build = AppVersion.buildNumber();

        assertTrue(build.matches("local|\\d+"), "build should be 'local' or digits but was: " + build);
    }

    @ParameterizedTest
    @CsvSource({
        "1.0.0, 42,    1.0.0 (build 42)",
        "1.0.0, local, 1.0.0 (build local)",
        "2.3.4, 7,     2.3.4 (build 7)"
    })
    void describeFormatsVersionAndBuildNumber(String version, String build, String expected) {
        assertEquals(expected, AppVersion.describe(version, build));
    }

    @Test
    void readReturnsValueOfKey() {
        InputStream in = new ByteArrayInputStream("build=42\n".getBytes(StandardCharsets.UTF_8));

        assertEquals("42", AppVersion.read(in, "build"));
    }

    @Test
    void readFailsWhenResourceIsMissing() {
        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> AppVersion.read(null, "version"));

        assertTrue(e.getMessage().startsWith("Resource not found"), e.getMessage());
    }

    @Test
    void readFailsWhenKeyIsMissing() {
        InputStream in = new ByteArrayInputStream("version=1.0.0\n".getBytes(StandardCharsets.UTF_8));

        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> AppVersion.read(in, "build"));

        assertTrue(e.getMessage().startsWith("Missing key 'build'"), e.getMessage());
    }

    @Test
    void readWrapsIoFailure() {
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("boom");
            }
        };

        IllegalStateException e =
                assertThrows(IllegalStateException.class, () -> AppVersion.read(in, "version"));

        assertInstanceOf(IOException.class, e.getCause());
    }

    @Test
    void describeUsesTheBundledVersionAndBuildNumber() {
        assertEquals(
                AppVersion.get() + " (build " + AppVersion.buildNumber() + ")", AppVersion.describe());
    }
}
