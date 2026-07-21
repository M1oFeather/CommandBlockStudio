package com.miofeather.commandblockstudio.main.ui.screen;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class StudioScaledScreen extends Screen {
    private float studioScale = 1.0F;
    private int physicalWidth;
    private int physicalHeight;

    protected StudioScaledScreen(Component title) {
        super(title);
    }

    protected final void prepareStudioScale() {
        refreshStudioScale();
    }

    protected final boolean refreshStudioScale() {
        float previousScale = studioScale;
        int previousWidth = width;
        int previousHeight = height;
        physicalWidth = minecraft == null ? width : minecraft.getWindow().getGuiScaledWidth();
        physicalHeight = minecraft == null ? height : minecraft.getWindow().getGuiScaledHeight();
        studioScale = StudioUiScale.resolve(
                CommandBlockStudio.UI_SCALE_PERCENT,
                physicalWidth,
                physicalHeight
        );
        width = Math.max(1, (int) Math.ceil(physicalWidth / studioScale));
        height = Math.max(1, (int) Math.ceil(physicalHeight / studioScale));
        return Math.abs(previousScale - studioScale) > 0.0001F
                || previousWidth != width
                || previousHeight != height;
    }

    protected final int studioMouseX(int mouseX) {
        return (int) Math.floor(mouseX / studioScale);
    }

    protected final int studioMouseY(int mouseY) {
        return (int) Math.floor(mouseY / studioScale);
    }

    protected final double studioMouseX(double mouseX) {
        return mouseX / studioScale;
    }

    protected final double studioMouseY(double mouseY) {
        return mouseY / studioScale;
    }

    protected final double studioMouseDelta(double delta) {
        return delta / studioScale;
    }

    protected final void beginStudioRender(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().scale(studioScale, studioScale, 1.0F);
    }

    protected final void endStudioRender(GuiGraphics graphics) {
        graphics.pose().popPose();
    }

    public final void enableStudioScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        graphics.enableScissor(
                (int) Math.floor(x1 * studioScale),
                (int) Math.floor(y1 * studioScale),
                (int) Math.ceil(x2 * studioScale),
                (int) Math.ceil(y2 * studioScale)
        );
    }

    public final int getEffectiveStudioScalePercent() {
        return Math.round(studioScale * 100.0F);
    }

    public final int getStudioViewportWidth() {
        return width;
    }

    public final int getStudioViewportHeight() {
        return height;
    }

    protected final int getPhysicalWidth() {
        return physicalWidth;
    }

    protected final int getPhysicalHeight() {
        return physicalHeight;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(studioMouseX(mouseX), studioMouseY(mouseY));
    }
}
