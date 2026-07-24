package com.miofeather.commandblockstudio.main.config;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.ui.MultiLineCommandSuggestor;
import com.miofeather.commandblockstudio.main.ui.MultiLineTextFieldWidget;
import com.miofeather.commandblockstudio.main.ui.screen.AbstractCommandBlockStudioScreen;
import com.miofeather.commandblockstudio.main.ui.screen.StudioScaledScreen;
import com.miofeather.commandblockstudio.main.ui.screen.StudioUiScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class ConfigScreen extends StudioScaledScreen {

    private Screen parent;
    private final String sampleText = "setblock ~ ~1 ~ oak_hanging_sign{front_text:{messages:['[\"[Brackets within a String]\"]','[\"Some more text\"]','[\"A really long example text to demonstrate text wraparound. Remember to stay hydrated and take care of yourself, I hope you have a lovely day :)\"]','[\"\"]']}}";
    public MultiLineTextFieldWidget textField;
    private MultiLineCommandSuggestor commandSuggestor;

    private static final int HEADER_HEIGHT = 34;
    private static final int CONTENT_TOP = 40;
    private static final int NUMBER_INPUT_WIDTH = 40;
    private static final int ROW_HEIGHT = 18;
    private static final int UI_SCALE_MIN = 70;
    private static final int UI_SCALE_MAX = 125;
    private static final int UI_SCALE_STEP = 5;
    private static final int UI_SCALE_SEGMENTS = (UI_SCALE_MAX - UI_SCALE_MIN) / UI_SCALE_STEP + 1;
    private Checkbox newLinePreOpen, newLinePostOpen, newLinePreClose, newLinePostClose, //newLinePostLastClose,
            newLinePostComma, avoidDoubleNewline, formatStrings, autosave, confirmUnsavedExit, bracketAutocomplete;
    private EditBox indentationFac, wraparound, scrollSpeedX, scrollSpeedY;
    private Button back, insightLanguage;
    private UiScaleSlider uiScale;
    private int contentX, contentWidth;
    private int previewPanelX, previewPanelWidth, settingsPanelX, settingsPanelWidth;
    private int settingsInnerX, settingsInnerWidth;
    private int beforeColumnCenter, afterColumnCenter;
    private int indentationLabelX, wraparoundLabelX, scrollSpeedXLabelX, scrollSpeedYLabelX;
    private boolean scaleLayoutDirty;

    public ConfigScreen() {
        this(null);
    }

    public ConfigScreen(Screen parent){
        super(Component.translatable("cbs.config.title"));
        this.parent = parent;
    }

    private void setup(){
        if (parent != null){
            back = Button.builder(CommonComponents.GUI_BACK,
                button -> onClose())
                .bounds(8, 8, 50, 20)
                .build();
        }
        insightLanguage = Button.builder(Component.translatable("cbs.config.insightLanguage"), button -> {
                    if (minecraft != null) {
                        minecraft.gui.setScreen(new CommandInsightLanguageScreen(this));
                    }
                })
                .bounds(0, 0, 150, 20)
                .build();
        uiScale = new UiScaleSlider(0, 0, 150, 20);
        uiScale.setTooltip(Tooltip.create(Component.translatable("cbs.config.uiScale.tooltip")));

        textField = new MultiLineTextFieldWidget(this.font, 0, CONTENT_TOP + 20, 220, 120, Component.literal(sampleText), this);
        textField.setMaxLength(32500);

        commandSuggestor = new MultiLineCommandSuggestor(this.minecraft, this, this.textField, this.font, true, true, 0, 7, false, Integer.MIN_VALUE);
        commandSuggestor.setAllowSuggestions(true);
        if (minecraft != null && minecraft.player != null) commandSuggestor.updateCommandInfo();

        textField.setCommandSuggestor(commandSuggestor);
        textField.setResponder(this::onCommandChanged);
        textField.setRawText(sampleText);
        textField.resetViewport();

        Checkbox.OnValueChange callback = new Checkbox.OnValueChange() {
            @Override
            public void onValueChange(Checkbox checkbox, boolean checked) {
                checkboxCallback(checkbox, checked);
            }
        };
        newLinePreOpen = Checkbox.builder(Component.literal(""), font).selected(CommandBlockStudio.NEWLINE_PRE_OPEN_BRACKET).onValueChange(callback).build();
        newLinePostOpen = Checkbox.builder(Component.empty(), font).selected(CommandBlockStudio.NEWLINE_POST_OPEN_BRACKET).onValueChange(callback).build();
        newLinePreClose = Checkbox.builder(Component.literal(""), font).selected(CommandBlockStudio.NEWLINE_PRE_CLOSE_BRACKET).onValueChange(callback).build();
        newLinePostClose = Checkbox.builder(Component.literal(""), font).selected(CommandBlockStudio.NEWLINE_POST_CLOSE_BRACKET).onValueChange(callback).build();
        //newLinePostLastClose = CheckboxWidget.builder(Text.literal("After last closing bracket"), textRenderer).checked(CommandBlockStudio.NEWLINE_POST_LAST_CLOSE_BRACKET).callback(callback).build();
        newLinePostComma = Checkbox.builder(Component.literal(""), font).selected(CommandBlockStudio.NEWLINE_POST_COMMA).onValueChange(callback).build();
        avoidDoubleNewline = Checkbox.builder(Component.translatable("cbs.config.avoidEmpty.short"), font).selected(CommandBlockStudio.AVOID_DOUBLE_NEWLINE).onValueChange(callback).build();
        avoidDoubleNewline.setTooltip(Tooltip.create(Component.translatable("cbs.config.avoidEmpty")));
        bracketAutocomplete = Checkbox.builder(Component.translatable("cbs.config.bracketAutocomplete.short"), font)
                .selected(CommandBlockStudio.BRACKET_AUTOCOMPLETE)
                .onValueChange(callback)
                .build();
        bracketAutocomplete.setTooltip(Tooltip.create(Component.translatable("cbs.config.bracketAutocomplete")));
        formatStrings = Checkbox.builder(Component.translatable("cbs.config.formatStrings.short"), font).selected(CommandBlockStudio.FORMAT_STRINGS).onValueChange(callback).build();
        formatStrings.setTooltip(Tooltip.create(Component.translatable("cbs.config.formatStrings")));
        autosave = Checkbox.builder(Component.translatable("cbs.config.autosave.short"), font).selected(CommandBlockStudio.AUTOSAVE).onValueChange(callback).build();
        autosave.setTooltip(Tooltip.create(Component.translatable("cbs.config.autosave")));
        confirmUnsavedExit = Checkbox.builder(Component.translatable("cbs.config.confirmUnsaved.short"), font)
                .selected(CommandBlockStudio.CONFIRM_UNSAVED_EXIT)
                .onValueChange(callback)
                .build();
        confirmUnsavedExit.setTooltip(Tooltip.create(Component.translatable("cbs.config.confirmUnsaved")));

        indentationFac = new EditBox(font, 0, 0, NUMBER_INPUT_WIDTH, 10, Component.translatable("cbs.config.indentation"));
        indentationFac.setValue(String.valueOf(CommandBlockStudio.INDENTATION_FACTOR));
        indentationFac.setResponder((input) -> {
            try {
                int inputInt = Math.min(Math.max(Integer.parseInt(input),1),16);
                CommandBlockStudio.setConfig(CommandBlockStudio.VAR_INDENTATION, String.valueOf(inputInt));
                this.textField.refreshFormatting();
            } catch (NumberFormatException e){
                CommandBlockStudio.INDENTATION_FACTOR = 2;
            }
        });
        wraparound = new EditBox(font, 0, 0, NUMBER_INPUT_WIDTH, 10, Component.translatable("cbs.config.wraparoundWidth"));
        wraparound.setValue(String.valueOf(CommandBlockStudio.WRAPAROUND_WIDTH));
        wraparound.setResponder((input) -> {
            try {
                int inputInt = Math.min(Math.max(Integer.parseInt(input),10),6400);
                CommandBlockStudio.setConfig(CommandBlockStudio.VAR_WRAPAROUND, String.valueOf(inputInt));
                this.textField.refreshFormatting();
            } catch (NumberFormatException e){
                CommandBlockStudio.WRAPAROUND_WIDTH = 200;
            }
        });
        scrollSpeedX = new EditBox(font, 0, 0, NUMBER_INPUT_WIDTH, 10, Component.translatable("cbs.config.scrollX"));
        scrollSpeedX.setValue(String.valueOf(CommandBlockStudio.SCROLL_STEP_X));
        scrollSpeedX.setResponder((input) -> {
            try {
                int inputInt = Math.min(Math.max(Integer.parseInt(input),1),64);
                CommandBlockStudio.setConfig(CommandBlockStudio.VAR_SCROLL_X, String.valueOf(inputInt));
            } catch (NumberFormatException e){
                CommandBlockStudio.SCROLL_STEP_X = 4;
            }
        });
        scrollSpeedY = new EditBox(font, 0, 0, NUMBER_INPUT_WIDTH, 10, Component.translatable("cbs.config.scrollY"));
        scrollSpeedY.setValue(String.valueOf(CommandBlockStudio.SCROLL_STEP_Y));
        scrollSpeedY.setResponder((input) -> {
            try {
                int inputInt = Math.min(Math.max(Integer.parseInt(input),1),64);
                CommandBlockStudio.setConfig(CommandBlockStudio.VAR_SCROLL_Y, String.valueOf(inputInt));
            } catch (NumberFormatException e){
                CommandBlockStudio.SCROLL_STEP_Y = 2;
            }
        });

    }

    @Override
    public void onClose() {
        CommandBlockStudio.writeConfig();
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
            if (parent instanceof AbstractCommandBlockStudioScreen) {
                ((AbstractCommandBlockStudioScreen) parent).returnFromConfig();
            }
        }
    }

    @Override
    protected void init(){
        prepareStudioScale();
        if (textField == null) {
            setup();
        }

        layoutWidgets();

        if (parent != null){
            addRenderableWidget(back);
        }
        addRenderableWidget(textField);
        this.setInitialFocus(this.textField);
        textField.setFocused(true);
        addRenderableWidget(newLinePreOpen);
        addRenderableWidget(newLinePostOpen);
        addRenderableWidget(newLinePreClose);
        addRenderableWidget(newLinePostClose);
        addRenderableWidget(newLinePostComma);
        addRenderableWidget(formatStrings);
        addRenderableWidget(avoidDoubleNewline);
        addRenderableWidget(autosave);
        addRenderableWidget(confirmUnsavedExit);
        addRenderableWidget(bracketAutocomplete);
        addRenderableWidget(indentationFac);
        addRenderableWidget(wraparound);
        addRenderableWidget(scrollSpeedX);
        addRenderableWidget(scrollSpeedY);
        addRenderableWidget(uiScale);
        addRenderableWidget(insightLanguage);
    }

    private void layoutWidgets() {
        int outerMargin = this.width < 480 ? 10 : 20;
        contentWidth = Math.max(260, Math.min(900, this.width - outerMargin * 2));
        contentX = (this.width - contentWidth) / 2;
        int panelGap = this.width < 480 ? 8 : 12;
        int maximumSettingsWidth = Math.max(120, contentWidth - panelGap - 140);
        settingsPanelWidth = Math.min(360, Math.max(160, contentWidth * 46 / 100));
        settingsPanelWidth = Math.min(settingsPanelWidth, maximumSettingsWidth);
        previewPanelWidth = contentWidth - panelGap - settingsPanelWidth;
        previewPanelX = contentX;
        settingsPanelX = previewPanelX + previewPanelWidth + panelGap;
        settingsInnerX = settingsPanelX + 4;
        settingsInnerWidth = settingsPanelWidth - 8;

        int contentBottom = Math.max(CONTENT_TOP + 176, this.height - 8);
        textField.setX(previewPanelX);
        textField.setY(CONTENT_TOP + 20);
        textField.setWidth(Math.max(80, previewPanelWidth - 11));
        textField.setHeight(Math.max(80, contentBottom - textField.getY() - 11));

        int optionAreaX = settingsInnerX + 18;
        int optionWidth = Math.max(40, settingsInnerWidth - 18);
        int optionCellWidth = optionWidth / 2;
        int beforeX = optionAreaX + Math.max(0, (optionCellWidth - newLinePreOpen.getWidth()) / 2);
        int afterX = optionAreaX + optionCellWidth + Math.max(0, (optionCellWidth - newLinePostOpen.getWidth()) / 2);
        beforeColumnCenter = beforeX + newLinePreOpen.getWidth() / 2;
        afterColumnCenter = afterX + newLinePostOpen.getWidth() / 2;

        int lineRowsTop = CONTENT_TOP + 40;
        newLinePreOpen.setPosition(beforeX, lineRowsTop);
        newLinePostOpen.setPosition(afterX, lineRowsTop);
        newLinePreClose.setPosition(beforeX, lineRowsTop + ROW_HEIGHT);
        newLinePostClose.setPosition(afterX, lineRowsTop + ROW_HEIGHT);
        newLinePostComma.setPosition(afterX, lineRowsTop + ROW_HEIGHT * 2);

        int cellGap = 4;
        int cellWidth = (settingsInnerWidth - cellGap) / 2;
        int secondCellX = settingsInnerX + cellWidth + cellGap;
        avoidDoubleNewline.setPosition(settingsInnerX, CONTENT_TOP + 96);
        formatStrings.setPosition(secondCellX, CONTENT_TOP + 96);
        bracketAutocomplete.setPosition(settingsInnerX, CONTENT_TOP + 114);
        autosave.setPosition(secondCellX, CONTENT_TOP + 114);
        confirmUnsavedExit.setPosition(settingsInnerX, CONTENT_TOP + 132);
        confirmUnsavedExit.active = !CommandBlockStudio.AUTOSAVE;

        indentationLabelX = settingsInnerX;
        wraparoundLabelX = secondCellX;
        scrollSpeedXLabelX = settingsInnerX;
        scrollSpeedYLabelX = secondCellX;
        positionNumericField(indentationFac, indentationLabelX, cellWidth, CONTENT_TOP + 150);
        positionNumericField(wraparound, wraparoundLabelX, cellWidth, CONTENT_TOP + 150);
        positionNumericField(scrollSpeedX, scrollSpeedXLabelX, cellWidth, CONTENT_TOP + 168);
        positionNumericField(scrollSpeedY, scrollSpeedYLabelX, cellWidth, CONTENT_TOP + 168);
        uiScale.setPosition(settingsInnerX, CONTENT_TOP + 188);
        uiScale.setWidth(settingsInnerWidth);
        uiScale.setMessage(uiScaleLabel());
        insightLanguage.setWidth(this.width < 400 ? 100 : 120);
        insightLanguage.setPosition(this.width - insightLanguage.getWidth() - 8, 8);
        insightLanguage.visible = true;
        insightLanguage.active = true;
    }

    private void positionNumericField(EditBox field, int cellX, int cellWidth, int y) {
        field.setPosition(cellX + Math.max(0, cellWidth - NUMBER_INPUT_WIDTH), y);
    }

    protected void onCommandChanged(String s){
        if (minecraft != null && minecraft.player != null) commandSuggestor.updateCommandInfo();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (scaleLayoutDirty) {
            refreshStudioScale();
            layoutWidgets();
            scaleLayoutDirty = false;
        }
        int studioMouseX = studioMouseX(mouseX);
        int studioMouseY = studioMouseY(mouseY);
        beginStudioRender(graphics);
        graphics.fill(0, 0, width, height, 0xFF0B0E12);
        graphics.fill(0, 0, width, HEADER_HEIGHT, 0xFF20242A);
        graphics.fill(previewPanelX, CONTENT_TOP, previewPanelX + previewPanelWidth, CONTENT_TOP + 17, 0xFF181C21);
        graphics.fill(previewPanelX, CONTENT_TOP + 16, previewPanelX + previewPanelWidth, CONTENT_TOP + 17, 0xFF343A42);
        graphics.fill(settingsPanelX, CONTENT_TOP, settingsPanelX + settingsPanelWidth, height - 8, 0xFF15191E);
        graphics.fill(settingsPanelX, CONTENT_TOP, settingsPanelX + settingsPanelWidth, CONTENT_TOP + 17, 0xFF181C21);
        graphics.fill(settingsPanelX, CONTENT_TOP + 16, settingsPanelX + settingsPanelWidth, CONTENT_TOP + 17, 0xFF343A42);
        graphics.text(font, Component.translatable("cbs.config.preview"), previewPanelX + 6, CONTENT_TOP + 4, 0xFFDDE2E7);
        graphics.text(font, Component.translatable("cbs.config.editorBehavior"), settingsPanelX + 6, CONTENT_TOP + 4, 0xFFDDE2E7);
        graphics.text(font, Component.translatable("cbs.config.lineBreaks"), settingsInnerX, CONTENT_TOP + 21, 0xFF9AA4AF);
        super.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
        int titleLeft = parent == null ? 8 : back.getX() + back.getWidth() + 8;
        int titleRight = insightLanguage.getX() - 8;
        int titleRadius = Math.max(10, Math.min(width / 2 - titleLeft, titleRight - width / 2));
        String visibleTitle = font.plainSubstrByWidth(getTitle().getString(), titleRadius * 2);
        graphics.centeredText(font, visibleTitle, width / 2, 14, 0xFFFFFFFF);
        if(minecraft == null || minecraft.player == null){
            int warningWidth = Math.max(0, previewPanelWidth - font.width(Component.translatable("cbs.config.preview")) - 20);
            String warning = font.plainSubstrByWidth(Component.translatable("cbs.config.colorError").getString(), warningWidth);
            graphics.text(font, warning, previewPanelX + previewPanelWidth - font.width(warning) - 6, CONTENT_TOP + 4, 0xFF808A95);
        }
        graphics.centeredText(font, Component.translatable("cbs.config.before"), beforeColumnCenter, CONTENT_TOP + 31, 0xFF9AA4AF);
        graphics.centeredText(font, Component.translatable("cbs.config.after"), afterColumnCenter, CONTENT_TOP + 31, 0xFF9AA4AF);
        graphics.text(font, Component.literal("{"), settingsInnerX + 2, newLinePreOpen.getY() + 5, 0xFFFFFFFF);
        graphics.text(font, Component.literal("}"), settingsInnerX + 2, newLinePreClose.getY() + 5, 0xFFFFFFFF);
        graphics.text(font, Component.literal(","), settingsInnerX + 2, newLinePostComma.getY() + 5, 0xFFFFFFFF);
        drawNumericFieldLabel(graphics, Component.translatable("cbs.config.indentation"), indentationFac, indentationLabelX);
        drawNumericFieldLabel(graphics, Component.translatable("cbs.config.wraparoundWidth.short"), wraparound, wraparoundLabelX);
        drawNumericFieldLabel(graphics, Component.translatable("cbs.config.scrollX.short"), scrollSpeedX, scrollSpeedXLabelX);
        drawNumericFieldLabel(graphics, Component.translatable("cbs.config.scrollY.short"), scrollSpeedY, scrollSpeedYLabelX);
        endStudioRender(graphics);
    }

    private void drawNumericFieldLabel(GuiGraphicsExtractor graphics, Component label, EditBox field, int labelX) {
        int availableWidth = field.getX() - labelX - 4;
        if (availableWidth <= 0) {
            return;
        }
        String visibleLabel = font.plainSubstrByWidth(label.getString(), availableWidth);
        graphics.text(font, visibleLabel, labelX, field.getY() + 2, 0xFFE0E0E0);
    }

    public void checkboxCallback(Checkbox source, boolean checked){
        if (source.equals(newLinePreOpen)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_NEWLINE_PRE_OPEN_BRACKET, String.valueOf(checked));
        if (source.equals(newLinePostOpen)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_NEWLINE_POST_OPEN_BRACKET, String.valueOf(checked));
        if (source.equals(newLinePreClose)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_NEWLINE_PRE_CLOSE_BRACKET, String.valueOf(checked));
        if (source.equals(newLinePostClose)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_NEWLINE_POST_CLOSE_BRACKET, String.valueOf(checked));
        //if (source.equals(newLinePostLastClose)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_NEWLINE_POST_LAST_CLOSE_BRACKET, String.valueOf(checked));
        if (source.equals(newLinePostComma)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_NEWLINE_POST_COMMA, String.valueOf(checked));
        if (source.equals(formatStrings)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_FORMAT_STRINGS, String.valueOf(checked));
        if (source.equals(autosave)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_AUTOSAVE, String.valueOf(checked));
        if (source.equals(confirmUnsavedExit)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_CONFIRM_UNSAVED_EXIT, String.valueOf(checked));
        if (source.equals(avoidDoubleNewline)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_AVOID_DOUBLE_NEWLINE, String.valueOf(checked));
        if (source.equals(bracketAutocomplete)) CommandBlockStudio.setConfig(CommandBlockStudio.VAR_BRACKET_AUTOCOMPLETE, String.valueOf(checked));
        confirmUnsavedExit.active = !CommandBlockStudio.AUTOSAVE;
        textField.refreshFormatting();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button){
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        if(commandSuggestor.mouseClicked(mouseX, mouseY, button)) return true;
        textField.onClick(mouseX, mouseY, button);
        /*if(textField.mouseClicked(click, doubled)) {
            setFocused(textField);
            return true;
        }*/
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY){
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        deltaX = studioMouseDelta(deltaX);
        deltaY = studioMouseDelta(deltaY);
        if(button == 0 && getFocused() == this.textField) {
            this.textField.onDrag(mouseX, mouseY, deltaX, deltaY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(studioMouseX(mouseX), studioMouseY(mouseY), scrollX, scrollY);
    }

    private Component uiScaleLabel() {
        if (CommandBlockStudio.UI_SCALE_PERCENT == StudioUiScale.AUTO) {
            int automaticPercent = Math.round(StudioUiScale.resolve(
                    StudioUiScale.AUTO,
                    getPhysicalWidth(),
                    getPhysicalHeight()
            ) * 100.0F);
            return Component.translatable("cbs.config.uiScale.auto", automaticPercent);
        }
        return Component.translatable("cbs.config.uiScale.value", CommandBlockStudio.UI_SCALE_PERCENT);
    }

    private static double sliderValueForScale(int scalePercent) {
        if (scalePercent == StudioUiScale.AUTO) {
            return 0.0D;
        }
        int manualIndex = Mth.clamp(
                Math.round((scalePercent - UI_SCALE_MIN) / (float) UI_SCALE_STEP),
                0,
                UI_SCALE_SEGMENTS - 1
        );
        return (manualIndex + 1) / (double) UI_SCALE_SEGMENTS;
    }

    private static int scaleForSliderValue(double value) {
        int index = Mth.clamp((int) Math.round(value * UI_SCALE_SEGMENTS), 0, UI_SCALE_SEGMENTS);
        return index == 0 ? StudioUiScale.AUTO : UI_SCALE_MIN + (index - 1) * UI_SCALE_STEP;
    }

    private final class UiScaleSlider extends AbstractSliderButton {
        private UiScaleSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), sliderValueForScale(CommandBlockStudio.UI_SCALE_PERCENT));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(uiScaleLabel());
        }

        @Override
        protected void applyValue() {
            int scalePercent = scaleForSliderValue(value);
            value = sliderValueForScale(scalePercent);
            if (scalePercent != CommandBlockStudio.UI_SCALE_PERCENT) {
                CommandBlockStudio.setConfig(CommandBlockStudio.VAR_UI_SCALE_PERCENT, String.valueOf(scalePercent));
                scaleLayoutDirty = true;
            }
            updateMessage();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button){
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        if(button == 0 && getFocused() == this.textField){
            this.textField.onRelease(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        
        if(keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.textField.keyPressed(keyCode, scanCode, modifiers);
        }
        if (this.commandSuggestor.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        
        if(keyCode == 340 || keyCode == 344){
            this.textField.keyReleased(keyCode, scanCode, modifiers);
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }
}
