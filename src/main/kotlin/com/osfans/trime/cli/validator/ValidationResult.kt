// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

data class ValidationMessage(
    val level: Level,
    val code: String,
    val path: String,
    val message: String,
    val suggestion: String? = null,
)

enum class Level { ERROR, WARNING, INFO }

data class ValidationReport(
    val configFile: String,
    val messages: List<ValidationMessage>,
) {
    val errorCount: Int get() = messages.count { it.level == Level.ERROR }
    val warningCount: Int get() = messages.count { it.level == Level.WARNING }
    val infoCount: Int get() = messages.count { it.level == Level.INFO }
    val isValid: Boolean get() = errorCount == 0
}
