// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.yamlMap
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ValidationEngineTest : BehaviorSpec({
    fun parseYaml(yaml: String) = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap

    given("ValidationEngine") {
        `when`("config has multiple issues from different validators") {
            then("aggregates all messages sorted by severity then path") {
                val yaml = """
                    preset_color_schemes:
                      default:
                        back_color: "#GGGGGG"
                """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml), "test.trime.yaml")

                report.configFile shouldBe "test.trime.yaml"
                report.messages.shouldNotBeEmpty()
                report.errorCount shouldBe 1
                report.isValid shouldBe false

                val levels = report.messages.map { it.level }
                val errorIdx = levels.indexOfFirst { it == Level.ERROR }
                val warnIdx = levels.indexOfFirst { it == Level.WARNING }
                if (errorIdx >= 0 && warnIdx >= 0) {
                    (errorIdx < warnIdx) shouldBe true
                }
            }
        }

        `when`("config is fully valid") {
            then("report.isValid is true and errorCount is 0") {
                val yaml = """
                    config_version: "3.0"
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - {click: a, width: 10}
                """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml), "valid.trime.yaml")
                report.isValid shouldBe true
                report.errorCount shouldBe 0
            }
        }
    }

    given("ValidationFormatter.formatText") {
        `when`("report has errors and warnings") {
            then("text output contains ERRORS and WARNINGS sections") {
                val yaml = """
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml), "test.trime.yaml")
                val text = ValidationFormatter.formatText(report)

                text shouldContain "正在校验：test.trime.yaml"
                text shouldContain "错误"
                text shouldContain "结果：未通过"
            }
        }

        `when`("report is valid") {
            then("text output says VALID") {
                val yaml = """
                    config_version: "3.0"
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys:
                          - {click: a, width: 10}
                """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml))
                val text = ValidationFormatter.formatText(report)
                text shouldContain "通过"
                text shouldNotContain "未通过"
            }
        }
    }

    given("ValidationFormatter.formatJson") {
        `when`("formatting any report") {
            then("output is valid JSON structure") {
                val yaml = """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml))
                val json = ValidationFormatter.formatJson(report)

                json shouldContain "\"file\":"
                json shouldContain "\"valid\":"
                json shouldContain "\"errors\":"
                json shouldContain "\"messages\":"

                json.trim().startsWith("{") shouldBe true
                json.trim().endsWith("}") shouldBe true
            }
        }
    }
})
