// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.rime

import com.sun.jna.Function
import com.sun.jna.Pointer
import com.sun.jna.Structure
import java.io.File
import java.nio.file.Files
import java.util.logging.Logger

/**
 * Deploys a trime.yaml config file using the RIME engine.
 * Handles __include and __patch directive resolution.
 *
 * Only exposes deploy_config_file -- no input processing.
 */
class RimeDeployer(
    private val rimeDataDir: File? = null,
) : AutoCloseable {
    private val logger = Logger.getLogger("trime-cli")
    private var tempDir: File? = null

    /**
     * Deploy a trime.yaml config file and return the compiled output
     * with __include/__patch directives resolved by the RIME engine.
     *
     * @param configFilePath path to the .trime.yaml file to deploy
     * @return compiled YAML file in a temp staging directory
     * @throws IllegalStateException if librime is not installed
     */
    fun deployFromPath(configFilePath: File): File {
        if (!RimeLibrary.isAvailable()) {
            throw IllegalStateException(
                RimeLibrary.missingLibraryMessage() +
                    "\n\n" +
                    "Alternatively, use --no-rime flag to skip RIME preprocessing.",
            )
        }

        val staging = Files.createTempDirectory("trime-cli-deploy").toFile()
        tempDir = staging

        try {
            val configDest = File(staging, configFilePath.name)
            configFilePath.copyTo(configDest, overwrite = true)

            copyRimePreludeData(staging)

            // Keep ".trime" in the config id to match app-side deployment.
            // Example: "tongwenfeng.trime.yaml" -> "tongwenfeng.trime"
            val configId = configFilePath.nameWithoutExtension

            deployWithRime(staging, configId)

            val compiled = File(staging, "build/$configId.yaml")
            if (!compiled.exists()) {
                val alt = File(staging, "build/${configFilePath.nameWithoutExtension}.yaml")
                if (alt.exists()) return alt
                throw IllegalStateException(
                    "RIME deployment completed but compiled output not found at: ${compiled.absolutePath}",
                )
            }
            return compiled
        } catch (e: Exception) {
            staging.deleteRecursively()
            tempDir = null
            throw e
        }
    }

    private fun copyRimePreludeData(targetDir: File) {
        var copiedAny = false
        copiedAny = copyBundledFiles(targetDir, PRELUDE_FILES) || copiedAny
        copiedAny = copyBundledFiles(targetDir, SHARED_DEFAULT_FILES) || copiedAny

        if (rimeDataDir != null) {
            if (rimeDataDir.exists()) {
                logger.info("Using RIME data directory from --rime-data: ${rimeDataDir.absolutePath}")
                copyDirectoryContents(rimeDataDir, targetDir)
                copiedAny = true
            } else {
                logger.warning("Configured --rime-data directory does not exist: ${rimeDataDir.absolutePath}")
            }
        }

        if (!copiedAny) {
            logger.warning(
                "No bundled RIME data or --rime-data directory was available. " +
                    "__include directives may not resolve correctly.",
            )
        }
    }

    private fun copyBundledFiles(
        targetDir: File,
        fileNames: List<String>,
    ): Boolean {
        var copiedAny = false
        for (fileName in fileNames) {
            val resource = javaClass.classLoader.getResourceAsStream(fileName)
            if (resource != null) {
                resource.use { input ->
                    File(targetDir, fileName).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                copiedAny = true
            }
        }
        return copiedAny
    }

    private fun copyDirectoryContents(
        sourceDir: File,
        targetDir: File,
    ) {
        sourceDir.walkTopDown().forEach { source ->
            if (source == sourceDir) return@forEach

            val relative = source.relativeTo(sourceDir)
            val target = File(targetDir, relative.path)

            when {
                source.isDirectory -> target.mkdirs()
                source.isFile -> {
                    target.parentFile?.mkdirs()
                    source.copyTo(target, overwrite = true)
                }
            }
        }
    }

    private fun deployWithRime(
        stagingDir: File,
        configId: String,
    ) {
        val apiPointer =
            runCatching { RimeLibrary.load().rime_get_api() }
                .getOrElse { error ->
                    throw IllegalStateException(
                        "Failed to acquire librime API: ${error.message}",
                        error,
                    )
                }
                ?: throw IllegalStateException("librime returned null from rime_get_api()")
        val rimeApi = VersionedRimeApi(RimeApi(apiPointer))

        val traits = RimeTraits()
        // RIME_STRUCT_INIT: data_size = sizeof(RimeTraits) - sizeof(data_size)
        traits.dataSize = traits.size() - 4
        traits.sharedDataDir = stagingDir.absolutePath
        traits.userDataDir = stagingDir.absolutePath
        traits.distributionName = "trime-cli"
        traits.distributionCodeName = "trime-cli"
        traits.distributionVersion = "1.0"
        traits.appName = "rime.trime-cli"
        traits.minLogLevel = 3 // FATAL only -- suppress verbose RIME output
        traits.logDir = "" // empty string = stderr only (no log files)
        traits.write()

        rimeApi.setup(traits)
        rimeApi.deployerInitialize(traits)

        try {
            val success = rimeApi.deployConfigFile(configId, "config_version")
            if (!success) {
                logger.warning("RimeDeployConfigFile returned false for config: $configId")
            }
        } finally {
            rimeApi.finalizeRime()
        }
    }

    override fun close() {
        tempDir?.deleteRecursively()
        tempDir = null
    }

    companion object {
        private val PRELUDE_FILES =
            listOf(
                "default.yaml",
                "symbols.yaml",
                "punctuation.yaml",
                "key_bindings.yaml",
                "essay.txt",
            )
        private val SHARED_DEFAULT_FILES =
            listOf(
                "trime.yaml",
                "tongwenfeng.trime.yaml",
            )
    }
}

/**
 * RimeTraits structure matching the C struct layout.
 * See: rime_api.h rime_traits_t
 *
 * Field order must exactly match the C struct for JNA to marshal correctly.
 */
@Structure.FieldOrder(
    "dataSize", "sharedDataDir", "userDataDir",
    "distributionName", "distributionCodeName", "distributionVersion",
    "appName", "modules", "minLogLevel", "logDir",
    "prebuiltDataDir", "stagingDir",
)
class RimeTraits : Structure() {
    @JvmField var dataSize: Int = 0

    @JvmField var sharedDataDir: String? = null

    @JvmField var userDataDir: String? = null

    @JvmField var distributionName: String? = null

    @JvmField var distributionCodeName: String? = null

    @JvmField var distributionVersion: String? = null

    @JvmField var appName: String? = null

    @JvmField var modules: Pointer? = null

    @JvmField var minLogLevel: Int = 0

    @JvmField var logDir: String? = null

    @JvmField var prebuiltDataDir: String? = null

    @JvmField var stagingDir: String? = null
}

/**
 * Leading portion of RimeApi in rime_api.h.
 * We only map fields up to deploy_config_file because that's all the CLI needs.
 */
@Structure.FieldOrder(
    "dataSize",
    "setup",
    "setNotificationHandler",
    "initialize",
    "finalize",
    "startMaintenance",
    "isMaintenanceMode",
    "joinMaintenanceThread",
    "deployerInitialize",
    "prebuild",
    "deploy",
    "deploySchema",
    "deployConfigFile",
    "syncUserData",
)
class RimeApi() : Structure() {
    constructor(pointer: Pointer) : this() {
        useMemory(pointer)
        read()
    }

    @JvmField var dataSize: Int = 0

    @JvmField var setup: Pointer? = null

    @JvmField var setNotificationHandler: Pointer? = null

    @JvmField var initialize: Pointer? = null

    @JvmField var finalize: Pointer? = null

    @JvmField var startMaintenance: Pointer? = null

    @JvmField var isMaintenanceMode: Pointer? = null

    @JvmField var joinMaintenanceThread: Pointer? = null

    @JvmField var deployerInitialize: Pointer? = null

    @JvmField var prebuild: Pointer? = null

    @JvmField var deploy: Pointer? = null

    @JvmField var deploySchema: Pointer? = null

    @JvmField var deployConfigFile: Pointer? = null

    @JvmField var syncUserData: Pointer? = null
}

private class VersionedRimeApi(
    private val api: RimeApi,
) {
    fun setup(traits: RimeTraits) {
        invokeVoid("setup", api.setup, traits)
    }

    fun initialize(traits: RimeTraits) {
        invokeVoid("initialize", api.initialize, traits)
    }

    fun deployerInitialize(traits: RimeTraits) {
        invokeVoid("deployer_initialize", api.deployerInitialize, traits)
    }

    fun finalizeRime() {
        invokeVoid("finalize", api.finalize)
    }

    fun deployConfigFile(
        fileName: String,
        versionKey: String,
    ): Boolean = invokeInt("deploy_config_file", api.deployConfigFile, fileName, versionKey) != 0

    private fun invokeVoid(
        name: String,
        functionPointer: Pointer?,
        vararg args: Any?,
    ) {
        function(name, functionPointer).invokeVoid(args)
    }

    private fun invokeInt(
        name: String,
        functionPointer: Pointer?,
        vararg args: Any?,
    ): Int = function(name, functionPointer).invokeInt(args)

    private fun function(
        name: String,
        functionPointer: Pointer?,
    ): Function =
        Function.getFunction(
            functionPointer ?: throw IllegalStateException(
                "librime API does not provide function pointer: $name",
            ),
        )
}
