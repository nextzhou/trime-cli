// SPDX-FileCopyrightText: 2015 - 2026 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.render

import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File

class KeyboardImageExporterTest : BehaviorSpec({
    given("KeyboardImageExporter") {
        `when`("rendering report previews from raw YAML with keyboard-specific background colors") {
            then("uses keyboard_back_color ahead of the page back_color") {
                val theme = ThemeFilesManager.parseThemeFromString(
                    """
                    style:
                      key_height: 40
                      key_width: 50
                      keyboard_height: 120
                    preset_color_schemes:
                      default:
                        back_color: "#123456"
                        keyboard_back_color: "#654321"
                        key_back_color: "#FFFFFF"
                        key_text_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a }
                    """.trimIndent(),
                )

                val exporter =
                    KeyboardImageExporter(
                        theme = theme,
                        colorManager = CliColorManager(theme),
                        fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir"))),
                        displayWidthPx = 1080,
                        density = 2.75f,
                )

                val image = exporter.renderAll()["default"]
                image shouldNotBe null
                image!!.getRGB(image.width - 1, 10) shouldBe 0xFF654321.toInt()
                image.getRGB(100, 30) shouldBe 0xFFFFFFFF.toInt()
            }
        }

        `when`("rendering report previews without keyboard-specific background colors") {
            then("falls back to back_color instead of the renderer's black default") {
                val theme = ThemeFilesManager.parseThemeFromString(
                    """
                    style:
                      key_height: 40
                      key_width: 50
                      keyboard_height: 120
                    preset_color_schemes:
                      default:
                        back_color: "#123456"
                        key_back_color: "#FFFFFF"
                        key_text_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a }
                    """.trimIndent(),
                )

                val exporter =
                    KeyboardImageExporter(
                        theme = theme,
                        colorManager = CliColorManager(theme),
                        fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir"))),
                        displayWidthPx = 1080,
                        density = 2.75f,
                    )

                val image = exporter.renderAll()["default"]
                image shouldNotBe null
                image!!.getRGB(image.width - 1, 10) shouldBe 0xFF123456.toInt()
                image.getRGB(100, 30) shouldBe 0xFFFFFFFF.toInt()
            }
        }
    }
})
