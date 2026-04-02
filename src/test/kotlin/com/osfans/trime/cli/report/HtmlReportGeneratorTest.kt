// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.report

import com.charleskorn.kaml.yamlMap
import com.osfans.trime.cli.validator.ValidationEngine
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files

class HtmlReportGeneratorTest : BehaviorSpec({
    fun parseYaml(yaml: String) = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap

    fun makeTestImage(
        width: Int = 400,
        height: Int = 100,
    ): BufferedImage {
        val img = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.fillRect(0, 0, width, height)
        g.dispose()
        return img
    }

    given("HtmlReportGenerator") {
        `when`("generating a report with validation errors and keyboard images") {
            then("produces self-contained HTML with embedded images") {
                val yaml =
                    """
                    preset_color_schemes:
                      default:
                        back_color: "#GGGGGG"
                    """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml), "test.trime.yaml")
                val images = mapOf("default" to makeTestImage())

                val tempDir = Files.createTempDirectory("trime-html-test").toFile()
                try {
                    val outputFile = File(tempDir, "report.html")
                    HtmlReportGenerator.generate(report, images, outputFile)

                    outputFile.exists() shouldBe true
                    outputFile.length() shouldNotBe 0L

                    val html = outputFile.readText()
                    html shouldContain "<!DOCTYPE html>"
                    html shouldContain "data:image/png;base64,"
                    html shouldContain "test.trime.yaml"
                    html shouldContain "未通过"
                    html shouldContain "错误"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("generating a report with no external references") {
            then("HTML is self-contained (no external link or script src)") {
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
                          - {click: a, width: 10}
                    """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml))
                val tempDir = Files.createTempDirectory("trime-html-test").toFile()
                try {
                    val outputFile = File(tempDir, "report.html")
                    HtmlReportGenerator.generate(report, emptyMap(), outputFile)

                    val html = outputFile.readText()
                    html shouldNotContain "<link href="
                    html shouldNotContain "<script src="
                    html shouldContain "通过"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("generating a report with keyboard images") {
            then("each keyboard image is embedded as base64 PNG") {
                val yaml =
                    """
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    """.trimIndent()
                val report = ValidationEngine.validate(parseYaml(yaml))
                val images =
                    mapOf(
                        "default" to makeTestImage(1080, 200),
                        "qwerty" to makeTestImage(1080, 150),
                    )

                val tempDir = Files.createTempDirectory("trime-html-test").toFile()
                try {
                    val outputFile = File(tempDir, "report.html")
                    HtmlReportGenerator.generate(report, images, outputFile)

                    val html = outputFile.readText()
                    val base64Count = html.split("data:image/png;base64,").size - 1
                    base64Count shouldBe 2
                    html shouldContain "default"
                    html shouldContain "qwerty"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }
    }
})
