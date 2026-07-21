package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Unique
    private CommandBlockAnnotationNetwork.CommandVersion commandBlockStudio$previousVersion;

    @Inject(method = "handleSetCommandBlock", at = @At("HEAD"))
    private void commandBlockStudio$capturePreviousVersion(
            ServerboundSetCommandBlockPacket packet,
            CallbackInfo callbackInfo
    ) {
        commandBlockStudio$previousVersion = CommandBlockAnnotationNetwork.captureCommandVersion(player, packet.getPos());
    }

    @Inject(method = "handleSetCommandBlock", at = @At("TAIL"))
    private void commandBlockStudio$recordEdit(ServerboundSetCommandBlockPacket packet, CallbackInfo callbackInfo) {
        CommandBlockAnnotationNetwork.CommandVersion submittedVersion = new CommandBlockAnnotationNetwork.CommandVersion(
                packet.getCommand(),
                packet.getMode(),
                packet.isTrackOutput(),
                packet.isConditional(),
                packet.isAutomatic()
        );
        CommandBlockAnnotationNetwork.recordCommandEdit(
                player,
                packet.getPos(),
                commandBlockStudio$previousVersion,
                submittedVersion
        );
        commandBlockStudio$previousVersion = null;
    }
}
