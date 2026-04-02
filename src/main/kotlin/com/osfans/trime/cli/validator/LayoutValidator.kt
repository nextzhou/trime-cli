// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar

object LayoutValidator {
    private const val defaultKeyWeight = 10.0
    private const val defaultColumns = 30
    private const val maxRowWeight = 100.5
    private val spacerOnlyFields = setOf("width", "height")

    fun validate(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        messages += checkInvisibleKeyboard(node)
        messages += checkEmptyKeyboard(node)
        messages += checkWeightOverflow(node)
        messages += checkMissingKeyActions(node)
        messages += checkUnreasonableDimensions(node)
        return messages
    }

    private fun checkInvisibleKeyboard(node: YamlMap): List<ValidationMessage> {
        val styleMap = node.get<YamlMap>("style")
        val styleKeyHeight = styleMap
            ?.get<YamlScalar>("key_height")
            ?.content
            ?.toIntOrNull() ?: 0

        if (styleKeyHeight > 0) return emptyList()

        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()

        val anyKeyboardHasHeight = keyboards.entries.entries.any { (_, v) ->
            val kbMap = (v as? YamlMap) ?: return@any false
            val height = kbMap.get<YamlScalar>("height")?.content?.toFloatOrNull() ?: 0f
            height > 0f
        }

        if (!anyKeyboardHasHeight) {
            return listOf(
                ValidationMessage(
                    level = Level.ERROR,
                    code = "L001",
                    path = "style.key_height",
                    message = "所有键盘都没有设置 `height`，且 `style.key_height` 为 0，键盘将无法显示。",
                    suggestion = "请在 `style` 中设置 `key_height`，或至少给一个键盘设置 `height`。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkEmptyKeyboard(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val messages = mutableListOf<ValidationMessage>()

        for ((nameNode, value) in keyboards.entries.entries) {
            val kbMap = (value as? YamlMap) ?: continue
            val name = nameNode.content

            val hasImportPreset = kbMap.get<YamlScalar>("import_preset")?.content?.isNotEmpty() == true
            if (hasImportPreset) continue

            val keysList = kbMap.get<YamlList>("keys")
            val hasKeys = keysList != null && keysList.items.isNotEmpty()

            if (!hasKeys) {
                messages += ValidationMessage(
                    level = Level.WARNING,
                    code = "L002",
                    path = "preset_keyboards.$name",
                    message = "键盘 `$name` 既没有 `keys`，也没有 `import_preset`，渲染结果会是空键盘。",
                    suggestion = "请添加 `keys`，或设置 `import_preset` 引用其他键盘。",
                )
            }
        }

        return messages
    }

    private fun checkWeightOverflow(node: YamlMap): List<ValidationMessage> {
        val styleKeyWidth =
            node
                .get<YamlMap>("style")
                ?.get<YamlScalar>("key_width")
                ?.content
                ?.toDoubleOrNull()
                ?.takeIf { it > 0.0 } ?: defaultKeyWeight
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val messages = mutableListOf<ValidationMessage>()

        for ((nameNode, value) in keyboards.entries.entries) {
            val kbMap = value as? YamlMap ?: continue
            val name = nameNode.content
            val keysList = kbMap.get<YamlList>("keys")?.items ?: continue
            val keyboardKeyWidth =
                kbMap
                    .get<YamlScalar>("width")
                    ?.content
                    ?.toDoubleOrNull()
                    ?.takeIf { it > 0.0 } ?: styleKeyWidth
            val maxColumns =
                kbMap
                    .get<YamlScalar>("columns")
                    ?.content
                    ?.toIntOrNull()
                    ?.let { if (it == -1) Int.MAX_VALUE else it }
                    ?: defaultColumns

            var rowIndex = 1
            var currentRowWeight = 0.0
            var currentColumn = 0

            fun flushRow() {
                if (currentRowWeight > maxRowWeight) {
                    messages += buildWeightOverflowMessage(name, rowIndex, currentRowWeight)
                }
                rowIndex++
                currentRowWeight = 0.0
                currentColumn = 0
            }

            for (keyNode in keysList) {
                if (isExplicitRowBreak(keyNode as? YamlScalar)) {
                    flushRow()
                    continue
                }

                val keyMap = keyNode as? YamlMap ?: continue
                val click = keyMap.get<YamlScalar>("click")?.content.orEmpty()
                val keyWeight = resolveKeyWeight(keyMap, click, keyboardKeyWidth)
                val willExceedWidth = currentRowWeight > 0.0 && currentRowWeight + keyWeight > 100.0
                val willExceedColumns = currentColumn >= maxColumns

                if (willExceedWidth || willExceedColumns) {
                    flushRow()
                }

                currentRowWeight += keyWeight
                if (click.isNotEmpty()) {
                    currentColumn++
                }
            }

            if (currentRowWeight > maxRowWeight) {
                messages += buildWeightOverflowMessage(name, rowIndex, currentRowWeight)
            }
        }
        return messages
    }

    private fun checkMissingKeyActions(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val messages = mutableListOf<ValidationMessage>()

        for ((nameNode, value) in keyboards.entries.entries) {
            val kbMap = value as? YamlMap ?: continue
            val name = nameNode.content
            val keysList = kbMap.get<YamlList>("keys")?.items ?: continue

            keysList.forEachIndexed { index, keyNode ->
                val keyMap = keyNode as? YamlMap ?: return@forEachIndexed
                if (isSpacerKey(keyMap)) return@forEachIndexed

                val hasAction =
                    keyMap.get<YamlScalar>("action")?.content?.isNotEmpty() == true ||
                        keyMap.get<YamlScalar>("click")?.content?.isNotEmpty() == true ||
                        keyMap.get<YamlScalar>("key")?.content?.isNotEmpty() == true ||
                        keyMap.get<YamlScalar>("text")?.content?.isNotEmpty() == true ||
                        keyMap.get<YamlScalar>("functional")?.content?.isNotEmpty() == true

                if (!hasAction) {
                    messages += ValidationMessage(
                        level = Level.WARNING,
                        code = "L004",
                        path = "preset_keyboards.$name.keys[$index]",
                        message = "键盘 `$name` 中索引为 $index 的按键没有定义点击动作。",
                        suggestion = "请添加 `action`、`click`、`key` 或 `text` 字段来定义按键行为。",
                    )
                }
            }
        }
        return messages
    }

    private fun buildWeightOverflowMessage(
        keyboardName: String,
        rowIndex: Int,
        rowWeight: Double,
    ) = ValidationMessage(
        level = Level.WARNING,
        code = "L003",
        path = "preset_keyboards.$keyboardName.keys[row $rowIndex]",
        message =
            "键盘 `$keyboardName` 的第 $rowIndex 行按键总权重为 ${rowWeight.toInt()}，超过了 100，" +
                "可能会溢出屏幕宽度。",
        suggestion = "请调整第 $rowIndex 行各按键的宽度，使总和不超过 100。",
    )

    private fun isExplicitRowBreak(scalar: YamlScalar?): Boolean {
        val content = scalar?.content?.trim() ?: return false
        return content.isEmpty() || content == "\\n"
    }

    private fun resolveKeyWeight(
        keyMap: YamlMap,
        click: String,
        keyboardKeyWidth: Double,
    ): Double {
        val explicitWidth = keyMap.get<YamlScalar>("width")?.content?.toDoubleOrNull()
        return if ((explicitWidth == null || explicitWidth == 0.0) && click.isNotEmpty()) {
            keyboardKeyWidth
        } else {
            explicitWidth ?: 0.0
        }
    }

    private fun isSpacerKey(keyMap: YamlMap): Boolean {
        if (keyMap.get<YamlScalar>("click")?.content?.isNotEmpty() == true) return false

        val width = keyMap.get<YamlScalar>("width")?.content?.toDoubleOrNull() ?: return false
        if (width <= 0.0) return true

        return keyMap.entries.entries.all { (key, _) -> key.content in spacerOnlyFields }
    }

    private fun checkUnreasonableDimensions(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        val style = node.get<YamlMap>("style") ?: return emptyList()

        val keyHeight = style.get<YamlScalar>("key_height")?.content?.toIntOrNull()
        if (keyHeight != null && (keyHeight < 20 || keyHeight > 80)) {
            messages += ValidationMessage(
                level = Level.WARNING,
                code = "L005",
                path = "style.key_height",
                message = "`style.key_height` 为 ${keyHeight}dp，超出了常见的 20-80dp 范围。",
                suggestion = "普通键盘按键建议使用 20dp 到 80dp 之间的值。",
            )
        }

        val keyWidth = style.get<YamlScalar>("key_width")?.content?.toFloatOrNull()
        if (keyWidth != null && keyWidth <= 0f) {
            messages += ValidationMessage(
                level = Level.WARNING,
                code = "L006",
                path = "style.key_width",
                message = "`style.key_width` 为 ${keyWidth}，等于 0 或小于 0，按键将不可见。",
                suggestion = "请将 `key_width` 设为正数，例如 10（表示屏幕宽度的百分比权重）。",
            )
        }

        return messages
    }
}
