package com.miofeather.commandblockstudio.main.network;

import com.miofeather.commandblockstudio.main.CommandBlockStudioMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandBlockAnnotationNetwork {
    public static final int MAX_ANNOTATION_LENGTH = 2048;
    public static final int MAX_HISTORY_ENTRIES = 10;
    public static final int MAX_COMMAND_LENGTH = 32500;
    private static final int MAX_EDITOR_NAME_LENGTH = 64;
    private static final String DATA_KEY = CommandBlockStudioMod.MODID + ":annotation";
    private static final String HISTORY_KEY = CommandBlockStudioMod.MODID + ":edit_history";
    private static final String HISTORY_LAST_COMMAND_KEY = CommandBlockStudioMod.MODID + ":history_last_command";
    private static final String NEOFORGE_DATA_KEY = "NeoForgeData";
    private static final double MAX_EDIT_DISTANCE_SQUARED = 64.0D * 64.0D;
    private static final Map<BlockPos, AnnotationSnapshot> RECEIVED_ANNOTATIONS = new ConcurrentHashMap<>();
    private static final Map<BlockPos, CommandPreviewSnapshot> RECEIVED_PREVIEWS = new ConcurrentHashMap<>();

    private CommandBlockAnnotationNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Keep every Studio payload optional so client-only and server-only installations can connect.
        PayloadRegistrar registrar = event.registrar("2").optional();
        registrar.playToServer(RequestAnnotation.TYPE, RequestAnnotation.STREAM_CODEC, CommandBlockAnnotationNetwork::handleRequest);
        registrar.playToServer(UpdateAnnotation.TYPE, UpdateAnnotation.STREAM_CODEC, CommandBlockAnnotationNetwork::handleUpdate);
        registrar.playToServer(RequestOpenCommandBlock.TYPE, RequestOpenCommandBlock.STREAM_CODEC, CommandBlockAnnotationNetwork::handleOpen);
        registrar.playToServer(RequestCommandPreview.TYPE, RequestCommandPreview.STREAM_CODEC, CommandBlockAnnotationNetwork::handlePreviewRequest);
        registrar.playToServer(RunCommandBlock.TYPE, RunCommandBlock.STREAM_CODEC, CommandBlockAnnotationNetwork::handleRun);
        registrar.playToClient(SyncAnnotation.TYPE, SyncAnnotation.STREAM_CODEC, CommandBlockAnnotationNetwork::handleSync);
        registrar.playToClient(SyncCommandPreview.TYPE, SyncCommandPreview.STREAM_CODEC, CommandBlockAnnotationNetwork::handlePreviewSync);
        registrar.playToClient(RunCommandBlockResult.TYPE, RunCommandBlockResult.STREAM_CODEC, CommandBlockAnnotationNetwork::handleRunResult);
    }

    public static void clearReceived(BlockPos pos) {
        RECEIVED_ANNOTATIONS.remove(pos);
    }

    public static Optional<AnnotationSnapshot> takeReceived(BlockPos pos) {
        return Optional.ofNullable(RECEIVED_ANNOTATIONS.remove(pos));
    }

    public static void clearReceivedPreview(BlockPos pos) {
        RECEIVED_PREVIEWS.remove(pos);
    }

    public static Optional<CommandPreviewSnapshot> takeReceivedPreview(BlockPos pos) {
        return Optional.ofNullable(RECEIVED_PREVIEWS.remove(pos));
    }

    private static void handleRequest(RequestAnnotation payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CommandBlockEntity commandBlock = editableCommandBlock(player, payload.pos());
        if (commandBlock != null) {
            context.reply(createSyncPayload(payload.pos(), commandBlock));
        }
    }

    private static void handleUpdate(UpdateAnnotation payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CommandBlockEntity commandBlock = editableCommandBlock(player, payload.pos());
        if (commandBlock == null) {
            return;
        }

        String annotation = payload.annotation().replace("\r\n", "\n").replace('\r', '\n');
        if (annotation.length() > MAX_ANNOTATION_LENGTH) {
            annotation = annotation.substring(0, MAX_ANNOTATION_LENGTH);
        }
        if (annotation.isBlank()) {
            commandBlock.getPersistentData().remove(DATA_KEY);
            annotation = "";
        } else {
            commandBlock.getPersistentData().putString(DATA_KEY, annotation);
        }
        commandBlock.setChanged();
        context.reply(createSyncPayload(payload.pos(), commandBlock));
    }

    private static void handleOpen(RequestOpenCommandBlock payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CommandBlockEntity commandBlock = editableCommandBlock(player, payload.pos());
        if (commandBlock != null) {
            player.connection.send(ClientboundBlockEntityDataPacket.create(commandBlock, BlockEntity::saveWithoutMetadata));
        }
    }

    private static void handlePreviewRequest(RequestCommandPreview payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CommandBlockEntity commandBlock = editableCommandBlock(player, payload.pos());
        context.reply(commandBlock == null
                ? SyncCommandPreview.unavailable(payload.pos())
                : createPreviewPayload(payload.pos(), commandBlock));
    }

    private static void handleRun(RunCommandBlock payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        CommandBlockEntity commandBlock = editableCommandBlock(player, payload.pos());
        if (commandBlock == null) {
            context.reply(new RunCommandBlockResult(payload.pos(), false, false));
            return;
        }

        boolean executed = commandBlock.getCommandBlock().performCommand(player.serverLevel());
        commandBlock.setChanged();
        player.connection.send(ClientboundBlockEntityDataPacket.create(commandBlock, BlockEntity::saveWithoutMetadata));
        context.reply(new RunCommandBlockResult(payload.pos(), true, executed));
    }

    private static void handleSync(SyncAnnotation payload, IPayloadContext context) {
        RECEIVED_ANNOTATIONS.put(
                payload.pos().immutable(),
                new AnnotationSnapshot(payload.annotation(), List.copyOf(payload.history()))
        );
    }

    private static void handlePreviewSync(SyncCommandPreview payload, IPayloadContext context) {
        RECEIVED_PREVIEWS.put(payload.pos().immutable(), payload.snapshot());
    }

    private static void handleRunResult(RunCommandBlockResult payload, IPayloadContext context) {
        String messageKey = !payload.accepted()
                ? "cbs.runTest.denied"
                : payload.executed() ? "cbs.runTest.success" : "cbs.runTest.skipped";
        context.player().displayClientMessage(net.minecraft.network.chat.Component.translatable(messageKey), true);
    }

    public static CommandVersion captureCommandVersion(ServerPlayer player, BlockPos pos) {
        CommandBlockEntity commandBlock = editableCommandBlock(player, pos);
        return commandBlock == null ? null : commandVersion(commandBlock);
    }

    public static void recordCommandEdit(
            ServerPlayer player,
            BlockPos pos,
            CommandVersion previousVersion,
            CommandVersion submittedVersion
    ) {
        if (player.getServer() == null
                || !player.getServer().isCommandBlockEnabled()
                || !player.canUseGameMasterBlocks()) {
            return;
        }
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof CommandBlockEntity commandBlock)) {
            return;
        }

        CommandVersion acceptedVersion = commandVersion(commandBlock);
        if (submittedVersion == null || !acceptedVersion.equals(submittedVersion)
                || previousVersion == null || previousVersion.equals(acceptedVersion)) {
            return;
        }
        CompoundTag data = commandBlock.getPersistentData();
        List<EditHistoryEntry> history = new ArrayList<>(readHistory(commandBlock));
        if (!history.isEmpty() && history.getFirst().matches(acceptedVersion)) {
            return;
        }
        String editorName = player.getGameProfile().getName();
        if (editorName.length() > MAX_EDITOR_NAME_LENGTH) {
            editorName = editorName.substring(0, MAX_EDITOR_NAME_LENGTH);
        }
        long timestamp = System.currentTimeMillis();
        if (history.stream().noneMatch(EditHistoryEntry::snapshotAvailable)) {
            history.add(0, EditHistoryEntry.snapshot(Math.max(1L, timestamp - 1L), "", previousVersion));
        }
        history.add(0, EditHistoryEntry.snapshot(timestamp, editorName, acceptedVersion));
        if (history.size() > MAX_HISTORY_ENTRIES) {
            history = new ArrayList<>(history.subList(0, MAX_HISTORY_ENTRIES));
        }

        ListTag serializedHistory = new ListTag();
        for (EditHistoryEntry entry : history) {
            CompoundTag serializedEntry = new CompoundTag();
            serializedEntry.putLong("time", entry.timestamp());
            serializedEntry.putString("editor", entry.editor());
            if (entry.snapshotAvailable()) {
                serializedEntry.putString("command", entry.command());
                serializedEntry.putString("mode", entry.mode().name());
                serializedEntry.putBoolean("track_output", entry.trackOutput());
                serializedEntry.putBoolean("conditional", entry.conditional());
                serializedEntry.putBoolean("automatic", entry.automatic());
            }
            serializedHistory.add(serializedEntry);
        }
        data.put(HISTORY_KEY, serializedHistory);
        data.remove(HISTORY_LAST_COMMAND_KEY);
        commandBlock.setChanged();
    }

    private static SyncAnnotation createSyncPayload(BlockPos pos, CommandBlockEntity commandBlock) {
        return new SyncAnnotation(
                pos,
                commandBlock.getPersistentData().getString(DATA_KEY),
                readHistory(commandBlock)
        );
    }

    private static SyncCommandPreview createPreviewPayload(BlockPos pos, CommandBlockEntity commandBlock) {
        Optional<LatestEditInfo> latestEdit = latestEdit(commandBlock.getPersistentData());
        return new SyncCommandPreview(
                pos,
                true,
                commandBlock.getCommandBlock().getCommand(),
                commandBlock.getMode(),
                commandBlock.isConditional(),
                commandBlock.isAutomatic(),
                latestEdit.map(LatestEditInfo::timestamp).orElse(0L),
                latestEdit.map(LatestEditInfo::editor).orElse("")
        );
    }

    public static Optional<LatestEditInfo> latestEditFromBlockEntityData(CompoundTag blockEntityData) {
        CompoundTag persistentData = blockEntityData.contains(NEOFORGE_DATA_KEY, Tag.TAG_COMPOUND)
                ? blockEntityData.getCompound(NEOFORGE_DATA_KEY)
                : blockEntityData;
        return latestEdit(persistentData);
    }

    private static Optional<LatestEditInfo> latestEdit(CompoundTag persistentData) {
        ListTag history = persistentData.getList(HISTORY_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < history.size(); i++) {
            CompoundTag entry = history.getCompound(i);
            long timestamp = entry.getLong("time");
            String editor = entry.getString("editor");
            if (timestamp > 0L || !editor.isBlank()) {
                return Optional.of(new LatestEditInfo(timestamp, editor));
            }
        }
        return Optional.empty();
    }

    private static List<EditHistoryEntry> readHistory(CommandBlockEntity commandBlock) {
        ListTag serializedHistory = commandBlock.getPersistentData().getList(HISTORY_KEY, Tag.TAG_COMPOUND);
        List<EditHistoryEntry> result = new ArrayList<>(Math.min(serializedHistory.size(), MAX_HISTORY_ENTRIES));
        for (int i = 0; i < serializedHistory.size() && result.size() < MAX_HISTORY_ENTRIES; i++) {
            CompoundTag entry = serializedHistory.getCompound(i);
            long timestamp = entry.getLong("time");
            String editor = entry.getString("editor");
            boolean snapshotAvailable = entry.contains("command", Tag.TAG_STRING);
            if (timestamp > 0L && (!editor.isBlank() || snapshotAvailable)) {
                String command = entry.getString("command");
                if (command.length() > MAX_COMMAND_LENGTH) {
                    command = command.substring(0, MAX_COMMAND_LENGTH);
                }
                result.add(new EditHistoryEntry(
                        timestamp,
                        editor,
                        snapshotAvailable,
                        command,
                        readMode(entry.getString("mode")),
                        entry.getBoolean("track_output"),
                        entry.getBoolean("conditional"),
                        entry.getBoolean("automatic")
                ));
            }
        }
        return List.copyOf(result);
    }

    private static CommandVersion commandVersion(CommandBlockEntity commandBlock) {
        return new CommandVersion(
                commandBlock.getCommandBlock().getCommand(),
                commandBlock.getMode(),
                commandBlock.getCommandBlock().isTrackOutput(),
                commandBlock.isConditional(),
                commandBlock.isAutomatic()
        );
    }

    private static CommandBlockEntity.Mode readMode(String value) {
        try {
            return CommandBlockEntity.Mode.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return CommandBlockEntity.Mode.REDSTONE;
        }
    }

    private static CommandBlockEntity editableCommandBlock(ServerPlayer player, BlockPos pos) {
        if (!player.canUseGameMasterBlocks() || player.blockPosition().distSqr(pos) > MAX_EDIT_DISTANCE_SQUARED) {
            return null;
        }
        BlockEntity blockEntity = player.serverLevel().getBlockEntity(pos);
        return blockEntity instanceof CommandBlockEntity commandBlock ? commandBlock : null;
    }

    public record RequestAnnotation(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestAnnotation> TYPE = new Type<>(id("annotation_request"));
        public static final StreamCodec<ByteBuf, RequestAnnotation> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                RequestAnnotation::pos,
                RequestAnnotation::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateAnnotation(BlockPos pos, String annotation) implements CustomPacketPayload {
        public static final Type<UpdateAnnotation> TYPE = new Type<>(id("annotation_update"));
        public static final StreamCodec<ByteBuf, UpdateAnnotation> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                UpdateAnnotation::pos,
                ByteBufCodecs.stringUtf8(MAX_ANNOTATION_LENGTH),
                UpdateAnnotation::annotation,
                UpdateAnnotation::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestOpenCommandBlock(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestOpenCommandBlock> TYPE = new Type<>(id("command_block_open_request"));
        public static final StreamCodec<ByteBuf, RequestOpenCommandBlock> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                RequestOpenCommandBlock::pos,
                RequestOpenCommandBlock::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestCommandPreview(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RequestCommandPreview> TYPE = new Type<>(id("command_preview_request"));
        public static final StreamCodec<ByteBuf, RequestCommandPreview> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                RequestCommandPreview::pos,
                RequestCommandPreview::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RunCommandBlock(BlockPos pos) implements CustomPacketPayload {
        public static final Type<RunCommandBlock> TYPE = new Type<>(id("command_run_request"));
        public static final StreamCodec<ByteBuf, RunCommandBlock> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                RunCommandBlock::pos,
                RunCommandBlock::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RunCommandBlockResult(BlockPos pos, boolean accepted, boolean executed) implements CustomPacketPayload {
        public static final Type<RunCommandBlockResult> TYPE = new Type<>(id("command_run_result"));
        public static final StreamCodec<ByteBuf, RunCommandBlockResult> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                RunCommandBlockResult::pos,
                ByteBufCodecs.BOOL,
                RunCommandBlockResult::accepted,
                ByteBufCodecs.BOOL,
                RunCommandBlockResult::executed,
                RunCommandBlockResult::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncAnnotation(BlockPos pos, String annotation, List<EditHistoryEntry> history) implements CustomPacketPayload {
        public static final Type<SyncAnnotation> TYPE = new Type<>(id("annotation_sync"));
        private static final StreamCodec<ByteBuf, List<EditHistoryEntry>> HISTORY_STREAM_CODEC = ByteBufCodecs.collection(
                ArrayList::new,
                EditHistoryEntry.STREAM_CODEC,
                MAX_HISTORY_ENTRIES
        );
        public static final StreamCodec<ByteBuf, SyncAnnotation> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                SyncAnnotation::pos,
                ByteBufCodecs.stringUtf8(MAX_ANNOTATION_LENGTH),
                SyncAnnotation::annotation,
                HISTORY_STREAM_CODEC,
                SyncAnnotation::history,
                SyncAnnotation::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncCommandPreview(
            BlockPos pos,
            boolean available,
            String command,
            CommandBlockEntity.Mode mode,
            boolean conditional,
            boolean automatic,
            long timestamp,
            String editor
    ) implements CustomPacketPayload {
        public static final Type<SyncCommandPreview> TYPE = new Type<>(id("command_preview_sync"));
        public static final StreamCodec<ByteBuf, SyncCommandPreview> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public SyncCommandPreview decode(ByteBuf buffer) {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buffer);
                boolean available = ByteBufCodecs.BOOL.decode(buffer);
                String command = ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH).decode(buffer);
                int modeIndex = ByteBufCodecs.VAR_INT.decode(buffer);
                CommandBlockEntity.Mode[] modes = CommandBlockEntity.Mode.values();
                CommandBlockEntity.Mode mode = modeIndex >= 0 && modeIndex < modes.length
                        ? modes[modeIndex]
                        : CommandBlockEntity.Mode.REDSTONE;
                boolean conditional = ByteBufCodecs.BOOL.decode(buffer);
                boolean automatic = ByteBufCodecs.BOOL.decode(buffer);
                long timestamp = ByteBufCodecs.VAR_LONG.decode(buffer);
                String editor = ByteBufCodecs.stringUtf8(MAX_EDITOR_NAME_LENGTH).decode(buffer);
                return new SyncCommandPreview(pos, available, command, mode, conditional, automatic, timestamp, editor);
            }

            @Override
            public void encode(ByteBuf buffer, SyncCommandPreview payload) {
                BlockPos.STREAM_CODEC.encode(buffer, payload.pos());
                ByteBufCodecs.BOOL.encode(buffer, payload.available());
                ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH).encode(buffer, payload.command());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.mode().ordinal());
                ByteBufCodecs.BOOL.encode(buffer, payload.conditional());
                ByteBufCodecs.BOOL.encode(buffer, payload.automatic());
                ByteBufCodecs.VAR_LONG.encode(buffer, payload.timestamp());
                ByteBufCodecs.stringUtf8(MAX_EDITOR_NAME_LENGTH).encode(buffer, payload.editor());
            }
        };

        private static SyncCommandPreview unavailable(BlockPos pos) {
            return new SyncCommandPreview(
                    pos,
                    false,
                    "",
                    CommandBlockEntity.Mode.REDSTONE,
                    false,
                    false,
                    0L,
                    ""
            );
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public CommandPreviewSnapshot snapshot() {
            return new CommandPreviewSnapshot(available, command, mode, conditional, automatic, timestamp, editor);
        }
    }

    public record AnnotationSnapshot(String annotation, List<EditHistoryEntry> history) {
    }

    public record CommandPreviewSnapshot(
            boolean available,
            String command,
            CommandBlockEntity.Mode mode,
            boolean conditional,
            boolean automatic,
            long timestamp,
            String editor
    ) {
    }

    public record LatestEditInfo(long timestamp, String editor) {
    }

    public record CommandVersion(
            String command,
            CommandBlockEntity.Mode mode,
            boolean trackOutput,
            boolean conditional,
            boolean automatic
    ) {
    }

    public record EditHistoryEntry(
            long timestamp,
            String editor,
            boolean snapshotAvailable,
            String command,
            CommandBlockEntity.Mode mode,
            boolean trackOutput,
            boolean conditional,
            boolean automatic
    ) {
        public static final StreamCodec<ByteBuf, EditHistoryEntry> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public EditHistoryEntry decode(ByteBuf buffer) {
                long timestamp = ByteBufCodecs.VAR_LONG.decode(buffer);
                String editor = ByteBufCodecs.stringUtf8(MAX_EDITOR_NAME_LENGTH).decode(buffer);
                boolean snapshotAvailable = ByteBufCodecs.BOOL.decode(buffer);
                String command = ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH).decode(buffer);
                int modeIndex = ByteBufCodecs.VAR_INT.decode(buffer);
                CommandBlockEntity.Mode[] modes = CommandBlockEntity.Mode.values();
                CommandBlockEntity.Mode mode = modeIndex >= 0 && modeIndex < modes.length
                        ? modes[modeIndex]
                        : CommandBlockEntity.Mode.REDSTONE;
                boolean trackOutput = ByteBufCodecs.BOOL.decode(buffer);
                boolean conditional = ByteBufCodecs.BOOL.decode(buffer);
                boolean automatic = ByteBufCodecs.BOOL.decode(buffer);
                return new EditHistoryEntry(
                        timestamp,
                        editor,
                        snapshotAvailable,
                        command,
                        mode,
                        trackOutput,
                        conditional,
                        automatic
                );
            }

            @Override
            public void encode(ByteBuf buffer, EditHistoryEntry entry) {
                ByteBufCodecs.VAR_LONG.encode(buffer, entry.timestamp());
                ByteBufCodecs.stringUtf8(MAX_EDITOR_NAME_LENGTH).encode(buffer, entry.editor());
                ByteBufCodecs.BOOL.encode(buffer, entry.snapshotAvailable());
                ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH).encode(buffer, entry.command());
                ByteBufCodecs.VAR_INT.encode(buffer, entry.mode().ordinal());
                ByteBufCodecs.BOOL.encode(buffer, entry.trackOutput());
                ByteBufCodecs.BOOL.encode(buffer, entry.conditional());
                ByteBufCodecs.BOOL.encode(buffer, entry.automatic());
            }
        };

        private static EditHistoryEntry snapshot(long timestamp, String editor, CommandVersion version) {
            return new EditHistoryEntry(
                    timestamp,
                    editor,
                    true,
                    version.command(),
                    version.mode(),
                    version.trackOutput(),
                    version.conditional(),
                    version.automatic()
            );
        }

        public boolean matches(CommandVersion version) {
            return snapshotAvailable
                    && command.equals(version.command())
                    && mode == version.mode()
                    && trackOutput == version.trackOutput()
                    && conditional == version.conditional()
                    && automatic == version.automatic();
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CommandBlockStudioMod.MODID, path);
    }
}
