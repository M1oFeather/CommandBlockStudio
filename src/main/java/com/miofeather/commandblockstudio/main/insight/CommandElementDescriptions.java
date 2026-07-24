package com.miofeather.commandblockstudio.main.insight;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;
import com.mojang.brigadier.arguments.ArgumentType;

import java.util.Locale;
import java.util.Map;

final class CommandElementDescriptions {
    private static final Map<String, Localized> ARGUMENTS = Map.ofEntries(
            entry("target", "One entity or player selected by name, UUID, or selector.", "通过名称、UUID 或选择器指定的单个实体或玩家。"),
            entry("targets", "One or more entities or players selected by name, UUID, or selector.", "通过名称、UUID 或选择器指定的一个或多个实体或玩家。"),
            entry("player", "A single player.", "单个玩家。"),
            entry("players", "One or more players.", "一个或多个玩家。"),
            entry("source", "The source object, entity, score, or data location used by this operation.", "本次操作读取的来源对象、实体、分数或数据位置。"),
            entry("pos", "A world position; supports absolute, relative (~), and local (^) coordinates.", "世界坐标，支持绝对坐标、相对坐标 ~ 和局部坐标 ^。"),
            entry("position", "A world position; supports absolute, relative (~), and local (^) coordinates.", "世界坐标，支持绝对坐标、相对坐标 ~ 和局部坐标 ^。"),
            entry("location", "A destination position or location in the current execution context.", "当前执行上下文中的目标坐标或位置。"),
            entry("from", "The first corner or source position.", "第一个角点或来源坐标。"),
            entry("to", "The opposite corner or destination position.", "对角点或目标坐标。"),
            entry("begin", "The first corner of a cuboid region.", "长方体区域的第一个角点。"),
            entry("end", "The opposite corner of a cuboid region.", "长方体区域的对角点。"),
            entry("destination", "The target entity, position, or destination object.", "目标实体、坐标或目标对象。"),
            entry("center", "The center point used by the operation.", "本次操作使用的中心点。"),
            entry("block", "A block id, state, predicate, or block NBT value.", "方块 id、方块状态、方块谓词或方块 NBT。"),
            entry("filter", "A predicate that limits which values are affected.", "用于限制受影响对象的过滤条件。"),
            entry("item", "An item id with optional data components.", "物品 id，可附带数据组件。"),
            entry("count", "The number of items or operations to apply.", "物品数量或执行次数。"),
            entry("maxcount", "The maximum number of matching items to affect.", "最多影响的匹配物品数量。"),
            entry("amount", "A numeric amount used by this operation.", "本次操作使用的数值。"),
            entry("value", "The value to read, write, compare, or return.", "要读取、写入、比较或返回的值。"),
            entry("scale", "A multiplier applied before storing or returning a numeric result.", "存储或返回数字结果前使用的倍率。"),
            entry("objective", "A scoreboard objective name.", "计分板目标名。"),
            entry("targetobjective", "The scoreboard objective that receives the operation result.", "接收运算结果的目标计分板目标。"),
            entry("sourceobjective", "The scoreboard objective read by the operation.", "运算读取的来源计分板目标。"),
            entry("score", "An integer scoreboard value.", "整数计分板分数。"),
            entry("operation", "A scoreboard arithmetic, assignment, comparison, or swap operator.", "计分板算术、赋值、比较或交换运算符。"),
            entry("name", "A namespaced id or user-defined name, depending on this command.", "命名空间 id 或本命令使用的自定义名称。"),
            entry("id", "A namespaced resource identifier.", "带命名空间的资源标识符。"),
            entry("message", "Text that can include selector substitutions where the command permits them.", "文本内容；命令允许时可包含实体选择器替换。"),
            entry("command", "A nested command to run in the current execution context.", "要在当前执行上下文中运行的嵌套命令。"),
            entry("nbt", "An SNBT value or compound tag.", "SNBT 值或复合标签。"),
            entry("path", "An NBT path such as Inventory[0].id.", "NBT 路径，例如 Inventory[0].id。"),
            entry("targetpath", "The NBT path that receives the modified value.", "接收修改结果的目标 NBT 路径。"),
            entry("sourcepath", "The NBT path from which data is copied.", "复制数据时读取的来源 NBT 路径。"),
            entry("storage", "A command storage resource location.", "命令 storage 的资源位置。"),
            entry("dimension", "A dimension id such as minecraft:overworld.", "维度 id，例如 minecraft:overworld。"),
            entry("biome", "A biome id or biome tag.", "生物群系 id 或标签。"),
            entry("structure", "A structure id or structure tag.", "结构 id 或标签。"),
            entry("effect", "A status-effect id.", "状态效果 id。"),
            entry("seconds", "Duration in seconds.", "持续时间，单位为秒。"),
            entry("duration", "How long the operation lasts; the accepted unit depends on the command.", "操作持续时间；具体单位由命令决定。"),
            entry("amplifier", "A zero-based status-effect strength.", "从 0 开始计算的状态效果等级。"),
            entry("sound", "A sound-event id.", "声音事件 id。"),
            entry("volume", "Sound volume and audible-range multiplier.", "声音音量及可听范围倍率。"),
            entry("pitch", "Sound playback pitch.", "声音播放音调。"),
            entry("particle", "A particle type with any required particle options.", "粒子类型及该粒子需要的附加参数。"),
            entry("delta", "Random particle spread along the three axes.", "粒子在三个坐标轴上的随机扩散范围。"),
            entry("speed", "A speed or motion multiplier.", "速度或运动倍率。"),
            entry("viewers", "Players that can see or receive the result.", "能够看到或接收结果的玩家。"),
            entry("rule", "A game rule offered by the active server.", "当前服务器提供的游戏规则。"),
            entry("gamemode", "A game mode: survival, creative, adventure, or spectator.", "游戏模式：生存、创造、冒险或旁观。"),
            entry("time", "A tick duration, time value, or named time accepted by this command.", "本命令接受的游戏刻时长、时间值或命名时间。"),
            entry("angle", "A rotation angle in degrees.", "以度为单位的旋转角。"),
            entry("rotation", "Yaw and pitch rotation.", "水平角和俯仰角。"),
            entry("reason", "Optional text explaining an administrative action.", "用于说明管理操作原因的可选文本。"),
            entry("range", "A numeric range written as min..max; either bound may be omitted.", "使用 min..max 表示的数值范围，两侧边界均可省略。")
    );

