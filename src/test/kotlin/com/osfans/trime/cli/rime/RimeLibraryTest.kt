// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.rime

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import java.nio.file.Files

class RimeLibraryTest : BehaviorSpec({
    given("RimeLibrary.detectPlatform") {
        `when`("running on macOS") {
            then("detects the macOS host platform") {
                RimeLibrary.detectPlatform("Mac OS X") shouldBe RimeLibrary.HostPlatform.MACOS
            }
        }

        `when`("running on Linux") {
            then("detects the Linux host platform") {
                RimeLibrary.detectPlatform("Linux") shouldBe RimeLibrary.HostPlatform.LINUX
            }
        }

        `when`("running on other platforms") {
            then("falls back to OTHER") {
                RimeLibrary.detectPlatform("Windows 11") shouldBe RimeLibrary.HostPlatform.OTHER
            }
        }
    }

    given("RimeLibrary.resolveLoadTarget") {
        `when`("LIBRIME_PATH points to an existing file") {
            then("prefers the explicit override on any platform") {
                val tempFile = Files.createTempFile("librime", ".so").toFile()
                try {
                    val target =
                        RimeLibrary.resolveLoadTarget(
                            envPath = tempFile.absolutePath,
                            osName = "Linux",
                        )
                    target shouldBe tempFile.absolutePath
                } finally {
                    tempFile.delete()
                }
            }
        }

        `when`("a known macOS path exists") {
            then("returns the first existing dylib path") {
                val target =
                    RimeLibrary.resolveLoadTarget(
                        envPath = null,
                        osName = "Mac OS X",
                        pathExists = { it == "/usr/local/lib/librime.dylib" },
                    )
                target shouldBe "/usr/local/lib/librime.dylib"
            }
        }

        `when`("no known Linux path exists") {
            then("does not guess availability from the dynamic linker during detection") {
                val target =
                    RimeLibrary.resolveLoadTarget(
                        envPath = null,
                        osName = "Linux",
                        pathExists = { false },
                    )
                target shouldBe null
            }
        }

        `when`("the platform is unsupported and no explicit path exists") {
            then("returns null") {
                val target =
                    RimeLibrary.resolveLoadTarget(
                        envPath = null,
                        osName = "Windows 11",
                        pathExists = { false },
                    )
                target shouldBe null
            }
        }
    }

    given("RimeLibrary.fallbackLibraryName") {
        `when`("running on Linux") {
            then("uses the generic librime soname as the final fallback") {
                RimeLibrary.fallbackLibraryName("Linux") shouldBe "rime"
            }
        }

        `when`("running on macOS") {
            then("does not use a generic dynamic-linker fallback") {
                RimeLibrary.fallbackLibraryName("Mac OS X") shouldBe null
            }
        }
    }

    given("RimeLibrary.installInstructions") {
        `when`("formatting platform-specific install help") {
            then("uses brew for macOS and apt for Linux") {
                RimeLibrary.installInstructions("Mac OS X") shouldContain "brew install librime"
                RimeLibrary.installInstructions("Linux") shouldContain "apt-get install -y librime-dev"
            }
        }
    }

    given("RimeLibrary.isAvailable") {
        `when`("called on this system") {
            then("returns a boolean without throwing") {
                val available = RimeLibrary.isAvailable()
                println("librime available: $available")
            }
        }
    }

    given("RimeLibrary.load") {
        `when`("librime is installed on this system") {
            then("loads successfully and rime_get_api returns non-null") {
                if (!RimeLibrary.isAvailable()) {
                    println("librime NOT available — skipping load test")
                    return@then
                }
                val lib = runCatching { RimeLibrary.load() }.getOrNull()
                lib shouldNotBe null
                val apiPointer = runCatching { lib!!.rime_get_api() }.getOrNull()
                apiPointer shouldNotBe null
                println("librime loaded successfully from: ${RimeLibrary.findLibrimePath() ?: "dynamic linker"}")
            }
        }
    }
})
