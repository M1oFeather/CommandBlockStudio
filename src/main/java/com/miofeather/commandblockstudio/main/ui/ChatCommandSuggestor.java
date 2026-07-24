package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.insight.CommandInsight;
import com.miofeather.commandblockstudio.main.insight.CommandInsightService;
import com.miofeather.commandblockstudio.main.insight.CommandSyntaxHint;
import com.miofeather.commandblockstudio.mixin.CommandSuggestorAccessor;
import com.miofeather.commandblockstudio.mixin.SuggestionWindowAccessor;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ChatCommandSuggestor extends CommandSuggestions {
    private static final int SUGGESTION_BACKGROUND = 0xF20D1117;
    private static final int SUGGESTION_BORDER = 0xFF59636E;

    private final Minecraft minecraft;
    private final Screen owner;
    private final CommandSuggestorAccessor accessor;
    private final CommandInsightService insightService;
    private Object preparedSuggestionWindow;
    private Map<Suggestion, CommandSuggestionVisual> suggestionVisuals = Map.of();
    private boolean suggestionIconsVisible;
    private int panelX = Integer.MAX_VALUE;

    public ChatCommandSuggestor(Minecraft minecraft, Screen owner, EditBox input, Font font) {
        super(minecraft, owner, input, font, false, false, 1, 10, true, -805306368);
        this.minecraft = minecraft;
        this.owner = owner;
        this.accessor = (CommandSuggestorAccessor) this;
        this.insightService = new CommandInsightService(minecraft, accessor);
        setAllowHiding(false);
    }

    public void setPanelX(int panelX) {
        this.panelX = panelX;
        relocateSuggestions();
    }

    public boolean isCommandMode() {
        return accessor.getInput().getValue().startsWith("/");
    }

    @Override
    public void updateCommandInfo() {
        super.updateCommandInfo();
        if (!isCommandMode() || !accessor.getInput().isFocused()) {
            return;
        }
        String command = accessor.getInput().getValue();
        int cursor = accessor.getInput().getCursorPosition();
        insightService.findStructuredSyntaxHint(command, cursor)
                .filter(hint -> !hint.suggestions().isEmpty())
                .ifPresent(hint -> showSyntaxSuggestions(command, cursor, hint));
    }

    public void showSuggestionsAtCursor() {
        updateCommandInfo();
        String command = accessor.getInput().getValue();
        int cursor = accessor.getInput().getCursorPosition();
        Optional<CommandSyntaxHint> hint = insightService.findSyntaxHint(command, cursor);
        if (hint.isPresent() && !hint.get().suggestions().isEmpty()) {
            showSyntaxSuggestions(command, cursor, hint.get());
            return;
        }
        CompletableFuture<Suggestions> pendingSuggestions = accessor.getPendingSuggestions();
        if (pendingSuggestions == null) {
            return;
        }
        if (pendingSuggestions.isDone()
                && !pendingSuggestions.isCompletedExceptionally()
                && !pendingSuggestions.isCancelled()) {
            showSuggestions(true);
            relocateSuggestions();
            return;
        }
        pendingSuggestions.thenRun(() -> minecraft.execute(() -> {
            if (accessor.getInput().getValue().equals(command)
                    && accessor.getInput().getCursorPosition() == cursor) {
                showSuggestions(true);
                relocateSuggestions();
            }
        }));
    }

    private void showSyntaxSuggestions(String command, int cursor, CommandSyntaxHint hint) {
        StringRange range = StringRange.between(hint.replacementStart(), cursor);
        List<Suggestion> candidates = hint.suggestions().stream()
                .map(value -> new Suggestion(range, value))
                .toList();
        accessor.setPendingSuggestions(CompletableFuture.completedFuture(new Suggestions(range, candidates)));
        showSuggestions(true);
        relocateSuggestions();
    }

    @Override
    public void showSuggestions(boolean narrateFirstSuggestion) {
        super.showSuggestions(narrateFirstSuggestion);
        relocateSuggestions();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isCommandMode()) {
            super.extractRenderState(graphics, mouseX, mouseY);
            return;
        }

        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null) {
            preparedSuggestionWindow = null;
            suggestionVisuals = Map.of();
            suggestionIconsVisible = false;
            extractUsage(graphics);
            return;
        }

        prepareSuggestionWindow(window);
        relocateSuggestions();
        renderSuggestionBackground(graphics, window);
        if (shouldSuppressNativeTooltip(window, mouseX, mouseY)) {
            accessor.getSuggestions().extractRenderState(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE);
        } else {
            accessor.getSuggestions().extractRenderState(graphics, mouseX, mouseY);
        }
        renderSuggestionIcons(graphics, window);
    }

    @Override
    public void extractUsage(GuiGraphicsExtractor graphics) {
        int maximumRight = Math.max(8, Math.min(owner.width - 4, panelX - 6));
        int width = Math.min(accessor.getCommandUsageWidth(), Math.max(1, maximumRight - 4));
        int x = Mth.clamp(
                accessor.getCommandUsagePosition(),
                3,
                Math.max(3, maximumRight - width - 1)
        );
        int row = 0;
        for (FormattedCharSequence usage : accessor.getCommandUsage()) {
            int y = owner.height - 14 - 13 - 12 * row;
            graphics.fill(x - 1, y, x + width + 1, y + 12, accessor.getFillColor());
            graphics.enableScissor(x, y, x + width, y + 12);
            graphics.text(accessor.getFont(), usage, x, y + 2, -1);
            graphics.disableScissor();
            row++;
        }
    }

    private void prepareSuggestionWindow(SuggestionWindowAccessor window) {
        if (preparedSuggestionWindow == window) {
            return;
        }
        preparedSuggestionWindow = window;
        String command = accessor.getInput().getValue();
        int cursor = accessor.getInput().getCursorPosition();
        List<Suggestion> suggestions = new ArrayList<>(window.getSuggestionList());
        if (insightService.expectsPlayerSuggestions(command, cursor)) {
            suggestions.sort(Comparator.comparingInt(
                    suggestion -> CommandSuggestionVisual.playerRank(minecraft, suggestion.getText())
            ));
            window.setSuggestionList(suggestions);
            window.invokeSelect(0);
        }

        Map<Suggestion, CommandSuggestionVisual> visuals = new HashMap<>();
        for (Suggestion suggestion : suggestions) {
            CommandSuggestionVisual.resolve(minecraft, insightService, command, cursor, suggestion.getText())
                    .ifPresent(visual -> visuals.put(suggestion, visual));
        }
        suggestionVisuals = Map.copyOf(visuals);
        suggestionIconsVisible = !suggestionVisuals.isEmpty();
    }

    private void relocateSuggestions() {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null) {
            return;
        }
        prepareSuggestionWindow(window);
        Rect2i rect = window.getRect();
        int iconGutter = suggestionIconsVisible ? 13 : 0;
        int minimumX = 3 + iconGutter;
        int maximumRight = Math.max(minimumX + rect.getWidth(), Math.min(owner.width - 4, panelX - 6));
        int maximumX = Math.max(minimumX, maximumRight - rect.getWidth());
        int x = Mth.clamp(rect.getX(), minimumX, maximumX);
        int maximumY = Math.max(4, owner.height - 18 - rect.getHeight());
        int y = Mth.clamp(rect.getY(), 4, maximumY);
        window.setRect(new Rect2i(x, y, rect.getWidth(), rect.getHeight()));
    }

    private void renderSuggestionBackground(GuiGraphicsExtractor graphics, SuggestionWindowAccessor window) {
        Rect2i rect = window.getRect();
        int left = rect.getX() - (suggestionIconsVisible ? 13 : 0);
        graphics.fill(
                left - 1,
                rect.getY() - 1,
                rect.getX() + rect.getWidth() + 1,
                rect.getY() + rect.getHeight() + 1,
                SUGGESTION_BORDER
        );
        graphics.fill(
                left,
                rect.getY(),
                rect.getX() + rect.getWidth(),
                rect.getY() + rect.getHeight(),
                SUGGESTION_BACKGROUND
        );
    }

    private void renderSuggestionIcons(GuiGraphicsExtractor graphics, SuggestionWindowAccessor window) {
        if (!suggestionIconsVisible) {
            return;
        }
        Rect2i rect = window.getRect();
        int visibleRows = Math.min(
                window.getSuggestionList().size() - window.getOffset(),
                rect.getHeight() / 12
        );
        for (int row = 0; row < visibleRows; row++) {
            Suggestion suggestion = window.getSuggestionList().get(window.getOffset() + row);
            CommandSuggestionVisual visual = suggestionVisuals.get(suggestion);
            if (visual != null) {
                visual.renderIcon(graphics, rect.getX() - 12, rect.getY() + row * 12 + 1, 10);
            }
        }
    }

    private boolean shouldSuppressNativeTooltip(SuggestionWindowAccessor window, int mouseX, int mouseY) {
        if (!window.getRect().contains(mouseX, mouseY)) {
            return false;
        }
        int row = (mouseY - window.getRect().getY()) / 12 + window.getOffset();
        if (row < 0 || row >= window.getSuggestionList().size()
                || window.getSuggestionList().get(row).getTooltip() == null) {
            return false;
        }
        window.invokeSelect(row);
        return true;
    }

    public CommandInsightService getInsightService() {
        return insightService;
    }

    public String getSelectedSuggestionKey() {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || window.getCurrent() < 0 || window.getCurrent() >= window.getSuggestionList().size()) {
            return "";
        }
        return window.getSuggestionList().get(window.getCurrent()).getText();
    }

    public Optional<CommandInsight> getSelectedInsight() {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || window.getCurrent() < 0 || window.getCurrent() >= window.getSuggestionList().size()) {
            return Optional.empty();
        }
        Suggestion suggestion = window.getSuggestionList().get(window.getCurrent());
        if (suggestion.getTooltip() != null) {
            return Optional.of(new CommandInsight(
                    suggestion.getText(),
                    ComponentUtils.fromMessage(suggestion.getTooltip()).getString(),
                    List.of(),
                    true
            ));
        }
        return insightService.describeSuggestion(
                accessor.getInput().getValue(),
                accessor.getInput().getCursorPosition(),
                suggestion.getText()
        );
    }

    public Optional<CommandSuggestionVisual> getSelectedVisual() {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || window.getCurrent() < 0 || window.getCurrent() >= window.getSuggestionList().size()) {
            return Optional.empty();
        }
        return Optional.ofNullable(suggestionVisuals.get(window.getSuggestionList().get(window.getCurrent())));
    }
}
