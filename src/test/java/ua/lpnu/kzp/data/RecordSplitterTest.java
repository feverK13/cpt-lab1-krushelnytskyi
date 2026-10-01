package ua.lpnu.kzp.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link RecordSplitter}: field count, empty fields and preserved whitespace.
 */
class RecordSplitterTest {

    @Test
    void splitsFiveFields() {
        assertArrayEquals(
                new String[] {"Монітор", "Монітори", "6499.00", "36", "12"},
                RecordSplitter.split("Монітор;Монітори;6499.00;36;12"));
    }

    @Test
    void trailingSeparatorYieldsExtraEmptyField() {
        assertEquals(6, RecordSplitter.split("a;b;c;d;e;").length);
    }

    @Test
    void emptyTrailingFieldsArePreserved() {
        assertArrayEquals(new String[] {"a", "", "", ""}, RecordSplitter.split("a;;;"));
    }

    @Test
    void lineWithoutSeparatorYieldsSingleField() {
        assertArrayEquals(new String[] {"abc"}, RecordSplitter.split("abc"));
    }

    @Test
    void whitespaceIsNotRemoved() {
        assertArrayEquals(new String[] {" a ", " b "}, RecordSplitter.split(" a ; b "));
    }

    @Test
    void expectedFieldCountIsFive() {
        assertEquals(5, RecordSplitter.FIELD_COUNT);
    }
}
