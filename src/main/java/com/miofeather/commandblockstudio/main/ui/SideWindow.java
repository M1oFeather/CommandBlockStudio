package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.ui.screen.AbstractCommandBlockStudioScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.layouts.LayoutElement;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;


import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedList;
import java.util.List;

public class SideWindow implements Renderable, GuiEventListener {
    protected static final WidgetSprites COPY_BUTTON_TEXTURES = new WidgetSprites(
            Identifier.parse("command_block_studio:button_copy_enabled"),
            Identifier.parse("command_block_studio:button_copy_disabled"),
            Identifier.parse("command_block_studio:button_copy_focused")
    );

    private static int piFraction = 4;
    private static double piSetting = 0.0;

    int x, y, width, height;
    int searchTitleY, searchFindLabelY, searchReplaceLabelY, searchStatusY;
    int angleTitleY, colorTitleY;
    int topMargin = 29;
    int leftMargin = 8;
    boolean visible = false;

    MultiLineTextFieldWidget commandField;
    Screen screen;

    String piFractionInputText = "2π / ";
    EditBox searchInput, replaceInput;
    EditBox piFractionInput, piOutput, colorTextR, colorTextG, colorTextB, colorHex, colorInt;
    Button searchTabButton, converterTabButton;
    Button searchPreviousButton, searchNextButton, replaceButton, replaceAllButton;
    Checkbox matchCaseCheckbox;
    ColorPaletteWidget colorPalette;
    NotchedSlider piSlider;
    RotationIndicator piRotationIndicator;
    Font textRenderer;

    List<AbstractWidget> widgets;
    int focusedWidget = -1;
    boolean syncingColorControls;
    boolean matchCase;
    ToolView toolView = ToolView.SEARCH;
    Component searchStatus = Component.translatable("cbs.tools.search.enter");
    String cachedSearchCommand = "";
    String cachedSearchQuery = "";
    boolean cachedMatchCase;

