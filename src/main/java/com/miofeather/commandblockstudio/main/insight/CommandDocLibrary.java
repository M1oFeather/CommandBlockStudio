package com.miofeather.commandblockstudio.main.insight;

import com.miofeather.commandblockstudio.main.CommandBlockStudio;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommandDocLibrary {
    private static final Map<String, CommandDoc> DOCS_EN = Map.ofEntries(
            doc("execute",
                    "Runs another command after changing the executor, position, rotation, dimension, or after checking conditions.",
                    List.of("execute as <targets> at @s run <command>", "execute if block <pos> <block> run <command>", "execute store result score <targets> <objective> run <command>"),
                    Map.ofEntries(
                            Map.entry("as", "Changes the executing entity. @s inside the run command becomes the selected entity."),
                            Map.entry("at", "Moves the execution position, rotation, and dimension to the selected entity."),
                            Map.entry("positioned", "Changes only the execution position."),
                            Map.entry("rotated", "Changes the execution rotation."),
                            Map.entry("facing", "Makes the execution face a position or entity."),
                            Map.entry("anchored", "Chooses whether local coordinates start from the executor's feet or eyes."),
                            Map.entry("in", "Changes the execution dimension."),
                            Map.entry("if", "Runs the command only when the condition succeeds."),
                            Map.entry("unless", "Runs the command only when the condition fails."),
                            Map.entry("store", "Stores the result or success value of the run command."),
                            Map.entry("run", "Ends the execute chain and runs the nested command.")
                    ),
                    Map.ofEntries(
                            Map.entry("targets", "Entity selector or player name."),
                            Map.entry("pos", "World position. Supports absolute, relative (~), and local (^) coordinates."),
                            Map.entry("block", "Block id or block predicate."),
                            Map.entry("command", "Nested command to execute with the current context."),
                            Map.entry("objective", "Scoreboard objective name."),
                            Map.entry("scale", "Multiplier applied before storing a numeric result.")
                    )),
            doc("data",
                    "Reads, writes, merges, removes, or modifies NBT data on blocks, entities, or command storage.",
                    List.of("data get entity <target> <path>", "data merge block <targetPos> <nbt>", "data modify entity <target> <targetPath> set value <value>"),
                    Map.ofEntries(
                            Map.entry("get", "Reads NBT data and prints it to chat or command output."),
                            Map.entry("merge", "Merges the given compound tag into existing NBT."),
                            Map.entry("modify", "Copies, inserts, appends, prepends, sets, or merges one NBT value."),
                            Map.entry("remove", "Deletes an NBT path."),
                            Map.entry("entity", "Targets an entity's NBT data."),
                            Map.entry("block", "Targets a block entity's NBT data."),
                            Map.entry("storage", "Targets command storage NBT.")
                    ),
                    Map.ofEntries(
                            Map.entry("target", "Single entity selector."),
                            Map.entry("targetPos", "Position of a block entity."),
                            Map.entry("path", "NBT path, for example Inventory[0].tag.display.Name."),
                            Map.entry("nbt", "Compound NBT value."),
                            Map.entry("value", "NBT value to write.")
                    )),
            doc("scoreboard",
                    "Manages scoreboard objectives, player scores, display slots, and arithmetic operations.",
                    List.of("scoreboard objectives add <objective> dummy", "scoreboard players set <targets> <objective> <score>", "scoreboard players operation <targets> <targetObjective> += <source> <sourceObjective>"),
                    Map.ofEntries(
                            Map.entry("objectives", "Creates, removes, lists, modifies, or displays objectives."),
                            Map.entry("players", "Reads and writes scores for players or fake player names."),
                            Map.entry("operation", "Performs arithmetic between two score values.")
                    ),
                    Map.ofEntries(
                            Map.entry("objective", "Scoreboard objective name."),
                            Map.entry("targets", "Players, fake player names, or entity selector."),
                            Map.entry("score", "Integer score value."),
                            Map.entry("operation", "Arithmetic operation such as +=, -=, *=, /=, %=, =, <, >, or ><.")
                    )),
            doc("summon", "Creates an entity at a position, optionally with NBT data.", List.of("summon <entity> <pos> <nbt>"), Map.of(), Map.ofEntries(
                    Map.entry("entity", "Entity type id, for example minecraft:zombie."),
                    Map.entry("pos", "Spawn position."),
                    Map.entry("nbt", "Optional entity NBT compound.")
            )),
            doc("give", "Gives item stacks to players.", List.of("give <targets> <item> <count>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "Players that receive the item."),
                    Map.entry("item", "Item id, optionally with components."),
                    Map.entry("count", "Number of items to give.")
            )),
            doc("tp", "Teleports entities to a position, rotation, or another entity.", List.of("tp <targets> <location>", "tp <targets> <destination>"), Map.of(), teleportArgumentsEn()),
            doc("teleport", "Teleports entities to a position, rotation, or another entity.", List.of("teleport <targets> <location>", "teleport <targets> <destination>"), Map.of(), teleportArgumentsEn()),
            doc("setblock",
                    "Changes one block at a position.",
                    List.of("setblock <pos> <block> replace"),
                    Map.ofEntries(
                            Map.entry("destroy", "Breaks the old block and drops loot before placing the new block."),
                            Map.entry("keep", "Places only if the target position is air."),
                            Map.entry("replace", "Replaces the existing block without dropping it.")
                    ),
                    Map.ofEntries(
                            Map.entry("pos", "Target block position."),
                            Map.entry("block", "Block id, block state, or block NBT.")
                    )),
            doc("fill",
                    "Fills a cuboid area with blocks.",
                    List.of("fill <from> <to> <block> replace"),
                    Map.ofEntries(
                            Map.entry("destroy", "Destroys old blocks and drops loot."),
                            Map.entry("hollow", "Fills only the outside shell."),
                            Map.entry("outline", "Fills the outside shell and leaves inside unchanged."),
                            Map.entry("replace", "Replaces matching blocks or all blocks."),
                            Map.entry("keep", "Places only into air blocks.")
                    ),
                    Map.ofEntries(
                            Map.entry("from", "First corner of the cuboid."),
                            Map.entry("to", "Opposite corner of the cuboid."),
                            Map.entry("block", "Block to place."),
                            Map.entry("filter", "Optional block filter for replace mode.")
                    )),
            doc("effect",
                    "Adds, clears, or queries status effects on entities.",
                    List.of("effect give <targets> <effect> <seconds> <amplifier>"),
                    Map.ofEntries(
                            Map.entry("give", "Applies a status effect."),
                            Map.entry("clear", "Removes status effects.")
                    ),
                    Map.ofEntries(
                            Map.entry("targets", "Entities that receive or lose the effect."),
                            Map.entry("effect", "Status effect id."),
                            Map.entry("seconds", "Duration in seconds."),
                            Map.entry("amplifier", "Effect strength, zero-based.")
                    )),
            doc("attribute",
                    "Reads or changes entity attributes and attribute modifiers.",
                    List.of("attribute <target> <attribute> get", "attribute <target> <attribute> base set <value>"),
                    Map.ofEntries(
                            Map.entry("base", "Reads or writes the base attribute value."),
                            Map.entry("modifier", "Adds, removes, or queries an attribute modifier.")
                    ),
                    Map.ofEntries(
                            Map.entry("target", "Single entity."),
                            Map.entry("attribute", "Attribute id."),
                            Map.entry("value", "Numeric attribute value.")
                    )),
            doc("clear", "Removes matching items from player inventories.", List.of("clear <targets> <item> <maxCount>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "Players whose inventories are searched."),
                    Map.entry("item", "Optional item or item predicate to remove."),
                    Map.entry("maxCount", "Maximum number of matching items to remove; zero only tests for matches.")
            )),
            doc("clone", "Copies blocks from one cuboid region to another.", List.of("clone <begin> <end> <destination> replace normal"), Map.ofEntries(
                    Map.entry("replace", "Copies all blocks, including air."),
                    Map.entry("masked", "Copies only non-air blocks."),
                    Map.entry("filtered", "Copies only blocks matching a filter."),
                    Map.entry("normal", "Fails when source and destination overlap."),
                    Map.entry("force", "Allows source and destination to overlap."),
                    Map.entry("move", "Moves blocks by replacing the source region with air.")
            ), Map.ofEntries(
                    Map.entry("begin", "First corner of the source region."),
                    Map.entry("end", "Opposite corner of the source region."),
                    Map.entry("destination", "Lowest-coordinate corner of the destination region.")
            )),
            doc("damage", "Deals a specified amount and type of damage to an entity.", List.of("damage <target> <amount>"), Map.ofEntries(
                    Map.entry("at", "Uses a position as the damage source location."),
                    Map.entry("by", "Uses an entity as the direct damage source."),
                    Map.entry("from", "Supplies the entity that ultimately caused the damage.")
            ), Map.ofEntries(
                    Map.entry("target", "Single entity to damage."),
                    Map.entry("amount", "Non-negative damage amount."),
                    Map.entry("damageType", "Damage type id."),
                    Map.entry("location", "Damage source position."),
                    Map.entry("entity", "Direct damage source entity."),
                    Map.entry("cause", "Entity responsible for the damage.")
            )),
            doc("enchant", "Adds an enchantment to the item held by selected entities.", List.of("enchant <targets> <enchantment> <level>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "Entities whose main-hand item is enchanted."),
                    Map.entry("enchantment", "Enchantment id."),
                    Map.entry("level", "Enchantment level within the enchantment's allowed range.")
            )),
            doc("experience", "Adds, sets, or queries player experience points and levels.", List.of("experience add <targets> <amount> points"), Map.ofEntries(
                    Map.entry("add", "Adds points or levels."),
                    Map.entry("set", "Sets the current points or level value."),
                    Map.entry("query", "Reads a player's points or level value."),
                    Map.entry("points", "Treats the amount as experience points."),
                    Map.entry("levels", "Treats the amount as experience levels.")
            ), Map.ofEntries(
                    Map.entry("targets", "Players to modify."),
                    Map.entry("amount", "Number of points or levels."),
                    Map.entry("target", "Single player to query.")
            )),
            doc("xp", "Alias of /experience; adds, sets, or queries player experience.", List.of("xp add <targets> <amount> points"), Map.ofEntries(
                    Map.entry("add", "Adds points or levels."),
                    Map.entry("set", "Sets points or levels."),
                    Map.entry("query", "Reads points or levels.")
            ), Map.ofEntries(
                    Map.entry("targets", "Players to modify."),
                    Map.entry("amount", "Number of points or levels."),
                    Map.entry("target", "Single player to query.")
            )),
            doc("gamemode", "Changes the game mode of one or more players.", List.of("gamemode <gameMode> <target>"), Map.of(), Map.ofEntries(
                    Map.entry("gameMode", "survival, creative, adventure, or spectator."),
                    Map.entry("target", "Players whose game mode is changed.")
            )),
            doc("gamerule", "Reads or changes a world game rule.", List.of("gamerule <rule> <value>"), Map.of(), Map.ofEntries(
                    Map.entry("rule", "Game rule name offered by the active server."),
                    Map.entry("value", "Boolean or integer value accepted by that rule.")
            )),
            doc("kill", "Immediately kills selected entities.", List.of("kill <targets>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "Entities to kill; defaults to the command executor.")
            )),
            doc("locate", "Finds the nearest matching structure, biome, or point of interest.", List.of("locate structure <structure>"), Map.ofEntries(
                    Map.entry("structure", "Searches for a configured structure."),
                    Map.entry("biome", "Searches for a biome."),
                    Map.entry("poi", "Searches for a point-of-interest type.")
            ), Map.ofEntries(
                    Map.entry("structure", "Structure id or structure tag."),
                    Map.entry("biome", "Biome id or biome tag."),
                    Map.entry("poi", "Point-of-interest id or tag.")
            )),
            doc("particle", "Spawns particles with optional spread, speed, count, and viewers.", List.of("particle <name> <pos>"), Map.ofEntries(
                    Map.entry("normal", "Uses normal visibility distance."),
                    Map.entry("force", "Uses long-distance particle visibility.")
            ), Map.ofEntries(
                    Map.entry("name", "Particle type and any required particle options."),
                    Map.entry("pos", "Particle origin."),
                    Map.entry("delta", "Random spread on each axis."),
                    Map.entry("speed", "Particle speed multiplier."),
                    Map.entry("count", "Number of particles."),
                    Map.entry("viewers", "Players that can see the particles.")
            )),
            doc("playsound", "Plays a sound for selected players.", List.of("playsound <sound> master <targets> <pos>"), Map.of(), Map.ofEntries(
                    Map.entry("sound", "Sound event id."),
                    Map.entry("source", "Sound category such as master, music, block, or player."),
                    Map.entry("targets", "Players that hear the sound."),
                    Map.entry("pos", "Sound origin."),
                    Map.entry("volume", "Volume and audible range multiplier."),
                    Map.entry("pitch", "Playback pitch."),
                    Map.entry("minVolume", "Minimum volume outside the normal audible range.")
            )),
            doc("tellraw", "Sends a rich text component to selected players.", List.of("tellraw <targets> <message>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "Players that receive the message."),
                    Map.entry("message", "Text component, including colors, styles, click events, and translations.")
            )),
            doc("time", "Reads or changes the world's day time.", List.of("time set <time>"), Map.ofEntries(
                    Map.entry("add", "Adds ticks to the current day time."),
                    Map.entry("set", "Sets the day time."),
                    Map.entry("query", "Reads daytime, gametime, or day count.")
            ), Map.ofEntries(
                    Map.entry("time", "Tick value or a named time such as day, noon, night, or midnight.")
            )),
            doc("title", "Displays title, subtitle, or action-bar text and controls title timing.", List.of("title <targets> title <title>"), Map.ofEntries(
                    Map.entry("title", "Sets and displays the main title."),
                    Map.entry("subtitle", "Sets subtitle text shown with the next title."),
                    Map.entry("actionbar", "Displays text above the hotbar."),
                    Map.entry("times", "Sets fade-in, stay, and fade-out durations."),
                    Map.entry("clear", "Hides the current title."),
                    Map.entry("reset", "Hides the title and resets timing values.")
            ), Map.ofEntries(
                    Map.entry("targets", "Players that receive the title."),
                    Map.entry("title", "Rich text component to display."),
                    Map.entry("fadeIn", "Fade-in duration in ticks."),
                    Map.entry("stay", "Visible duration in ticks."),
                    Map.entry("fadeOut", "Fade-out duration in ticks.")
            )),
            doc("weather", "Changes the world's weather for an optional duration.", List.of("weather clear <duration>"), Map.ofEntries(
                    Map.entry("clear", "Stops rain and thunder."),
                    Map.entry("rain", "Starts rain."),
                    Map.entry("thunder", "Starts a thunderstorm.")
            ), Map.ofEntries(
                    Map.entry("duration", "Optional weather duration in seconds.")
            )),
            doc("function", "Runs one or more functions from a datapack.", List.of("function <name>"), Map.of(), Map.ofEntries(
                    Map.entry("name", "Function id, for example namespace:path/to/function.")
            ))
    );

    private static final Map<String, CommandDoc> DOCS_ZH = Map.ofEntries(
            doc("execute",
                    "在改变执行者、执行位置、朝向、维度，或检查条件之后，再运行另一条命令。",
                    List.of("execute as <targets> at @s run <command>", "execute if block <pos> <block> run <command>", "execute store result score <targets> <objective> run <command>"),
                    Map.ofEntries(
                            Map.entry("as", "改变命令执行者。run 后面的 @s 会变成这里选中的实体。"),
                            Map.entry("at", "把执行位置、朝向和维度移动到选中实体所在处。"),
                            Map.entry("positioned", "只改变命令的执行位置。"),
                            Map.entry("rotated", "改变命令的执行朝向。"),
                            Map.entry("facing", "让执行朝向指向某个坐标或实体。"),
                            Map.entry("anchored", "选择局部坐标从执行者脚部还是眼睛开始计算。"),
                            Map.entry("in", "改变命令执行所在的维度。"),
                            Map.entry("if", "条件成立时才继续执行后续命令。"),
                            Map.entry("unless", "条件不成立时才继续执行后续命令。"),
                            Map.entry("store", "把 run 命令的结果值或成功值存到分数、NBT 或 bossbar。"),
                            Map.entry("run", "结束 execute 链，并运行后面的嵌套命令。")
                    ),
                    Map.ofEntries(
                            Map.entry("targets", "实体选择器或玩家名。"),
                            Map.entry("pos", "世界坐标，支持绝对坐标、相对坐标 ~ 和局部坐标 ^。"),
                            Map.entry("block", "方块 id 或方块谓词。"),
                            Map.entry("command", "要在当前执行上下文中运行的嵌套命令。"),
                            Map.entry("objective", "计分板目标名。"),
                            Map.entry("scale", "存储数字结果前使用的倍率。")
                    )),
            doc("data",
                    "读取、写入、合并、删除或修改方块实体、实体、storage 中的 NBT 数据。",
                    List.of("data get entity <target> <path>", "data merge block <targetPos> <nbt>", "data modify entity <target> <targetPath> set value <value>"),
                    Map.ofEntries(
                            Map.entry("get", "读取 NBT 数据，并输出到聊天栏或命令输出。"),
                            Map.entry("merge", "把给定复合标签合并到已有 NBT。"),
                            Map.entry("modify", "复制、插入、追加、前置、设置或合并某个 NBT 值。"),
                            Map.entry("remove", "删除某条 NBT 路径。"),
                            Map.entry("entity", "操作实体的 NBT 数据。"),
                            Map.entry("block", "操作方块实体的 NBT 数据。"),
                            Map.entry("storage", "操作命令 storage 中的 NBT 数据。")
                    ),
                    Map.ofEntries(
                            Map.entry("target", "单个实体选择器。"),
                            Map.entry("targetPos", "方块实体所在坐标。"),
                            Map.entry("path", "NBT 路径，例如 Inventory[0].tag.display.Name。"),
                            Map.entry("nbt", "复合 NBT 值。"),
                            Map.entry("value", "要写入的 NBT 值。")
                    )),
            doc("scoreboard",
                    "管理计分板目标、玩家分数、显示栏位和分数运算。",
                    List.of("scoreboard objectives add <objective> dummy", "scoreboard players set <targets> <objective> <score>", "scoreboard players operation <targets> <targetObjective> += <source> <sourceObjective>"),
                    Map.ofEntries(
                            Map.entry("objectives", "创建、删除、列出、修改或显示计分板目标。"),
                            Map.entry("players", "读取和写入玩家、实体或假名玩家的分数。"),
                            Map.entry("operation", "在两个分数之间执行算术或交换操作。")
                    ),
                    Map.ofEntries(
                            Map.entry("objective", "计分板目标名。"),
                            Map.entry("targets", "玩家、假名玩家或实体选择器。"),
                            Map.entry("score", "整数分数值。"),
                            Map.entry("operation", "分数运算，例如 +=、-=、*=、/=、%=、=、<、> 或 ><。")
                    )),
            doc("summon", "在指定位置生成实体，可附带 NBT 数据。", List.of("summon <entity> <pos> <nbt>"), Map.of(), Map.ofEntries(
                    Map.entry("entity", "实体类型 id，例如 minecraft:zombie。"),
                    Map.entry("pos", "实体生成位置。"),
                    Map.entry("nbt", "可选的实体 NBT 复合标签。")
            )),
            doc("give", "把物品给予玩家。", List.of("give <targets> <item> <count>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "接收物品的玩家。"),
                    Map.entry("item", "物品 id，可附带组件。"),
                    Map.entry("count", "给予数量。")
            )),
            doc("tp", "把实体传送到坐标、朝向或另一个实体处。", List.of("tp <targets> <location>", "tp <targets> <destination>"), Map.of(), teleportArgumentsZh()),
            doc("teleport", "把实体传送到坐标、朝向或另一个实体处。", List.of("teleport <targets> <location>", "teleport <targets> <destination>"), Map.of(), teleportArgumentsZh()),
            doc("setblock",
                    "修改指定坐标处的单个方块。",
                    List.of("setblock <pos> <block> replace"),
                    Map.ofEntries(
                            Map.entry("destroy", "先破坏旧方块并掉落战利品，再放置新方块。"),
                            Map.entry("keep", "只在目标位置为空气时放置。"),
                            Map.entry("replace", "直接替换已有方块，不掉落旧方块。")
                    ),
                    Map.ofEntries(
                            Map.entry("pos", "目标方块坐标。"),
                            Map.entry("block", "要放置的方块 id、方块状态或方块 NBT。")
                    )),
            doc("fill",
                    "用方块填充一个长方体区域。",
                    List.of("fill <from> <to> <block> replace"),
                    Map.ofEntries(
                            Map.entry("destroy", "破坏旧方块并掉落战利品。"),
                            Map.entry("hollow", "只填充外壳，内部变为空气。"),
                            Map.entry("outline", "只填充外壳，内部保持不变。"),
                            Map.entry("replace", "替换匹配方块，未给过滤器时替换全部。"),
                            Map.entry("keep", "只填充空气方块。")
                    ),
                    Map.ofEntries(
                            Map.entry("from", "长方体的第一个角点。"),
                            Map.entry("to", "长方体的对角点。"),
                            Map.entry("block", "要填充的方块。"),
                            Map.entry("filter", "replace 模式下可选的方块过滤器。")
                    )),
            doc("effect",
                    "给予、清除或查询实体的状态效果。",
                    List.of("effect give <targets> <effect> <seconds> <amplifier>"),
                    Map.ofEntries(
                            Map.entry("give", "给予状态效果。"),
                            Map.entry("clear", "清除状态效果。")
                    ),
                    Map.ofEntries(
                            Map.entry("targets", "获得或失去效果的实体。"),
                            Map.entry("effect", "状态效果 id。"),
                            Map.entry("seconds", "持续时间，单位为秒。"),
                            Map.entry("amplifier", "效果等级，注意这里从 0 开始。")
                    )),
            doc("attribute",
                    "读取或修改实体属性以及属性修饰符。",
                    List.of("attribute <target> <attribute> get", "attribute <target> <attribute> base set <value>"),
                    Map.ofEntries(
                            Map.entry("base", "读取或写入属性基础值。"),
                            Map.entry("modifier", "添加、删除或查询属性修饰符。")
                    ),
                    Map.ofEntries(
                            Map.entry("target", "单个实体。"),
                            Map.entry("attribute", "属性 id。"),
                            Map.entry("value", "数字属性值。")
                    )),
            doc("clear", "从玩家背包中移除符合条件的物品。", List.of("clear <targets> <item> <maxCount>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "要检查背包的玩家。"),
                    Map.entry("item", "可选的物品或物品谓词。"),
                    Map.entry("maxCount", "最多移除的数量；填 0 时只检测而不移除。")
            )),
            doc("clone", "把一个长方体区域中的方块复制到另一个位置。", List.of("clone <begin> <end> <destination> replace normal"), Map.ofEntries(
                    Map.entry("replace", "复制全部方块，包括空气。"),
                    Map.entry("masked", "只复制非空气方块。"),
                    Map.entry("filtered", "只复制符合过滤条件的方块。"),
                    Map.entry("normal", "源区域和目标区域重叠时失败。"),
                    Map.entry("force", "允许源区域和目标区域重叠。"),
                    Map.entry("move", "复制后把源区域替换为空气，相当于移动。")
            ), Map.ofEntries(
                    Map.entry("begin", "源区域的第一个角点。"),
                    Map.entry("end", "源区域的对角点。"),
                    Map.entry("destination", "目标区域坐标最小的一角。")
            )),
            doc("damage", "对单个实体造成指定数值和类型的伤害。", List.of("damage <target> <amount>"), Map.ofEntries(
                    Map.entry("at", "使用某个坐标作为伤害来源位置。"),
                    Map.entry("by", "使用某个实体作为直接伤害来源。"),
                    Map.entry("from", "指定最终导致此次伤害的实体。")
            ), Map.ofEntries(
                    Map.entry("target", "受到伤害的单个实体。"),
                    Map.entry("amount", "非负伤害数值。"),
                    Map.entry("damageType", "伤害类型 id。"),
                    Map.entry("location", "伤害来源坐标。"),
                    Map.entry("entity", "直接伤害来源实体。"),
                    Map.entry("cause", "造成此次伤害的实体。")
            )),
            doc("enchant", "给选中实体主手中的物品添加附魔。", List.of("enchant <targets> <enchantment> <level>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "主手物品将被附魔的实体。"),
                    Map.entry("enchantment", "附魔 id。"),
                    Map.entry("level", "附魔允许范围内的等级。")
            )),
            doc("experience", "增加、设置或查询玩家的经验点数与等级。", List.of("experience add <targets> <amount> points"), Map.ofEntries(
                    Map.entry("add", "增加经验点数或等级。"),
                    Map.entry("set", "设置当前经验点数或等级。"),
                    Map.entry("query", "查询玩家的经验点数或等级。"),
                    Map.entry("points", "把数量视为经验点数。"),
                    Map.entry("levels", "把数量视为经验等级。")
            ), Map.ofEntries(
                    Map.entry("targets", "要修改经验的玩家。"),
                    Map.entry("amount", "经验点数或等级数量。"),
                    Map.entry("target", "要查询的单个玩家。")
            )),
            doc("xp", "/experience 的别名，用于增加、设置或查询玩家经验。", List.of("xp add <targets> <amount> points"), Map.ofEntries(
                    Map.entry("add", "增加经验点数或等级。"),
                    Map.entry("set", "设置经验点数或等级。"),
                    Map.entry("query", "查询经验点数或等级。")
            ), Map.ofEntries(
                    Map.entry("targets", "要修改经验的玩家。"),
                    Map.entry("amount", "经验点数或等级数量。"),
                    Map.entry("target", "要查询的单个玩家。")
            )),
            doc("gamemode", "修改一个或多个玩家的游戏模式。", List.of("gamemode <gameMode> <target>"), Map.of(), Map.ofEntries(
                    Map.entry("gameMode", "生存、创造、冒险或旁观模式。"),
                    Map.entry("target", "要修改游戏模式的玩家。")
            )),
            doc("gamerule", "读取或修改世界游戏规则。", List.of("gamerule <rule> <value>"), Map.of(), Map.ofEntries(
                    Map.entry("rule", "当前服务器命令树提供的游戏规则名。"),
                    Map.entry("value", "该规则接受的布尔值或整数值。")
            )),
            doc("kill", "立即杀死选中的实体。", List.of("kill <targets>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "要杀死的实体；省略时默认为命令执行者。")
            )),
            doc("locate", "查找最近的指定结构、生物群系或兴趣点。", List.of("locate structure <structure>"), Map.ofEntries(
                    Map.entry("structure", "搜索结构。"),
                    Map.entry("biome", "搜索生物群系。"),
                    Map.entry("poi", "搜索兴趣点类型。")
            ), Map.ofEntries(
                    Map.entry("structure", "结构 id 或结构标签。"),
                    Map.entry("biome", "生物群系 id 或标签。"),
                    Map.entry("poi", "兴趣点 id 或标签。")
            )),
            doc("particle", "生成粒子，可设置扩散范围、速度、数量和可见玩家。", List.of("particle <name> <pos>"), Map.ofEntries(
                    Map.entry("normal", "使用普通可见距离。"),
                    Map.entry("force", "使用远距离粒子可见范围。")
            ), Map.ofEntries(
                    Map.entry("name", "粒子类型及该粒子需要的附加参数。"),
                    Map.entry("pos", "粒子生成原点。"),
                    Map.entry("delta", "三个坐标轴上的随机扩散范围。"),
                    Map.entry("speed", "粒子速度倍率。"),
                    Map.entry("count", "生成粒子数量。"),
                    Map.entry("viewers", "能够看到粒子的玩家。")
            )),
            doc("playsound", "为选中的玩家播放声音。", List.of("playsound <sound> master <targets> <pos>"), Map.of(), Map.ofEntries(
                    Map.entry("sound", "声音事件 id。"),
                    Map.entry("source", "声音分类，例如 master、music、block 或 player。"),
                    Map.entry("targets", "听到声音的玩家。"),
                    Map.entry("pos", "声音来源坐标。"),
                    Map.entry("volume", "音量及可听范围倍率。"),
                    Map.entry("pitch", "播放音调。"),
                    Map.entry("minVolume", "超出正常可听范围后的最低音量。")
            )),
            doc("tellraw", "向选中的玩家发送富文本消息。", List.of("tellraw <targets> <message>"), Map.of(), Map.ofEntries(
                    Map.entry("targets", "接收消息的玩家。"),
                    Map.entry("message", "富文本组件，可包含颜色、样式、点击事件和翻译键。")
            )),
            doc("time", "读取或修改世界时间。", List.of("time set <time>"), Map.ofEntries(
                    Map.entry("add", "在当前时间上增加游戏刻。"),
                    Map.entry("set", "设置世界时间。"),
                    Map.entry("query", "查询昼夜时间、总游戏时间或天数。")
            ), Map.ofEntries(
                    Map.entry("time", "游戏刻数，或 day、noon、night、midnight 等命名时间。")
            )),
            doc("title", "显示标题、副标题或动作栏文本，并控制标题显示时间。", List.of("title <targets> title <title>"), Map.ofEntries(
                    Map.entry("title", "设置并显示主标题。"),
                    Map.entry("subtitle", "设置下次主标题同时显示的副标题。"),
                    Map.entry("actionbar", "在快捷栏上方显示文本。"),
                    Map.entry("times", "设置淡入、停留和淡出时间。"),
                    Map.entry("clear", "隐藏当前标题。"),
                    Map.entry("reset", "隐藏标题并重置显示时间。")
            ), Map.ofEntries(
                    Map.entry("targets", "接收标题的玩家。"),
                    Map.entry("title", "要显示的富文本组件。"),
                    Map.entry("fadeIn", "淡入时间，单位为游戏刻。"),
                    Map.entry("stay", "停留时间，单位为游戏刻。"),
                    Map.entry("fadeOut", "淡出时间，单位为游戏刻。")
            )),
            doc("weather", "修改世界天气，并可指定持续时间。", List.of("weather clear <duration>"), Map.ofEntries(
                    Map.entry("clear", "停止降雨和雷暴。"),
                    Map.entry("rain", "开始降雨。"),
                    Map.entry("thunder", "开始雷暴。")
            ), Map.ofEntries(
                    Map.entry("duration", "可选的天气持续秒数。")
            )),
            doc("function", "运行数据包中的一个或多个函数。", List.of("function <name>"), Map.of(), Map.ofEntries(
                    Map.entry("name", "函数 id，例如 namespace:path/to/function。")
            ))
    );

    private CommandDocLibrary() {
    }

    public static Optional<CommandDoc> find(String command) {
        String normalized = normalize(command);
        CommandDoc curated = activeDocs().get(normalized);
        if (curated != null) {
            return Optional.of(curated);
        }
        boolean chinese = CommandBlockStudio.useChineseCommandInsight();
        return VanillaCommandDocLibrary.find(normalized, chinese)
                .or(() -> ModCommandDocLibrary.find(normalized, chinese));
    }

    public static boolean isModDocumented(String command) {
        return ModCommandDocLibrary.contains(normalize(command));
    }

    public static int documentedCommandCount() {
        return VanillaCommandDocLibrary.size() + ModCommandDocLibrary.size();
    }

    private static Map<String, CommandDoc> activeDocs() {
        return CommandBlockStudio.useChineseCommandInsight() ? DOCS_ZH : DOCS_EN;
    }

    private static String normalize(String command) {
        if (command == null) {
            return "";
        }
        return command.startsWith("minecraft:") ? command.substring("minecraft:".length()) : command;
    }

    private static Map<String, String> teleportArgumentsEn() {
        return Map.ofEntries(
                Map.entry("targets", "Entities to teleport."),
                Map.entry("location", "Target position."),
                Map.entry("destination", "Entity to teleport to."),
                Map.entry("rotation", "Yaw and pitch after teleporting.")
        );
    }

    private static Map<String, String> teleportArgumentsZh() {
        return Map.ofEntries(
                Map.entry("targets", "要传送的实体。"),
                Map.entry("location", "目标坐标。"),
                Map.entry("destination", "要传送到的目标实体。"),
                Map.entry("rotation", "传送后的水平角和俯仰角。")
        );
    }

    private static Map.Entry<String, CommandDoc> doc(String name, String summary, List<String> examples, Map<String, String> nodes, Map<String, String> arguments) {
        return Map.entry(name, new CommandDoc(name, summary, examples, nodes, arguments));
    }
}
