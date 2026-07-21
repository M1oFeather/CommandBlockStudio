# 配置

## 打开设置

在 Studio 顶部点击“设置”。配置页左侧显示编辑器预览，右侧显示行为与格式化选项。界面缩放滑杆会实时作用于设置页，无需退出重进。

客户端配置文件位于：

```text
config/command_block_studio-client.toml
```

配置由 NeoForge `ModConfigSpec` 管理。通常应在游戏内修改；手动编辑前先关闭游戏。

## 保存行为

| 配置项 | 默认值 | 说明 |
| --- | ---: | --- |
| `autosave` | `true` | 输入停止后自动保存，关闭界面或切换页签时再次保存 |
| `confirm_unsaved_exit` | `true` | 仅在自动保存关闭时生效；Esc 或页签切换前确认未保存修改 |
| `track_output_default_used` | `false` | 是否覆盖命令方块原有的输出追踪默认行为 |
| `track_output_default_value` | `true` | 启用覆盖后采用的输出追踪默认值 |
| `show_output_default` | `false` | 打开 Studio 时是否默认展开输出视图 |

## 编辑与格式化

| 配置项 | 默认值 | 范围或作用 |
| --- | ---: | --- |
| `indentation_spaces` | `2` | 每层额外缩进，范围 1-16 |
| `wraparound` | `250` | 可视换行宽度，范围 10-6400 |
| `format_strings` | `true` | 可视格式化是否处理字符串内部结构 |
| `bracket_autocomplete` | `true` | 自动闭合括号和引号 |
| `avoid_double_newline` | `true` | 合并格式化产生的空可视行 |
| `newline_pre_open_bracket` | `true` | 左括号前换行 |
| `newline_post_open_bracket` | `true` | 左括号后换行 |
| `newline_pre_close_bracket` | `true` | 右括号前换行 |
| `newline_post_close_bracket` | `false` | 右括号后换行 |
| `newline_post_comma` | `true` | 逗号后换行 |

这些换行只影响编辑器渲染，不会把多行文本写入命令方块。

## 滚动

| 配置项 | 默认值 | 范围 |
| --- | ---: | ---: |
| `scroll_step_vertical` | `2` | 1-64 个可视行 |
| `scroll_step_horizontal` | `4` | 1-64 个字符 |

## 界面缩放

`ui_scale_percent` 控制 Studio 自己的缩放比例：

- `0`：根据当前 Minecraft GUI 画布自动选择。
- 设置界面滑杆：70%-125%。
- 配置值合法范围：0-125；建议使用设置界面可选范围。

自动模式不直接链接 Modern UI API，而是从当前 GUI 尺寸推导比例，因此可与改变 Minecraft 界面尺度的模组软兼容。

## 提示语言

`command_insight_language` 支持：

- `zh_cn`：中文快速文档与参数说明。
- `en_us`：English command documentation.

设置页右上角的“命令提示语言...”会打开独立子页面。该选项只影响 Studio 文档、参数说明和模板说明，不改变 Minecraft 本体语言。

## 恢复默认配置

关闭游戏后删除 `config/command_block_studio-client.toml`，NeoForge 会在下次启动时按默认值重新生成。若只排查缩放问题，可先将 `ui_scale_percent` 改为 `0`。
