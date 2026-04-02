// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ColorUtilsTest : BehaviorSpec({
    given("ColorUtils.parseColor") {
        `when`("given a 6-digit hex color with #") {
            then("returns fully opaque ARGB int") {
                ColorUtils.parseColor("#FF0000") shouldBe 0xFFFF0000.toInt()
                ColorUtils.parseColor("#00FF00") shouldBe 0xFF00FF00.toInt()
                ColorUtils.parseColor("#0000FF") shouldBe 0xFF0000FF.toInt()
                ColorUtils.parseColor("#FFFFFF") shouldBe 0xFFFFFFFF.toInt()
                ColorUtils.parseColor("#000000") shouldBe 0xFF000000.toInt()
            }
        }
        `when`("given an 8-digit hex color with # (AARRGGBB)") {
            then("returns ARGB int with correct alpha") {
                ColorUtils.parseColor("#80FF0000") shouldBe 0x80FF0000.toInt()
                ColorUtils.parseColor("#00000000") shouldBe 0x00000000
                ColorUtils.parseColor("#FFFFFFFF") shouldBe 0xFFFFFFFF.toInt()
            }
        }
        `when`("given a 3-digit hex color with #") {
            then("expands to 6-digit and returns fully opaque ARGB int") {
                ColorUtils.parseColor("#F00") shouldBe 0xFFFF0000.toInt()
                ColorUtils.parseColor("#0F0") shouldBe 0xFF00FF00.toInt()
                ColorUtils.parseColor("#00F") shouldBe 0xFF0000FF.toInt()
                ColorUtils.parseColor("#FFF") shouldBe 0xFFFFFFFF.toInt()
            }
        }
        `when`("given a hex color with 0x prefix") {
            then("parses as RRGGBB (fully opaque)") {
                ColorUtils.parseColor("0xFF0000") shouldBe 0xFFFF0000.toInt()
                ColorUtils.parseColor("0x0000FF") shouldBe 0xFF0000FF.toInt()
            }
        }
        `when`("given a named color") {
            then("returns correct ARGB value") {
                ColorUtils.parseColor("red") shouldBe 0xFFFF0000.toInt()
                ColorUtils.parseColor("green") shouldBe 0xFF00FF00.toInt()
                ColorUtils.parseColor("blue") shouldBe 0xFF0000FF.toInt()
                ColorUtils.parseColor("black") shouldBe 0xFF000000.toInt()
                ColorUtils.parseColor("white") shouldBe 0xFFFFFFFF.toInt()
                ColorUtils.parseColor("transparent") shouldBe 0x00000000
            }
        }
        `when`("given a named color in uppercase") {
            then("parses case-insensitively") {
                ColorUtils.parseColor("RED") shouldBe 0xFFFF0000.toInt()
                ColorUtils.parseColor("Blue") shouldBe 0xFF0000FF.toInt()
            }
        }
        `when`("given an invalid color string") {
            then("throws IllegalArgumentException") {
                shouldThrow<IllegalArgumentException> { ColorUtils.parseColor("not_a_color") }
                shouldThrow<IllegalArgumentException> { ColorUtils.parseColor("") }
                shouldThrow<IllegalArgumentException> { ColorUtils.parseColor("#GGGGGG") }
                shouldThrow<IllegalArgumentException> { ColorUtils.parseColor("#12345") }
            }
        }
    }
})
