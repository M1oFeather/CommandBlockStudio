package com.miofeather.commandblockstudio.main.ui.screen;

import com.miofeather.commandblockstudio.main.ChainHandler;
import com.miofeather.commandblockstudio.main.CommandBlockWorkspace;
import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import com.miofeather.commandblockstudio.main.ui.CyclingTexturedButtonWidget;
import com.miofeather.commandblockstudio.main.ui.MultiLineTextFieldWidget;
import com.miofeather.commandblockstudio.main.ui.StudioIconButton;
import com.miofeather.commandblockstudio.main.util.Pair;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.miofeather.commandblockstudio.main.CommandBlockStudio.*;
import static net.minecraft.world.level.block.entity.CommandBlockEntity.Mode.REDSTONE;
import static net.minecraft.world.level.block.entity.CommandBlockEntity.Mode.SEQUENCE;
import static net.minecraft.world.level.block.entity.CommandBlockEntity.Mode.AUTO;

@OnlyIn(Dist.CLIENT)
public class CommandBlockStudioScreen extends AbstractCommandBlockStudioScreen {
    private static final int MAX_GROUP_MEMBERS_PER_TAB = 6;
    private static final int MAX_DISCOVERED_GROUP_BLOCKS = 24;

    private CyclingTexturedButtonWidget<CommandBlockEntity.Mode> modeButton;
    private CyclingTexturedButtonWidget<Boolean> conditionalModeButton;
    private CyclingTexturedButtonWidget<Boolean> redstoneTriggerButton;
    private CommandBlockEntity.Mode mode = REDSTONE;

    private ChainHandler chainHandler;
    private final List<WorkspaceTabEntry> workspaceTabs = new ArrayList<>();
    private final List<WorkspaceTabModel> workspaceTargets = new ArrayList<>();
    private StudioIconButton previousTabsButton;
    private StudioIconButton nextTabsButton;
    private int tabOffset;
    CommandBlockEntity blockEntity;
    private final BlockPos workspaceRoot;
    private boolean conditional;
    private boolean autoActivate;

    public CommandBlockStudioScreen(Minecraft client, CommandBlockEntity blockEntity, BaseCommandBlock commandExecutor) {
        this(client, blockEntity, commandExecutor, blockEntity.getBlockPos());
    }

    public CommandBlockStudioScreen(
            Minecraft client,
            CommandBlockEntity blockEntity,
            BaseCommandBlock commandExecutor,
            BlockPos workspaceRoot
    ) {
        this.blockEntity = blockEntity;
        this.commandExecutor = commandExecutor;
        this.workspaceRoot = workspaceRoot.immutable();
        this.chainHandler = new ChainHandler(
                Objects.requireNonNull(client.level, "Command block screen opened without a client level"),
                this.blockEntity.getBlockPos());
    }

    public BlockPos getWorkspaceRoot() {
        return workspaceRoot;
    }

    public boolean isEditing(BlockPos position) {
        return this.blockEntity.getBlockPos().equals(position);
    }

    @Override
    protected Optional<BlockPos> getAnnotationTarget() {
        return Optional.of(blockEntity.getBlockPos());
    }

    @Override
    public void init(){
        super.init();

        Component[] modeTooltips = {
                Component.translatable("advMode.mode.redstone"),
                Component.translatable("advMode.mode.sequence"),
                Component.translatable("advMode.mode.auto")};
        this.modeButton = this.addRenderableWidget(
                new CyclingTexturedButtonWidget<CommandBlockEntity.Mode>(
                        getContextControlX(0),
                        getContextControlY(),
                        CYCLE_BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.nullToEmpty(""),
                        button -> {
                            this.mode = ((CyclingTexturedButtonWidget<CommandBlockEntity.Mode>) button).getValue();
                            applyContextSettingImmediately();
                        },
                        new net.minecraft.client.gui.components.WidgetSprites[]{BUTTON_IMPULSE, BUTTON_CHAIN, BUTTON_REPEAT},
                        0,
                        new CommandBlockEntity.Mode[]{REDSTONE, SEQUENCE, AUTO},
                        modeTooltips
                ));

        Component[] conditionalTooltips = {
                Component.translatable("advMode.mode.unconditional"),
                Component.translatable("advMode.mode.conditional")};
        this.conditionalModeButton = this.addRenderableWidget(
                new CyclingTexturedButtonWidget<Boolean>(
                        getContextControlX(1),
                        getContextControlY(),
                        CYCLE_BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.nullToEmpty(""),
                        button -> {
                            this.conditional = ((CyclingTexturedButtonWidget<Boolean>) button).getValue();
                            applyContextSettingImmediately();
                        },
                        new net.minecraft.client.gui.components.WidgetSprites[]{BUTTON_UNCONDITIONAL, BUTTON_CONDITIONAL},
                        0,
                        new Boolean[]{false, true},
                        conditionalTooltips
                ));

        Component[] activeTooltips = {
                Component.translatable("advMode.mode.redstoneTriggered"),
                Component.translatable("advMode.mode.autoexec.bat")};
        this.redstoneTriggerButton = this.addRenderableWidget(
                new CyclingTexturedButtonWidget<Boolean>(
                        getContextControlX(2),
                        getContextControlY(),
                        CYCLE_BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        Component.nullToEmpty(""),
                        button -> {
                            this.autoActivate = ((CyclingTexturedButtonWidget<Boolean>) button).getValue();
                            applyContextSettingImmediately();
                        },
                        new net.minecraft.client.gui.components.WidgetSprites[]{BUTTON_POWER_INACTIVE, BUTTON_POWER_ACTIVE},
                        0,
                        new Boolean[]{false, true},
                        activeTooltips
                ));

        createWorkspaceTabs();

        setButtonsActive(false);
    }

