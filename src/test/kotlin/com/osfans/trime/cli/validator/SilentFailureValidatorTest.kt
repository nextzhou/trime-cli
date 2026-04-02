// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.yamlMap
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

class SilentFailureValidatorTest : BehaviorSpec({
    fun parseYaml(yaml: String) = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap

    given("SilentFailureValidator") {
        `when`("no preset_keyboards defined") {
            then("produces W001 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W001" }
                warnings shouldHaveSize 1
                warnings[0].level shouldBe Level.WARNING
            }
        }

        `when`("keyboards defined but no 'default' keyboard") {
            then("produces W002 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      qwerty:
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W002" }
                warnings shouldHaveSize 1
            }
        }

        `when`("color scheme has invalid hex color") {
            then("produces W003 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#GGGGGG"
                        text_color: "#FF0000"
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W003" }
                warnings shouldHaveSize 1
                warnings[0].message shouldContain "#GGGGGG"
                warnings[0].message shouldContain "default"
            }
        }

        `when`("import_preset references non-existent keyboard") {
            then("produces W004 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      main:
                        import_preset: nonexistent
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W004" }
                warnings shouldHaveSize 1
                warnings[0].message shouldContain "nonexistent"
            }
        }

        `when`("font file does not exist in font directory") {
            then("produces W005 WARNING") {
                val tempDir = Files.createTempDirectory("trime-test-fonts").toFile()
                try {
                    val yaml = """
                        style:
                          key_height: 40
                          key_font:
                            - MissingFont.ttf
                        preset_color_schemes:
                          default:
                            back_color: "#000000"
                    """.trimIndent()
                    val messages = SilentFailureValidator.validate(parseYaml(yaml), fontDir = tempDir)
                    val warnings = messages.filter { it.code == "W005" }
                    warnings shouldHaveSize 1
                    warnings[0].message shouldContain "MissingFont.ttf"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("multiple keyboards are unreferenced") {
            then("produces a single aggregated W006 info") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                      orphan_a:
                        keys: []
                      orphan_b:
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val infos = messages.filter { it.code == "W006" }
                infos shouldHaveSize 1
                infos[0].path shouldBe "preset_keyboards"
                infos[0].message shouldContain "orphan_a"
                infos[0].message shouldContain "orphan_b"
            }
        }

        `when`("keyboard uses a named action without preset_keys definition") {
            then("produces W008 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: Keyboard_number }
                          - { click: a }
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W008" }
                warnings shouldHaveSize 1
                warnings[0].path shouldBe "preset_keyboards.default.keys[0].click"
                warnings[0].message shouldContain "Keyboard_number"
            }
        }

        `when`("tool bar uses a named action without preset_keys definition") {
            then("produces W008 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    tool_bar:
                      primary_button:
                        action: clipboard_window
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W008" }
                warnings shouldHaveSize 1
                warnings[0].path shouldBe "tool_bar.primary_button.action"
                warnings[0].message shouldContain "clipboard_window"
            }
        }

        `when`("keyboard uses a built-in keypad action") {
            then("does not produce W008 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: KP_1 }
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "W008" }.shouldBeEmpty()
            }
        }

        `when`("preset key selects a non-existent keyboard") {
            then("produces W009 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keys:
                      Keyboard_number:
                        select: number
                    preset_keyboards:
                      default:
                        keys:
                          - { click: Keyboard_number }
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W009" }
                warnings shouldHaveSize 1
                warnings[0].path shouldBe "preset_keys.Keyboard_number.select"
                warnings[0].message shouldContain "number"
            }
        }

        `when`("ascii keyboard target does not exist") {
            then("produces W009 WARNING") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        ascii_keyboard: letter
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "W009" }
                warnings shouldHaveSize 1
                warnings[0].path shouldBe "preset_keyboards.default.ascii_keyboard"
                warnings[0].message shouldContain "letter"
            }
        }

        `when`("references come from select, ascii_keyboard, and __include") {
            then("only truly unreferenced keyboards remain in W006") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keys:
                      Keyboard_symbols:
                        select: symbols
                      Keyboard_bpmf:
                        select: bopomofo
                    preset_keyboards:
                      default:
                        keys: []
                      letter:
                        __include: /preset_keyboards/default
                      bopomofo:
                        ascii_keyboard: letter
                        keys:
                          - { click: a }
                      symbols:
                        keys: []
                      orphan:
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                val infos = messages.filter { it.code == "W006" }
                infos shouldHaveSize 1
                infos[0].message shouldContain "orphan"
                infos[0].message shouldNotContain "letter"
                infos[0].message shouldNotContain "bopomofo"
                infos[0].message shouldNotContain "symbols"
            }
        }

        `when`("config is fully valid") {
            then("produces no WARNING messages for these checks") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keys:
                      Keyboard_number:
                        select: number
                    preset_keyboards:
                      default:
                        keys:
                          - { click: Keyboard_number }
                      number:
                        keys: []
                      qwerty:
                        import_preset: default
                        keys: []
                """.trimIndent()
                val messages = SilentFailureValidator.validate(parseYaml(yaml))
                messages.filter {
                    it.code in listOf("W001", "W002", "W003", "W004", "W008", "W009")
                }.shouldBeEmpty()
            }
        }
    }
})
