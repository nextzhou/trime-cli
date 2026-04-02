// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar

object SchemaConstraintValidator {
    fun validate(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        messages += checkConfigVersion(node)
        messages += checkStyleEnums(node)
        messages += checkNumericRanges(node)
        messages += checkKeyNamePatterns(node)
        messages += checkLiquidKeyboardConstraints(node)
        messages += checkUnknownTopLevelKeys(node)
        return messages
    }

    private fun checkConfigVersion(node: YamlMap): List<ValidationMessage> {
        val version = node.get<YamlScalar>("config_version")?.content ?: return emptyList()
        val pattern = Regex("""\d+(\.\d+)*""")
        if (!pattern.matches(version)) {
            return listOf(
                ValidationMessage(
                    level = Level.ERROR,
                    code = "S001",
                    path = "config_version",
                    message =
                        "`config_version` 的值 `$version` 不符合要求的格式 " +
                            "`\\d+(\\.\\d+)*`（例如 `3.0` 或 `2.1.0`）。",
                    suggestion = "请使用类似 `3.0` 或 `2.1.0` 的版本号。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkStyleEnums(node: YamlMap): List<ValidationMessage> {
        val style = node.get<YamlMap>("style") ?: return emptyList()
        val messages = mutableListOf<ValidationMessage>()

        // auto_caps: true | false | "ascii"
        style.get<YamlScalar>("auto_caps")?.content?.let { value ->
            val valid = setOf("true", "false", "ascii")
            if (value.lowercase() !in valid) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "S002",
                        path = "style.auto_caps",
                        message = "`style.auto_caps` 的值 `$value` 无效，必须是以下之一：${valid.joinToString(", ")}。",
                        suggestion = "请使用 `true`、`false` 或 `ascii`。",
                    )
            }
        }

        // comment_position: "right" | "top" | "overlay"
        style.get<YamlScalar>("comment_position")?.content?.let { value ->
            val valid = setOf("right", "top", "overlay")
            if (value.lowercase() !in valid) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "S003",
                        path = "style.comment_position",
                        message = "`style.comment_position` 的值 `$value` 无效，必须是以下之一：${valid.joinToString(", ")}。",
                        suggestion = "请使用 `right`、`top` 或 `overlay`。",
                    )
            }
        }

        // enter_label_mode: 0-3
        style.get<YamlScalar>("enter_label_mode")?.content?.toIntOrNull()?.let { value ->
            if (value !in 0..3) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "S004",
                        path = "style.enter_label_mode",
                        message = "`style.enter_label_mode` 的值 $value 超出范围，必须在 0 到 3 之间。",
                        suggestion = "请使用 0 到 3 之间的值。",
                    )
            }
        }

        return messages
    }

    private fun checkNumericRanges(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()

        // preedit.alpha: 0.0-1.0
        node.get<YamlMap>("preedit")?.get<YamlScalar>("alpha")?.content?.toFloatOrNull()?.let { value ->
            if (value < 0f || value > 1f) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "S005",
                        path = "preedit.alpha",
                        message = "`preedit.alpha` 的值 $value 超出范围，必须在 0.0 到 1.0 之间。",
                        suggestion = "请使用 0.0（完全透明）到 1.0（完全不透明）之间的值。",
                    )
            }
        }

        // window.alpha: 0.0-1.0
        node.get<YamlMap>("window")?.get<YamlScalar>("alpha")?.content?.toFloatOrNull()?.let { value ->
            if (value < 0f || value > 1f) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "S006",
                        path = "window.alpha",
                        message = "`window.alpha` 的值 $value 超出范围，必须在 0.0 到 1.0 之间。",
                        suggestion = "请使用 0.0（完全透明）到 1.0（完全不透明）之间的值。",
                    )
            }
        }

        // preedit.horizontal_padding: 0-64
        node.get<YamlMap>("preedit")?.get<YamlScalar>("horizontal_padding")?.content?.toIntOrNull()?.let { value ->
            if (value < 0 || value > 64) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "S007",
                        path = "preedit.horizontal_padding",
                        message = "`preedit.horizontal_padding` 的值 $value 超出范围，必须在 0 到 64 之间。",
                        suggestion = "请使用 0 到 64 之间的值。",
                    )
            }
        }

        return messages
    }

    private fun checkKeyNamePatterns(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        val wordPattern = Regex("""\w+""")

        fun checkMapKeys(
            map: YamlMap?,
            sectionName: String,
            code: String,
        ) {
            map ?: return
            for ((key, _) in map.entries.entries) {
                val name = key.content
                if (!wordPattern.matches(name)) {
                    messages +=
                        ValidationMessage(
                            level = Level.WARNING,
                            code = code,
                            path = "$sectionName.$name",
                            message =
                                "`$sectionName` 中的键名 `$name` 包含非法字符。" +
                                    " 键名必须匹配 `\\w+`（字母、数字或下划线）。",
                            suggestion = "请将 `$name` 重命名为只包含字母、数字和下划线的名称。",
                        )
                }
            }
        }

        checkMapKeys(node.get<YamlMap>("preset_keyboards"), "preset_keyboards", "S008")
        checkMapKeys(node.get<YamlMap>("preset_keys"), "preset_keys", "S008")
        checkMapKeys(node.get<YamlMap>("preset_color_schemes"), "preset_color_schemes", "S008")

        return messages
    }

    private fun checkLiquidKeyboardConstraints(node: YamlMap): List<ValidationMessage> {
        val liquidKb = node.get<YamlMap>("liquid_keyboard") ?: return emptyList()
        val fixedKeyBar = liquidKb.get<YamlMap>("fixed_key_bar") ?: return emptyList()
        val position = fixedKeyBar.get<YamlScalar>("position")?.content ?: return emptyList()

        val validPositions = setOf("top", "bottom", "left", "right")
        if (position.lowercase() !in validPositions) {
            return listOf(
                ValidationMessage(
                    level = Level.WARNING,
                    code = "S009",
                    path = "liquid_keyboard.fixed_key_bar.position",
                    message =
                        "`liquid_keyboard.fixed_key_bar.position` 的值 `$position` 无效。" +
                            " 必须是以下之一：${validPositions.sorted().joinToString(", ")}。",
                    suggestion = "请使用 `bottom`、`left`、`right` 或 `top`。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkUnknownTopLevelKeys(node: YamlMap): List<ValidationMessage> {
        val knownKeys =
            setOf(
                "config_version", "name", "style", "preset_keys", "preset_keyboards",
                "preset_color_schemes", "fallback_colors", "preedit", "window",
                "liquid_keyboard", "tool_bar", "key_sound", "key_vibrate",
                "author", "android_keys",
            )
        val messages = mutableListOf<ValidationMessage>()
        for ((key, _) in node.entries.entries) {
            val keyName = key.content
            if (keyName.startsWith("__")) continue
            if (keyName !in knownKeys) {
                messages +=
                    ValidationMessage(
                        level = Level.INFO,
                        code = "S010",
                        path = keyName,
                        message = "未知的顶层键 `$keyName`。它可能是拼写错误，也可能是当前 CLI 不支持的字段。",
                        suggestion = "当前已知的顶层键有：${knownKeys.sorted().joinToString(", ")}。",
                    )
            }
        }
        return messages
    }
}
