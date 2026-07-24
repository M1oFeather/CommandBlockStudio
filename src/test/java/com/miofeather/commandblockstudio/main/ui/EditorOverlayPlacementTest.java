package com.miofeather.commandblockstudio.main.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditorOverlayPlacementTest {
    @Test
    void keepsOverlayBelowTheCursorWhenItFits() {
        assertEquals(120, EditorOverlayPlacement.placeVertical(120, 100, 30, 20, 200));
    }

    @Test
    void flipsOverlayAboveTheCursorBeforeCrossingTheStatusBar() {
        assertEquals(136, EditorOverlayPlacement.placeVertical(190, 166, 30, 20, 200));
    }

    @Test
    void usesTheLastSafePositionWhenNeitherSideHasEnoughRoom() {
        assertEquals(60, EditorOverlayPlacement.placeVertical(90, 35, 40, 60, 100));
    }
}
