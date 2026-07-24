package com.miofeather.commandblockstudio.main.config;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.ui.screen.StudioScaledScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CommandInsightLanguageScreen extends StudioScaledScreen {
    private final Screen parent;

    public CommandInsightLanguageScreen(Screen parent) {
        super(Component.translatable("cbs.config.insightLanguage.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        prepareStudioScale();
        int buttonWidth = 180;
        int buttonHeight = 20;
        int x = this.width / 2 - buttonWidth / 2;
        int y = this.height / 2 - 24;

        addRenderableWidget(Button.builder(languageButtonText("zh_cn", "中文"), button -> setLanguage("zh_cn"))
                .bounds(x, y, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(languageButtonText("en_us", "English"), button -> setLanguage("en_us"))
                .bounds(x, y + 26, buttonWidth, buttonHeight)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, button -> onClose())
                .bounds(x, y + 60, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void onClose() {
        CommandBlockStudio.writeConfig();
        if (minecraft != null) {
            minecraft.gui.setScreen(parent);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int studioMouseX = studioMouseX(mouseX);
        int studioMouseY = studioMouseY(mouseY);
        beginStudioRender(graphics);
        graphics.fill(0, 0, width, height, 0xFF0B0E12);
        graphics.fill(0, 0, width, 34, 0xFF20242A);
        graphics.fill(0, 34, width, 35, 0xFF343A42);
        super.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
        graphics.centeredText(font, title, width / 2, 14, 0xFFFFFFFF);
        List<FormattedCharSequence> hintLines = font.split(
                Component.translatable("cbs.config.insightLanguage.hint"),
                Math.max(120, Math.min(300, width - 24))
        );
        int hintY = height / 2 - 42 - Math.max(0, hintLines.size() - 1) * (font.lineHeight + 2);
        for (int i = 0; i < hintLines.size(); i++) {
            graphics.centeredText(font, hintLines.get(i), width / 2, hintY + i * (font.lineHeight + 2), 0xFFA0A0A0);
        }
        endStudioRender(graphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(studioMouseX(mouseX), studioMouseY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(studioMouseX(mouseX), studioMouseY(mouseY), button);
    }

    private void setLanguage(String language) {
        CommandBlockStudio.setConfig(CommandBlockStudio.VAR_COMMAND_INSIGHT_LANGUAGE, language);
        rebuildWidgets();
    }

    private Component languageButtonText(String language, String label) {
        boolean selected = CommandBlockStudio.COMMAND_INSIGHT_LANGUAGE.equalsIgnoreCase(language);
        return Component.literal((selected ? "> " : "  ") + label);
    }
}
