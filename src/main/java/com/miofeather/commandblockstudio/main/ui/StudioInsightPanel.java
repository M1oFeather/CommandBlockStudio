package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.insight.CommandDiagnostic;
import com.miofeather.commandblockstudio.main.insight.CommandInsight;
import com.miofeather.commandblockstudio.main.insight.CommandSyntaxHint;
import com.miofeather.commandblockstudio.main.insight.ParticleDisplayNames;
import com.miofeather.commandblockstudio.main.ui.screen.StudioScaledScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StudioInsightPanel implements Renderable {
    private final Font font;
    private final MultiLineTextFieldWidget editor;
    private final MultiLineCommandSuggestor suggestor;
    private final StudioScaledScreen screen;
    private int x;
    private int y;
    private int width;
    private int height;
    private int scrollOffset;
    private int maxScroll;
    private String contentKey = "";
    private String cachedCommand = "";
    private String cachedSelection = "";
    private String cachedSuggestion = "";
    private int cachedCursor = -1;
    private boolean cachedChinese;
    private Optional<CommandInsight> cachedInsight = Optional.empty();
    private int cachedWrapWidth = -1;
    private List<FormattedCharSequence> cachedWrappedLines = List.of();
    private Optional<CommandDiagnostic> activeDiagnostic = Optional.empty();
    private boolean visible = true;

    public StudioInsightPanel(
            Font font,
            MultiLineTextFieldWidget editor,
            MultiLineCommandSuggestor suggestor,
            StudioScaledScreen screen,
            int x,
            int y,
            int width,
            int height
    ) {
        this.font = font;
        this.editor = editor;
        this.suggestor = suggestor;
        this.screen = screen;
        setBounds(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = Math.max(80, width);
        this.height = Math.max(50, height);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!visible || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        scrollOffset = Mth.clamp(scrollOffset - (int) Math.signum(amount) * 20, 0, maxScroll);
        return true;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || button != 0 || activeDiagnostic.isEmpty()
                || mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        editor.jumpToDiagnostic(activeDiagnostic.get());
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) {
            return;
        }

        graphics.fill(x, y, x + width, y + height, 0xFF16191E);
        graphics.fill(x, y, x + 2, y + height, 0xFF343A42);
        graphics.fill(x, y + 22, x + width, y + 23, 0xFF2A3037);
        graphics.drawString(font, Component.translatable("cbs.panel.docs"), x + 8, y + 7, 0xFFE8EAED);

        Optional<CommandInsight> resolved = resolveCachedInsight();
        Optional<MultiLineCommandSuggestor.ParticlePreview> particlePreview = suggestor.getSelectedParticlePreview();
        List<Component> sourceLines = resolved
                .map(CommandInsight::toTooltipLines)
                .orElseGet(() -> List.of(
                        Component.translatable("cbs.panel.docs.empty.title").withStyle(ChatFormatting.GRAY),
                        Component.translatable("cbs.panel.docs.empty.body").withStyle(ChatFormatting.DARK_GRAY)
                ));

        String nextKey = resolved
                .map(insight -> insight.title() + '\n' + insight.summary() + '\n' + String.join("\n", insight.examples()))
                .orElse("empty");
        if (!nextKey.equals(contentKey)) {
            contentKey = nextKey;
            scrollOffset = 0;
            cachedWrapWidth = -1;
        }

        int textWidth = Math.max(40, width - 18);
        if (cachedWrapWidth != textWidth) {
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            for (Component sourceLine : sourceLines) {
                wrapped.addAll(font.split(sourceLine, textWidth));
            }
            cachedWrappedLines = List.copyOf(wrapped);
            cachedWrapWidth = textWidth;
        }
        List<FormattedCharSequence> wrapped = cachedWrappedLines;

        int contentTop = y + 29;
        if (particlePreview.isPresent()) {
            renderParticlePreview(graphics, particlePreview.get(), contentTop);
            contentTop += 54;
        }
        int contentBottom = y + height - 19;
        int lineHeight = font.lineHeight + 3;
        int visibleHeight = Math.max(lineHeight, contentBottom - contentTop);
        maxScroll = Math.max(0, wrapped.size() * lineHeight - visibleHeight);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        screen.enableStudioScissor(graphics, x + 3, contentTop, x + width - 3, contentBottom);
        for (int i = 0; i < wrapped.size(); i++) {
            int lineY = contentTop + i * lineHeight - scrollOffset;
            if (lineY + lineHeight >= contentTop && lineY < contentBottom) {
                graphics.drawString(font, wrapped.get(i), x + 8, lineY, 0xFFE8EAED);
            }
        }
        graphics.disableScissor();

        if (maxScroll > 0) {
            int trackHeight = Math.max(10, visibleHeight * visibleHeight / (visibleHeight + maxScroll));
            int trackTravel = visibleHeight - trackHeight;
            int thumbY = contentTop + (maxScroll == 0 ? 0 : scrollOffset * trackTravel / maxScroll);
            graphics.fill(x + width - 4, contentTop, x + width - 2, contentBottom, 0xFF252A30);
            graphics.fill(x + width - 4, thumbY, x + width - 2, thumbY + trackHeight, 0xFF65717E);
        }

        graphics.fill(x, y + height - 16, x + width, y + height, 0xFF111419);
        String shortcut = font.plainSubstrByWidth(
                Component.translatable("cbs.panel.docs.shortcut").getString(),
                width - 16
        );
        graphics.drawString(font, shortcut, x + 8, y + height - 12, 0xFF8A949E);
    }

    private void renderParticlePreview(
            GuiGraphics graphics,
            MultiLineCommandSuggestor.ParticlePreview preview,
            int previewY
    ) {
        int previewSize = Math.min(42, Math.max(24, width / 4));
        int spriteX = x + 8;
        int spriteY = previewY + 4;
        graphics.fill(x + 5, previewY, x + width - 5, previewY + 49, 0xFF101419);
        graphics.renderOutline(spriteX - 1, spriteY - 1, previewSize + 2, previewSize + 2, 0xFF343A42);
        if (preview.sprite() != null) {
            graphics.blitSprite(RenderType::guiTextured, preview.sprite(), spriteX, spriteY, previewSize, previewSize);
        } else {
            MultiLineCommandSuggestor.renderFallbackParticle(graphics, preview.id(), spriteX, spriteY, previewSize);
        }
        int labelX = spriteX + previewSize + 8;
        int labelWidth = Math.max(20, x + width - 8 - labelX);
        graphics.drawString(
                font,
                font.plainSubstrByWidth(ParticleDisplayNames.get(preview.id()), labelWidth),
                labelX,
                previewY + 8,
                0xFF56B6C2
        );
        graphics.drawString(
                font,
                font.plainSubstrByWidth(preview.id().toString(), labelWidth),
                labelX,
                previewY + 22,
                0xFFDDE2E7
        );
    }

    private Optional<CommandInsight> resolveInsight() {
        activeDiagnostic = Optional.empty();
        Optional<CommandInsight> selectedSuggestion = suggestor.getSelectedInsight();
        if (selectedSuggestion.isPresent()) {
            return selectedSuggestion;
        }

        String command = editor.getValue();
        int cursor = editor.getCursorPosition();
        if (command.isBlank()) {
            return Optional.empty();
        }

        Optional<CommandInsight> placeholder = suggestor.getInsightService().describePlaceholder(editor.getHighlighted());
        if (placeholder.isPresent()) {
            return placeholder;
        }

        Optional<CommandSyntaxHint> syntaxHint = suggestor.getInsightService().findSyntaxHint(command, cursor);
        if (syntaxHint.isPresent()) {
            String title = CommandBlockStudio.useChineseCommandInsight() ? "参数未完成" : "Incomplete argument";
            return Optional.of(new CommandInsight(
                    title,
                    syntaxHint.get().summary(),
                    syntaxHint.get().suggestions().stream().limit(8).toList(),
                    true
            ));
        }

        Optional<CommandDiagnostic> diagnostic = suggestor.getInsightService().getDiagnostic(command);
        if (diagnostic.isPresent() && cursor >= diagnostic.get().start()) {
            activeDiagnostic = diagnostic;
            String title = CommandBlockStudio.useChineseCommandInsight() ? "语法错误" : "Syntax error";
            String snippet = editor.getDiagnosticSnippet(diagnostic.get());
            List<String> examples = snippet.isBlank()
                    ? List.of(Component.translatable("cbs.diagnostic.jump").getString())
                    : List.of(snippet, Component.translatable("cbs.diagnostic.jump").getString());
            return Optional.of(new CommandInsight(
                    title + " · " + editor.getDiagnosticLocation(diagnostic.get()).getString(),
                    diagnostic.get().message(),
                    examples,
                    true
            ));
        }
        return suggestor.getInsightService().describeCursor(command, cursor);
    }

    private Optional<CommandInsight> resolveCachedInsight() {
        String command = editor.getValue();
        String selection = editor.getHighlighted();
        String suggestion = suggestor.getSelectedSuggestionKey();
        int cursor = editor.getCursorPosition();
        boolean chinese = CommandBlockStudio.useChineseCommandInsight();
        if (cachedCursor != cursor || cachedChinese != chinese
                || !cachedCommand.equals(command)
                || !cachedSelection.equals(selection)
                || !cachedSuggestion.equals(suggestion)) {
            cachedCommand = command;
            cachedSelection = selection;
            cachedSuggestion = suggestion;
            cachedCursor = cursor;
            cachedChinese = chinese;
            cachedInsight = resolveInsight();
        }
        return cachedInsight;
    }
}
