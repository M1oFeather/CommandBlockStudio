package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.ui.screen.CommandBlockStudioScreen;
import com.miofeather.commandblockstudio.mixin.ClientCommonPacketListenerImplAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method= "handleBlockEntityData(Lnet/minecraft/network/protocol/game/ClientboundBlockEntityDataPacket;)V",
    at=@At("TAIL"))
    public void blockEntityUpdateInject(ClientboundBlockEntityDataPacket packet, CallbackInfo ci){
        BlockPos blockPos = packet.getPos();
        Minecraft client = ((ClientCommonPacketListenerImplAccessor)(Object)this).getMinecraft();
        if (client.gui.screen() instanceof CommandBlockStudioScreen commandScreen && commandScreen.isEditing(blockPos)) {
            commandScreen.updateCommandBlock();
        }
    }
}
