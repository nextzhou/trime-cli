// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar

object CrashPreventionValidator {
    fun validate(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        messages += checkMissingStyleField(node)
        messages += checkEmptyColorSchemes(node)
        messages += checkCircularImportPreset(node)
        messages += checkCircularFallbackColors(node)
        return messages
    }

    private fun checkMissingStyleField(node: YamlMap): List<ValidationMessage> {
        if (node.get<YamlMap>("style") == null) {
            return listOf(
                ValidationMessage(
                    level = Level.ERROR,
                    code = "E001",
                    path = "style",
                    message = "缺少必需字段 `style`。没有它时键盘会直接崩溃。",
                    suggestion = "请添加 `style:` 段，且至少定义 `key_height`。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkEmptyColorSchemes(node: YamlMap): List<ValidationMessage> {
        val schemes = node.get<YamlMap>("preset_color_schemes")
        if (schemes == null || schemes.entries.entries.isEmpty()) {
            return listOf(
                ValidationMessage(
                    level = Level.ERROR,
                    code = "E002",
                    path = "preset_color_schemes",
                    message = "未定义配色方案。至少需要一个配色方案。",
                    suggestion = "请添加 `preset_color_schemes:` 段，并至少定义一个方案。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkCircularImportPreset(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val keyboardMap =
            keyboards.entries.entries.associate { (k, v) ->
                k.content to ((v as? YamlMap)?.get<YamlScalar>("import_preset")?.content)
            }
        val messages = mutableListOf<ValidationMessage>()
        for (startKey in keyboardMap.keys) {
            val cycle = detectCycle(startKey, keyboardMap)
            if (cycle != null) {
                messages +=
                    ValidationMessage(
                        level = Level.ERROR,
                        code = "E003",
                        path = "preset_keyboards.$startKey.import_preset",
                        message = "检测到 `import_preset` 循环引用：${cycle.joinToString(" -> ")}。这会导致 `StackOverflowError`。",
                        suggestion = "请移除其中一处 `import_preset` 引用以打破循环。",
                    )
                break
            }
        }
        return messages
    }

    private fun checkCircularFallbackColors(node: YamlMap): List<ValidationMessage> {
        val fallbackColors = node.get<YamlMap>("fallback_colors") ?: return emptyList()
        val fallbackMap =
            fallbackColors.entries.entries.associate { (k, v) ->
                k.content to (v as? YamlScalar)?.content
            }
        val messages = mutableListOf<ValidationMessage>()
        val reportedCycles = mutableSetOf<Set<String>>()
        for (startKey in fallbackMap.keys) {
            val cycle = detectCycle(startKey, fallbackMap)
            if (cycle != null) {
                val cycleSet = cycle.toSet()
                if (cycleSet !in reportedCycles) {
                    reportedCycles += cycleSet
                    messages +=
                        ValidationMessage(
                            level = Level.ERROR,
                            code = "E004",
                            path = "fallback_colors.$startKey",
                            message = "检测到 `fallback_colors` 循环引用：${cycle.joinToString(" -> ")}。这会导致无限循环。",
                            suggestion = "请移除其中一处 fallback 引用以打破循环。",
                        )
                }
            }
        }
        return messages
    }

    private fun detectCycle(
        start: String,
        graph: Map<String, String?>,
    ): List<String>? {
        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()
        var current: String? = start
        while (current != null) {
            if (current in visited) {
                val cycleStart = path.indexOf(current)
                return path.subList(cycleStart, path.size) + current
            }
            visited += current
            path += current
            current = graph[current]
        }
        return null
    }
}
