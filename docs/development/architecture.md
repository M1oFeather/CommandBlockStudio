# 架构说明

## 加载边界

项目分为公共入口和客户端 UI：

- `CommandBlockStudioMod`：公共 `@Mod` 入口、配置注册和可选网络载荷。
- `CommandBlockStudio`：客户端事件、运行时配置和编辑器状态。
- `network/`：服务端可加载的注释与命令方块刷新载荷。
- `ui/`、`config/`、`insight/`：客户端编辑器、设置和命令语义服务。
- `mixin/`：替换原版界面并访问必要私有状态。

公共入口不得直接初始化仅客户端可用的渲染或界面类，以保证 dedicated server 可以加载模组。

## 编辑器数据流

```text
命令方块 / 命令方块矿车
        |
        v
AbstractCommandBlockStudioScreen
        |
        +--> MultiLineTextFieldWidget      多行显示、选择、行号和编辑历史
        +--> MultiLineCommandSuggestor     Brigadier 与结构化候选
        +--> CommandInsightService         文档、签名和诊断
        +--> StudioInsightPanel            固定快速文档与预览
        +--> 原版设置命令方块数据包         提交命令与控制状态
```

多行格式化属于视图层。提交前仍从编辑器维护的原始命令文本生成原版数据包，不改变服务器命令执行格式。

## 命令语义层

提示系统按以下顺序提供信息：

1. 命令专用节点文档。
2. 原版根命令目录和模板。
3. Component、物品数据组件等结构化补全。
4. Brigadier 节点、参数名和参数类型回退。

服务端或其他模组注册的命令即使没有内置文档，也可以使用当前连接命令树提供的候选和签名。

## 工作区

`CommandBlockWorkspace` 在客户端内存中维护当前世界会话的固定位置，使用有序集合并限制为 16 个。`ChainHandler` 负责命令链导航，`CommandBlockStudioScreen` 把当前、连锁和固定目标组合成页签。

服务端载荷可在权限与 64 格距离限制内刷新目标方块数据；没有载荷通道时回退到原版交互。

## Mixin 范围

Mixin 主要用于：

- 替换命令方块和命令方块矿车的原版编辑屏幕。
- 在客户端接收方块实体更新时刷新当前 Studio。
- 访问原版私有编辑器、字体、粒子和建议窗口状态。

命令注册、配置和网络使用 NeoForge 原生 API，不通过 Mixin 模拟加载器生命周期。

## 资源与本地化

资源命名空间是 `command_block_studio`，语言文件位于：

```text
src/main/resources/assets/command_block_studio/lang/
```

界面文本使用 Minecraft 翻译键。命令文档语言由独立配置选择，目前支持 `zh_cn` 和 `en_us`。
