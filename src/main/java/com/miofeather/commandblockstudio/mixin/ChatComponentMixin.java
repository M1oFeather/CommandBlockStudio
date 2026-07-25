package com.miofeather.commandblockstudio.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void commandBlockStudio$hideHistoryBehindCommandEditor(CallbackInfo callbackInfo) {
        if (Minecraft.getInstance().screen instanceof ChatScreen chatScreen
                && ((ChatScreenAccessor) chatScreen).commandBlockStudio$getInput().getValue().startsWith("/")) {
            callbackInfo.cancel();
        }
    }
}
