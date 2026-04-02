// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.theme

data class DrawableInfo(
    val type: Type,
    val color: Int = 0,
    val borderColor: Int = 0,
    val borderWidth: Float = 0f,
    val cornerRadius: Float = 0f,
    val gradientColors: IntArray? = null,
    val gradientOrientation: GradientOrientation = GradientOrientation.TOP_BOTTOM,
    val imagePath: String? = null,
) {
    enum class Type { SOLID_COLOR, GRADIENT, IMAGE }

    enum class GradientOrientation { TOP_BOTTOM, LEFT_RIGHT, TL_BR }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DrawableInfo) return false
        return type == other.type &&
            color == other.color &&
            borderColor == other.borderColor &&
            borderWidth == other.borderWidth &&
            cornerRadius == other.cornerRadius &&
            gradientColors.contentEquals(other.gradientColors) &&
            gradientOrientation == other.gradientOrientation &&
            imagePath == other.imagePath
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + color
        result = 31 * result + borderColor
        result = 31 * result + borderWidth.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + (gradientColors?.contentHashCode() ?: 0)
        result = 31 * result + gradientOrientation.hashCode()
        result = 31 * result + (imagePath?.hashCode() ?: 0)
        return result
    }
}
