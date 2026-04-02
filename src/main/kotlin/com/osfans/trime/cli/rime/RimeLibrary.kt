// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.rime

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import java.io.File
import java.util.logging.Logger

/**
 * JNA interface for the minimal librime C API needed for config deployment.
 * Only exposes functions needed for deploy_config_file — no input processing.
 */
interface RimeLibraryApi : Library {
    fun rime_get_api(): Pointer?
}

/**
 * Detects and loads the librime native library on macOS and Linux.
 */
object RimeLibrary {
    internal enum class HostPlatform {
        MACOS,
        LINUX,
        OTHER,
    }

    private val logger = Logger.getLogger("trime-cli")
    private const val LINUX_LIBRARY_NAME = "rime"

    private val MACOS_KNOWN_PATHS = listOf(
        "/opt/homebrew/opt/librime/lib/librime.dylib",
        "/usr/local/opt/librime/lib/librime.dylib",
        "/opt/homebrew/lib/librime.dylib",
        "/usr/local/lib/librime.dylib",
    )

    private val LINUX_KNOWN_PATHS = listOf(
        "/usr/lib/x86_64-linux-gnu/librime.so",
        "/usr/lib/x86_64-linux-gnu/librime.so.1",
        "/usr/lib/aarch64-linux-gnu/librime.so",
        "/usr/lib/aarch64-linux-gnu/librime.so.1",
        "/usr/lib/arm-linux-gnueabihf/librime.so",
        "/usr/lib/arm-linux-gnueabihf/librime.so.1",
        "/usr/local/lib/librime.so",
        "/usr/local/lib/librime.so.1",
        "/usr/lib64/librime.so",
        "/usr/lib64/librime.so.1",
        "/usr/lib/librime.so",
        "/usr/lib/librime.so.1",
        "/home/linuxbrew/.linuxbrew/opt/librime/lib/librime.so",
        "/home/linuxbrew/.linuxbrew/opt/librime/lib/librime.so.1",
        "/home/linuxbrew/.linuxbrew/lib/librime.so",
        "/home/linuxbrew/.linuxbrew/lib/librime.so.1",
        "/linuxbrew/.linuxbrew/opt/librime/lib/librime.so",
        "/linuxbrew/.linuxbrew/opt/librime/lib/librime.so.1",
        "/linuxbrew/.linuxbrew/lib/librime.so",
        "/linuxbrew/.linuxbrew/lib/librime.so.1",
    )

    internal fun detectPlatform(osName: String = System.getProperty("os.name")): HostPlatform {
        val normalized = osName.lowercase()
        return when {
            normalized.contains("mac") || normalized.contains("darwin") -> HostPlatform.MACOS
            normalized.contains("linux") -> HostPlatform.LINUX
            else -> HostPlatform.OTHER
        }
    }

    internal fun knownPaths(osName: String = System.getProperty("os.name")): List<String> =
        when (detectPlatform(osName)) {
            HostPlatform.MACOS -> MACOS_KNOWN_PATHS
            HostPlatform.LINUX -> LINUX_KNOWN_PATHS
            HostPlatform.OTHER -> emptyList()
        }

    internal fun installInstructions(osName: String = System.getProperty("os.name")): String =
        when (detectPlatform(osName)) {
            HostPlatform.MACOS ->
                "  macOS: brew install librime\n" +
                    "  Or set LIBRIME_PATH=/path/to/librime.dylib"
            HostPlatform.LINUX ->
                "  Ubuntu / Debian: sudo apt-get update && sudo apt-get install -y librime-dev\n" +
                    "  Other Linux: install librime from your package manager\n" +
                    "  Or set LIBRIME_PATH=/path/to/librime.so"
            HostPlatform.OTHER ->
                "  Install librime for your platform and set LIBRIME_PATH=/path/to/librime"
        }

    internal fun reinstallHint(osName: String = System.getProperty("os.name")): String =
        when (detectPlatform(osName)) {
            HostPlatform.MACOS -> "Try: brew reinstall librime"
            HostPlatform.LINUX -> "Try: sudo apt-get install --reinstall librime-dev"
            HostPlatform.OTHER -> "Try reinstalling librime or set LIBRIME_PATH to the native library file"
        }

    internal fun missingLibraryMessage(osName: String = System.getProperty("os.name")): String =
        "librime not found. Please install it:\n${installInstructions(osName)}"

    internal fun resolveLoadTarget(
        envPath: String? = System.getenv("LIBRIME_PATH"),
        osName: String = System.getProperty("os.name"),
        pathExists: (String) -> Boolean = { File(it).exists() },
        libraryExists: (String) -> Boolean = { libraryName ->
            runCatching {
                NativeLibrary.getInstance(libraryName).close()
            }.isSuccess
        },
    ): String? {
        envPath?.takeIf { it.isNotBlank() }?.let { candidate ->
            if (pathExists(candidate)) {
                logger.info("librime found via LIBRIME_PATH: $candidate")
                return candidate
            }
            logger.warning("LIBRIME_PATH set to '$candidate' but file does not exist")
        }

        for (path in knownPaths(osName)) {
            if (pathExists(path)) {
                logger.info("librime found at: $path")
                return path
            }
        }

        if (detectPlatform(osName) == HostPlatform.LINUX && libraryExists(LINUX_LIBRARY_NAME)) {
            logger.info("librime not found in known paths, falling back to dynamic linker lookup: $LINUX_LIBRARY_NAME")
            return LINUX_LIBRARY_NAME
        }

        return null
    }

    private fun describeLoadTarget(
        target: String,
        osName: String = System.getProperty("os.name"),
    ): String =
        if (detectPlatform(osName) == HostPlatform.LINUX && target == LINUX_LIBRARY_NAME) {
            "system library '$LINUX_LIBRARY_NAME'"
        } else {
            target
        }

    fun findLibrimePath(): String? =
        resolveLoadTarget()?.takeUnless {
            detectPlatform() == HostPlatform.LINUX && it == LINUX_LIBRARY_NAME
        }

    /**
     * Load the librime JNA interface.
     *
     * @throws IllegalStateException if librime is not found with install instructions
     */
    fun load(): RimeLibraryApi {
        val osName = System.getProperty("os.name")
        val target =
            resolveLoadTarget(osName = osName)
                ?: throw IllegalStateException(missingLibraryMessage(osName))

        return try {
            Native.load(target, RimeLibraryApi::class.java)
        } catch (e: UnsatisfiedLinkError) {
            throw IllegalStateException(
                "Failed to load librime from '${describeLoadTarget(target, osName)}': ${e.message}\n" +
                    reinstallHint(osName),
                e,
            )
        }
    }

    /**
     * Check if librime is available on this system.
     */
    fun isAvailable(): Boolean = resolveLoadTarget() != null
}
