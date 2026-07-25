package com.miofeather.commandblockstudio.main;

import com.miofeather.commandblockstudio.main.config.ConfigScreen;
import com.miofeather.commandblockstudio.main.config.CommandBlockStudioConfig;
import com.miofeather.commandblockstudio.main.client.CommandBlockItemTooltip;
import com.miofeather.commandblockstudio.main.client.CommandBlockScanner;
import com.miofeather.commandblockstudio.main.client.CommandBlockWorkMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.CommandBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@net.neoforged.api.distmarker.OnlyIn(Dist.CLIENT)
public class CommandBlockStudio {
    public static final String MODID = CommandBlockStudioMod.MODID;
    private static long suppressCommandSaveMessageUntil;

    public static final ResourceLocation SLIDER = ResourceLocation.parse("command_block_studio:slider");
    public static final ResourceLocation SLIDER_NOTCH = ResourceLocation.parse("command_block_studio:slider_notch");
    public static final ResourceLocation COMPASS_FRAME = ResourceLocation.parse("command_block_studio:compass_frame");
    public static final ResourceLocation COMPASS_NEEDLE = ResourceLocation.parse("command_block_studio:compass_needle");
    public static final ResourceLocation ICON_EDITOR = iconTexture("icon_editor");
    public static final ResourceLocation ICON_DOCS = iconTexture("icon_docs");
    public static final ResourceLocation ICON_PROBLEMS = iconTexture("icon_problems");
    public static final ResourceLocation ICON_OUTPUT = iconTexture("icon_output");
    public static final ResourceLocation ICON_TOOLS = iconTexture("icon_tools");
    public static final ResourceLocation ICON_ANNOTATION = iconTexture("icon_annotation");
    public static final ResourceLocation ICON_PIN = iconTexture("icon_pin");
    public static final ResourceLocation ICON_PIN_ACTIVE = iconTexture("icon_pin_active");
    public static final ResourceLocation ICON_ARROW_LEFT = iconTexture("icon_arrow_left");
    public static final ResourceLocation ICON_ARROW_RIGHT = iconTexture("icon_arrow_right");
    public static final ResourceLocation ICON_RUN = iconTexture("icon_run");

