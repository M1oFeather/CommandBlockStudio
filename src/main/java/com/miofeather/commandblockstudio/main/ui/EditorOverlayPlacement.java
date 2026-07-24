package com.miofeather.commandblockstudio.main.ui;

final class EditorOverlayPlacement {
    private EditorOverlayPlacement() {
    }

    static int placeVertical(
            int preferredBelowY,
            int preferredAboveBottomY,
            int overlayHeight,
            int minimumY,
            int maximumBottomY
    ) {
        int safeMinimumY = Math.min(minimumY, maximumBottomY);
        int maximumY = Math.max(safeMinimumY, maximumBottomY - Math.max(0, overlayHeight));
        if (preferredBelowY <= maximumY) {
            return Math.max(safeMinimumY, preferredBelowY);
        }

        int aboveY = preferredAboveBottomY - Math.max(0, overlayHeight);
        if (aboveY >= safeMinimumY) {
            return Math.min(aboveY, maximumY);
        }
        return maximumY;
    }
}