    private static final Map<String, Localized> LITERALS = Map.ofEntries(
            entry("add", "Adds a new value or increases an existing value.", "添加新值或增加已有值。"),
            entry("remove", "Removes a value or decreases an existing value.", "移除值或减少已有值。"),
            entry("set", "Replaces the current value.", "替换当前值。"),
            entry("get", "Reads and reports the current value.", "读取并输出当前值。"),
            entry("query", "Queries state without modifying it.", "查询状态而不进行修改。"),
            entry("list", "Lists the values available in this branch.", "列出此分支中的可用值。"),
            entry("clear", "Clears the selected state or values.", "清除选中的状态或值。"),
            entry("reset", "Restores the selected state to its default.", "把选中的状态恢复为默认值。"),
            entry("enable", "Enables the selected feature or value.", "启用选中的功能或值。"),
            entry("disable", "Disables the selected feature or value.", "禁用选中的功能或值。"),
            entry("start", "Starts the selected operation.", "开始执行选中的操作。"),
            entry("stop", "Stops the selected operation.", "停止选中的操作。"),
            entry("pause", "Pauses the selected operation.", "暂停选中的操作。"),
            entry("resume", "Resumes a paused operation.", "继续已暂停的操作。"),
            entry("modify", "Modifies a nested value using another value or operation.", "使用另一个值或操作修改嵌套值。"),
            entry("merge", "Merges a compound value into existing data.", "把复合值合并到已有数据中。"),
            entry("append", "Adds a value to the end of a list.", "把值追加到列表末尾。"),
            entry("prepend", "Adds a value to the beginning of a list.", "把值添加到列表开头。"),
            entry("insert", "Inserts a value at a list index.", "在列表指定索引处插入值。"),
            entry("from", "Copies or reads the value from another source.", "从另一个来源复制或读取值。"),
            entry("value", "Uses the literal value that follows.", "使用后面直接给出的字面值。"),
            entry("entity", "Uses entity data or an entity target.", "使用实体数据或实体目标。"),
            entry("block", "Uses a block position or block-entity data.", "使用方块坐标或方块实体数据。"),
            entry("storage", "Uses command storage data.", "使用命令 storage 数据。"),
            entry("replace", "Replaces matching or existing values.", "替换匹配值或已有值。"),
            entry("destroy", "Destroys existing blocks and drops their loot.", "破坏已有方块并掉落战利品。"),
            entry("keep", "Changes only positions that are currently empty.", "只修改当前为空的位置。"),
            entry("masked", "Ignores air blocks in the source region.", "忽略源区域中的空气方块。"),
            entry("filtered", "Affects only values matching a filter.", "只影响符合过滤条件的值。"),
            entry("hollow", "Changes the outer shell and clears the inside.", "修改外壳并清空内部。"),
            entry("outline", "Changes only the outer shell.", "只修改外壳。"),
            entry("normal", "Uses normal overlap or visibility behavior.", "使用普通的重叠或可见范围行为。"),
            entry("force", "Forces the operation or extends its visibility.", "强制执行操作或扩大可见范围。"),
            entry("move", "Moves values by clearing the source after copying.", "复制后清除来源，相当于移动。"),
            entry("if", "Continues only when the condition succeeds.", "仅在条件成立时继续。"),
            entry("unless", "Continues only when the condition fails.", "仅在条件不成立时继续。"),
            entry("run", "Runs the nested command using the current execution context.", "使用当前执行上下文运行嵌套命令。"),
            entry("as", "Changes the command executor.", "改变命令执行者。"),
            entry("at", "Moves execution position, rotation, and dimension to an entity.", "把执行位置、朝向和维度移动到实体处。"),
            entry("positioned", "Changes the execution position.", "改变命令执行位置。"),
            entry("rotated", "Changes the execution rotation.", "改变命令执行朝向。"),
            entry("facing", "Rotates execution to face a position or entity.", "让执行朝向指向坐标或实体。"),
            entry("anchored", "Chooses whether local coordinates use feet or eyes as the anchor.", "选择局部坐标以脚部还是眼睛为锚点。"),
            entry("in", "Changes the execution dimension.", "改变命令执行维度。"),
            entry("align", "Aligns selected coordinate axes to block boundaries.", "把选中的坐标轴对齐到方块边界。"),
            entry("store", "Stores command success or result in another data location.", "把命令成功值或结果存入其他数据位置。"),
            entry("result", "Uses the numeric result returned by a command.", "使用命令返回的数字结果。"),
            entry("success", "Uses whether a command succeeded.", "使用命令是否成功的状态。"),
            entry("all", "Selects every available value in this branch.", "选择此分支中的全部可用值。"),
            entry("only", "Selects only the named value.", "只选择指定值。"),
            entry("through", "Includes the named value and all of its prerequisites.", "包含指定值及其全部前置内容。"),
            entry("until", "Includes the named value and all dependent values.", "包含指定值及其全部后续依赖内容。"),
            entry("rain", "Changes weather to rain.", "把天气改为降雨。"),
            entry("thunder", "Changes weather to a thunderstorm.", "把天气改为雷暴。"),
            entry("title", "Uses the main title display.", "使用主标题显示。"),
            entry("subtitle", "Uses subtitle text displayed with a title.", "使用随主标题显示的副标题文本。"),
            entry("actionbar", "Uses text displayed above the hotbar.", "使用快捷栏上方显示的文本。"),
            entry("times", "Configures fade-in, stay, and fade-out durations.", "配置淡入、停留和淡出时间。")
    );

