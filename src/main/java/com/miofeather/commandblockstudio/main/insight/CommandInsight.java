package com.miofeather.commandblockstudio.main.insight;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record CommandInsight(String title, String summary, List<String> examples, boolean documented) {
    public static CommandInsight undocumented(String title, String summary) {
        return new CommandInsight(title, summary, List.of(), false);
    }

    public List<Component> toTooltipLines() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(title).withStyle(documented ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        if (!summary.isBlank()) {
            lines.add(Component.literal(summary).withStyle(ChatFormatting.WHITE));
        }
        if (!examples.isEmpty()) {
            lines.add(Component.literal(CommandBlockStudio.useChineseCommandInsight() ? "示例" : "Examples").withStyle(ChatFormatting.GOLD));
            for (String example : examples) {
                lines.add(Component.literal("  " + example).withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }
}
