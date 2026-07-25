package com.miofeather.commandblockstudio.main.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorTokenSelectionTest {
    @Test
    void selectsOnlyTheContentsOfDoubleQuotedValues() {
        String text = "{\"color\":\"yellow\"}";
        int start = text.indexOf("yellow");
        assertEquals(
                new EditorTokenSelection.Range(start, start + "yellow".length()),
                EditorTokenSelection.at(text, start + 2).orElseThrow()
        );
    }

    @Test
    void selectsSingleQuotedAndEscapedQuotedContents() {
        String singleQuoted = "{color:'yellow'}";
        int singleStart = singleQuoted.indexOf("yellow");
        assertEquals(
                new EditorTokenSelection.Range(singleStart, singleStart + "yellow".length()),
                EditorTokenSelection.at(singleQuoted, singleStart).orElseThrow()
        );

        String escaped = "{\"text\":\"say \\\"hello\\\"\"}";
        int escapedStart = escaped.indexOf("say");
        int escapedEnd = escaped.lastIndexOf('"');
        assertEquals(
                new EditorTokenSelection.Range(escapedStart, escapedEnd),
                EditorTokenSelection.at(escaped, escaped.indexOf("hello")).orElseThrow()
        );
    }

    @Test
    void keepsNumberSelectionAndIgnoresPunctuation() {
        String text = "foo 12.5, bar";
        assertEquals(
                new EditorTokenSelection.Range(4, 8),
                EditorTokenSelection.at(text, 6).orElseThrow()
        );
        assertTrue(EditorTokenSelection.at(text, text.indexOf("bar") - 1).isEmpty());
    }
}