    private void createWorkspaceTabs() {
        for (WorkspaceTabEntry entry : workspaceTabs) {
            removeWidget(entry.tabButton());
            removeWidget(entry.pinButton());
        }
        if (previousTabsButton != null) {
            removeWidget(previousTabsButton);
        }
        if (nextTabsButton != null) {
            removeWidget(nextTabsButton);
        }
        workspaceTabs.clear();
        workspaceTargets.clear();
        tabOffset = 0;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        BlockPos current = blockEntity.getBlockPos().immutable();
        BlockPos root = isCommandBlock(client, workspaceRoot) ? workspaceRoot : current;
        List<TabTarget> rawTargets = new ArrayList<>();
        Set<BlockPos> included = new HashSet<>();
        boolean currentInChain = chainHandler.isInChain();
        boolean rootIsCurrent = root.equals(current);
        boolean rootInChain = rootIsCurrent
                ? currentInChain
                : new ChainHandler(client.level, root).isInChain();
        addWorkspaceTarget(
                rawTargets,
                root,
                rootIsCurrent
                        ? rootInChain ? TabKind.CURRENT_CHAIN : TabKind.CURRENT_STANDALONE
                        : rootInChain ? TabKind.WORKSPACE_CHAIN : TabKind.WORKSPACE_STANDALONE,
                Component.translatable(
                        rootIsCurrent
                                ? rootInChain ? "cbs.tabs.currentChain" : "cbs.tabs.currentStandalone"
                                : "cbs.tabs.workspaceRoot",
                        shortPosition(root)
                ),
                included
        );

        if (!rootIsCurrent) {
            addWorkspaceTarget(
                    rawTargets,
                    current,
                    currentInChain ? TabKind.CURRENT_CHAIN : TabKind.CURRENT_STANDALONE,
                    Component.translatable(
                            currentInChain ? "cbs.tabs.currentChain" : "cbs.tabs.currentStandalone",
                            shortPosition(current)
                    ),
                    included
            );
        }

        if (chainHandler.isInChain()) {
            BlockState next = chainHandler.getNext();
            if (next != null && next.is(Blocks.CHAIN_COMMAND_BLOCK)) {
                Direction direction = client.level.getBlockState(current).getValue(CommandBlock.FACING);
                BlockPos position = current.relative(direction);
                addWorkspaceTarget(rawTargets, position, TabKind.CHAIN, Component.translatable("cbs.tabs.chainNext", shortPosition(position)), included);
            }
            for (Pair<BlockState, Direction> entry : chainHandler.getPrior()) {
                BlockPos position = current.relative(entry.getB());
                addWorkspaceTarget(rawTargets, position, TabKind.CHAIN, Component.translatable("cbs.tabs.chainPrior", shortPosition(position)), included);
            }
        }

        for (BlockPos pinned : CommandBlockWorkspace.pinned(client.level)) {
            addWorkspaceTarget(rawTargets, pinned, TabKind.PINNED, Component.translatable("cbs.tabs.pinned", shortPosition(pinned)), included);
        }

        workspaceTargets.addAll(groupWorkspaceTargets(client, rawTargets, current));

        for (WorkspaceTabModel target : workspaceTargets) {
            boolean currentTarget = target.contains(current);
            Button tabButton;
            if (target.targets().size() > 1) {
                tabButton = addRenderableWidget(new WorkspaceGroupTabButton(target, current));
            } else {
                TabTarget single = target.targets().getFirst();
                WorkspaceTabButton singleButton = addRenderableWidget(new WorkspaceTabButton(
                        single.label(),
                        single.kind(),
                        currentTarget,
                        pressed -> requestNavigate(single.position())
                ));
                singleButton.active = !currentTarget;
                singleButton.setTooltip(Tooltip.create(Component.translatable(
                        currentTarget ? "cbs.tabs.current.tooltip" : "cbs.tabs.switch.tooltip",
                        single.position().getX(),
                        single.position().getY(),
                        single.position().getZ()
                )));
                tabButton = singleButton;
            }
            StudioIconButton pinButton = addRenderableWidget(new StudioIconButton(
                    0,
                    0,
                    14,
                    14,
                    Component.translatable("cbs.tabs.pin"),
                    ICON_PIN,
                    12,
                    true,
                    pressed -> toggleTabPinned(target)
            ));
            WorkspaceTabEntry entry = new WorkspaceTabEntry(target, tabButton, pinButton);
            workspaceTabs.add(entry);
            updateTabPinButton(entry);
        }

        previousTabsButton = addRenderableWidget(new StudioIconButton(
                0,
                0,
                18,
                18,
                Component.translatable("cbs.tabs.previous"),
                ICON_ARROW_LEFT,
                12,
                true,
                button -> {
                    tabOffset = Math.max(0, tabOffset - 1);
                    layoutWorkspaceTabs();
                }
        ));
        previousTabsButton.setTooltip(Tooltip.create(Component.translatable("cbs.tabs.previous")));
        nextTabsButton = addRenderableWidget(new StudioIconButton(
                0,
                0,
                18,
                18,
                Component.translatable("cbs.tabs.next"),
                ICON_ARROW_RIGHT,
                12,
                true,
                button -> {
                    tabOffset++;
                    layoutWorkspaceTabs();
                }
        ));
        nextTabsButton.setTooltip(Tooltip.create(Component.translatable("cbs.tabs.next")));
        layoutWorkspaceTabs();
    }

