// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.util

/**
 * Pure-JVM dp/sp to pixel conversion utilities.
 * Replaces Android's TypedValue.applyDimension() and splitties dp() extension.
 */
object DpConverter {
    /**
     * Convert density-independent pixels (dp) to physical pixels.
     *
     * @param dp value in density-independent pixels
     * @param density screen density (e.g. 2.75 for a 440 DPI screen)
     * @return pixel value (rounded to nearest integer)
     */
    fun dpToPx(dp: Int, density: Float): Int = (dp * density + 0.5f).toInt()

    /**
     * Convert scale-independent pixels (sp) to physical pixels.
     *
     * @param sp value in scale-independent pixels
     * @param scaledDensity font scale factor (typically same as density)
     * @return pixel value as float
     */
    fun spToPx(sp: Float, scaledDensity: Float): Float = sp * scaledDensity

    /**
     * Convert physical pixels back to dp.
     */
    fun pxToDp(px: Int, density: Float): Float = px / density
}
