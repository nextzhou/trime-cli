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

class CrashPreventionValidatorTest : BehaviorSpec({
    fun parseYaml(yaml: String) = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap

    given("CrashPreventionValidator") {
        `when`("config has no 'style' field") {
            then("produces E001 ERROR") {
                val yaml =
                    """
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                val errors = messages.filter { it.code == "E001" }
                errors shouldHaveSize 1
                errors[0].level shouldBe Level.ERROR
                errors[0].path shouldBe "style"
                errors[0].message shouldContain "style"
            }
        }

        `when`("config has a 'style' field") {
            then("produces no E001 error") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "E001" }.shouldBeEmpty()
            }
        }

        `when`("config has no 'preset_color_schemes'") {
            then("produces E002 ERROR") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                val errors = messages.filter { it.code == "E002" }
                errors shouldHaveSize 1
                errors[0].level shouldBe Level.ERROR
            }
        }

        `when`("keyboards have circular import_preset A -> B -> A") {
            then("produces E003 ERROR with cycle path") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      keyboard_a:
                        import_preset: keyboard_b
                        keys: []
                      keyboard_b:
                        import_preset: keyboard_a
                        keys: []
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                val errors = messages.filter { it.code == "E003" }
                errors shouldHaveSize 1
                errors[0].level shouldBe Level.ERROR
                errors[0].message shouldContain "keyboard_a"
                errors[0].message shouldContain "keyboard_b"
            }
        }

        `when`("keyboards have no circular import_preset") {
            then("produces no E003 error") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                      qwerty:
                        import_preset: default
                        keys: []
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "E003" }.shouldBeEmpty()
            }
        }

        `when`("fallback_colors has circular reference A -> B -> A") {
            then("produces E004 ERROR") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    fallback_colors:
                      color_a: color_b
                      color_b: color_a
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                val errors = messages.filter { it.code == "E004" }
                errors shouldHaveSize 1
                errors[0].level shouldBe Level.ERROR
                errors[0].message shouldContain "color_a"
                errors[0].message shouldContain "color_b"
            }
        }

        `when`("config is fully valid") {
            then("produces no ERROR level messages") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                    fallback_colors:
                      key_back_color: back_color
                    """.trimIndent()
                val messages = CrashPreventionValidator.validate(parseYaml(yaml))
                messages.filter { it.level == Level.ERROR }.shouldBeEmpty()
            }
        }
    }
})