    private void addWorkspaceTarget(
            List<TabTarget> targets,
            BlockPos position,
            TabKind kind,
            Component label,
            Set<BlockPos> included
    ) {
        BlockPos immutable = position.immutable();
        if (included.add(immutable)) {
            targets.add(new TabTarget(immutable, kind, label));
        }
    }

    private List<WorkspaceTabModel> groupWorkspaceTargets(
            Minecraft client,
            List<TabTarget> rawTargets,
            BlockPos current
    ) {
        if (client.level == null) {
            return List.of();
        }

        Map<BlockPos, TabTarget> targetsByPosition = new LinkedHashMap<>();
        for (TabTarget target : rawTargets) {
            targetsByPosition.putIfAbsent(target.position(), target);
        }

        List<WorkspaceTabModel> result = new ArrayList<>();
        Set<BlockPos> consumed = new HashSet<>();
        for (TabTarget seed : rawTargets) {
            if (consumed.contains(seed.position())) {
                continue;
            }
            if (!isCommandBlock(client, seed.position())) {
                consumed.add(seed.position());
                result.add(WorkspaceTabModel.single(seed));
                continue;
            }

            List<BlockPos> ordered = orderCommandBlockComponent(
                    client,
                    discoverCommandBlockComponent(client, seed.position(), seed.kind() == TabKind.PINNED)
            );
            consumed.addAll(ordered);
            if (ordered.size() <= 1) {
                result.add(WorkspaceTabModel.single(seed));
                continue;
            }

            List<WorkspaceTabModel> componentTabs = new ArrayList<>();
            for (int start = 0; start < ordered.size(); start += MAX_GROUP_MEMBERS_PER_TAB) {
                int end = Math.min(ordered.size(), start + MAX_GROUP_MEMBERS_PER_TAB);
                List<TabTarget> members = ordered.subList(start, end).stream()
                        .map(position -> targetsByPosition.getOrDefault(
                                position,
                                new TabTarget(
                                        position,
                                        position.equals(current) ? TabKind.CURRENT_STANDALONE : TabKind.WORKSPACE_STANDALONE,
                                        Component.translatable("cbs.tabs.workspaceStandalone", shortPosition(position))
                                )
                        ))
                        .toList();
                boolean containsCurrent = members.stream().anyMatch(target -> target.position().equals(current));
                boolean containsChain = members.stream()
                        .anyMatch(target -> isChainCommandBlock(client, target.position()));
                componentTabs.add(new WorkspaceTabModel(
                        members,
                        containsCurrent
                                ? containsChain ? TabKind.CURRENT_CHAIN_GROUP : TabKind.CURRENT_GROUP
                                : containsChain ? TabKind.GROUP_CHAIN : TabKind.GROUP_STANDALONE,
                        Component.translatable("cbs.tabs.group", members.size())
                ));
            }
            componentTabs.sort(Comparator.comparing((WorkspaceTabModel model) -> !model.contains(current)));
            result.addAll(componentTabs);
        }
        return result;
    }

    private Set<BlockPos> discoverCommandBlockComponent(Minecraft client, BlockPos seed, boolean pinnedOnly) {
        if (client.level == null) {
            return Set.of(seed);
        }
        Set<BlockPos> discovered = new LinkedHashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        pending.add(seed.immutable());
        while (!pending.isEmpty() && discovered.size() < MAX_DISCOVERED_GROUP_BLOCKS) {
            BlockPos position = pending.removeFirst();
            if (!discovered.add(position) || !isWorkspaceComponentMember(client, position, pinnedOnly)) {
                discovered.remove(position);
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = position.relative(direction).immutable();
                if (!discovered.contains(neighbor) && isWorkspaceComponentMember(client, neighbor, pinnedOnly)) {
                    pending.addLast(neighbor);
                }
            }
        }
        return discovered;
    }

