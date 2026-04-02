// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.keyboard

import com.osfans.trime.cli.util.DpConverter
import com.osfans.trime.data.theme.model.GeneralStyle
import com.osfans.trime.data.theme.model.PresetKey
import com.osfans.trime.data.theme.model.TextKeyboard
import com.osfans.trime.ime.keyboard.KeyBehavior
import kotlin.math.abs

object KeyboardCalculator {
    private const val MAX_TOTAL_WEIGHT = 100

    fun calculate(
        keyboard: TextKeyboard,
        generalStyle: GeneralStyle,
        displayWidthPx: Int,
        density: Float,
        isLandscape: Boolean = false,
        presetKeys: Map<String, PresetKey> = emptyMap(),
        previewContext: KeyboardPreviewContext = KeyboardPreviewContext(),
    ): List<CalculatedKey> {
        val horizontalGap =
            intArrayOf(
                keyboard.horizontalGap,
                generalStyle.horizontalGap,
            ).firstOrNull { it > 0 }?.let { DpConverter.dpToPx(it, density) } ?: 0

        val keyHeight =
            intArrayOf(
                keyboard.height.toInt(),
                generalStyle.keyHeight,
            ).firstOrNull { it > 0 } ?: 0

        val verticalGap =
            intArrayOf(
                keyboard.verticalGap,
                generalStyle.verticalGap,
            ).firstOrNull { it > 0 }?.let { DpConverter.dpToPx(it, density) } ?: 0

        val roundCorner =
            keyboard.roundCorner.takeIf { it >= 0f } ?: generalStyle.roundCorner

        val keyBorder =
            keyboard.keyBorder.takeIf { it >= 0 } ?: generalStyle.keyBorder

        val padding = if (isLandscape) generalStyle.keyboardPaddingLand else generalStyle.keyboardPadding
        val allowedWidth = displayWidthPx - 2 * DpConverter.dpToPx(padding, density)

        val keyboardKeyWidth =
            keyboard.width.takeIf { it > 0f }
                ?: generalStyle.keyWidth.takeIf { it > 0f }
                ?: 0f

        val maxColumns = if (keyboard.columns == -1) Int.MAX_VALUE else keyboard.columns

        val landscapePercent = if (isLandscape) keyboard.landscapeSplitPercent else 0
        val isSplit = isLandscape && landscapePercent > 0
        val splitRatio = if (isSplit) landscapePercent / 100f else 0f

        val oneWeightWidthPx = allowedWidth.toFloat() / (MAX_TOTAL_WEIGHT * (1 + splitRatio))

        val keyboardHeight = resolveKeyboardHeight(keyboard, generalStyle, density, isLandscape)

        val rowWidthTotalWeight = mutableListOf<Float>()
        val rowRawHeight = mutableListOf<Int>()

        var x = 0
        var column = 0
        var rowHeight = keyHeight
        var totalKeyWidth = 0f

        for (key in keyboard.keys) {
            val keyWidthWeight =
                if (key.width == 0f && key.click.isNotEmpty()) keyboardKeyWidth else key.width
            val widthPx = (keyWidthWeight * allowedWidth / MAX_TOTAL_WEIGHT).toInt()

            if (column >= maxColumns || x + widthPx > allowedWidth) {
                rowWidthTotalWeight.add(totalKeyWidth)
                rowRawHeight.add(rowHeight)
                x = 0
                column = 0
                totalKeyWidth = 0f
            }

            if (column == 0) {
                rowHeight = if (key.height > 0) key.height.toInt() else keyHeight
            }

            totalKeyWidth += keyWidthWeight

            if (key.click.isNotEmpty()) {
                column++
            }

            x += widthPx
        }

        rowWidthTotalWeight.add(totalKeyWidth)
        rowRawHeight.add(rowHeight)

        val rows = rowRawHeight.size
        val rawHeightSum = rowRawHeight.sum()

        val rowHeightScaled = MutableList(rows) { 0 }

        if (keyboardHeight > 0 && rawHeightSum > 0) {
            var remainHeight = keyboardHeight
            val scale = keyboardHeight.toFloat() / rawHeightSum
            for (i in 0 until rows - 1) {
                val h = (rowRawHeight[i] * scale).toInt()
                rowHeightScaled[i] = h
                remainHeight -= h
            }
            rowHeightScaled[rows - 1] = remainHeight
        } else {
            for (i in 0 until rows) {
                rowHeightScaled[i] = DpConverter.dpToPx(rowRawHeight[i], density)
            }
        }

        val result = mutableListOf<CalculatedKey>()

        var xPos = 0
        var yPos = 0
        var row = 0
        column = 0
        var rowWeightAccumulo = 0f
        var currentRowHeight = rowHeightScaled[0]
        var splitInserted = false
        var minWidth = 0

        for (textKey in keyboard.keys) {
            val keyWidthWeight =
                if (textKey.width == 0f && textKey.click.isNotEmpty()) keyboardKeyWidth else textKey.width
            var widthPx = (keyWidthWeight * oneWeightWidthPx).toInt()

            if (column >= maxColumns || xPos + widthPx > allowedWidth) {
                xPos = 0
                yPos += currentRowHeight
                row++
                column = 0
                rowWeightAccumulo = 0f
                splitInserted = false
                currentRowHeight = rowHeightScaled[row]
            }

            rowWeightAccumulo += keyWidthWeight
            val totalWeight = rowWidthTotalWeight[row]

            if (isSplit && !splitInserted && rowWeightAccumulo > totalWeight * 0.5f) {
                splitInserted = true
                val gap = (totalWeight * splitRatio * oneWeightWidthPx).toInt()
                if (keyWidthWeight > 20f) {
                    widthPx += gap
                } else {
                    xPos += gap
                }
            }

            if (textKey.click.isEmpty()) {
                xPos += widthPx
                continue
            }

            val rightGap = abs(allowedWidth - xPos - widthPx)
            val finalWidth = if (rightGap <= allowedWidth / 100) allowedWidth - xPos else widthPx

            val keyRoundCorner = textKey.roundCorner.takeIf { it >= 0f } ?: roundCorner
            val keyBorderVal = textKey.keyBorder.takeIf { it >= 0 } ?: keyBorder

            result.add(
                CalculatedKey(
                    x = xPos,
                    y = yPos,
                    width = finalWidth,
                    height = currentRowHeight,
                    label = resolveKeyLabel(textKey, presetKeys, previewContext),
                    labelSymbol = resolveKeySymbol(textKey, presetKeys, previewContext),
                    hint = textKey.hint,
                    keyTextSize = resolveKeyTextSize(textKey, generalStyle, previewContext, presetKeys),
                    symbolTextSize =
                    firstNonZero(
                        textKey.symbolTextSize,
                        generalStyle.symbolTextSize,
                        generalStyle.keyTextSize,
                    ),
                    keyTextOffsetX = firstNonZero(textKey.keyTextOffsetX, keyboard.keyTextOffsetX, generalStyle.keyTextOffsetX),
                    keyTextOffsetY = firstNonZero(textKey.keyTextOffsetY, keyboard.keyTextOffsetY, generalStyle.keyTextOffsetY),
                    keySymbolOffsetX = firstNonZero(textKey.keySymbolOffsetX, keyboard.keySymbolOffsetX, generalStyle.keySymbolOffsetX),
                    keySymbolOffsetY = firstNonZero(textKey.keySymbolOffsetY, keyboard.keySymbolOffsetY, generalStyle.keySymbolOffsetY),
                    keyHintOffsetX = firstNonZero(textKey.keyHintOffsetX, keyboard.keyHintOffsetX, generalStyle.keyHintOffsetX),
                    keyHintOffsetY = firstNonZero(textKey.keyHintOffsetY, keyboard.keyHintOffsetY, generalStyle.keyHintOffsetY),
                    roundCorner = keyRoundCorner,
                    keyBorder = keyBorderVal,
                    keyBackgroundRef = textKey.keyBackColor,
                    keyTextColorRef = textKey.keyTextColor,
                    keySymbolColorRef = textKey.keySymbolColor,
                    horizontalGap = horizontalGap,
                    verticalGap = verticalGap,
                    isSpacer = false,
                ),
            )

            column++
            xPos += finalWidth

            if (xPos > minWidth) {
                minWidth = xPos
            }
        }

        return result
    }

