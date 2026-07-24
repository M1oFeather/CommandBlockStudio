package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.ui.screen.CommandBlockStudioMinecartScreen;
import com.miofeather.commandblockstudio.mixin.LocalPlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecartCommandBlock.class)
public class CommandBlockMinecartMixin {
    @Redirect(
            method = "interact",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;openMinecartCommandBlock(Lnet/minecraft/world/entity/vehicle/minecart/MinecartCommandBlock;)V"
            )
    )
    public void openCommandBlockStudioMinecartScreen(Player player, MinecartCommandBlock minecart) {
        if(player instanceof LocalPlayer){
            Minecraft client = ((LocalPlayerAccessor)player).getMinecraft();
            client.gui.setScreen(new CommandBlockStudioMinecartScreen(client, minecart));
            return;
        }
        player.openMinecartCommandBlock(minecart);
    }
}
