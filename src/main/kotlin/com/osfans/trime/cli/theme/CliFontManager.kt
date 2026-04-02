/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.cli.theme

import com.osfans.trime.data.theme.model.GeneralStyle
import java.awt.Font
import java.io.File
import java.util.logging.Logger
import kotlin.math.roundToInt

/**
 * Pure-JVM font path resolver for the CLI tool.
 * Replaces Android's FontManager (which uses Typeface/FontFamily APIs).
 */
class CliFontManager(
    private val generalStyle: GeneralStyle,
    private val fontDir: File,
) {
    private val logger = Logger.getLogger("trime-cli")
    private val baseFontCache = mutableMapOf<String, Font?>()
    private val derivedFontCache = mutableMapOf<Pair<String, Int>, Font>()

    /**
     * Resolve a font name to a File in the font directory.
     * Returns null if the font file does not exist.
     */
    fun resolveFont(fontName: String): File? {
        val file = File(fontDir, fontName)
        return if (file.exists()) file else null
    }

    /**
     * Resolve all fonts referenced in GeneralStyle.
     * Returns a map of font key → resolved File list (null entries for missing fonts).
     */
    fun resolveAllFonts(): Map<String, List<File?>> {
        val result = mutableMapOf<String, List<File?>>()

        fun resolveFontList(fontList: List<String>?): List<File?> =
            fontList?.map { resolveFont(it) } ?: emptyList()

        result["hanb_font"] = resolveFontList(generalStyle.hanbFont)
        result["latin_font"] = resolveFontList(generalStyle.latinFont)
        result["candidate_font"] = resolveFontList(generalStyle.candidateFont)
        result["comment_font"] = resolveFontList(generalStyle.commentFont)
        result["key_font"] = resolveFontList(generalStyle.keyFont)
        result["label_font"] = resolveFontList(generalStyle.labelFont)
        result["popup_font"] = resolveFontList(generalStyle.popupFont)
        result["symbol_font"] = resolveFontList(generalStyle.symbolFont)
        result["text_font"] = resolveFontList(generalStyle.textFont)

        return result
    }

    /**
     * Get the first available font file for a given key.
     * Returns null if no font in the list exists.
     */
    fun getFirstAvailableFont(fontList: List<String>?): File? =
        fontList?.firstNotNullOfOrNull { resolveFont(it) }

    fun keyFont(sizePx: Float): Font = fontFor("key_font", generalStyle.keyFont, sizePx)

    fun symbolFont(sizePx: Float): Font = fontFor("symbol_font", generalStyle.symbolFont, sizePx)

    private fun fontFor(
        cacheKey: String,
        fontList: List<String>,
        sizePx: Float,
    ): Font {
        val sizeKey = sizePx.roundToInt().coerceAtLeast(1)
        return derivedFontCache.getOrPut(cacheKey to sizeKey) {
            val baseFont = getFirstAvailableFont(fontList)?.let(::loadBaseFont)
            (baseFont ?: Font(Font.SANS_SERIF, Font.PLAIN, sizeKey)).deriveFont(sizePx.coerceAtLeast(1f))
        }
    }

    private fun loadBaseFont(file: File): Font? =
        baseFontCache.getOrPut(file.absolutePath) {
            runCatching {
                file.inputStream().use { input ->
                    Font.createFont(Font.TRUETYPE_FONT, input)
                }
            }.onFailure {
                logger.warning("Failed to load font ${file.absolutePath}: ${it.message}")
            }.getOrNull()
        }
}
