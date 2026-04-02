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

class SchemaConstraintValidatorTest : BehaviorSpec({
    fun parseYaml(yaml: String) = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap

    given("SchemaConstraintValidator") {
        `when`("config_version has invalid format") {
            then("produces S001 ERROR") {
                val yaml =
                    """
                    config_version: "not-a-version"
                    style:
                      key_height: 40
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val errors = messages.filter { it.code == "S001" }
                errors shouldHaveSize 1
                errors[0].level shouldBe Level.ERROR
                errors[0].message shouldContain "not-a-version"
            }
        }

        `when`("config_version is valid") {
            then("produces no S001 error") {
                val yaml =
                    """
                    config_version: "3.0"
                    style:
                      key_height: 40
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "S001" }.shouldBeEmpty()
            }
        }

        `when`("config_version with three segments is valid") {
            then("produces no S001 error") {
                val yaml =
                    """
                    config_version: "2.1.0"
                    style:
                      key_height: 40
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "S001" }.shouldBeEmpty()
            }
        }

        `when`("style.auto_caps has invalid value") {
            then("produces S002 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                      auto_caps: invalid
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S002" }
                warnings shouldHaveSize 1
                warnings[0].level shouldBe Level.WARNING
                warnings[0].message shouldContain "invalid"
            }
        }

        `when`("style.comment_position has invalid value") {
            then("produces S003 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                      comment_position: invalid_value
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S003" }
                warnings shouldHaveSize 1
                warnings[0].level shouldBe Level.WARNING
                warnings[0].message shouldContain "invalid_value"
            }
        }

        `when`("style.enter_label_mode is out of range") {
            then("produces S004 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                      enter_label_mode: 5
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S004" }
                warnings shouldHaveSize 1
                warnings[0].level shouldBe Level.WARNING
                warnings[0].message shouldContain "5"
            }
        }

        `when`("preedit.alpha is out of range") {
            then("produces S005 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preedit:
                      alpha: 2.5
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S005" }
                warnings shouldHaveSize 1
                warnings[0].message shouldContain "2.5"
            }
        }

        `when`("window.alpha is out of range") {
            then("produces S006 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    window:
                      alpha: -0.5
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S006" }
                warnings shouldHaveSize 1
                warnings[0].level shouldBe Level.WARNING
            }
        }

        `when`("preedit.horizontal_padding is out of range") {
            then("produces S007 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preedit:
                      horizontal_padding: 100
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S007" }
                warnings shouldHaveSize 1
                warnings[0].message shouldContain "100"
            }
        }

        `when`("preset_keyboards has key with invalid characters") {
            then("produces S008 WARNING") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_keyboards:
                      valid_key:
                        keys: []
                      "invalid-key!":
                        keys: []
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val warnings = messages.filter { it.code == "S008" }
                warnings shouldHaveSize 1
                warnings[0].message shouldContain "invalid-key!"
            }
        }

        `when`("author, android_keys, and deploy metadata are present") {
            then("produces no S010 info") {
                val yaml =
                    """
                    config_version: "3.0"
                    author: test
                    __build_info:
                      version: test
                    android_keys:
                      __include: trime:/android_keys
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                messages.filter { it.code == "S010" }.shouldBeEmpty()
            }
        }

        `when`("an unknown top-level key is present") {
            then("produces S010 info") {
                val yaml =
                    """
                    config_version: "3.0"
                    mystery_key: true
                    style:
                      key_height: 40
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                val infos = messages.filter { it.code == "S010" }
                infos shouldHaveSize 1
                infos[0].message shouldContain "mystery_key"
            }
        }

        `when`("config is fully valid") {
            then("produces no schema constraint messages") {
                val yaml =
                    """
                    config_version: "3.0"
                    style:
                      key_height: 40
                      comment_position: right
                      enter_label_mode: 0
                      auto_caps: ascii
                    preedit:
                      alpha: 0.8
                      horizontal_padding: 16
                    window:
                      alpha: 1.0
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                    preset_keys:
                      space:
                        label: " "
                    """.trimIndent()
                val messages = SchemaConstraintValidator.validate(parseYaml(yaml))
                messages.shouldBeEmpty()
            }
        }
    }
})
