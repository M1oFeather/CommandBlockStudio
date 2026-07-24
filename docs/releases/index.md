# 版本与发布管理

`docs/releases/` 是 Command Block Studio 的版本信息、更新日志和发布文案唯一事实源。

## 公用文档策略

- 文档只在默认分支 `1.21.1` 维护和部署。
- `1.20.4`、`1.21.4`、`26.2` 等版本分支只维护对应 Minecraft 代码、构建配置和必要 README 差异。
- Pull Request 可以验证文档，但非 `1.21.1` 分支的 push 不部署 GitHub Pages。

在线文档始终指向当前维护版本：<https://m1ofeather.github.io/CommandBlockStudio/>

## 目录规则

```text
docs/releases/
|-- index.md                         # 发布流程和目录约定
|-- versions.md                      # 玩家与开发者版本选择
|-- changelog.md                     # 中文总更新日志
|-- 1.0.0/
|   `-- release-notes.md             # 已发布版本记录
`-- 1.1.0/
    |-- common.md                    # 本版本唯一事实源
    |-- release-notes.md             # GitHub Release 可用正文
    |-- media.md                     # 图片与图注清单
    `-- platforms/
        |-- mcmod.md
        |-- modrinth.md
        `-- curseforge.md
```

## 文件职责

| 文件 | 职责 |
| --- | --- |
| `versions.md` | 当前稳定版、待发布版、Minecraft 构建和 Git 分支映射 |
| `changelog.md` | 面向用户的中文完整更新历史 |
| `<版本>/common.md` | 版本边界、支持范围、安装方式和功能事实 |
| `<版本>/release-notes.md` | 可直接用于 GitHub Release 的版本正文 |
| `<版本>/platforms/*.md` | 各平台字段、简介和上传文本 |
| `<版本>/media.md` | 封面、功能截图、图注与发布前检查 |

根目录 `CHANGELOG.md` 保留英文开发日志。每次发布时，必须同时更新根日志、中文总日志和当前版本 Release Notes。

## 发布流程

1. 从上一个正式版本的冻结提交核对 `git log` 和实际差异。
2. 更新当前版本的 `common.md`，先锁定版本事实。
3. 更新 `release-notes.md` 与全局 `changelog.md`。
4. 根据共同信息同步 MC百科、Modrinth 和 CurseForge 文案。
5. 按 `media.md` 完成实机截图。
6. 构建所有 Minecraft 分支，并检查 JAR 文件名与内嵌版本。
7. 上传完成后，将 `Unreleased` 改为发布日期并补充平台链接。

!!! warning "不要提前写发布日期"
    待发布版本统一使用“待发布”或 `Unreleased`。只有实际上传完成后才填写发布日期。
