package com.miofeather.commandblockstudio.main.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.function.IntConsumer;

public final class ColorPaletteWidget extends AbstractWidget {
    private static final int HUE_BAR_WIDTH = 10;
    private static final int GAP = 6;
    private static final int SAMPLE_STEP = 2;

    private float hue;
    private float saturation;
    private float brightness;
    private DragTarget dragTarget = DragTarget.NONE;
    private IntConsumer changedListener;

    public ColorPaletteWidget(int x, int y, int width, int height, Component message) {
        super(x, y, Math.max(48, width), Math.max(32, height), message);
        setColor(ColorPicker.getInteger());
    }

    public void setChangedListener(IntConsumer changedListener) {
        this.changedListener = changedListener;
    }

    public void setColor(int rgb) {
        int red = rgb >> 16 & 0xFF;
        int green = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        float[] hsv = rgbToHsv(red, green, blue);
        if (hsv[1] > 0.0001F) {
            hue = hsv[0];
        }
        saturation = hsv[1];
        brightness = hsv[2];
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int fieldWidth = getFieldWidth();
        int fieldHeight = getHeight();

        for (int sampleX = 0; sampleX < fieldWidth; sampleX += SAMPLE_STEP) {
            float sampleSaturation = sampleX / (float) Math.max(1, fieldWidth - 1);
            int topColor = 0xFF000000 | hsvToRgb(hue, sampleSaturation, 1.0F);
            graphics.fillGradient(
                    getX() + sampleX,
                    getY(),
                    getX() + Math.min(fieldWidth, sampleX + SAMPLE_STEP),
                    getY() + fieldHeight,
                    topColor,
                    0xFF000000
            );
        }
        graphics.outline(getX() - 1, getY() - 1, fieldWidth + 2, fieldHeight + 2, 0xFF8B949E);

        int hueX = getHueX();
        for (int sampleY = 0; sampleY < fieldHeight; sampleY += SAMPLE_STEP) {
            float sampleHue = sampleY / (float) Math.max(1, fieldHeight - 1);
            int color = 0xFF000000 | hsvToRgb(sampleHue, 1.0F, 1.0F);
            graphics.fill(
                    hueX,
                    getY() + sampleY,
                    hueX + HUE_BAR_WIDTH,
                    getY() + Math.min(fieldHeight, sampleY + SAMPLE_STEP),
                    color
            );
        }
        graphics.outline(hueX - 1, getY() - 1, HUE_BAR_WIDTH + 2, fieldHeight + 2, 0xFF8B949E);

        int selectorX = getX() + Math.round(saturation * Math.max(1, fieldWidth - 1));
        int selectorY = getY() + Math.round((1.0F - brightness) * Math.max(1, fieldHeight - 1));
        graphics.outline(selectorX - 2, selectorY - 2, 5, 5, 0xFF000000);
        graphics.outline(selectorX - 1, selectorY - 1, 3, 3, 0xFFFFFFFF);

        int hueY = getY() + Math.round(hue * Math.max(1, fieldHeight - 1));
        graphics.fill(hueX - 2, hueY - 1, hueX + HUE_BAR_WIDTH + 2, hueY + 2, 0xFF000000);
        graphics.fill(hueX - 1, hueY, hueX + HUE_BAR_WIDTH + 1, hueY + 1, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (!visible || !active || event.button() != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        dragTarget = mouseX >= getHueX() - 2 ? DragTarget.HUE : DragTarget.FIELD;
        updateFromMouse(mouseX, mouseY);
        setFocused(true);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != 0 || dragTarget == DragTarget.NONE) {
            return false;
        }
        updateFromMouse(event.x(), event.y());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = dragTarget != DragTarget.NONE;
        dragTarget = DragTarget.NONE;
        return handled;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible
                && mouseX >= getX() - 1
                && mouseX < getX() + getWidth() + 1
                && mouseY >= getY() - 1
                && mouseY < getY() + getHeight() + 1;
    }

    private void updateFromMouse(double mouseX, double mouseY) {
        if (dragTarget == DragTarget.HUE) {
            hue = Mth.clamp((float) ((mouseY - getY()) / Math.max(1, getHeight() - 1)), 0.0F, 1.0F);
        } else {
            saturation = Mth.clamp((float) ((mouseX - getX()) / Math.max(1, getFieldWidth() - 1)), 0.0F, 1.0F);
            brightness = 1.0F - Mth.clamp((float) ((mouseY - getY()) / Math.max(1, getHeight() - 1)), 0.0F, 1.0F);
        }
        if (changedListener != null) {
            changedListener.accept(hsvToRgb(hue, saturation, brightness));
        }
    }

    private int getFieldWidth() {
        return Math.max(30, getWidth() - HUE_BAR_WIDTH - GAP);
    }

    private int getHueX() {
        return getX() + getFieldWidth() + GAP;
    }

    private static int hsvToRgb(float hue, float saturation, float brightness) {
        if (saturation <= 0.0F) {
            int gray = Math.round(brightness * 255.0F);
            return gray << 16 | gray << 8 | gray;
        }

        float scaledHue = (hue - (float) Math.floor(hue)) * 6.0F;
        int sector = (int) Math.floor(scaledHue);
        float fraction = scaledHue - sector;
        float p = brightness * (1.0F - saturation);
        float q = brightness * (1.0F - saturation * fraction);
        float t = brightness * (1.0F - saturation * (1.0F - fraction));
        float red;
        float green;
        float blue;
        switch (sector) {
            case 0 -> { red = brightness; green = t; blue = p; }
            case 1 -> { red = q; green = brightness; blue = p; }
            case 2 -> { red = p; green = brightness; blue = t; }
            case 3 -> { red = p; green = q; blue = brightness; }
            case 4 -> { red = t; green = p; blue = brightness; }
            default -> { red = brightness; green = p; blue = q; }
        }
        return Math.round(red * 255.0F) << 16
                | Math.round(green * 255.0F) << 8
                | Math.round(blue * 255.0F);
    }

    private static float[] rgbToHsv(int red, int green, int blue) {
        float r = red / 255.0F;
        float g = green / 255.0F;
        float b = blue / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue = 0.0F;
        if (delta > 0.0001F) {
            if (max == r) {
                hue = ((g - b) / delta) % 6.0F;
            } else if (max == g) {
                hue = (b - r) / delta + 2.0F;
            } else {
                hue = (r - g) / delta + 4.0F;
            }
            hue /= 6.0F;
            if (hue < 0.0F) {
                hue += 1.0F;
            }
        }
        float saturation = max <= 0.0F ? 0.0F : delta / max;
        return new float[]{hue, saturation, max};
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    private enum DragTarget {
        NONE,
        FIELD,
        HUE
    }
}
