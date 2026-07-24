# Command Block Studio

<p align="center">
  <img class="cbs-logo" src="assets/icon.png" alt="Command Block Studio 图标">
</p>

Command Block Studio 是面向 Minecraft 1.21.1 的 NeoForge 原生命令方块编辑工作台。它把原版单行输入框替换为具有多行排版、Brigadier 补全、快速文档、语法诊断和工作区页签的编辑器。

!!! info "原生 NeoForge 实现"
    项目基于 NeoForge 官方 MDK 结构，目标版本为 NeoForge `21.1.217`，不依赖 Fabric API、Architectury 或加载器兼容层。

<div class="grid cards" markdown>

-   **安装与兼容性**

    ---

    确认 Minecraft、NeoForge、Java 和客户端/服务端安装方式。

    [开始安装](getting-started/installation.md)

-   **第一次使用**

    ---

    打开命令方块，认识活动栏、页签、编辑器和保存流程。

    [快速上手](getting-started/quick-start.md)

-   **命令智能提示**

    ---

    使用原生命令树补全、模板、Component 补全和精确错误定位。

    [查看提示系统](command-insight.md)

-   **故障排查**

    ---

    处理界面未替换、缩放异常、运行参数缺失和服务端功能不可用。

    [排查问题](reference/troubleshooting.md)

</div>

## 兼容性

<div class="cbs-compatibility-table" markdown>

| 项目 | 目标 |
| --- | --- |
| Minecraft | `1.21.1` |
| NeoForge | `21.1.217` |
| Java | `21` |
| Mod ID | `command_block_studio` |
| 当前版本 | `1.2.0` |
| 客户端 | 可选；安装后提供 Studio 编辑器 |
| 服务端 | 可选；安装后提供共享注释、编辑记录和增强的远程页签同步 |

</div>

## 主要能力

- 多行命令编辑、行号、语法着色和括号配对。
- 聊天内容以 `/` 开头时自动展开四分之一屏幕宽的命令助手。
- 当前连接 Brigadier 命令树驱动的原生补全。
- 文本 Component、物品数据组件和粒子参数的结构化提示。
- 当前方块、连锁方块和固定方块组成的工作区页签。
- 可选择自动保存，或在退出、切换页签时确认未保存修改。
- 方块、附魔、玩家头像和粒子纹理等可视化候选。
- 服务端可选安装后的共享命令方块注释。

## 安装范围

仅希望获得增强编辑器时，只在客户端安装即可。服务器没有安装模组时，原版命令提交仍然正常工作；共享注释和服务端辅助打开页签会自动隐藏或回退。反过来，服务端可以单独安装，未安装模组的客户端仍可正常连接并使用原版界面。

需要多人共同维护命令方块说明、使用工作模式远程预览或运行测试时，请在服务端也安装支持对应接口的版本。详细区别见[服务端增强](guide/server-features.md)。
