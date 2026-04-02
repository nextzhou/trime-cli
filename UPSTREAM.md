<!--
SPDX-FileCopyrightText: 2015 - 2026 Rime community
SPDX-License-Identifier: GPL-3.0-or-later
-->

# Upstream Alignment

`trime-cli` 以 `osfans/trime` 作为开发期事实源，但不会把该仓库作为运行时依赖。

当前独立仓库的 CLI 代码已经领先 `osfans/trime` 上游分支，因此第一阶段只对 vendored 资源做自动同步与校验；CLI 源码逻辑以本仓为主维护。

## Current Pin

- Upstream repository: `https://github.com/osfans/trime.git`
- Tracking policy: release/tag only
- Current aligned tag: `v3.3.9`

## What Is Synced

- `vendor/rime/prelude`
- `vendor/rime/shared/*.yaml`
- `vendor/rime/essay.txt`

## Maintenance Commands

```bash
./gradlew refreshUpstreamResources
./gradlew checkUpstreamSync
```
