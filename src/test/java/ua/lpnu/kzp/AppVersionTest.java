package ua.lpnu.kzp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
    void getDoesNotContainUnresolvedPlaceholder() {
        String version = AppVersion.get();

        assertFalse(version.contains("${"), "version should not contain an unresolved placeholder");
    }
}