    public SideWindow(int x, int y, int width, int height, MultiLineTextFieldWidget commandField, Screen screen){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.commandField = commandField;
        this.screen = screen;

        this.widgets = new LinkedList<>();

        int posY = y + topMargin;
        Minecraft minecraftClient = Minecraft.getInstance();
        this.textRenderer = minecraftClient.font;
        int contentWidth = Math.max(48, width - leftMargin * 2);

        int tabGap = 2;
        int tabWidth = Math.max(22, (contentWidth - tabGap) / 2);
        this.searchTabButton = (Button) addWidget(Button.builder(
                        Component.translatable("cbs.tools.tab.search"),
                        button -> setToolView(ToolView.SEARCH))
                .bounds(x + leftMargin, posY, tabWidth, 18)
                .build());
        this.converterTabButton = (Button) addWidget(Button.builder(
                        Component.translatable("cbs.tools.tab.converter"),
                        button -> setToolView(ToolView.CONVERTER))
                .bounds(x + leftMargin + tabWidth + tabGap, posY,
                        Math.max(22, contentWidth - tabWidth - tabGap), 18)
                .build());
        posY += 22;

        this.searchTitleY = posY;
        posY += 12;
        Component findLabel = Component.translatable("cbs.tools.search.find");
        Component replaceLabel = Component.translatable("cbs.tools.search.replaceWith");
        int searchLabelWidth = Math.max(textRenderer.width(findLabel), textRenderer.width(replaceLabel));
        int searchFieldX = x + leftMargin + searchLabelWidth + 5;
        int searchFieldWidth = Math.max(48, x + width - leftMargin - searchFieldX);
        this.searchFindLabelY = posY + 3;
        this.searchInput = (EditBox) addWidget(new EditBox(
                textRenderer,
                searchFieldX,
                posY,
                searchFieldWidth,
                14,
                Component.translatable("cbs.tools.search.find")
        ));
        this.searchInput.setMaxLength(256);
        this.searchInput.setResponder(value -> updateSearchStatus());

        posY += 17;
        this.searchReplaceLabelY = posY + 3;
        this.replaceInput = (EditBox) addWidget(new EditBox(
                textRenderer,
                searchFieldX,
                posY,
                searchFieldWidth,
                14,
                Component.translatable("cbs.tools.search.replaceWith")
        ));
        this.replaceInput.setMaxLength(2048);

        posY += 18;
        int gap = 2;
        int navWidth = 20;
        int actionWidth = Math.max(32, (contentWidth - navWidth * 2 - gap * 3) / 2);
        int buttonX = x + leftMargin;
        this.searchPreviousButton = (Button) addWidget(Button.builder(
                        Component.literal("↑"),
                        button -> findMatch(true))
                .bounds(buttonX, posY, navWidth, 16)
                .tooltip(Tooltip.create(Component.translatable("cbs.tools.search.previous")))
                .build());
        buttonX += navWidth + gap;
        this.searchNextButton = (Button) addWidget(Button.builder(
                        Component.literal("↓"),
                        button -> findMatch(false))
                .bounds(buttonX, posY, navWidth, 16)
                .tooltip(Tooltip.create(Component.translatable("cbs.tools.search.next")))
                .build());
        buttonX += navWidth + gap;
        this.replaceButton = (Button) addWidget(Button.builder(
                        Component.translatable("cbs.tools.search.replace"),
                        button -> replaceCurrent())
                .bounds(buttonX, posY, actionWidth, 16)
                .build());
        buttonX += actionWidth + gap;
        this.replaceAllButton = (Button) addWidget(Button.builder(
                        Component.translatable("cbs.tools.search.all"),
                        button -> replaceAll())
                .bounds(buttonX, posY, Math.max(24, x + width - leftMargin - buttonX), 16)
                .build());

        posY += 19;
        this.searchStatusY = posY + 3;
        this.matchCaseCheckbox = (Checkbox) addWidget(Checkbox.builder(Component.literal("Aa"), textRenderer)
                .pos(x + leftMargin, posY)
                .selected(false)
                .onValueChange((checkbox, checked) -> {
                    matchCase = checked;
                    updateSearchStatus();
                })
                .tooltip(Tooltip.create(Component.translatable("cbs.tools.search.matchCase")))
                .build());
        posY = y + topMargin + 22;

        this.angleTitleY = posY;
        posY += 12;
        int angleLabelWidth = textRenderer.width(piFractionInputText);
        int angleGap = 4;
        int rotationSize = 24;
        int angleRowRight = x + width - leftMargin - rotationSize - angleGap;
        int fractionInputX = x + leftMargin + angleLabelWidth;
        int fractionInputWidth = Mth.clamp((angleRowRight - fractionInputX - angleGap) / 2, 32, 60);
        this.piFractionInput = (EditBox) addWidget(
                new EditBox(
                        textRenderer,
                        fractionInputX,
                        posY,
                        fractionInputWidth,
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.piFractionInput.setResponder((input)->{
            try {
                piFraction = Math.min(Math.max(Integer.parseInt(input),1),16);
                this.piSlider.setSubdivisions(piFraction);
            } catch (NumberFormatException e){
                this.piSlider.setSubdivisions(4);
            }
        });

        int outputX = this.piFractionInput.getX() + this.piFractionInput.getWidth() + angleGap;
        this.piOutput = (EditBox) addWidget(
                new OutputTextFieldWidget(
                        textRenderer,
                        outputX,
                        posY,
                        Math.max(28, angleRowRight - outputX),
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.piOutput.setEditable(false);
        String piOutputText = Double.toString(piSetting * 2*Math.PI);
        this.piOutput.setValue(piOutputText.substring(0, Math.min(8,piOutputText.length())));

        this.piRotationIndicator = (RotationIndicator) addWidget(
                new RotationIndicator(
                        x + width - leftMargin - rotationSize,
                        posY - 6,
                        rotationSize,
                        Component.nullToEmpty("")
                )
        );
        this.piRotationIndicator.setChangedListener((value)->{
            this.piSlider.setPos(value);
            piSetting = value;
            String text = Double.toString(piSetting * 2*Math.PI);
            this.piOutput.setValue(text.substring(0, Math.min(8,text.length())));
        });
        this.piRotationIndicator.setAngle(piSetting);

        posY += rotationSize;
        this.piSlider = (NotchedSlider) addWidget(new NotchedSlider(
                x + leftMargin,
                posY,
                contentWidth,
                12,
                Component.nullToEmpty("")
        ));
        this.piSlider.setSubdivisions(piFraction);
        this.piSlider.setPos(piSetting);
        this.piFractionInput.setValue(String.valueOf(piFraction));
        this.piSlider.setChangedListener((value)->{
            piSetting = value;
            this.piRotationIndicator.setAngle(value);
            String text = Double.toString(piSetting * 2*Math.PI);
            this.piOutput.setValue(text.substring(0, Math.min(8,text.length())));
        });

        posY += 16;
        this.colorTitleY = posY;
        posY += 12;
        // Keep the RGB and value outputs inside the dock on compact GUI scales.
        int paletteHeight = Mth.clamp(height - (posY - y) - 72, 24, 72);
        this.colorPalette = (ColorPaletteWidget) addWidget(new ColorPaletteWidget(
                x + leftMargin,
                posY,
                contentWidth,
                paletteHeight,
                Component.translatable("cbs.tools.color.palette")
        ));
        this.colorPalette.setChangedListener(rgb -> {
            ColorPicker.setInteger(rgb);
            syncColorControls();
        });

        posY += paletteHeight + 8;
        int channelSlotWidth = Math.max(16, contentWidth / 3);
        int channelFieldWidth = Math.max(14, channelSlotWidth - textRenderer.width("R:") - 3);
        this.colorTextR = (EditBox) addWidget(
                new EditBox(
                        textRenderer,
                        x + leftMargin + textRenderer.width("R:"),
                        posY,
                        channelFieldWidth,
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.colorTextR.setMaxLength(3);
        this.colorTextR.setResponder((input)->{
            updateColorChannel(ColorPicker.COLOR.RED, input);
        });
        this.colorTextG = (EditBox) addWidget(
                new EditBox(
                        textRenderer,
                        x + leftMargin + channelSlotWidth + textRenderer.width("G:"),
                        posY,
                        channelFieldWidth,
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.colorTextG.setMaxLength(3);
        this.colorTextG.setResponder((input)->{
            updateColorChannel(ColorPicker.COLOR.GREEN, input);
        });
        this.colorTextB = (EditBox) addWidget(
                new EditBox(
                        textRenderer,
                        x + leftMargin + channelSlotWidth * 2 + textRenderer.width("B:"),
                        posY,
                        channelFieldWidth,
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.colorTextB.setMaxLength(3);
        this.colorTextB.setResponder((input)->{
            updateColorChannel(ColorPicker.COLOR.BLUE, input);
        });

        posY += 13;
        int colorOutputX = x + leftMargin + 25;
        int outputWidth = Math.max(23, contentWidth - 25);
        this.colorHex = (EditBox) addWidget(
                new OutputTextFieldWidget(
                        textRenderer,
                        colorOutputX,
                        posY,
                        outputWidth,
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.colorHex.setEditable(false);
        posY += 12;
        this.colorInt = (EditBox) addWidget(
                new OutputTextFieldWidget(
                        textRenderer,
                        colorOutputX,
                        posY,
                        outputWidth,
                        10,
                        Component.nullToEmpty("")
                )
        );
        this.colorInt.setEditable(false);
        syncColorControls();
        updateSearchStatus();
        updateToolViewVisibility();
    }

    private void updateSearchStatus() {
        if (searchInput == null || searchPreviousButton == null) {
            return;
        }
        String query = searchInput.getValue();
        boolean hasQuery = !query.isEmpty();
        int matches = commandField.countMatches(query, matchCase);
        searchStatus = !hasQuery
                ? Component.translatable("cbs.tools.search.enter")
                : matches == 0
                ? Component.translatable("cbs.tools.search.none")
                : Component.translatable("cbs.tools.search.matches", matches);
        searchPreviousButton.active = hasQuery && matches > 0;
        searchNextButton.active = hasQuery && matches > 0;
        replaceButton.active = hasQuery && matches > 0;
        replaceAllButton.active = hasQuery && matches > 0;
        cachedSearchCommand = commandField.getValue();
        cachedSearchQuery = query;
        cachedMatchCase = matchCase;
    }

    private void refreshSearchStatusIfChanged() {
        if (!cachedSearchCommand.equals(commandField.getValue())
                || !cachedSearchQuery.equals(searchInput.getValue())
                || cachedMatchCase != matchCase) {
            updateSearchStatus();
        }
    }

    private void setToolView(ToolView view) {
        this.toolView = view;
        setFocused(false);
        updateToolViewVisibility();
    }

    private void updateToolViewVisibility() {
        boolean searchVisible = visible && toolView == ToolView.SEARCH;
        boolean converterVisible = visible && toolView == ToolView.CONVERTER;
        searchTabButton.visible = visible;
        converterTabButton.visible = visible;
        searchTabButton.active = toolView != ToolView.SEARCH;
        converterTabButton.active = toolView != ToolView.CONVERTER;

        searchInput.visible = searchVisible;
        replaceInput.visible = searchVisible;
        searchPreviousButton.visible = searchVisible;
        searchNextButton.visible = searchVisible;
        replaceButton.visible = searchVisible;
        replaceAllButton.visible = searchVisible;
        matchCaseCheckbox.visible = searchVisible;

        piFractionInput.visible = converterVisible;
        piSlider.visible = converterVisible;
        piOutput.visible = converterVisible;
        piRotationIndicator.visible = converterVisible;
        colorPalette.visible = converterVisible;
        colorTextR.visible = converterVisible;
        colorTextG.visible = converterVisible;
        colorTextB.visible = converterVisible;
        colorHex.visible = converterVisible;
        colorInt.visible = converterVisible;
    }

    private void findMatch(boolean backwards) {
        commandField.findMatch(searchInput.getValue(), backwards, matchCase);
        updateSearchStatus();
    }

    private void replaceCurrent() {
        commandField.replaceCurrentMatch(searchInput.getValue(), replaceInput.getValue(), matchCase);
        updateSearchStatus();
    }

    private void replaceAll() {
        int count = commandField.replaceAllMatches(searchInput.getValue(), replaceInput.getValue(), matchCase);
        updateSearchStatus();
        searchStatus = count == 0
                ? Component.translatable("cbs.tools.search.none")
                : Component.translatable("cbs.tools.search.replaced", count);
        cachedSearchCommand = commandField.getValue();
        cachedSearchQuery = searchInput.getValue();
        cachedMatchCase = matchCase;
    }

    public void focusSearch(boolean replacement) {
        setToolView(ToolView.SEARCH);
        EditBox target = replacement ? replaceInput : searchInput;
        setFocused(false);
        target.setFocused(true);
        target.setCursorPosition(target.getValue().length());
        target.setHighlightPos(0);
        focusedWidget = widgets.indexOf(target);
    }

    public boolean repeatSearch(boolean backwards) {
        if (searchInput.getValue().isEmpty()) {
            focusSearch(false);
            return false;
        }
        findMatch(backwards);
        return true;
    }

    public void updateColorOutputs(){
        String hexText = ColorPicker.getHexString().toUpperCase();
        colorHex.setValue("#"+"0".repeat(6-hexText.length())+hexText);
        colorInt.setValue(""+ColorPicker.getInteger());
    }

    private void updateColorChannel(ColorPicker.COLOR channel, String input) {
        if (syncingColorControls) {
            return;
        }
        int value = 0;
        try {
            value = Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
        }
        ColorPicker.setColor(channel, value);
        colorPalette.setColor(ColorPicker.getInteger());
        updateColorOutputs();
    }

    private void syncColorControls() {
        syncingColorControls = true;
        colorTextR.setValue(Integer.toString(ColorPicker.getColor(ColorPicker.COLOR.RED)));
        colorTextG.setValue(Integer.toString(ColorPicker.getColor(ColorPicker.COLOR.GREEN)));
        colorTextB.setValue(Integer.toString(ColorPicker.getColor(ColorPicker.COLOR.BLUE)));
        colorPalette.setColor(ColorPicker.getInteger());
        updateColorOutputs();
        syncingColorControls = false;
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if(!visible) return;

        graphics.fill(x, y, x + width, y + height, 0xFF16191E);
        graphics.fill(x, y, x + width, y + 23, 0xFF1C2026);
        graphics.fill(x, y, x + 1, y + height, 0xFF3A424C);
        graphics.fill(x, y + 23, x + width, y + 24, 0xFF343A42);
        graphics.text(this.textRenderer, Component.translatable("cbs.panel.tools"), x + leftMargin, y + 8, 0xFFDDE2E7);
        this.searchTabButton.extractRenderState(graphics, mouseX, mouseY, delta);
        this.converterTabButton.extractRenderState(graphics, mouseX, mouseY, delta);

        if (toolView == ToolView.SEARCH) {
            refreshSearchStatusIfChanged();
            graphics.text(this.textRenderer, Component.translatable("cbs.tools.search"), x + leftMargin, searchTitleY, 0xFFE6F3FF);
            graphics.text(this.textRenderer, Component.translatable("cbs.tools.search.find"), x + leftMargin, searchFindLabelY, 0xFFB7C0CA);
            graphics.text(this.textRenderer, Component.translatable("cbs.tools.search.replaceWith"), x + leftMargin, searchReplaceLabelY, 0xFFB7C0CA);
            this.searchInput.extractRenderState(graphics, mouseX, mouseY, delta);
            this.replaceInput.extractRenderState(graphics, mouseX, mouseY, delta);
            this.searchPreviousButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.searchNextButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.replaceButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.replaceAllButton.extractRenderState(graphics, mouseX, mouseY, delta);
            this.matchCaseCheckbox.extractRenderState(graphics, mouseX, mouseY, delta);
            int statusX = matchCaseCheckbox.getX() + matchCaseCheckbox.getWidth() + 5;
            int statusWidth = Math.max(10, x + width - leftMargin - statusX);
            String visibleStatus = textRenderer.plainSubstrByWidth(searchStatus.getString(), statusWidth);
            graphics.text(this.textRenderer, visibleStatus, statusX, searchStatusY, 0xFF8A949E);
        } else {
            graphics.text(this.textRenderer, Component.translatable("cbs.tools.angle"), x + leftMargin, angleTitleY, 0xFFE6F3FF);
            graphics.text(this.textRenderer, "2π / ", x + leftMargin, piFractionInput.getY(), 0xFFFFFFFF);
            this.piFractionInput.extractRenderState(graphics, mouseX, mouseY, delta);
            this.piSlider.extractRenderState(graphics, mouseX, mouseY, delta);
            this.piOutput.extractRenderState(graphics, mouseX, mouseY, delta);
            this.piRotationIndicator.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.fill(x + leftMargin, colorTitleY - 6, x + width - leftMargin, colorTitleY - 5, 0xFF343A42);
            graphics.text(this.textRenderer, Component.translatable("cbs.tools.color"), x + leftMargin, colorTitleY, 0xFFE6F3FF);
            this.colorPalette.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.text(this.textRenderer, "R:", x + leftMargin, colorTextR.getY(), 0xFFFF0000);
            this.colorTextR.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.text(this.textRenderer, "G:", colorTextG.getX() - textRenderer.width("G:"), colorTextG.getY(), 0xFF00FF00);
            this.colorTextG.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.text(this.textRenderer, "B:", colorTextB.getX() - textRenderer.width("B:"), colorTextB.getY(), 0xFF5C7CFA);
            this.colorTextB.extractRenderState(graphics, mouseX, mouseY, delta);
            graphics.fill(x + leftMargin, colorHex.getY(), x + leftMargin + 20, colorInt.getY() + 10, 0xFF000000 | ColorPicker.getInteger());
            graphics.outline(x + leftMargin, colorHex.getY(), 20, colorInt.getY() + 10 - colorHex.getY(), 0xFF8B949E);
            this.colorHex.extractRenderState(graphics, mouseX, mouseY, delta);
            this.colorInt.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    LayoutElement addWidget(AbstractWidget widget){
        widgets.add(widget);
        return widget;
    }

    public void setVisible(boolean value){
        this.visible = value;
        updateToolViewVisibility();
        if (value) {
            updateSearchStatus();
        }
        if (!value) {
            setFocused(false);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setFocused(boolean focused) {
        if(!focused){
            for (AbstractWidget widget : widgets) {
                widget.setFocused(false);
            }
        }
    }

    @Override
    public boolean isFocused() {
        for(AbstractWidget w : widgets){
            if (w.isFocused()) return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!visible) return false;
        boolean widgetClicked = false;
        int index = 0;
        for(AbstractWidget w : widgets){
            if(!widgetClicked && w.mouseClicked(StudioInputEvents.mouse(mouseX, mouseY, button), false)){
                w.setFocused(true);
                widgetClicked = true;
                focusedWidget = index;
            } else {
                w.setFocused(false);
            }
            index++;
        }
        boolean returnVal = widgetClicked;
        if(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) returnVal = true;
        if (returnVal){
            ((AbstractCommandBlockStudioScreen)screen).sideWindowFocused();
            return true;
        }
        setFocused(false);
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(!visible) return false;
        boolean handled = false;
        for(AbstractWidget w : widgets){
            handled |= w.mouseReleased(StudioInputEvents.mouse(mouseX, mouseY, button));
        }
        return handled;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY){
        if(!visible) return false;
        boolean handled = false;
        for(AbstractWidget w : widgets){
            if(!(w instanceof EditBox)) {
                handled |= w.mouseDragged(StudioInputEvents.mouse(mouseX, mouseY, button), deltaX, deltaY);
            }
        }
        return handled || (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers){
        if(!visible) return false;
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (searchInput.isFocused()) {
                findMatch(Minecraft.getInstance().hasShiftDown());
                return true;
            }
            if (replaceInput.isFocused()) {
                replaceCurrent();
                return true;
            }
        }
        for(AbstractWidget w : widgets){
            if(w.keyPressed(StudioInputEvents.key(keyCode, scanCode, modifiers))) return true;
        }
        if (keyCode == 258) {
            focusedWidget += Minecraft.getInstance().hasShiftDown() ? -1 : 1;
            focusedWidget %= widgets.size();
            if (focusedWidget < 0) focusedWidget = widgets.size() + focusedWidget;

            /*if (widgets.get(focusedWidget) instanceof ColorScrollbarWidget){
                keyPressed(input); // Skip color sliders
            } else {
                int index = 0;
                for(ClickableWidget w : widgets){
                    w.setFocused(index == focusedWidget);
                    index++;
                }
            }*/
        }
        return false;
    }

    public boolean charTyped(char codePoint, int modifiers){
        if(!visible) return false;
        for(AbstractWidget w : widgets){
            if(w.charTyped(StudioInputEvents.character(codePoint))) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return mouseReleased(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        return mouseDragged(event.x(), event.y(), event.button(), deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    private enum ToolView {
        SEARCH,
        CONVERTER
    }
}
