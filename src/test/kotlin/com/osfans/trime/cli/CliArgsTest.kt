// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CliArgsTest : BehaviorSpec({
    given("CliArgs.parse") {
        `when`("given validate command with config file") {
            then("parses command and file correctly") {
                val args = CliArgs.parse(arrayOf("validate", "my.trime.yaml"))
                args.command shouldBe CliArgs.Command.VALIDATE
                args.configFile shouldNotBe null
                args.configFile!!.name shouldBe "my.trime.yaml"
                args.noRime shouldBe false
                args.format shouldBe CliArgs.OutputFormat.TEXT
            }
        }

        `when`("given --format json flag") {
            then("sets format to JSON") {
                val args = CliArgs.parse(arrayOf("validate", "--format", "json", "my.trime.yaml"))
                args.format shouldBe CliArgs.OutputFormat.JSON
            }
        }

        `when`("given --no-rime flag") {
            then("sets noRime to true") {
                val args = CliArgs.parse(arrayOf("validate", "--no-rime", "my.trime.yaml"))
                args.noRime shouldBe true
            }
        }

        `when`("given --width and --density flags") {
            then("parses numeric values correctly") {
                val args = CliArgs.parse(arrayOf("render", "--width", "1440", "--density", "3.0", "my.trime.yaml"))
                args.width shouldBe 1440
                args.density shouldBe 3.0f
            }
        }

        `when`("given -o output flag") {
            then("sets output path") {
                val args = CliArgs.parse(arrayOf("report", "my.trime.yaml", "-o", "/tmp/report.html"))
                args.outputPath shouldNotBe null
                args.outputPath!!.path shouldBe "/tmp/report.html"
            }
        }

        `when`("given --help flag") {
            then("sets showHelp to true") {
                val args = CliArgs.parse(arrayOf("--help"))
                args.showHelp shouldBe true
            }
        }

        `when`("given --version flag") {
            then("sets showVersion to true") {
                val args = CliArgs.parse(arrayOf("--version"))
                args.showVersion shouldBe true
            }
        }

        `when`("given no arguments") {
            then("command is null and showHelp is false") {
                val args = CliArgs.parse(arrayOf())
                args.command shouldBe null
                args.configFile shouldBe null
            }
        }

        `when`("given render command with landscape flag") {
            then("sets landscape to true") {
                val args = CliArgs.parse(arrayOf("render", "--landscape", "my.trime.yaml"))
                args.command shouldBe CliArgs.Command.RENDER
                args.landscape shouldBe true
            }
        }
    }
})
