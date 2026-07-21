package com.miofeather.commandblockstudio.main.insight;

import java.util.List;
import java.util.Map;
import java.util.Optional;

final class VanillaCommandDocLibrary {
    private static final Map<String, Seed> DOCS = Map.ofEntries(
            entry("advancement", "Grants, revokes, or tests player advancements.", "授予、撤销或检测玩家进度。", "advancement grant <targets> only <advancement>"),
            entry("attribute", "Reads and changes entity attributes and modifiers.", "读取或修改实体属性与修饰符。", "attribute <target> <attribute> get"),
            entry("ban", "Bans one or more player profiles from the server.", "封禁一个或多个玩家档案。", "ban <targets> <reason>"),
            entry("ban-ip", "Bans an IP address from the server.", "封禁服务器上的 IP 地址。", "ban-ip <target> <reason>"),
            entry("banlist", "Lists banned players or IP addresses.", "列出已封禁的玩家或 IP 地址。", "banlist players"),
            entry("bossbar", "Creates and manages custom boss bars.", "创建并管理自定义 Boss 栏。", "bossbar add <id> <name>"),
            entry("chase", "Starts or stops the development chase network tool.", "启动或停止开发环境的 chase 网络工具。", "chase start <serverHost> <serverPort> <clientPort>"),
            entry("clear", "Removes matching items from player inventories.", "从玩家背包中移除符合条件的物品。", "clear <targets> <item> <maxCount>"),
            entry("clone", "Copies blocks from one region to another.", "把一个区域中的方块复制到另一个位置。", "clone <begin> <end> <destination> replace normal"),
            entry("damage", "Deals a specified amount and type of damage to an entity.", "对实体造成指定数值和类型的伤害。", "damage <target> <amount>"),
            entry("data", "Reads and modifies entity, block, and storage NBT.", "读取和修改实体、方块实体及 storage 的 NBT。", "data get entity <target> <path>"),
            entry("datapack", "Enables, disables, and lists data packs.", "启用、禁用并列出数据包。", "datapack list available"),
            entry("debug", "Starts or stops server profiling and debug reporting.", "启动或停止服务器性能分析与调试报告。", "debug start"),
            entry("debugconfig", "Controls development debug configuration.", "控制开发环境调试配置。", "debugconfig config"),
            entry("debugpath", "Visualizes pathfinding for development diagnostics.", "显示用于开发诊断的寻路路径。", "debugpath <x> <y> <z>"),
            entry("defaultgamemode", "Changes the default game mode for new players.", "修改新玩家的默认游戏模式。", "defaultgamemode <gamemode>"),
            entry("deop", "Removes operator privileges from players.", "移除玩家的管理员权限。", "deop <targets>"),
            entry("difficulty", "Reads or changes the world difficulty.", "读取或修改世界难度。", "difficulty <difficulty>"),
            entry("effect", "Adds, clears, or queries status effects.", "给予、清除或查询状态效果。", "effect give <targets> <effect> <seconds> <amplifier>"),
            entry("enchant", "Adds an enchantment to held items.", "给实体手持物品添加附魔。", "enchant <targets> <enchantment> <level>"),
            entry("execute", "Runs commands with changed context or conditions.", "在改变执行上下文或检查条件后运行命令。", "execute as <targets> at @s run <command>"),
            entry("experience", "Adds, sets, or queries player experience.", "增加、设置或查询玩家经验。", "experience add <targets> <amount> points"),
            entry("fill", "Fills a cuboid region with blocks.", "用方块填充长方体区域。", "fill <from> <to> <block> replace"),
            entry("fillbiome", "Changes biomes inside a cuboid region.", "修改长方体区域内的生物群系。", "fillbiome <from> <to> <biome>"),
            entry("forceload", "Controls chunks that remain force-loaded.", "控制保持强制加载的区块。", "forceload add <from> <to>"),
            entry("function", "Runs functions or function tags from data packs.", "运行数据包中的函数或函数标签。", "function <name>"),
            entry("gamemode", "Changes player game modes.", "修改玩家游戏模式。", "gamemode <gameMode> <target>"),
            entry("gamerule", "Reads or changes world game rules.", "读取或修改世界游戏规则。", "gamerule <rule> <value>"),
            entry("give", "Gives item stacks to players.", "把物品给予玩家。", "give <targets> <item> <count>"),
            entry("help", "Shows syntax help for available commands.", "显示当前可用命令的语法帮助。", "help <command>"),
            entry("item", "Reads, replaces, or modifies inventory slots.", "读取、替换或修改物品栏槽位。", "item replace entity <targets> <slot> with <item> <count>"),
            entry("jfr", "Starts or stops a Java Flight Recorder profile.", "启动或停止 Java Flight Recorder 性能记录。", "jfr start"),
            entry("kick", "Disconnects players from the server.", "将玩家踢出服务器。", "kick <targets> <reason>"),
            entry("kill", "Immediately kills selected entities.", "立即杀死选中的实体。", "kill <targets>"),
            entry("list", "Lists players currently connected to the server.", "列出当前连接到服务器的玩家。", "list"),
            entry("locate", "Finds nearby structures, biomes, or points of interest.", "查找附近的结构、生物群系或兴趣点。", "locate structure <structure>"),
            entry("loot", "Moves generated loot into inventories or the world.", "把生成的战利品放入物品栏或世界。", "loot give <players> loot <loot_table>"),
            entry("me", "Broadcasts an action message from the executor.", "以命令执行者身份广播动作消息。", "me <action>"),
            entry("msg", "Sends a private message to players.", "向玩家发送私聊消息。", "msg <targets> <message>"),
            entry("op", "Grants operator privileges to players.", "授予玩家管理员权限。", "op <targets>"),
            entry("pardon", "Removes player profiles from the ban list.", "从封禁列表中移除玩家档案。", "pardon <targets>"),
            entry("pardon-ip", "Removes an IP address from the ban list.", "从封禁列表中移除 IP 地址。", "pardon-ip <target>"),
            entry("particle", "Spawns particles with configurable visibility and motion.", "生成可配置可见范围和运动参数的粒子。", "particle <name> <pos>"),
            entry("perf", "Starts or stops a dedicated-server performance recording.", "启动或停止专用服务器性能记录。", "perf start"),
            entry("place", "Places configured features, structures, jigsaws, or templates.", "放置配置地物、结构、拼图或结构模板。", "place feature <feature> <pos>"),
            entry("playsound", "Plays a sound for selected players.", "为选中的玩家播放声音。", "playsound <sound> master <targets> <pos>"),
            entry("publish", "Opens an integrated world to the local network.", "把单人世界开放到局域网。", "publish"),
            entry("raid", "Controls raids for development testing.", "控制用于开发测试的袭击事件。", "raid start <omenLevel>"),
            entry("random", "Generates random values or controls random sequences.", "生成随机值或控制随机序列。", "random value <range>"),
            entry("recipe", "Grants or revokes crafting recipes.", "授予或撤销合成配方。", "recipe give <players> <recipe>"),
            entry("reload", "Reloads data packs and server data.", "重新加载数据包和服务器数据。", "reload"),
            entry("return", "Returns a value from the current function.", "从当前函数返回一个值。", "return <value>"),
            entry("ride", "Makes an entity mount or dismount another entity.", "让实体骑乘、被骑乘或下车。", "ride <target> mount <vehicle>"),
            entry("save-all", "Saves all server worlds and player data.", "保存全部服务器世界和玩家数据。", "save-all flush"),
            entry("save-off", "Disables automatic world saving.", "关闭世界自动保存。", "save-off"),
            entry("save-on", "Enables automatic world saving.", "开启世界自动保存。", "save-on"),
            entry("say", "Broadcasts a chat message from the executor.", "以命令执行者身份广播聊天消息。", "say <message>"),
            entry("schedule", "Schedules functions to run later.", "安排函数在稍后运行。", "schedule function <function> <time> replace"),
            entry("scoreboard", "Manages objectives, scores, displays, and operations.", "管理计分板目标、分数、显示栏位和运算。", "scoreboard players set <targets> <objective> <score>"),
            entry("seed", "Displays the current world's seed.", "显示当前世界种子。", "seed"),
            entry("serverpack", "Controls the development server-pack test command.", "控制开发环境服务器资源包测试命令。", "serverpack push <url> <uuid>"),
            entry("setblock", "Changes one block at a position.", "修改指定坐标处的单个方块。", "setblock <pos> <block> replace"),
            entry("setidletimeout", "Sets the automatic idle-player kick timeout.", "设置自动踢出挂机玩家的超时时间。", "setidletimeout <minutes>"),
            entry("setworldspawn", "Changes the world's default spawn position.", "修改世界默认出生点。", "setworldspawn <pos> <angle>"),
            entry("spawn_armor_trims", "Spawns armor-trim combinations for development testing.", "生成用于开发测试的盔甲纹饰组合。", "spawn_armor_trims"),
            entry("spawnpoint", "Changes player respawn positions.", "修改玩家重生点。", "spawnpoint <targets> <pos> <angle>"),
            entry("spectate", "Makes a spectator observe an entity.", "让旁观者观察指定实体。", "spectate <target> <player>"),
            entry("spreadplayers", "Spreads entities across an area.", "把实体分散到指定区域。", "spreadplayers <center> <spreadDistance> <maxRange> false <targets>"),
            entry("stop", "Stops the dedicated server.", "停止专用服务器。", "stop"),
            entry("stopsound", "Stops sounds for selected players.", "停止选中玩家正在播放的声音。", "stopsound <targets> <source> <sound>"),
            entry("summon", "Creates an entity at a position.", "在指定位置生成实体。", "summon <entity> <pos> <nbt>"),
            entry("tag", "Adds, removes, or lists entity tags.", "添加、移除或列出实体标签。", "tag <targets> add <name>"),
            entry("team", "Creates and configures scoreboard teams.", "创建并配置计分板队伍。", "team add <team> <displayName>"),
            entry("teammsg", "Sends a message to the executor's team.", "向命令执行者所在队伍发送消息。", "teammsg <message>"),
            entry("teleport", "Teleports entities to coordinates or other entities.", "把实体传送到坐标或另一个实体处。", "teleport <targets> <location>"),
            entry("tell", "Alias of /msg for private messages.", "/msg 的别名，用于发送私聊消息。", "tell <targets> <message>"),
            entry("tellraw", "Sends rich text components to players.", "向玩家发送富文本组件。", "tellraw <targets> <message>"),
            entry("test", "Runs and manages GameTests in development environments.", "在开发环境中运行和管理 GameTest。", "test run <testName>"),
            entry("tick", "Controls, steps, freezes, or profiles server ticks.", "控制、步进、冻结或分析服务器刻。", "tick query"),
            entry("time", "Reads or changes world time.", "读取或修改世界时间。", "time set <time>"),
            entry("title", "Displays titles, subtitles, and action-bar text.", "显示标题、副标题和动作栏文本。", "title <targets> title <title>"),
            entry("tm", "Alias of /teammsg.", "/teammsg 的别名。", "tm <message>"),
            entry("tp", "Alias of /teleport.", "/teleport 的别名。", "tp <targets> <location>"),
            entry("transfer", "Transfers players to another server.", "把玩家转移到另一个服务器。", "transfer <hostname> <port> <players>"),
            entry("trigger", "Changes trigger-type scoreboard objectives for a player.", "修改玩家可触发类型的计分板目标。", "trigger <objective> add <value>"),
            entry("w", "Alias of /msg for private messages.", "/msg 的别名，用于发送私聊消息。", "w <targets> <message>"),
            entry("warden_spawn_tracker", "Controls Warden spawn tracking diagnostics.", "控制监守者生成追踪诊断。", "warden_spawn_tracker clear"),
            entry("weather", "Changes world weather for an optional duration.", "修改世界天气并可指定持续时间。", "weather clear <duration>"),
            entry("whitelist", "Manages the dedicated-server player whitelist.", "管理专用服务器玩家白名单。", "whitelist list"),
            entry("worldborder", "Reads and changes the world border.", "读取或修改世界边界。", "worldborder set <distance> <time>"),
            entry("xp", "Alias of /experience.", "/experience 的别名。", "xp add <targets> <amount> points")
    );

    private VanillaCommandDocLibrary() {
    }

    static Optional<CommandDoc> find(String command, boolean chinese) {
        Seed seed = DOCS.get(command);
        if (seed == null) {
            return Optional.empty();
        }
        return Optional.of(new CommandDoc(
                command,
                chinese ? seed.zhSummary() : seed.enSummary(),
                seed.examples(),
                Map.of(),
                Map.of()
        ));
    }

    static int size() {
        return DOCS.size();
    }

    private static Map.Entry<String, Seed> entry(String name, String enSummary, String zhSummary, String... examples) {
        return Map.entry(name, new Seed(enSummary, zhSummary, List.of(examples)));
    }

    private record Seed(String enSummary, String zhSummary, List<String> examples) {
    }
}
