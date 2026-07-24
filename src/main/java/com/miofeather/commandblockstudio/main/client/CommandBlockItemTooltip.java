package com.miofeather.commandblockstudio.main.client;

import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@OnlyIn(Dist.CLIENT)
public final class CommandBlockItemTooltip {
    private static final int COMMAND_PREVIEW_LENGTH = 120;
    private static final DateTimeFormatter EDIT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @SubscribeEvent
    public void appendTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!isCommandBlock(stack)) {
            return;
        }
        CompoundTag stackTag = stack.getTag();
        if (stackTag == null || !stackTag.contains("BlockEntityTag", Tag.TAG_COMPOUND)) {
            return;
        }

        CompoundTag tag = stackTag.getCompound("BlockEntityTag");
        String command = normalizeCommand(tag.getString("Command"));
        if (!command.isBlank()) {
            event.getToolTip().add(Component.empty());
            event.getToolTip().add(Component.translatable(
                    "cbs.itemTooltip.command",
                    Component.literal(shorten(command)).withStyle(ChatFormatting.AQUA)
            ).withStyle(ChatFormatting.GRAY));
        }

        CommandBlockAnnotationNetwork.latestEditFromBlockEntityData(tag).ifPresentOrElse(
                latest -> {
                    String time = latest.timestamp() > 0L
                            ? EDIT_TIME_FORMAT.format(Instant.ofEpochMilli(latest.timestamp()))
                            : Component.translatable("cbs.itemTooltip.unknown").getString();
                    String editor = latest.editor().isBlank()
                            ? Component.translatable("cbs.itemTooltip.unknown").getString()
                            : latest.editor();
                    event.getToolTip().add(Component.translatable(
                            "cbs.itemTooltip.lastEdit",
                            time,
                            editor
                    ).withStyle(ChatFormatting.DARK_GRAY));
                },
                () -> {
                    if (!command.isBlank()) {
                        event.getToolTip().add(Component.translatable("cbs.itemTooltip.noHistory")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
        );
    }

    private static boolean isCommandBlock(ItemStack stack) {
        return stack.is(Items.COMMAND_BLOCK)
                || stack.is(Items.CHAIN_COMMAND_BLOCK)
                || stack.is(Items.REPEATING_COMMAND_BLOCK);
    }

    private static String normalizeCommand(String command) {
        return command.replace('\r', ' ').replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String shorten(String command) {
        if (command.length() <= COMMAND_PREVIEW_LENGTH) {
            return command;
        }
        return command.substring(0, COMMAND_PREVIEW_LENGTH - 1) + "…";
    }
}
