package com.miofeather.commandblockstudio.main.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;


import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractButton;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CyclingTexturedButtonWidget<T> extends AbstractButton {
    WidgetSprites[] textures;
    T[] values;
    CyclingTooltipSupplier tooltipSupplier;
    PressAction action;

    public CyclingTexturedButtonWidget(int x, int y, int width, int height, Component message, PressAction onPress, WidgetSprites[] textures, int initialIndex, T[] values) {
        super(x, y, width, height, message);
        this.textures = textures;
        this.values = values;
        this.tooltipSupplier = new CyclingTooltipSupplier(initialIndex, new Component[values.length]);
        this.action = onPress;
        this.setTooltip(tooltipSupplier.getTooltip());
    }

    public CyclingTexturedButtonWidget(int x, int y, int width, int height, Component message, PressAction onPress, WidgetSprites[] textures, int initialIndex, T[] values, Component[] tooltips) {
        super(x, y, width, height, message);
        this.textures = textures;
        this.values = values;
        this.tooltipSupplier = new CyclingTooltipSupplier(initialIndex, tooltips);
        this.action = onPress;
        this.setTooltip(tooltipSupplier.getTooltip());
    }

    @Override
    public void onPress(){
        if(!active) return;
        this.tooltipSupplier.incrementIndex();
        this.setTooltip(tooltipSupplier.getTooltip());
        action.onPress(this);
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if(!active) return;
        if(mouseX > getX() && mouseX <= getX() + getWidth() && mouseY > getY() && mouseY <= getY() + getHeight()){
            this.tooltipSupplier.incrementIndex();
            this.setTooltip(tooltipSupplier.getTooltip());
            action.onPress(this);
        }
    }

    public T getValue(){
        return values[this.tooltipSupplier.getCurrentIndex()];
    }

    public void setIndex(int index){
        this.tooltipSupplier.setIndex(index);
        this.setTooltip(tooltipSupplier.getTooltip());
    }

    public void setActive(boolean value){
        this.active = value;
    }

    @Override
    public void renderWidget(final GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        Minecraft minecraftClient = Minecraft.getInstance();
        Font textRenderer = minecraftClient.font;
        int i = this.active?(this.isHovered()?2:1):0;
        graphics.blitSprite( this.textures[this.tooltipSupplier.getCurrentIndex()].get(this.active,this.isHovered()), this.getX(), this.getY(), 20, 20);
        //this.renderBackground(matrices, minecraftClient, mouseX, mouseY);
        int j = this.active ? 0xFFFFFF : 0xA0A0A0;
        graphics.drawCenteredString(textRenderer, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, j | Mth.ceil(this.alpha * 255.0f) << 24);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {

    }

    public void setTooltipVisible(boolean value){
        this.setTooltip(value ? tooltipSupplier.getTooltip() : null);
    }

    public interface PressAction{
        public void onPress(CyclingTexturedButtonWidget button);
    }
}
