# 命令智能提示

Command Block Studio 的提示系统直接读取当前连接下发的 Brigadier 命令树，不维护一套替代解析器。命令是否可用、候选值和权限过滤都以当前世界或服务器为准。

## 提示层级

1. 命令专用文档：为常用命令提供更具体的子命令和参数语义。
2. 原版与模组根命令目录：覆盖原版、专用服务器、别名、开发命令和一组已验证的常见模组命令，提供中英文简介与模板。
3. 结构化参数层：为原生 Codec 没有继续提供候选的文本 Component 与物品数据组件补充编辑器级嵌套补全。
4. 注册表预览层：根据参数类型和参数名识别模组方块、物品、粒子和玩家候选，并显示图标、纹理或头像。
5. 命令树回退：从参数名、字面量和 Brigadier 参数类型生成说明，未收录的模组命令也能使用。

候选列表、光标状态栏、悬停窗口和模板生成共用上述文档源，因此不会出现四套提示内容互相不一致的情况。

## 编辑流程

| 操作 | 结果 |
| --- | --- |
| 输入命令 | 实时解析并显示语法着色、幽灵文本和当前参数说明 |
| `Ctrl+Space` | 打开当前节点的原生或结构化补全候选 |
| `Enter` / `Tab` | 接受当前候选 |
| `Ctrl+Shift+Space` | 插入命令模板并选中第一个占位符 |
| `Tab` / `Shift+Tab` | 在模板占位符间前后跳转 |
| 悬停命令、参数或候选 | 显示语义说明和可用示例 |
| 输入无效片段 | 显示 Brigadier 的精确错误位置与消息 |

## Component 补全

文本 Component 适用于 `tellraw`、`title` 等命令。编辑器允许在 JSON 尚未闭合时继续补全字段和值，包括内容源、文本样式、记分板、选择器、NBT、点击事件与悬停事件。候选接受后仍由 Minecraft 1.21.4 原生 `ComponentSerialization` Codec 做最终校验。

```mcfunction
tellraw @a {"text":"Hello","color":"aqua","hoverEvent":{"action":"show_text","contents":{"text":"Details"}}}
title @a title {"translate":"commands.help.header","bold":true}
```

物品数据组件适用于 `give` 等使用 `ItemArgument` 的命令。组件键实时读取 `DATA_COMPONENT_TYPE` 注册表，常用组件同时提供值模板、用途说明和示例；未知或模组组件仍会显示注册表候选，并交给该组件自身 Codec 校验。

```mcfunction
give @s minecraft:diamond[minecraft:custom_name='{"text":"Blade","color":"aqua"}']
give @s minecraft:diamond_sword[minecraft:enchantments={levels:{"minecraft:sharpness":5}}]
```

`minecraft:custom_name`、`minecraft:item_name` 和 `minecraft:lore` 内的单引号 JSON 字符串会重新进入文本 Component 补全，因此可以继续补全 `text`、`translate`、样式与交互字段。输入过程中的未闭合括号、引号或对象按“尚未完成”处理；保存时仍以 Brigadier 和原生 Codec 的结果为准。

## 文档覆盖

目录包含常规命令、专用服务器命令、命令别名和游戏测试开发命令，并为整合包索引中实际存在的常见模组提供可选说明。命令树中没有出现的权限命令不会显示候选；其他模组通过 NeoForge 原生注册的命令会自动进入回退提示链路。

模组文档不会引用或加载模组类。语法、候选和权限始终以服务器下发的 Brigadier 树为准，Studio 只在相应根命令真实出现时补充说明。已验证范围和限制见[模组命令软兼容](guide/mod-compatibility.md)。

详细条目维护在 `VanillaCommandDocLibrary` 与 `ModCommandDocLibrary`，命令专用节点说明维护在 `CommandDocLibrary`，通用参数类型说明维护在 `CommandElementDescriptions`，Component 字段与物品数据组件模板维护在 `StructuredArgumentCompletion`。
