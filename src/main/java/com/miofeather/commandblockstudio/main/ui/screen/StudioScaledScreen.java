package com.miofeather.commandblockstudio.main.ui.screen;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
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

    protected final void beginStudioRender(GuiGraphicsExtractor graphics) {
        graphics.pose().pushMatrix();
        graphics.pose().scale(studioScale, studioScale);
    }

    protected final void endStudioRender(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }

    public final void enableStudioScissor(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2) {
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return mouseClicked(event.x(), event.y(), event.button());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)), false);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return mouseReleased(event.x(), event.y(), event.button());
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)));
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return mouseDragged(
                event.x(),
                event.y(),
                event.button(),
                deltaX,
                deltaY
        );
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(
                new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button, 0)),
                deltaX,
                deltaY
        );
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }
}
