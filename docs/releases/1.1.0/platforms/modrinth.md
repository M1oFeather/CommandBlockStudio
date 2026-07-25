# Modrinth 发布文本

## 项目摘要

An IDE-style, NeoForge-native command block editor with Brigadier completion, live documentation, chat assistance and in-world inspection.

## 项目简介

**Command Block Studio** turns Minecraft command blocks into an IDE-like command development workspace.

It provides a multiline editor, syntax highlighting, line numbers, search and replace, Brigadier-backed completion, parameter documentation, Component and data-component assistance, visual registry candidates, precise diagnostics, workspace tabs, shared comments and versioned command-block history.

Version 1.1.0 expands Studio beyond the command block screen:

- A chat command assistant opens when the chat input starts with `/`.
- A configurable work-mode key, defaulting to `Alt+Tab`, shows a camera-facing command panel with wrapped command text and shared annotations; a marked vanilla spyglass also acts as a handheld scanner.
- Command block items with block-entity data show a wrapped command excerpt, a second-line shared annotation, and last-edit metadata when available.
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
- Shared annotations in the second tooltip line and directly below the in-world projection title.
- A handheld command block scanner based on the vanilla spyglass item.

### Fixed

- Preserved normal space width in very long commands.
- Corrected wrapped-line mouse hit testing and caret placement.
- Prevented completion and documentation overlays from covering the parameter status bar.
- Removed the vanilla completion layer below the chat editor and redundant hover documentation beside the right-hand panel.
- Fixed crashes after clicking transient Brigadier completion results and restored immediate keyboard focus to the multiline chat editor.
- Made quoted-content replacement and completion ghost text selection-safe.
- Fixed enlarged or cropped Studio icons on Minecraft 1.21.4 and 26.2.

### Compatibility

- Client-only and server-only installation remain supported.
- Client and server versions may differ.
- Shared features are negotiated per network payload and disabled when unavailable.

## 上传字段

| 字段 | 内容 |
| --- | --- |
| Version number | `1.1.0` |
| Version title | `Command Block Studio 1.1.0 - Chat Assistant and Work Mode` |
| Release date | 2026-07-25 |
| Release channel | Release |
| Loaders | NeoForge |
| Client side | Optional |
| Server side | Optional |
| License | MIT |

每个 Minecraft 构建单独上传对应 JAR，不要把不同游戏版本合并为同一个文件。

## 文件与依赖

| Minecraft | NeoForge | Java | 文件名 |
| --- | --- | --- | --- |
| `1.20.4` | `20.4.167` | 17 | `command_block_studio-1.1.0-1.20.4-NeoForge.jar` |
| `1.21.1` | `21.1.217` | 21 | `command_block_studio-1.1.0-1.21.1-NeoForge.jar` |
| `1.21.4` | `21.4.121` | 21 | `command_block_studio-1.1.0-1.21.4-NeoForge.jar` |
| `26.2` | `26.2.0.10-beta` | 25 | `command_block_studio-1.1.0-26.2-NeoForge.jar` |
