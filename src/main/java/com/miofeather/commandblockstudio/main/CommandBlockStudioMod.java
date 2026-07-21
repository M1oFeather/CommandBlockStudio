package com.miofeather.commandblockstudio.main;

import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CommandBlockStudioMod.MODID)
public final class CommandBlockStudioMod {
    public static final String MODID = "command_block_studio";

    public CommandBlockStudioMod(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(CommandBlockAnnotationNetwork::registerPayloads);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            new CommandBlockStudio(modEventBus, modContainer);
        }
    }
}
