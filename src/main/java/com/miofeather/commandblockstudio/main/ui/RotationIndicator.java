package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;


import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector2d;

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
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        graphics.blitSprite(RenderType::guiTextured, CommandBlockStudio.COMPASS_FRAME, getX(), getY(), getWidth(), getHeight());
        graphics.pose().pushPose();
        graphics.pose().translate(getX() + halfWidth, getY() + halfHeight, 0.0f);
        graphics.pose().mulPose(new Quaternionf().rotateZ((float) ((angle + 1) * Math.PI)));
        graphics.blitSprite(RenderType::guiTextured, CommandBlockStudio.COMPASS_NEEDLE, -halfWidth, -halfHeight, getWidth(), getHeight());
        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {}

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        if (this.isValidClickButton(button) && this.clicked(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            dragging = true;
            setAngleFromMousePos(mouseX, mouseY);
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        dragging = false;
    }

    public boolean clicked(double mouseX, double mouseY){
        return midPos.distance(new Vector2d(mouseX, mouseY)) <= getWidth() / 2.0;
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double distX, double distY){
        if(dragging) {
            setAngleFromMousePos(mouseX, mouseY);
        }
    }

    private void setAngleFromMousePos(double mouseX, double mouseY){
        Vector2d toMouse = new Vector2d(mouseX, mouseY).sub(midPos);
        angle = new Vector2d(0.0,1.0).angle(toMouse) / Math.PI;
        changedListener.accept(getAngle());
    }
}
