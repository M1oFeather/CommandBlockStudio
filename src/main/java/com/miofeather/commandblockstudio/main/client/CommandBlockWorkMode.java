package com.miofeather.commandblockstudio.main.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class CommandBlockWorkMode {
    private static final long REFRESH_INTERVAL_MILLIS = 1_000L;
    private static final int PANEL_TEXT_WIDTH = 210;
    private static final int MAX_COMMAND_LINES = 8;
    private static final float HOLOGRAM_SCALE = 0.014F;
    private static final DateTimeFormatter EDIT_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private static boolean enabled;
    private static boolean shortcutLatched;

    private BlockPos target;
    private Direction targetFace = Direction.NORTH;
    private CommandBlockAnnotationNetwork.CommandPreviewSnapshot preview;
    private boolean unavailable;
    private long nextRefreshAt;

    public static boolean handleDebugShortcut() {
        if (!shortcutLatched) {
            shortcutLatched = true;
            enabled = !enabled;
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(Component.translatable(
                        enabled ? "cbs.workMode.enabled" : "cbs.workMode.disabled"
                ).withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
            }
        }
        return true;
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (event.getKey() == GLFW.GLFW_KEY_F4 && event.getAction() == GLFW.GLFW_RELEASE) {
            shortcutLatched = false;
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (!enabled || client.player == null || client.level == null || client.screen != null) {
            clearTarget();
            return;
        }

        TargetedCommandBlock pointed = pointedCommandBlock(client).orElse(null);
        if (pointed == null) {
            clearTarget();
            return;
        }
        targetFace = pointed.face();
        if (!pointed.position().equals(target)) {
            target = pointed.position().immutable();
            preview = null;
            unavailable = false;
            nextRefreshAt = 0L;
            CommandBlockAnnotationNetwork.clearReceivedPreview(target);
        }

        CommandBlockAnnotationNetwork.takeReceivedPreview(target).ifPresent(snapshot -> {
            preview = snapshot.available() ? snapshot : null;
            unavailable = !snapshot.available();
        });

        long now = Util.getMillis();
        if (now < nextRefreshAt) {
            return;
        }
        nextRefreshAt = now + REFRESH_INTERVAL_MILLIS;
        if (client.getConnection() != null
                && client.getConnection().hasChannel(CommandBlockAnnotationNetwork.RequestCommandPreview.TYPE)) {
            client.getConnection().send(new CommandBlockAnnotationNetwork.RequestCommandPreview(target));
            return;
        }

        preview = localPreview(client, target).orElse(null);
        unavailable = preview == null;
    }

    @SubscribeEvent
    public void renderWorldProjection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (!enabled || target == null || client.screen != null || client.options.hideGui) {
            return;
        }

        Font font = client.font;
        List<ProjectedLine> lines = buildProjectionLines(font);
        int panelWidth = Math.max(116, lines.stream().mapToInt(line -> font.width(line.text())).max().orElse(100) + 16);
        int panelHeight = lines.size() * 10 + 12;
        float left = -panelWidth / 2.0F;
        float top = -panelHeight / 2.0F;

        Vec3 cameraPosition = event.getCamera().getPosition();
        Vec3 faceOffset = Vec3.atLowerCornerOf(targetFace.getNormal()).scale(0.68D);
        Vec3 anchor = Vec3.atCenterOf(target).add(faceOffset).add(0.0D, 1.05D, 0.0D);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(anchor.x - cameraPosition.x, anchor.y - cameraPosition.y, anchor.z - cameraPosition.z);
        pose.mulPose(event.getCamera().rotation());
        pose.scale(HOLOGRAM_SCALE, -HOLOGRAM_SCALE, HOLOGRAM_SCALE);

        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();
        VertexConsumer background = buffers.getBuffer(RenderType.textBackgroundSeeThrough());
        int accent = accentColor(preview);
        addPanelQuad(background, matrix, left, top, left + panelWidth, top + panelHeight, 0xD91A1F25);
        addPanelQuad(background, matrix, left, top, left + 3.0F, top + panelHeight, accent);
        addPanelQuad(background, matrix, left, top, left + panelWidth, top + 1.0F, accent);
        addPanelQuad(background, matrix, left, top + panelHeight - 1.0F, left + panelWidth, top + panelHeight, accent);
        addPanelQuad(background, matrix, left + panelWidth - 1.0F, top, left + panelWidth, top + panelHeight, accent);

        float lineY = top + 7.0F;
        for (ProjectedLine line : lines) {
            font.drawInBatch(
                    line.text(),
                    left + 8.0F,
                    lineY,
                    line.color(),
                    false,
                    matrix,
                    buffers,
                    Font.DisplayMode.SEE_THROUGH,
                    0,
                    LightTexture.FULL_BRIGHT
            );
            lineY += 10.0F;
        }
        buffers.endBatch();
        pose.popPose();
    }

    private List<ProjectedLine> buildProjectionLines(Font font) {
        List<ProjectedLine> lines = new ArrayList<>();
        lines.add(new ProjectedLine(
                Component.translatable("cbs.workMode.title").getVisualOrderText(),
                0xFFF2F4F7
        ));
        lines.add(new ProjectedLine(
                Component.literal(target.getX() + ", " + target.getY() + ", " + target.getZ()).getVisualOrderText(),
                0xFF8B949E
        ));

        if (preview == null) {
            Component status = Component.translatable(unavailable
                    ? "cbs.workMode.unavailable"
                    : "cbs.workMode.loading");
            lines.add(new ProjectedLine(status.getVisualOrderText(), unavailable ? 0xFFE3B341 : 0xFF8ED8E6));
            return lines;
        }

        Component command = Component.literal(preview.command().isBlank() ? "<empty>" : preview.command())
                .withStyle(preview.command().isBlank() ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA);
        List<FormattedCharSequence> commandLines = font.split(command, PANEL_TEXT_WIDTH);
        int visibleLines = Math.min(MAX_COMMAND_LINES, commandLines.size());
        for (int i = 0; i < visibleLines; i++) {
            lines.add(new ProjectedLine(commandLines.get(i), 0xFFE6EDF3));
        }
        if (commandLines.size() > visibleLines) {
            lines.add(new ProjectedLine(Component.literal("...").getVisualOrderText(), 0xFF8B949E));
        }

        Component mode = Component.translatable(modeKey(preview.mode()))
                .append(Component.literal(preview.conditional() ? " · C" : ""))
                .append(Component.literal(preview.automatic() ? " · AUTO" : ""));
        lines.add(new ProjectedLine(mode.getVisualOrderText(), 0xFF8B949E));
        if (preview.timestamp() > 0L) {
            String editor = preview.editor().isBlank() ? "?" : preview.editor();
            Component edited = Component.translatable(
                    "cbs.workMode.lastEdit",
                    EDIT_TIME_FORMAT.format(Instant.ofEpochMilli(preview.timestamp())),
                    editor
            );
            lines.add(new ProjectedLine(edited.getVisualOrderText(), 0xFF707A85));
        }
        return lines;
    }

    private static void addPanelQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float left,
            float top,
            float right,
            float bottom,
            int color
    ) {
        consumer.addVertex(matrix, left, bottom, 0.01F).setColor(color).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, right, bottom, 0.01F).setColor(color).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, right, top, 0.01F).setColor(color).setLight(LightTexture.FULL_BRIGHT);
        consumer.addVertex(matrix, left, top, 0.01F).setColor(color).setLight(LightTexture.FULL_BRIGHT);
    }

    private static Optional<TargetedCommandBlock> pointedCommandBlock(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        BlockPos pos = hit.getBlockPos();
        return client.level.getBlockState(pos).is(Blocks.COMMAND_BLOCK)
                || client.level.getBlockState(pos).is(Blocks.CHAIN_COMMAND_BLOCK)
                || client.level.getBlockState(pos).is(Blocks.REPEATING_COMMAND_BLOCK)
                ? Optional.of(new TargetedCommandBlock(pos, hit.getDirection()))
                : Optional.empty();
    }

    private static Optional<CommandBlockAnnotationNetwork.CommandPreviewSnapshot> localPreview(
            Minecraft client,
            BlockPos pos
    ) {
        BlockEntity blockEntity = client.level.getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity commandBlock)
                || commandBlock.getCommandBlock().getCommand().isBlank()) {
            return Optional.empty();
        }
        CommandBlockAnnotationNetwork.LatestEditInfo latest = CommandBlockAnnotationNetwork
                .latestEditFromBlockEntityData(commandBlock.saveWithoutMetadata(client.level.registryAccess()))
                .orElse(new CommandBlockAnnotationNetwork.LatestEditInfo(0L, ""));
        return Optional.of(new CommandBlockAnnotationNetwork.CommandPreviewSnapshot(
                true,
                commandBlock.getCommandBlock().getCommand(),
                commandBlock.getMode(),
                commandBlock.isConditional(),
                commandBlock.isAutomatic(),
                latest.timestamp(),
                latest.editor()
        ));
    }

    private void clearTarget() {
        if (target != null) {
            CommandBlockAnnotationNetwork.clearReceivedPreview(target);
        }
        target = null;
        targetFace = Direction.NORTH;
        preview = null;
        unavailable = false;
        nextRefreshAt = 0L;
    }

    private static int accentColor(CommandBlockAnnotationNetwork.CommandPreviewSnapshot snapshot) {
        if (snapshot == null) {
            return 0xFF4FC3D7;
        }
        return switch (snapshot.mode()) {
            case REDSTONE -> 0xFFE89036;
            case SEQUENCE -> 0xFF58C6B7;
            case AUTO -> 0xFF8367C7;
        };
    }

    private static String modeKey(CommandBlockEntity.Mode mode) {
        return switch (mode) {
            case REDSTONE -> "advMode.mode.redstone";
            case SEQUENCE -> "advMode.mode.sequence";
            case AUTO -> "advMode.mode.auto";
        };
    }

    private record TargetedCommandBlock(BlockPos position, Direction face) {
    }

    private record ProjectedLine(FormattedCharSequence text, int color) {
    }
}