    private boolean isWorkspaceComponentMember(Minecraft client, BlockPos position, boolean pinnedOnly) {
        return isCommandBlock(client, position)
                && (!pinnedOnly || CommandBlockWorkspace.isPinned(client.level, position));
    }

    private boolean isCommandBlock(Minecraft client, BlockPos position) {
        if (client.level == null) {
            return false;
        }
        return client.level.getBlockEntity(position) instanceof CommandBlockEntity;
    }

    private boolean isChainCommandBlock(Minecraft client, BlockPos position) {
        if (client.level == null) {
            return false;
        }
        BlockEntity blockEntity = client.level.getBlockEntity(position);
        return blockEntity instanceof CommandBlockEntity commandBlock && commandBlock.getMode() == SEQUENCE;
    }

    private List<BlockPos> orderCommandBlockComponent(Minecraft client, Set<BlockPos> component) {
        List<BlockPos> spatialOrder = new ArrayList<>(component);
        if (spatialOrder.size() < 2) {
            return spatialOrder;
        }

        Comparator<BlockPos> spatialComparator = createSpatialComparator(spatialOrder);
        spatialOrder.sort(spatialComparator);
        if (client.level == null || spatialOrder.stream().noneMatch(position -> isChainCommandBlock(client, position))) {
            return spatialOrder;
        }

        Map<BlockPos, BlockPos> nextByPosition = new LinkedHashMap<>();
        Map<BlockPos, Integer> incomingEdges = new LinkedHashMap<>();
        for (BlockPos position : spatialOrder) {
            incomingEdges.put(position, 0);
        }
        for (BlockPos source : spatialOrder) {
            BlockState sourceState = client.level.getBlockState(source);
            if (!(sourceState.getBlock() instanceof CommandBlock)) {
                continue;
            }
            BlockPos target = source.relative(sourceState.getValue(CommandBlock.FACING)).immutable();
            if (component.contains(target) && isChainCommandBlock(client, target)) {
                nextByPosition.put(source, target);
                incomingEdges.computeIfPresent(target, (ignored, count) -> count + 1);
            }
        }

        LinkedHashSet<BlockPos> executionOrder = new LinkedHashSet<>();
        spatialOrder.stream()
                .filter(nextByPosition::containsKey)
                .filter(position -> incomingEdges.getOrDefault(position, 0) == 0)
                .forEach(position -> appendExecutionPath(position, nextByPosition, executionOrder));
        for (BlockPos position : spatialOrder) {
            appendExecutionPath(position, nextByPosition, executionOrder);
        }
        return new ArrayList<>(executionOrder);
    }