    public static final ResourceLocation ID_BLOCK_IMPULSE = ResourceLocation.parse("command_block_studio:block_impulse");
    public static final ResourceLocation ID_BLOCK_IMPULSE_FOCUSED = ResourceLocation.parse("command_block_studio:block_impulse_focused");
    public static final ResourceLocation ID_BLOCK_IMPULSE_CONDITIONAL = ResourceLocation.parse("command_block_studio:block_impulse_conditional");
    public static final ResourceLocation ID_BLOCK_IMPULSE_CONDITIONAL_FOCUSED = ResourceLocation.parse("command_block_studio:block_impulse_conditional_focused");
    public static final ResourceLocation ID_BLOCK_CHAIN = ResourceLocation.parse("command_block_studio:block_chain");
    public static final ResourceLocation ID_BLOCK_CHAIN_FOCUSED = ResourceLocation.parse("command_block_studio:block_chain_focused");
    public static final ResourceLocation ID_BLOCK_CHAIN_CONDITIONAL = ResourceLocation.parse("command_block_studio:block_chain_conditional");
    public static final ResourceLocation ID_BLOCK_CHAIN_CONDITIONAL_FOCUSED = ResourceLocation.parse("command_block_studio:block_chain_conditional_focused");
    public static final ResourceLocation ID_BLOCK_REPEAT = ResourceLocation.parse("command_block_studio:block_repeat");
    public static final ResourceLocation ID_BLOCK_REPEAT_FOCUSED = ResourceLocation.parse("command_block_studio:block_repeat_focused");
    public static final ResourceLocation ID_BLOCK_REPEAT_CONDITIONAL = ResourceLocation.parse("command_block_studio:block_repeat_conditional");
    public static final ResourceLocation ID_BLOCK_REPEAT_CONDITIONAL_FOCUSED = ResourceLocation.parse("command_block_studio:block_repeat_conditional_focused");
    public static final ResourceLocation ID_BUTTON_IMPULSE_DISABLED = ResourceLocation.parse("command_block_studio:button_impulse_disabled");
    public static final ResourceLocation ID_BUTTON_IMPULSE_ENABLED = ResourceLocation.parse("command_block_studio:button_impulse_enabled");
    public static final ResourceLocation ID_BUTTON_IMPULSE_FOCUSED = ResourceLocation.parse("command_block_studio:button_impulse_focused");
    public static final ResourceLocation ID_BUTTON_CHAIN_DISABLED = ResourceLocation.parse("command_block_studio:button_chain_disabled");
    public static final ResourceLocation ID_BUTTON_CHAIN_ENABLED = ResourceLocation.parse("command_block_studio:button_chain_enabled");
    public static final ResourceLocation ID_BUTTON_CHAIN_FOCUSED = ResourceLocation.parse("command_block_studio:button_chain_focused");
    public static final ResourceLocation ID_BUTTON_REPEAT_DISABLED = ResourceLocation.parse("command_block_studio:button_repeat_disabled");
    public static final ResourceLocation ID_BUTTON_REPEAT_ENABLED = ResourceLocation.parse("command_block_studio:button_repeat_enabled");
    public static final ResourceLocation ID_BUTTON_REPEAT_FOCUSED = ResourceLocation.parse("command_block_studio:button_repeat_focused");
    public static final ResourceLocation ID_BUTTON_COMMAND_DISABLED = ResourceLocation.parse("command_block_studio:button_command_disabled");
    public static final ResourceLocation ID_BUTTON_COMMAND_ENABLED = ResourceLocation.parse("command_block_studio:button_command_enabled");
    public static final ResourceLocation ID_BUTTON_COMMAND_FOCUSED = ResourceLocation.parse("command_block_studio:button_command_focused");
    public static final ResourceLocation ID_BUTTON_OUTPUT_DISABLED = ResourceLocation.parse("command_block_studio:button_output_disabled");
    public static final ResourceLocation ID_BUTTON_OUTPUT_ENABLED = ResourceLocation.parse("command_block_studio:button_output_enabled");
    public static final ResourceLocation ID_BUTTON_OUTPUT_FOCUSED = ResourceLocation.parse("command_block_studio:button_output_focused");
    public static final ResourceLocation ID_BUTTON_POWER_INACTIVE_DISABLED = ResourceLocation.parse("command_block_studio:button_power_inactive_disabled");
    public static final ResourceLocation ID_BUTTON_POWER_INACTIVE_ENABLED = ResourceLocation.parse("command_block_studio:button_power_inactive_enabled");
    public static final ResourceLocation ID_BUTTON_POWER_INACTIVE_FOCUSED = ResourceLocation.parse("command_block_studio:button_power_inactive_focused");
    public static final ResourceLocation ID_BUTTON_POWER_ACTIVE_DISABLED = ResourceLocation.parse("command_block_studio:button_power_active_disabled");
    public static final ResourceLocation ID_BUTTON_POWER_ACTIVE_ENABLED = ResourceLocation.parse("command_block_studio:button_power_active_enabled");
    public static final ResourceLocation ID_BUTTON_POWER_ACTIVE_FOCUSED = ResourceLocation.parse("command_block_studio:button_power_active_focused");
    public static final ResourceLocation ID_BUTTON_IGNORE_OUTPUT_DISABLED = ResourceLocation.parse("command_block_studio:button_ignore_output_disabled");
    public static final ResourceLocation ID_BUTTON_IGNORE_OUTPUT_ENABLED = ResourceLocation.parse("command_block_studio:button_ignore_output_enabled");
    public static final ResourceLocation ID_BUTTON_IGNORE_OUTPUT_FOCUSED = ResourceLocation.parse("command_block_studio:button_ignore_output_focused");
    public static final ResourceLocation ID_BUTTON_TRACK_OUTPUT_DISABLED = ResourceLocation.parse("command_block_studio:button_track_output_disabled");
    public static final ResourceLocation ID_BUTTON_TRACK_OUTPUT_ENABLED = ResourceLocation.parse("command_block_studio:button_track_output_enabled");
    public static final ResourceLocation ID_BUTTON_TRACK_OUTPUT_FOCUSED = ResourceLocation.parse("command_block_studio:button_track_output_focused");
    public static final ResourceLocation ID_BUTTON_UNCONDITIONAL_DISABLED = ResourceLocation.parse("command_block_studio:button_unconditional_disabled");
    public static final ResourceLocation ID_BUTTON_UNCONDITIONAL_ENABLED = ResourceLocation.parse("command_block_studio:button_unconditional_enabled");
    public static final ResourceLocation ID_BUTTON_UNCONDITIONAL_FOCUSED = ResourceLocation.parse("command_block_studio:button_unconditional_focused");
    public static final ResourceLocation ID_BUTTON_CONDITIONAL_DISABLED = ResourceLocation.parse("command_block_studio:button_conditional_disabled");
    public static final ResourceLocation ID_BUTTON_CONDITIONAL_ENABLED = ResourceLocation.parse("command_block_studio:button_conditional_enabled");
    public static final ResourceLocation ID_BUTTON_CONDITIONAL_FOCUSED = ResourceLocation.parse("command_block_studio:button_conditional_focused");
    public static final ResourceLocation ID_SCROLLBAR_HORIZONTAL_DISABLED = ResourceLocation.parse("command_block_studio:scrollbar_horizontal_disabled");
    public static final ResourceLocation ID_SCROLLBAR_HORIZONTAL_ENABLED = ResourceLocation.parse("command_block_studio:scrollbar_horizontal_enabled");
    public static final ResourceLocation ID_SCROLLBAR_HORIZONTAL_FOCUSED = ResourceLocation.parse("command_block_studio:scrollbar_horizontal_focused");
    public static final ResourceLocation ID_SCROLLBAR_VERTICAL_DISABLED = ResourceLocation.parse("command_block_studio:scrollbar_vertical_disabled");
    public static final ResourceLocation ID_SCROLLBAR_VERTICAL_ENABLED = ResourceLocation.parse("command_block_studio:scrollbar_vertical_enabled");
    public static final ResourceLocation ID_SCROLLBAR_VERTICAL_FOCUSED = ResourceLocation.parse("command_block_studio:scrollbar_vertical_focused");
    public static final ResourceLocation ID_SLIDER_PICK_ENABLED = ResourceLocation.parse("command_block_studio:slider_pick_enabled");
    public static final ResourceLocation ID_SLIDER_PICK_FOCUSED = ResourceLocation.parse("command_block_studio:slider_pick_focused");

