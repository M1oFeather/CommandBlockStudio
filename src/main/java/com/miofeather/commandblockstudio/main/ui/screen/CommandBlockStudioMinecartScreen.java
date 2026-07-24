package com.miofeather.commandblockstudio.main.ui.screen;

import com.miofeather.commandblockstudio.main.ui.MultiLineTextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BaseCommandBlock;

import java.util.Objects;

public class CommandBlockStudioMinecartScreen extends AbstractCommandBlockStudioScreen{

    private final MinecartCommandBlock minecart;

    public CommandBlockStudioMinecartScreen(Minecraft client, MinecartCommandBlock minecart){
        this.commandExecutor = minecart.getCommandBlock();
        this.minecart = minecart;
        updated = true;
    }

    public void init(){
        super.init();
        ((MultiLineTextFieldWidget) consoleCommandTextField).setRawText(commandExecutor.getCommand());
    }

    @Override
    protected Component getTargetDescription() {
        return Component.translatable("cbs.target.minecart", minecart.getId());
    }

    @Override
    protected void syncSettingsToServer(BaseCommandBlock commandExecutor) {
        var connection = Objects.requireNonNull(
                Objects.requireNonNull(this.minecraft, "Minecart command screen is not attached to Minecraft").getConnection(),
                "No client connection while saving command block minecart");
        connection.send(new ServerboundSetCommandMinecartPacket(
                minecart.getId(),
                this.consoleCommandTextField.getValue(),
                this.trackOutput));
    }
}
