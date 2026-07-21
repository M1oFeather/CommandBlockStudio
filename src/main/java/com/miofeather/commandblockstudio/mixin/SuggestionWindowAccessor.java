package com.miofeather.commandblockstudio.mixin;

import com.mojang.brigadier.suggestion.Suggestion;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(CommandSuggestions.SuggestionsList.class)
public interface SuggestionWindowAccessor {
    @Accessor @Mutable
    void setRect(Rect2i area);

    @Accessor
    Rect2i getRect();

    @Accessor
    List<Suggestion> getSuggestionList();

    @Accessor @Mutable
    void setSuggestionList(List<Suggestion> suggestions);

    @Accessor
    int getOffset();

    @Accessor
    void setOffset(int offset);

    @Accessor
    int getCurrent();

    @Invoker("useSuggestion")
    void invokeUseSuggestion();

    @Invoker("select")
    void invokeSelect(int index);

}