    private static final Map<String, Localized> TYPES = Map.ofEntries(
            entry("BoolArgumentType", "A boolean value: true or false.", "布尔值：true 或 false。"),
            entry("IntegerArgumentType", "An integer within the range accepted by this command.", "本命令允许范围内的整数。"),
            entry("LongArgumentType", "A 64-bit integer.", "64 位整数。"),
            entry("FloatArgumentType", "A decimal number.", "小数。"),
            entry("DoubleArgumentType", "A decimal number.", "小数。"),
            entry("StringArgumentType", "A word, quoted string, or remaining text depending on this argument.", "根据参数定义填写单词、带引号字符串或剩余整段文本。"),
            entry("EntityArgument", "An entity selector, player name, or UUID.", "实体选择器、玩家名或 UUID。"),
            entry("GameProfileArgument", "One or more player profiles.", "一个或多个玩家档案。"),
            entry("MessageArgument", "A chat message with selector expansion support.", "支持展开实体选择器的聊天消息。"),
            entry("ComponentArgument", "A rich text component with structured completion for content, style, click and hover fields.", "富文本组件；支持内容、样式、点击事件与悬停事件的结构化补全。"),
            entry("CompoundTagArgument", "An SNBT compound tag.", "SNBT 复合标签。"),
            entry("NbtTagArgument", "An SNBT value.", "SNBT 值。"),
            entry("NbtPathArgument", "An NBT path expression.", "NBT 路径表达式。"),
            entry("BlockPosArgument", "A block position using absolute, relative, or local coordinates.", "使用绝对、相对或局部坐标表示的方块坐标。"),
            entry("ColumnPosArgument", "An X/Z chunk-column position.", "X/Z 区块柱坐标。"),
            entry("Vec2Argument", "A two-axis coordinate pair.", "双轴坐标。"),
            entry("Vec3Argument", "A three-axis coordinate using absolute, relative, or local coordinates.", "使用绝对、相对或局部坐标表示的三轴坐标。"),
            entry("RotationArgument", "Yaw and pitch rotation.", "水平角和俯仰角。"),
            entry("BlockStateArgument", "A block id with optional state properties and NBT.", "方块 id，可附带方块状态和 NBT。"),
            entry("BlockPredicateArgument", "A block predicate or block tag.", "方块谓词或方块标签。"),
            entry("ItemArgument", "An item id with registry-backed data-component keys, value templates, and nested text Component completion.", "物品 id；支持注册表数据组件键、值模板，以及嵌套文本 Component 补全。"),
            entry("ItemPredicateArgument", "An item predicate used to match stacks.", "用于匹配物品堆的物品谓词。"),
            entry("IdentifierArgument", "A namespaced resource location.", "带命名空间的资源位置。"),
            entry("ResourceArgument", "A registry resource from the active server.", "当前服务器注册表中的资源。"),
            entry("ResourceKeyArgument", "A resource key from a registry.", "注册表中的资源键。"),
            entry("ResourceOrTagArgument", "A registry resource or resource tag.", "注册表资源或资源标签。"),
            entry("ResourceOrTagKeyArgument", "A resource key or tag key.", "资源键或标签键。"),
            entry("ObjectiveArgument", "An existing scoreboard objective.", "已有计分板目标。"),
            entry("ObjectiveCriteriaArgument", "A scoreboard objective criterion.", "计分板目标准则。"),
            entry("ScoreHolderArgument", "Score holders selected by name, wildcard, or entity selector.", "通过名称、通配符或实体选择器指定的分数持有者。"),
            entry("OperationArgument", "A scoreboard operation such as +=, =, <, >, or ><.", "计分板运算，例如 +=、=、<、> 或 ><。"),
            entry("ScoreboardSlotArgument", "A scoreboard display slot.", "计分板显示栏位。"),
            entry("SlotArgument", "A single inventory slot.", "单个物品栏槽位。"),
            entry("SlotsArgument", "One or more inventory slots.", "一个或多个物品栏槽位。"),
            entry("TimeArgument", "A time value with an optional unit such as d, s, or t.", "时间值，可使用 d、s 或 t 等单位。"),
            entry("RangeArgument", "A numeric range such as 1..10, ..5, or 3...", "数值范围，例如 1..10、..5 或 3..。"),
            entry("UuidArgument", "A UUID.", "UUID。"),
            entry("GameModeArgument", "A game mode.", "游戏模式。"),
            entry("DimensionArgument", "A dimension id.", "维度 id。"),
            entry("ParticleArgument", "A particle type and its required options.", "粒子类型及其所需参数。")
    );

    private CommandElementDescriptions() {
    }

    static String describeArgument(String name, ArgumentType<?> type) {
        String normalized = name.replace("_", "").toLowerCase(Locale.ROOT);
        Localized byName = ARGUMENTS.get(normalized);
        if (byName != null) {
            return byName.active();
        }
        if (type != null) {
            Localized byType = TYPES.get(type.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            if (byType != null) {
                return byType.active();
            }
        }
        return CommandBlockStudio.useChineseCommandInsight()
                ? "由当前服务器 Brigadier 命令树定义的参数。"
                : "Argument defined by the active server's Brigadier command tree.";
    }

    static String describeLiteral(String name) {
        Localized description = LITERALS.get(name.toLowerCase(Locale.ROOT));
        return description == null ? null : description.active();
    }

    private static Map.Entry<String, Localized> entry(String name, String en, String zh) {
        return Map.entry(name.toLowerCase(Locale.ROOT), new Localized(en, zh));
    }

    private record Localized(String en, String zh) {
        String active() {
            return CommandBlockStudio.useChineseCommandInsight() ? zh : en;
        }
    }
}
