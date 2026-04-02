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

class LayoutValidatorTest : BehaviorSpec({
    fun parseYaml(yaml: String) = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap

    given("LayoutValidator L001 - invisible keyboard") {
        `when`("style.key_height is 0 and no keyboard has height") {
            then("produces L001 ERROR") {
                val yaml =
                    """
                    style:
                      key_height: 0
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                val errors = messages.filter { it.code == "L001" }
                errors shouldHaveSize 1
                errors[0].level shouldBe Level.ERROR
                errors[0].message shouldContain "无法显示"
            }
        }

        `when`("style.key_height is set to a positive value") {
            then("produces no L001 error") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L001" }.shouldBeEmpty()
            }
        }

        `when`("style.key_height is 0 but a keyboard has its own height") {
            then("produces no L001 error") {
                val yaml =
                    """
                    style:
                      key_height: 0
                    preset_keyboards:
                      default:
                        height: 40
                        keys:
                          - { click: a }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L001" }.shouldBeEmpty()
            }
        }
    }

    given("LayoutValidator L002 - empty keyboard") {
        `when`("keyboard has no keys and no import_preset") {
            then("produces L002 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      empty_kb: {}
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "L002" }
                warnings shouldHaveSize 1
                warnings[0].level shouldBe Level.WARNING
                warnings[0].path shouldContain "empty_kb"
            }
        }

        `when`("keyboard has import_preset but no keys") {
            then("produces no L002 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      imported:
                        import_preset: default
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L002" }.shouldBeEmpty()
            }
        }

        `when`("keyboard has keys") {
            then("produces no L002 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L002" }.shouldBeEmpty()
            }
        }
    }

    given("LayoutValidator L003 - row weight overflow") {
        `when`("keys rely on automatic width wrapping") {
            then("produces no L003 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                      key_width: 10
                    preset_keyboards:
                      default:
                        keys:
                          - { click: q }
                          - { click: w }
                          - { click: e }
                          - { click: r }
                          - { click: t }
                          - { click: y }
                          - { click: u }
                          - { click: i }
                          - { click: o }
                          - { click: p }
                          - { click: a }
                          - { click: s }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L003" }.shouldBeEmpty()
            }
        }

        `when`("columns and spacer widths wrap rows like the app") {
            then("produces no L003 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      default:
                        columns: 2
                        keys:
                          - { click: a, width: 40 }
                          - { width: 20 }
                          - { click: b, width: 40 }
                          - { click: c, width: 40 }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L003" }.shouldBeEmpty()
            }
        }

        `when`("a single key is wider than the keyboard") {
            then("still produces L003 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      default:
                        keys:
                          - { click: a, width: 120 }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "L003" }
                warnings shouldHaveSize 1
                warnings[0].message shouldContain "120"
            }
        }
    }

    given("LayoutValidator L004 - missing key actions") {
        `when`("positive-width spacer keys are used for indentation") {
            then("produces no L004 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      default:
                        keys:
                          - { width: 5 }
                          - { click: a, width: 10 }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "L004" }.shouldBeEmpty()
            }
        }

        `when`("a key has visible content but no action") {
            then("still produces L004 warning") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      default:
                        keys:
                          - { label: test, width: 10 }
                    """.trimIndent()
                val messages = LayoutValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "L004" }
                warnings shouldHaveSize 1
                warnings[0].path shouldContain "keys[0]"
            }
        }
    }
})
