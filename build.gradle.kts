// SPDX-FileCopyrightText: 2015 - 2026 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.GradleException
import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "io.github.nextzhou"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

application {
    mainClass.set("com.osfans.trime.cli.MainKt")
}

val vendorPreludeDir = file("vendor/rime/prelude")
val vendorSharedDir = file("vendor/rime/shared")
val vendorRootDir = file("vendor/rime")
val upstreamRootDir = file("upstream/trime")
val syncManifestFile = file("upstream/sync-manifest.txt")

sourceSets.main {
    resources.srcDir(vendorPreludeDir)
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    if (vendorSharedDir.exists()) {
        from(vendorSharedDir) {
            include("*.yaml")
        }
    }
    if (vendorRootDir.exists()) {
        from(vendorRootDir) {
            include("essay.txt")
        }
    }
}

dependencies {
    implementation(libs.kaml)

    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.8.18")
    implementation("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.8.18")
    implementation("net.java.dev.jna:jna:5.14.0")

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("trime-cli")
    archiveVersion.set("")
    archiveClassifier.set("all")
    mergeServiceFiles()
}

enum class SyncKind { CODE, RESOURCE }

data class SyncMapping(
    val kind: SyncKind,
    val localPath: String,
    val upstreamPath: String,
    val mode: String,
)

fun loadSyncMappings(): List<SyncMapping> {
    if (!syncManifestFile.exists()) return emptyList()
    return syncManifestFile.readLines().mapIndexedNotNull { index, rawLine ->
        val line = rawLine.substringBefore("#").trim()
        if (line.isBlank()) return@mapIndexedNotNull null
        val parts = line.split("|").map { it.trim() }
        require(parts.size == 4) {
            "Invalid sync manifest entry at line ${index + 1}: $rawLine"
        }
        val kind =
            when (parts[0].lowercase()) {
                "code" -> SyncKind.CODE
                "resource" -> SyncKind.RESOURCE
                else -> throw GradleException("Unknown sync kind '${parts[0]}' at line ${index + 1}")
            }
        SyncMapping(
            kind = kind,
            localPath = parts[1],
            upstreamPath = parts[2],
            mode = parts[3],
        )
    }
}

fun requireUpstreamRoot() {
    if (!upstreamRootDir.exists()) {
        throw GradleException(
            "upstream/trime is not available. " +
                "Initialize the submodule first: git submodule update --init --recursive",
        )
    }
}

fun File.safeRelativePath(base: File): String = relativeTo(base).invariantSeparatorsPath

fun collectDirectoryFiles(base: File): Map<String, File> {
    if (!base.exists()) return emptyMap()
    return base
        .walkTopDown()
        .filter { it.isFile }
        .filterNot { it.safeRelativePath(base).startsWith(".git") }
        .associateBy { it.safeRelativePath(base) }
}

fun collectRootYamlFiles(base: File): Map<String, File> {
    if (!base.exists()) return emptyMap()
    return base
        .listFiles()
        .orEmpty()
        .filter { it.isFile && it.extension == "yaml" }
        .associateBy { it.name }
}

fun compareFileMaps(
    localFiles: Map<String, File>,
    upstreamFiles: Map<String, File>,
    localLabel: String,
    upstreamLabel: String,
) {
    val localNames = localFiles.keys
    val upstreamNames = upstreamFiles.keys
    val missingLocal = upstreamNames - localNames
    val extraLocal = localNames - upstreamNames
    if (missingLocal.isNotEmpty()) {
        throw GradleException(
            "Missing local files in $localLabel relative to $upstreamLabel:\n" +
                missingLocal.sorted().joinToString("\n") { "  $it" },
        )
    }
    if (extraLocal.isNotEmpty()) {
        throw GradleException(
            "Unexpected local files in $localLabel relative to $upstreamLabel:\n" +
                extraLocal.sorted().joinToString("\n") { "  $it" },
        )
    }
    for (name in localNames.sorted()) {
        val localBytes = localFiles.getValue(name).readBytes()
        val upstreamBytes = upstreamFiles.getValue(name).readBytes()
        if (!localBytes.contentEquals(upstreamBytes)) {
            throw GradleException("File content drift detected for $name between $localLabel and $upstreamLabel")
        }
    }
}

val stripFromApp =
    listOf(
        Regex("""^[^\S\n]*import android\.os\.Parcelable\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*import kotlinx\.parcelize\.Parcelize\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*import kotlinx\.parcelize\.RawValue\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*@Parcelize\s*$""", RegexOption.MULTILINE),
        Regex("""\s*:\s*Parcelable\b"""),
        Regex("""^[^\S\n]*import timber\.log\.Timber\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*Timber\.\w+\(.*\)\s*$""", RegexOption.MULTILINE),
    )

val stripFromBoth =
    listOf(
        Regex("""^[^\S\n]*import timber\.log\.Timber\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*Timber\.\w+\(.*\)\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*import java\.util\.logging\.Logger\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*private val logger = Logger\.getLogger\(.*\)\s*$""", RegexOption.MULTILINE),
        Regex("""^[^\S\n]*logger\.\w+\(.*\)\s*$""", RegexOption.MULTILINE),
    )