    public static final WidgetSprites BLOCK_IMPULSE = new WidgetSprites(ID_BLOCK_IMPULSE, ID_BLOCK_IMPULSE_FOCUSED);
    public static final WidgetSprites BLOCK_IMPULSE_CONDITIONAL = new WidgetSprites(ID_BLOCK_IMPULSE_CONDITIONAL, ID_BLOCK_IMPULSE_CONDITIONAL_FOCUSED);
    public static final WidgetSprites BLOCK_CHAIN = new WidgetSprites(ID_BLOCK_CHAIN, ID_BLOCK_CHAIN_FOCUSED);
    public static final WidgetSprites BLOCK_CHAIN_CONDITIONAL = new WidgetSprites(ID_BLOCK_CHAIN_CONDITIONAL, ID_BLOCK_CHAIN_CONDITIONAL_FOCUSED);
    public static final WidgetSprites BLOCK_REPEAT = new WidgetSprites(ID_BLOCK_REPEAT, ID_BLOCK_REPEAT_FOCUSED);
    public static final WidgetSprites BLOCK_REPEAT_CONDITIONAL = new WidgetSprites(ID_BLOCK_REPEAT_CONDITIONAL, ID_BLOCK_REPEAT_CONDITIONAL_FOCUSED);

    private static ResourceLocation iconTexture(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                CommandBlockStudioMod.MODID,
                "textures/gui/sprites/" + name + ".png"
        );
    }

    public static final WidgetSprites BUTTON_IMPULSE = new WidgetSprites(ID_BUTTON_IMPULSE_ENABLED, ID_BUTTON_IMPULSE_DISABLED, ID_BUTTON_IMPULSE_FOCUSED);
    public static final WidgetSprites BUTTON_CHAIN = new WidgetSprites(ID_BUTTON_CHAIN_ENABLED, ID_BUTTON_CHAIN_DISABLED, ID_BUTTON_CHAIN_FOCUSED);
    public static final WidgetSprites BUTTON_REPEAT = new WidgetSprites(ID_BUTTON_REPEAT_ENABLED, ID_BUTTON_REPEAT_DISABLED, ID_BUTTON_REPEAT_FOCUSED);
    public static final WidgetSprites BUTTON_POWER_INACTIVE = new WidgetSprites(ID_BUTTON_POWER_INACTIVE_ENABLED, ID_BUTTON_POWER_INACTIVE_DISABLED, ID_BUTTON_POWER_INACTIVE_FOCUSED);
    public static final WidgetSprites BUTTON_POWER_ACTIVE = new WidgetSprites(ID_BUTTON_POWER_ACTIVE_ENABLED, ID_BUTTON_POWER_ACTIVE_DISABLED, ID_BUTTON_POWER_ACTIVE_FOCUSED);
    public static final WidgetSprites BUTTON_UNCONDITIONAL = new WidgetSprites(ID_BUTTON_UNCONDITIONAL_ENABLED, ID_BUTTON_UNCONDITIONAL_DISABLED, ID_BUTTON_UNCONDITIONAL_FOCUSED);
    public static final WidgetSprites BUTTON_CONDITIONAL = new WidgetSprites(ID_BUTTON_CONDITIONAL_ENABLED, ID_BUTTON_CONDITIONAL_DISABLED, ID_BUTTON_CONDITIONAL_FOCUSED);
    public static final WidgetSprites BUTTON_COMMAND = new WidgetSprites(ID_BUTTON_COMMAND_ENABLED, ID_BUTTON_COMMAND_DISABLED, ID_BUTTON_COMMAND_FOCUSED);
    public static final WidgetSprites BUTTON_OUTPUT = new WidgetSprites(ID_BUTTON_OUTPUT_ENABLED, ID_BUTTON_OUTPUT_DISABLED, ID_BUTTON_OUTPUT_FOCUSED);
    public static final WidgetSprites BUTTON_TRACK_OUTPUT = new WidgetSprites(ID_BUTTON_TRACK_OUTPUT_ENABLED, ID_BUTTON_TRACK_OUTPUT_DISABLED, ID_BUTTON_TRACK_OUTPUT_FOCUSED);
    public static final WidgetSprites BUTTON_IGNORE_OUTPUT = new WidgetSprites(ID_BUTTON_IGNORE_OUTPUT_ENABLED, ID_BUTTON_IGNORE_OUTPUT_DISABLED, ID_BUTTON_IGNORE_OUTPUT_FOCUSED);
    public static final WidgetSprites SLIDER_PICK = new WidgetSprites(ID_SLIDER_PICK_ENABLED, ID_SLIDER_PICK_FOCUSED);
    public static final WidgetSprites SCROLLBAR_HORIZONTAL = new WidgetSprites(ID_SCROLLBAR_HORIZONTAL_ENABLED, ID_SCROLLBAR_HORIZONTAL_DISABLED, ID_SCROLLBAR_HORIZONTAL_FOCUSED);
    public static final WidgetSprites SCROLLBAR_VERTICAL = new WidgetSprites(ID_SCROLLBAR_VERTICAL_ENABLED, ID_SCROLLBAR_VERTICAL_DISABLED, ID_SCROLLBAR_VERTICAL_FOCUSED);

    public static WidgetSprites BlockStateToButtonTextures(BlockState state){
        if (!state.hasProperty(CommandBlock.CONDITIONAL)) {
            throw new IllegalArgumentException("Expected a command block state, got " + state);
        }
        boolean conditional = state.getValue(CommandBlock.CONDITIONAL);
        if(state.is(Blocks.COMMAND_BLOCK)){
            return conditional ? BLOCK_IMPULSE_CONDITIONAL : BLOCK_IMPULSE;
        }
        if(state.is(Blocks.CHAIN_COMMAND_BLOCK)){
            return conditional ? BLOCK_CHAIN_CONDITIONAL : BLOCK_CHAIN;
        }
        if(state.is(Blocks.REPEATING_COMMAND_BLOCK)){
            return conditional ? BLOCK_REPEAT_CONDITIONAL : BLOCK_REPEAT;
        }
        throw new IllegalArgumentException("Expected a command block state, got " + state);
    }

    public static Component DirectionToText(Direction dir){
        return switch(dir){
            case UP -> Component.translatable("cbs.direction.up");
            case DOWN -> Component.translatable("cbs.direction.down");
            case NORTH -> Component.translatable("cbs.direction.north");
            case SOUTH -> Component.translatable("cbs.direction.south");
            case EAST -> Component.translatable("cbs.direction.east");
            case WEST -> Component.translatable("cbs.direction.west");
        };
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("commandBlockStudio");

    public static final String VAR_SCROLL_X = "scroll_step_horizontal";
    public static final String VAR_SCROLL_Y = "scroll_step_vertical";
    public static final String VAR_INDENTATION = "indentation_spaces";
    public static final String VAR_WRAPAROUND = "wraparound";
    public static final String VAR_FORMAT_STRINGS = "format_strings";
    public static final String VAR_AUTOSAVE = "autosave";
    public static final String VAR_CONFIRM_UNSAVED_EXIT = "confirm_unsaved_exit";
    public static final String VAR_NEWLINE_PRE_OPEN_BRACKET = "newline_pre_open_bracket";
    public static final String VAR_NEWLINE_POST_OPEN_BRACKET = "newline_post_open_bracket";
    public static final String VAR_NEWLINE_PRE_CLOSE_BRACKET = "newline_pre_close_bracket";
    public static final String VAR_NEWLINE_POST_CLOSE_BRACKET = "newline_post_close_bracket";
    public static final String VAR_NEWLINE_POST_COMMA = "newline_post_comma";
    public static final String VAR_AVOID_DOUBLE_NEWLINE = "avoid_double_newline";
    public static final String VAR_BRACKET_AUTOCOMPLETE = "bracket_autocomplete";
    public static final String VAR_TRACK_OUTPUT_DEFAULT_USED = "track_output_default_used";
    public static final String VAR_TRACK_OUTPUT_DEFAULT_VALUE = "track_output_default_value";
    public static final String VAR_SHOW_OUTPUT_DEFAULT = "show_output_default";
    public static final String VAR_COMMAND_INSIGHT_LANGUAGE = "command_insight_language";
    public static final String VAR_UI_SCALE_PERCENT = "ui_scale_percent";


    public static int SCROLL_STEP_X = 4;
    public static int SCROLL_STEP_Y = 2;
    public static int INDENTATION_FACTOR = 2;
    public static char INDENTATION_CHAR = ' ';
    public static int WRAPAROUND_WIDTH = 250;
    public static boolean FORMAT_STRINGS = true;
    public static boolean AUTOSAVE = true;
    public static boolean CONFIRM_UNSAVED_EXIT = true;
    public static boolean NEWLINE_PRE_OPEN_BRACKET = true;
    public static boolean NEWLINE_POST_OPEN_BRACKET = true;
    public static boolean NEWLINE_PRE_CLOSE_BRACKET = true;
    public static boolean NEWLINE_POST_CLOSE_BRACKET = false;
    public static boolean NEWLINE_POST_COMMA = true;
    public static boolean AVOID_DOUBLE_NEWLINE = true;
    public static boolean BRACKET_AUTOCOMPLETE = true;
    public static boolean TRACK_OUTPUT_DEFAULT_USED = false;
    public static boolean TRACK_OUTPUT_DEFAULT_VALUE = true;
    public static boolean SHOW_OUTPUT_DEFAULT = false;
    public static String COMMAND_INSIGHT_LANGUAGE = "zh_cn";
    public static int UI_SCALE_PERCENT = 0;

    public static boolean useChineseCommandInsight() {
        return COMMAND_INSIGHT_LANGUAGE.equalsIgnoreCase("zh_cn");
    }

    private static KeyMapping areaSelectionInput;
    private static KeyMapping workModeInput;

    public CommandBlockStudio(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerKeyMappings);
        modEventBus.addListener(this::buildCreativeToolsTab);
        modEventBus.addListener(this::onConfigLoading);
        modEventBus.addListener(this::onConfigReloading);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new CommandBlockItemTooltip());
        NeoForge.EVENT_BUS.register(new CommandBlockWorkMode());

        migrateLegacyConfig();
        modContainer.registerConfig(ModConfig.Type.CLIENT, CommandBlockStudioConfig.SPEC, MODID + "-client.toml");

        modContainer.registerExtensionPoint(
                IConfigScreenFactory.class,
                (IConfigScreenFactory) (container, parent) -> new ConfigScreen(parent));

        LOGGER.info("[CBS] Command Block Studio initialized.");
    }

    private static void migrateLegacyConfig() {
        Path configDirectory = FMLPaths.CONFIGDIR.get();
        Path legacyConfig = configDirectory.resolve("neo_better_cbui-client.toml");
        Path studioConfig = configDirectory.resolve(MODID + "-client.toml");
        if (!Files.isRegularFile(legacyConfig) || Files.exists(studioConfig)) {
            return;
        }

        try {
            Files.copy(legacyConfig, studioConfig);
            LOGGER.info("[CBS] Migrated legacy client config from {}", legacyConfig.getFileName());
        } catch (IOException exception) {
            LOGGER.warn("[CBS] Could not migrate legacy client config", exception);
        }
    }

    private void onConfigLoading(ModConfigEvent.Loading event) {
        refreshConfig(event.getConfig());
    }

    private void onConfigReloading(ModConfigEvent.Reloading event) {
        refreshConfig(event.getConfig());
    }

    private static void refreshConfig(ModConfig config) {
        if (config.getSpec() != CommandBlockStudioConfig.SPEC) {
            return;
        }
        refreshConfigValues();
    }

    @SubscribeEvent
    public void onSystemChat(ClientChatReceivedEvent.System event) {
        if (event.isOverlay() || !(event.getMessage().getContents() instanceof TranslatableContents contents)
                || !"advMode.setCommand.success".equals(contents.getKey())
                || contents.getArgs().length == 0) {
            return;
        }

        if (System.currentTimeMillis() <= suppressCommandSaveMessageUntil) {
            suppressCommandSaveMessageUntil = 0L;
            event.setCanceled(true);
            return;
        }

        String command = contents.getArgs()[0] instanceof Component component
                ? component.getString()
                : String.valueOf(contents.getArgs()[0]);
        event.setMessage(Component.empty()
                .append(Component.translatable("cbs.chat.commandSaved").withStyle(net.minecraft.ChatFormatting.GREEN))
                .append(Component.literal("\n  > ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY))
                .append(Component.literal(command).withStyle(net.minecraft.ChatFormatting.AQUA)));
    }

    public static void suppressNextCommandSaveMessage() {
        suppressCommandSaveMessageUntil = System.currentTimeMillis() + 2_000L;
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        areaSelectionInput = new KeyMapping(
                "key.cbs.areaselectioninput",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_SEMICOLON,
                "key.category.cbs.keybinds");
        workModeInput = new KeyMapping(
                "key.cbs.workMode",
                KeyConflictContext.IN_GAME,
                KeyModifier.ALT,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_TAB,
                "key.category.cbs.keybinds");
        event.register(areaSelectionInput);
        event.register(workModeInput);
    }

    private void buildCreativeToolsTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(CommandBlockScanner.createStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    @SubscribeEvent
    public void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (areaSelectionInput != null && client.player != null) {
            while (areaSelectionInput.consumeClick()) {
                boolean startedSelection = AreaSelectionHandler.areaSelectionInput();
                client.player.displayClientMessage(startedSelection ? Component.translatable("cbs.areaSelection.start") : Component.translatable("cbs.areaSelection.end"), true);
            }
        }
        if (workModeInput != null && client.player != null) {
            while (workModeInput.consumeClick()) {
                CommandBlockWorkMode.toggle();
            }
        }
    }

    public static void setConfig(String key, String value){
        switch (key) {
            case VAR_SCROLL_X -> CommandBlockStudioConfig.SCROLL_STEP_HORIZONTAL.set(Integer.parseInt(value));
            case VAR_SCROLL_Y -> CommandBlockStudioConfig.SCROLL_STEP_VERTICAL.set(Integer.parseInt(value));
            case VAR_INDENTATION -> CommandBlockStudioConfig.INDENTATION_SPACES.set(Integer.parseInt(value));
            case VAR_WRAPAROUND -> CommandBlockStudioConfig.WRAPAROUND.set(Integer.parseInt(value));
            case VAR_FORMAT_STRINGS -> CommandBlockStudioConfig.FORMAT_STRINGS.set(Boolean.parseBoolean(value));
            case VAR_AUTOSAVE -> CommandBlockStudioConfig.AUTOSAVE.set(Boolean.parseBoolean(value));
            case VAR_CONFIRM_UNSAVED_EXIT -> CommandBlockStudioConfig.CONFIRM_UNSAVED_EXIT.set(Boolean.parseBoolean(value));
            case VAR_NEWLINE_PRE_OPEN_BRACKET -> CommandBlockStudioConfig.NEWLINE_PRE_OPEN_BRACKET.set(Boolean.parseBoolean(value));
            case VAR_NEWLINE_POST_OPEN_BRACKET -> CommandBlockStudioConfig.NEWLINE_POST_OPEN_BRACKET.set(Boolean.parseBoolean(value));
            case VAR_NEWLINE_PRE_CLOSE_BRACKET -> CommandBlockStudioConfig.NEWLINE_PRE_CLOSE_BRACKET.set(Boolean.parseBoolean(value));
            case VAR_NEWLINE_POST_CLOSE_BRACKET -> CommandBlockStudioConfig.NEWLINE_POST_CLOSE_BRACKET.set(Boolean.parseBoolean(value));
            case VAR_NEWLINE_POST_COMMA -> CommandBlockStudioConfig.NEWLINE_POST_COMMA.set(Boolean.parseBoolean(value));
            case VAR_AVOID_DOUBLE_NEWLINE -> CommandBlockStudioConfig.AVOID_DOUBLE_NEWLINE.set(Boolean.parseBoolean(value));
            case VAR_BRACKET_AUTOCOMPLETE -> CommandBlockStudioConfig.BRACKET_AUTOCOMPLETE.set(Boolean.parseBoolean(value));
            case VAR_TRACK_OUTPUT_DEFAULT_USED -> CommandBlockStudioConfig.TRACK_OUTPUT_DEFAULT_USED.set(Boolean.parseBoolean(value));
            case VAR_TRACK_OUTPUT_DEFAULT_VALUE -> CommandBlockStudioConfig.TRACK_OUTPUT_DEFAULT_VALUE.set(Boolean.parseBoolean(value));
            case VAR_SHOW_OUTPUT_DEFAULT -> CommandBlockStudioConfig.SHOW_OUTPUT_DEFAULT.set(Boolean.parseBoolean(value));
            case VAR_COMMAND_INSIGHT_LANGUAGE -> CommandBlockStudioConfig.COMMAND_INSIGHT_LANGUAGE.set(value.toLowerCase());
            case VAR_UI_SCALE_PERCENT -> CommandBlockStudioConfig.UI_SCALE_PERCENT.set(Integer.parseInt(value));
            default -> throw new IllegalArgumentException("Unknown client config key: " + key);
        }
        refreshConfigValues();
    }

    public static void writeConfig(){
        CommandBlockStudioConfig.SPEC.save();
    }

    private static void refreshConfigValues() {
        SCROLL_STEP_X = CommandBlockStudioConfig.SCROLL_STEP_HORIZONTAL.get();
        SCROLL_STEP_Y = CommandBlockStudioConfig.SCROLL_STEP_VERTICAL.get();
        INDENTATION_FACTOR = CommandBlockStudioConfig.INDENTATION_SPACES.get();
        WRAPAROUND_WIDTH = CommandBlockStudioConfig.WRAPAROUND.get();
        FORMAT_STRINGS = CommandBlockStudioConfig.FORMAT_STRINGS.get();
        AUTOSAVE = CommandBlockStudioConfig.AUTOSAVE.get();
        CONFIRM_UNSAVED_EXIT = CommandBlockStudioConfig.CONFIRM_UNSAVED_EXIT.get();
        NEWLINE_PRE_OPEN_BRACKET = CommandBlockStudioConfig.NEWLINE_PRE_OPEN_BRACKET.get();
        NEWLINE_POST_OPEN_BRACKET = CommandBlockStudioConfig.NEWLINE_POST_OPEN_BRACKET.get();
        NEWLINE_PRE_CLOSE_BRACKET = CommandBlockStudioConfig.NEWLINE_PRE_CLOSE_BRACKET.get();
        NEWLINE_POST_CLOSE_BRACKET = CommandBlockStudioConfig.NEWLINE_POST_CLOSE_BRACKET.get();
        NEWLINE_POST_COMMA = CommandBlockStudioConfig.NEWLINE_POST_COMMA.get();
        AVOID_DOUBLE_NEWLINE = CommandBlockStudioConfig.AVOID_DOUBLE_NEWLINE.get();
        BRACKET_AUTOCOMPLETE = CommandBlockStudioConfig.BRACKET_AUTOCOMPLETE.get();
        TRACK_OUTPUT_DEFAULT_USED = CommandBlockStudioConfig.TRACK_OUTPUT_DEFAULT_USED.get();
        TRACK_OUTPUT_DEFAULT_VALUE = CommandBlockStudioConfig.TRACK_OUTPUT_DEFAULT_VALUE.get();
        SHOW_OUTPUT_DEFAULT = CommandBlockStudioConfig.SHOW_OUTPUT_DEFAULT.get();
        COMMAND_INSIGHT_LANGUAGE = CommandBlockStudioConfig.COMMAND_INSIGHT_LANGUAGE.get();
        UI_SCALE_PERCENT = CommandBlockStudioConfig.UI_SCALE_PERCENT.get();
    }


}
