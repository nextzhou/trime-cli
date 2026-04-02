// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.theme

import com.osfans.trime.cli.util.ColorUtils
import com.osfans.trime.data.theme.Theme
import com.osfans.trime.data.theme.model.ColorScheme

class CliColorManager(
    private val theme: Theme,
    private val isDarkMode: Boolean = false,
) {
    private val activeColorScheme: ColorScheme by lazy { selectColorScheme() }

    // Ported exactly from ColorManager.kt lines 52-94 (42 entries)
    private val builtinFallbackColors: Map<String, String> =
        mapOf(
            "candidate_text_color" to "text_color",
            "comment_text_color" to "candidate_text_color",
            "border_color" to "back_color",
            "candidate_separator_color" to "border_color",
            "hilited_text_color" to "text_color",
            "hilited_back_color" to "back_color",
            "hilited_candidate_text_color" to "hilited_text_color",
            "hilited_candidate_back_color" to "hilited_back_color",
            "hilited_candidate_button_color" to "hilited_candidate_back_color",
            "hilited_label_color" to "hilited_candidate_text_color",
            "hilited_comment_text_color" to "comment_text_color",
            "hilited_key_back_color" to "hilited_candidate_back_color",
            "hilited_key_text_color" to "hilited_candidate_text_color",
            "hilited_key_symbol_color" to "hilited_comment_text_color",
            "hilited_off_key_back_color" to "hilited_key_back_color",
            "hilited_on_key_back_color" to "hilited_key_back_color",
            "hilited_off_key_text_color" to "hilited_key_text_color",
            "hilited_on_key_text_color" to "hilited_key_text_color",
            "key_back_color" to "back_color",
            "key_border_color" to "border_color",
            "key_text_color" to "candidate_text_color",
            "key_symbol_color" to "comment_text_color",
            "label_color" to "candidate_text_color",
            "off_key_back_color" to "key_back_color",
            "off_key_text_color" to "key_text_color",
            "on_key_back_color" to "hilited_key_back_color",
            "on_key_text_color" to "hilited_key_text_color",
            "popup_back_color" to "key_back_color",
            "popup_text_color" to "key_text_color",
            "hilited_popup_back_color" to "hilited_key_back_color",
            "hilited_popup_text_color" to "hilited_key_text_color",
            "shadow_color" to "border_color",
            "root_background" to "back_color",
            "candidate_background" to "back_color",
            "keyboard_back_color" to "border_color",
            "keyboard_background" to "keyboard_back_color",
            "liquid_keyboard_background" to "keyboard_back_color",
            "text_back_color" to "back_color",
            "long_text_color" to "key_text_color",
            "long_text_back_color" to "key_back_color",
        )

    private fun colorScheme(id: String) = theme.colorSchemes.find { it.id == id }

    private fun selectColorScheme(): ColorScheme {
        val defaultScheme = colorScheme("default") ?: theme.colorSchemes.firstOrNull()
            ?: ColorScheme("default", emptyMap())

        if (isDarkMode) {
            defaultScheme.colors["dark_scheme"]?.let { colorScheme(it) }?.let { return it }
        } else {
            defaultScheme.colors["light_scheme"]?.let { colorScheme(it) }?.let { return it }
        }
        return defaultScheme
    }

    fun getColor(key: String): Int = resolveColor(key, mutableSetOf())

    fun getColorOrNull(key: String): Int? = runCatching { getColor(key) }.getOrNull()

    private fun resolveColor(
        key: String,
        visited: MutableSet<String>,
    ): Int {
        if (!visited.add(key)) {
            throw IllegalArgumentException(
                "Circular color fallback detected: ${visited.joinToString(" -> ")} -> $key",
            )
        }

        activeColorScheme.colors[key]?.let { value ->
            return if (ColorUtils.isValidColor(value)) {
                ColorUtils.parseColor(value)
            } else {
                resolveColor(value, visited)
            }
        }

        theme.fallbackColors[key]?.let { fallbackKey ->
            return resolveColor(fallbackKey, visited)
        }

        builtinFallbackColors[key]?.let { fallbackKey ->
            return resolveColor(fallbackKey, visited)
        }

        throw IllegalArgumentException("Color key '$key' not found in any color scheme or fallback chain")
    }

    fun getDrawable(key: String): DrawableInfo? =
        getColorOrNull(key)?.let { color ->
            DrawableInfo(type = DrawableInfo.Type.SOLID_COLOR, color = color)
        }
}
