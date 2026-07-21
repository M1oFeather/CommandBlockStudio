package com.miofeather.commandblockstudio.main.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextSearchEngineTest {
    @Test
    void countsNonOverlappingMatchesWithOptionalCaseSensitivity() {
        assertEquals(2, TextSearchEngine.countMatches("Tp @p\ntp @a", "tp", false));
        assertEquals(1, TextSearchEngine.countMatches("Tp @p\ntp @a", "tp", true));
        assertEquals(2, TextSearchEngine.countMatches("aaaa", "aa", true));
    }

    @Test
    void wrapsForwardAndBackwardWithoutChangingTextOffsets() {
        TextSearchEngine.Match forward = TextSearchEngine.findMatch("alpha beta alpha", "alpha", 12, 16, false, true).orElseThrow();
        assertEquals(new TextSearchEngine.Match(0, 5), forward);

        TextSearchEngine.Match backward = TextSearchEngine.findMatch("alpha beta alpha", "alpha", 0, 5, true, true).orElseThrow();
        assertEquals(new TextSearchEngine.Match(11, 16), backward);

        TextSearchEngine.Match unicode = TextSearchEngine.findMatch("İtem item", "item", 0, 0, false, false).orElseThrow();
        assertEquals(new TextSearchEngine.Match(0, 4), unicode);
    }

    @Test
    void replacesAllMatchesAndKeepsUnmatchedText() {
        TextSearchEngine.ReplaceAllResult result = TextSearchEngine.replaceAll("Tp @p; tp @a", "tp", "teleport", false);
        assertEquals(2, result.count());
        assertEquals("teleport @p; teleport @a", result.text());
        assertTrue(TextSearchEngine.selectionMatches(result.text(), 0, 8, "TELEPORT", false));
    }
}
