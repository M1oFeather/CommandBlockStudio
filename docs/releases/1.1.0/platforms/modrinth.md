# Modrinth 发布文本

## 项目摘要

An IDE-style, NeoForge-native command block editor with Brigadier completion, live documentation, chat assistance and in-world inspection.

## 项目简介

**Command Block Studio** turns Minecraft command blocks into an IDE-like command development workspace.

It provides a multiline editor, syntax highlighting, line numbers, search and replace, Brigadier-backed completion, parameter documentation, Component and data-component assistance, visual registry candidates, precise diagnostics, workspace tabs, shared comments and versioned command-block history.

Version 1.1.0 expands Studio beyond the command block screen:

- A chat command assistant opens when the chat input starts with `/`.
- A configurable work-mode key, defaulting to `Alt+Tab`, shows a camera-facing command panel in front of the targeted command block.
- Command block items with block-entity data show a command excerpt, plus last-edit metadata when available.
- The editor can save and run-test a command block once without temporary redstone.
- Long-command spacing, wrapping, mouse hit testing and caret alignment have been corrected.

The client and server installations are independent. Client-only installations keep the complete editor and local assistance. A server installation adds shared comments, ten-entry edit history, rollback and other negotiated features. Different client/server mod versions may connect; unavailable features are disabled individually.

Command Block Studio is implemented with native NeoForge APIs and does not require Fabric API, Architectury, Sinytra Connector or another compatibility layer.

## 1.1.0 版本标题

Command Block Studio 1.1.0 - Chat Assistant and Work Mode

## 1.1.0 版本说明

### Added

- Command excerpts and edit metadata in command block item tooltips.
- Configurable work mode, defaulting to `Alt+Tab`, with an in-world command block projection.
- A one-click run-test action in the editor.
- A quarter-screen command assistant for `/` input in chat.
- Block, item, animated particle and player previews in chat completion.

### Fixed

- Preserved normal space width in very long commands.
- Corrected wrapped-line mouse hit testing and caret placement.
- Prevented completion and documentation overlays from covering the parameter status bar.

### Compatibility

- Client-only and server-only installation remain supported.
- Client and server versions may differ.
- Shared features are negotiated per network payload and disabled when unavailable.

## 上传字段

| 字段 | 内容 |
| --- | --- |
| Version number | `1.1.0` |
| Version title | `Command Block Studio 1.1.0 - Chat Assistant and Work Mode` |
| Release channel | Release |
| Loaders | NeoForge |
| Client side | Optional |
| Server side | Optional |
| License | MIT |

每个 Minecraft 构建单独上传对应 JAR，不要把不同游戏版本合并为同一个文件。
