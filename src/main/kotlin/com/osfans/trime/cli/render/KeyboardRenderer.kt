// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.render

import com.osfans.trime.cli.keyboard.CalculatedKey
import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.cli.util.DpConverter
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.File
import kotlin.math.max
import javax.imageio.ImageIO

class KeyboardRenderer(
    private val colorManager: CliColorManager,
    private val fontManager: CliFontManager,
    private val density: Float = 2.75f,
) {
    fun render(
        keys: List<CalculatedKey>,
        width: Int,
        height: Int,
        backgroundColor: Int = 0xFF1A1A1A.toInt(),
    ): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            g.color = argbToAwtColor(backgroundColor)
            g.fillRect(0, 0, width, height)

            for (key in keys) {
                if (key.isSpacer) continue
                renderKey(g, key)
            }
        } finally {
            g.dispose()
        }

        return image
    }

    private fun renderKey(
        g: Graphics2D,
        key: CalculatedKey,
    ) {
        val horizontalInset = key.horizontalGap / 2f
        val verticalInset = key.verticalGap / 2f
        val x = key.x + horizontalInset
        val y = key.y + verticalInset
        val w = max(1f, key.width - key.horizontalGap.toFloat())
        val h = max(1f, key.height - key.verticalGap.toFloat())
        val radius = key.roundCorner * density

        val bgColor = resolveColor(key.keyBackgroundRef, "key_back_color", 0xFF333333.toInt())

        val rrect = RoundRectangle2D.Float(x, y, w, h, radius, radius)
        g.color = argbToAwtColor(bgColor)
        g.fill(rrect)

        val borderWidth = resolveBorderWidth(key)
        if (borderWidth > 0f) {
            val borderColor = colorManager.getColorOrNull("key_border_color") ?: 0xFF555555.toInt()
            g.color = argbToAwtColor(borderColor)
            g.stroke = BasicStroke(borderWidth)
            g.draw(rrect)
        }

        if (key.label.isNotEmpty()) {
            val textColor = resolveColor(key.keyTextColorRef, "key_text_color", 0xFFFFFFFF.toInt())
            renderMainLabel(g, key, x, y, w, h, textColor)
        }

        if (key.labelSymbol.isNotEmpty()) {
            val symbolColor =
                resolveColor(
                    preferredKey = key.keySymbolColorRef,
                    fallbackKey = "key_symbol_color",
                    default = 0xFF888888.toInt(),
                )
            renderSymbol(g, key.labelSymbol, key.symbolTextSize, key.keySymbolOffsetX, key.keySymbolOffsetY, x, y, w, h, symbolColor, isTop = true)
        }

        if (key.hint.isNotEmpty()) {
            val hintColor =
                resolveColor(
                    preferredKey = key.keySymbolColorRef,
                    fallbackKey = "key_symbol_color",
                    default = 0xFF888888.toInt(),
                )
            renderSymbol(g, key.hint, key.symbolTextSize, key.keyHintOffsetX, key.keyHintOffsetY, x, y, w, h, hintColor, isTop = false)
        }
    }

    private fun renderMainLabel(
        g: Graphics2D,
        key: CalculatedKey,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
    ) {
        if (key.label.startsWith("ic@")) return

        val fontSize = DpConverter.spToPx(key.keyTextSize, density).coerceAtLeast(1f)
        val font = fontManager.keyFont(fontSize)
        val offsetX = DpConverter.spToPx(key.keyTextOffsetX, density)
        val offsetY = DpConverter.spToPx(key.keyTextOffsetY, density)

        g.color = argbToAwtColor(color)
        g.font = font

        val fm = g.getFontMetrics(font)
        val ascent = fm.ascent.toFloat()
        val descent = fm.descent.toFloat()
        val centerX = x + w / 2f + offsetX
        val baselineY = y + (h + ascent - descent) / 2f + offsetY
        drawCenteredText(g, key.label, centerX, baselineY)
    }

    private fun renderSymbol(
        g: Graphics2D,
        text: String,
        textSize: Float,
        offsetX: Float,
        offsetY: Float,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color: Int,
        isTop: Boolean,
    ) {
        if (text.startsWith("ic@")) return

        val fontSize = DpConverter.spToPx(textSize, density).coerceAtLeast(1f)
        val font = fontManager.symbolFont(fontSize)
        val resolvedOffsetX = DpConverter.spToPx(offsetX, density)
        val resolvedOffsetY = DpConverter.spToPx(offsetY, density)

        g.color = argbToAwtColor(color)
        g.font = font

        val fm = g.getFontMetrics(font)
        val ascent = fm.ascent.toFloat()
        val descent = fm.descent.toFloat()
        val centerX = x + w / 2f + resolvedOffsetX
        val baselineY =
            if (isTop) {
                y + ascent + resolvedOffsetY
            } else {
                y + h - descent + resolvedOffsetY
            }

        drawCenteredText(g, text, centerX, baselineY)
    }

    private fun drawCenteredText(
        g: Graphics2D,
        text: String,
        centerX: Float,
        baselineY: Float,
    ) {
        val fm = g.fontMetrics
        val textX = centerX - fm.stringWidth(text) / 2f
        g.drawString(text, textX, baselineY)
    }

    private fun resolveColor(
        preferredKey: String,
        fallbackKey: String,
        default: Int,
    ): Int =
        preferredKey.takeIf { it.isNotEmpty() }?.let(colorManager::getColorOrNull)
            ?: colorManager.getColorOrNull(fallbackKey)
            ?: default

    private fun resolveBorderWidth(key: CalculatedKey): Float {
        if (key.keyBorder > 0) {
            return key.keyBorder * density
        }

        return if (key.horizontalGap == 0 || key.verticalGap == 0) 1f else 0f
    }

    private fun argbToAwtColor(argb: Int): Color {
        val a = (argb shr 24) and 0xFF
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return Color(r, g, b, a)
    }

    fun exportToPng(
        image: BufferedImage,
        outputFile: File,
    ) {
        outputFile.parentFile?.mkdirs()
        ImageIO.write(image, "PNG", outputFile)
    }
}