fun normalizeText(text: String): String {
    var result = text
    for (pattern in stripFromBoth) {
        result = result.replace(pattern, "")
    }
    return result
        .replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("""^[^\S\n]*//.*$""", RegexOption.MULTILINE), "")
        .replace(Regex("""\n{3,}"""), "\n\n")
        .trim()
}

fun normalizeUpstreamCode(text: String): String {
    var result = text
    for (pattern in stripFromApp) {
        result = result.replace(pattern, "")
    }
    return normalizeText(result)
}

fun replaceDirectoryContents(
    sourceDir: File,
    targetDir: File,
    topLevelYamlOnly: Boolean,
) {
    delete(targetDir)
    targetDir.mkdirs()
    if (topLevelYamlOnly) {
        copy {
            from(sourceDir) {
                include("*.yaml")
            }
            into(targetDir)
        }
    } else {
        copy {
            from(sourceDir) {
                exclude(".git", ".git/**")
            }
            into(targetDir)
        }
    }
}

tasks.register("refreshUpstreamResources") {
    group = "maintenance"
    description = "Refresh vendored RIME resources from upstream/trime"

    doLast {
        requireUpstreamRoot()
        val resourceMappings = loadSyncMappings().filter { it.kind == SyncKind.RESOURCE }
        if (resourceMappings.isEmpty()) {
            throw GradleException("No resource mappings found in upstream/sync-manifest.txt")
        }

        resourceMappings.forEach { mapping ->
            val source = File(upstreamRootDir, mapping.upstreamPath)
            val target = file(mapping.localPath)
            when (mapping.mode) {
                "dir" -> {
                    require(source.isDirectory) { "Expected directory: ${source.absolutePath}" }
                    replaceDirectoryContents(source, target, topLevelYamlOnly = false)
                }
                "root-yaml" -> {
                    require(source.isDirectory) { "Expected directory: ${source.absolutePath}" }
                    replaceDirectoryContents(source, target, topLevelYamlOnly = true)
                }
                "file" -> {
                    require(source.isFile) { "Expected file: ${source.absolutePath}" }
                    target.parentFile?.mkdirs()
                    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                else -> throw GradleException("Unsupported resource sync mode: ${mapping.mode}")
            }
            println("Updated ${mapping.localPath} from ${mapping.upstreamPath}")
        }
    }
}

tasks.register("checkUpstreamSync") {
    group = "verification"
    description = "Check vendored resources and JVM-adapted code against upstream/trime"

    doLast {
        requireUpstreamRoot()
        val mappings = loadSyncMappings()
        if (mappings.isEmpty()) {
            throw GradleException("No sync mappings found in upstream/sync-manifest.txt")
        }

        mappings.forEach { mapping ->
            val local = file(mapping.localPath)
            val upstream = File(upstreamRootDir, mapping.upstreamPath)

            when (mapping.kind) {
                SyncKind.CODE -> {
                    when (mapping.mode) {
                        "normalized-app" -> {
                            require(local.isFile) { "Missing local file: ${local.absolutePath}" }
                            require(upstream.isFile) { "Missing upstream file: ${upstream.absolutePath}" }
                            val localNormalized = normalizeText(local.readText())
                            val upstreamNormalized = normalizeUpstreamCode(upstream.readText())
                            if (localNormalized != upstreamNormalized) {
                                throw GradleException(
                                    "Code drift detected for ${mapping.localPath}\n" +
                                        "Compare with upstream file ${mapping.upstreamPath}",
                                )
                            }
                        }
                        "exists-only" -> {
                            require(local.exists()) { "Missing local file: ${local.absolutePath}" }
                            require(upstream.exists()) { "Missing upstream file: ${upstream.absolutePath}" }
                        }
                        else -> throw GradleException("Unsupported code sync mode: ${mapping.mode}")
                    }
                }
                SyncKind.RESOURCE -> {
                    when (mapping.mode) {
                        "dir" -> compareFileMaps(
                            localFiles = collectDirectoryFiles(local),
                            upstreamFiles = collectDirectoryFiles(upstream),
                            localLabel = mapping.localPath,
                            upstreamLabel = mapping.upstreamPath,
                        )
                        "root-yaml" -> compareFileMaps(
                            localFiles = collectRootYamlFiles(local),
                            upstreamFiles = collectRootYamlFiles(upstream),
                            localLabel = mapping.localPath,
                            upstreamLabel = mapping.upstreamPath,
                        )
                        "file" -> {
                            require(local.isFile) { "Missing local file: ${local.absolutePath}" }
                            require(upstream.isFile) { "Missing upstream file: ${upstream.absolutePath}" }
                            if (!local.readBytes().contentEquals(upstream.readBytes())) {
                                throw GradleException(
                                    "Resource drift detected for ${mapping.localPath} against ${mapping.upstreamPath}",
                                )
                            }
                        }
                        else -> throw GradleException("Unsupported resource sync mode: ${mapping.mode}")
                    }
                }
            }
            println("OK: ${mapping.localPath}")
        }
    }
}
