# Changelog

## 1.0.0 - 2026-07-19

### NeoForge port

- Ported the command block and command block minecart editor to Minecraft 1.21.1 and NeoForge 21.1.217.
- Rebuilt lifecycle, key mapping, config screen integration, and client events with native NeoForge APIs.
- Replaced the legacy properties config with a native `ModConfigSpec` client config.
- Preserved the official NeoForge MDK build, metadata template, and run configuration structure.

### Editor

- Rebranded the project as Command Block Studio with the `command_block_studio` mod ID and `com.miofeather.commandblockstudio` package; existing `neo_better_cbui-client.toml` settings migrate automatically.
- Rebuilt the command screen as a workbench with a top context toolbar, left activity rail, persistent editor, docked quick documentation, bottom Problems/Output tools, status bar, and explicit Save/Cancel actions.
- Moved command-block mode, conditional, redstone, output tracking, target identity, chain navigation, settings, and conversion tools into stable IDE-style regions.
- Redesigned settings and language selection to preserve the Studio layout and retain the current command buffer and cursor when navigating back.
- Removed Enter-to-save-and-close behavior; Enter now only accepts an open completion candidate.
- Upgraded server edit history to ten full command-block snapshots with selection, preview, and Git-style non-destructive rollback.
- Kept hover documentation to the right of the editor caret and below the active line so it no longer covers the command being edited.
- Wrapped the parameter status panel instead of truncating long syntax hints.
- Reformatted command-save chat feedback into a localized status line and an indented command preview.
- Added multiline visual formatting, scrolling, command/output switching, chain navigation, and area selection.
- Added Brigadier-backed completion with `Ctrl+Space`, candidate selection, ghost text, and asynchronous suggestion refresh.
- Added native composite-argument hints for coordinates and rotations, including missing-part ghost text, non-error incomplete states, and `Tab` placeholder insertion.
- Added bilingual command documentation for 91 vanilla and server commands, generic mod-command fallback, cursor parameter help, and syntax diagnostics.
- Added command templates, placeholder navigation, undo/redo, paired bracket and quote editing, bracket matching, and editor-style Home/End navigation.
- Added an in-editor settings entry and a language subpage without discarding the command buffer.
- Replaced the ambiguous side-panel gear with a labeled tools button and clarified the angle/radian and color-conversion sections.
- Added line numbers, exact error navigation, search/replace, selection-safe scrollbars, and a full HSV/RGB/hex color picker.
- Grouped contiguous command blocks into ordered chain tabs, added per-tab pinning and horizontal tab scrolling, and preserved the current unpinned tab while switching workspaces.
- Added block, item, enchantment, player-avatar, and particle previews backed by the active registries and player list.
- Added rich text Component and item data-component completion with nested documentation and value templates.
- Added responsive Studio scaling, opaque editor documentation surfaces, and compact layouts verified at 856x512.

### Compatibility

- The editor remains client-side; no Fabric API, Architectury, Sinytra Connector, or other compatibility layer is required.
- Added optional native NeoForge server payloads for shared comments and ten-version command-block history with rollback.
- Added Brigadier-gated documentation for common commands from WorldEdit, Carpet, Create, CMDCam, Corpse, Dynamic Trees, Exposure, Serene Seasons, Supplementaries, WaterFrames, SecurityCraft, and Yes Steve Model.
- Added JUnit coverage for editor search/replace behavior and curated mod-command recognition.
