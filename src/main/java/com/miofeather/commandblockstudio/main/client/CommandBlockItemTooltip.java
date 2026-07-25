package com.miofeather.commandblockstudio.main.client;

import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public final class CommandBlockItemTooltip {
    private static final int COMMAND_PREVIEW_LENGTH = 120;
    private static final int ANNOTATION_PREVIEW_LENGTH = 160;
    private static final int TOOLTIP_TEXT_WIDTH = 250;
    private static final int MAX_COMMAND_LINES = 8;
    private static final int MAX_ANNOTATION_LINES = 3;
    private static final DateTimeFormatter EDIT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @SubscribeEvent
    public void appendTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (CommandBlockScanner.isScanner(stack)) {
            event.getToolTip().add(Component.translatable("cbs.scanner.tooltip")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("cbs.scanner.keyHint")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (!isCommandBlock(stack)) {
            return;
        }
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || blockEntityData.isEmpty()) {
            return;
        }

        CompoundTag tag = blockEntityData.copyTag();
        String command = normalizeCommand(tag.getString("Command"));
        String annotation = normalizeAnnotation(CommandBlockAnnotationNetwork.annotationFromBlockEntityData(tag));
        Font font = Minecraft.getInstance().font;
        if (!annotation.isBlank()) {
            appendAnnotation(event.getToolTip(), font, annotation);
        }
        if (!command.isBlank()) {
            event.getToolTip().add(Component.empty());
            appendCommand(event.getToolTip(), font, command);
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

    private static String normalizeAnnotation(String annotation) {
        return annotation.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private static void appendAnnotation(List<Component> tooltip, Font font, String annotation) {
        Component label = Component.translatable("cbs.annotation.title")
                .append(Component.literal(": "));
        int bodyWidth = Math.max(16, TOOLTIP_TEXT_WIDTH - font.width(label));
        List<String> lines = CommandProjectionFormatter.wrapPlain(
                shorten(annotation, ANNOTATION_PREVIEW_LENGTH),
                font,
                bodyWidth
        );
        int visibleLines = Math.min(MAX_ANNOTATION_LINES, lines.size());
        for (int i = 0; i < visibleLines; i++) {
            Component body = Component.literal(lines.get(i)).withStyle(ChatFormatting.YELLOW);
            tooltip.add(i == 0
                    ? label.copy().withStyle(ChatFormatting.GRAY).append(body)
                    : body);
        }
        if (lines.size() > visibleLines) {
            tooltip.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void appendCommand(List<Component> tooltip, Font font, String command) {
        Component label = Component.translatable("cbs.itemTooltip.command", Component.empty());
        int bodyWidth = Math.max(16, TOOLTIP_TEXT_WIDTH - font.width(label));
        List<String> lines = CommandProjectionFormatter.formatAndWrap(
                shorten(command, COMMAND_PREVIEW_LENGTH),
                font,
                bodyWidth
        );
        int visibleLines = Math.min(MAX_COMMAND_LINES, lines.size());
        for (int i = 0; i < visibleLines; i++) {
            Component body = Component.literal(lines.get(i)).withStyle(ChatFormatting.AQUA);
            tooltip.add(i == 0
                    ? Component.translatable("cbs.itemTooltip.command", body).withStyle(ChatFormatting.GRAY)
                    : body);
        }
        if (lines.size() > visibleLines) {
            tooltip.add(Component.literal("...").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String shorten(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + "…";
    }
}
