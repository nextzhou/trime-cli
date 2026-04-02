<!--
SPDX-FileCopyrightText: 2015 - 2026 Rime community
SPDX-License-Identifier: GPL-3.0-or-later
-->

# trime-cli

`trime-cli` 是一个独立发布的命令行工具，用于在电脑上校验、渲染并生成 `*.trime.yaml` 的报告。

它的目标不是做一个“差不多能用”的第三方检查器，而是尽量贴近同文输入法（Trime）的实际行为：

- 复用同源的主题模型与解析逻辑
- 支持通过 `librime` 展开 `__include` / `__patch`
- 内置默认的 RIME prelude 与共享 YAML 资源

## 当前状态

- 实现语言：Kotlin / JVM
- 运行方式：fat JAR
- 官方验证平台：macOS、Ubuntu 24.04
- 其他 Linux 发行版：最佳努力支持，可通过 `LIBRIME_PATH` 指定 `librime.so`
- 许可证：GPL-3.0-or-later

## 下载

- 最新 release: <https://github.com/nextzhou/trime-cli/releases/latest>
- 直接下载 fat JAR: <https://github.com/nextzhou/trime-cli/releases/latest/download/trime-cli-all.jar>

## 快速开始

直接运行：

```bash
java -jar trime-cli-all.jar --help
```

安装 `librime`：

```bash
# macOS
brew install librime

# Ubuntu 24.04
sudo apt-get update
sudo apt-get install -y librime-dev fonts-noto-cjk
```

如果暂时不方便安装 `librime`，也可以使用 `--no-rime` 跳过 `__include` / `__patch` 预处理。

自行构建：

```bash
./gradlew shadowJar
```

产物：

```text
build/libs/trime-cli-all.jar
```

示例：

```bash
java -jar build/libs/trime-cli-all.jar validate demo.trime.yaml
java -jar build/libs/trime-cli-all.jar render demo.trime.yaml -o /tmp/keyboards
java -jar build/libs/trime-cli-all.jar report demo.trime.yaml -o report.html
```

完整使用说明见 [docs/usage.md](./docs/usage.md)。

## 与 Trime 的关系

这个仓库是 `trime-cli` 的独立主仓库，但仍然保留了和 `osfans/trime` 的开发期对齐机制。

- 最终发布物不依赖 `upstream/trime`
- `upstream/trime` 仅作为开发期事实源，用于：
  - 刷新默认资源
  - 跑 vendored 资源的一致性检查
- CLI 源码本身以本仓为主，不再要求逐文件跟随上游同步

当前对齐信息见 [UPSTREAM.md](./UPSTREAM.md)。

## 维护者工作流

初始化 submodule：

```bash
git submodule update --init --recursive
```

刷新 vendored 资源：

```bash
./gradlew refreshUpstreamResources
```

检查与上游的同步情况：

```bash
./gradlew checkUpstreamSync
```
