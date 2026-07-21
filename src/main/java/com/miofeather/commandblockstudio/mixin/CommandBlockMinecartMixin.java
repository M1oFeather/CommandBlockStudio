package com.miofeather.commandblockstudio.mixin;

import com.miofeather.commandblockstudio.main.ui.screen.CommandBlockStudioMinecartScreen;
import com.miofeather.commandblockstudio.mixin.LocalPlayerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecartCommandBlock.class)
public class CommandBlockMinecartMixin {
    @Redirect(method= "interact",
            at=@At(value="INVOKE", target= "Lnet/minecraft/world/level/BaseCommandBlock;usedBy(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/InteractionResult;"))
    public InteractionResult openCommandBlockStudioMinecartScreen(BaseCommandBlock commandBlock, Player player){
        if(player instanceof LocalPlayer){
            Minecraft client = ((LocalPlayerAccessor)player).getMinecraft();
            client.setScreen(new CommandBlockStudioMinecartScreen(client, (MinecartCommandBlock)(Object)this));
            return InteractionResult.sidedSuccess(true);
        }
        return commandBlock.usedBy(player);
    }
}
