package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.ui.screen.AbstractCommandBlockStudioScreen;
import com.miofeather.commandblockstudio.main.ui.screen.CommandBlockStudioScreen;
import com.miofeather.commandblockstudio.main.ui.screen.StudioScaledScreen;
import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.insight.CommandDiagnostic;
import com.miofeather.commandblockstudio.main.insight.CommandInsight;
import com.miofeather.commandblockstudio.main.insight.CommandSyntaxHint;
import com.miofeather.commandblockstudio.main.util.Pair;
import com.miofeather.commandblockstudio.mixin.EditBoxAccessor;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.components.EditBox;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
//import org.eclipse.tm4e.core.grammar.IToken;
//import org.eclipse.tm4e.core.grammar.ITokenizeLineResult;

import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MultiLineTextFieldWidget extends EditBox implements GuiEventListener {
    private static final int MAX_HISTORY_SIZE = 100;
    private static final long HISTORY_MERGE_WINDOW_MILLIS = 750L;
    private static final int SCROLLBAR_SIZE = 10;
    private static final int COMPACT_SCROLLBAR_RESERVE = 3;
    private static final int STATUS_BAR_HEIGHT = 17;
    private static final int TEXT_TOP_PADDING = 5;
    private static final Pattern NUMBER_TOKEN = Pattern.compile(
            "(?<![A-Za-z0-9_.])[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][-+]?\\d+)?"
    );
    private StudioScaledScreen screen;
    private ScrollbarWidget scrollX, scrollY;
    private List<String> lines;
    private List<Integer> lineOffsets, textOffsets;
    private List<Pair<Style, Integer>> textColors;
    private int visibleLines = 11;
    private int scrolledLines = 0;
    private int horizontalOffset = 0;
    private Pair<Integer, Integer> cursorPosPreference;
    private boolean LShiftPressed, RShiftPressed = false;
    private boolean hasCommandSuggestor = false;
    private boolean textModified = false;
    private MultiLineCommandSuggestor suggestor;
    private EditBoxAccessor accessor = (EditBoxAccessor)this;
    private float timeSinceClick = 0.0f;
    private final Deque<HistoryEntry> undoHistory = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoHistory = new ArrayDeque<>();
    private boolean applyingHistory = false;
    private EditKind lastEditKind = EditKind.OTHER;
    private long lastEditAt = 0L;
    private int lastEditCursor = -1;
    private int lastEditHighlight = -1;
    private String bracketIndexText = "";
    private int[] bracketPartners = new int[0];
    private CommandDiagnostic visibleStatusDiagnostic;
    private int statusBarX1;
    private int statusBarY1;
    private int statusBarX2;
    private int statusBarY2;
    private int clickedNumberStart = -1;
    private int clickedNumberEnd = -1;

    public MultiLineTextFieldWidget(Font textRenderer, int x, int y, int width, int height, Component text, StudioScaledScreen screen) {
        super(textRenderer, x, y, width, height, text);
        this.lines = new LinkedList<String>();
        this.lineOffsets = new LinkedList<Integer>();
        this.textOffsets = new LinkedList<Integer>();
        this.visibleLines = calculateVisibleLines(height);
        this.scrolledLines = 0;
        this.screen = screen;
        this.scrollX = new TextFieldScrollbarWidget(x + 1, y + height - SCROLLBAR_SIZE, Math.max(1, width - 2), SCROLLBAR_SIZE, Component.empty(), this, true);
        this.scrollY = new TextFieldScrollbarWidget(x + width - SCROLLBAR_SIZE, y + 1, SCROLLBAR_SIZE, Math.max(1, height - 2), Component.empty(), this, false);
        cursorPosPreference = new Pair<>(0,0);
        updateScrollbarMetrics();
    }

    @Override
    public void setWidth(int width){
        this.width = width;
        scrollX.setWidth(Math.max(1, width - 2));
        scrollY.setX(this.getX() + width - SCROLLBAR_SIZE);
        updateScrollbarMetrics();
    }

    @Override
    public void setHeight(int height){
        this.height = height;
        this.visibleLines = calculateVisibleLines(height);
        scrollY.setHeight(Math.max(1, height - 2));
        scrollX.setY(this.getY() + height - SCROLLBAR_SIZE);
        updateScrollbarMetrics();
    }

    @Override
    public void setX(int x){
        super.setX(x);
        scrollX.setX(x + 1);
        scrollY.setX(x + width - SCROLLBAR_SIZE);
    }

    @Override
    public void setY(int y){
        super.setY(y);
        scrollX.setY(y + height - SCROLLBAR_SIZE);
        scrollY.setY(y + 1);
    }

    @Override
    public void extractWidgetRenderState(final GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta){
        timeSinceClick += delta/20.0f;
        visibleStatusDiagnostic = null;
        int color;
        if (!this.isVisible()) {
            return;
        }
        if (accessor.getBordered()) {
            color = this.isFocused() ? -1 : -6250336;
            graphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, color);
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
        }

        color = accessor.getIsEditable() ? accessor.getTextColor() : accessor.getTextColorUneditable();
        if (screen == null) {
            graphics.enableScissor(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height);
        } else {
            screen.enableStudioScissor(
                    graphics,
                    this.getX(),
                    this.getY(),
                    this.getX() + this.width,
                    this.getY() + this.height
            );
        }

        if(hasCommandSuggestor) {
            if(lines.isEmpty()){
                graphics.disableScissor();
                renderSuggestor(graphics, mouseX, mouseY);
                return;
            }

            renderLineGutter(graphics);

            for (int i = scrolledLines; i < scrolledLines + visibleLines && i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.length() >= horizontalOffset) {
                    line = line.substring(horizontalOffset);
                    line = accessor.getFont().plainSubstrByWidth(line, getTextViewportWidth());

                    this.drawColoredLine(graphics, line, getTextLeft(), this.getY() + 10 * (i - scrolledLines) + 5, i);
                }
            }
        } else {
            this.drawRawText(graphics, accessor.getValue(), this.getX() + 5, this.getY() + 5, color);
        }

        scrollX.extractRenderState(graphics, mouseX, mouseY, delta);
        scrollY.extractRenderState(graphics, mouseX, mouseY, delta);

        if(!hasCommandSuggestor) {
            graphics.disableScissor();
            return;
        }

        int selectionStart = accessor.getCursorPos();
        int selectionEnd = accessor.getHighlightPos();

        boolean selectingBackwards = false;
        if(selectionStart > selectionEnd){
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
            selectingBackwards = true;
        }

        Pair<Integer, Integer> start = indexToLineAndOffset(selectionStart);
        Pair<Integer, Integer> end = indexToLineAndOffset(selectionEnd);

        int firstSelectedLine = start.getA();
        int selectionStartOffset = start.getB();
        int lastSelectedLine = end.getA();
        int selectionEndOffset = end.getB();

        selectionStartOffset -= horizontalOffset;
        selectionEndOffset -= horizontalOffset;

        selectionStartOffset = Math.max(selectionStartOffset,0);
        selectionEndOffset = Math.max(selectionEndOffset,0);

        boolean renderVerticalCursor = selectionStart < accessor.getValue().length();
        boolean verticalCursorVisible = this.isFocused() && (Util.getMillis() - accessor.getFocusedTime()) / 300L % 2L == 0L;

        for (int i = firstSelectedLine; i <= lastSelectedLine; i++) {
            if(i < scrolledLines || i >= scrolledLines+visibleLines) continue;
            int x1 = getTextLeft();
            int x2 = x1;
            int y = this.getY() + 10 * (i - scrolledLines) + 5;

            String visibleLine = lines.get(i).substring(Math.min(horizontalOffset,lines.get(i).length()));
            if (i == firstSelectedLine) {
                x1 += accessor.getFont().width(visibleLine.substring(0, Math.min(selectionStartOffset,visibleLine.length())));

                if (verticalCursorVisible && !selectingBackwards) {
                    if (renderVerticalCursor) {
                        graphics.fill(x1, y - 1, x1 + 1, y + 1 + accessor.getFont().lineHeight, -3092272);
                    } else {
                        graphics.text(accessor.getFont(), "_", x1, y, -3092272);
                    }
                }
            }

            if (i == lastSelectedLine) {
                x2 += accessor.getFont().width(visibleLine.substring(0, Math.min(selectionEndOffset,visibleLine.length())));

                if (verticalCursorVisible && renderVerticalCursor && selectingBackwards) {
                    graphics.fill(x2, y - 1, x2 + 1, y + 1 + accessor.getFont().lineHeight, -3092272);
                }

            } else {
                x2 += getTextViewportWidth();
            }
            graphics.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, x1, y, x2, y + 10, -16776961);
        }

        renderMatchingBrackets(graphics);
        renderInlineSuggestion(graphics);
        graphics.disableScissor();
        renderCursorInsight(graphics);
        renderSuggestor(graphics, mouseX, mouseY);
        renderHoverInsight(graphics, mouseX, mouseY);
    }

    private void renderLineGutter(GuiGraphicsExtractor graphics) {
        int gutterRight = getTextLeft() - 3;
        graphics.fill(getX() + 1, getY() + 1, gutterRight, getTextViewportBottom(), 0xFF11151A);
        graphics.fill(gutterRight, getY() + 1, gutterRight + 1, getTextViewportBottom(), 0xFF30363D);

        int cursorLine = lines.isEmpty()
                ? -1
                : indexToLineAndOffset(clamp(accessor.getCursorPos(), 0, accessor.getValue().length())).getA();
        int errorLine = -1;
        if (suggestor != null && !accessor.getValue().isBlank()) {
            errorLine = suggestor.getInsightService().getDiagnostic(accessor.getValue())
                    .map(diagnostic -> indexToLineAndOffset(
                            clamp(diagnostic.start(), 0, accessor.getValue().length())
                    ).getA())
                    .orElse(-1);
        }

        for (int i = scrolledLines; i < scrolledLines + visibleLines && i < lines.size(); i++) {
            int lineY = getY() + 10 * (i - scrolledLines) + 4;
            boolean error = i == errorLine;
            boolean selected = i == cursorLine;
            if (error || selected) {
                int background = error ? 0xFF32171B : 0xFF2B2917;
                graphics.fill(getX() + 1, lineY, getX() + getWidth() - 1, lineY + 10, background);
                graphics.fill(getX() + 1, lineY, getX() + 3, lineY + 10,
                        error ? 0xFFE05252 : 0xFFE3B341);
            }

            String number = Integer.toString(i + 1);
            int numberColor = error ? 0xFFFF7B72 : selected ? 0xFFFFD866 : 0xFF68727D;
            graphics.text(
                    accessor.getFont(),
                    number,
                    gutterRight - accessor.getFont().width(number) - 3,
                    lineY + 1,
                    numberColor
            );
        }
    }

    private int getLineNumberGutterWidth() {
        if (!hasCommandSuggestor) {
            return 0;
        }
        int digits = Integer.toString(Math.max(1, lines.size())).length();
        return Math.max(18, accessor.getFont().width("9".repeat(digits)) + 9);
    }

    private int getTextLeft() {
        return getX() + 5 + getLineNumberGutterWidth();
    }

    private int getTextViewportWidth() {
        if (!hasCommandSuggestor) {
            return getInnerWidth();
        }
        return Math.max(1, getWidth() - (getTextLeft() - getX()) - 6 - COMPACT_SCROLLBAR_RESERVE);
    }

    private static int calculateVisibleLines(int height) {
        return Math.max(1, (height - TEXT_TOP_PADDING - SCROLLBAR_SIZE - STATUS_BAR_HEIGHT) / 10);
    }

    private int getTextViewportBottom() {
        return getY() + getHeight() - SCROLLBAR_SIZE - STATUS_BAR_HEIGHT;
    }

    int getEditorOverlayTop() {
        return getY() + 4;
    }

    int getEditorOverlayBottom() {
        return Math.max(getEditorOverlayTop() + 1, getTextViewportBottom() - 3);
    }

    private void renderInlineSuggestion(GuiGraphicsExtractor graphics) {
        String completion = accessor.getSuggestion();
        boolean syntaxHint = false;
        if ((completion == null || completion.isEmpty()) && hasCommandSuggestor && suggestor != null) {
            completion = suggestor.getInsightService()
                    .findSyntaxHint(accessor.getValue(), accessor.getCursorPos())
                    .map(CommandSyntaxHint::ghostText)
                    .orElse("");
            syntaxHint = !completion.isEmpty();
        }
        if (completion == null || completion.isEmpty() || accessor.getCursorPos() != accessor.getHighlightPos()) {
            return;
        }

        Pair<Integer, Integer> cursor = indexToLineAndOffset(accessor.getCursorPos());
        int lineIndex = cursor.getA();
        if (lineIndex < scrolledLines || lineIndex >= scrolledLines + visibleLines || lineIndex >= lines.size()) {
            return;
        }

        String line = lines.get(lineIndex);
        int visibleOffset = Math.max(cursor.getB() - horizontalOffset, 0);
        String visibleLine = line.substring(Math.min(horizontalOffset, line.length()));
        int x = getTextLeft() + accessor.getFont().width(visibleLine.substring(0, Math.min(visibleOffset, visibleLine.length())));
        int y = this.getY() + 10 * (lineIndex - scrolledLines) + 5;
        int availableWidth = this.getX() + this.getWidth() - 5 - x;
        if (availableWidth <= 0) {
            return;
        }

        String visibleCompletion = accessor.getFont().plainSubstrByWidth(completion, availableWidth);
        graphics.text(accessor.getFont(), visibleCompletion, x, y, syntaxHint ? 0xFF68737D : 0xFF777777);
    }

    private void renderMatchingBrackets(GuiGraphicsExtractor graphics) {
        if (!this.isFocused() || accessor.getCursorPos() != accessor.getHighlightPos() || bracketPartners.length == 0) {
            return;
        }

        int cursor = accessor.getCursorPos();
        int bracketIndex = cursor > 0 && isStructuralBracket(cursor - 1)
                ? cursor - 1
                : cursor < bracketPartners.length && isStructuralBracket(cursor) ? cursor : -1;
        if (bracketIndex < 0) {
            return;
        }

        int partner = bracketPartners[bracketIndex];
        int color = partner >= 0 ? 0xB03BAFDA : 0xC0E05252;
        renderBracketMarker(graphics, bracketIndex, color);
        if (partner >= 0) {
            renderBracketMarker(graphics, partner, color);
        }
    }

    private boolean isStructuralBracket(int index) {
        return index >= 0 && index < bracketPartners.length && bracketPartners[index] != -2;
    }

    private void renderBracketMarker(GuiGraphicsExtractor graphics, int index, int color) {
        Pair<Integer, Integer> location = indexToLineAndOffset(index);
        int lineIndex = location.getA();
        if (lineIndex < scrolledLines || lineIndex >= scrolledLines + visibleLines || lineIndex >= lines.size()) {
            return;
        }

        String line = lines.get(lineIndex);
        int visualOffset = location.getB();
        int visibleStart = Math.min(horizontalOffset, line.length());
        if (visualOffset < visibleStart || visualOffset >= line.length()) {
            return;
        }

        int x = getTextLeft() + accessor.getFont().width(line.substring(visibleStart, visualOffset));
        int y = this.getY() + 10 * (lineIndex - scrolledLines) + 5;
        int characterWidth = Math.max(2, accessor.getFont().width(String.valueOf(line.charAt(visualOffset))));
        graphics.fill(x, y, x + characterWidth, y + accessor.getFont().lineHeight, (color & 0x00FFFFFF) | 0x60000000);
        graphics.fill(x, y + accessor.getFont().lineHeight - 1, x + characterWidth, y + accessor.getFont().lineHeight, color);
    }

    private void renderSuggestor(GuiGraphicsExtractor graphics, int mouseX, int mouseY){
        if(suggestor.getY() > getY() + getHeight() || suggestor.getY() < getY()) return;
        if(suggestor.getX() > getX() + getWidth() || suggestor.getX() < getX()) return;
        suggestor.extractRenderState(graphics, mouseX, mouseY);
    }

    private void renderCursorInsight(GuiGraphicsExtractor graphics) {
        if (!hasCommandSuggestor || suggestor == null || accessor.getValue().isBlank() || usesDockedInsightPanel()) {
            return;
        }

        Optional<CommandInsight> placeholder = suggestor.getInsightService().describePlaceholder(this.getHighlighted());
        if (placeholder.isPresent()) {
            renderStatusBar(graphics, placeholder.get().title() + ": " + placeholder.get().summary(), false);
            return;
        }

        Optional<CommandSyntaxHint> syntaxHint = suggestor.getInsightService()
                .findSyntaxHint(accessor.getValue(), accessor.getCursorPos());
        if (syntaxHint.isPresent()) {
            String title = CommandBlockStudio.useChineseCommandInsight() ? "参数未完成" : "Incomplete argument";
            renderStatusBar(graphics, title + ": " + syntaxHint.get().summary(), false);
            return;
        }

        Optional<CommandDiagnostic> diagnostic = suggestor.getInsightService().getDiagnostic(accessor.getValue());
        if (diagnostic.isPresent() && accessor.getCursorPos() >= diagnostic.get().start()) {
            String title = CommandBlockStudio.useChineseCommandInsight() ? "语法错误" : "Syntax error";
            String text = title + " · " + getDiagnosticLocation(diagnostic.get()).getString()
                    + ": " + diagnostic.get().message();
            renderStatusBar(graphics, text, true, diagnostic.get());
            return;
        }

        Optional<CommandInsight> insight = suggestor.getInsightService().describeCursor(accessor.getValue(), accessor.getCursorPos());
        if (insight.isEmpty()) {
            return;
        }

        String text = insight.get().title() + ": " + insight.get().summary();
        if (suggestor.getInsightService().findTemplate(accessor.getValue()).isPresent()) {
            text += CommandBlockStudio.useChineseCommandInsight()
                    ? "  Ctrl+Shift+Space: 插入模板"
                    : "  Ctrl+Shift+Space: insert template";
        }
        renderStatusBar(graphics, text, false);
    }

    private void renderStatusBar(GuiGraphicsExtractor graphics, String text, boolean error) {
        renderStatusBar(graphics, text, error, null);
    }

    private void renderStatusBar(GuiGraphicsExtractor graphics, String text, boolean error, CommandDiagnostic diagnostic) {
        Font textRenderer = accessor.getFont();
        int x1 = this.getX() + 1;
        int x2 = this.getX() + this.getWidth() - 1;
        int y2 = this.getY() + this.getHeight() - SCROLLBAR_SIZE - 1;
        if (x2 <= x1 + 20) {
            return;
        }

        int lineHeight = textRenderer.lineHeight + 1;
        int jumpWidth = diagnostic == null ? 0 : 20;
        String visibleText = textRenderer.plainSubstrByWidth(text, x2 - x1 - 8 - jumpWidth);
        int y1 = y2 - (lineHeight + 6);
        if (y1 <= this.getY() + 4) {
            return;
        }

        if (diagnostic != null) {
            visibleStatusDiagnostic = diagnostic;
            statusBarX1 = x2 - 18;
            statusBarY1 = y1;
            statusBarX2 = x2;
            statusBarY2 = y2;
        }

        graphics.fill(x1, y1, x2, y2, error ? 0xFF251216 : 0xFF10141A);
        graphics.outline(x1, y1, x2 - x1, y2 - y1, error ? 0xFFE05252 : 0xFF3BAFDA);
        int color = error ? 0xFFFFD7D7 : 0xFFE6F3FF;
        graphics.text(textRenderer, visibleText, x1 + 4, y1 + 3, color);
        if (diagnostic != null) {
            graphics.fill(statusBarX1, statusBarY1, statusBarX2, statusBarY2, 0xFF3A1A20);
            graphics.outline(statusBarX1, statusBarY1, statusBarX2 - statusBarX1, statusBarY2 - statusBarY1, 0xFFE05252);
            graphics.centeredText(textRenderer, "↪", (statusBarX1 + statusBarX2) / 2, statusBarY1 + 3, 0xFFFFE2E2);
        }
    }

    private void renderHoverInsight(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!hasCommandSuggestor || suggestor == null || accessor.getValue().isBlank()
                || showCommandSuggestions() || usesDockedInsightPanel()) {
            return;
        }
        if (suggestor.getInsightService().describePlaceholder(this.getHighlighted()).isPresent()) {
            return;
        }
        OptionalInt hoveredIndex = getHoveredTextIndex(mouseX, mouseY);
        if (hoveredIndex.isEmpty()) {
            return;
        }

        int index = hoveredIndex.getAsInt();
        Optional<CommandDiagnostic> diagnostic = suggestor.getInsightService().getDiagnostic(accessor.getValue());
        if (diagnostic.isPresent() && diagnostic.get().contains(index)) {
            String title = CommandBlockStudio.useChineseCommandInsight() ? "语法错误" : "Syntax error";
            renderEditorTooltip(graphics, List.of(
                    Component.literal(title).withStyle(ChatFormatting.RED),
                    getDiagnosticLocation(diagnostic.get()).copy().withStyle(ChatFormatting.YELLOW),
                    Component.literal(diagnostic.get().message())
            ), mouseX, mouseY);
            return;
        }

        Optional<CommandInsight> insight = suggestor.getInsightService().describeAt(accessor.getValue(), index);
        if (insight.isEmpty()) {
            return;
        }

        renderEditorTooltip(graphics, insight.get().toTooltipLines(), mouseX, mouseY);
    }

    void renderEditorTooltip(GuiGraphicsExtractor graphics, List<Component> components, int mouseX, int mouseY) {
        TooltipAnchor anchor = getTooltipAnchor(mouseX, mouseY);
        renderEditorTooltip(graphics, components, anchor.x(), anchor.belowLineY(), mouseX, mouseY);
    }

    void renderEditorTooltip(
            GuiGraphicsExtractor graphics,
            List<Component> components,
            int preferredX,
            int preferredY,
            int mouseX,
            int mouseY
    ) {
        TooltipAnchor caretAnchor = getTooltipAnchor(mouseX, mouseY);
        TooltipAnchor anchor = new TooltipAnchor(
                Math.max(caretAnchor.x(), preferredX),
                Math.max(caretAnchor.belowLineY(), preferredY),
                caretAnchor.lineY()
        );
        int overlayLeft = getX() + 4;
        int overlayRight = getX() + getWidth() - 4;
        int wrapWidth = Math.max(72, Math.min(220, overlayRight - overlayLeft - 8));
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component component : components) {
            lines.addAll(accessor.getFont().split(component, wrapWidth));
        }

        ClientTooltipPositioner positioner = (screenWidth, screenHeight, ignoredMouseX, ignoredMouseY, tooltipWidth, tooltipHeight) -> {
            int x = Mth.clamp(anchor.x(), overlayLeft, Math.max(overlayLeft, overlayRight - tooltipWidth));
            int y = EditorOverlayPlacement.placeVertical(
                    anchor.belowLineY(),
                    anchor.lineY() - 6,
                    tooltipHeight,
                    getEditorOverlayTop(),
                    getEditorOverlayBottom()
            );
            return new Vector2i(x, y);
        };
        graphics.setTooltipForNextFrame(accessor.getFont(), lines, mouseX, mouseY);
    }

    private TooltipAnchor getTooltipAnchor(int mouseX, int mouseY) {
        Pair<Integer, Integer> cursor = indexToLineAndOffset(accessor.getCursorPos());
        int lineIndex = cursor.getA();
        if (lineIndex >= scrolledLines && lineIndex < scrolledLines + visibleLines && lineIndex < lines.size()) {
            String line = lines.get(lineIndex);
            int visibleOffset = Math.max(cursor.getB() - horizontalOffset, 0);
            String visibleLine = line.substring(Math.min(horizontalOffset, line.length()));
            int cursorX = getTextLeft()
                    + accessor.getFont().width(visibleLine.substring(0, Math.min(visibleOffset, visibleLine.length())));
            int lineY = this.getY() + 10 * (lineIndex - scrolledLines) + 5;
            return new TooltipAnchor(Math.max(mouseX + 12, cursorX + 8), lineY + accessor.getFont().lineHeight + 4, lineY);
        }
        return new TooltipAnchor(mouseX + 12, mouseY + accessor.getFont().lineHeight + 4, mouseY);
    }

    private boolean showCommandSuggestions() {
        return suggestor != null && suggestor.isVisible();
    }

    public boolean usesDockedInsightPanel() {
        return screen instanceof AbstractCommandBlockStudioScreen studioScreen
                && studioScreen.isQuickDocsVisible();
    }

    private record TooltipAnchor(int x, int belowLineY, int lineY) {
    }

    private void drawColoredLine(GuiGraphicsExtractor graphics, String content, int x, int y, int lineIndex){
        if (content.isEmpty()) {
            return;
        }

        Font textRenderer = accessor.getFont();
        int rawLineStart = textOffsets.get(lineIndex);
        int indentation = lineOffsets.get(lineIndex);
        int renderOffset = 0;
        int runStart = 0;
        int styleIndex = 0;
        int activeColor = TextColor.fromLegacyFormat(ChatFormatting.GRAY).getValue();
        int rawIndex = rawLineStart + Math.max(0, horizontalOffset - indentation);
        while (styleIndex < textColors.size() && textColors.get(styleIndex).getB() <= rawIndex) {
            TextColor textColor = textColors.get(styleIndex).getA().getColor();
            if (textColor != null) {
                activeColor = textColor.getValue();
            }
            styleIndex++;
        }
        int runColor = activeColor | 0xFF000000;

        for (int i = 1; i <= content.length(); i++) {
            int color = Integer.MIN_VALUE;
            if (i < content.length()) {
                rawIndex = rawLineStart + Math.max(0, horizontalOffset + i - indentation);
                while (styleIndex < textColors.size() && textColors.get(styleIndex).getB() <= rawIndex) {
                    TextColor textColor = textColors.get(styleIndex).getA().getColor();
                    if (textColor != null) {
                        activeColor = textColor.getValue();
                    }
                    styleIndex++;
                }
                color = activeColor | 0xFF000000;
            }
            if (i == content.length() || color != runColor) {
                String run = content.substring(runStart, i);
                graphics.text(textRenderer, run, x + renderOffset, y, runColor);
                renderOffset += textRenderer.width(run);
                runStart = i;
                runColor = color;
            }
        }
    }

    private void drawRawText(GuiGraphicsExtractor graphics, String content, int x, int y, int color){
        Font textRenderer = accessor.getFont();
        String line = content.substring(Math.max(Math.min(horizontalOffset, content.length() - 1),0));
        String trimmedLine = textRenderer.plainSubstrByWidth(line, getTextViewportWidth());
        graphics.text(textRenderer, trimmedLine, x, y, color);
    }

    private int pointToIndex(double x, double y){
        if(lines.size() > 0) {
            int lineIndex = (int) Math.floor((y - (this.getY() + TEXT_TOP_PADDING)) / 10) + scrolledLines;
            lineIndex = Math.max(Math.min(lineIndex, lines.size() - 1), 0);
            String line = lines.get(lineIndex);
            int indentation = lineOffsets.get(lineIndex);
            int rawLineLength = Math.max(0, line.length() - indentation);
            int visibleStart = Math.min(horizontalOffset, line.length());
            int displayOffset = nearestDisplayOffset(line, visibleStart, x - getTextLeft());
            int rawOffset = clamp(displayOffset - indentation, 0, rawLineLength);
            return textOffsets.get(lineIndex) + rawOffset;
        }
        return 0;
    }

    private int nearestDisplayOffset(String line, int visibleStart, double pixelX) {
        if (pixelX <= 0.0D || visibleStart >= line.length()) {
            return visibleStart;
        }

        Font font = accessor.getFont();
        int fullWidth = font.width(line.substring(visibleStart));
        if (pixelX >= fullWidth) {
            return line.length();
        }

        int low = visibleStart + 1;
        int high = line.length();
        while (low < high) {
            int middle = (low + high) >>> 1;
            int width = font.width(line.substring(visibleStart, middle));
            if (width < pixelX) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }

        int nextOffset = low;
        int previousOffset = nextOffset - 1;
        int previousWidth = font.width(line.substring(visibleStart, previousOffset));
        int nextWidth = font.width(line.substring(visibleStart, nextOffset));
        return pixelX < (previousWidth + nextWidth) / 2.0D ? previousOffset : nextOffset;
    }

    private OptionalInt getHoveredTextIndex(double mouseX, double mouseY) {
        if (!getHovered(mouseX, mouseY) || mouseY >= getTextViewportBottom() || lines.isEmpty()) {
            return OptionalInt.empty();
        }

        int relativeY = (int) Math.floor(mouseY - (this.getY() + 5));
        if (relativeY < 0) {
            return OptionalInt.empty();
        }
        int visibleLineIndex = relativeY / 10;
        int lineIndex = visibleLineIndex + scrolledLines;
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return OptionalInt.empty();
        }

        int lineY = this.getY() + 10 * visibleLineIndex + 5;
        if (mouseY < lineY || mouseY >= lineY + accessor.getFont().lineHeight) {
            return OptionalInt.empty();
        }

        String line = lines.get(lineIndex);
        String visibleLine = line.substring(Math.min(horizontalOffset, line.length()));
        visibleLine = accessor.getFont().plainSubstrByWidth(visibleLine, getTextViewportWidth());
        int textX = getTextLeft();
        int renderedWidth = accessor.getFont().width(visibleLine);
        if (visibleLine.isEmpty() || mouseX < textX || mouseX >= textX + renderedWidth) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(pointToIndex(mouseX, mouseY));
    }

    private Pair<Integer, Integer> indexToLineAndOffset(int index){
        Pair<Integer, Integer> output = new Pair<>(0,0);
        for(int i=0; i<lines.size(); i++){
            if(textOffsets.get(i)+(lines.get(i).length()-lineOffsets.get(i))+1>index){
                output.setA(i);
                output.setB((index - textOffsets.get(i)) + lineOffsets.get(i));
                return output;
            }
        }
        return output;
    }

    public Pair<Integer, Integer> getCharacterPos(int index){
        Pair<Integer,Integer> output = indexToLineAndOffset(index);
        if (lines.isEmpty()) {
            return new Pair<>(getTextLeft(), getY() + TEXT_TOP_PADDING);
        }
        String line = lines.get(clamp(output.getA(), 0, lines.size() - 1));
        int visibleStart = clamp(horizontalOffset, 0, line.length());
        int cursorOffset = clamp(output.getB(), visibleStart, line.length());
        int x = getTextLeft() + accessor.getFont().width(line.substring(visibleStart, cursorOffset));
        int y;
        y = this.getY() + 5 + 10 * (output.getA()-scrolledLines);
        return new Pair<>(x,y);
    }

    public void setCommandSuggestor(MultiLineCommandSuggestor newSuggestor){
        this.suggestor = newSuggestor;
        this.hasCommandSuggestor = true;
    }

    @Override
    public void insertText(String text) {
        String string;
        String string2;
        int l;
        int start = Math.min(accessor.getCursorPos(), accessor.getHighlightPos());
        int end = Math.max(accessor.getCursorPos(), accessor.getHighlightPos());
        int k = accessor.invokeGetMaxLength() - accessor.getValue().length() - (start - end);
        if (k < (l = (string = StringUtil.filterText(text)).length())) {
            string = string.substring(0, k);
            l = k;
        }
        /*if (!accessor.getFilter().test(string2 = new StringBuilder(accessor.getValue()).replace(start, end, string).toString())) {
            return;
        }*/
        string2 = new StringBuilder(accessor.getValue()).replace(start, end, string).toString();
        if (string2.equals(accessor.getValue())) {
            return;
        }
        EditKind editKind = string.isEmpty()
                ? EditKind.DELETE
                : string.length() == 1 && start == end ? EditKind.INSERT : EditKind.OTHER;
        beginHistoryEdit(editKind, editKind == EditKind.OTHER);
        accessor.setTextVariable(string2);
        this.setCursorPosition(start + l);
        this.setHighlightPos(accessor.getCursorPos());
        this.onChanged(accessor.getValue(), true);
        this.updateScrollPositions();
        textModified = true;
        finishHistoryEdit();
    }

    private void erase(int offset) {
        if (accessor.getValue().isEmpty()) {
            return;
        }
        if (offset < 0 && eraseMatchingPair()) {
            return;
        }
        boolean directDelete = accessor.getHighlightPos() == accessor.getCursorPos();
        if (directDelete) {
            beginHistoryEdit(EditKind.DELETE, false);
        }
        if (Minecraft.getInstance().hasControlDown()) {
            this.deleteWords(offset);
        } else {
            this.deleteChars(offset);
        }
        this.onChanged(accessor.getValue(), true);
        this.updateScrollPositions();
        textModified = true;
        if (directDelete) {
            finishHistoryEdit();
        }
    }

    private boolean eraseMatchingPair() {
        if (!CommandBlockStudio.BRACKET_AUTOCOMPLETE
                || accessor.getCursorPos() != accessor.getHighlightPos()
                || accessor.getCursorPos() <= 0
                || accessor.getCursorPos() >= accessor.getValue().length()) {
            return false;
        }

        int cursor = accessor.getCursorPos();
        char opening = accessor.getValue().charAt(cursor - 1);
        char closing = accessor.getValue().charAt(cursor);
        if (matchingClose(opening) != closing) {
            return false;
        }

        beginHistoryEdit(EditKind.DELETE, false);
        accessor.setTextVariable(new StringBuilder(accessor.getValue()).delete(cursor - 1, cursor + 1).toString());
        this.moveCursorTo(cursor - 1, false);
        this.onChanged(accessor.getValue(), true);
        this.updateScrollPositions();
        textModified = true;
        finishHistoryEdit();
        return true;
    }

    private boolean isAlphanumeric(char a){
        return Character.isLetter(a) || Character.isDigit(a) || a == '_';
    }

    private int getWordLength(int offsetDir){
        /*
         * If traversing letters or numbers, keep going until reaching a non-alphanumeric character.
         * Otherwise, only traverse chunks of the same character
         */
        int startIndex = accessor.getCursorPos() + (offsetDir < 0 ? -1 : 0);
        String text = getValue();
        if(text.isEmpty() || startIndex < 0 || startIndex >= text.length()) return 0;
        char current = text.charAt(startIndex);
        char startChar = current;
        boolean erasingAlphanumeric = isAlphanumeric(startChar);
        int endIndex = startIndex;
        do{
            endIndex += offsetDir;
            if (endIndex < 0 || endIndex >= text.length()) break;
            current = text.charAt(endIndex);
        } while (
                (erasingAlphanumeric && isAlphanumeric(current))
                        || (!erasingAlphanumeric && current == startChar)
        );
        endIndex -= offsetDir;
        int min = Math.min(startIndex, endIndex);
        int max = Math.max(startIndex, endIndex);
        return ((max-min) + 1) * offsetDir;
    }

    private void selectWord(){
        int forward = getWordLength(1);
        int backward = getWordLength(-1);
        accessor.setCursorPos(accessor.getCursorPos()+forward);
        accessor.setHighlightPos((accessor.getCursorPos()-forward)+backward);
    }

    @Override
    public void deleteWords(int wordOffset) {
        if (accessor.getValue().isEmpty()) {
            return;
        }
        if (accessor.getHighlightPos() != accessor.getCursorPos()) {
            this.insertText("");
            return;
        }
        //this.eraseCharacters(this.getWordSkipPosition(wordOffset) - accessor.getSelectionStart());
        deleteChars(getWordLength(wordOffset));
    }

    @Override
    public void deleteChars(int characterOffset) {
        int k;
        if (accessor.getValue().isEmpty()) {
            return;
        }
        if (accessor.getHighlightPos() != accessor.getCursorPos()) {
            this.insertText("");
            return;
        }
        int i = accessor.invokeGetCursorPos(characterOffset);
        int j = Math.min(i, accessor.getCursorPos());
        if (j == (k = Math.max(i, accessor.getCursorPos()))) {
            return;
        }
        String string = new StringBuilder(accessor.getValue()).delete(j, k).toString();
        /*if (!accessor.getFilter().test(string)) {
            return;
        }*/
        accessor.setTextVariable(string);
        this.moveCursorTo(j, false);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        
        if(keyCode == 340){
            LShiftPressed = true;
        }
        if(keyCode == 344){
            RShiftPressed = true;
        }

        if (!this.canConsumeInput()) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_SPACE && Minecraft.getInstance().hasControlDown() && accessor.getIsEditable()) {
            if (Minecraft.getInstance().hasShiftDown() && insertCommandTemplate()) {
                return true;
            }
            if (hasCommandSuggestor && suggestor != null) {
                suggestor.showSuggestionsAtCursor();
                return true;
            }
        }

        if (accessor.getIsEditable() && Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            return Minecraft.getInstance().hasShiftDown() ? redoEdit() : undoEdit();
        }
        if (accessor.getIsEditable() && Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) {
            return redoEdit();
        }

        if ((Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_A)) {
            this.moveCursorToEnd(Minecraft.getInstance().hasShiftDown());
            this.setHighlightPos(0);
            return true;
        }
        if ((Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_C)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
            return true;
        }
        if ((Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_V)) {
            if (accessor.getIsEditable()) {
                this.insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
            }
            return true;
        }
        if ((Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_X)) {
            Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
            if (accessor.getIsEditable()) {
                this.insertText("");
            }
            return true;
        }
        switch (keyCode) {
            case 263: {
                if (Minecraft.getInstance().hasControlDown()) {
                    //this.setCursor(this.getWordSkipPosition(-1), Minecraft.getInstance().hasShiftDown());
                    this.moveCursorTo(getCursorPosition() + getWordLength(-1), Minecraft.getInstance().hasShiftDown());
                    updateScrollPositions();
                } else {
                    this.moveCursor(-1, Minecraft.getInstance().hasShiftDown());
                }
                return true;
            }
            case 264:{//DOWN
                this.moveCursorVertical(1);
                return true;
            }
            case 265:{//UP
                this.moveCursorVertical(-1);
                return true;
            }
            case 262: {
                if (Minecraft.getInstance().hasControlDown()) {
                    //this.setCursor(this.getWordSkipPosition(1), Minecraft.getInstance().hasShiftDown());
                    this.moveCursorTo(getCursorPosition() + getWordLength(1), Minecraft.getInstance().hasShiftDown());
                    updateScrollPositions();
                } else {
                    this.moveCursor(1, Minecraft.getInstance().hasShiftDown());
                }
                return true;
            }
            case 259: {
                if (accessor.getIsEditable()) {
                    this.erase(-1);
                }
                return true;
            }
            case 261: {
                if (accessor.getIsEditable()) {
                    this.erase(1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_HOME: {
                if (Minecraft.getInstance().hasControlDown()) {
                    this.moveCursorToStart(Minecraft.getInstance().hasShiftDown());
                } else {
                    this.moveCursorToVisualLineBoundary(false, Minecraft.getInstance().hasShiftDown());
                }
                updateScrollPositions();
                return true;
            }
            case GLFW.GLFW_KEY_END: {
                if (Minecraft.getInstance().hasControlDown()) {
                    this.moveCursorToEnd(Minecraft.getInstance().hasShiftDown());
                } else {
                    this.moveCursorToVisualLineBoundary(true, Minecraft.getInstance().hasShiftDown());
                }
                updateScrollPositions();
                return true;
            }
        }
        return false;
    }

    private void moveCursorToVisualLineBoundary(boolean end, boolean selecting) {
        if (lines.isEmpty()) {
            this.moveCursorTo(end ? accessor.getValue().length() : 0, selecting);
            return;
        }

        int lineIndex = indexToLineAndOffset(accessor.getCursorPos()).getA();
        int target = textOffsets.get(lineIndex);
        if (end) {
            target += lines.get(lineIndex).length() - lineOffsets.get(lineIndex);
        }
        this.moveCursorTo(target, selecting);
    }

    private boolean insertCommandTemplate() {
        if (!hasCommandSuggestor || suggestor == null) {
            return false;
        }

        Optional<String> template = suggestor.getInsightService().findTemplate(accessor.getValue());
        if (template.isEmpty()) {
            return false;
        }

        boolean keepSlash = accessor.getValue().trim().startsWith("/");
        String replacement = keepSlash ? "/" + template.get() : template.get();
        this.setValue(replacement);
        selectFirstPlaceholder(replacement);
        updateScrollPositions();
        textModified = true;
        return true;
    }

    private void selectFirstPlaceholder(String text) {
        int start = text.indexOf('<');
        int end = text.indexOf('>', start + 1);
        if (start < 0 || end <= start) {
            this.moveCursorToEnd(false);
            return;
        }
        accessor.setCursorPos(start);
        accessor.setHighlightPos(end + 1);
        refreshSuggestorPos();
    }

    public boolean selectNextPlaceholder(boolean backwards) {
        String text = accessor.getValue();
        if (text.isBlank()) {
            return false;
        }

        int cursor = backwards
                ? Math.min(accessor.getCursorPos(), accessor.getHighlightPos())
                : Math.max(accessor.getCursorPos(), accessor.getHighlightPos());
        Pair<Integer, Integer> range = backwards ? findPreviousPlaceholder(text, cursor) : findNextPlaceholder(text, cursor);
        if (range == null && !backwards) {
            range = findNextPlaceholder(text, 0);
        } else if (range == null) {
            range = findPreviousPlaceholder(text, text.length());
        }
        if (range == null) {
            return false;
        }

        accessor.setCursorPos(range.getA());
        accessor.setHighlightPos(range.getB());
        updateScrollPositions();
        refreshSuggestorPos();
        return true;
    }

    public boolean acceptSyntaxHint() {
        if (!hasCommandSuggestor || suggestor == null || !accessor.getIsEditable()
                || accessor.getCursorPos() != accessor.getHighlightPos()) {
            return false;
        }

        Optional<CommandSyntaxHint> hint = suggestor.getInsightService()
                .findSyntaxHint(accessor.getValue(), accessor.getCursorPos());
        if (hint.isEmpty()) {
            return false;
        }

        int insertionStart = accessor.getCursorPos();
        this.insertText(hint.get().insertionText());
        String text = accessor.getValue();
        int placeholderStart = text.indexOf('<', insertionStart);
        int placeholderEnd = placeholderStart < 0 ? -1 : text.indexOf('>', placeholderStart + 1);
        if (placeholderStart >= 0 && placeholderEnd > placeholderStart) {
            accessor.setCursorPos(placeholderStart);
            accessor.setHighlightPos(placeholderEnd + 1);
        }
        updateScrollPositions();
        refreshSuggestorPos();
        return true;
    }

    private Pair<Integer, Integer> findNextPlaceholder(String text, int cursor) {
        int start = Math.max(0, cursor);
        while (start < text.length()) {
            start = text.indexOf('<', start);
            if (start < 0) {
                return null;
            }
            int end = text.indexOf('>', start + 1);
            if (end < 0) {
                return null;
            }
            if (end > cursor) {
                return new Pair<>(start, end + 1);
            }
            start = end + 1;
        }
        return null;
    }

    private Pair<Integer, Integer> findPreviousPlaceholder(String text, int cursor) {
        int end = Math.min(cursor, text.length());
        while (end > 0) {
            int start = text.lastIndexOf('<', end - 1);
            if (start < 0) {
                return null;
            }
            int close = text.indexOf('>', start + 1);
            if (close >= 0 && close < cursor) {
                return new Pair<>(start, close + 1);
            }
            end = start;
        }
        return null;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        
        if(keyCode == 340){
            LShiftPressed = false;
        }
        if(keyCode == 344){
            RShiftPressed = false;
        }
        return super.keyReleased(new KeyEvent(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.canConsumeInput()) {
            return false;
        }
        if (StringUtil.isAllowedChatCharacter(codePoint)) {
            if (accessor.getIsEditable()) {
                if (CommandBlockStudio.BRACKET_AUTOCOMPLETE && typePairedCharacter(codePoint)) {
                    textModified = true;
                    return true;
                }
                this.insertText(Character.toString(codePoint));
            }
            textModified = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return keyPressed(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return keyReleased(event.key(), event.scancode(), event.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return charTyped((char) event.codepoint(), 0);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClick(event.x(), event.y(), event.button());
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        onRelease(event.x(), event.y());
    }

    private boolean typePairedCharacter(char codePoint) {
        int selectionStart = Math.min(accessor.getCursorPos(), accessor.getHighlightPos());
        int selectionEnd = Math.max(accessor.getCursorPos(), accessor.getHighlightPos());
        boolean selectionEmpty = selectionStart == selectionEnd;

        if (selectionEmpty
                && isClosingCharacter(codePoint)
                && selectionStart < accessor.getValue().length()
                && accessor.getValue().charAt(selectionStart) == codePoint) {
            this.moveCursor(1, false);
            this.updateScrollPositions();
            return true;
        }

        char closing = matchingClose(codePoint);
        if (closing == '\0') {
            return false;
        }

        String selected = accessor.getValue().substring(selectionStart, selectionEnd);
        this.insertText(codePoint + selected + closing);
        if (selectionEmpty) {
            this.moveCursorTo(selectionStart + 1, false);
        } else {
            accessor.setCursorPos(selectionStart + selected.length() + 1);
            accessor.setHighlightPos(selectionStart + 1);
        }
        this.updateScrollPositions();
        this.refreshSuggestorPos();
        return true;
    }

    private static char matchingClose(char opening) {
        return switch (opening) {
            case '{' -> '}';
            case '[' -> ']';
            case '(' -> ')';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> '\0';
        };
    }

    private static boolean isClosingCharacter(char character) {
        return character == '}' || character == ']' || character == ')' || character == '"' || character == '\'';
    }

    private void beginHistoryEdit(EditKind kind, boolean forceCheckpoint) {
        if (applyingHistory) {
            return;
        }

        long now = Util.getMillis();
        HistoryEntry current = captureHistoryEntry();
        boolean canMerge = !forceCheckpoint
                && kind != EditKind.OTHER
                && kind == lastEditKind
                && now - lastEditAt <= HISTORY_MERGE_WINDOW_MILLIS
                && current.cursorPosition() == lastEditCursor
                && current.highlightPosition() == lastEditHighlight;
        if (!canMerge && (undoHistory.isEmpty() || !undoHistory.peekLast().equals(current))) {
            pushHistoryEntry(undoHistory, current);
        }
        redoHistory.clear();
        lastEditKind = kind;
        lastEditAt = now;
    }

    private void finishHistoryEdit() {
        lastEditCursor = accessor.getCursorPos();
        lastEditHighlight = accessor.getHighlightPos();
    }

    private boolean undoEdit() {
        if (undoHistory.isEmpty()) {
            return false;
        }
        pushHistoryEntry(redoHistory, captureHistoryEntry());
        applyHistoryEntry(undoHistory.removeLast());
        resetHistoryMerge();
        return true;
    }

    private boolean redoEdit() {
        if (redoHistory.isEmpty()) {
            return false;
        }
        pushHistoryEntry(undoHistory, captureHistoryEntry());
        applyHistoryEntry(redoHistory.removeLast());
        resetHistoryMerge();
        return true;
    }

    private void applyHistoryEntry(HistoryEntry entry) {
        applyingHistory = true;
        try {
            this.setValue(entry.text());
            accessor.setCursorPos(clamp(entry.cursorPosition(), 0, accessor.getValue().length()));
            accessor.setHighlightPos(clamp(entry.highlightPosition(), 0, accessor.getValue().length()));
            textModified = true;
            this.updateScrollPositions();
            this.refreshSuggestorPos();
        } finally {
            applyingHistory = false;
        }
    }

    private HistoryEntry captureHistoryEntry() {
        return new HistoryEntry(accessor.getValue(), accessor.getCursorPos(), accessor.getHighlightPos());
    }

    private void pushHistoryEntry(Deque<HistoryEntry> history, HistoryEntry entry) {
        if (!history.isEmpty() && history.peekLast().equals(entry)) {
            return;
        }
        history.addLast(entry);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeFirst();
        }
    }

    private void clearHistory() {
        undoHistory.clear();
        redoHistory.clear();
        resetHistoryMerge();
    }

    private void resetHistoryMerge() {
        lastEditKind = EditKind.OTHER;
        lastEditAt = 0L;
        lastEditCursor = -1;
        lastEditHighlight = -1;
    }

    private enum EditKind {
        INSERT,
        DELETE,
        OTHER
    }

    private record HistoryEntry(String text, int cursorPosition, int highlightPosition) {
    }

    private record BracketEntry(char character, int index) {
    }

    @Override
    public void setValue(String text) {
        /*if (!accessor.getFilter().test(text)) {
            return;
        }*/
        String value = text.length() > accessor.invokeGetMaxLength() ? text.substring(0, accessor.invokeGetMaxLength()) : text;
        if (value.equals(accessor.getValue())) {
            this.onChanged(value, true);
            return;
        }
        if (!applyingHistory) {
            beginHistoryEdit(EditKind.OTHER, true);
        }
        accessor.setTextVariable(value);
        this.moveCursorToEnd(Minecraft.getInstance().hasShiftDown());
        this.setHighlightPos(accessor.getCursorPos());
        this.onChanged(value, true);
        this.updateScrollPositions();
        if (!applyingHistory) {
            finishHistoryEdit();
        }
    }

    public void setRawText(String text) {
        this.setValue(text);
        clearHistory();
    }

    public void resetViewport() {
        accessor.setCursorPos(0);
        accessor.setHighlightPos(0);
        scrolledLines = 0;
        horizontalOffset = 0;
        scrollY.updatePos(0.0);
        scrollX.updatePos(0.0);
        refreshSuggestorPos();
    }

    public void refreshFormatting(){
        this.onChanged(getValue(), true);
        this.updateScrollPositions();
    }

    private void onChanged(String newText, boolean formatText) {
        rebuildBracketIndex(newText);
        if (accessor.getResponder() != null) {
            accessor.getResponder().accept(newText);
        }
        if(hasCommandSuggestor) {
            if (formatText) this.formatText(newText);
        } else {
            this.setUnformattedText(newText);
        }
        updateScrollbarMetrics();
        refreshSuggestorPos();
    }

    private void updateScrollbarMetrics() {
        if (scrollX == null || scrollY == null || lines == null) {
            return;
        }

        int viewportWidth = Math.max(1, getTextViewportWidth());
        int maxPixelWidth = 0;
        for (String line : lines) {
            maxPixelWidth = Math.max(maxPixelWidth, accessor.getFont().width(line));
        }

        scrollY.setScale(lines.size() / (double) Math.max(1, visibleLines));
        scrollX.setScale(maxPixelWidth / (double) viewportWidth);

        int maxVerticalScroll = getMaxVerticalScroll();
        int maxHorizontalScroll = getMaxHorizontalScroll();
        scrolledLines = clamp(scrolledLines, 0, maxVerticalScroll);
        horizontalOffset = clamp(horizontalOffset, 0, maxHorizontalScroll);
        scrollY.updatePos(maxVerticalScroll == 0 ? 0.0d : scrolledLines / (double) maxVerticalScroll);
        scrollX.updatePos(maxHorizontalScroll == 0 ? 0.0d : horizontalOffset / (double) maxHorizontalScroll);
    }

    private int getMaxVerticalScroll() {
        return Math.max(0, lines.size() - visibleLines);
    }

    private int getMaxHorizontalScroll() {
        int viewportWidth = Math.max(1, getTextViewportWidth() - 3);
        int maximum = 0;
        for (String line : lines) {
            maximum = Math.max(maximum, findFirstVisibleCharacter(line, line.length(), viewportWidth));
        }
        return maximum;
    }

    private int findFirstVisibleCharacter(String line, int cursorOffset, int viewportWidth) {
        int cursor = clamp(cursorOffset, 0, line.length());
        if (accessor.getFont().width(line.substring(0, cursor)) <= viewportWidth) {
            return 0;
        }

        int low = 0;
        int high = cursor;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (accessor.getFont().width(line.substring(middle, cursor)) <= viewportWidth) {
                high = middle;
            } else {
                low = middle + 1;
            }
        }
        return low;
    }

    private void setUnformattedText(String text){
        textColors = new LinkedList<>();
        textColors.add(new Pair<>(Style.EMPTY.withColor(ChatFormatting.GRAY), 0));

        lines = new LinkedList<>();
        lines.add(text);
        lineOffsets = new LinkedList<>();
        lineOffsets.add(0);
        textOffsets = new LinkedList<>();
        textOffsets.add(0);

    }

    private interface SpacePeeker {
        Pair<Integer,String> run(int startIndex);
    }

    private void submitLine(String line, int indent){
        submitLine(line, indent, false);
    }

    private void submitLine(String line, int indent, boolean lastLine){
        if (CommandBlockStudio.AVOID_DOUBLE_NEWLINE && !lastLine) {
            String trimmedLine = line.replace(CommandBlockStudio.INDENTATION_CHAR+"", "");
            if (trimmedLine.isEmpty()) {
                return;
            }
        }
        String indentChar = "" + CommandBlockStudio.INDENTATION_CHAR;
        lines.add(indentChar.repeat(indent * CommandBlockStudio.INDENTATION_FACTOR) + line);
        lineOffsets.add(indent * CommandBlockStudio.INDENTATION_FACTOR);
        if (textOffsets.isEmpty()){
            textOffsets.add(0);
        } else {
            int index = textOffsets.size()-1;
            textOffsets.add(
                    textOffsets.get(index) + (lines.get(index).length() - lineOffsets.get(index))
            );
        }
    }

    private void formatText(String text) {
        Font textRenderer = Minecraft.getInstance().font;
        textColors = new LinkedList<>();
        List<Pair<Integer,Integer>> colorIndices = suggestor.getColors(text, 0);
        Stack<Integer> colorStack = new Stack<>();
        int currentColorListIndex = 0;
        int currentHighlightColor = 0;

        lines = new LinkedList<String>();
        lineOffsets = new LinkedList<Integer>();
        textOffsets = new LinkedList<Integer>();
        char[] textArr = text.toCharArray();
        int linestart = 0;
        int parenthesesDepth = 0;
        int currentIndex = 0;
        boolean singleQuoteString = false;
        boolean doubleQuoteString = false;
        boolean escapeChar = false;

        char current;
        String currentLine = "";
        boolean newLine = false;
        int lastWordStart = -1;

        SpacePeeker peeker = new SpacePeeker() {
            @Override
            public Pair<Integer,String> run(int startIndex) {
                // Check for following spaces, makes for consistent indentation
                StringBuilder outputLine = new StringBuilder();
                int tempCurrentIndex = startIndex + 1;
                while (tempCurrentIndex < textArr.length && textArr[tempCurrentIndex] == ' ') {
                    outputLine.append(textArr[tempCurrentIndex]);
                    tempCurrentIndex++;
                }
                startIndex = tempCurrentIndex - 1;

                return new Pair<>(startIndex, outputLine.toString());
            }
        };

        while(currentIndex < textArr.length) {
            while(currentColorListIndex < colorIndices.size() && colorIndices.get(currentColorListIndex).getB() < currentIndex){
                colorStack.push(colorIndices.get(currentColorListIndex).getA());
                currentColorListIndex++;
            }

            current = textArr[currentIndex];

            if (textRenderer.width(currentLine) > CommandBlockStudio.WRAPAROUND_WIDTH){
                if(lastWordStart < 0){
                    lastWordStart = currentLine.length();
                }
                String truncatedLine = currentLine.substring(0, Math.min(lastWordStart, currentLine.length()));
                submitLine(truncatedLine, parenthesesDepth);
                if(truncatedLine.length() < currentLine.length()) {
                    currentLine = currentLine.substring(lastWordStart);
                } else {
                    currentLine = "";
                }
                lastWordStart = -1;
            }

            int colorStartIndex = currentIndex; // Necessary to make color start in right spot when peeking ahead
            switch(current){
                case '{':
                case '[':
                    escapeChar = false;
                    if (singleQuoteString || doubleQuoteString){
                        currentLine += current;
                        newLine = false;
                        break;
                    }
                    if (CommandBlockStudio.NEWLINE_PRE_OPEN_BRACKET){
                        submitLine(currentLine, parenthesesDepth);
                        lastWordStart = -1;
                        currentLine = "";
                        newLine = true;
                    }
                    currentLine += current;
                    if (CommandBlockStudio.NEWLINE_POST_OPEN_BRACKET){
                        // Peek ahead for spaces
                        Pair<Integer,String> peekResult = peeker.run(currentIndex);
                        currentIndex = peekResult.getA();
                        currentLine += peekResult.getB();

                        submitLine(currentLine, parenthesesDepth);
                        lastWordStart = -1;
                        currentLine = "";
                        newLine = true;
                    }
                    if (newLine) {
                        parenthesesDepth++;
                    }

                    if (currentColorListIndex > 0) {
                        currentHighlightColor = colorIndices.get(currentColorListIndex - 1).getA();
                        colorStack.push(currentHighlightColor);
                        if (currentHighlightColor != 0) {
                            colorIndices.add(currentColorListIndex, new Pair<>(getHighlightColorIndex(currentHighlightColor + 1), colorStartIndex));
                            currentColorListIndex++;
                        }
                    }
                    break;
                case '}':
                case ']':
                    escapeChar = false;
                    if (singleQuoteString || doubleQuoteString){
                        currentLine += current;
                        newLine = false;
                        break;
                    }
                    boolean indentationChanged = false;
                    if (CommandBlockStudio.NEWLINE_PRE_CLOSE_BRACKET){
                        submitLine(currentLine, parenthesesDepth);
                        lastWordStart = -1;
                        currentLine = "";
                        newLine = true;
                        parenthesesDepth = Math.max(0,parenthesesDepth-1);
                        indentationChanged = true;
                    }
                    currentLine += current;
                    if (CommandBlockStudio.NEWLINE_POST_CLOSE_BRACKET){
                        // Peek ahead for spaces
                        Pair<Integer,String> peekResult = peeker.run(currentIndex);
                        currentIndex = peekResult.getA();
                        currentLine += peekResult.getB();

                        submitLine(currentLine, parenthesesDepth);
                        lastWordStart = -1;
                        currentLine = "";
                        newLine = true;
                        if (!indentationChanged)
                            parenthesesDepth = Math.max(0,parenthesesDepth-1);
                    }

                    if (currentColorListIndex > 0 && colorIndices.get(currentColorListIndex - 1).getA() != 0) {
                        colorIndices.add(currentColorListIndex, new Pair<>(colorStack.pop(), colorStartIndex + 1));
                        currentColorListIndex++;
                    }
                    break;
                case ',':
                    escapeChar = false;
                    currentLine += current;
                    lastWordStart = currentLine.length();

                    // Peek ahead for spaces
                    Pair<Integer,String> peekResult = peeker.run(currentIndex);
                    currentIndex = peekResult.getA();
                    currentLine += peekResult.getB();

                    if (CommandBlockStudio.NEWLINE_POST_COMMA){
                        submitLine(currentLine, parenthesesDepth);
                        lastWordStart = -1;
                        currentLine = "";
                        //newLine = true;
                    }
                    break;
                case ' ':
                    lastWordStart = currentLine.length();
                    currentLine += current;
                    newLine = false;
                    escapeChar = false;
                    break;
                case '\\':
                    escapeChar = !escapeChar;
                    currentLine += current;
                    newLine = false;
                    break;
                case '"':
                    if (!CommandBlockStudio.FORMAT_STRINGS && !singleQuoteString && !escapeChar){
                        doubleQuoteString = !doubleQuoteString;
                        escapeChar = false;
                    }
                    currentLine += current;
                    break;
                case '\'':
                    if (!CommandBlockStudio.FORMAT_STRINGS && !escapeChar){
                        singleQuoteString = !singleQuoteString;
                        escapeChar = false;
                    }
                    currentLine += current;
                    break;
                default:
                    currentLine += current;
                    newLine = false;
                    escapeChar = false;
                    break;
            }

            currentIndex++;
        }
        if (!currentLine.isEmpty()){
            submitLine(currentLine, parenthesesDepth, true);
        }

        for(Pair<Integer,Integer> p : colorIndices){
            textColors.add(new Pair<>(suggestor.getColor(p.getA()),p.getB()));
        }

    }

    private int getHighlightColorIndex(int i){
        int index = i - 2;
        int count = suggestor.getHighlighColorCount();
        if(index < 0) index+= count;
        return (index % count) + 2;
    }

    private void transformTokenOutput(List<Component> output){
        textColors.clear();
        int index = 0;
        for(Component text : output) {
            if (text.getStyle().getColor() != null) {
                textColors.add(new Pair<>(text.getStyle(), index));
            }
            index += text.getString().length();
        }
    }

    private boolean getHovered(double mouseX, double mouseY){
        return mouseX >= (double) this.getX() && mouseX < (double)(this.getX() + this.width) && mouseY >= (double) this.getY() && mouseY < (double)(this.getY() + this.height);
    }

    public void onClick(double mouseX, double mouseY, int button){
        if (!this.isVisible()) {
            return;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && visibleStatusDiagnostic != null
                && mouseX >= statusBarX1 && mouseX < statusBarX2
                && mouseY >= statusBarY1 && mouseY < statusBarY2) {
            jumpToDiagnostic(visibleStatusDiagnostic);
            return;
        }

        boolean scrollbarClicked = scrollX.mouseClicked(mouseX, mouseY, button);
        scrollbarClicked = scrollY.mouseClicked(mouseX, mouseY, button) || scrollbarClicked;
        if (scrollbarClicked) {
            this.setFocused(true);
            return;
        }

        boolean hovered = getHovered(mouseX, mouseY);
        if (accessor.getCanLoseFocus()) {
            this.setFocused(hovered);
        }
        if(this.isFocused() && hovered && mouseY < getTextViewportBottom() && button == 0) {
            int clickedIndex = pointToIndex(mouseX, mouseY);
            boolean handledNumber = !Minecraft.getInstance().hasShiftDown() && handleNumberClick(clickedIndex);
            if (!handledNumber) {
                clearClickedNumber();
                this.moveCursorTo(clickedIndex, Minecraft.getInstance().hasShiftDown());
            }
            cursorPosPreference = new Pair<>((int)mouseX, (int)mouseY);
            if(!handledNumber && timeSinceClick < 0.25f){
                selectWord();
            }
            timeSinceClick = 0.0f;
        }
    }

    private boolean handleNumberClick(int clickedIndex) {
        if (clickedNumberStart >= 0
                && accessor.getCursorPos() == clickedNumberStart
                && accessor.getHighlightPos() == clickedNumberEnd
                && clickedIndex >= clickedNumberStart
                && clickedIndex <= clickedNumberEnd) {
            accessor.setCursorPos(clickedNumberEnd);
            accessor.setHighlightPos(clickedNumberEnd);
            clearClickedNumber();
            onChanged(accessor.getValue(), false);
            return true;
        }

        Matcher matcher = NUMBER_TOKEN.matcher(accessor.getValue());
        while (matcher.find()) {
            if (clickedIndex >= matcher.start() && clickedIndex <= matcher.end()) {
                clickedNumberStart = matcher.start();
                clickedNumberEnd = matcher.end();
                accessor.setCursorPos(clickedNumberStart);
                accessor.setHighlightPos(clickedNumberEnd);
                onChanged(accessor.getValue(), false);
                return true;
            }
        }
        return false;
    }

    private void clearClickedNumber() {
        clickedNumberStart = -1;
        clickedNumberEnd = -1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount){
        if (!this.isVisible()) {
            return false;
        }
        if (hasCommandSuggestor && suggestor != null
                && suggestor.mouseScrolledAt(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if(LShiftPressed || RShiftPressed){
            int maxScroll = getMaxHorizontalScroll();
            if (!scrollX.isScrollable() || maxScroll == 0) {
                return false;
            }
            horizontalOffset = clamp(horizontalOffset-(int)verticalAmount*CommandBlockStudio.SCROLL_STEP_X, 0, maxScroll);
            scrollX.updatePos((double)horizontalOffset / maxScroll);
        } else {
            int maxScroll = getMaxVerticalScroll();
            if (!scrollY.isScrollable() || maxScroll == 0) {
                return false;
            }
            scrolledLines = clamp(scrolledLines-(int)verticalAmount*CommandBlockStudio.SCROLL_STEP_Y, 0, maxScroll);
            scrollY.updatePos((double)scrolledLines / maxScroll);
        }
        refreshSuggestorPos();
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.isVisible()) {
            return;
        }
        scrollX.mouseMoved(mouseX, mouseY);
        scrollY.mouseMoved(mouseX, mouseY);
    }

    public void onDrag(double mouseX, double mouseY, double offsetX, double offsetY){
        if (!this.isVisible()) {
            return;
        }
        scrollX.onDrag(mouseX, mouseY, offsetX, offsetY);
        scrollY.onDrag(mouseX, mouseY, offsetX, offsetY);
        if (scrollX.isDragging() || scrollY.isDragging()) {
            refreshSuggestorPos();
            return;
        }

        if (this.isHovered() && this.isFocused()){
            clearClickedNumber();
            setCursorPosition(pointToIndex(mouseX, mouseY));
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double offsetX, double offsetY) {
        onDrag(event.x(), event.y(), offsetX, offsetY);
    }

    public void onRelease(double mouseX, double mouseY){
        if (!this.isVisible()) {
            return;
        }
        scrollX.onRelease(mouseX, mouseY);
        scrollY.onRelease(mouseX, mouseY);
    }

    private void moveCursorVertical(int delta){
        Pair<Integer, Integer> lineAndOffset = indexToLineAndOffset(accessor.invokeGetCursorPos(0));
        int yPreference = getY() + 5 + (lineAndOffset.getA() - scrolledLines) * 10;
        cursorPosPreference.setB(yPreference + delta * 10);
        int index = pointToIndex(cursorPosPreference.getA(), cursorPosPreference.getB());
        moveCursorTo(index, Minecraft.getInstance().hasShiftDown());

        updateScrollPositions();
    }

    @Override
    public void moveCursor(int offset, boolean shiftKeyPressed) {
        Font textRenderer = accessor.getFont();
        this.moveCursorTo(accessor.invokeGetCursorPos(offset), shiftKeyPressed);
        if(lines.isEmpty()) return;
        Pair<Integer, Integer> lineAndOffset = indexToLineAndOffset(accessor.invokeGetCursorPos(0));
        String line = lines.get(lineAndOffset.getA());
        int visibleStart = clamp(horizontalOffset, 0, line.length());
        int cursorOffset = clamp(lineAndOffset.getB(), visibleStart, line.length());
        int xPreference = getTextLeft() + textRenderer.width(line.substring(visibleStart, cursorOffset));
        cursorPosPreference = new Pair<>(xPreference, this.getY() + 10 * (lineAndOffset.getA() - scrolledLines));

        updateScrollPositions();
    }

    private void updateScrollPositions(){
        Pair<Integer, Integer> lineAndOffset = indexToLineAndOffset(accessor.invokeGetCursorPos(0));
        if(lines.size() < 1){
            horizontalOffset = 0;
            scrollY.updatePos(0);
            scrolledLines = 0;
            scrollX.updatePos(0);
            return;
        }
        String line = lines.get(lineAndOffset.getA());
        int cursorOffset = clamp(lineAndOffset.getB(), 0, line.length());
        int textWidth = Math.max(1, getTextViewportWidth() - 3);
        int maxHorizontalScroll = getMaxHorizontalScroll();
        if (cursorOffset < horizontalOffset) {
            horizontalOffset = cursorOffset;
        } else {
            int visibleStart = clamp(horizontalOffset, 0, line.length());
            if (accessor.getFont().width(line.substring(visibleStart, cursorOffset)) > textWidth) {
                horizontalOffset = findFirstVisibleCharacter(line, cursorOffset, textWidth);
            }
        }
        horizontalOffset = clamp(horizontalOffset, 0, maxHorizontalScroll);
        scrollX.updatePos(maxHorizontalScroll == 0 ? 0.0d : (double) horizontalOffset / maxHorizontalScroll);


        int lineIndex = lineAndOffset.getA();
        int maxVerticalScroll = getMaxVerticalScroll();
        if(lineIndex < scrolledLines){
            scrolledLines = clamp(scrolledLines - (scrolledLines - lineIndex), 0, maxVerticalScroll);
            scrollY.updatePos(maxVerticalScroll == 0 ? 0.0d : (double) scrolledLines / maxVerticalScroll);
        } else if(lineIndex >= scrolledLines + visibleLines){
            scrolledLines = clamp(scrolledLines + 1 + ((lineIndex - scrolledLines) - visibleLines), 0, maxVerticalScroll);
            scrollY.updatePos(maxVerticalScroll == 0 ? 0.0d : (double) scrolledLines / maxVerticalScroll);
        }
    }

    @Override
    public void moveCursorTo(int cursor, boolean shiftKeyPressed) {
        this.setCursorPosition(cursor);
        if (!shiftKeyPressed) {
            this.setHighlightPos(accessor.getCursorPos());
        }
        this.onChanged(accessor.getValue(), false);
    }

    public void setScroll(double value){
        this.scrolledLines = (int)Math.max(Math.round(getMaxVerticalScroll() * value),0);
        refreshSuggestorPos();
    }

    public void revealCursor() {
        updateScrollPositions();
        refreshSuggestorPos();
    }

    public Component getDiagnosticLocation(CommandDiagnostic diagnostic) {
        Pair<Integer, Integer> position = indexToLineAndOffset(clamp(diagnostic.start(), 0, accessor.getValue().length()));
        return Component.translatable("cbs.diagnostic.location", position.getA() + 1, position.getB() + 1);
    }

    public String getDiagnosticSnippet(CommandDiagnostic diagnostic) {
        if (lines.isEmpty()) {
            return "";
        }
        Pair<Integer, Integer> position = indexToLineAndOffset(clamp(diagnostic.start(), 0, accessor.getValue().length()));
        String line = lines.get(clamp(position.getA(), 0, lines.size() - 1)).strip();
        return accessor.getFont().plainSubstrByWidth(line, 220);
    }

    public void jumpToDiagnostic(CommandDiagnostic diagnostic) {
        int length = accessor.getValue().length();
        int start = clamp(diagnostic.start(), 0, length);
        int end = clamp(Math.max(start, diagnostic.end()), 0, length);
        accessor.setCursorPos(start);
        accessor.setHighlightPos(end);
        setFocused(true);
        revealCursor();
    }

    public void setHorizontalOffset(double value){
        this.horizontalOffset = (int)Math.max(Math.floor(getMaxHorizontalScroll() * value),0);
        refreshSuggestorPos();
    }

    @Override
    public void setEditable(boolean value){
        super.setEditable(value);
        if(this.suggestor != null) this.suggestor.setAllowSuggestions(value);
    }

    public boolean wasModified(){
        return textModified;
    }

    public int getHighlightPosition() {
        return accessor.getHighlightPos();
    }

    public int countMatches(String query, boolean matchCase) {
        return TextSearchEngine.countMatches(accessor.getValue(), query, matchCase);
    }

    public boolean findMatch(String query, boolean backwards, boolean matchCase) {
        if (query == null || query.isEmpty() || accessor.getValue().isEmpty()) {
            return false;
        }
        int selectionStart = Math.min(accessor.getCursorPos(), accessor.getHighlightPos());
        int selectionEnd = Math.max(accessor.getCursorPos(), accessor.getHighlightPos());
        Optional<TextSearchEngine.Match> match = TextSearchEngine.findMatch(
                accessor.getValue(), query, selectionStart, selectionEnd, backwards, matchCase
        );
        if (match.isEmpty()) {
            return false;
        }
        selectSearchMatch(match.get().start(), match.get().end());
        return true;
    }

    public boolean replaceCurrentMatch(String query, String replacement, boolean matchCase) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        int selectionStart = Math.min(accessor.getCursorPos(), accessor.getHighlightPos());
        int selectionEnd = Math.max(accessor.getCursorPos(), accessor.getHighlightPos());
        boolean matches = TextSearchEngine.selectionMatches(
                accessor.getValue(), selectionStart, selectionEnd, query, matchCase
        );
        if (!matches) {
            if (!findMatch(query, false, matchCase)) {
                return false;
            }
        }
        insertText(replacement == null ? "" : replacement);
        findMatch(query, false, matchCase);
        return true;
    }

    public int replaceAllMatches(String query, String replacement, boolean matchCase) {
        if (query == null || query.isEmpty()) {
            return 0;
        }
        TextSearchEngine.ReplaceAllResult result = TextSearchEngine.replaceAll(
                accessor.getValue(), query, replacement, matchCase
        );
        if (result.count() == 0) {
            return 0;
        }
        setValue(result.text());
        textModified = true;
        return result.count();
    }

    private void selectSearchMatch(int start, int end) {
        accessor.setCursorPos(clamp(start, 0, accessor.getValue().length()));
        accessor.setHighlightPos(clamp(end, 0, accessor.getValue().length()));
        setFocused(true);
        clearClickedNumber();
        revealCursor();
    }

    public void resetModified(){
        textModified = false;
    }

    public void restoreEditorState(String text, int cursorPosition, int highlightPosition, boolean modified) {
        this.setRawText(text);
        accessor.setCursorPos(clamp(cursorPosition, 0, text.length()));
        accessor.setHighlightPos(clamp(highlightPosition, 0, text.length()));
        textModified = modified;
        updateScrollPositions();
        refreshSuggestorPos();
    }

    private void rebuildBracketIndex(String text) {
        if (text.equals(bracketIndexText)) {
            return;
        }

        bracketIndexText = text;
        bracketPartners = new int[text.length()];
        Arrays.fill(bracketPartners, -2);
        Deque<BracketEntry> stack = new ArrayDeque<>();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean escaped = false;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if ((singleQuoted || doubleQuoted) && character == '\\') {
                escaped = true;
                continue;
            }
            if (!doubleQuoted && character == '\'') {
                singleQuoted = !singleQuoted;
                continue;
            }
            if (!singleQuoted && character == '"') {
                doubleQuoted = !doubleQuoted;
                continue;
            }
            if (singleQuoted || doubleQuoted) {
                continue;
            }

            if (character == '{' || character == '[' || character == '(') {
                bracketPartners[i] = -1;
                stack.push(new BracketEntry(character, i));
            } else if (character == '}' || character == ']' || character == ')') {
                bracketPartners[i] = -1;
                if (!stack.isEmpty() && matchingClose(stack.peek().character()) == character) {
                    BracketEntry opening = stack.pop();
                    bracketPartners[opening.index()] = i;
                    bracketPartners[i] = opening.index();
                }
            }
        }
    }

    void refreshSuggestorPos(){
        if(!hasCommandSuggestor || suggestor == null || accessor.getFont() == null) return;
        int selectionStart = accessor.getCursorPos();
        int selectionEnd = accessor.getHighlightPos();

        if(selectionStart > selectionEnd){
            selectionStart = selectionEnd;
        }
        Pair<Integer, Integer> cursor = indexToLineAndOffset(selectionStart);
        int selectionStartOffset = Math.max(cursor.getB() - horizontalOffset, 0);
        int fontHeight = accessor.getFont().lineHeight + 1;
        if(lines.isEmpty()){
            suggestor.setPos(getTextLeft(), getY() + 5 + fontHeight);
            suggestor.refreshRenderPos();
            return;
        }
        String line = lines.get(cursor.getA());
        line = line.substring(clamp(horizontalOffset, 0,line.length()));
        int x = getTextLeft() + accessor.getFont().width(line.substring(0, Math.min(selectionStartOffset,line.length())));
        int y = this.getY() + 5 + fontHeight + (cursor.getA() - scrolledLines) * fontHeight;

        suggestor.setPos(x, y);
        suggestor.refreshRenderPos();
    }

    private int clamp(int i, int min, int max){
        return Math.max(Math.min(i,max),min);
    }

    private double clamp(double i, double min, double max){
        return Math.max(Math.min(i,max),min);
    }

}
