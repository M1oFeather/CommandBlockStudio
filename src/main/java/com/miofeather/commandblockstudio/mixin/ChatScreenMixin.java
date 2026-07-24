package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.ui.ChatCommandAssistantPanel;
import com.miofeather.commandblockstudio.main.ui.ChatCommandSuggestor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    @Shadow
    protected EditBox input;

    @Shadow
    CommandSuggestions commandSuggestions;

    @Unique
    private ChatCommandSuggestor commandBlockStudio$chatSuggestor;

    @Unique
    private ChatCommandAssistantPanel commandBlockStudio$assistantPanel;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void commandBlockStudio$installChatAssistant(CallbackInfo callbackInfo) {
        if (minecraft == null) {
            return;
        }
        commandBlockStudio$chatSuggestor = new ChatCommandSuggestor(minecraft, this, input, font);
        commandBlockStudio$chatSuggestor.setAllowSuggestions(true);
        commandBlockStudio$chatSuggestor.updateCommandInfo();
        commandSuggestions = commandBlockStudio$chatSuggestor;
        commandBlockStudio$assistantPanel = new ChatCommandAssistantPanel(
                font,
                input,
                commandBlockStudio$chatSuggestor
        );
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
            )
    )
    private void commandBlockStudio$renderChatAssistant(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        if (commandBlockStudio$assistantPanel == null) {
            return;
        }
        int panelWidth = Math.min(280, Math.max(150, width / 4));
        int panelX = width - panelWidth - 6;
        int panelHeight = Math.max(90, height - 30);
        commandBlockStudio$assistantPanel.setBounds(panelX, 6, panelWidth, panelHeight);
        commandBlockStudio$assistantPanel.render(graphics, mouseX, mouseY, partialTick);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void commandBlockStudio$completeWithControlSpace(
            int keyCode,
            int scanCode,
            int modifiers,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (commandBlockStudio$chatSuggestor != null
                && commandBlockStudio$chatSuggestor.isCommandMode()
                && keyCode == GLFW.GLFW_KEY_SPACE
                && hasControlDown()) {
            commandBlockStudio$chatSuggestor.showSuggestionsAtCursor();
            callbackInfo.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void commandBlockStudio$scrollAssistant(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (commandBlockStudio$assistantPanel != null
                && commandBlockStudio$assistantPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
