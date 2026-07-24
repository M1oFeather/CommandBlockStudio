package com.miofeather.commandblockstudio.main.ui.screen;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.miofeather.commandblockstudio.main.config.ConfigScreen;
import com.miofeather.commandblockstudio.main.insight.CommandDiagnostic;
import com.miofeather.commandblockstudio.main.insight.CommandSyntaxHint;
import com.miofeather.commandblockstudio.main.network.CommandBlockAnnotationNetwork;
import com.miofeather.commandblockstudio.main.ui.CyclingTexturedButtonWidget;
import com.miofeather.commandblockstudio.main.ui.MultiLineCommandSuggestor;
import com.miofeather.commandblockstudio.main.ui.MultiLineTextFieldWidget;
import com.miofeather.commandblockstudio.main.ui.SideWindow;
import com.miofeather.commandblockstudio.main.ui.StudioInsightPanel;
import com.miofeather.commandblockstudio.main.ui.StudioAnnotationPanel;
import com.miofeather.commandblockstudio.main.ui.StudioInputEvents;
import com.miofeather.commandblockstudio.main.ui.StudioIconButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

import static com.miofeather.commandblockstudio.main.CommandBlockStudio.*;

public abstract class AbstractCommandBlockStudioScreen extends StudioScaledScreen {
    protected enum BottomPanelMode {
        NONE,
        PROBLEMS,
        OUTPUT
    }

    protected enum RightPanelMode {
        DOCS,
        TOOLS,
        ANNOTATION
    }

    protected class CommandBlockState {
        public CommandBlockEntity.Mode type = CommandBlockEntity.Mode.REDSTONE;
        boolean conditional;
        public boolean needsRedstone = true;
        public boolean trackOutput = true;

        public CommandBlockState(CommandBlockEntity.Mode type, boolean conditional, boolean needsRedstone, boolean trackOutput) {
            this.type = type;
            this.conditional = conditional;
            this.needsRedstone = needsRedstone;
            this.trackOutput = trackOutput;
        }
    }

    protected static final int BUTTON_HEIGHT = 20;
    protected static final int CYCLE_BUTTON_WIDTH = 20;
    protected static final int TOP_BAR_HEIGHT = 34;
    protected static final int EDITOR_TAB_STRIP_HEIGHT = 24;
    protected static final int RAIL_WIDTH = 38;
    protected static final int ACTION_BAR_HEIGHT = 30;
    private static final int RECLAIMED_STATUS_HEIGHT = 18;
    private static final int TEXT_FIELD_GUTTER = 5;
    private static final int MIN_TOOLS_PANEL_HEIGHT = 188;
    private static final long AUTO_SAVE_DELAY_MILLIS = 350L;

    protected CommandBlockState priorState;
    protected EditBox consoleCommandTextField;
    protected EditBox previousOutputTextField;
    protected Button doneButton;
    protected Button cancelButton;
    protected StudioIconButton runTestButton;
    protected Button configButton;
    protected Button editorRailButton;
    protected Button problemsRailButton;
    protected Button outputRailButton;
    protected Button docsRailButton;
    protected Button showSideWindowButton;
    protected Button annotationRailButton;
    protected Button bottomProblemsButton;
    protected Button bottomOutputButton;
    protected Button bottomCloseButton;
    protected Button diagnosticJumpButton;
    protected CyclingTexturedButtonWidget<Boolean> toggleTrackingOutputButton;
    protected Checkbox setTrackingOutputDefaultCheckbox;
    protected Checkbox setShowOutputDefaultCheckbox;
    protected SideWindow sideWindow;
    protected StudioInsightPanel insightPanel;
    protected StudioAnnotationPanel annotationPanel;
    protected BaseCommandBlock commandExecutor;
    protected CommandSuggestions commandSuggestor;
    protected boolean trackOutput = true;
    protected boolean showOutput = SHOW_OUTPUT_DEFAULT;
    protected boolean updated;
    protected boolean closing;

    protected int editorX;
    protected int editorY;
    protected int editorWidth;
    protected int editorHeight;
    protected int workspaceBottom;
    protected int bottomPanelX;
    protected int bottomPanelY;
    protected int bottomPanelWidth;
    protected int bottomPanelHeight;
    protected int rightPanelX;
    protected int rightPanelY;
    protected int rightPanelWidth;
    protected int rightPanelHeight;
    protected int contextControlsX;
    private boolean rightPanelOverlay;
    private boolean rightPanelOpen;
    private boolean layoutInitialized;
    private long autoSaveDueAt = -1L;

    private BottomPanelMode bottomPanelMode = SHOW_OUTPUT_DEFAULT ? BottomPanelMode.OUTPUT : BottomPanelMode.NONE;
    private RightPanelMode rightPanelMode = RightPanelMode.DOCS;
    private String configCommandBuffer = "";
    private int configCursorPosition;
    private int configHighlightPosition;
    private boolean configCommandModified;

    protected AbstractCommandBlockStudioScreen() {
        super(GameNarrator.NO_TITLE);
    }

