package com.miofeather.commandblockstudio.main.ui.screen;

public final class StudioUiScale {
    public static final int AUTO = 0;

    private StudioUiScale() {
    }

    public static float resolve(int configuredPercent, int viewportWidth, int viewportHeight) {
        float requested = configuredPercent == AUTO
                ? 1.0F
                : configuredPercent / 100.0F;
        float widthLimit = viewportWidth / 320.0F;
        float heightLimit = viewportHeight / 260.0F;
        return Math.max(0.5F, Math.min(requested, Math.min(widthLimit, heightLimit)));
    }

}
