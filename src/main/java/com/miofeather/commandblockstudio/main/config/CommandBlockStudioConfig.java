package com.miofeather.commandblockstudio.main.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Set;

public final class CommandBlockStudioConfig {
    private static final Set<String> SUPPORTED_INSIGHT_LANGUAGES = Set.of("zh_cn", "en_us");

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue INDENTATION_SPACES;
    public static final ModConfigSpec.IntValue SCROLL_STEP_VERTICAL;
    public static final ModConfigSpec.IntValue SCROLL_STEP_HORIZONTAL;
    public static final ModConfigSpec.IntValue WRAPAROUND;
    public static final ModConfigSpec.BooleanValue FORMAT_STRINGS;
    public static final ModConfigSpec.BooleanValue AUTOSAVE;
    public static final ModConfigSpec.BooleanValue CONFIRM_UNSAVED_EXIT;
    public static final ModConfigSpec.ConfigValue<String> COMMAND_INSIGHT_LANGUAGE;
    public static final ModConfigSpec.IntValue UI_SCALE_PERCENT;
    public static final ModConfigSpec.BooleanValue BRACKET_AUTOCOMPLETE;
    public static final ModConfigSpec.BooleanValue NEWLINE_PRE_OPEN_BRACKET;
    public static final ModConfigSpec.BooleanValue NEWLINE_POST_OPEN_BRACKET;
    public static final ModConfigSpec.BooleanValue NEWLINE_PRE_CLOSE_BRACKET;
    public static final ModConfigSpec.BooleanValue NEWLINE_POST_CLOSE_BRACKET;
    public static final ModConfigSpec.BooleanValue NEWLINE_POST_COMMA;
    public static final ModConfigSpec.BooleanValue AVOID_DOUBLE_NEWLINE;
    public static final ModConfigSpec.BooleanValue TRACK_OUTPUT_DEFAULT_USED;
    public static final ModConfigSpec.BooleanValue TRACK_OUTPUT_DEFAULT_VALUE;
    public static final ModConfigSpec.BooleanValue SHOW_OUTPUT_DEFAULT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        INDENTATION_SPACES = builder
                .comment("Number of extra spaces per level of visual indentation.")
                .defineInRange("indentation_spaces", 2, 1, 16);
        SCROLL_STEP_VERTICAL = builder
                .comment("Number of visual lines scrolled per mouse-wheel step.")
                .defineInRange("scroll_step_vertical", 2, 1, 64);
        SCROLL_STEP_HORIZONTAL = builder
                .comment("Number of characters scrolled horizontally per mouse-wheel step.")
                .defineInRange("scroll_step_horizontal", 4, 1, 64);
        WRAPAROUND = builder
                .comment("Maximum rendered line width before the editor wraps command text.")
                .defineInRange("wraparound", 250, 10, 6400);
        FORMAT_STRINGS = builder
                .comment("Whether visual bracket formatting is also applied inside string arguments.")
                .define("format_strings", true);
        AUTOSAVE = builder
                .comment("Whether command edits are saved after a short typing debounce and again when closing.")
                .define("autosave", true);
        CONFIRM_UNSAVED_EXIT = builder
                .comment("Whether Escape asks before leaving modified commands when autosave is disabled.")
                .define("confirm_unsaved_exit", true);
        COMMAND_INSIGHT_LANGUAGE = builder
                .comment("Language used by command documentation. Supported values: zh_cn, en_us.")
                .define("command_insight_language", "zh_cn",
                         value -> value instanceof String language && SUPPORTED_INSIGHT_LANGUAGES.contains(language.toLowerCase()));
        UI_SCALE_PERCENT = builder
                .comment("Command Block Studio UI scale in percent. 0 follows the current Minecraft GUI scale automatically.")
                .defineInRange("ui_scale_percent", 0, 0, 125);
        BRACKET_AUTOCOMPLETE = builder
                .comment("Whether matching brackets and quotes are inserted while editing.")
                .define("bracket_autocomplete", true);

        NEWLINE_PRE_OPEN_BRACKET = builder.comment("Break before an opening bracket.")
                .define("newline_pre_open_bracket", true);
        NEWLINE_POST_OPEN_BRACKET = builder.comment("Break after an opening bracket.")
                .define("newline_post_open_bracket", true);
        NEWLINE_PRE_CLOSE_BRACKET = builder.comment("Break before a closing bracket.")
                .define("newline_pre_close_bracket", true);
        NEWLINE_POST_CLOSE_BRACKET = builder.comment("Break after a closing bracket.")
                .define("newline_post_close_bracket", false);
        NEWLINE_POST_COMMA = builder.comment("Break after a comma.")
                .define("newline_post_comma", true);
        AVOID_DOUBLE_NEWLINE = builder.comment("Collapse empty visual lines produced by formatting.")
                .define("avoid_double_newline", true);

        TRACK_OUTPUT_DEFAULT_USED = builder.comment("Whether the custom track-output default is enabled.")
                .define("track_output_default_used", false);
        TRACK_OUTPUT_DEFAULT_VALUE = builder.comment("Default value used when custom track-output behavior is enabled.")
                .define("track_output_default_value", true);
        SHOW_OUTPUT_DEFAULT = builder.comment("Whether command output is shown when the editor opens.")
                .define("show_output_default", false);

        SPEC = builder.build();
    }

    private CommandBlockStudioConfig() {
    }
}
