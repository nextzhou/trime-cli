/*
 * SPDX-FileCopyrightText: 2015 - 2024 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.cli.theme

import com.osfans.trime.data.theme.Theme
import com.osfans.trime.data.theme.ThemeFilesManager
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CliColorManagerTest : BehaviorSpec({
    fun parseTheme(yaml: String): Theme = ThemeFilesManager.parseThemeFromString(yaml)

    given("direct color resolution") {
        `when`("color key exists in the color scheme") {
            then("returns the correct ARGB int") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#FF0000"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                manager.getColor("back_color") shouldBe 0xFFFF0000.toInt()
            }
        }
    }

    given("fallback color resolution") {
        `when`("color key resolves through theme fallback_colors") {
            then("follows the chain and returns the target color") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    fallback_colors:
                      key_back_color: back_color
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                manager.getColor("key_back_color") shouldBe 0xFF000000.toInt()
            }
        }

        `when`("color key resolves through builtin fallback colors") {
            then("follows the builtin chain") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        text_color: "#112233"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                // candidate_text_color -> text_color (builtin fallback)
                manager.getColor("candidate_text_color") shouldBe 0xFF112233.toInt()
            }
        }

        `when`("color value in scheme is a reference to another key") {
            then("resolves the reference transitively") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        text_color: "#AABBCC"
                        my_color: text_color
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                manager.getColor("my_color") shouldBe 0xFFAABBCC.toInt()
            }
        }
    }

    given("cycle detection") {
        `when`("fallback chain has a cycle") {
            then("throws IllegalArgumentException") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    fallback_colors:
                      color_a: color_b
                      color_b: color_a
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                shouldThrow<IllegalArgumentException> {
                    manager.getColor("color_a")
                }
            }
        }
    }

    given("dark mode support") {
        `when`("isDarkMode is true and dark_scheme exists") {
            then("uses the dark color scheme") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#FFFFFF"
                        dark_scheme: dark
                      dark:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val lightManager = CliColorManager(theme, isDarkMode = false)
                val darkManager = CliColorManager(theme, isDarkMode = true)
                lightManager.getColor("back_color") shouldBe 0xFFFFFFFF.toInt()
                darkManager.getColor("back_color") shouldBe 0xFF000000.toInt()
            }
        }

        `when`("isDarkMode is true but no dark_scheme") {
            then("falls back to default scheme") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#FFFFFF"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme, isDarkMode = true)
                manager.getColor("back_color") shouldBe 0xFFFFFFFF.toInt()
            }
        }
    }

    given("getDrawable") {
        `when`("color key is resolvable") {
            then("returns a SOLID_COLOR DrawableInfo") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#FF0000"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                val drawable = manager.getDrawable("back_color")
                drawable shouldNotBe null
                drawable!!.type shouldBe DrawableInfo.Type.SOLID_COLOR
                drawable.color shouldBe 0xFFFF0000.toInt()
            }
        }

        `when`("color key is not resolvable") {
            then("returns null") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                manager.getDrawable("nonexistent_unique_key") shouldBe null
            }
        }
    }

    given("missing color key") {
        `when`("key is not found anywhere") {
            then("throws IllegalArgumentException") {
                val theme = parseTheme("""
                    style:
                      key_height: 40
                    preset_color_schemes:
                      default:
                        back_color: "#000000"
                    preset_keyboards:
                      default:
                        keys: []
                """.trimIndent())
                val manager = CliColorManager(theme)
                shouldThrow<IllegalArgumentException> {
                    manager.getColor("totally_unknown_color_key")
                }
            }
        }
    }
})
