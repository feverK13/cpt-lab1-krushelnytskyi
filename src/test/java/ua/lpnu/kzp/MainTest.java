package ua.lpnu.kzp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Placeholder test proving JUnit 5 is wired into the build.
 *
 * <p>This is not a real test of application behaviour; it is replaced by the full suite described
 * in Issue #12.</p>
 */
class MainTest {

    @Test
    void mainRunsWithoutArguments() {
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
