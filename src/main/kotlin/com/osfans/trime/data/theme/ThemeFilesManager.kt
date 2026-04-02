// SPDX-FileCopyrightText: 2015 - 2025 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.theme

import com.charleskorn.kaml.AnchorsAndAliases
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import com.charleskorn.kaml.yamlMap
import java.io.File
import java.util.logging.Logger

object ThemeFilesManager {
    private const val CODE_POINT_LIMIT = 10 * 1024 * 1024 // 10 MB
    private val logger = Logger.getLogger("trime-cli")

    val yaml =
        Yaml(
            configuration =
            YamlConfiguration(
                strictMode = false,
                yamlNamingStrategy = YamlNamingStrategy.SnakeCase,
                decodeEnumCaseInsensitive = true,
                anchorsAndAliases = AnchorsAndAliases.Permitted(null),
                codePointLimit = CODE_POINT_LIMIT,
            ),
        )

    fun parseTheme(file: File): Theme {
        val node = yaml.parseToYamlNode(file.readText()).yamlMap
        return Theme.decode(node)
    }

    fun parseThemeFromString(content: String): Theme {
        val node = yaml.parseToYamlNode(content).yamlMap
        return Theme.decode(node)
    }
}
