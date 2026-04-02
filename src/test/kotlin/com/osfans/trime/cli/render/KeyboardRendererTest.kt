// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.render

import com.osfans.trime.cli.keyboard.KeyboardCalculator
import io.kotest.matchers.ints.shouldBeGreaterThan
import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.nio.file.Files

class KeyboardRendererTest : BehaviorSpec({
    val minimalYaml =
        """
        style:
          key_height: 40
          key_width: 10
        preset_color_schemes:
          default:
            back_color: "#1A1A1A"
            key_back_color: "#333333"
            key_text_color: "#FFFFFF"
        preset_keyboards:
          default:
            keys:
              - {click: a, width: 10, label: A}
              - {click: b, width: 10, label: B}
              - {click: c, width: 10, label: C}
        """.trimIndent()

    fun parseTheme(yaml: String) = ThemeFilesManager.parseThemeFromString(yaml)

    given("KeyboardRenderer") {
        `when`("rendering a simple 3-key keyboard at 1080px width") {
            then("produces a PNG image with correct dimensions") {
                val theme = parseTheme(minimalYaml)
                val colorManager = CliColorManager(theme)
                val fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir")))
                val renderer = KeyboardRenderer(colorManager, fontManager, density = 2.75f)

                val keyboard = theme.presetKeyboards["default"]!!
                val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 1080, 2.75f)
                val keyboardHeight = keys.maxOf { it.y + it.height }

                val image = renderer.render(keys, 1080, keyboardHeight)

                image.width shouldBe 1080
                image.height shouldBe keyboardHeight
                image.height shouldNotBe 0
            }
        }

        `when`("exporting keyboard to PNG file") {
            then("creates a valid PNG file") {
                val theme = parseTheme(minimalYaml)
                val colorManager = CliColorManager(theme)
                val fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir")))
                val renderer = KeyboardRenderer(colorManager, fontManager, density = 2.75f)

                val keyboard = theme.presetKeyboards["default"]!!
                val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 1080, 2.75f)
                val keyboardHeight = keys.maxOf { it.y + it.height }
                val image = renderer.render(keys, 1080, keyboardHeight)

                val tempDir = Files.createTempDirectory("trime-render-test").toFile()
                try {
                    val outputFile = File(tempDir, "test_keyboard.png")
                    renderer.exportToPng(image, outputFile)

                    outputFile.exists() shouldBe true
                    outputFile.length() shouldNotBe 0L

                    // PNG magic bytes: 89 50 4E 47
                    val magic = outputFile.readBytes().take(4)
                    magic[1] shouldBe 0x50.toByte()
                    magic[2] shouldBe 0x4E.toByte()
                    magic[3] shouldBe 0x47.toByte()
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("rendering keyboard with spacer keys") {
            then("spacer keys are not rendered without crashing") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - {click: a, width: 10, label: A}
                          - {width: 10}
                          - {click: b, width: 10, label: B}
                    """.trimIndent()
                val theme = parseTheme(yaml)
                val colorManager = CliColorManager(theme)
                val fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir")))
                val renderer = KeyboardRenderer(colorManager, fontManager)

                val keyboard = theme.presetKeyboards["default"]!!
                val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 1080, 2.75f)
                val keyboardHeight = keys.maxOf { it.y + it.height }.coerceAtLeast(100)

                val image = renderer.render(keys, 1080, keyboardHeight)
                image shouldNotBe null
                image.width shouldBe 1080
            }
        }

        `when`("rendering a key with top symbol and bottom hint") {
            then("draws auxiliary text in both the top and bottom areas") {
                val theme = parseTheme(
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#DDDDDD"
                        key_back_color: "#FFFFFF"
                        key_text_color: "#000000"
                        key_symbol_color: "#555555"
                    preset_keyboards:
                      default:
                        keys:
                          - {click: a, label: A}
                    """.trimIndent(),
                )
                val colorManager = CliColorManager(theme)
                val fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir")))
                val renderer = KeyboardRenderer(colorManager, fontManager, density = 2.75f)

                val image =
                    renderer.render(
                        keys =
                        listOf(
                            com.osfans.trime.cli.keyboard.CalculatedKey(
                                x = 0,
                                y = 0,
                                width = 200,
                                height = 100,
                                label = "A",
                                labelSymbol = "1",
                                hint = "/",
                                keyTextSize = 22f,
                                symbolTextSize = 14f,
                                keyTextOffsetX = 0f,
                                keyTextOffsetY = 0f,
                                keySymbolOffsetX = 0f,
                                keySymbolOffsetY = 0f,
                                keyHintOffsetX = 0f,
                                keyHintOffsetY = 0f,
                                roundCorner = 0f,
                                keyBorder = 0,
                                horizontalGap = 2,
                                verticalGap = 2,
                                isSpacer = false,
                            ),
                        ),
                        width = 200,
                        height = 100,
                        backgroundColor = 0xFFDDDDDD.toInt(),
                    )

                fun countInk(yStart: Int, yEnd: Int): Int {
                    var count = 0
                    for (y in yStart until yEnd) {
                        for (x in 20 until 180) {
                            val pixel = image.getRGB(x, y)
                            if (pixel != 0xFFFFFFFF.toInt() && pixel != 0xFFDDDDDD.toInt()) {
                                count++
                            }
                        }
                    }
                    return count
                }

                countInk(4, 24) shouldBeGreaterThan 0
                countInk(76, 96) shouldBeGreaterThan 0
            }
        }
    }
})
