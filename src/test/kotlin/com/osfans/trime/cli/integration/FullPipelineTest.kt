// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.integration

import com.charleskorn.kaml.yamlMap
import com.osfans.trime.cli.keyboard.KeyboardCalculator
import com.osfans.trime.cli.render.KeyboardRenderer
import com.osfans.trime.cli.report.HtmlReportGenerator
import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.cli.validator.ValidationEngine
import com.osfans.trime.cli.validator.ValidationFormatter
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

class FullPipelineTest : BehaviorSpec({
    val configContent =
        FullPipelineTest::class.java.classLoader
            .getResourceAsStream("tongwenfeng.trime.yaml")
            ?.bufferedReader()
            ?.readText()
            ?: error("tongwenfeng.trime.yaml not found in test resources")

    fun parseTheme() = ThemeFilesManager.parseThemeFromString(configContent)

    given("Full pipeline with tongwenfeng.trime.yaml") {

        `when`("parsing the config") {
            then("Theme object is created with expected fields") {
                val theme = parseTheme()
                theme.name shouldNotBe null
                theme.generalStyle shouldNotBe null
                theme.presetKeyboards.shouldNotBeEmpty()
                theme.colorSchemes.shouldNotBeEmpty()
            }
        }

        `when`("validating the config") {
            then("tongwenfeng.trime.yaml passes validation without errors") {
                val node = ThemeFilesManager.yaml.parseToYamlNode(configContent).yamlMap
                val report = ValidationEngine.validate(node, "tongwenfeng.trime.yaml")
                report.errorCount shouldBe 0
                report.isValid shouldBe true
            }
        }

        `when`("calculating keyboard layout") {
            then("default keyboard has keys with positive dimensions") {
                val theme = parseTheme()
                val defaultKeyboard =
                    theme.presetKeyboards["default"]
                        ?: theme.presetKeyboards.values.firstOrNull()
                        ?: error("No keyboards found in tongwenfeng.trime.yaml")

                val keys =
                    KeyboardCalculator.calculate(
                        keyboard = defaultKeyboard,
                        generalStyle = theme.generalStyle,
                        displayWidthPx = 1080,
                        density = 2.75f,
                    )

                keys.shouldNotBeEmpty()
                keys[0].width shouldBeGreaterThan 0
                keys[0].height shouldBeGreaterThan 0
            }
        }

        `when`("rendering a keyboard to PNG") {
            then("produces a valid PNG file") {
                val theme = parseTheme()
                val colorManager = CliColorManager(theme)
                val fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir")))
                val renderer = KeyboardRenderer(colorManager, fontManager, density = 2.75f)

                val keyboard =
                    theme.presetKeyboards["default"]
                        ?: theme.presetKeyboards.values.first()
                val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 1080, 2.75f)
                val keyboardHeight = keys.maxOfOrNull { it.y + it.height } ?: 200

                val image = renderer.render(keys, 1080, keyboardHeight)
                image.width shouldBe 1080
                image.height shouldBe keyboardHeight

                val tempDir = Files.createTempDirectory("trime-integration-test").toFile()
                try {
                    val outputFile = File(tempDir, "keyboard_test.png")
                    renderer.exportToPng(image, outputFile)
                    outputFile.exists() shouldBe true
                    outputFile.length() shouldBeGreaterThan 0L
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("generating HTML report") {
            then("produces self-contained HTML with embedded images") {
                val theme = parseTheme()
                val colorManager = CliColorManager(theme)
                val fontManager = CliFontManager(theme.generalStyle, File(System.getProperty("java.io.tmpdir")))
                val node = ThemeFilesManager.yaml.parseToYamlNode(configContent).yamlMap
                val report = ValidationEngine.validate(node, "tongwenfeng.trime.yaml")

                val keyboardImages =
                    theme.presetKeyboards.entries.take(2).associate { (name, keyboard) ->
                        val renderer = KeyboardRenderer(colorManager, fontManager, 2.75f)
                        val keys = KeyboardCalculator.calculate(keyboard, theme.generalStyle, 1080, 2.75f)
                        val keyboardHeight = keys.maxOfOrNull { it.y + it.height } ?: 200
                        name to renderer.render(keys, 1080, keyboardHeight)
                    }

                val tempDir = Files.createTempDirectory("trime-integration-test").toFile()
                try {
                    val outputFile = File(tempDir, "report.html")
                    HtmlReportGenerator.generate(report, keyboardImages, outputFile)

                    outputFile.exists() shouldBe true
                    val html = outputFile.readText()
                    html shouldContain "<!DOCTYPE html>"
                    html shouldContain "data:image/png;base64,"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("running validation on broken YAML") {
            then("reports errors for minimal config missing required sections") {
                val brokenYaml =
                    """
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    """.trimIndent()
                val node = ThemeFilesManager.yaml.parseToYamlNode(brokenYaml).yamlMap
                val report = ValidationEngine.validate(node, "broken.trime.yaml")

                report.isValid shouldBe false
                report.errorCount shouldBeGreaterThan 0

                val text = ValidationFormatter.formatText(report)
                text shouldContain "未通过"
                text shouldContain "错误"
            }
        }
    }
})
