/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.cli.theme

import com.charleskorn.kaml.yamlMap
import com.osfans.trime.data.theme.ThemeFilesManager
import com.osfans.trime.data.theme.model.GeneralStyle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.File
import java.nio.file.Files

class CliFontManagerTest : BehaviorSpec({
    fun parseStyle(yaml: String): GeneralStyle {
        val node = ThemeFilesManager.yaml.parseToYamlNode(yaml).yamlMap
        return GeneralStyle.decode(node.get<com.charleskorn.kaml.YamlMap>("style")!!)
    }

    given("CliFontManager") {
        `when`("font file exists in font directory") {
            then("resolveFont returns the File") {
                val tempDir = Files.createTempDirectory("trime-test-fonts").toFile()
                try {
                    val testFont = File(tempDir, "TestFont.ttf")
                    testFont.createNewFile()

                    val style = parseStyle("""
                        style:
                          key_height: 40
                    """.trimIndent())
                    val manager = CliFontManager(style, tempDir)
                    manager.resolveFont("TestFont.ttf") shouldNotBe null
                    manager.resolveFont("TestFont.ttf")!!.name shouldBe "TestFont.ttf"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("font file does not exist") {
            then("resolveFont returns null") {
                val tempDir = Files.createTempDirectory("trime-test-fonts").toFile()
                try {
                    val style = parseStyle("""
                        style:
                          key_height: 40
                    """.trimIndent())
                    val manager = CliFontManager(style, tempDir)
                    manager.resolveFont("NonExistent.ttf") shouldBe null
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("getFirstAvailableFont with mixed existing/missing fonts") {
            then("returns first existing font") {
                val tempDir = Files.createTempDirectory("trime-test-fonts").toFile()
                try {
                    val existingFont = File(tempDir, "Existing.ttf")
                    existingFont.createNewFile()

                    val style = parseStyle("""
                        style:
                          key_height: 40
                    """.trimIndent())
                    val manager = CliFontManager(style, tempDir)
                    val result = manager.getFirstAvailableFont(listOf("Missing.ttf", "Existing.ttf"))
                    result shouldNotBe null
                    result!!.name shouldBe "Existing.ttf"
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }

        `when`("getFirstAvailableFont with all missing fonts") {
            then("returns null") {
                val tempDir = Files.createTempDirectory("trime-test-fonts").toFile()
                try {
                    val style = parseStyle("""
                        style:
                          key_height: 40
                    """.trimIndent())
                    val manager = CliFontManager(style, tempDir)
                    manager.getFirstAvailableFont(listOf("A.ttf", "B.ttf")) shouldBe null
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }
    }
})
