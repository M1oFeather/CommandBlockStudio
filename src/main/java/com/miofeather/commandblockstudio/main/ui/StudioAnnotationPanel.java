package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public final class StudioAnnotationPanel {
    private static final int COMPACT_EDITOR_TOP = 25;
    private static final int EDITOR_TOP = 29;
    private static final int CHARACTER_COUNTER_SPACE = 15;
    private static final int SAVE_ROW_HEIGHT = 18;
    private static final int MIN_EDITOR_HEIGHT = 20;
    private static final int MIN_HISTORY_EDITOR_HEIGHT = 36;
    private static final int HISTORY_LAYOUT_RESERVE = 115;
    private static final int MIN_HISTORY_PANEL_HEIGHT = HISTORY_LAYOUT_RESERVE + MIN_HISTORY_EDITOR_HEIGHT;
    private static final int HISTORY_ROW_HEIGHT = 28;
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter
            .ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Font font;
    private final MultiLineEditBox editor;
    private final Button saveButton;
    private final Button restoreButton;
    private final Consumer<String> saveAction;
    private final Consumer<CommandBlockAnnotationNetwork.EditHistoryEntry> restoreAction;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int historyHeaderY;
    private final int historyEntriesY;
    private final boolean historyVisible;
    private boolean visible;
    private boolean applyingRemoteValue;
    private boolean dirty;
    private List<CommandBlockAnnotationNetwork.EditHistoryEntry> history = List.of();
    private int historyScroll;
    private int selectedHistoryIndex = -1;
    private Status status = Status.LOADING;

    public StudioAnnotationPanel(
            Font font,
            int x,
            int y,
            int width,
            int height,
            Consumer<String> saveAction,
            Consumer<CommandBlockAnnotationNetwork.EditHistoryEntry> restoreAction
    ) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.saveAction = saveAction;
        this.restoreAction = restoreAction;
        this.historyVisible = height >= MIN_HISTORY_PANEL_HEIGHT;
        int editorTop = height < 100 ? COMPACT_EDITOR_TOP : EDITOR_TOP;
        int editorHeight = historyVisible
                ? Mth.clamp(Math.min(height / 3, height - HISTORY_LAYOUT_RESERVE), MIN_HISTORY_EDITOR_HEIGHT, 100)
                : Math.max(MIN_EDITOR_HEIGHT, height - editorTop - CHARACTER_COUNTER_SPACE - SAVE_ROW_HEIGHT - 2);
        this.editor = MultiLineEditBox.builder()
                .setX(x + 7)
                .setY(y + editorTop)
                .setPlaceholder(Component.translatable("cbs.annotation.placeholder"))
                .build(
                        font,
                        Math.max(64, width - 14),
                        editorHeight,
                        Component.translatable("cbs.annotation.title")
                );
        this.editor.setCharacterLimit(CommandBlockAnnotationNetwork.MAX_ANNOTATION_LENGTH);
        this.saveButton = Button.builder(Component.translatable("cbs.annotation.save"), button -> save())
                .bounds(
                        x + width - 63,
                        editor.getY() + editor.getHeight() + CHARACTER_COUNTER_SPACE,
                        56,
                        SAVE_ROW_HEIGHT
                )
                .build();
        this.historyHeaderY = saveButton.getY() + 23;
        this.historyEntriesY = historyHeaderY + 14;
        this.restoreButton = Button.builder(
                        Component.translatable("cbs.annotation.history.restore"),
                        button -> restoreSelectedHistory())
                .bounds(x + width - 55, historyHeaderY - 3, 48, 16)
                .build();
        this.saveButton.active = false;
        this.restoreButton.active = false;
        this.editor.setValueListener(value -> {
            if (!applyingRemoteValue) {
                dirty = true;
                status = Status.MODIFIED;
                saveButton.active = true;
            }
        });
        setVisible(false);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        editor.visible = visible;
        saveButton.visible = visible;
        restoreButton.visible = visible && historyVisible;
        if (!visible) {
            editor.setFocused(false);
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void beginLoading() {
        applyingRemoteValue = true;
        editor.setValue("");
        applyingRemoteValue = false;
        dirty = false;
        history = List.of();
        historyScroll = 0;
        selectedHistoryIndex = -1;
        status = Status.LOADING;
        saveButton.active = false;
        restoreButton.active = false;
    }

    public void acceptRemoteValue(CommandBlockAnnotationNetwork.AnnotationSnapshot snapshot) {
        history = List.copyOf(snapshot.history());
        historyScroll = Mth.clamp(historyScroll, 0, getMaxHistoryScroll());
        selectedHistoryIndex = -1;
        restoreButton.active = false;
        if (dirty) {
            return;
        }
        applyingRemoteValue = true;
        editor.setValue(snapshot.annotation());
        applyingRemoteValue = false;
        dirty = false;
        status = Status.SAVED;
        saveButton.active = false;
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!visible) {
            return;
        }
        graphics.fill(x, y, x + width, y + height, 0xFF16191E);
        graphics.fill(x, y, x + width, y + 23, 0xFF1C2026);
        graphics.fill(x, y, x + 2, y + height, 0xFF343A42);
        graphics.fill(x, y + 22, x + width, y + 23, 0xFF2A3037);
        graphics.text(font, Component.translatable("cbs.annotation.title"), x + 8, y + 7, 0xFFE8EAED);
        editor.extractRenderState(graphics, mouseX, mouseY, delta);
        saveButton.extractRenderState(graphics, mouseX, mouseY, delta);
        Component state = Component.translatable(status.translationKey);
        int available = Math.max(20, saveButton.getX() - x - 15);
        graphics.text(font, font.plainSubstrByWidth(state.getString(), available), x + 8, saveButton.getY() + 5, status.color);
        renderHistory(graphics, mouseX, mouseY, delta);
    }

    private void renderHistory(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!historyVisible) {
            return;
        }
        graphics.fill(x + 7, historyHeaderY - 3, x + width - 7, historyHeaderY - 2, 0xFF343A42);
        Component title = Component.translatable(
                "cbs.annotation.history",
                history.size(),
                CommandBlockAnnotationNetwork.MAX_HISTORY_ENTRIES
        );
        int titleWidth = Math.max(20, restoreButton.getX() - x - 13);
        graphics.text(font, font.plainSubstrByWidth(title.getString(), titleWidth), x + 8, historyHeaderY, 0xFFDDE2E7);
        restoreButton.extractRenderState(graphics, mouseX, mouseY, delta);

        if (history.isEmpty()) {
            graphics.text(
                    font,
                    Component.translatable("cbs.annotation.history.empty"),
                    x + 8,
                    historyEntriesY + 3,
                    0xFF7D8792
            );
            return;
        }

        int bottom = y + height - 6;
        int visibleRows = Math.max(1, (bottom - historyEntriesY) / HISTORY_ROW_HEIGHT);
        for (int row = 0; row < visibleRows; row++) {
            int index = historyScroll + row;
            if (index >= history.size()) {
                break;
            }
            int rowY = historyEntriesY + row * HISTORY_ROW_HEIGHT;
            int background = index == selectedHistoryIndex
                    ? 0xFF253847
                    : (row & 1) == 0 ? 0xFF1B2026 : 0xFF16191E;
            graphics.fill(x + 7, rowY, x + width - 7, rowY + HISTORY_ROW_HEIGHT - 1, background);
            if (index == selectedHistoryIndex) {
                graphics.fill(x + 7, rowY, x + 9, rowY + HISTORY_ROW_HEIGHT - 1, 0xFF4FC3DC);
            }
            CommandBlockAnnotationNetwork.EditHistoryEntry entry = history.get(index);
            String time = HISTORY_TIME_FORMAT.format(Instant.ofEpochMilli(entry.timestamp()));
            graphics.text(font, time, x + 11, rowY + 3, 0xFF8A949E);
            int editorX = x + 10 + font.width("00-00 00:00") + 7;
            String editorName = entry.editor().isBlank()
                    ? Component.translatable("cbs.annotation.history.initial").getString()
                    : entry.editor();
            if (index == 0) {
                editorName += " · " + Component.translatable("cbs.annotation.history.current").getString();
            }
            editorName = font.plainSubstrByWidth(editorName, Math.max(20, x + width - 10 - editorX));
            graphics.text(font, editorName, editorX, rowY + 3, 0xFFE6EDF3);

            String commandSummary = entry.snapshotAvailable()
                    ? entry.command().replace('\n', ' ').replace('\r', ' ')
                    : Component.translatable("cbs.annotation.history.unavailable").getString();
            commandSummary = font.plainSubstrByWidth(commandSummary, Math.max(20, width - 24));
            graphics.text(
                    font,
                    commandSummary,
                    x + 11,
                    rowY + 15,
                    entry.snapshotAvailable() ? 0xFFB7C0CA : 0xFF6F7882
            );
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        if (restoreButton.mouseClicked(StudioInputEvents.mouse(mouseX, mouseY, button), false)) {
            return true;
        }
        if (saveButton.mouseClicked(StudioInputEvents.mouse(mouseX, mouseY, button), false)) {
            return true;
        }
        if (historyVisible
                && mouseX >= x + 7 && mouseX < x + width - 7
                && mouseY >= historyEntriesY && mouseY < y + height - 6) {
            int row = (int) ((mouseY - historyEntriesY) / HISTORY_ROW_HEIGHT);
            int index = historyScroll + row;
            if (row >= 0 && row < getVisibleHistoryRows() && index < history.size()) {
                selectedHistoryIndex = index;
                updateRestoreButton();
                return true;
            }
        }
        if (editor.mouseClicked(StudioInputEvents.mouse(mouseX, mouseY, button), false)) {
            editor.setFocused(true);
            return true;
        }
        editor.setFocused(false);
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return visible && editor.mouseDragged(StudioInputEvents.mouse(mouseX, mouseY, button), deltaX, deltaY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        boolean handled = editor.mouseReleased(StudioInputEvents.mouse(mouseX, mouseY, button));
        handled = saveButton.mouseReleased(StudioInputEvents.mouse(mouseX, mouseY, button)) || handled;
        return restoreButton.mouseReleased(StudioInputEvents.mouse(mouseX, mouseY, button)) || handled;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible) {
            return false;
        }
        if (editor.isMouseOver(mouseX, mouseY)) {
            return editor.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (historyVisible
                && mouseX >= x + 7 && mouseX < x + width - 7
                && mouseY >= historyEntriesY && mouseY < y + height - 4) {
            int direction = scrollY > 0.0D ? -1 : scrollY < 0.0D ? 1 : 0;
            historyScroll = Mth.clamp(historyScroll + direction, 0, getMaxHistoryScroll());
            return direction != 0;
        }
        return false;
    }

    private int getMaxHistoryScroll() {
        if (!historyVisible) {
            return 0;
        }
        return Math.max(0, history.size() - getVisibleHistoryRows());
    }

    private int getVisibleHistoryRows() {
        return Math.max(1, (y + height - 6 - historyEntriesY) / HISTORY_ROW_HEIGHT);
    }

    private void updateRestoreButton() {
        restoreButton.active = selectedHistoryIndex > 0
                && selectedHistoryIndex < history.size()
                && history.get(selectedHistoryIndex).snapshotAvailable();
    }

    private void restoreSelectedHistory() {
        if (!restoreButton.active || selectedHistoryIndex < 0 || selectedHistoryIndex >= history.size()) {
            return;
        }
        restoreAction.accept(history.get(selectedHistoryIndex));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return visible && editor.isFocused() && editor.keyPressed(StudioInputEvents.key(keyCode, scanCode, modifiers));
    }

    public boolean charTyped(char codePoint, int modifiers) {
        return visible && editor.isFocused() && editor.charTyped(StudioInputEvents.character(codePoint));
    }

    private void save() {
        if (!dirty) {
            return;
        }
        saveAction.accept(editor.getValue());
        dirty = false;
        status = Status.SAVING;
        saveButton.active = false;
    }

    private enum Status {
        LOADING("cbs.annotation.loading", 0xFF8A949E),
        MODIFIED("cbs.annotation.modified", 0xFFE3B341),
        SAVING("cbs.annotation.saving", 0xFF8A949E),
        SAVED("cbs.annotation.saved", 0xFF56D364);

        private final String translationKey;
        private final int color;

        Status(String translationKey, int color) {
            this.translationKey = translationKey;
            this.color = color;
        }
    }
}
