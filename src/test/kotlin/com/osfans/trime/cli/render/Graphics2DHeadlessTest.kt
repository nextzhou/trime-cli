// SPDX-FileCopyrightText: 2015 - 2026 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.render

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO

class Graphics2DHeadlessTest : BehaviorSpec({
    given("Graphics2D fallback raster rendering") {
        `when`("creating a BufferedImage and drawing with Graphics2D") {
            then("can render to PNG without a display") {
                val image = BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB)
                val g = image.createGraphics()
                try {
                    g.color = Color(0, 0, 255)
                    g.fillRoundRect(10, 10, 100, 50, 10, 10)
                } finally {
                    g.dispose()
                }

                val tempFile = Files.createTempFile("g2d-test", ".png").toFile()
                try {
                    ImageIO.write(image, "PNG", tempFile)

                    tempFile.exists() shouldBe true
                    tempFile.length() shouldBeGreaterThan 0L

                    val magic = tempFile.readBytes().take(8)
                    magic[0] shouldBe 0x89.toByte()
                    magic[1] shouldBe 0x50.toByte()
                    magic[2] shouldBe 0x4E.toByte()
                    magic[3] shouldBe 0x47.toByte()
                } finally {
                    tempFile.delete()
                }
            }
        }
    }
})
