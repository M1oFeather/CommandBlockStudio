package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.client.CommandBlockWorkMode;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyboardHandler.class)
public final class KeyboardHandlerMixin {
    @Inject(method = "handleDebugKeys", at = @At("HEAD"), cancellable = true)
    private void commandBlockStudio$toggleWorkMode(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (event.key() == GLFW.GLFW_KEY_F4) {
            callback.setReturnValue(CommandBlockWorkMode.handleDebugShortcut());
        }
    }
}