    private fun resolveKeyLabel(
        key: TextKeyboard.TextKey,
        presetKeys: Map<String, PresetKey>,
        previewContext: KeyboardPreviewContext,
    ): String {
        if (key.label.isNotEmpty()) return key.label

        return resolveActionLabel(key.click, presetKeys, previewContext)
    }

    private fun resolveKeySymbol(
        key: TextKeyboard.TextKey,
        presetKeys: Map<String, PresetKey>,
        previewContext: KeyboardPreviewContext,
    ): String {
        if (key.labelSymbol.isNotEmpty()) return key.labelSymbol

        val longClickAction = key.behaviors[KeyBehavior.LONG_CLICK] ?: return ""
        return resolveActionLabel(longClickAction, presetKeys, previewContext)
    }

    private fun resolveKeyTextSize(
        key: TextKeyboard.TextKey,
        generalStyle: GeneralStyle,
        previewContext: KeyboardPreviewContext,
        presetKeys: Map<String, PresetKey>,
    ): Float {
        if (key.keyTextSize > 0f) return key.keyTextSize

        val resolvedLabel = resolveKeyLabel(key, presetKeys, previewContext)
        val keyTextSize = generalStyle.keyTextSize
        val keyLongTextSize = generalStyle.keyLongTextSize.takeIf { it > 0f } ?: keyTextSize
        return if (resolvedLabel.length > 1) keyLongTextSize else keyTextSize
    }

