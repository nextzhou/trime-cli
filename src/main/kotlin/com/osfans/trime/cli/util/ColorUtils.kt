// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.util

/**
 * Pure-JVM color parsing utilities.
 * Replaces android.graphics.Color and androidx.core.graphics.toColorInt()
 * for use in desktop CLI contexts.
 */
object ColorUtils {
    // Named colors matching Android's Color.parseColor() set
    private val NAMED_COLORS = mapOf(
        "black" to 0xFF000000.toInt(),
        "darkgray" to 0xFF444444.toInt(),
        "gray" to 0xFF888888.toInt(),
        "lightgray" to 0xFFCCCCCC.toInt(),
        "white" to 0xFFFFFFFF.toInt(),
        "red" to 0xFFFF0000.toInt(),
        "green" to 0xFF00FF00.toInt(),
        "blue" to 0xFF0000FF.toInt(),
        "yellow" to 0xFFFFFF00.toInt(),
        "cyan" to 0xFF00FFFF.toInt(),
        "magenta" to 0xFFFF00FF.toInt(),
        "aqua" to 0xFF00FFFF.toInt(),
        "fuchsia" to 0xFFFF00FF.toInt(),
        "darkgrey" to 0xFF444444.toInt(),
        "grey" to 0xFF888888.toInt(),
        "lightgrey" to 0xFFCCCCCC.toInt(),
        "lime" to 0xFF00FF00.toInt(),
        "maroon" to 0xFF800000.toInt(),
        "navy" to 0xFF000080.toInt(),
        "olive" to 0xFF808000.toInt(),
        "purple" to 0xFF800080.toInt(),
        "silver" to 0xFFC0C0C0.toInt(),
        "teal" to 0xFF008080.toInt(),
        "transparent" to 0x00000000,
    )

    /**
     * Parse a color string to an ARGB int (same format as Android's Color.parseColor).
     *
     * Supported formats:
     * - `#RGB` → expands to `#FFRRGGBB`
     * - `#RRGGBB` → `#FFRRGGBB` (fully opaque)
     * - `#AARRGGBB`
     * - `0xRRGGBB` → `#FFRRGGBB` (fully opaque)
     * - `0xAARRGGBB`
     * - Named colors (see NAMED_COLORS map)
     *
     * @throws IllegalArgumentException if the string is not a recognized color format
     */
    fun parseColor(colorString: String): Int {
        if (colorString.isEmpty()) {
            throw IllegalArgumentException("Color string must not be empty")
        }

        // Check named colors first (case-insensitive)
        NAMED_COLORS[colorString.lowercase()]?.let { return it }

        // Handle hex formats
        val hex = when {
            colorString.startsWith("#") -> colorString.substring(1)
            colorString.startsWith("0x", ignoreCase = true) ||
                colorString.startsWith("0X") -> colorString.substring(2)
            else -> throw IllegalArgumentException(
                "Unknown color format: '$colorString'. " +
                "Expected #RGB, #RRGGBB, #AARRGGBB, 0xRRGGBB, or a named color."
            )
        }

        return when (hex.length) {
            3 -> {
                // #RGB → #FFRRGGBB
                val r = hex[0].toString().repeat(2).toInt(16)
                val g = hex[1].toString().repeat(2).toInt(16)
                val b = hex[2].toString().repeat(2).toInt(16)
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            6 -> {
                // #RRGGBB → #FFRRGGBB
                val color = hex.toLongOrNull(16)
                    ?: throw IllegalArgumentException("Invalid hex color: '$colorString'")
                (0xFF000000L or color).toInt()
            }
            8 -> {
                // #AARRGGBB
                val color = hex.toLongOrNull(16)
                    ?: throw IllegalArgumentException("Invalid hex color: '$colorString'")
                color.toInt()
            }
            else -> throw IllegalArgumentException(
                "Invalid hex color length in '$colorString': expected 3, 6, or 8 hex digits"
            )
        }
    }

    /**
     * Returns true if the string is a valid color string parseable by parseColor().
     */
    fun isValidColor(colorString: String): Boolean = runCatching { parseColor(colorString) }.isSuccess
}
