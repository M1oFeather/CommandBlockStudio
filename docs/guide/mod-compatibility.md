# 模组命令软兼容

Command Block Studio 不依赖命令模组，也不把其他模组类链接进客户端代码。命令是否存在、玩家能否看到、语法分支和候选值都来自当前世界或服务器下发的 Brigadier 命令树。

## 兼容层次

| 层次 | 行为 |
| --- | --- |
| 命令树 | 自动支持任何正常注册并同步到客户端的命令，包括语法着色、补全、错误定位和参数签名 |
| 文档 | 对已确认的模组根命令补充中英文用途、常见分支和示例 |
| 注册表预览 | 自定义参数名可识别为方块、物品、粒子或玩家时，显示当前注册表图标、粒子纹理或玩家头像 |
| 通用回退 | 未收录命令仍显示 Brigadier 字面量、参数名、参数类型和最短可执行用法 |

因此，安装或移除下列模组不会改变 Studio 自身的加载结果；文档只会在相应命令实际出现在服务器命令树中时被使用。

## 已收录范围

本轮适配以提供的 `modrinth.index.json` 中的实际 JAR 版本为依据，确认并收录了以下命令根：

| 模组 | 已确认命令 |
| --- | --- |
| WorldEdit 7.3.8 | `worldedit`、`we`、`//set`、`//replace`、`//copy`、`//cut`、`//paste`、`/undo`、`/redo`、选区角点与魔杖 |
| NeoForge Carpet 1.0.8 | `counter`、`draw`、`spawn`、`player`、`info`、`log` 及常用诊断根 |
| Create 6.0.10 | `create` 下的列车、强力胶、连接、乘客、装配高亮与铁路诊断分支 |
| CMDCam 2.2.8 | `cam-server` 场景创建、启动、暂停、恢复与停止 |
| Corpse 1.1.13 | `deathhistory` |
| Dynamic Trees 1.7.2 | `dt`、`dynamictrees` 及树木查询、设置、生长和注册表分支 |
| Exposure 1.9.18 | `exposure`、`shader` |
| Serene Seasons 10.1.0.3 | `season get`、`season set` |
| Supplementaries 3.8.0 | `supplementaries`、`supp` |
| WaterFrames 2.1.23 | `waterframes` 的编辑、审计、给予与白名单分支 |
| SecurityCraft 1.10.2.1 | `sc`、`securitycraft` 的转换、所有权与帮助分支 |
| Yes Steve Model 2.5.1 | `ysm` 根命令；具体分支继续以服务端命令树为准 |

Yuushya 等索引内未注册服务端命令的内容模组不需要单独命令适配。若后续版本新增命令，它们仍会先进入通用 Brigadier 回退链路。

## 运行验证

通用命令树适配已迁移到 Minecraft 26.2、NeoForge 26.2.0.32-beta 和 Java 25；本页列出的第三方模组组合启动结论来自 1.21.1 分支，26.2 使用时仍需选择对应游戏版本的模组文件：

- WorldEdit、NeoForge Carpet、CMDCam、Corpse、Dynamic Trees、Exposure、Serene Seasons、Supplementaries、WaterFrames、SecurityCraft 及其必要前置可共同启动 dedicated server，并到达 `Done`。
- 同一组合可启动客户端、进入单人世界、打开四方块连锁页签，并实际检查编辑器、搜索替换和转换器布局。
- Command Block Studio 不直接引用这些模组的类；组合中出现的可选集成告警、非致命错误、缺少翻译或其他模组自身日志不会改变 Studio 的加载方式。

以下两项受提供文件本身限制，不计入组合启动结论：

- Create 6.0.10 的元数据要求 NeoForge 21.1.219 或更高版本。Studio 已收录其命令文档，但不能在目标 21.1.217 运行环境中强行加载该 JAR。
- Yes Steve Model 2.5.1 在本机 Windows 客户端与 dedicated server 测试中都因其原生库返回 `err: 54` 而停止；Studio 仅按服务器实际下发的 `ysm` 命令树启用文档，不会主动加载 YSM 类。

## 预览识别

原版参数类型可以直接表明候选类别。部分模组使用自定义参数类型，因此 Studio 还会检查当前参数名：

- `block`、`blockstate`、`state`、`palette`：尝试显示方块图标与状态属性。
- `item`、`itemstack`、`stack`：尝试显示物品图标和本地化名称。
- `particle`、`particletype`、`effect`：尝试显示粒子纹理和粒子文档。
- `player`、`profile`、`target`：在线玩家优先显示 Tab 头像，离线名称使用稳定默认头像。

候选必须同时存在于当前客户端注册表中才会显示预览，无法识别时保留普通文本候选，不影响接受或执行。

## 限制

- 服务器没有向当前玩家同步的权限命令，Studio 无法主动绕过权限显示。
- 模组参数若既不提供候选，也没有可识别的参数名，Studio 只能显示通用参数类型说明。
- 文档示例用于解释命令形态，最终可用分支以当前服务器补全为准。
- 客户端注册表没有对应模组内容时，不显示图标预览，但命令文本仍可编辑。
- “软兼容”表示从 Brigadier 和注册表读取能力，不表示 Studio 替其他模组放宽其 NeoForge 版本、前置或原生库要求。
