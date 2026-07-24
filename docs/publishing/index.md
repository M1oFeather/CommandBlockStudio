# 发布资料维护

这里归档 Command Block Studio 在 MC百科、Modrinth 和 CurseForge 使用的发布文本与图片清单。

## 目录规则

每个待发布版本使用独立目录：

```text
docs/publishing/
|-- index.md
`-- <版本>/
    |-- common.md
    |-- mcmod.md
    |-- modrinth.md
    |-- curseforge.md
    `-- media.md
```

- `common.md`：版本边界、兼容范围、安装方式和功能事实的唯一来源。
- `mcmod.md`：MC百科正文、更新日志和编辑字段。
- `modrinth.md`：Modrinth 项目简介与版本说明。
- `curseforge.md`：CurseForge 项目简介与版本说明。
- `media.md`：封面、功能截图和图注的统一编号。

## 发布流程

1. 从上一个正式版本的 Git 提交开始核对差异。
2. 更新 `common.md`，先确定版本事实和支持范围。
3. 同步三个平台文本，不在平台页面单独创造功能描述。
4. 按 `media.md` 拍摄实机截图，并核对界面语言、版本和尺寸。
5. 完成三个 Minecraft 分支的构建后，再把状态从“待发布”改为“已发布”。

!!! warning "不要提前写发布日期"
    待发布版本统一使用“待发布”或 `Unreleased`。实际上传完成后，再填写三个平台的发布日期与页面链接。
