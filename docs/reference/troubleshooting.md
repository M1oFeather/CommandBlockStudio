# 故障排查

## 打开命令方块仍是原版界面

依次确认：

1. 游戏版本是 Minecraft 26.2。
2. 加载器是 NeoForge 26.2.0.32-beta。
3. 模组列表中存在 `command_block_studio`。
4. `mods` 目录中没有重复版本。
5. 玩家拥有命令方块编辑权限。

检查 `logs/latest.log` 中是否出现 Mixin 应用失败、类加载错误或强制依赖错误。

## 缩放后编辑器为空

先在设置中把 Studio 缩放改为“自动”，或关闭游戏后编辑：

```toml
ui_scale_percent = 0
```

如果仍然异常，记录以下信息：

- 游戏窗口分辨率和全屏状态。
- Minecraft GUI 缩放设置。
- 是否安装 Modern UI 或其他 GUI 缩放模组。
- 当前 Studio 缩放百分比。

## 提示或补全不完整

补全来自当前连接下发的 Brigadier 命令树。确认目标命令在当前世界或服务器中已注册，并且玩家有权限查看。

文本 Component 和物品数据组件允许在结构尚未闭合时继续提示，但最终仍由 Minecraft 26.2 原生 Codec 校验。模组自定义参数若不公开内部结构，只能显示命令树能够提供的候选和类型回退说明。

## 命令显示错误但仍未输入完

坐标、旋转等复合参数缺少后续值时应显示半透明占位符，而不是错误。如果状态栏已经显示精确错误，请点击右侧跳转按钮并检查错误列附近是否存在：

- 多余或缺失的引号。
- 未闭合的 JSON / SNBT 括号。
- 旧版本 NBT 语法。
- 当前服务器没有注册的命令或参数值。

## 共享注释入口不存在

`//` 入口只在服务端提供对应注释载荷后出现，不要求客户端与服务端的模组版本号完全一致。还需要玩家能够使用游戏管理员命令方块，并位于目标方块 64 格范围内。

## `clientRunVmArgs.txt` 无法打开

IDE 的 Client 运行配置依赖 ModDevGradle 生成文件。执行：

=== "Windows PowerShell"

    ```powershell
    .\gradlew.bat --no-configuration-cache --console plain prepareClientRun
    ```

=== "Linux / macOS"

    ```bash
    ./gradlew --no-configuration-cache --console plain prepareClientRun
    ```

成功后应存在：

```text
build/moddev/clientRunVmArgs.txt
build/moddev/clientRunProgramArgs.txt
```

常规 `build` 也会在结束时刷新这些文件。

## 重置客户端配置

1. 关闭游戏。
2. 备份并删除 `config/command_block_studio-client.toml`。
3. 重新启动游戏，让 NeoForge 生成默认配置。

## 收集日志

报告问题时提供：

- `logs/latest.log`。
- 对应的崩溃报告（如果生成）。
- Minecraft、NeoForge、Java 和模组版本。
- 能稳定复现问题的命令文本。
- GUI 缩放、窗口尺寸以及相关界面模组列表。

请先移除日志中的服务器地址、玩家令牌或其他隐私信息。
