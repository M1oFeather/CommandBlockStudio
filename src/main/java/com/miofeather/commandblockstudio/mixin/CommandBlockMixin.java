package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.ui.screen.CommandBlockStudioScreen;
import com.miofeather.commandblockstudio.mixin.LocalPlayerAccessor;
import com.miofeather.commandblockstudio.mixin.ServerPlayerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CommandBlock.class)
public class CommandBlockMixin {

    @Redirect(method= "useWithoutItem",
    at=@At(value="INVOKE", target= "Lnet/minecraft/world/entity/player/Player;openCommandBlock(Lnet/minecraft/world/level/block/entity/CommandBlockEntity;)V"))
    public void openCommandBlockStudioScreen(Player instance, CommandBlockEntity commandBlock){
        if(instance instanceof LocalPlayer){
            Minecraft client = ((LocalPlayerAccessor)instance).getMinecraft();
            BlockPos workspaceRoot = client.gui.screen() instanceof CommandBlockStudioScreen studioScreen
                    ? studioScreen.getWorkspaceRoot()
                    : commandBlock.getBlockPos();
            client.gui.setScreen(new CommandBlockStudioScreen(
                    client,
                    commandBlock,
                    commandBlock.getCommandBlock(),
                    workspaceRoot
            ));
        } else if (instance instanceof ServerPlayer){
            ((ServerPlayerAccessor)instance).getConnection().send(ClientboundBlockEntityDataPacket.create(commandBlock, BlockEntity::saveWithoutMetadata));
        }
    }
}
