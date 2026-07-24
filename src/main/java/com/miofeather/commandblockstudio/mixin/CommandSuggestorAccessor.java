package com.miofeather.commandblockstudio.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public interface CommandSuggestorAccessor{
    @Invoker("formatChat")
    FormattedCharSequence invokeFormatChat(String original, int firstCharacterIndex);

    @Accessor
    CommandSuggestions.SuggestionsList getSuggestions();

    @Accessor
    List<FormattedCharSequence> getCommandUsage();


    @Accessor
    Screen getScreen();

    @Accessor
    Font getFont();

    @Accessor
    EditBox getInput();

    @Accessor
    ParseResults<ClientSuggestionProvider> getCurrentParse();

    @Accessor
    CompletableFuture<Suggestions> getPendingSuggestions();

    @Accessor
    void setPendingSuggestions(CompletableFuture<Suggestions> pendingSuggestions);

    @Accessor
    boolean getAnchorToBottom();

    @Accessor
    boolean getKeepSuggestions();

    @Accessor
    int getFillColor();

    @Accessor
    int getCommandUsageWidth();

    @Accessor
    int getCommandUsagePosition();
}
