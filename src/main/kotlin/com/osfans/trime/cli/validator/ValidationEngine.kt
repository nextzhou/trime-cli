// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

import com.charleskorn.kaml.YamlMap
import java.io.File

object ValidationEngine {
    fun validate(
        node: YamlMap,
        configFile: String = "<unknown>",
        fontDir: File? = null,
    ): ValidationReport {
        val messages = mutableListOf<ValidationMessage>()
        messages += CrashPreventionValidator.validate(node)
        messages += SilentFailureValidator.validate(node, fontDir)
        messages += SchemaConstraintValidator.validate(node)
        messages += LayoutValidator.validate(node)

        val sorted =
            messages.sortedWith(
                compareBy({ it.level.ordinal }, { it.path }),
            )

        return ValidationReport(configFile = configFile, messages = sorted)
    }
}