    private static Comparator<BlockPos> createSpatialComparator(List<BlockPos> positions) {
        int spanX = coordinateSpan(positions, BlockPos::getX);
        int spanY = coordinateSpan(positions, BlockPos::getY);
        int spanZ = coordinateSpan(positions, BlockPos::getZ);

        if (spanY >= spanX && spanY >= spanZ) {
            return Comparator.comparingInt((BlockPos position) -> position.getY()).reversed()
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ);
        }
        if (spanX >= spanZ) {
            return Comparator.comparingInt((BlockPos position) -> position.getX())
                    .thenComparing(Comparator.comparingInt((BlockPos position) -> position.getY()).reversed())
                    .thenComparingInt(BlockPos::getZ);
        }
        return Comparator.comparingInt((BlockPos position) -> position.getZ())
                .thenComparing(Comparator.comparingInt((BlockPos position) -> position.getY()).reversed())
                .thenComparingInt(BlockPos::getX);
    }

    private static void appendExecutionPath(
            BlockPos start,
            Map<BlockPos, BlockPos> nextByPosition,
            Set<BlockPos> result
    ) {
        BlockPos current = start;
        while (current != null && result.add(current)) {
            current = nextByPosition.get(current);
        }
    }

    private static int coordinateSpan(List<BlockPos> positions, java.util.function.ToIntFunction<BlockPos> coordinate) {
        int minimum = Integer.MAX_VALUE;
        int maximum = Integer.MIN_VALUE;
        for (BlockPos position : positions) {
            int value = coordinate.applyAsInt(position);
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
        }
        return maximum - minimum;
    }

    private void layoutWorkspaceTabs() {
        if (previousTabsButton == null || nextTabsButton == null) {
            return;
        }
        int y = TOP_BAR_HEIGHT + 7;
        int nextX = editorX + editorWidth - 21;
        nextTabsButton.setPosition(nextX, y);
        previousTabsButton.setPosition(nextX - 20, y);

        int tabsX = editorX + 4;
        int availableWidth = Math.max(58, previousTabsButton.getX() - tabsX - 3);
        int visibleCount = Math.max(1, availableWidth / 76);
        int maximumOffset = Math.max(0, workspaceTabs.size() - visibleCount);
        tabOffset = Mth.clamp(tabOffset, 0, maximumOffset);
        int actualVisible = Math.max(1, Math.min(visibleCount, workspaceTabs.size() - tabOffset));
        int tabWidth = Mth.clamp(availableWidth / actualVisible, 72, 116);

        for (int i = 0; i < workspaceTabs.size(); i++) {
            WorkspaceTabEntry entry = workspaceTabs.get(i);
            boolean visible = i >= tabOffset && i < tabOffset + visibleCount;
            entry.tabButton().visible = visible;
            entry.pinButton().visible = visible;
            if (visible) {
                int visibleIndex = i - tabOffset;
                int slotX = tabsX + visibleIndex * tabWidth;
                int slotWidth = tabWidth - 2;
                int tabButtonWidth = Math.max(42, slotWidth - 15);
                entry.tabButton().setPosition(slotX, y);
                entry.tabButton().setWidth(tabButtonWidth);
                entry.pinButton().setPosition(slotX + tabButtonWidth, y + 2);
            }
        }
        boolean paged = workspaceTabs.size() > visibleCount;
        previousTabsButton.visible = paged;
        nextTabsButton.visible = paged;
        previousTabsButton.active = tabOffset > 0;
        nextTabsButton.active = tabOffset < maximumOffset;
    }

    private void updateTabPinButton(WorkspaceTabEntry entry) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        boolean pinned = entry.target().targets().stream()
                .allMatch(target -> CommandBlockWorkspace.isPinned(client.level, target.position()));
        entry.pinButton().setIcon(pinned ? ICON_PIN_ACTIVE : ICON_PIN);
        entry.pinButton().setSelected(pinned);
        boolean group = entry.target().targets().size() > 1;
        entry.pinButton().setMessage(Component.translatable(
                pinned
                        ? group ? "cbs.tabs.unpinGroup" : "cbs.tabs.unpin"
                        : group ? "cbs.tabs.pinGroup" : "cbs.tabs.pin"
        ));
        entry.pinButton().setTooltip(Tooltip.create(Component.translatable(
                pinned
                        ? group ? "cbs.tabs.unpinGroup" : "cbs.tabs.unpin"
                        : group ? "cbs.tabs.pinGroup" : "cbs.tabs.pin"
        )));
    }

    private void toggleTabPinned(WorkspaceTabModel target) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        boolean allPinned = target.targets().stream()
                .allMatch(member -> CommandBlockWorkspace.isPinned(client.level, member.position()));
        for (TabTarget member : target.targets()) {
            boolean pinned = CommandBlockWorkspace.isPinned(client.level, member.position());
            if (pinned == allPinned) {
                CommandBlockWorkspace.togglePinned(client.level, member.position());
            }
        }
        createWorkspaceTabs();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        double studioX = studioMouseX(mouseX);
        double studioY = studioMouseY(mouseY);
        boolean overTabStrip = studioX >= editorX
                && studioX < editorX + editorWidth
                && studioY >= TOP_BAR_HEIGHT + 4
                && studioY < editorY - 2;
        if (overTabStrip && previousTabsButton != null && previousTabsButton.visible) {
            boolean horizontalGesture = Math.abs(scrollX) > Math.abs(scrollY);
            double amount = horizontalGesture ? scrollX : scrollY;
            if (Math.abs(amount) > 0.01D) {
                int steps = Math.max(1, (int) Math.round(Math.abs(amount)));
                int direction = horizontalGesture
                        ? amount > 0.0D ? 1 : -1
                        : amount < 0.0D ? 1 : -1;
                tabOffset += direction * steps;
                layoutWorkspaceTabs();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static String shortPosition(BlockPos position) {
        return position.getX() + "," + position.getY() + "," + position.getZ();
    }

    private void requestNavigate(BlockPos position) {
        if (position.equals(blockEntity.getBlockPos()) || minecraft == null) {
            return;
        }
        if (AUTOSAVE && wasModified()) {
            commitSilently();
            navigateToBlock(position);
            return;
        }
        if (!AUTOSAVE && CONFIRM_UNSAVED_EXIT && wasModified()) {
            EditorSessionState sessionState = captureEditorSession();
            minecraft.setScreen(new ConfirmScreen(
                    save -> {
                        resumeEditorSession(sessionState);
                        if (save) {
                            commit();
                            navigateToBlock(position);
                        }
                    },
                    Component.translatable("cbs.unsaved.title"),
                    Component.translatable("cbs.unsaved.switchMessage"),
                    Component.translatable("cbs.unsaved.saveSwitch"),
                    Component.translatable("cbs.unsaved.continue")
            ));
            return;
        }
        restoreLocalStateBeforeDiscard();
        navigateToBlock(position);
    }

    private void navigateToBlock(BlockPos position) {
        Minecraft client = Minecraft.getInstance();
        if (client.gameMode == null || client.player == null) {
            return;
        }
        if (client.level != null && client.getConnection() != null
                && client.getConnection().hasChannel(CommandBlockAnnotationNetwork.RequestOpenCommandBlock.TYPE)) {
            BlockEntity target = client.level.getBlockEntity(position);
            if (target instanceof CommandBlockEntity commandBlock) {
                client.setScreen(new CommandBlockStudioScreen(
                        client,
                        commandBlock,
                        commandBlock.getCommandBlock(),
                        workspaceRoot
                ));
                client.getConnection().send(new CommandBlockAnnotationNetwork.RequestOpenCommandBlock(position));
                return;
            }
        }
        Vec3 hitPos = Vec3.atCenterOf(position);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, new BlockHitResult(
                hitPos,
                Direction.UP,
                position,
                false
        ));
    }

    @Override
    protected void setButtonsActive(boolean value){
        super.setButtonsActive(value);
        if (this.modeButton != null) {
            this.modeButton.setActive(value);
            this.conditionalModeButton.setActive(value);
            this.redstoneTriggerButton.setActive(value);
        }
    }

    @Override
    protected void restoreTargetControlsAfterReinit() {
        int modeIndex = switch (mode) {
            case REDSTONE -> 0;
            case SEQUENCE -> 1;
            case AUTO -> 2;
        };
        modeButton.setIndex(modeIndex);
        conditionalModeButton.setIndex(conditional ? 1 : 0);
        redstoneTriggerButton.setIndex(autoActivate ? 1 : 0);
    }

    @Override
    protected void restoreHistoryTargetState(CommandBlockAnnotationNetwork.EditHistoryEntry entry) {
        mode = entry.mode();
        conditional = entry.conditional();
        autoActivate = entry.automatic();
        restoreTargetControlsAfterReinit();
    }

    @Override
    protected Component getTargetDescription() {
        BlockPos pos = blockEntity.getBlockPos();
        return Component.translatable("cbs.target.block", pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    protected boolean wasModified(){
        boolean bl = super.wasModified();
        return bl
                || modeButton.getValue() != priorState.type
                || conditionalModeButton.getValue() != priorState.conditional
                || redstoneTriggerButton.getValue() != priorState.needsRedstone;
    }

    @Override
    public void returnFromConfig(){
        updateCommandBlock();
        super.returnFromConfig();
    }

    public void updateCommandBlock() {
        updated = true;
        BaseCommandBlock commandBlockExecutor = this.blockEntity.getCommandBlock();
        MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) this.consoleCommandTextField;
        if (!editor.wasModified()) {
            editor.setRawText(commandBlockExecutor.getCommand());
        }
        this.trackOutput = commandBlockExecutor.isTrackOutput();
        this.mode = this.blockEntity.getMode();
        this.conditional = this.blockEntity.isConditional();
        this.autoActivate = this.blockEntity.isAutomatic();

        this.priorState = new CommandBlockState(mode, conditional, autoActivate, trackOutput);

        if(!TRACK_OUTPUT_DEFAULT_USED) {
            int trackingOutputIndex = trackOutput ? 0 : 1;
            this.toggleTrackingOutputButton.setIndex(trackingOutputIndex);
        }

        int modeIndex = 0;
        this.mode = blockEntity.getMode();
        switch (this.mode) {
            case SEQUENCE -> modeIndex = 1;
            case AUTO -> modeIndex = 2;
        }
        this.modeButton.setIndex(modeIndex);

        int conditionalIndex = this.conditional?1:0;
        this.conditionalModeButton.setIndex(conditionalIndex);

        int autoIndex = this.autoActivate?1:0;
        this.redstoneTriggerButton.setIndex(autoIndex);

        this.setPreviousOutputText(trackOutput);
        this.setButtonsActive(true);
        refreshWorkspaceContext();
    }

    private void refreshWorkspaceContext() {
        if (minecraft == null || minecraft.level == null) {
            return;
        }
        chainHandler = new ChainHandler(minecraft.level, blockEntity.getBlockPos());
        createWorkspaceTabs();
    }

    @Override
    protected void commit(){
        super.commit();
        priorState.needsRedstone = redstoneTriggerButton.getValue();
        priorState.type = modeButton.getValue();
        priorState.conditional = conditional;
    }

    protected void syncSettingsToServer(BaseCommandBlock commandExecutor) {
        sendSettingsToServer(this.consoleCommandTextField.getValue());
    }

    @Override
    protected boolean supportsRunTest() {
        return true;
    }

    @Override
    protected boolean canRunTest() {
        return minecraft != null
                && minecraft.getConnection() != null
                && minecraft.getConnection().hasChannel(CommandBlockAnnotationNetwork.RunCommandBlock.TYPE);
    }

    @Override
    protected void runTest() {
        if (!canRunTest()) {
            return;
        }
        if (wasModified()) {
            commitSilently();
        }
        minecraft.getConnection().send(new CommandBlockAnnotationNetwork.RunCommandBlock(blockEntity.getBlockPos()));
    }

    private void applyContextSettingImmediately() {
        if (!updated || modeButton == null || conditionalModeButton == null || redstoneTriggerButton == null) {
            return;
        }
        if (AUTOSAVE) {
            commitSilently();
            return;
        }
        sendSettingsToServer(commandExecutor.getCommand());
        priorState.type = mode;
        priorState.conditional = conditional;
        priorState.needsRedstone = autoActivate;
    }

    private BlockState currentPreviewBlockState() {
        BlockState preview = switch (mode) {
            case REDSTONE -> Blocks.COMMAND_BLOCK.defaultBlockState();
            case SEQUENCE -> Blocks.CHAIN_COMMAND_BLOCK.defaultBlockState();
            case AUTO -> Blocks.REPEATING_COMMAND_BLOCK.defaultBlockState();
        };
        return preview.setValue(CommandBlock.CONDITIONAL, conditional);
    }

    private void sendSettingsToServer(String command) {
        var connection = Objects.requireNonNull(
                Objects.requireNonNull(this.minecraft, "Command block screen is not attached to Minecraft").getConnection(),
                "No client connection while saving command block");
        connection.send(
                new ServerboundSetCommandBlockPacket(
                        blockEntity.getBlockPos(),
                        command,
                        this.mode,
                        this.trackOutput,
                        this.conditional,
                        this.autoActivate));
    }

    @Override
    protected void renderTargetOverlay(final GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderAsterisk(graphics, modeButton, modeButton.getValue() != priorState.type);
        renderAsterisk(graphics, conditionalModeButton, conditionalModeButton.getValue() != priorState.conditional);
        renderAsterisk(graphics, redstoneTriggerButton, redstoneTriggerButton.getValue() != priorState.needsRedstone);

        if(chainHandler.isInChain() && !updated){
            graphics.fill(
                    consoleCommandTextField.getX(),
                    consoleCommandTextField.getY(),
                    consoleCommandTextField.getX() + consoleCommandTextField.getWidth(),
                    consoleCommandTextField.getY() + consoleCommandTextField.getHeight(),
                    0x8F000000);
            if(previousOutputTextField.isVisible()){
                graphics.fill(
                        previousOutputTextField.getX(),
                        previousOutputTextField.getY(),
                        previousOutputTextField.getX() + previousOutputTextField.getWidth(),
                        previousOutputTextField.getY() + previousOutputTextField.getHeight(),
                        0x8F000000);
            }
            graphics.drawCenteredString(font, Component.translatable("cbs.chain.tooFar"), width/2, height/2, 0xFFA0A0A0);
        }
    }

    @Override
    protected void renderEditorHeader(GuiGraphics graphics) {
        // Command-block targets use the editor header as an IDE-style workspace tab strip.
    }

    @Override
    protected void onDynamicLayoutApplied() {
        layoutWorkspaceTabs();
    }

    private enum TabKind {
        CURRENT_STANDALONE,
        CURRENT_CHAIN,
        CURRENT_GROUP,
        CURRENT_CHAIN_GROUP,
        CHAIN,
        WORKSPACE_STANDALONE,
        WORKSPACE_CHAIN,
        GROUP_STANDALONE,
        GROUP_CHAIN,
        PINNED
    }

    private record TabTarget(BlockPos position, TabKind kind, Component label) {
    }

    private record WorkspaceTabModel(List<TabTarget> targets, TabKind kind, Component label) {
        private WorkspaceTabModel {
            targets = List.copyOf(targets);
        }

        private static WorkspaceTabModel single(TabTarget target) {
            return new WorkspaceTabModel(List.of(target), target.kind(), target.label());
        }

        private boolean contains(BlockPos position) {
            return targets.stream().anyMatch(target -> target.position().equals(position));
        }
    }

    private record WorkspaceTabEntry(
            WorkspaceTabModel target,
            Button tabButton,
            StudioIconButton pinButton
    ) {
    }

    private final class WorkspaceTabButton extends Button {
        private final TabKind kind;
        private final boolean current;

        private WorkspaceTabButton(Component message, TabKind kind, boolean current, OnPress onPress) {
            super(0, 0, 80, 18, message, onPress, DEFAULT_NARRATION);
            this.kind = kind;
            this.current = current;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int background = current
                    ? 0xFF242A31
                    : isHoveredOrFocused() ? 0xFF20262D : 0xFF181C21;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
            int markerColor = switch (kind) {
                case CURRENT_STANDALONE, WORKSPACE_STANDALONE -> 0xFF56D364;
                case CURRENT_CHAIN, CURRENT_CHAIN_GROUP, CHAIN, WORKSPACE_CHAIN, GROUP_CHAIN -> 0xFF4FC3D7;
                case CURRENT_GROUP, GROUP_STANDALONE -> 0xFF56D364;
                case PINNED -> 0xFFE3B341;
            };
            graphics.fill(getX(), getY(), getX() + 3, getY() + getHeight(), markerColor);
            graphics.fill(getX(), getY() + getHeight() - 1, getX() + getWidth(), getY() + getHeight(),
                    current ? 0xFF4FC3D7 : 0xFF343A42);
            int textColor = switch (kind) {
                case CURRENT_STANDALONE, CURRENT_CHAIN, CURRENT_GROUP, CURRENT_CHAIN_GROUP -> 0xFFF2F4F7;
                case CHAIN, WORKSPACE_CHAIN, GROUP_CHAIN -> 0xFF8ED8E6;
                case WORKSPACE_STANDALONE, GROUP_STANDALONE -> 0xFFB8C0C8;
                case PINNED -> 0xFFFFD866;
            };
            String label = font.plainSubstrByWidth(getMessage().getString(), Math.max(1, getWidth() - 11));
            graphics.drawCenteredString(font, label, getX() + getWidth() / 2 + 1, getY() + 5, textColor);
        }
    }

    private final class WorkspaceGroupTabButton extends Button {
        private final WorkspaceTabModel model;
        private final BlockPos current;
        private int hoveredMember = -1;

        private WorkspaceGroupTabButton(WorkspaceTabModel model, BlockPos current) {
            super(0, 0, 80, 18, model.label(), ignored -> {
            }, DEFAULT_NARRATION);
            this.model = model;
            this.current = current;
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            int member = memberAt(mouseX);
            if (member < 0 || member >= model.targets().size()) {
                return;
            }
            BlockPos position = model.targets().get(member).position();
            if (!position.equals(current)) {
                requestNavigate(position);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            boolean currentGroup = model.contains(current);
            boolean chainGroup = model.kind() == TabKind.CURRENT_CHAIN_GROUP || model.kind() == TabKind.GROUP_CHAIN;
            int background = currentGroup ? 0xFF242A31 : isHoveredOrFocused() ? 0xFF20262D : 0xFF181C21;
            graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), background);
            graphics.fill(
                    getX(),
                    getY(),
                    getX() + 3,
                    getY() + getHeight(),
                    chainGroup ? 0xFF4FC3D7 : 0xFF56D364
            );
            graphics.fill(
                    getX(),
                    getY() + getHeight() - 1,
                    getX() + getWidth(),
                    getY() + getHeight(),
                    currentGroup ? 0xFF4FC3D7 : 0xFF343A42
            );

            int count = model.targets().size();
            int contentX = getX() + 4;
            int contentWidth = Math.max(count, getWidth() - 5);
            int cellWidth = Math.max(1, contentWidth / count);
            int newHoveredMember = -1;
            Minecraft client = Minecraft.getInstance();
            for (int index = 0; index < count; index++) {
                TabTarget target = model.targets().get(index);
                int cellX = contentX + index * cellWidth;
                int cellRight = index == count - 1 ? getX() + getWidth() : cellX + cellWidth;
                boolean hovered = mouseX >= cellX && mouseX < cellRight
                        && mouseY >= getY() && mouseY < getY() + getHeight();
                if (hovered) {
                    newHoveredMember = index;
                    graphics.fill(cellX, getY() + 1, cellRight, getY() + getHeight() - 1, 0xFF303640);
                }
                if (target.position().equals(current)) {
                    graphics.fill(cellX, getY() + 1, cellRight, getY() + getHeight() - 1, 0xFF26383D);
                    graphics.fill(cellX, getY() + getHeight() - 2, cellRight, getY() + getHeight(), 0xFF4FC3D7);
                }

                int iconSize = Math.max(7, Math.min(16, Math.min(cellRight - cellX - 1, getHeight() - 2)));
                int iconX = cellX + Math.max(0, (cellRight - cellX - iconSize) / 2);
                int iconY = getY() + (getHeight() - iconSize) / 2;
                BlockState state;
                if (target.position().equals(blockEntity.getBlockPos())) {
                    state = currentPreviewBlockState();
                } else if (client.level == null) {
                    state = Blocks.COMMAND_BLOCK.defaultBlockState();
                } else {
                    state = client.level.getBlockState(target.position());
                }
                WidgetSprites sprites;
                try {
                    sprites = BlockStateToButtonTextures(state);
                } catch (IllegalArgumentException ignored) {
                    sprites = BLOCK_IMPULSE;
                }
                graphics.blitSprite(RenderType::guiTextured, sprites.get(true, hovered), iconX, iconY, iconSize, iconSize);

                String order = Integer.toString(index + 1);
                int orderX = iconX + iconSize - font.width(order);
                int orderY = iconY + iconSize - 8;
                graphics.drawString(font, order, orderX, orderY, 0xFFFFFFFF, true);
                if (index + 1 < count) {
                    graphics.fill(cellRight - 1, getY() + 3, cellRight, getY() + getHeight() - 3, 0xFF46505A);
                }
            }

            if (newHoveredMember != hoveredMember) {
                hoveredMember = newHoveredMember;
                if (hoveredMember >= 0) {
                    BlockPos position = model.targets().get(hoveredMember).position();
                    setTooltip(Tooltip.create(Component.translatable(
                            "cbs.tabs.group.member",
                            hoveredMember + 1,
                            count,
                            position.getX(),
                            position.getY(),
                            position.getZ()
                    )));
                } else {
                    setTooltip(null);
                }
            }
        }

        private int memberAt(double mouseX) {
            int count = model.targets().size();
            if (count == 0 || mouseX < getX() + 4 || mouseX >= getX() + getWidth()) {
                return -1;
            }
            int contentWidth = Math.max(count, getWidth() - 5);
            int relative = (int) mouseX - (getX() + 4);
            return Mth.clamp(relative * count / contentWidth, 0, count - 1);
        }
    }
}
