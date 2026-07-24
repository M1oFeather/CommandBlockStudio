# 文档维护与部署

## 唯一事实源

公共文档只在默认分支 `1.21.1` 维护。`1.21.4`、`26.2` 等 Minecraft 版本分支不再复制发布资料，也不会因普通 push 触发文档部署。

版本选择、总更新日志、各版本 Release Notes 和三平台发布文本统一位于 [`docs/releases/`](../releases/index.md)。

## 文件结构

```text
mkdocs.yml                         # 站点配置与导航
requirements-docs.txt              # 固定的文档构建依赖
docs/                              # Markdown 与静态资源
.github/workflows/docs.yml         # 构建和 GitHub Pages 部署
```

新增页面后必须把它加入 `mkdocs.yml` 的 `nav`。工作流使用严格模式，遗漏导航、无效内部链接或部分配置警告会让构建失败。

## 本地预览

=== "Windows PowerShell"

    ```powershell
    python -m venv .venv-docs
    .\.venv-docs\Scripts\python.exe -m pip install -r requirements-docs.txt
    .\.venv-docs\Scripts\python.exe -m mkdocs serve
    ```

=== "Linux / macOS"

    ```bash
    python3 -m venv .venv-docs
    .venv-docs/bin/python -m pip install -r requirements-docs.txt
    .venv-docs/bin/python -m mkdocs serve
    ```

打开 `http://127.0.0.1:8000/`。提交前运行：

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs build --strict
```

## GitHub Actions 行为

`Documentation` 工作流监听：

- `docs/**`
- `mkdocs.yml`
- `requirements-docs.txt`
- 工作流自身

不同事件的行为：

| 事件 | 构建 | 部署 |
| --- | --- | --- |
| Pull request | 严格构建校验 | 否 |
| 默认分支 push | 严格构建并上传 Pages artifact | 是 |
| 默认分支手动 `workflow_dispatch` | 严格构建并上传 Pages artifact | 是 |
| 其他分支 push | 不触发 | 否 |

工作流会从 `GITHUB_REPOSITORY` 动态推导项目 Pages 地址，因此兼容任意仓库名，也兼容 `<owner>.github.io` 用户站点仓库。

## 首次启用 GitHub Pages

1. 推送新增的 MkDocs 和工作流文件。
2. 打开仓库 **Settings → Pages**。
3. 将 **Build and deployment → Source** 设为 **GitHub Actions**。
4. 在 Actions 页面手动运行一次 `Documentation`，或向默认分支推送文档变更。
5. 部署完成后从工作流的 `github-pages` environment 打开站点。

工作流使用 GitHub Pages 官方的 `configure-pages`、`upload-pages-artifact` 和 `deploy-pages` 流程，不创建 `gh-pages` 分支。

## 版本发布文档

每次准备发布时：

1. 在 `docs/releases/<版本>/` 创建 `common.md` 与 `release-notes.md`。
2. 更新 `docs/releases/changelog.md` 和根目录 `CHANGELOG.md`。
3. 需要平台文本时，在该版本下建立 `platforms/`。
4. 将所有新增页面加入 `mkdocs.yml`。
5. 发布完成后补充发布日期、下载链接和平台页面链接。

## 依赖升级

文档主题固定在 `requirements-docs.txt`。升级时：

1. 修改版本号。
2. 本地运行严格构建。
3. 检查浅色、深色、桌面和窄屏导航。
4. 通过 PR 让 GitHub Actions 再验证一次。
