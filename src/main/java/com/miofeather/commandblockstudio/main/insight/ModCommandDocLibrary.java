package com.miofeather.commandblockstudio.main.insight;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ModCommandDocLibrary {
    private static final Map<String, Seed> DOCS = createDocs();

    private ModCommandDocLibrary() {
    }

    static Optional<CommandDoc> find(String command, boolean chinese) {
        Seed seed = DOCS.get(command);
        if (seed == null) {
            return Optional.empty();
        }
        Map<String, String> nodes = new HashMap<>();
        seed.nodes().forEach((name, text) -> nodes.put(name, chinese ? text.zh() : text.en()));
        return Optional.of(new CommandDoc(
                command,
                chinese ? seed.summary().zh() : seed.summary().en(),
                seed.examples(),
                Map.copyOf(nodes),
                Map.of()
        ));
    }

    static boolean contains(String command) {
        return DOCS.containsKey(command);
    }

    static int size() {
        return DOCS.size();
    }

    private static Map<String, Seed> createDocs() {
        Map<String, Seed> docs = new HashMap<>();

        add(docs, List.of("create"), seed(
                "Create administration and diagnostics, including trains, glue, couplings, and contraptions.",
                "Create 的管理与诊断命令，涵盖列车、强力胶、连接器和机械结构。",
                List.of("create train tp <train>", "create glue <from> <to>", "create coupling add <cart1> <cart2>"),
                nodes(
                        node("train", "Removes a train or teleports to it.", "移除列车或传送到列车。"),
                        node("glue", "Applies glue across a selected cuboid.", "在选定长方体区域内应用强力胶。"),
                        node("coupling", "Adds or removes minecart couplings.", "添加或移除矿车连接。"),
                        node("passenger", "Moves a rider to a contraption seat.", "把乘客移动到机械结构座位。"),
                        node("highlight", "Highlights a Create assembly problem.", "高亮显示 Create 结构装配问题。"),
                        node("trains", "Prints railway and train diagnostics.", "输出铁路与列车诊断信息。")
                )));

        add(docs, List.of("counter"), seed(
                "Reads or resets Carpet hopper counters.", "读取或重置 Carpet 漏斗计数器。",
                List.of("counter <color>", "counter <color> reset"), Map.of()));
        add(docs, List.of("draw"), seed(
                "Generates geometric block shapes through Carpet.", "通过 Carpet 生成几何方块形状。",
                List.of("draw sphere <center> <radius> <block>", "draw cylinder <center> <radius> <height> <block>"),
                nodes(
                        node("sphere", "Draws a hollow sphere.", "绘制空心球体。"),
                        node("ball", "Draws a filled sphere.", "绘制实心球体。"),
                        node("cylinder", "Draws a cylinder.", "绘制圆柱体。"),
                        node("cuboid", "Draws a cuboid.", "绘制长方体。")
                )));
        add(docs, List.of("spawn"), seed(
                "Inspects and tests Carpet mob spawning behavior.", "检查并测试 Carpet 生物生成行为。",
                List.of("spawn list <pos>", "spawn tracking start", "spawn mobcaps"),
                nodes(
                        node("tracking", "Starts or stops spawn tracking.", "开始或停止生成追踪。"),
                        node("mobcaps", "Reads or adjusts mob caps.", "读取或调整生物容量。"),
                        node("rates", "Reads or resets spawn rates.", "读取或重置生成速率。")
                )));
        add(docs, List.of("player"), seed(
                "Spawns and controls Carpet fake players or existing players.", "生成并控制 Carpet 假人或现有玩家。",
                List.of("player <player> spawn", "player <player> attack continuous", "player <player> stop"),
                nodes(
                        node("spawn", "Spawns a fake player with optional location and game mode.", "生成假人，并可指定位置和游戏模式。"),
                        node("attack", "Makes the player attack once, continuously, or at intervals.", "让玩家攻击一次、持续攻击或间隔攻击。"),
                        node("use", "Makes the player use the held item.", "让玩家使用手持物品。"),
                        node("stop", "Stops all scripted player actions.", "停止全部脚本化玩家动作。")
                )));
        add(docs, List.of("info"), seed(
                "Shows Carpet block and world diagnostics.", "显示 Carpet 方块与世界诊断信息。",
                List.of("info block <block position>"), Map.of()));
        add(docs, List.of("log"), seed(
                "Subscribes players to Carpet loggers.", "让玩家订阅 Carpet 日志记录器。",
                List.of("log", "log <log name> <option> <player>"), Map.of()));
        add(docs, List.of("profile", "distance", "perimeterinfo", "mobai"), seed(
                "Carpet diagnostics supplied by the active server command tree.",
                "由当前服务器命令树提供的 Carpet 诊断命令。",
                List.of(), Map.of()));

        add(docs, List.of("cam-server"), seed(
                "Creates and controls CMDCam server camera scenes.", "创建并控制 CMDCam 服务端镜头场景。",
                List.of("cam-server create <name>", "cam-server start <players> <name>", "cam-server stop <players>"),
                nodes(
                        node("create", "Creates a named camera scene.", "创建命名镜头场景。"),
                        node("start", "Starts a scene for selected players.", "为选中的玩家启动场景。"),
                        node("pause", "Pauses the running camera path.", "暂停正在运行的镜头路径。"),
                        node("resume", "Resumes a paused camera path.", "恢复暂停的镜头路径。"),
                        node("stop", "Stops the camera path for selected players.", "停止选中玩家的镜头路径。")
                )));
        add(docs, List.of("deathhistory"), seed(
                "Opens Corpse death history for a player profile.", "打开指定玩家的 Corpse 死亡记录。",
                List.of("deathhistory", "deathhistory <player>"), Map.of()));

        Seed dynamicTrees = seed(
                "Inspects and edits Dynamic Trees at a world position.", "检查或编辑世界中的 Dynamic Trees。",
                List.of("dt gettree <location>", "dt settree <location> <species> <jo_code>", "dt growpulse <location> <number>"),
                nodes(
                        node("gettree", "Reads species, JoCode, rotation, and fertility.", "读取树种、JoCode、旋转与肥力。"),
                        node("settree", "Places or replaces a dynamic tree.", "放置或替换动态树。"),
                        node("growpulse", "Applies one or more growth pulses.", "施加一次或多次生长脉冲。"),
                        node("killtree", "Kills the dynamic tree at the location.", "杀死指定位置的动态树。"),
                        node("registry", "Lists Dynamic Trees registry entries.", "列出 Dynamic Trees 注册表条目。")
                ));
        add(docs, List.of("dt", "dynamictrees"), dynamicTrees);

        add(docs, List.of("exposure"), seed(
                "Loads, exposes, exports, displays, and adjusts Exposure photographs.",
                "加载、曝光、导出、显示并调整 Exposure 照片。",
                List.of("exposure expose <capture_properties>", "exposure show latest <player>", "exposure export <id>"),
                nodes(
                        node("load", "Loads an exposure into command context.", "把照片加载到命令上下文。"),
                        node("expose", "Creates an exposure from capture properties.", "根据拍摄属性生成照片。"),
                        node("show", "Shows an exposure to players.", "向玩家显示照片。"),
                        node("export", "Exports an exposure.", "导出照片。"),
                        node("palette", "Changes or inspects an exposure color palette.", "修改或检查照片调色板。")
                )));
        add(docs, List.of("shader"), seed(
                "Applies or removes an Exposure shader for selected players.",
                "为选中的玩家应用或移除 Exposure 着色器。",
                List.of("shader apply <targets> <shader_location>", "shader remove <targets>"),
                nodes(
                        node("apply", "Applies the selected shader.", "应用选定着色器。"),
                        node("remove", "Removes the active shader.", "移除当前着色器。")
                )));
        add(docs, List.of("season"), seed(
                "Reads or changes the current Serene Seasons season.", "读取或修改 Serene Seasons 当前季节。",
                List.of("season get", "season set <season>"),
                nodes(
                        node("get", "Displays the current season.", "显示当前季节。"),
                        node("set", "Changes the current season.", "修改当前季节。")
                )));

        Seed supplementaries = seed(
                "Supplementaries administration for globes, cages, records, maps, and configs.",
                "Supplementaries 的地球仪、笼子、唱片、地图与配置管理命令。",
                List.of("supplementaries globe", "supplementaries configs"),
                nodes(
                        node("globe", "Changes or resets globe seeds.", "修改或重置地球仪种子。"),
                        node("configs", "Opens or reloads Supplementaries configuration.", "打开或重载 Supplementaries 配置。")
                ));
        add(docs, List.of("supplementaries", "supp"), supplementaries);

        add(docs, List.of("waterframes"), seed(
                "Edits WaterFrames media blocks and manages media access.", "编辑 WaterFrames 媒体方块并管理媒体访问。",
                List.of("waterframes edit <blockpos> url <url>", "waterframes edit <blockpos> size <width> <height>", "waterframes give <targets>"),
                nodes(
                        node("edit", "Edits URL, position, size, rotation, brightness, playback, or volume.", "编辑 URL、位置、尺寸、旋转、亮度、播放状态或音量。"),
                        node("audit", "Audits media blocks by author or range.", "按作者或范围审计媒体方块。"),
                        node("give", "Gives WaterFrames items to players.", "给予玩家 WaterFrames 物品。"),
                        node("whitelist", "Manages allowed media URLs.", "管理允许访问的媒体 URL。")
                )));

        Seed securityCraft = seed(
                "SecurityCraft administration for conversions, ownership, and diagnostics.",
                "SecurityCraft 的方块转换、所有权与诊断管理命令。",
                List.of("sc convert set <pos> <mode>", "sc owner set <pos> <player>", "securitycraft help"),
                nodes(
                        node("convert", "Converts one block or a filled region between variants.", "转换单个方块或区域内的方块变体。"),
                        node("owner", "Sets or resets ownership for one block or a region.", "设置或重置单个方块或区域的所有者。"),
                        node("help", "Shows SecurityCraft command help.", "显示 SecurityCraft 命令帮助。")
                ));
        add(docs, List.of("sc", "securitycraft"), securityCraft);

        add(docs, List.of("ysm"), seed(
                "Yes Steve Model server commands. Available branches are supplied by the server.",
                "Yes Steve Model 服务端命令；可用分支由服务器命令树提供。",
                List.of("ysm"), Map.of()));

        add(docs, List.of("worldedit", "we"), seed(
                "WorldEdit session, configuration, and utility commands.", "WorldEdit 会话、配置与实用命令。",
                List.of("worldedit help"), Map.of()));
        add(docs, List.of("/set"), seed(
                "Fills the current WorldEdit selection with a block pattern.", "用方块模式填充当前 WorldEdit 选区。",
                List.of("//set <pattern>"), Map.of()));
        add(docs, List.of("/replace"), seed(
                "Replaces matching blocks in the current WorldEdit selection.", "替换当前 WorldEdit 选区内匹配的方块。",
                List.of("//replace <from> <to>"), Map.of()));
        add(docs, List.of("/copy", "/cut", "/paste"), seed(
                "WorldEdit clipboard operation for the current selection.", "针对当前选区的 WorldEdit 剪贴板操作。",
                List.of("//copy", "//cut", "//paste"), Map.of()));
        add(docs, List.of("undo", "redo"), seed(
                "Undoes or redoes WorldEdit session changes.", "撤销或重做 WorldEdit 会话修改。",
                List.of("undo", "redo"), Map.of()));
        add(docs, List.of("/pos1", "/pos2"), seed(
                "Sets a WorldEdit selection corner, optionally from coordinates.", "设置 WorldEdit 选区角点，可直接指定坐标。",
                List.of("//pos1 <coordinates>", "//pos2 <coordinates>"), Map.of()));
        add(docs, List.of("/wand"), seed(
                "Gives the WorldEdit selection wand.", "给予 WorldEdit 选区工具。",
                List.of("//wand"), Map.of()));

        return Map.copyOf(docs);
    }

    private static Seed seed(String en, String zh, List<String> examples, Map<String, Text> nodes) {
        return new Seed(new Text(en, zh), examples, nodes);
    }

    private static Map<String, Text> nodes(Node... nodes) {
        Map<String, Text> result = new HashMap<>();
        for (Node node : nodes) {
            result.put(node.name(), node.text());
        }
        return Map.copyOf(result);
    }

    private static Node node(String name, String en, String zh) {
        return new Node(name, new Text(en, zh));
    }

    private static void add(Map<String, Seed> docs, List<String> names, Seed seed) {
        names.forEach(name -> docs.put(name, seed));
    }

    private record Text(String en, String zh) {
    }

    private record Node(String name, Text text) {
    }

    private record Seed(Text summary, List<String> examples, Map<String, Text> nodes) {
    }
}
