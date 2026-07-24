# 构建与运行

本页参数对应默认分支 `1.21.1`。构建 Minecraft `1.21.4` 或 `26.2` 时，请先切换到同名分支；各分支会使用自己的 Minecraft、NeoForge 与映射版本。

## 开发环境

| 工具 | 版本 |
| --- | --- |
| JDK | 21 |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.217 |
| ModDevGradle | 2.0.141 |
| Parchment | 2024.11.17 for 1.21.1 |

仓库保留 NeoForge 官方 MDK 的 Gradle Wrapper、资源模板展开和运行配置结构。

## 构建模组

=== "Windows PowerShell"

    ```powershell
    .\gradlew.bat --no-configuration-cache --console plain clean build
    ```

=== "Linux / macOS"

    ```bash
    ./gradlew --no-configuration-cache --console plain clean build
    ```

构建产物位于：

```text
build/libs/command_block_studio-1.1.0-<Minecraft版本>-NeoForge.jar
```

`build` 结束后会自动执行 `prepareClientRun`，确保 IDE 运行参数文件存在。

## 启动开发客户端

=== "Windows PowerShell"

    ```powershell
    .\gradlew.bat --no-configuration-cache --console plain runClient
    ```

=== "Linux / macOS"

    ```bash
    ./gradlew --no-configuration-cache --console plain runClient
    ```

运行目录是 `run/`。客户端配置位于 `run/config/command_block_studio-client.toml`，日志位于 `run/logs/latest.log`。

## 启动开发服务端

```powershell
.\gradlew.bat --no-configuration-cache --console plain runServer
```

服务端开发运行用于验证公共入口、载荷注册、共享注释持久化和 dedicated server 类加载边界。客户端构建成功不能替代服务端启动验证。

## 运行参数缺失

单独刷新 ModDevGradle 客户端参数：

```powershell
.\gradlew.bat --no-configuration-cache --console plain prepareClientRun
```

参见[故障排查](../reference/troubleshooting.md)。

## 本地构建文档

```powershell
python -m venv .venv-docs
.\.venv-docs\Scripts\python.exe -m pip install -r requirements-docs.txt
.\.venv-docs\Scripts\python.exe -m mkdocs build --strict
```

实时预览：

```powershell
.\.venv-docs\Scripts\python.exe -m mkdocs serve
```

浏览器访问 `http://127.0.0.1:8000/`。
