// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.keyboard

import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class KeyboardCalculatorTest : BehaviorSpec({
    given("KeyboardCalculator") {
        `when`("calculating layout for a keyboard with 3 keys") {
            val theme = ThemeFilesManager.parseThemeFromString(
                """
                style:
                  key_height: 40
                  keyboard_height: 200
                preset_color_schemes:
                  default:
                    back_color: "#000000"
                preset_keyboards:
                  default:
                    width: 33
                    keys:
                      - { click: a, label: A, width: 33 }
                      - { click: b, label: B, width: 33 }
                      - { click: c, label: C, width: 34 }
                """.trimIndent(),
            )
            val keyboard = theme.presetKeyboards["default"]!!
            val keys = KeyboardCalculator.calculate(
                keyboard = keyboard,
                generalStyle = theme.generalStyle,
                displayWidthPx = 1080,
                density = 2.75f,
            )

            then("returns 3 keys") {
                keys shouldHaveSize 3
            }

            then("first key starts at x=0") {
                keys[0].x shouldBe 0
            }

            then("all keys have positive width and height") {
                keys.forEach { key ->
                    key.width shouldBeGreaterThan 0
                    key.height shouldBeGreaterThan 0
                }
            }

            then("keys are placed left to right in same row (y=0)") {
                keys[0].y shouldBe 0
                keys[1].y shouldBe 0
                keys[2].y shouldBe 0
                keys[1].x shouldBeGreaterThan keys[0].x
                keys[2].x shouldBeGreaterThan keys[1].x
            }
        }

        `when`("calculating layout with multi-row keyboard") {
            val theme = ThemeFilesManager.parseThemeFromString(
                """
                style:
                  key_height: 40
                  keyboard_height: 200
                preset_color_schemes:
                  default:
                    back_color: "#000000"
                preset_keyboards:
                  default:
                    width: 50
                    keys:
                      - { click: a, label: A, width: 50 }
                      - { click: b, label: B, width: 50 }
                      - { click: c, label: C, width: 50 }
                      - { click: d, label: D, width: 50 }
                """.trimIndent(),
            )
            val keyboard = theme.presetKeyboards["default"]!!
            val keys = KeyboardCalculator.calculate(
                keyboard = keyboard,
                generalStyle = theme.generalStyle,
                displayWidthPx = 1080,
                density = 2.75f,
            )

            then("places keys in 2 rows") {
                keys shouldHaveSize 4
                keys[0].y shouldBe 0
                keys[1].y shouldBe 0
                keys[2].y shouldBeGreaterThan 0
                keys[3].y shouldBe keys[2].y
            }
        }

        `when`("keyboard has spacer key (empty click)") {
            val theme = ThemeFilesManager.parseThemeFromString(
                """
                style:
                  key_height: 40
                  keyboard_height: 200
                preset_color_schemes:
                  default:
                    back_color: "#000000"
                preset_keyboards:
                  default:
                    width: 30
                    keys:
                      - { click: a, label: A, width: 30 }
                      - { width: 10 }
                      - { click: b, label: B, width: 30 }
                """.trimIndent(),
            )
            val keyboard = theme.presetKeyboards["default"]!!
            val keys = KeyboardCalculator.calculate(
                keyboard = keyboard,
                generalStyle = theme.generalStyle,
                displayWidthPx = 1080,
                density = 2.75f,
            )

            then("spacer is excluded from result but offsets subsequent keys") {
                keys shouldHaveSize 2
                keys[1].x shouldBeGreaterThan keys[0].x + keys[0].width
            }
        }

        `when`("keyboard width is only available from general style and labels must be inferred") {
            val theme = ThemeFilesManager.parseThemeFromString(
                """
                style:
                  key_height: 40
                  key_width: 25
                  keyboard_height: 200
                preset_color_schemes:
                  default:
                    back_color: "#000000"
                preset_keys:
                  back:
                    label: 返回
                preset_keyboards:
                  default:
                    keys:
                      - { click: back }
                      - { click: "。" }
                      - { click: a }
                """.trimIndent(),
            )
            val keyboard = theme.presetKeyboards["default"]!!
            val keys = KeyboardCalculator.calculate(
                keyboard = keyboard,
                generalStyle = theme.generalStyle,
                displayWidthPx = 1080,
                density = 2.75f,
                presetKeys = theme.presetKeys,
            )

            then("falls back to style.key_width so keys remain visible") {
                keys shouldHaveSize 3
                keys.forEach { key ->
                    key.width shouldBeGreaterThan 0
                }
            }

            then("resolves labels from preset_keys before falling back to click text") {
                keys[0].label shouldBe "返回"
                keys[1].label shouldBe "。"
                keys[2].label shouldBe "a"
            }

            then("keeps key-level rendering metadata for the renderer") {
                keys[0].horizontalGap shouldBe 0
                keys[0].verticalGap shouldBe 0
            }
        }

        `when`("preview labels and text metrics need runtime-style fallbacks") {
            val theme = ThemeFilesManager.parseThemeFromString(
                """
                style:
                  key_height: 40
                  key_width: 30
                  key_text_size: 20
                  key_long_text_size: 18
                  symbol_text_size: 12
                  key_text_offset_x: 1
                  key_hint_offset_y: -0.5
                  keyboard_height: 200
                preset_color_schemes:
                  default:
                    back_color: "#000000"
                preset_keys:
                  space:
                    send: space
                  Return:
                    label: enter_labels
                    send: Return
                  Escape:
                    label: Esc
                    send: Escape
                preset_keyboards:
                  default:
                    key_text_offset_y: 2
                    key_symbol_offset_x: 3
                    keys:
                      - { click: space, width: 40 }
                      - { click: Return, width: 30 }
                      - { click: a, long_click: Escape, hint: "/", width: 30, key_text_size: 22, symbol_text_size: 14, key_text_offset_x: 4 }
                """.trimIndent(),
            )
            val keyboard = theme.presetKeyboards["default"]!!
            val keys = KeyboardCalculator.calculate(
                keyboard = keyboard,
                generalStyle = theme.generalStyle,
                displayWidthPx = 1080,
                density = 2.75f,
                presetKeys = theme.presetKeys,
                previewContext = KeyboardPreviewContext(spaceKeyLabel = "万象拼音PRO", enterKeyLabel = "Return"),
            )

            then("space and enter use preview labels instead of staying blank") {
                keys[0].label shouldBe "万象拼音PRO"
                keys[1].label shouldBe "Return"
            }

            then("long click falls back to the top symbol label") {
                keys[2].label shouldBe "a"
                keys[2].labelSymbol shouldBe "Esc"
                keys[2].hint shouldBe "/"
            }

            then("text sizes and offsets follow key > keyboard > style precedence") {
                keys[0].keyTextSize shouldBe 18f
                keys[0].keyTextOffsetY shouldBe 2f
                keys[2].keyTextSize shouldBe 22f
                keys[2].symbolTextSize shouldBe 14f
                keys[2].keyTextOffsetX shouldBe 4f
                keys[2].keySymbolOffsetX shouldBe 3f
                keys[2].keyHintOffsetY shouldBe -0.5f
            }
        }
    }
})
