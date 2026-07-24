package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.insight.CommandDiagnostic;
import com.miofeather.commandblockstudio.main.insight.CommandInsight;
import com.miofeather.commandblockstudio.main.insight.CommandSyntaxHint;
import com.miofeather.commandblockstudio.main.insight.ParticleDisplayNames;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ChatCommandAssistantPanel {
    private final Font font;
    private final EditBox input;
    private final ChatCommandSuggestor suggestor;
    private int x;
    private int y;
    private int width;
    private int height;
    private int scrollOffset;
    private int maxScroll;
    private int cachedWrapWidth = -1;
    private String cachedContentKey = "";
    private List<FormattedCharSequence> cachedLines = List.of();

    public ChatCommandAssistantPanel(Font font, EditBox input, ChatCommandSuggestor suggestor) {
        this.font = font;
        this.input = input;
        this.suggestor = suggestor;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(120, width);
        this.height = Math.max(90, height);
        suggestor.setPanelX(x);
    }

    public boolean isVisible() {
        return suggestor.isCommandMode();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!isVisible() || !contains(mouseX, mouseY)) {
            return false;
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(amount) * 20, 0, maxScroll);
        return true;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!isVisible()) {
            return;
        }

        graphics.fill(x, y, x + width, y + height, 0xF216191E);
        graphics.fill(x, y, x + 2, y + height, 0xFF3BAFDA);
        graphics.fill(x, y + 22, x + width, y + 23, 0xFF30363D);
        graphics.text(font, Component.translatable("cbs.chatAssistant.title"), x + 8, y + 7, 0xFFF0F3F6);

        Optional<CommandSuggestionVisual> visual = suggestor.getSelectedVisual();
        int contentTop = y + 29;
        if (visual.isPresent()) {
            renderPreview(graphics, visual.get(), contentTop);
            contentTop += 55;
        }

        Optional<CommandInsight> insight = resolveInsight();
        List<Component> sourceLines = insight
                .map(CommandInsight::toTooltipLines)
                .orElseGet(() -> List.of(
                        Component.translatable("cbs.chatAssistant.empty.title").withStyle(ChatFormatting.GRAY),
                        Component.translatable("cbs.chatAssistant.empty.body").withStyle(ChatFormatting.DARK_GRAY)
                ));
        String contentKey = insight
                .map(value -> value.title() + '\n' + value.summary() + '\n' + String.join("\n", value.examples()))
                .orElse("empty");
        if (visual.isPresent()) {
            contentKey += '\n' + visual.get().suggestion();
        }
        int wrapWidth = Math.max(50, width - 18);
        if (!contentKey.equals(cachedContentKey) || cachedWrapWidth != wrapWidth) {
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            for (Component sourceLine : sourceLines) {
                wrapped.addAll(font.split(sourceLine, wrapWidth));
            }
            cachedContentKey = contentKey;
            cachedWrapWidth = wrapWidth;
            cachedLines = List.copyOf(wrapped);
            scrollOffset = 0;
        }

        int contentBottom = y + height - 19;
        int lineHeight = font.lineHeight + 3;
        int visibleHeight = Math.max(lineHeight, contentBottom - contentTop);
        maxScroll = Math.max(0, cachedLines.size() * lineHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        graphics.enableScissor(x + 3, contentTop, x + width - 3, contentBottom);
        for (int index = 0; index < cachedLines.size(); index++) {
            int lineY = contentTop + index * lineHeight - scrollOffset;
            if (lineY + lineHeight >= contentTop && lineY < contentBottom) {
                graphics.text(font, cachedLines.get(index), x + 8, lineY, 0xFFE6EDF3);
            }
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int thumbHeight = Math.max(10, visibleHeight * visibleHeight / (visibleHeight + maxScroll));
            int thumbTravel = visibleHeight - thumbHeight;
            int thumbY = contentTop + scrollOffset * thumbTravel / maxScroll;
            graphics.fill(x + width - 4, contentTop, x + width - 2, contentBottom, 0xFF252A30);
            graphics.fill(x + width - 4, thumbY, x + width - 2, thumbY + thumbHeight, 0xFF65717E);
        }

        graphics.fill(x, y + height - 16, x + width, y + height, 0xFF111419);
        String shortcut = font.plainSubstrByWidth(
                Component.translatable("cbs.chatAssistant.shortcut").getString(),
                width - 16
        );
        graphics.text(font, shortcut, x + 8, y + height - 12, 0xFF8A949E);
    }

    private void renderPreview(GuiGraphicsExtractor graphics, CommandSuggestionVisual visual, int previewY) {
        int previewSize = Math.min(38, Math.max(28, width / 5));
        int previewX = x + 8;
        graphics.fill(x + 5, previewY, x + width - 5, previewY + 49, 0xFF101419);
        graphics.outline(previewX - 1, previewY + 3, previewSize + 2, previewSize + 2, 0xFF343A42);
        visual.renderIcon(graphics, previewX, previewY + 4, previewSize);

        int labelX = previewX + previewSize + 8;
        int labelWidth = Math.max(30, x + width - labelX - 8);
        if (visual.player() != null || visual.skin() != null) {
            renderPlayerDetails(graphics, visual, labelX, previewY, labelWidth);
        } else if (visual.particleId() != null) {
            graphics.text(
                    font,
                    font.plainSubstrByWidth(ParticleDisplayNames.get(visual.particleId()), labelWidth),
                    labelX,
                    previewY + 6,
                    0xFF56D4DD
            );
            graphics.text(
                    font,
                    font.plainSubstrByWidth(visual.particleId().toString(), labelWidth),
                    labelX,
                    previewY + 20,
                    0xFFDDE2E7
            );
            graphics.text(
                    font,
                    Component.translatable("cbs.particle.preview"),
                    labelX,
                    previewY + 34,
                    0xFF8A949E
            );
        } else if (!visual.item().isEmpty()) {
            graphics.text(
                    font,
                    font.plainSubstrByWidth(visual.item().getHoverName().getString(), labelWidth),
                    labelX,
                    previewY + 9,
                    0xFFF0F3F6
            );
            graphics.text(
                    font,
                    font.plainSubstrByWidth(visual.suggestion(), labelWidth),
                    labelX,
                    previewY + 25,
                    0xFF8A949E
            );
        }
    }

    private void renderPlayerDetails(
            GuiGraphicsExtractor graphics,
            CommandSuggestionVisual visual,
            int labelX,
            int previewY,
            int labelWidth
    ) {
        graphics.text(
                font,
                font.plainSubstrByWidth(visual.suggestion(), labelWidth),
                labelX,
                previewY + 4,
                0xFFF0F3F6
        );
        PlayerInfo player = visual.player();
        if (player == null) {
            graphics.text(
                    font,
                    Component.translatable("cbs.chatAssistant.player.offline"),
                    labelX,
                    previewY + 19,
                    0xFF8A949E
            );
            return;
        }
        graphics.text(
                font,
                Component.translatable("cbs.chatAssistant.player.online", player.getLatency()),
                labelX,
                previewY + 18,
                0xFF57D17B
        );
        graphics.text(
                font,
                font.plainSubstrByWidth(player.getGameMode().getLongDisplayName().getString(), labelWidth),
                labelX,
                previewY + 32,
                0xFF8A949E
        );
    }

    private Optional<CommandInsight> resolveInsight() {
        Optional<CommandInsight> selected = suggestor.getSelectedInsight();
        if (selected.isPresent()) {
            return selected;
        }
        String command = input.getValue();
        int cursor = input.getCursorPosition();
        if (command.length() <= 1) {
            return Optional.empty();
        }

        Optional<CommandSyntaxHint> syntaxHint = suggestor.getInsightService().findSyntaxHint(command, cursor);
        if (syntaxHint.isPresent()) {
            return Optional.of(new CommandInsight(
                    Component.translatable("cbs.status.incomplete").getString(),
                    syntaxHint.get().summary(),
                    syntaxHint.get().suggestions().stream().limit(8).toList(),
                    true
            ));
        }
        Optional<CommandDiagnostic> diagnostic = suggestor.getInsightService().getDiagnostic(command);
        if (diagnostic.isPresent() && cursor >= diagnostic.get().start()) {
            return Optional.of(new CommandInsight(
                    Component.translatable("cbs.status.error").getString(),
                    diagnostic.get().message(),
                    List.of(),
                    true
            ));
        }
        return suggestor.getInsightService().describeCursor(command, cursor);
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
