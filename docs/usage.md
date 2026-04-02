<!--
SPDX-FileCopyrightText: 2015 - 2024 Rime community

SPDX-License-Identifier: GPL-3.0-or-later
-->

# trime-cli 使用说明

`trime-cli` 是一个独立发布的命令行工具，用于在电脑上直接检验和预览 `*.trime.yaml` 配置文件，无需启动 Android 设备或模拟器。

## 目录

- [功能概述](#功能概述)
- [前提条件](#前提条件)
- [构建](#构建)
- [快速开始](#快速开始)
- [命令详解](#命令详解)
  - [validate — 配置验证](#validate--配置验证)
  - [render — 键盘渲染](#render--键盘渲染)
  - [report — HTML 报告](#report--html-报告)
- [选项参考](#选项参考)
- [验证规则说明](#验证规则说明)
- [退出码](#退出码)
- [常见问题](#常见问题)

---

## 功能概述

| 功能 | 说明 |
|------|------|
| **validate** | 检查配置文件的语法、语义、引用完整性，输出结构化报告（支持 JSON 格式） |
| **render** | 将每个键盘布局渲染为 PNG 图片，方便直观确认布局效果 |
| **report** | 生成一个自包含的 HTML 报告，同时包含验证结果和所有键盘的预览图 |

工具复用与 Android 端完全相同的 YAML 解析逻辑，支持通过系统安装的 librime 预处理 `__include`/`__patch` 指令。

---

## 前提条件

- **JRE 17+**（运行 fat JAR）
- **macOS**（当前版本仅支持 macOS）
- **librime**（可选，用于 `__include`/`__patch` 预处理）

  ```bash
  # Apple Silicon 或 Intel Mac
  brew install librime
  ```

  如果不需要 `__include`/`__patch` 解析，可使用 `--no-rime` 跳过此依赖。

---

## 构建

在仓库根目录执行：

```bash
./gradlew shadowJar
```

构建产物位于：

```
build/libs/trime-cli-all.jar
```

推荐将其复制到常用目录并创建别名：

```bash
cp build/libs/trime-cli-all.jar ~/bin/trime-cli.jar
alias trime-cli='java -jar ~/bin/trime-cli.jar'
```

之后文档中的示例均以 `trime-cli` 代指实际调用方式（`java -jar build/libs/trime-cli-all.jar`）。

---

## 快速开始

```bash
# 验证配置文件（使用 --no-rime 跳过 librime 依赖）
trime-cli validate --no-rime my.trime.yaml

# 渲染所有键盘布局为 PNG
trime-cli render --no-rime --width 1080 --density 2.75 my.trime.yaml -o /tmp/keyboards/

# 生成包含验证结果和键盘预览的 HTML 报告
trime-cli report --no-rime my.trime.yaml -o report.html
```

---

## 命令详解

### validate — 配置验证

检查 `*.trime.yaml` 文件的合法性，输出问题报告。

**语法**

```
trime-cli validate [选项] <config-file>
```

**示例**

```bash
# 基本验证，文本格式输出
trime-cli validate my.trime.yaml

# JSON 格式输出（适合脚本解析）
trime-cli validate --format json my.trime.yaml

# 跳过 librime（不解析 __include/__patch）
trime-cli validate --no-rime my.trime.yaml

# 同时检查字体文件是否存在
trime-cli validate --font-dir ~/.local/share/fcitx5/rime/fonts my.trime.yaml
```

**文本格式输出示例**

```
Validating: my.trime.yaml

ERRORS (1):
  [E001] style: Required field 'style' is missing
         Suggestion: Add a 'style:' section to your trime.yaml.

WARNINGS (2):
  [W003] preset_color_schemes.dark.text_color: '#xyz' is not a valid color
         Suggestion: Use a valid hex color format like '#FF0000' for red.
  [W005] style.hanb_font: Font 'CustomFont.ttf' not found in font directory.

Result: INVALID (1 errors, 2 warnings)
```

**JSON 格式输出示例**

```json
{
  "file": "my.trime.yaml",
  "valid": false,
  "errors": 1,
  "warnings": 2,
  "info": 0,
  "messages": [
    {
      "level": "ERROR",
      "code": "E001",
      "path": "style",
      "message": "Required field 'style' is missing",
      "suggestion": "Add a 'style:' section to your trime.yaml."
    }
  ]
}
```

---

### render — 键盘渲染

将配置文件中所有键盘布局渲染为 PNG 图片，输出到指定目录。

**语法**

```
trime-cli render [选项] <config-file> -o <output-dir>
```

**示例**

```bash
# 以 1080p 密度渲染所有键盘
trime-cli render --width 1080 --density 2.75 my.trime.yaml -o /tmp/keyboards/

# 横屏渲染
trime-cli render --width 2400 --density 2.75 --landscape my.trime.yaml -o /tmp/keyboards/

# 模拟低密度屏幕（hdpi）
trime-cli render --width 720 --density 2.0 my.trime.yaml -o /tmp/keyboards/
```

**输出说明**

- 每个键盘对应一个 PNG 文件，命名规则为 `keyboard_<name>.png`
- 渲染分辨率由 `--width` 和 `--density` 共同决定，宽度为像素值，高度由键盘行高自动计算
- 工具会在 stdout 打印每个生成文件的绝对路径

```
/tmp/keyboards/keyboard_default.png
/tmp/keyboards/keyboard_qwerty.png
Rendered 2 keyboard(s) to /tmp/keyboards/
```

**关于渲染精度**

当前版本使用 Java AWT Graphics2D 进行渲染（Skia/Skiko 在无头环境下不可用）。渲染效果接近 Android 实际显示，可用于确认布局、颜色和文字，但不保证像素级完全一致。

---

### report — HTML 报告

生成一个自包含的 HTML 文件，同时包含：
- 验证结果摘要（错误/警告/信息）
- 所有键盘布局的预览图（base64 内嵌，无需外部文件）

**语法**

```
trime-cli report [选项] <config-file> -o <output.html>
```

**示例**

```bash
# 生成报告到当前目录
trime-cli report my.trime.yaml -o report.html

# 指定渲染参数
trime-cli report --width 1080 --density 2.75 my.trime.yaml -o report.html
```

用浏览器直接打开生成的 `report.html` 即可查看完整报告，无需网络连接。

---

## 选项参考

| 选项 | 默认值 | 说明 |
|------|--------|------|
| `--width <px>` | `1080` | 屏幕宽度（像素），影响键盘布局计算和渲染宽度 |
| `--height <px>` | `2400` | 屏幕高度（像素），当前版本仅用于参数传递，键盘高度由行高自动计算 |
| `--density <float>` | `2.75` | 屏幕密度（dp → px 换算系数），等同于 Android 的 `displayMetrics.density` |
| `--landscape` | 关闭 | 以横屏模式计算键盘布局 |
| `--format text\|json` | `text` | `validate` 命令的输出格式 |
| `--color-scheme <name>` | `default` | 渲染时使用的颜色方案名称（须在配置中定义） |
| `--font-dir <path>` | 无 | 字体文件目录，用于字体存在性检查和渲染时加载字体 |
| `--rime-data <path>` | 无 | 自定义 RIME 数据目录（覆盖 JAR 内置的 prelude 数据） |
| `--no-rime` | 关闭 | 跳过 librime 部署，直接解析原始 YAML（不展开 `__include`/`__patch`） |
| `-o, --output <path>` | 取决于命令 | 输出路径（`render` 时为目录，`report` 时为 HTML 文件） |
| `--version` | — | 打印版本号并退出 |
| `--help, -h` | — | 打印帮助信息并退出 |

### `--no-rime` 与 librime 模式的区别

| 模式 | `__include` / `__patch` | 适用场景 |
|------|------------------------|----------|
| 默认（librime 模式） | 完全展开 | 与 Android 端行为一致，推荐用于正式检查 |
| `--no-rime` | 保留原始指令 | 无法安装 librime 时，或仅需检查文件结构时 |

使用 `--no-rime` 时，验证和渲染基于**未展开**的原始 YAML，某些跨文件引用错误可能无法被检测到。

---

## 验证规则说明

工具内置以下四类验证规则：

### 崩溃级错误（ERROR）

这些问题会导致 Android 端 trime 崩溃，必须修复：

| 代码 | 检查项 | 说明 |
|------|--------|------|
| `E001` | `style` 字段缺失 | `style:` 是必填顶层字段，缺失时 trime 抛出 NullPointerException |
| `E002` | `preset_color_schemes` 为空 | 至少需要一个颜色方案，否则 `.first()` 调用崩溃 |
| `E003` | `import_preset` 存在循环引用 | 键盘 A → B → A 的链会导致 StackOverflowError |
| `E004` | `fallback_colors` 存在循环引用 | 颜色回退链存在环路会导致死循环 |

### 静默失败（WARNING）

这些问题不会崩溃，但会导致功能异常或显示错误：

| 代码 | 检查项 | 说明 |
|------|--------|------|
| `W001` | `preset_keyboards` 为空 | 无键盘可显示 |
| `W002` | 无 `default` 键盘 | 首个键盘将被用作回退 |
| `W003` | 无效颜色值 | 例如 `#xyz`，颜色将回退为透明 |
| `W004` | `import_preset` 目标不存在 | 引用了未定义的键盘名 |
| `W005` | 字体文件缺失 | 指定字体不存在时使用系统默认字体 |
| `W006` | 颜色键不可达 | 在键定义中引用了未在任何颜色方案中定义的颜色键 |
| `W007` | `liquid_keyboard.fixed_key_bar.position` 值无效 | 必须为 `top`、`bottom`、`left`、`right` 之一 |

### Schema 约束（WARNING / ERROR）

基于 `doc/trime-schema.json` 的结构约束：

| 代码 | 检查项 | 说明 |
|------|--------|------|
| `S001` | `config_version` 格式 | 必须匹配 `\d+(\.\d+)*`，如 `3.0` |
| `S002` | `style.auto_caps` 枚举 | 仅允许 `true`、`false`、`ascii` |
| `S003` | `style.comment_position` 枚举 | 仅允许 `right`、`top`、`overlay` |
| `S004` | `style.enter_label_mode` 范围 | 必须为 0–3 |
| `S005` | `preedit.alpha` 范围 | 必须为 0.0–1.0 |
| `S006` | `window.alpha` 范围 | 必须为 0.0–1.0 |
| `S007` | `preedit.horizontal_padding` 范围 | 必须为 0–64 |
| `S008` | 键名格式 | `preset_keyboards`、`preset_keys`、`preset_color_schemes` 的键名须匹配 `\w+` |
| `S009` | `liquid_keyboard.fixed_key_bar.position` 枚举 | 仅允许 `top`、`bottom`、`left`、`right` |
| `S010` | 未知顶层键 | 提示可能的拼写错误（INFO 级别） |

### 布局合理性（WARNING / ERROR）

| 代码 | 检查项 | 说明 |
|------|--------|------|
| `L001` | 键盘高度为零 | `key_height` 和各键盘 `height` 均未设置，键盘将不可见（ERROR） |
| `L002` | 键盘无按键定义 | 键盘既无 `keys` 也无 `import_preset` |
| `L003` | 行权重溢出 | 某行各键 `width` 之和超过 100，按键将超出屏幕 |
| `L004` | 按键无动作 | 非 spacer 键未定义 `action`、`click`、`key` 或 `text` |
| `L005` | 键高不合理 | `key_height` 小于 20dp 或大于 80dp |
| `L006` | 键宽为零或负数 | `key_width` 设置异常 |

---

## 退出码

| 退出码 | 含义 |
|--------|------|
| `0` | 成功（`validate` 时表示配置有效） |
| `1` | 配置存在错误（`validate` 返回 ERROR 级别问题） |
| `2` | 参数错误（配置文件未指定或文件不存在） |
| `3` | librime 未安装（且未使用 `--no-rime`） |

---

## 常见问题

**Q: 如何处理 `__include` 和 `__patch` 指令？**

需要安装 librime（`brew install librime`）。工具会在临时目录中暂存配置文件并调用 librime 引擎进行编译，展开所有指令后再做验证和渲染。

如果只是快速检查文件结构，可使用 `--no-rime` 跳过 librime 依赖，但 `__include`/`__patch` 引用的内容将不会被展开。

---

**Q: `Error: librime not found` 怎么办？**

1. 安装 librime：`brew install librime`
2. 如果安装在非标准路径，设置环境变量：
   ```bash
   export LIBRIME_PATH=/path/to/librime.dylib
   trime-cli validate my.trime.yaml
   ```
3. 如不需要 `__include`/`__patch` 解析，添加 `--no-rime` 参数。

---

**Q: 渲染结果与手机上看到的不完全一致？**

这是预期行为。当前版本使用 Java AWT 渲染，目标是"足以调试布局"而非像素级一致。以下内容不会被渲染：
- 按键按下/高亮状态
- Liquid Keyboard（符号键盘）
- 候选词栏和预编辑区
- 图标类按键（显示为空白）

---

**Q: 如何指定渲染时使用的颜色方案？**

使用 `--color-scheme` 选项：

```bash
trime-cli render --color-scheme dark my.trime.yaml -o /tmp/keyboards/
```

指定的颜色方案名须在配置的 `preset_color_schemes` 中存在，否则将回退到默认方案。

---

**Q: validate 输出了大量 INFO 信息，是问题吗？**

INFO 级别（代码 `S010`、`W006` 等）只是提示信息，不影响配置合法性。`validate` 命令在无 ERROR 时返回退出码 0。INFO 信息可用于发现潜在的拼写错误或未引用的资源。

---

**Q: 如何在 CI 中集成配置检查？**

```bash
# 检查配置是否有效，有错误时 CI 失败
java -jar trime-cli-all.jar validate --no-rime my.trime.yaml
echo "Exit code: $?"  # 0 = valid, 1 = has errors

# JSON 格式输出，供进一步解析
java -jar trime-cli-all.jar validate --no-rime --format json my.trime.yaml \
  | python3 -c "import json,sys; r=json.load(sys.stdin); print(f'Errors: {r[\"errors\"]}')"
```
