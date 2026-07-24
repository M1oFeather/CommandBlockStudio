package com.miofeather.commandblockstudio.main.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class StudioIconButton extends Button {
    private static final int ICON_TEXTURE_SIZE = 32;

    private Identifier icon;
    private final int iconSize;
    private final boolean drawIdleBackground;
    private boolean selected;
    private boolean prominentBackground;
    private boolean smoothTextureConfigured;

    public StudioIconButton(
            int x,
            int y,
            int width,
            int height,
            Component narration,
            Identifier icon,
            int iconSize,
            boolean drawIdleBackground,
            OnPress onPress
    ) {
        super(x, y, width, height, narration, onPress, DEFAULT_NARRATION);
        this.icon = icon;
        this.iconSize = iconSize;
        this.drawIdleBackground = drawIdleBackground;
    }

    public void setIcon(Identifier icon) {
        this.icon = icon;
        this.smoothTextureConfigured = false;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setProminentBackground(boolean prominentBackground) {
        this.prominentBackground = prominentBackground;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (prominentBackground) {
            int background = !active
                    ? 0xFF252A30
                    : isHoveredOrFocused() ? 0xFF4B5561 : selected ? 0xFF414B56 : 0xFF383F47;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0xFF0D1014);
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + getHeight() - 1, background);
            graphics.fill(getX() + 1, getY() + 1, getX() + getWidth() - 1, getY() + 2, 0xFF66707B);
            graphics.fill(getX() + 1, getY() + 1, getX() + 2, getY() + getHeight() - 1, 0xFF59636E);
        } else if (drawIdleBackground || selected || isHoveredOrFocused()) {
            int background = !active
                    ? 0xFF15181C
                    : isHoveredOrFocused() ? 0xFF303640 : selected ? 0xFF272D34 : 0xFF1C2026;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
        }

        int drawSize = Math.min(iconSize, Math.min(getWidth(), getHeight()));
        int iconX = getX() + (getWidth() - drawSize) / 2;
        int iconY = getY() + (getHeight() - drawSize) / 2;
        smoothTextureConfigured = true;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                icon,
                iconX,
                iconY,
                0.0F,
                0.0F,
                ICON_TEXTURE_SIZE,
                ICON_TEXTURE_SIZE,
                drawSize,
                drawSize,
                ICON_TEXTURE_SIZE,
                ICON_TEXTURE_SIZE
        );
    }
}
