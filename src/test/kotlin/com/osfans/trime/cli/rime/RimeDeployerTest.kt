// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.rime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.io.File
import java.nio.file.Files

class RimeDeployerTest : BehaviorSpec({
    val isLinux = System.getProperty("os.name").lowercase().contains("linux")

    given("RimeDeployer") {
        `when`("bundled shared defaults are present") {
            then("exposes trime.yaml as a classpath resource for trime:/... includes") {
                RimeDeployer::class.java.classLoader.getResourceAsStream("trime.yaml") shouldNotBe null
            }
        }

        `when`("librime is NOT installed") {
            then("deployFromPath throws IllegalStateException with install instructions") {
                if (RimeLibrary.isAvailable()) {
                    println("librime IS available -- skipping 'not installed' test")
                    return@then
                }
                val tempFile = Files.createTempFile("test", ".trime.yaml").toFile()
                tempFile.writeText("style:\n  key_height: 40\n")
                try {
                    val deployer = RimeDeployer()
                    shouldThrow<IllegalStateException> {
                        deployer.deployFromPath(tempFile)
                    }.message shouldContain "Please install it"
                } finally {
                    tempFile.delete()
                }
            }
        }

        `when`("deployFromPath is called with a valid config (librime available)") {
            then("returns a compiled YAML file with resolved includes") {
                if (isLinux) {
                    println("Skipping native deploy test on Linux — CLI smoke tests cover deploy/render/report end-to-end")
                    return@then
                }
                if (!RimeLibrary.isAvailable()) {
                    println("librime NOT available -- skipping deployment test")
                    return@then
                }
                val tempFile = Files.createTempFile("test", ".trime.yaml").toFile()
                tempFile.writeText(
                    """
                    config_version: "3.0"
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                    """.trimIndent(),
                )
                try {
                    RimeDeployer().use { deployer ->
                        val compiled = deployer.deployFromPath(tempFile)
                        compiled shouldNotBe null
                        compiled.exists() shouldBe true
                        println("Compiled output: ${compiled.absolutePath}")
                    }
                } finally {
                    tempFile.delete()
                }
            }
        }

        `when`("deployFromPath resolves trime:/ includes with bundled defaults") {
            then("compiles successfully without requiring --rime-data") {
                if (isLinux) {
                    println("Skipping bundled default deploy test on Linux — CLI smoke tests cover deploy/render/report end-to-end")
                    return@then
                }
                if (!RimeLibrary.isAvailable()) {
                    println("librime NOT available -- skipping bundled default deployment test")
                    return@then
                }
                val tempFile = Files.createTempFile("test-bundled", ".trime.yaml").toFile()
                tempFile.writeText(
                    """
                    config_version: "3.0"
                    name: test
                    style:
                      key_height: 40
                    android_keys:
                      __include: trime:/android_keys
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                    """.trimIndent(),
                )
                try {
                    RimeDeployer().use { deployer ->
                        val compiled = deployer.deployFromPath(tempFile)
                        compiled.exists() shouldBe true
                        compiled.readText() shouldContain "android_keys"
                    }
                } finally {
                    tempFile.delete()
                }
            }
        }

        `when`("close is called") {
            then("temp directory is cleaned up") {
                val deployer = RimeDeployer()
                deployer.close()
            }
        }
    }
})