    @Override
    public void init() {
        prepareStudioScale();
        priorState = new CommandBlockState(CommandBlockEntity.Mode.REDSTONE, false, false, commandExecutor.isTrackOutput());
        trackOutput = TRACK_OUTPUT_DEFAULT_USED ? TRACK_OUTPUT_DEFAULT_VALUE : commandExecutor.isTrackOutput();
        calculateLayout();
        createTopAndRailControls();
        createEditor();
        createBottomPanelControls();
        createRightPanels();
        createActionControls();
        updatePanelVisibility();

        setInitialFocus(consoleCommandTextField);
        consoleCommandTextField.setFocused(true);
        layoutInitialized = true;
    }

    private void calculateLayout() {
        contextControlsX = Math.max(150, Math.min(190, this.width / 4));
        workspaceBottom = this.height - ACTION_BAR_HEIGHT;
        rightPanelY = TOP_BAR_HEIGHT + 4;
        rightPanelHeight = Math.max(80, workspaceBottom - rightPanelY - 4);
        rightPanelOverlay = this.width < 560;
        rightPanelWidth = this.width >= 720
                ? 210
                : this.width >= 560
                ? 168
                : Math.min(210, Math.max(168, this.width - RAIL_WIDTH - 18));
        rightPanelX = this.width - rightPanelWidth - 4;

        editorX = RAIL_WIDTH + 5;
        int editorBoundary = !rightPanelOpen || rightPanelOverlay ? this.width : rightPanelX;
        int editorRight = editorBoundary - TEXT_FIELD_GUTTER;
        editorWidth = Math.max(140, editorRight - editorX);
        editorY = TOP_BAR_HEIGHT + EDITOR_TAB_STRIP_HEIGHT + 3;

        bottomPanelX = editorX;
        bottomPanelWidth = editorWidth;
        bottomPanelHeight = bottomPanelMode == BottomPanelMode.NONE
                ? 0
                : Mth.clamp(this.height / 6 + RECLAIMED_STATUS_HEIGHT, 80, 104);
        bottomPanelY = workspaceBottom - bottomPanelHeight - 4;
        editorHeight = Math.max(52, bottomPanelY - editorY - TEXT_FIELD_GUTTER);
    }

    private void createTopAndRailControls() {
        Component[] trackOutputTooltips = {
                Component.translatable("cbs.trackOutput.true"),
                Component.translatable("cbs.trackOutput.false")
        };
        toggleTrackingOutputButton = addRenderableWidget(new CyclingTexturedButtonWidget<>(
                getContextControlX(3),
                getContextControlY(),
                CYCLE_BUTTON_WIDTH,
                BUTTON_HEIGHT,
                Component.empty(),
                button -> {
                    trackOutput = ((CyclingTexturedButtonWidget<Boolean>) button).getValue();
                    commandExecutor.setTrackOutput(trackOutput);
                    setPreviousOutputText(trackOutput);
                },
                new WidgetSprites[]{BUTTON_TRACK_OUTPUT, BUTTON_IGNORE_OUTPUT},
                trackOutput ? 0 : 1,
                new Boolean[]{true, false},
                trackOutputTooltips
        ));

        configButton = addRenderableWidget(Button.builder(Component.translatable("cbs.config.short"), button -> openConfig())
                .bounds(this.width - 62, 7, 56, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("cbs.config")))
                .build());

