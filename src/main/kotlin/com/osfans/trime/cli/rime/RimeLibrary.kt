// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.rime

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
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
 * Detects and loads the librime native library on macOS.
 * Supports Intel (/usr/local/opt/librime) and Apple Silicon (/opt/homebrew/opt/librime).
 */
object RimeLibrary {
    private val logger = Logger.getLogger("trime-cli")

    /** Known librime dylib paths on macOS */
    private val KNOWN_PATHS = listOf(
        "/opt/homebrew/opt/librime/lib/librime.dylib",   // Apple Silicon
        "/usr/local/opt/librime/lib/librime.dylib",       // Intel Mac
        "/opt/homebrew/lib/librime.dylib",                // Alternative Homebrew
        "/usr/local/lib/librime.dylib",                   // Alternative Intel
    )

    /**
     * Find the librime dylib path.
     * Checks LIBRIME_PATH env var first, then known macOS paths.
     *
     * @return path to librime.dylib, or null if not found
     */
    fun findLibrimePath(): String? {
        // Check environment variable override first
        System.getenv("LIBRIME_PATH")?.let { envPath ->
            val file = File(envPath)
            if (file.exists()) {
                logger.info("librime found via LIBRIME_PATH: $envPath")
                return envPath
            } else {
                logger.warning("LIBRIME_PATH set to '$envPath' but file does not exist")
            }
        }

        // Check known paths
        for (path in KNOWN_PATHS) {
            if (File(path).exists()) {
                logger.info("librime found at: $path")
                return path
            }
        }

        return null
    }

    /**
     * Load the librime JNA interface.
     *
     * @throws IllegalStateException if librime is not found with install instructions
     */
    fun load(): RimeLibraryApi {
        val path = findLibrimePath()
            ?: throw IllegalStateException(
                "librime not found. Please install it:\n" +
                "  macOS (Apple Silicon): brew install librime\n" +
                "  macOS (Intel):         brew install librime\n" +
                "  Or set LIBRIME_PATH=/path/to/librime.dylib"
            )

        return try {
            Native.load(path, RimeLibraryApi::class.java)
        } catch (e: UnsatisfiedLinkError) {
            throw IllegalStateException(
                "Failed to load librime from '$path': ${e.message}\n" +
                "Try: brew reinstall librime",
                e
            )
        }
    }

    /**
     * Check if librime is available on this system.
     */
    fun isAvailable(): Boolean = findLibrimePath() != null
}
