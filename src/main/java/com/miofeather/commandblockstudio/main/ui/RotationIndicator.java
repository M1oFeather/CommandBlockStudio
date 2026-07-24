package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;


import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import org.joml.Vector2d;
import net.minecraft.client.input.MouseButtonEvent;

public class RotationIndicator extends AbstractWidget {
    private boolean dragging = false;
    private double angle = -1.0d;
    private Vector2d midPos;
    private java.util.function.Consumer<Double> changedListener;

    public RotationIndicator(int x, int y, Component message) {
        this(x, y, 32, message);
    }

    public RotationIndicator(int x, int y, int size, Component message) {
        super(x, y, size, size, message);
        midPos = new Vector2d(getX() + getWidth()/2.0d, getY() + getHeight()/2.0d);
    }

    public void setChangedListener(java.util.function.Consumer<Double> listener){
        changedListener = listener;
    }

    public void setAngle(double value){
        angle = (value-0.5) * 2.0;
    }

    public double getAngle(){
        return (angle + 1) / 2.0;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CommandBlockStudio.COMPASS_FRAME, getX(), getY(), getWidth(), getHeight());
        graphics.pose().pushMatrix();
        graphics.pose().translate(getX() + halfWidth, getY() + halfHeight);
        graphics.pose().rotate((float) ((angle + 1) * Math.PI));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, CommandBlockStudio.COMPASS_NEEDLE, -halfWidth, -halfHeight, getWidth(), getHeight());
        graphics.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.isValidClickButton(event.buttonInfo()) && this.clicked(event.x(), event.y())) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            dragging = true;
            setAngleFromMousePos(event.x(), event.y());
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        dragging = false;
    }

    public boolean clicked(double mouseX, double mouseY){
        return midPos.distance(new Vector2d(mouseX, mouseY)) <= getWidth() / 2.0;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double distX, double distY){
        if(dragging) {
            setAngleFromMousePos(event.x(), event.y());
        }
    }

    private void setAngleFromMousePos(double mouseX, double mouseY){
        Vector2d toMouse = new Vector2d(mouseX, mouseY).sub(midPos);
        angle = new Vector2d(0.0,1.0).angle(toMouse) / Math.PI;
        changedListener.accept(getAngle());
    }
}
