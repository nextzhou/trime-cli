// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.render

import com.osfans.trime.cli.keyboard.KeyboardCalculator
import com.osfans.trime.cli.keyboard.KeyboardPreviewContext
import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.data.theme.Theme
import java.awt.image.BufferedImage
import java.io.File
import java.util.logging.Logger

class KeyboardImageExporter(
    private val theme: Theme,
    private val colorManager: CliColorManager,
    private val fontManager: CliFontManager,
    private val displayWidthPx: Int = 1080,
    private val density: Float = 2.75f,
    private val isLandscape: Boolean = false,
) {
    private val logger = Logger.getLogger("trime-cli")
    private val renderer = KeyboardRenderer(colorManager, fontManager, density)
    private val previewContext = KeyboardPreviewContext.fromGeneralStyle(theme.generalStyle)

    fun renderAll(): Map<String, BufferedImage> {
        val rendered = linkedMapOf<String, BufferedImage>()

        for ((name, keyboard) in theme.presetKeyboards) {
            try {
                val keys =
                    KeyboardCalculator.calculate(
                        keyboard = keyboard,
                        generalStyle = theme.generalStyle,
                        presetKeys = theme.presetKeys,
                        displayWidthPx = displayWidthPx,
                        density = density,
                        isLandscape = isLandscape,
                        previewContext = previewContext,
                    )

                if (keys.isEmpty()) {
                    logger.warning("Keyboard '$name' has no keys, skipping render")
                    continue
                }

                val keyboardHeight = keys.maxOf { it.y + it.height }
                if (keyboardHeight == 0) {
                    logger.warning("Keyboard '$name' has zero height, skipping render")
                    continue
                }

                rendered[name] = renderer.render(keys, displayWidthPx, keyboardHeight, resolveBackgroundColor())
            } catch (e: Exception) {
                logger.warning("Failed to render keyboard '$name': ${e.message}")
            }
        }

        return rendered
    }

    fun exportAll(outputDir: File): List<File> {
        outputDir.mkdirs()
        val created = mutableListOf<File>()

        renderAll().forEach { (name, image) ->
            val outputFile = File(outputDir, "keyboard_$name.png")
            renderer.exportToPng(image, outputFile)
            created += outputFile
            logger.info("Rendered keyboard '$name' -> ${outputFile.name}")
        }

        return created
    }

    private fun resolveBackgroundColor(): Int =
        colorManager.getColorOrNull("keyboard_background")
            ?: colorManager.getColorOrNull("keyboard_back_color")
            ?: colorManager.getColorOrNull("back_color")
            ?: 0xFF1A1A1A.toInt()
}
