package com.miofeather.commandblockstudio.main.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;


import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import static com.miofeather.commandblockstudio.main.CommandBlockStudio.SCROLLBAR_HORIZONTAL;
import static com.miofeather.commandblockstudio.main.CommandBlockStudio.SCROLLBAR_VERTICAL;

public class ScrollbarWidget extends AbstractWidget {
    private static final int COMPACT_THICKNESS = 3;
    protected boolean dragging = false;
    protected boolean horizontal = false;
    protected double prevMouseX = 0.0d;
    protected double prevMouseY = 0.0d;
    protected double pos = 0.0d;
    protected double scale;
    protected int length;
    protected int barLength;
    protected int frameRepeatLength;
    protected int barRepeatLength;
    protected final int textureLength = 256;
    protected java.util.function.Consumer<Double> changedListener;

    public ScrollbarWidget(int x, int y, int width, int height, Component message, boolean horizontal) {
        super(x, y, width, height, message);
        this.horizontal = horizontal;
        this.scale = 1.0d;
        recalculateGeometry();
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }
        this.isHovered = isTrackHovered(mouseX, mouseY);

        this.renderFrame(graphics);
        this.renderSlider(graphics, mouseX, mouseY, delta);
    }

    protected void renderFrame(GuiGraphicsExtractor graphics){
        renderLongBox(graphics, false, false, 0, horizontal ? width : height, frameRepeatLength);
    }

    protected void renderSlider(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        renderLongBox(graphics, true, isHovered, (int)(pos * (length - barLength)), barLength, barRepeatLength);
    }

    protected void renderLongBox(GuiGraphicsExtractor graphics, boolean enabled, boolean hovered, int position, int boxLength, int repeatLength){
        int thickness = currentThickness();
        if(horizontal){
            Identifier textures = SCROLLBAR_HORIZONTAL.get(enabled,hovered);
            int drawY = this.getY() + this.height - thickness;
            int sourceY = 10 - thickness;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textures, textureLength, 10, 0, sourceY, this.getX() + position, drawY, Math.min(boxLength / 2, textureLength / 2), thickness);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textures, textureLength, 10, Math.max(textureLength/2, textureLength - boxLength / 2), sourceY, Math.max(this.getX() + position + boxLength/2, this.getX() + position + boxLength - textureLength/2), drawY, Math.min(boxLength / 2, textureLength / 2), thickness);
            int drawX = this.getX() + position + textureLength/2;
            for (int i=0; i<(repeatLength/(textureLength/2))+1; i++){
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textures, textureLength, 10, textureLength/4, sourceY, drawX, drawY, Math.min((repeatLength - i*textureLength/2), textureLength/2), thickness);
                drawX += textureLength/2;
            }
        } else {
            Identifier textures = SCROLLBAR_VERTICAL.get(enabled,hovered);
            int drawX = this.getX() + this.width - thickness;
            int sourceX = 10 - thickness;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textures, 10 , textureLength, sourceX, 0, drawX, this.getY() + position, thickness, Math.min(boxLength / 2, textureLength / 2));
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textures, 10 , textureLength, sourceX, Math.max(textureLength/2, textureLength - boxLength / 2), drawX, Math.max(this.getY() + position + boxLength/2, this.getY() + position + boxLength - textureLength/2), thickness, Math.min(boxLength / 2, textureLength / 2));
            int drawY = this.getY() + textureLength/2;
            for (int i=0; i<(repeatLength/(textureLength/2))+1; i++){
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, textures, 10, textureLength, sourceX, textureLength/4, drawX, drawY, thickness, Math.min((repeatLength - i*textureLength/2), textureLength/2));
                drawY += textureLength/2;
            }
        }
    }

    private int currentThickness() {
        int fullThickness = horizontal ? this.height : this.width;
        return this.isHovered || dragging ? fullThickness : Math.min(COMPACT_THICKNESS, fullThickness);
    }

    private boolean isTrackHovered(double mouseX, double mouseY) {
        return mouseX >= this.getX() && mouseX < this.getX() + this.width
                && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }

    public void setChangedListener(java.util.function.Consumer<Double> changedListener){
        this.changedListener = changedListener;
    }

    /*@Override
    protected boolean clicked(double mouseX, double mouseY){
        if (!this.visible) {
            return false;
        }
        return checkHovered(mouseX, mouseY);
    }*/

    protected boolean checkHovered(double mouseX, double mouseY){
        if(horizontal){
            return mouseX >= this.getX() + pos * (length-barLength) && mouseY >= this.getY() && mouseX < this.getX() + pos * (length-barLength) + barLength && mouseY < this.getY() + this.height;
        } else {
            return mouseX >= this.getX() && mouseY >= this.getY() + pos * (length-barLength) && mouseX < this.getX() + this.width && mouseY < this.getY() + pos * (length-barLength) + barLength;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.visible && button == 0 && this.isTrackHovered(mouseX, mouseY)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onClick(mouseX, mouseY, button);
            return true;
        }
        return false;
    }

    public void onClick(double mouseX, double mouseY, int button) {
        if (!this.visible) {
            return;
        }
        int travel = length - barLength;
        double axis = horizontal ? mouseX - this.getX() : mouseY - this.getY();
        double barStart = pos * travel;
        if (travel > 0 && (axis < barStart || axis >= barStart + barLength)) {
            double previous = pos;
            pos = Mth.clamp((axis - barLength / 2.0D) / travel, 0.0D, 1.0D);
            if (changedListener != null && Math.abs(previous - pos) > 0.0D) {
                changedListener.accept(pos);
            }
        }
        dragging = true;
        prevMouseX = mouseX;
        prevMouseY = mouseY;
    }

    public void onRelease(double mouseX, double mouseY) {
        if (!this.visible) {
            return;
        }
        dragging = false;
    }

    public void onDrag(double mouseX, double mouseY, double distX, double distY){
        if(dragging) {
            int travel = length - barLength;
            if (travel <= 0) {
                return;
            }
            double posBefore = pos;
            if (horizontal) {
                pos = Math.min(Math.max(pos + distX/travel, 0), 1);
            } else {
                pos = Math.min(Math.max(pos + distY/travel, 0), 1);
            }
            if ((changedListener != null) && (Math.abs(posBefore-pos) > 0.0d)){
                changedListener.accept(pos);
            }
        }
    }

    public void setScale(double newScale){
        this.scale = Math.max(newScale,1);
        this.visible = this.scale > 1.0001d;
        if (!this.visible) {
            this.pos = 0.0d;
            this.dragging = false;
        }
        recalculateGeometry();
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        if (horizontal) {
            recalculateGeometry();
        }
    }

    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        if (!horizontal) {
            recalculateGeometry();
        }
    }

    private void recalculateGeometry() {
        this.length = Math.max(1, horizontal ? this.width : this.height);
        this.barLength = Mth.clamp((int) ((double) length / Math.min(scale, 8)), 1, length);
        this.frameRepeatLength = length - textureLength;
        this.barRepeatLength = barLength - textureLength;
    }

    public boolean isScrollable() {
        return this.visible;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void updatePos(double newPos){
        this.pos = Math.max(Math.min(newPos,1),0);
    }

    public double getPos(){
        return pos;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(new CharacterEvent(codePoint));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClick(event.x(), event.y(), event.button());
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        onRelease(event.x(), event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        onDrag(event.x(), event.y(), deltaX, deltaY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {

    }
}