    private fun resolveActionLabel(
        action: String,
        presetKeys: Map<String, PresetKey>,
        previewContext: KeyboardPreviewContext,
    ): String {
        if (action.isBlank()) return ""

        val preset = presetKeys[action]
        val presetLabel = preset?.label?.trim().orEmpty()

        return when {
            isSpaceAction(action, preset) && presetLabel.isEmpty() -> previewContext.spaceKeyLabel
            presetLabel == "enter_labels" -> previewContext.enterKeyLabel
            presetLabel.isNotEmpty() -> if (presetLabel.startsWith("ic@")) "" else presetLabel
            isEnterAction(action, preset) -> previewContext.enterKeyLabel
            else -> action.takeIf(::isRenderableLiteralLabel) ?: ""
        }
    }

    private fun isSpaceAction(
        action: String,
        preset: PresetKey?,
    ): Boolean = action.equals("space", ignoreCase = true) || preset?.send.equals("space", ignoreCase = true)

    private fun isEnterAction(
        action: String,
        preset: PresetKey?,
    ): Boolean = action.equals("Return", ignoreCase = true) || preset?.send.equals("Return", ignoreCase = true)

    private fun firstNonZero(vararg values: Float): Float = values.firstOrNull { it != 0f } ?: 0f

    private fun isRenderableLiteralLabel(value: String): Boolean {
        if (value.isBlank()) return false
        if (value.any { it == '\n' || it == '\r' }) return false

        val codePointCount = value.codePointCount(0, value.length)
        return codePointCount <= 4
    }

    private fun resolveKeyboardHeight(
        keyboard: TextKeyboard,
        generalStyle: GeneralStyle,
        density: Float,
        isLandscape: Boolean,
    ): Int {
        val fromKeyboard = run {
            var h = keyboard.keyboardHeight
            if (isLandscape && keyboard.keyboardHeightLand > 0) {
                h = keyboard.keyboardHeightLand
            }
            DpConverter.dpToPx(h, density)
        }

        val fromTheme = run {
            var h = generalStyle.keyboardHeight
            if (isLandscape && generalStyle.keyboardHeightLand > 0) {
                h = generalStyle.keyboardHeightLand
            }
            DpConverter.dpToPx(h, density)
        }

        return intArrayOf(fromKeyboard, fromTheme).firstOrNull { it > 0 } ?: 0
    }
}

data class CalculatedKey(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val label: String,
    val labelSymbol: String,
    val hint: String,
    val keyTextSize: Float,
    val symbolTextSize: Float,
    val keyTextOffsetX: Float,
    val keyTextOffsetY: Float,
    val keySymbolOffsetX: Float,
    val keySymbolOffsetY: Float,
    val keyHintOffsetX: Float,
    val keyHintOffsetY: Float,
    val roundCorner: Float,
    val keyBorder: Int,
    val keyBackgroundRef: String = "",
    val keyTextColorRef: String = "",
    val keySymbolColorRef: String = "",
    val horizontalGap: Int = 0,
    val verticalGap: Int = 0,
    val isSpacer: Boolean,
)
