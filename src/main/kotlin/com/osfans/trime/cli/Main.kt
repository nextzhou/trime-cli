// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli

import com.charleskorn.kaml.yamlMap
import com.osfans.trime.cli.render.KeyboardImageExporter
import com.osfans.trime.cli.report.HtmlReportGenerator
import com.osfans.trime.cli.rime.RimeDeployer
import com.osfans.trime.cli.theme.CliColorManager
import com.osfans.trime.cli.theme.CliFontManager
import com.osfans.trime.cli.validator.ValidationEngine
import com.osfans.trime.cli.validator.ValidationFormatter
import com.osfans.trime.data.theme.ThemeFilesManager
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val cliArgs = CliArgs.parse(args)

    if (cliArgs.showVersion) {
        println("trime-cli ${CliArgs.VERSION}")
        exitProcess(0)
    }

    if (cliArgs.showHelp || cliArgs.command == null) {
        println(CliArgs.HELP_TEXT)
        exitProcess(0)
    }

    val configFile = cliArgs.configFile
    if (configFile == null) {
        System.err.println("错误：未指定配置文件。")
        System.err.println(CliArgs.HELP_TEXT)
        exitProcess(2)
    }

    if (!configFile.exists()) {
        System.err.println("错误：找不到配置文件：${configFile.absolutePath}")
        exitProcess(2)
    }

    val compiledFile: File
    val deployer: RimeDeployer?

    if (cliArgs.noRime) {
        compiledFile = configFile
        deployer = null
    } else {
        try {
            val d = RimeDeployer(cliArgs.rimeDataDir)
            compiledFile = d.deployFromPath(configFile)
            deployer = d
        } catch (e: IllegalStateException) {
            System.err.println("错误：${e.message}")
            exitProcess(3)
        }
    }

    try {
        val theme = ThemeFilesManager.parseTheme(compiledFile)
        val colorManager = CliColorManager(theme)
        val fontDir = cliArgs.fontDir ?: File(System.getProperty("java.io.tmpdir"))
        val fontManager = CliFontManager(theme.generalStyle, fontDir)

        when (cliArgs.command) {
            CliArgs.Command.VALIDATE -> {
                val report =
                    ValidationEngine.validate(
                        node = ThemeFilesManager.yaml.parseToYamlNode(compiledFile.readText()).yamlMap,
                        configFile = configFile.name,
                        fontDir = cliArgs.fontDir,
                    )
                val output =
                    when (cliArgs.format) {
                        CliArgs.OutputFormat.JSON -> ValidationFormatter.formatJson(report)
                        CliArgs.OutputFormat.TEXT -> ValidationFormatter.formatText(report)
                    }
                println(output)
                exitProcess(if (report.isValid) 0 else 1)
            }

            CliArgs.Command.RENDER -> {
                val outputDir = cliArgs.outputPath ?: File("keyboards")
                val exporter =
                    KeyboardImageExporter(
                        theme = theme,
                        colorManager = colorManager,
                        fontManager = fontManager,
                        displayWidthPx = cliArgs.width,
                        density = cliArgs.density,
                        isLandscape = cliArgs.landscape,
                )
                val files = exporter.exportAll(outputDir)
                files.forEach { println(it.absolutePath) }
                println("已将 ${files.size} 个键盘渲染到 ${outputDir.absolutePath}")
                exitProcess(0)
            }

            CliArgs.Command.REPORT -> {
                val outputFile = cliArgs.outputPath ?: File("report.html")

                val report =
                    ValidationEngine.validate(
                        node = ThemeFilesManager.yaml.parseToYamlNode(compiledFile.readText()).yamlMap,
                        configFile = configFile.name,
                        fontDir = cliArgs.fontDir,
                    )

                val exporter =
                    KeyboardImageExporter(
                        theme = theme,
                        colorManager = colorManager,
                        fontManager = fontManager,
                        displayWidthPx = cliArgs.width,
                        density = cliArgs.density,
                        isLandscape = cliArgs.landscape,
                    )
                val keyboardImages = exporter.renderAll()

                HtmlReportGenerator.generate(report, keyboardImages, outputFile)
                println("报告已生成：${outputFile.absolutePath}")
                exitProcess(0)
            }
        }
    } catch (e: Exception) {
        System.err.println("错误：${e.message}")
        exitProcess(1)
    } finally {
        deployer?.close()
    }
}
