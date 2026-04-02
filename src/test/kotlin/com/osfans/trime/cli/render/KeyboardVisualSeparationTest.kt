// SPDX-FileCopyrightText: 2015 - 2026 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.render

import com.osfans.trime.cli.keyboard.KeyboardCalculator
import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

class KeyboardVisualSeparationTest : BehaviorSpec({
    given("KeyboardRenderer visual separation") {
        `when`("two adjacent keys share the same fill color") {
            then("a fallback border still separates them when no gap is configured") {
                val theme = ThemeFilesManager.parseThemeFromString(
                    """
                    style:
                      key_height: 40
                      key_width: 50
                      keyboard_height: 80
                    preset_color_schemes:
                      default:
                        back_color: "#CCCCCC"
                        key_back_color: "#FFFFFF"
                        key_border_color: "#CCCCCC"
                        key_text_color: "#111111"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a, label: A }
                          - { click: b, label: B }
                    """.trimIndent(),
                )

                val keyboard = theme.presetKeyboards["default"]!!
                val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 200, 1.0f)
                val renderer =
                    KeyboardRenderer(
                        colorManager = CliColorManager(theme),
                        fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir"))),
                        density = 1.0f,
                    )

                val image = renderer.render(keys, 200, 80, 0xFFCCCCCC.toInt())
                val seamPixels = (98..101).map { x -> image.getRGB(x, 20) }
                seamPixels.contains(0xFFCCCCCC.toInt()) shouldBe true
                image.getRGB(50, 20) shouldBe 0xFFFFFFFF.toInt()
            }
        }

        `when`("a key overrides its background color") {
            then("the renderer uses the key-level color reference instead of the global key color") {
                val theme = ThemeFilesManager.parseThemeFromString(
                    """
                    style:
                      key_height: 40
                      key_width: 50
                      keyboard_height: 80
                    preset_color_schemes:
                      default:
                        back_color: "#CCCCCC"
                        key_back_color: "#FFFFFF"
                        off_key_back_color: "#444444"
                        key_text_color: "#111111"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a, label: A, key_back_color: off_key_back_color }
                          - { click: b, label: B }
                    """.trimIndent(),
                )

                val keyboard = theme.presetKeyboards["default"]!!
                val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 200, 1.0f)
                val renderer =
                    KeyboardRenderer(
                        colorManager = CliColorManager(theme),
                        fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir"))),
                        density = 1.0f,
                    )

                val image = renderer.render(keys, 200, 80, 0xFFCCCCCC.toInt())
                image.getRGB(50, 20) shouldBe 0xFF444444.toInt()
                image.getRGB(150, 20) shouldBe 0xFFFFFFFF.toInt()
                image.getRGB(50, 20) shouldNotBe image.getRGB(150, 20)
            }
        }
    }
})
