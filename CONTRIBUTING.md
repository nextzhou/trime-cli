<!--
SPDX-FileCopyrightText: 2015 - 2026 Rime community
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Contributing to trime-cli

感谢你为 `trime-cli` 贡献改动。

## 开发环境

- JDK 17
- Git
- macOS 或 Ubuntu 24.04
- Windows 用户请优先使用 WSL2 + Ubuntu 24.04
- `librime`
  - macOS: `brew install librime`
  - Ubuntu 24.04: `sudo apt-get update && sudo apt-get install -y librime-dev fonts-noto-cjk`

如果你只修改纯静态分析或文档，也可以先使用 `--no-rime` 验证部分行为；但涉及 `report`、`render`、部署链路或 `__include` / `__patch` 行为时，请优先在安装了 `librime` 的环境下验证。

## 获取源码

```bash
git clone https://github.com/nextzhou/trime-cli.git
cd trime-cli
```

如果你要刷新 vendored 资源或跑上游一致性检查，再初始化 submodule：

```bash
git submodule update --init --recursive
```

普通功能开发、测试和打包不依赖 submodule。

## 本地验证

提交前至少运行：

```bash
./gradlew test
./gradlew shadowJar
```

如果改动影响 CLI 对外行为，再补跑一遍 smoke：

```bash
java -jar build/libs/trime-cli-all.jar validate vendor/rime/shared/trime.yaml
java -jar build/libs/trime-cli-all.jar render vendor/rime/shared/tongwenfeng.trime.yaml -o /tmp/trime-cli-smoke
java -jar build/libs/trime-cli-all.jar report vendor/rime/shared/tongwenfeng.trime.yaml -o /tmp/trime-cli-smoke/report.html
```

## 提交与 PR

- PR 目标分支：`main`
- 提交信息使用 Angular 风格：

```text
<type>(<scope>): <summary>
```

例如：

```text
fix(rime): support linux library lookup
docs(readme): add report preview screenshot
```

- 如果改动影响命令行为、输出格式或支持平台，请同步更新：
  - `README.md`
  - `docs/usage.md`
  - 必要时更新 release note
- 如果改动影响和上游 Trime 的对齐关系，请同步更新：
  - `UPSTREAM.md`
  - `upstream/sync-manifest.txt`

## 与上游 Trime 的关系

`trime-cli` 是独立仓库，但仍然保留开发期对齐机制。涉及默认资源、主题模型或上游一致性检查的改动时，请优先确认这些改动是：

- 这个仓库自己的行为演进
- 还是需要和 `upstream/trime` 对齐的同步项

不要在没有说明原因的情况下同时修改 vendored 资源和核心行为逻辑。
