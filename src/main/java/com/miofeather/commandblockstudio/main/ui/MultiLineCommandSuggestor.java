package com.miofeather.commandblockstudio.main.ui;

import com.miofeather.commandblockstudio.main.util.Pair;
import com.miofeather.commandblockstudio.main.insight.CommandInsight;
import com.miofeather.commandblockstudio.main.insight.CommandInsightService;
import com.miofeather.commandblockstudio.mixin.CommandSuggestorAccessor;
import com.miofeather.commandblockstudio.mixin.SuggestionWindowAccessor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class MultiLineCommandSuggestor extends CommandSuggestions {
    private static final List<Style> HIGHLIGHT_STYLES = Stream.of(ChatFormatting.RED, ChatFormatting.GRAY, ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD).map(Style.EMPTY::withColor).collect(ImmutableList.toImmutableList());
    private static final int SUGGESTION_BACKGROUND = 0xFF0D1117;
    private static final int SUGGESTION_BORDER = 0xFF59636E;

    private CommandSuggestorAccessor accessor;
    private CommandInsightService insightService;
    private Pair<Integer, Integer> startPos;
    private int x, y;
    private Object preparedSuggestionWindow;
    private Map<Suggestion, CommandSuggestionVisual> suggestionVisuals = Map.of();
    private boolean suggestionIconsVisible;

    public MultiLineCommandSuggestor(Minecraft client, Screen owner, EditBox textField, Font textRenderer, boolean slashOptional, boolean suggestingWhenEmpty, int inWindowIndexOffset, int maxSuggestionSize, boolean chatScreenSized, int color) {
        super(client, owner, textField, textRenderer, slashOptional, suggestingWhenEmpty, inWindowIndexOffset, maxSuggestionSize, chatScreenSized, color);
        accessor = (CommandSuggestorAccessor) this;
        insightService = new CommandInsightService(client, accessor);
        startPos = ((MultiLineTextFieldWidget)accessor.getInput()).getCharacterPos(0);
    }

    @Override
    public void render(final GuiGraphics graphics, int mouseX, int mouseY) {
        if (accessor.getSuggestions() != null) {
            SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
            if (prepareSuggestionWindow(window)) {
                refreshRenderPos();
            }
            renderSuggestionBackground(graphics, window);
            if (shouldSuppressNativeTooltip(window, mouseX, mouseY)) {
                accessor.getSuggestions().render(graphics, Integer.MIN_VALUE, Integer.MIN_VALUE);
            } else {
                accessor.getSuggestions().render(graphics, mouseX, mouseY);
            }
            renderSuggestionIcons(graphics, window);
            renderSuggestionInsight(graphics, mouseX, mouseY);
        } else {
            if (insightService.findSyntaxHint(
                    accessor.getInput().getValue(),
                    accessor.getInput().getCursorPosition()
            ).isPresent() || hasSelectedPlaceholder()
                    || insightService.getDiagnostic(accessor.getInput().getValue()).isPresent()) {
                return;
            }
            MultiLineTextFieldWidget input = (MultiLineTextFieldWidget) accessor.getInput();
            int usageHeight = accessor.getCommandUsage().isEmpty()
                    ? 0
                    : (accessor.getCommandUsage().size() - 1) * 10 + 12;
            int usageY = EditorOverlayPlacement.placeVertical(
                    this.y,
                    this.y - accessor.getFont().lineHeight - 4,
                    usageHeight,
                    input.getEditorOverlayTop(),
                    input.getEditorOverlayBottom()
            );
            int minimumX = input.getX() + 4;
            int maximumX = Math.max(
                    minimumX,
                    input.getX() + input.getWidth() - accessor.getCommandUsageWidth() - 4
            );
            int usageX = Mth.clamp(this.x, minimumX, maximumX);
            int i = 0;
            for (FormattedCharSequence orderedText : accessor.getCommandUsage()) {
                int j = i * 10;
                graphics.fill(usageX - 1, j + usageY, usageX + accessor.getCommandUsageWidth() + 1, j + 12 + usageY, accessor.getFillColor());
                graphics.drawString(accessor.getFont(), orderedText, usageX, usageY + j + 2, -1);
                ++i;
            }
        }
    }

    private boolean shouldSuppressNativeTooltip(SuggestionWindowAccessor window, int mouseX, int mouseY) {
        if (window == null || !window.getRect().contains(mouseX, mouseY)) {
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

    private void renderSuggestionInsight(GuiGraphics graphics, int mouseX, int mouseY) {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || ((MultiLineTextFieldWidget) accessor.getInput()).usesDockedInsightPanel()) {
            return;
        }

        boolean hovered = window.getRect().contains(mouseX, mouseY);
        if (!hovered) {
            return;
        }
        int row = (mouseY - window.getRect().getY()) / 12 + window.getOffset();
        List<Suggestion> suggestions = window.getSuggestionList();
        if (row < 0 || row >= suggestions.size()) {
            return;
        }

        Suggestion suggestion = suggestions.get(row);
        MultiLineTextFieldWidget input = (MultiLineTextFieldWidget) accessor.getInput();
        int preferredX = window.getRect().getX() + window.getRect().getWidth() + 6;
        int preferredY = window.getRect().getY() + 2;
        if (suggestion.getTooltip() != null) {
            input.renderEditorTooltip(
                    graphics,
                    List.of(ComponentUtils.fromMessage(suggestion.getTooltip())),
                    preferredX,
                    preferredY,
                    mouseX,
                    mouseY
            );
            return;
        }

        insightService.describeSuggestion(accessor.getInput().getValue(), accessor.getInput().getCursorPosition(), suggestion.getText())
                .ifPresent(insight -> {
                    input.renderEditorTooltip(
                            graphics,
                            insight.toTooltipLines(),
                            preferredX,
                            preferredY,
                            mouseX,
                            mouseY
                    );
                });
    }

    @Override
    public void updateCommandInfo(){
        super.updateCommandInfo();
        this.refreshRenderPos();
        String command = accessor.getInput().getValue();
        int cursor = accessor.getInput().getCursorPosition();
        if (accessor.getInput().isFocused()
                && insightService.findStructuredSyntaxHint(command, cursor)
                .filter(hint -> !hint.suggestions().isEmpty())
                .isPresent()) {
            showSyntaxSuggestions(command, cursor);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        boolean handled = super.mouseClicked(mouseX, mouseY, mouseButton);
        if (handled && mouseButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            revealAcceptedSuggestion();
        }
        return handled;
    }

    private void revealAcceptedSuggestion() {
        MultiLineTextFieldWidget input = (MultiLineTextFieldWidget) accessor.getInput();
        input.revealCursor();
        refreshRenderPos();
    }

    public void refreshRenderPos(){
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if(window != null){
            prepareSuggestionWindow(window);
            MultiLineTextFieldWidget input = (MultiLineTextFieldWidget) accessor.getInput();
            int popupWidth = window.getRect().getWidth();
            int popupHeight = window.getRect().getHeight();
            int iconGutter = suggestionIconsVisible ? 13 : 0;
            int minX = input.getX() + 4 + iconGutter;
            int maxX = Math.max(minX, input.getX() + input.getWidth() - popupWidth - 4);
            int popupX = Math.max(minX, Math.min(this.x, maxX));
            int popupY = EditorOverlayPlacement.placeVertical(
                    this.y,
                    this.y - accessor.getFont().lineHeight - 4,
                    popupHeight,
                    input.getEditorOverlayTop(),
                    input.getEditorOverlayBottom()
            );
            Rect2i area = new Rect2i(popupX, popupY, popupWidth, popupHeight);
            window.setRect(area);
        }
    }

    public boolean mouseScrolledAt(double mouseX, double mouseY, double amount) {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || !window.getRect().contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        int maximumOffset = Math.max(window.getSuggestionList().size() - window.getRect().getHeight() / 12, 0);
        window.setOffset(Mth.clamp(window.getOffset() - (int) Math.signum(amount), 0, maximumOffset));
        return true;
    }

    public String getSelectedSuggestionKey() {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || window.getCurrent() < 0 || window.getCurrent() >= window.getSuggestionList().size()) {
            return "";
        }
        return window.getSuggestionList().get(window.getCurrent()).getText();
    }

    private boolean prepareSuggestionWindow(SuggestionWindowAccessor window) {
        if (window == null || preparedSuggestionWindow == window) {
            return false;
        }
        preparedSuggestionWindow = window;
        String command = accessor.getInput().getValue();
        int cursor = accessor.getInput().getCursorPosition();
        List<Suggestion> suggestions = new ArrayList<>(window.getSuggestionList());
        if (insightService.expectsPlayerSuggestions(command, cursor)) {
            suggestions.sort(Comparator.comparingInt(
                    suggestion -> CommandSuggestionVisual.playerRank(Minecraft.getInstance(), suggestion.getText())
            ));
            window.setSuggestionList(suggestions);
            window.invokeSelect(0);
        }

        Map<Suggestion, CommandSuggestionVisual> visuals = new HashMap<>();
        for (Suggestion suggestion : suggestions) {
            CommandSuggestionVisual.resolve(
                    Minecraft.getInstance(),
                    insightService,
                    command,
                    cursor,
                    suggestion.getText()
            ).ifPresent(visual -> visuals.put(suggestion, visual));
        }
        suggestionVisuals = Map.copyOf(visuals);
        suggestionIconsVisible = !suggestionVisuals.isEmpty();
        return true;
    }

    private void renderSuggestionBackground(GuiGraphics graphics, SuggestionWindowAccessor window) {
        Rect2i rect = window.getRect();
        int left = rect.getX() - (suggestionIconsVisible ? 13 : 0);
        graphics.fill(left - 1, rect.getY() - 1,
                rect.getX() + rect.getWidth() + 1, rect.getY() + rect.getHeight() + 1,
                SUGGESTION_BORDER);
        graphics.fill(left, rect.getY(),
                rect.getX() + rect.getWidth(), rect.getY() + rect.getHeight(),
                SUGGESTION_BACKGROUND);
    }

    private void renderSuggestionIcons(GuiGraphics graphics, SuggestionWindowAccessor window) {
        if (!suggestionIconsVisible) {
            return;
        }
        Rect2i rect = window.getRect();
        int visibleRows = Math.min(window.getSuggestionList().size() - window.getOffset(), rect.getHeight() / 12);
        for (int row = 0; row < visibleRows; row++) {
            Suggestion suggestion = window.getSuggestionList().get(window.getOffset() + row);
            CommandSuggestionVisual visual = suggestionVisuals.get(suggestion);
            if (visual == null) {
                continue;
            }
            int iconX = rect.getX() - 12;
            int iconY = rect.getY() + row * 12 + 1;
            visual.renderIcon(graphics, iconX, iconY, 10);
        }
    }

    public Optional<ParticlePreview> getSelectedParticlePreview() {
        SuggestionWindowAccessor window = (SuggestionWindowAccessor) accessor.getSuggestions();
        if (window == null || window.getCurrent() < 0 || window.getCurrent() >= window.getSuggestionList().size()) {
            return Optional.empty();
        }
        CommandSuggestionVisual visual = suggestionVisuals.get(window.getSuggestionList().get(window.getCurrent()));
        if (visual == null || visual.particleId() == null) {
            return Optional.empty();
        }
        return Optional.of(new ParticlePreview(visual.particleId(), visual.particleSprite()));
    }

    public static void renderFallbackParticle(GuiGraphics graphics, ResourceLocation id, int x, int y, int size) {
        CommandSuggestionVisual.renderFallbackParticle(graphics, id, x, y, size);
    }

    public record ParticlePreview(ResourceLocation id, TextureAtlasSprite sprite) {
    }

    public Style getColor(int colorIndex){
        return HIGHLIGHT_STYLES.get(colorIndex);
    }

    /**
     * @return List of (colorIndex, startIndex)-Pairs.
     * Color Indices:
     *  0   - Error
     *  1   - Info
     *  2-7 - Highlight
     */
    public List<Pair<Integer,Integer>> getColors(String original, int firstCharacterIndex) {
        if(accessor.getCurrentParse() == null){
            return new ArrayList<Pair<Integer,Integer>>();
        }
        ParseResults<SharedSuggestionProvider> parse = accessor.getCurrentParse();

        int m;
        ArrayList<Pair<Integer,Integer>> list = Lists.newArrayList();
        list.add(new Pair<>(1,0));
        int colorIndex = -1;
        CommandContextBuilder<SharedSuggestionProvider> commandContextBuilder = parse.getContext();
        do {
            for (ParsedArgument<SharedSuggestionProvider, ?> parsedArgument : commandContextBuilder.getArguments().values()) {
                int k;
                colorIndex = bumpColorIndex(colorIndex);
                if ((k = Math.max(parsedArgument.getRange().getStart() - firstCharacterIndex, 0)) >= original.length())
                    break;
                int l = Math.min(parsedArgument.getRange().getEnd() - firstCharacterIndex, original.length());
                if (l <= 0) continue;
                list.add(new Pair<>(colorIndex + 2, k));
                list.add(new Pair<>(1, l));
            }
            commandContextBuilder = commandContextBuilder.getChild();
        } while (commandContextBuilder != null);


        boolean incompleteComposite = insightService.findSyntaxHint(
                accessor.getInput().getValue(),
                accessor.getInput().getCursorPosition()
        ).isPresent() || hasSelectedPlaceholder();
        if (!incompleteComposite && parse.getReader().canRead()
                && (m = Math.max(parse.getReader().getCursor() - firstCharacterIndex, 0)) < original.length()) {
            int n = Math.min(m + parse.getReader().getRemainingLength(), original.length());
            list.add(new Pair<>(0, m));
        }
        return list;
    }

    public int getY(){
        return y;
    }

    public int getX(){
        return x;
    }

    public void setPos(int x, int y){
        this.x = x;
        this.y = y;
    }

    public int getHighlighColorCount(){
        return HIGHLIGHT_STYLES.size() - 2;
    }

    public CommandInsightService getInsightService() {
        return insightService;
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

    public void showSuggestionsAtCursor() {
        updateCommandInfo();
        String command = accessor.getInput().getValue();
        int cursor = accessor.getInput().getCursorPosition();
        if (showSyntaxSuggestions(command, cursor)) {
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
            return;
        }
        pendingSuggestions.thenRun(() -> Minecraft.getInstance().execute(() -> {
            if (accessor.getInput().getValue().equals(command)
                    && accessor.getInput().getCursorPosition() == cursor) {
                showSuggestions(true);
            }
        }));
    }

    private boolean showSyntaxSuggestions(String command, int cursor) {
        Optional<com.miofeather.commandblockstudio.main.insight.CommandSyntaxHint> hint =
                insightService.findSyntaxHint(command, cursor);
        if (hint.isEmpty() || hint.get().suggestions().isEmpty()) {
            return false;
        }

        StringRange range = StringRange.between(hint.get().replacementStart(), cursor);
        List<Suggestion> candidates = hint.get().suggestions().stream()
                .map(value -> new Suggestion(range, value))
                .toList();
        accessor.setPendingSuggestions(CompletableFuture.completedFuture(new Suggestions(range, candidates)));
        showSuggestions(true);
        refreshRenderPos();
        return true;
    }

    private boolean hasSelectedPlaceholder() {
        return insightService.describePlaceholder(accessor.getInput().getHighlighted()).isPresent();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (accessor.getSuggestions() != null
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            ((SuggestionWindowAccessor) accessor.getSuggestions()).invokeUseSuggestion();
            hide();
            accessor.getInput().setSuggestion(null);
            revealAcceptedSuggestion();
            return true;
        }
        boolean acceptingSuggestion = accessor.getSuggestions() != null && keyCode == GLFW.GLFW_KEY_TAB;
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled && acceptingSuggestion) {
            revealAcceptedSuggestion();
        }
        return handled;
    }

    private int bumpColorIndex(int colorIndex){
        return (colorIndex + 1) % getHighlighColorCount();
    }
}
