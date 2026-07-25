package com.miofeather.commandblockstudio.main.client;

import com.miofeather.commandblockstudio.main.CommandBlockStudioMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;

public final class CommandBlockScanner {
    private static final String SCANNER_KEY = CommandBlockStudioMod.MODID + ":scanner";

    private CommandBlockScanner() {
    }

    public static ItemStack createStack() {
        ItemStack stack = new ItemStack(Items.SPYGLASS);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(SCANNER_KEY, true);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        stack.set(DataComponents.RARITY, Rarity.UNCOMMON);
        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.translatable("cbs.scanner.name").withStyle(ChatFormatting.AQUA)
        );
        return stack;
    }

    public static boolean isScanner(ItemStack stack) {
        if (!stack.is(Items.SPYGLASS)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(SCANNER_KEY);
    }

    public static boolean isHeld(Player player) {
        return isScanner(player.getMainHandItem()) || isScanner(player.getOffhandItem());
    }
}