        int railX = 5;
        int railY = TOP_BAR_HEIGHT + 7;
        editorRailButton = createRailButton(ICON_EDITOR, "cbs.rail.editor", railX, railY, button -> {
            closeRightPanel();
            setBottomPanelMode(BottomPanelMode.NONE);
        });
        docsRailButton = createRailButton(ICON_DOCS, "cbs.rail.docs", railX, railY + 24, button -> setRightPanelMode(RightPanelMode.DOCS));
        problemsRailButton = createRailButton(ICON_PROBLEMS, "cbs.rail.problems", railX, railY + 48, button -> {
            closeRightPanel();
            setBottomPanelMode(BottomPanelMode.PROBLEMS);
        });
        outputRailButton = createRailButton(ICON_OUTPUT, "cbs.rail.output", railX, railY + 72, button -> {
            closeRightPanel();
            setBottomPanelMode(BottomPanelMode.OUTPUT);
        });
        showSideWindowButton = createRailButton(ICON_TOOLS, "cbs.rail.tools", railX, railY + 96, button -> setRightPanelMode(RightPanelMode.TOOLS));
        annotationRailButton = createRailButton(ICON_ANNOTATION, "cbs.rail.annotation", railX, railY + 120, button -> {
            setRightPanelMode(RightPanelMode.ANNOTATION);
            requestAnnotation();
        });
    }

    private Button createRailButton(
            net.minecraft.resources.Identifier icon,
            String tooltipKey,
            int x,
            int y,
            Button.OnPress action
    ) {
        StudioIconButton button = new StudioIconButton(
                x,
                y,
                28,
                BUTTON_HEIGHT,
                Component.translatable(tooltipKey),
                icon,
                14,
                true,
                action
        );
        button.setProminentBackground(true);
        button.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        return addRenderableWidget(button);
    }

    private void createEditor() {
        consoleCommandTextField = new MultiLineTextFieldWidget(
                font,
                editorX,
                editorY,
                editorWidth,
                editorHeight,
                Component.translatable("advMode.command"),
                this
        ) {
            @Override
            protected MutableComponent createNarrationMessage() {
                return super.createNarrationMessage().append(commandSuggestor.getNarrationMessage());
            }
        };
        commandSuggestor = new MultiLineCommandSuggestor(
                minecraft,
                this,
                consoleCommandTextField,
                font,
                true,
                true,
                0,
                7,
                false,
                Integer.MIN_VALUE
        );
        commandSuggestor.setAllowSuggestions(true);
        commandSuggestor.updateCommandInfo();
        ((MultiLineTextFieldWidget) consoleCommandTextField).setCommandSuggestor((MultiLineCommandSuggestor) commandSuggestor);
        consoleCommandTextField.setMaxLength(32500);
        ((MultiLineTextFieldWidget) consoleCommandTextField).setRawText(commandExecutor.getCommand());
        consoleCommandTextField.setResponder(this::onCommandChanged);
        addWidget(consoleCommandTextField);

        previousOutputTextField = new MultiLineTextFieldWidget(
                font,
                bottomPanelX + 5,
                bottomPanelY + 27,
                bottomPanelWidth - 10,
                Math.max(24, bottomPanelHeight - 32),
                Component.translatable("advMode.previousOutput"),
                this
        );
        previousOutputTextField.setMaxLength(32500);
        previousOutputTextField.setEditable(false);
        addWidget(previousOutputTextField);
        setPreviousOutputText(trackOutput);
    }

    private void createBottomPanelControls() {
        bottomProblemsButton = addRenderableWidget(Button.builder(
                        Component.translatable("cbs.panel.problems"),
                        button -> setBottomPanelMode(BottomPanelMode.PROBLEMS))
                .bounds(bottomPanelX + 5, bottomPanelY + 3, 58, 18)
                .build());
        bottomOutputButton = addRenderableWidget(Button.builder(
                        Component.translatable("cbs.panel.output"),
                        button -> setBottomPanelMode(BottomPanelMode.OUTPUT))
                .bounds(bottomPanelX + 66, bottomPanelY + 3, 58, 18)
                .build());
        bottomCloseButton = addRenderableWidget(Button.builder(
                        Component.literal("x"),
                        button -> setBottomPanelMode(BottomPanelMode.NONE))
                .bounds(bottomPanelX + bottomPanelWidth - 23, bottomPanelY + 3, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("cbs.panel.close")))
                .build());
        diagnosticJumpButton = addRenderableWidget(Button.builder(
                        Component.literal("↪"),
                        button -> jumpToCurrentDiagnostic())
                .bounds(bottomPanelX + bottomPanelWidth - 44, bottomPanelY + 3, 18, 18)
                .tooltip(Tooltip.create(Component.translatable("cbs.diagnostic.jump")))
                .build());

        setTrackingOutputDefaultCheckbox = addRenderableWidget(
                Checkbox.builder(Component.translatable("cbs.panel.output.trackDefault"), font)
                        .pos(bottomPanelX + 134, bottomPanelY + 3)
                        .selected(TRACK_OUTPUT_DEFAULT_USED)
                        .onValueChange((checkbox, checked) -> {
                            CommandBlockStudio.setConfig(VAR_TRACK_OUTPUT_DEFAULT_USED, String.valueOf(checked));
                            CommandBlockStudio.setConfig(VAR_TRACK_OUTPUT_DEFAULT_VALUE, String.valueOf(trackOutput));
                        })
                        .tooltip(Tooltip.create(Component.translatable("cbs.trackOutput.setDefault")))
                        .build());

        setShowOutputDefaultCheckbox = addRenderableWidget(
                Checkbox.builder(Component.translatable("cbs.panel.output.openDefault"), font)
                        .pos(bottomPanelX + 300, bottomPanelY + 3)
                        .selected(SHOW_OUTPUT_DEFAULT)
                        .onValueChange((checkbox, checked) -> CommandBlockStudio.setConfig(VAR_SHOW_OUTPUT_DEFAULT, String.valueOf(checked)))
                        .tooltip(Tooltip.create(Component.translatable("cbs.view.outputDefault")))
                        .build());
    }

    private void createRightPanels() {
        int panelWidth = Math.max(80, rightPanelWidth);
        sideWindow = new SideWindow(
                rightPanelX,
                rightPanelY,
                Math.max(112, panelWidth),
                rightPanelHeight,
                (MultiLineTextFieldWidget) consoleCommandTextField,
                this
        );
        insightPanel = new StudioInsightPanel(
                font,
                (MultiLineTextFieldWidget) consoleCommandTextField,
                (MultiLineCommandSuggestor) commandSuggestor,
                this,
                rightPanelX,
                rightPanelY,
                panelWidth,
                rightPanelHeight
        );
        annotationPanel = new StudioAnnotationPanel(
                font,
                rightPanelX,
                rightPanelY,
                panelWidth,
                rightPanelHeight,
                this::saveAnnotation,
                this::requestHistoryRestore
        );
    }

    private void createActionControls() {
        int actionY = this.height - ACTION_BAR_HEIGHT + 5;
        int x = this.width - 6;
        cancelButton = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> cancelAndClose())
                .bounds(x - 56, actionY, 56, BUTTON_HEIGHT)
                .build());
        x -= 60;
        doneButton = addRenderableWidget(Button.builder(Component.translatable("cbs.action.save"), button -> commitAndClose())
                .bounds(x - 68, actionY, 68, BUTTON_HEIGHT)
                .build());
        x -= 72;
        if (supportsRunTest()) {
            runTestButton = addRenderableWidget(new StudioIconButton(
                    x - 28,
                    actionY,
                    28,
                    BUTTON_HEIGHT,
                    Component.translatable("cbs.runTest"),
                    ICON_RUN,
                    14,
                    true,
                    button -> runTest()
            ));
            runTestButton.setProminentBackground(true);
            updateRunTestButton(false);
        }
    }

    protected boolean supportsRunTest() {
        return false;
    }

    protected boolean canRunTest() {
        return false;
    }

    protected void runTest() {
    }

    private void updateRunTestButton(boolean editorActive) {
        if (runTestButton == null) {
            return;
        }
        boolean available = canRunTest();
        runTestButton.active = editorActive && available;
        runTestButton.setTooltip(Tooltip.create(Component.translatable(
                available ? "cbs.runTest.tooltip" : "cbs.runTest.serverRequired"
        )));
    }

    protected void setBottomPanelMode(BottomPanelMode mode) {
        bottomPanelMode = mode;
        showOutput = mode == BottomPanelMode.OUTPUT;
        if (consoleCommandTextField != null) {
            calculateLayout();
            applyDynamicLayout();
            updatePanelVisibility();
            if (mode == BottomPanelMode.NONE) {
                consoleCommandTextField.setFocused(true);
            }
        }
    }

    protected void setRightPanelMode(RightPanelMode mode) {
        rightPanelMode = mode;
        rightPanelOpen = true;
        if (rightPanelOverlay && bottomPanelMode != BottomPanelMode.NONE) {
            bottomPanelMode = BottomPanelMode.NONE;
            showOutput = false;
        }
        calculateLayout();
        applyDynamicLayout();
        updatePanelVisibility();
    }

    private void closeRightPanel() {
        if (!rightPanelOpen) {
            return;
        }
        rightPanelOpen = false;
        calculateLayout();
        applyDynamicLayout();
        updatePanelVisibility();
    }

    private void applyDynamicLayout() {
        consoleCommandTextField.setX(editorX);
        consoleCommandTextField.setY(editorY);
        consoleCommandTextField.setWidth(editorWidth);
        consoleCommandTextField.setHeight(editorHeight);
        previousOutputTextField.setX(bottomPanelX + 5);
        previousOutputTextField.setY(bottomPanelY + 27);
        previousOutputTextField.setWidth(bottomPanelWidth - 10);
        previousOutputTextField.setHeight(Math.max(24, bottomPanelHeight - 32));
        bottomProblemsButton.setPosition(bottomPanelX + 5, bottomPanelY + 3);
        bottomOutputButton.setPosition(bottomPanelX + 66, bottomPanelY + 3);
        bottomCloseButton.setPosition(bottomPanelX + bottomPanelWidth - 23, bottomPanelY + 3);
        diagnosticJumpButton.setPosition(bottomPanelX + bottomPanelWidth - 44, bottomPanelY + 3);
        setTrackingOutputDefaultCheckbox.setPosition(bottomPanelX + 134, bottomPanelY + 3);
        setShowOutputDefaultCheckbox.setPosition(bottomPanelX + 300, bottomPanelY + 3);
        commandSuggestor.updateCommandInfo();
        onDynamicLayoutApplied();
    }

    protected void onDynamicLayoutApplied() {
    }

    private void updatePanelVisibility() {
        boolean bottomVisible = bottomPanelMode != BottomPanelMode.NONE;
        boolean outputVisible = bottomPanelMode == BottomPanelMode.OUTPUT;
        bottomProblemsButton.visible = bottomVisible;
        bottomOutputButton.visible = bottomVisible;
        bottomCloseButton.visible = bottomVisible;
        diagnosticJumpButton.visible = false;
        previousOutputTextField.setVisible(outputVisible);
        boolean outputOptionsVisible = outputVisible && bottomPanelWidth >= 500;
        setTrackingOutputDefaultCheckbox.visible = outputOptionsVisible;
        setShowOutputDefaultCheckbox.visible = outputOptionsVisible;

        boolean toolsAvailable = rightPanelHeight >= MIN_TOOLS_PANEL_HEIGHT;
        showSideWindowButton.visible = toolsAvailable;
        boolean annotationAvailable = isAnnotationAvailable();
        annotationRailButton.visible = annotationAvailable;
        if (!toolsAvailable && rightPanelMode == RightPanelMode.TOOLS) {
            rightPanelMode = RightPanelMode.DOCS;
        }
        boolean rightVisible = rightPanelOpen;
        insightPanel.setVisible(rightVisible && rightPanelMode == RightPanelMode.DOCS);
        sideWindow.setVisible(rightVisible && toolsAvailable && rightPanelMode == RightPanelMode.TOOLS);
        annotationPanel.setVisible(rightVisible && annotationAvailable && rightPanelMode == RightPanelMode.ANNOTATION);
    }

    protected Optional<BlockPos> getAnnotationTarget() {
        return Optional.empty();
    }

    private boolean isAnnotationAvailable() {
        return getAnnotationTarget().isPresent()
                && minecraft != null
                && minecraft.getConnection() != null
                && minecraft.getConnection().hasChannel(CommandBlockAnnotationNetwork.RequestAnnotation.TYPE)
                && minecraft.getConnection().hasChannel(CommandBlockAnnotationNetwork.UpdateAnnotation.TYPE);
    }

    private void requestAnnotation() {
        if (!isAnnotationAvailable()) {
            return;
        }
        getAnnotationTarget().ifPresent(pos -> {
            CommandBlockAnnotationNetwork.clearReceived(pos);
            annotationPanel.beginLoading();
            minecraft.getConnection().send(new CommandBlockAnnotationNetwork.RequestAnnotation(pos));
        });
    }

    private void saveAnnotation(String annotation) {
        if (!isAnnotationAvailable()) {
            return;
        }
        getAnnotationTarget().ifPresent(pos -> minecraft.getConnection().send(
                new CommandBlockAnnotationNetwork.UpdateAnnotation(pos, annotation)
        ));
    }

    private void requestHistoryRestore(CommandBlockAnnotationNetwork.EditHistoryEntry entry) {
        if (minecraft == null || !entry.snapshotAvailable()) {
            return;
        }
        EditorSessionState sessionState = captureEditorSession();
        minecraft.gui.setScreen(new ConfirmScreen(
                restore -> {
                    resumeEditorSession(sessionState);
                    if (restore) {
                        restoreHistorySnapshot(entry);
                        commitSilently();
                    }
                },
                Component.translatable("cbs.annotation.history.revert.title"),
                Component.translatable("cbs.annotation.history.revert.message"),
                Component.translatable("cbs.annotation.history.revert.confirm"),
                CommonComponents.GUI_CANCEL
        ));
    }

    private void restoreHistorySnapshot(CommandBlockAnnotationNetwork.EditHistoryEntry entry) {
        MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
        editor.setValue(entry.command());
        trackOutput = entry.trackOutput();
        toggleTrackingOutputButton.setIndex(entry.trackOutput() ? 0 : 1);
        restoreHistoryTargetState(entry);
        commandSuggestor.updateCommandInfo();
        consoleCommandTextField.setFocused(true);
    }

    protected void restoreHistoryTargetState(CommandBlockAnnotationNetwork.EditHistoryEntry entry) {
    }

    private void refreshAnnotationSnapshot() {
        if (!isAnnotationAvailable()) {
            return;
        }
        getAnnotationTarget().ifPresent(pos -> minecraft.getConnection().send(
                new CommandBlockAnnotationNetwork.RequestAnnotation(pos)
        ));
    }

    @Override
    public void tick() {
        super.tick();
        if (AUTOSAVE && autoSaveDueAt >= 0L && Util.getMillis() >= autoSaveDueAt && wasModified()) {
            autoSaveDueAt = -1L;
            commitSilently();
        }
        if (annotationPanel != null) {
            getAnnotationTarget().flatMap(CommandBlockAnnotationNetwork::takeReceived)
                    .ifPresent(annotationPanel::acceptRemoteValue);
        }
    }

    public boolean isQuickDocsVisible() {
        return insightPanel != null && insightPanel.isVisible();
    }

    public int getContextControlX(int index) {
        return contextControlsX + index * 24;
    }

    public int getContextControlY() {
        return 7;
    }

    protected int getChainRailX() {
        return 9;
    }

    protected int getChainRailStartY() {
        return TOP_BAR_HEIGHT + 137;
    }

    protected Component getTargetDescription() {
        return Component.translatable("cbs.target.commandBlock");
    }

    @Override
    public void onClose() {
        if (closing) {
            super.onClose();
            return;
        }
        if (AUTOSAVE) {
            if (wasModified()) {
                commitSilently();
            }
            closing = true;
            super.onClose();
            return;
        }
        if (CONFIRM_UNSAVED_EXIT && wasModified() && minecraft != null) {
            EditorSessionState sessionState = captureEditorSession();
            minecraft.gui.setScreen(new ConfirmScreen(
                    save -> {
                        resumeEditorSession(sessionState);
                        if (save) {
                            commitAndClose();
                        }
                    },
                    Component.translatable("cbs.unsaved.title"),
                    Component.translatable("cbs.unsaved.message"),
                    Component.translatable("cbs.unsaved.saveExit"),
                    Component.translatable("cbs.unsaved.continue")
            ));
            return;
        }
        restoreLocalStateBeforeDiscard();
        closing = true;
        super.onClose();
    }

    protected final EditorSessionState captureEditorSession() {
        MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
        CommandBlockState savedPriorState = new CommandBlockState(
                priorState.type,
                priorState.conditional,
                priorState.needsRedstone,
                priorState.trackOutput
        );
        return new EditorSessionState(
                editor.getValue(),
                editor.getCursorPosition(),
                editor.getHighlightPosition(),
                editor.wasModified(),
                toggleTrackingOutputButton.getValue(),
                savedPriorState
        );
    }

    protected final void resumeEditorSession(EditorSessionState sessionState) {
        minecraft.gui.setScreen(this);
        ((MultiLineTextFieldWidget) consoleCommandTextField).restoreEditorState(
                sessionState.command(),
                sessionState.cursor(),
                sessionState.highlight(),
                sessionState.modified()
        );
        priorState = sessionState.priorState();
        trackOutput = sessionState.trackOutput();
        toggleTrackingOutputButton.setIndex(trackOutput ? 0 : 1);
        setPreviousOutputText(trackOutput);
        restoreTargetControlsAfterReinit();
        commandSuggestor.updateCommandInfo();
        setButtonsActive(true);
        consoleCommandTextField.setFocused(true);
    }

    protected void restoreTargetControlsAfterReinit() {
    }

    @Override
    public void resize(int width, int height) {
        MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
        String value = editor.getValue();
        int cursor = editor.getCursorPosition();
        int highlight = editor.getHighlightPosition();
        boolean modified = editor.wasModified();
        this.init(width, height);
        ((MultiLineTextFieldWidget) consoleCommandTextField).restoreEditorState(value, cursor, highlight, modified);
        commandSuggestor.updateCommandInfo();
        setButtonsActive(true);
    }

    public void openConfig() {
        if (minecraft == null) {
            return;
        }
        MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
        configCommandBuffer = editor.getValue();
        configCursorPosition = editor.getCursorPosition();
        configHighlightPosition = editor.getHighlightPosition();
        configCommandModified = editor.wasModified();
        minecraft.gui.setScreen(new ConfigScreen(this));
    }

    public void returnFromConfig() {
        ((MultiLineTextFieldWidget) consoleCommandTextField).restoreEditorState(
                configCommandBuffer,
                configCursorPosition,
                configHighlightPosition,
                configCommandModified
        );
    }

    public void sideWindowFocused() {
        consoleCommandTextField.setFocused(false);
    }

    public boolean scroll(double amount) {
        return commandSuggestor.mouseScrolled(Mth.clamp(amount, -1.0, 1.0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        if (annotationPanel.isVisible() && annotationPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (insightPanel.isVisible() && insightPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (sideWindow.isVisible() && sideWindow.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (commandSuggestor.isVisible() && commandSuggestor.mouseClicked(StudioInputEvents.mouse(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void jumpToCurrentDiagnostic() {
        MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
        Optional<CommandDiagnostic> diagnostic = ((MultiLineCommandSuggestor) commandSuggestor)
                .getInsightService()
                .getDiagnostic(editor.getValue());
        diagnostic.ifPresent(editor::jumpToDiagnostic);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        deltaX = studioMouseDelta(deltaX);
        deltaY = studioMouseDelta(deltaY);
        if (button == 0 && annotationPanel.isVisible()
                && annotationPanel.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        if (button == 0 && sideWindow.isVisible() && sideWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        if (annotationPanel.isVisible() && annotationPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (sideWindow.isVisible() && sideWindow.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        mouseX = studioMouseX(mouseX);
        mouseY = studioMouseY(mouseY);
        if (annotationPanel.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (insightPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            setRightPanelMode(RightPanelMode.TOOLS);
            sideWindow.focusSearch(false);
            return true;
        }
        if (Minecraft.getInstance().hasControlDown() && keyCode == GLFW.GLFW_KEY_H) {
            setRightPanelMode(RightPanelMode.TOOLS);
            sideWindow.focusSearch(true);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F3 && sideWindow.isVisible()) {
            return sideWindow.repeatSearch(Minecraft.getInstance().hasShiftDown());
        }
        if (annotationPanel.isVisible() && annotationPanel.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (sideWindow.isVisible() && sideWindow.isFocused() && sideWindow.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!showOutput && keyCode == GLFW.GLFW_KEY_TAB && !commandSuggestor.isVisible()) {
            MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
            if (!Minecraft.getInstance().hasShiftDown() && editor.acceptSyntaxHint()) {
                return true;
            }
            if (editor.selectNextPlaceholder(Minecraft.getInstance().hasShiftDown())) {
                return true;
            }
        }
        if (commandSuggestor.keyPressed(StudioInputEvents.key(keyCode, scanCode, modifiers))) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_S && Minecraft.getInstance().hasControlDown()) {
            if (((MultiLineTextFieldWidget) consoleCommandTextField).wasModified()) {
                commit();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (annotationPanel.isVisible() && annotationPanel.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (sideWindow.isVisible() && sideWindow.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    protected void onCommandChanged(String command) {
        commandSuggestor.updateCommandInfo();
        if (AUTOSAVE) {
            autoSaveDueAt = Util.getMillis() + AUTO_SAVE_DELAY_MILLIS;
        }
    }

    protected void setButtonsActive(boolean active) {
        doneButton.active = active;
        updateRunTestButton(active);
        toggleTrackingOutputButton.active = active;
        consoleCommandTextField.setEditable(active);
        configButton.active = active;
    }

    protected void setPreviousOutputText(boolean trackOutput) {
        ((MultiLineTextFieldWidget) previousOutputTextField).setRawText(
                trackOutput ? commandExecutor.getLastOutput().getString() : "-"
        );
    }

    protected void commit() {
        autoSaveDueAt = -1L;
        trackOutput = toggleTrackingOutputButton.getValue();
        syncSettingsToServer(commandExecutor);
        refreshAnnotationSnapshot();
        if (!commandExecutor.isTrackOutput()) {
            commandExecutor.setLastOutput(null);
        }
        ((MultiLineTextFieldWidget) consoleCommandTextField).resetModified();
        priorState.trackOutput = trackOutput;
    }

    protected final void commitSilently() {
        CommandBlockStudio.suppressNextCommandSaveMessage();
        commit();
    }

    protected void commitAndClose() {
        commit();
        closing = true;
        onClose();
    }

    private void cancelAndClose() {
        closing = true;
        restoreLocalStateBeforeDiscard();
        super.onClose();
    }

    protected void restoreLocalStateBeforeDiscard() {
        commandExecutor.setTrackOutput(priorState.trackOutput);
    }

    protected boolean wasModified() {
        return ((MultiLineTextFieldWidget) consoleCommandTextField).wasModified()
                || toggleTrackingOutputButton.getValue() != priorState.trackOutput;
    }

    protected abstract void syncSettingsToServer(BaseCommandBlock commandExecutor);

    protected record EditorSessionState(
            String command,
            int cursor,
            int highlight,
            boolean modified,
            boolean trackOutput,
            CommandBlockState priorState
    ) {
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // The Studio draws an opaque workbench background before vanilla widgets render.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int studioMouseX = studioMouseX(mouseX);
        int studioMouseY = studioMouseY(mouseY);
        beginStudioRender(graphics);
        try {
            renderStudioChrome(graphics);
            consoleCommandTextField.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
            if (previousOutputTextField.isVisible()) {
                previousOutputTextField.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
            }
            insightPanel.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
            sideWindow.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
            annotationPanel.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
            super.extractRenderState(graphics, studioMouseX, studioMouseY, delta);
            renderRailSelection(graphics);
            renderAsterisk(graphics, toggleTrackingOutputButton, toggleTrackingOutputButton.getValue() != priorState.trackOutput);
            renderTargetOverlay(graphics, studioMouseX, studioMouseY, delta);
        } finally {
            endStudioRender(graphics);
        }
    }

    protected void renderTargetOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
    }

    private void renderStudioChrome(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, height, 0xFF0B0E12);
        graphics.fill(0, 0, width, TOP_BAR_HEIGHT, 0xFF20242A);
        graphics.fill(0, TOP_BAR_HEIGHT, RAIL_WIDTH, height - ACTION_BAR_HEIGHT, 0xFF171A1F);
        graphics.fill(RAIL_WIDTH - 1, TOP_BAR_HEIGHT, RAIL_WIDTH, height - ACTION_BAR_HEIGHT, 0xFF343A42);
        graphics.fill(0, height - ACTION_BAR_HEIGHT, width, height, 0xFF20242A);

        graphics.text(font, Component.literal("Command Block Studio").withStyle(ChatFormatting.WHITE), 9, 12, 0xFFF2F4F7);
        int targetX = getContextControlX(4) + 8;
        int targetWidth = configButton.getX() - targetX - 8;
        if (targetWidth >= 32) {
            String target = font.plainSubstrByWidth(getTargetDescription().getString(), targetWidth);
            graphics.text(font, target, targetX, 12, 0xFF9AA4AF);
        }

        graphics.fill(editorX, TOP_BAR_HEIGHT + 4, editorX + editorWidth, editorY - 3, 0xFF181C21);
        graphics.fill(editorX, editorY - 3, editorX + editorWidth, editorY - 2, 0xFF343A42);
        renderEditorHeader(graphics);

        if (bottomPanelMode != BottomPanelMode.NONE) {
            graphics.fill(bottomPanelX, bottomPanelY, bottomPanelX + bottomPanelWidth, workspaceBottom - 4, 0xFF12161B);
            graphics.fill(bottomPanelX, bottomPanelY, bottomPanelX + bottomPanelWidth, bottomPanelY + 24, 0xFF1C2026);
            graphics.fill(bottomPanelX, bottomPanelY, bottomPanelX + bottomPanelWidth, bottomPanelY + 1, 0xFF3A424C);
            if (bottomPanelMode == BottomPanelMode.PROBLEMS) {
                renderProblemsPanel(graphics);
            }
        }

        renderActionStatus(graphics);
    }

    protected void renderEditorHeader(GuiGraphicsExtractor graphics) {
        graphics.text(font, Component.translatable("cbs.editor.file"), editorX + 7, TOP_BAR_HEIGHT + 10, 0xFFDDE2E7);
        String length = Component.translatable(
                "cbs.editor.characters",
                consoleCommandTextField.getValue().length()
        ).getString();
        graphics.text(font, length, editorX + editorWidth - font.width(length) - 7, TOP_BAR_HEIGHT + 10, 0xFF707A85);

    }

    private void renderProblemsPanel(GuiGraphicsExtractor graphics) {
        int contentX = bottomPanelX + 9;
        int contentY = bottomPanelY + 31;
        int textWidth = bottomPanelWidth - 18;
        String command = consoleCommandTextField.getValue();
        Component message;
        int color;
        diagnosticJumpButton.visible = false;
        if (command.isBlank()) {
            message = Component.translatable("cbs.problems.empty");
            color = 0xFF808A95;
        } else {
            var insightService = ((MultiLineCommandSuggestor) commandSuggestor).getInsightService();
            var placeholder = insightService.describePlaceholder(consoleCommandTextField.getHighlighted());
            Optional<CommandDiagnostic> diagnostic = insightService.getDiagnostic(command);
            Optional<CommandSyntaxHint> hint = insightService.findSyntaxHint(
                    command,
                    consoleCommandTextField.getCursorPosition()
            );
            if (placeholder.isPresent()) {
                message = Component.translatable("cbs.problems.incomplete", placeholder.get().summary());
                color = 0xFFE3B341;
            } else if (hint.isPresent()) {
                message = Component.translatable("cbs.problems.incomplete", hint.get().summary());
                color = 0xFFE3B341;
            } else if (diagnostic.isPresent()) {
                MultiLineTextFieldWidget editor = (MultiLineTextFieldWidget) consoleCommandTextField;
                message = Component.translatable(
                        "cbs.problems.errorLocatedShort",
                        editor.getDiagnosticLocation(diagnostic.get()),
                        diagnostic.get().message()
                );
                color = 0xFFFF7B72;
                diagnosticJumpButton.visible = true;
                diagnosticJumpButton.active = true;
            } else {
                message = Component.translatable("cbs.problems.valid");
                color = 0xFF56D364;
            }
        }

        List<FormattedCharSequence> lines = font.split(message, textWidth);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            graphics.text(font, lines.get(i), contentX, contentY + i * (font.lineHeight + 2), color);
        }
    }

    private void renderActionStatus(GuiGraphicsExtractor graphics) {
        int x = RAIL_WIDTH + 7;
        int y = this.height - ACTION_BAR_HEIGHT + 11;
        int rightEdge = doneButton.getX() - 8;
        if (rightEdge <= x) {
            return;
        }

        MultiLineCommandSuggestor suggestor = (MultiLineCommandSuggestor) commandSuggestor;
        String command = consoleCommandTextField.getValue();
        String syntax;
        int syntaxColor;
        if (command.isBlank()) {
            syntax = Component.translatable("cbs.status.ready").getString();
            syntaxColor = 0xFF9AA4AF;
        } else if (suggestor.getInsightService().describePlaceholder(consoleCommandTextField.getHighlighted()).isPresent()
                || suggestor.getInsightService().findSyntaxHint(command, consoleCommandTextField.getCursorPosition()).isPresent()) {
            syntax = Component.translatable("cbs.status.incomplete").getString();
            syntaxColor = 0xFFE3B341;
        } else if (suggestor.getInsightService().getDiagnostic(command).isPresent()) {
            syntax = Component.translatable("cbs.status.error").getString();
            syntaxColor = 0xFFFF7B72;
        } else {
            syntax = Component.translatable("cbs.status.valid").getString();
            syntaxColor = 0xFF56D364;
        }

        String saved = Component.translatable(
                AUTOSAVE ? "cbs.status.autosave" : wasModified() ? "cbs.status.modified" : "cbs.status.saved"
        ).getString();
        String cursor = "Pos " + consoleCommandTextField.getCursorPosition();
        String separator = "  |  ";
        int availableWidth = rightEdge - x;
        String fullStatus = syntax + separator + saved + separator + cursor;
        if (font.width(fullStatus) <= availableWidth) {
            graphics.text(font, syntax, x, y, syntaxColor);
            x += font.width(syntax);
            graphics.text(font, separator, x, y, 0xFF68727D);
            x += font.width(separator);
            graphics.text(font, saved, x, y, wasModified() ? 0xFFE3B341 : 0xFFAAB3BD);
            x += font.width(saved);
            graphics.text(font, separator + cursor, x, y, 0xFF808A95);
            return;
        }

        String compactStatus = syntax + separator + saved;
        String visibleStatus = font.plainSubstrByWidth(compactStatus, availableWidth);
        graphics.text(font, visibleStatus, x, y, syntaxColor);
    }

    private void renderRailSelection(GuiGraphicsExtractor graphics) {
        int y;
        if (rightPanelMode == RightPanelMode.DOCS && isQuickDocsVisible()) {
            y = docsRailButton.getY();
        } else if (rightPanelMode == RightPanelMode.TOOLS && sideWindow.isVisible()) {
            y = showSideWindowButton.getY();
        } else if (rightPanelMode == RightPanelMode.ANNOTATION && annotationPanel.isVisible()) {
            y = annotationRailButton.getY();
        } else if (bottomPanelMode == BottomPanelMode.PROBLEMS) {
            y = problemsRailButton.getY();
        } else if (bottomPanelMode == BottomPanelMode.OUTPUT) {
            y = outputRailButton.getY();
        } else {
            y = editorRailButton.getY();
        }
        graphics.fill(1, y + 2, 3, y + BUTTON_HEIGHT - 2, 0xFF56B6C2);
    }

    protected void renderAsterisk(GuiGraphicsExtractor graphics, LayoutElement widget, boolean draw) {
        if (draw) {
            renderAsterisk(graphics, widget.getX() + widget.getWidth(), widget.getY() - 4, true);
        }
    }

    protected void renderAsterisk(GuiGraphicsExtractor graphics, int x, int y, boolean draw) {
        if (draw) {
            graphics.text(font, "*", x, y, 0xFFFFC000);
        }
    }
}
