// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli

import java.io.File

data class CliArgs(
    val command: Command?,
    val configFile: File?,
    val outputPath: File?,
    val width: Int = 1080,
    val height: Int = 2400,
    val density: Float = 2.75f,
    val landscape: Boolean = false,
    val format: OutputFormat = OutputFormat.TEXT,
    val colorScheme: String = "default",
    val fontDir: File? = null,
    val rimeDataDir: File? = null,
    val noRime: Boolean = false,
    val showHelp: Boolean = false,
    val showVersion: Boolean = false,
) {
    enum class Command { VALIDATE, RENDER, REPORT }

    enum class OutputFormat { TEXT, JSON }

    companion object {
        const val VERSION = "0.1.0"

        val HELP_TEXT =
            """
            Usage: trime-cli <command> [options] <config-file>

            Commands:
              validate    Validate trime.yaml configuration
              render      Render keyboard layouts as PNG images
              report      Generate HTML report with validation + previews

            Options:
              --width <px>           Screen width in pixels (default: 1080)
              --height <px>          Screen height in pixels (default: 2400)
              --density <float>      Screen density (default: 2.75)
              --landscape            Render in landscape mode
              --format <fmt>         Output format for validate: text|json (default: text)
              --color-scheme <name>  Color scheme to use (default: default)
              --font-dir <path>      Directory containing font files
              --rime-data <path>     Override bundled RIME data files with a local directory
              --no-rime              Skip RIME deployment (parse raw YAML)
              -o, --output <path>    Output file or directory
              --version              Print version and exit
              --help                 Print this help and exit

            Examples:
              trime-cli validate my.trime.yaml
              trime-cli validate --format json my.trime.yaml
              trime-cli render --width 1080 --density 2.75 my.trime.yaml -o /tmp/keyboards/
              trime-cli report my.trime.yaml -o report.html
            """.trimIndent()

        fun parse(args: Array<String>): CliArgs {
            var command: Command? = null
            var configFile: File? = null
            var outputPath: File? = null
            var width = 1080
            var height = 2400
            var density = 2.75f
            var landscape = false
            var format = OutputFormat.TEXT
            var colorScheme = "default"
            var fontDir: File? = null
            var rimeDataDir: File? = null
            var noRime = false
            var showHelp = false
            var showVersion = false

            var i = 0
            while (i < args.size) {
                when (val arg = args[i]) {
                    "validate" -> command = Command.VALIDATE
                    "render" -> command = Command.RENDER
                    "report" -> command = Command.REPORT
                    "--help", "-h" -> showHelp = true
                    "--version" -> showVersion = true
                    "--landscape" -> landscape = true
                    "--no-rime" -> noRime = true
                    "--width" -> {
                        i++; width = args.getOrNull(i)?.toIntOrNull() ?: width
                    }
                    "--height" -> {
                        i++; height = args.getOrNull(i)?.toIntOrNull() ?: height
                    }
                    "--density" -> {
                        i++; density = args.getOrNull(i)?.toFloatOrNull() ?: density
                    }
                    "--format" -> {
                        i++
                        format =
                            when (args.getOrNull(i)?.lowercase()) {
                                "json" -> OutputFormat.JSON
                                else -> OutputFormat.TEXT
                            }
                    }
                    "--color-scheme" -> {
                        i++; colorScheme = args.getOrNull(i) ?: colorScheme
                    }
                    "--font-dir" -> {
                        i++; args.getOrNull(i)?.let { fontDir = File(it) }
                    }
                    "--rime-data" -> {
                        i++; args.getOrNull(i)?.let { rimeDataDir = File(it) }
                    }
                    "-o", "--output" -> {
                        i++; args.getOrNull(i)?.let { outputPath = File(it) }
                    }
                    else -> {
                        if (!arg.startsWith("-") && configFile == null) {
                            configFile = File(arg)
                        }
                    }
                }
                i++
            }

            return CliArgs(
                command = command,
                configFile = configFile,
                outputPath = outputPath,
                width = width,
                height = height,
                density = density,
                landscape = landscape,
                format = format,
                colorScheme = colorScheme,
                fontDir = fontDir,
                rimeDataDir = rimeDataDir,
                noRime = noRime,
                showHelp = showHelp,
                showVersion = showVersion,
            )
        }
    }
}
