// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import com.osfans.trime.cli.util.ColorUtils
import com.osfans.trime.ime.keyboard.KeyBehavior
import java.io.File

object SilentFailureValidator {
    private data class ActionReference(
        val action: String,
        val path: String,
    )

    private data class KeyboardTargetReference(
        val target: String,
        val path: String,
        val kind: String,
    )

    private val builtinActionNames =
        setOf(
            "Return", "BackSpace", "space", "Shift_L", "Shift_R",
            "Control_L", "Control_R", "Alt_L", "Alt_R",
            "Meta_L", "Meta_R", "Escape", "Delete",
            "Home", "End", "Insert", "Page_Up", "Page_Down",
            "Left", "Right", "Up", "Down",
            "Tab", "Menu", "Find", "Mode_switch",
            "Henkan", "Zenkaku_Hankaku", "Eisu_toggle",
            "LANGUAGE_SWITCH", "PROG_RED", "SYM",
            "comma", "period",
        )

    fun validate(
        node: YamlMap,
        fontDir: File? = null,
    ): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        messages += checkMissingPresetKeyboards(node)
        messages += checkMissingDefaultKeyboard(node)
        messages += checkInvalidColorValues(node)
        messages += checkInvalidImportPresetTargets(node)
        messages += checkMissingNamedActionDefinitions(node)
        messages += checkInvalidKeyboardTargets(node)
        messages += checkUnreferencedKeyboards(node)
        messages += checkUnreachableColorKeys(node)
        if (fontDir != null) {
            messages += checkMissingFontFiles(node, fontDir)
        }
        return messages
    }

    private fun checkMissingPresetKeyboards(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards")
        if (keyboards == null || keyboards.entries.entries.isEmpty()) {
            return listOf(
                ValidationMessage(
                    level = Level.WARNING,
                    code = "W001",
                    path = "preset_keyboards",
                    message = "`preset_keyboards` 中没有定义任何键盘，应用将无键盘可显示。",
                    suggestion = "请在 `preset_keyboards:` 下至少添加一个键盘定义。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkMissingDefaultKeyboard(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val hasDefault = keyboards.entries.entries.any { it.key.content == "default" }
        if (!hasDefault && keyboards.entries.entries.isNotEmpty()) {
            return listOf(
                ValidationMessage(
                    level = Level.WARNING,
                    code = "W002",
                    path = "preset_keyboards",
                    message =
                        "`preset_keyboards` 中没有名为 `default` 的键盘。" +
                            " 程序会回退到第一个键盘。",
                    suggestion = "请添加名为 `default` 的键盘，或将现有键盘重命名为 `default`。",
                ),
            )
        }
        return emptyList()
    }

    private fun checkInvalidColorValues(node: YamlMap): List<ValidationMessage> {
        val messages = mutableListOf<ValidationMessage>()
        val schemes = node.get<YamlMap>("preset_color_schemes") ?: return emptyList()
        for ((schemeKey, schemeValue) in schemes.entries.entries) {
            val schemeMap = schemeValue as? YamlMap ?: continue
            for ((colorKey, colorValue) in schemeMap.entries.entries) {
                val colorStr = (colorValue as? YamlScalar)?.content ?: continue
                // Skip non-color values (like scheme names, boolean flags)
                if (!colorStr.startsWith("#") &&
                    !colorStr.startsWith("0x") &&
                    !colorStr.startsWith("0X")
                ) {
                    continue
                }
                if (!ColorUtils.isValidColor(colorStr)) {
                    messages +=
                        ValidationMessage(
                            level = Level.WARNING,
                            code = "W003",
                            path = "preset_color_schemes.${schemeKey.content}.${colorKey.content}",
                            message =
                                "配色方案 `${schemeKey.content}` 中的 `$colorStr` 不是合法颜色值。" +
                                    " 期望格式为 `#RGB`、`#RRGGBB` 或 `#AARRGGBB`。",
                            suggestion = "请使用合法的十六进制颜色格式，例如红色可写为 `#FF0000`。",
                        )
                }
            }
        }
        return messages
    }

    private fun checkInvalidImportPresetTargets(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val keyboardNames = keyboards.entries.entries.map { it.key.content }.toSet()
        val messages = mutableListOf<ValidationMessage>()
        for ((kbKey, kbValue) in keyboards.entries.entries) {
            val kbMap = kbValue as? YamlMap ?: continue
            val importPreset = kbMap.get<YamlScalar>("import_preset")?.content ?: continue
            if (importPreset !in keyboardNames) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "W004",
                        path = "preset_keyboards.${kbKey.content}.import_preset",
                        message =
                            "键盘 `${kbKey.content}` 引用了不存在的 `import_preset`：" +
                                " `$importPreset`。",
                        suggestion = "请定义名为 `$importPreset` 的键盘，或修改 `import_preset` 的值。",
                    )
            }
        }
        return messages
    }

    private fun checkMissingFontFiles(
        node: YamlMap,
        fontDir: File,
    ): List<ValidationMessage> {
        val style = node.get<YamlMap>("style") ?: return emptyList()
        val fontKeys =
            listOf(
                "hanb_font",
                "latin_font",
                "candidate_font",
                "comment_font",
                "key_font",
                "label_font",
                "popup_font",
                "symbol_font",
                "text_font",
            )
        val messages = mutableListOf<ValidationMessage>()
        for (fontKey in fontKeys) {
            val fontList = style.get<YamlList>(fontKey)?.items ?: continue
            for (fontNode in fontList) {
                val fontName = (fontNode as? YamlScalar)?.content ?: continue
                if (!File(fontDir, fontName).exists()) {
                    messages +=
                        ValidationMessage(
                            level = Level.WARNING,
                            code = "W005",
                            path = "style.$fontKey",
                            message = "`style.$fontKey` 引用的字体 `$fontName` 在字体目录中不存在。",
                            suggestion = "请将 `$fontName` 放入字体目录，或移除这条引用。",
                        )
                }
            }
        }

        // Also check tool_bar.button_font
        val toolBar = node.get<YamlMap>("tool_bar")
        val buttonFontList = toolBar?.get<YamlList>("button_font")?.items ?: emptyList()
        for (fontNode in buttonFontList) {
            val fontName = (fontNode as? YamlScalar)?.content ?: continue
            if (!File(fontDir, fontName).exists()) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "W005",
                        path = "tool_bar.button_font",
                        message = "`tool_bar.button_font` 引用的字体 `$fontName` 在字体目录中不存在。",
                        suggestion = "请将 `$fontName` 放入字体目录，或移除这条引用。",
                    )
            }
        }

        return messages
    }

    private fun checkMissingNamedActionDefinitions(node: YamlMap): List<ValidationMessage> {
        val presetKeyNames =
            node.get<YamlMap>("preset_keys")
                ?.entries
                ?.entries
                ?.map { it.key.content }
                ?.toSet()
                ?: emptySet()

        return collectActionReferences(node)
            .filter { looksLikeNamedActionWithoutPreset(it.action, presetKeyNames) }
            .groupBy { it.action }
            .toSortedMap()
            .values
            .map { refs ->
                val action = refs.first().action
                val paths =
                    refs
                        .map { it.path }
                        .distinct()
                        .sorted()
                ValidationMessage(
                    level = Level.WARNING,
                    code = "W008",
                    path = paths.first(),
                    message =
                        "命名动作 `$action` 在 ${paths.size} 处被引用，但没有在 `preset_keys` 中定义。" +
                            " Trime 会把它当作普通文本，而不是执行预期动作。",
                    suggestion =
                        "请在 `preset_keys` 中补充 `$action` 的定义，" +
                            "或把这些位置改成标准键名/直接文本。引用位置：" +
                            paths.joinToString(", "),
                )
            }
    }

    private fun checkInvalidKeyboardTargets(node: YamlMap): List<ValidationMessage> {
        val keyboardNames =
            node.get<YamlMap>("preset_keyboards")
                ?.entries
                ?.entries
                ?.map { it.key.content }
                ?.toSet()
                ?: emptySet()

        return collectKeyboardTargetReferences(node)
            .filter { it.target.isNotBlank() && !it.target.startsWith(".") && it.target !in keyboardNames }
            .groupBy { it.target }
            .toSortedMap()
            .values
            .map { refs ->
                val target = refs.first().target
                val paths =
                    refs
                        .map { it.path }
                        .distinct()
                        .sorted()
                val kinds =
                    refs
                        .map { it.kind }
                        .distinct()
                        .sorted()
                ValidationMessage(
                    level = Level.WARNING,
                    code = "W009",
                    path = paths.first(),
                    message =
                        "键盘 `$target` 被 ${kinds.joinToString("、")} 引用，但没有在 `preset_keyboards` 中定义。",
                    suggestion =
                        "请在 `preset_keyboards` 中定义 `$target`，或修改这些引用：" +
                            paths.joinToString(", "),
                )
            }
    }

    private fun checkUnreferencedKeyboards(node: YamlMap): List<ValidationMessage> {
        val keyboards = node.get<YamlMap>("preset_keyboards") ?: return emptyList()
        val keyboardNames = keyboards.entries.entries.map { it.key.content }.toSet()
        val referenced = mutableSetOf<String>()

        fun addReference(candidate: String?) {
            val value = candidate?.trim().orEmpty()
            if (value.isEmpty() || value.startsWith(".")) return
            referenced += value
        }

        fun addIncludeReference(includePath: String?) {
            val value = includePath?.trim().orEmpty()
            if (!value.startsWith("/preset_keyboards/")) return
            addReference(value.removePrefix("/preset_keyboards/"))
        }

        for ((_, value) in keyboards.entries.entries) {
            val kbMap = value as? YamlMap ?: continue
            addReference(kbMap.get<YamlScalar>("import_preset")?.content)
            addReference(kbMap.get<YamlScalar>("ascii_keyboard")?.content)
            addReference(kbMap.get<YamlScalar>("landscape_keyboard")?.content)
            addIncludeReference(kbMap.get<YamlScalar>("__include")?.content)

            val keys = kbMap.get<YamlList>("keys")?.items ?: emptyList()
            for (keyNode in keys) {
                val keyMap = keyNode as? YamlMap ?: continue
                addReference(keyMap.get<YamlScalar>("select")?.content)
                addIncludeReference(keyMap.get<YamlScalar>("__include")?.content)
            }
        }

        val presetKeys = node.get<YamlMap>("preset_keys")
        presetKeys?.entries?.entries?.forEach { (_, keyValue) ->
            val keyMap = keyValue as? YamlMap ?: return@forEach
            addReference(keyMap.get<YamlScalar>("select")?.content)
        }

        val unreferenced =
            keyboardNames
                .filter { it != "default" && it !in referenced }
                .sorted()

        if (unreferenced.isEmpty()) return emptyList()

        return listOf(
            ValidationMessage(
                level = Level.INFO,
                code = "W006",
                path = "preset_keyboards",
                message =
                    "有 ${unreferenced.size} 个键盘已定义，但没有被 `import_preset`、`select`、" +
                        "`ascii_keyboard`、`landscape_keyboard` 或 `__include` 引用：" +
                        unreferenced.joinToString(", "),
                suggestion =
                    "如果这些键盘本来就是为了给别的主题复用导出的，可以忽略此提示；" +
                        "否则请删除它们或补充引用关系。",
            ),
        )
    }

    private fun checkUnreachableColorKeys(node: YamlMap): List<ValidationMessage> {
        // Collect all defined color keys from all schemes + fallback_colors
        val definedColorKeys = mutableSetOf<String>()

        val schemes = node.get<YamlMap>("preset_color_schemes")
        schemes?.entries?.entries?.forEach { (_, schemeValue) ->
            val schemeMap = schemeValue as? YamlMap ?: return@forEach
            schemeMap.entries.entries.forEach { (k, _) -> definedColorKeys.add(k.content) }
        }

        val fallbacks = node.get<YamlMap>("fallback_colors")
        fallbacks?.entries?.entries?.forEach { (k, _) -> definedColorKeys.add(k.content) }

        // Known built-in color keys that are always available
        val builtinKeys =
            setOf(
                "text_color", "back_color", "key_text_color", "key_back_color",
                "shadow_color", "candidate_text_color", "candidate_view_border",
                "hilited_text_color", "hilited_back_color", "hilited_candidate_text_color",
                "hilited_candidate_back_color", "comment_text_color", "hilited_comment_text_color",
                "key_border_color", "border_color", "candidate_separator_color",
                "hilited_key_text_color", "hilited_key_back_color", "hilited_key_symbol_color",
                "key_symbol_color", "key_hint_color", "hilited_key_hint_color", "spacer_color",
            )
        definedColorKeys.addAll(builtinKeys)

        // Collect color key references from preset_keys
        val usedColorKeys = mutableSetOf<Pair<String, String>>() // key name → path
        val presetKeys = node.get<YamlMap>("preset_keys")
        presetKeys?.entries?.entries?.forEach { (keyName, keyValue) ->
            val keyMap = keyValue as? YamlMap ?: return@forEach
            listOf("key_text_color", "key_back_color", "key_symbol_color", "key_border_color").forEach { colorField ->
                val colorRef = keyMap.get<YamlScalar>(colorField)?.content ?: return@forEach
                // Only check references (not literal hex values)
                if (!colorRef.startsWith("#") && !colorRef.startsWith("0x") && !colorRef.startsWith("0X")) {
                    usedColorKeys.add(colorRef to "preset_keys.${keyName.content}.$colorField")
                }
            }
        }

        val messages = mutableListOf<ValidationMessage>()
        for ((colorKey, path) in usedColorKeys) {
            if (colorKey !in definedColorKeys) {
                messages +=
                    ValidationMessage(
                        level = Level.WARNING,
                        code = "W007",
                        path = path,
                        message =
                            "颜色键 `$colorKey` 被引用了，但没有在任何配色方案或 `fallback_colors` 中定义。",
                        suggestion = "请在配色方案中定义 `$colorKey`，或把它加入 `fallback_colors`。",
                    )
            }
        }
        return messages
    }

    private fun collectActionReferences(node: YamlMap): List<ActionReference> {
        val references = mutableListOf<ActionReference>()
        val actionFields = KeyBehavior.entries.map { it.name.lowercase() }

        val keyboards = node.get<YamlMap>("preset_keyboards")
        keyboards?.entries?.entries?.forEach { (keyboardName, keyboardValue) ->
            val keyboardMap = keyboardValue as? YamlMap ?: return@forEach
            val keys = keyboardMap.get<YamlList>("keys")?.items ?: emptyList()
            keys.forEachIndexed { index, keyNode ->
                val keyMap = keyNode as? YamlMap ?: return@forEachIndexed
                actionFields.forEach { field ->
                    val action = keyMap.get<YamlScalar>(field)?.content?.trim().orEmpty()
                    if (action.isNotEmpty()) {
                        references +=
                            ActionReference(
                                action = action,
                                path = "preset_keyboards.${keyboardName.content}.keys[$index].$field",
                            )
                    }
                }
            }
        }

        val toolBar = node.get<YamlMap>("tool_bar")
        toolBar?.get<YamlMap>("primary_button")?.let { button ->
            addToolBarActionReferences(
                button = button,
                basePath = "tool_bar.primary_button",
                references = references,
            )
        }
        toolBar?.get<YamlList>("buttons")?.items?.forEachIndexed { index, buttonNode ->
            val button = buttonNode as? YamlMap ?: return@forEachIndexed
            addToolBarActionReferences(
                button = button,
                basePath = "tool_bar.buttons[$index]",
                references = references,
            )
        }

        return references
    }

    private fun addToolBarActionReferences(
        button: YamlMap,
        basePath: String,
        references: MutableList<ActionReference>,
    ) {
        listOf("action", "long_press_action").forEach { field ->
            val action = button.get<YamlScalar>(field)?.content?.trim().orEmpty()
            if (action.isNotEmpty()) {
                references += ActionReference(action = action, path = "$basePath.$field")
            }
        }
    }

    private fun collectKeyboardTargetReferences(node: YamlMap): List<KeyboardTargetReference> {
        val references = mutableListOf<KeyboardTargetReference>()

        val presetKeys = node.get<YamlMap>("preset_keys")
        presetKeys?.entries?.entries?.forEach { (keyName, keyValue) ->
            val keyMap = keyValue as? YamlMap ?: return@forEach
            val target = keyMap.get<YamlScalar>("select")?.content?.trim().orEmpty()
            if (target.isNotEmpty()) {
                references +=
                    KeyboardTargetReference(
                        target = target,
                        path = "preset_keys.${keyName.content}.select",
                        kind = "`select`",
                    )
            }
        }

        val keyboards = node.get<YamlMap>("preset_keyboards")
        keyboards?.entries?.entries?.forEach { (keyboardName, keyboardValue) ->
            val keyboardMap = keyboardValue as? YamlMap ?: return@forEach
            listOf(
                "ascii_keyboard" to "`ascii_keyboard`",
                "landscape_keyboard" to "`landscape_keyboard`",
            ).forEach { (field, kind) ->
                val target = keyboardMap.get<YamlScalar>(field)?.content?.trim().orEmpty()
                if (target.isNotEmpty()) {
                    references +=
                        KeyboardTargetReference(
                            target = target,
                            path = "preset_keyboards.${keyboardName.content}.$field",
                            kind = kind,
                        )
                }
            }

            val keys = keyboardMap.get<YamlList>("keys")?.items ?: emptyList()
            keys.forEachIndexed { index, keyNode ->
                val keyMap = keyNode as? YamlMap ?: return@forEachIndexed
                val target = keyMap.get<YamlScalar>("select")?.content?.trim().orEmpty()
                if (target.isNotEmpty()) {
                    references +=
                        KeyboardTargetReference(
                            target = target,
                            path = "preset_keyboards.${keyboardName.content}.keys[$index].select",
                            kind = "`select`",
                        )
                }
            }
        }

        return references
    }

    private fun looksLikeNamedActionWithoutPreset(
        action: String,
        presetKeyNames: Set<String>,
    ): Boolean {
        if (action.isBlank() || action in presetKeyNames) return false
        if (action.length == 1) return false
        if (action.startsWith("{") && action.endsWith("}")) return false
        if (action in builtinActionNames) return false
        if (action.all { it.isUpperCase() || it.isDigit() || it == '_' }) return false

        if (!action.all { it.isLetterOrDigit() || it == '_' }) return false

        return action.any { it == '_' || it.isUpperCase() || it.isDigit() }
    }
}
