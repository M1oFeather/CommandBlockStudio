# 版本选择

Command Block Studio 的模组版本与 Minecraft 版本是两个不同维度。下载时必须同时确认两者。

## 当前状态

| 模组版本 | 状态 | 说明 |
| --- | --- | --- |
| `1.1.0` | 待发布 | 当前开发版本，包含工作模式、聊天命令助手、物品命令摘要和运行测试 |
| `1.0.0` | 已发布 | 首个正式版本，冻结于 Git 提交 `6aff965` |

## Minecraft 构建

| Minecraft | Git 分支 | 文件名格式 | 文档 |
| --- | --- | --- | --- |
| `1.21.1` | `1.21.1`（默认分支） | `command_block_studio-<模组版本>-1.21.1-NeoForge.jar` | 公用文档源 |
| `1.21.4` | `1.21.4` | `command_block_studio-<模组版本>-1.21.4-NeoForge.jar` | 使用公用文档 |
| `26.2` | `26.2` | `command_block_studio-<模组版本>-26.2-NeoForge.jar` | 使用公用文档 |

不要把 `1.21.1` 的 JAR 放进 `1.21.4` 或 `26.2`。NeoForge 会根据模组元数据检查 Minecraft 和加载器版本。

## 玩家如何选择

1. 在 Minecraft 主菜单或启动器实例中确认游戏版本。
2. 确认实例使用 NeoForge，而不是 Forge、Fabric 或其他加载器。
3. 下载文件名中 Minecraft 版本完全一致的 JAR。
4. 客户端、服务端或双方均可安装；不同端的模组版本可以不同。
5. 双端接口不兼容时，只禁用对应的共享功能，不阻止连接。

### 安装方式差异

| 安装位置 | 可用能力 |
| --- | --- |
| 仅客户端 | Studio 编辑器、聊天命令助手、补全、文档、诊断、本地预览 |
| 仅服务端 | 原版客户端仍可加入；不强制安装客户端模组 |
| 双端 | 额外启用共享注释、十次编辑记录、回滚、运行测试等协商功能 |

## 开发者如何切换

```powershell
git switch 1.21.1
git switch 1.21.4
git switch 26.2
```

`1.21.1` 是仓库默认分支和公共文档唯一事实源。修改其他版本代码时，不要复制一套新的发布文档。

切换后执行：

```powershell
.\gradlew.bat --no-configuration-cache --console plain clean build
```

最终检查 `build/libs/` 中的文件名，并读取 JAR 内 `META-INF/neoforge.mods.toml` 的版本值。
