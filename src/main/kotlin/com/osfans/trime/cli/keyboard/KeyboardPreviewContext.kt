// SPDX-FileCopyrightText: 2015 - 2026 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.keyboard

import com.osfans.trime.data.theme.model.GeneralStyle

data class KeyboardPreviewContext(
    val spaceKeyLabel: String = DEFAULT_SPACE_KEY_LABEL,
    val enterKeyLabel: String = DEFAULT_ENTER_KEY_LABEL,
) {
    companion object {
        const val DEFAULT_SPACE_KEY_LABEL = "当前输入方案"
        const val DEFAULT_ENTER_KEY_LABEL = "Return"

        fun fromGeneralStyle(style: GeneralStyle): KeyboardPreviewContext {
            val themedEnterLabel = style.enterLabel.default.trim()
            val enterKeyLabel =
                themedEnterLabel
                    .takeIf { it.isNotEmpty() && it != "default" && it.codePointCount(0, it.length) > 1 }
                    ?: DEFAULT_ENTER_KEY_LABEL
            return KeyboardPreviewContext(enterKeyLabel = enterKeyLabel)
        }
    }
}
