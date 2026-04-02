// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.validator

object ValidationFormatter {
    fun formatText(report: ValidationReport): String {
        val sb = StringBuilder()
        sb.appendLine("正在校验：${report.configFile}")
        sb.appendLine()

        if (report.messages.isEmpty()) {
            sb.appendLine("结果：通过（未发现问题）")
            return sb.toString()
        }

        val errors = report.messages.filter { it.level == Level.ERROR }
        val warnings = report.messages.filter { it.level == Level.WARNING }
        val infos = report.messages.filter { it.level == Level.INFO }

        if (errors.isNotEmpty()) {
            sb.appendLine("错误（${errors.size}）：")
            for (msg in errors) {
                sb.appendLine("  [${msg.code}] ${msg.path}: ${msg.message}")
                msg.suggestion?.let { sb.appendLine("         建议：$it") }
            }
            sb.appendLine()
        }

        if (warnings.isNotEmpty()) {
            sb.appendLine("警告（${warnings.size}）：")
            for (msg in warnings) {
                sb.appendLine("  [${msg.code}] ${msg.path}: ${msg.message}")
                msg.suggestion?.let { sb.appendLine("         建议：$it") }
            }
            sb.appendLine()
        }

        if (infos.isNotEmpty()) {
            sb.appendLine("提示（${infos.size}）：")
            for (msg in infos) {
                sb.appendLine("  [${msg.code}] ${msg.path}: ${msg.message}")
                msg.suggestion?.let { sb.appendLine("         建议：$it") }
            }
            sb.appendLine()
        }

        val resultStr = if (report.isValid) "通过" else "未通过"
        sb.append("结果：$resultStr（${report.errorCount} 个错误，${report.warningCount} 个警告）")

        return sb.toString()
    }

    fun formatJson(report: ValidationReport): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"file\": ${jsonString(report.configFile)},")
        sb.appendLine("  \"valid\": ${report.isValid},")
        sb.appendLine("  \"errors\": ${report.errorCount},")
        sb.appendLine("  \"warnings\": ${report.warningCount},")
        sb.appendLine("  \"info\": ${report.infoCount},")
        sb.appendLine("  \"messages\": [")

        val msgs = report.messages
        for ((idx, msg) in msgs.withIndex()) {
            sb.append("    {")
            sb.append("\"level\": ${jsonString(msg.level.name)}, ")
            sb.append("\"code\": ${jsonString(msg.code)}, ")
            sb.append("\"path\": ${jsonString(msg.path)}, ")
            sb.append("\"message\": ${jsonString(msg.message)}")
            msg.suggestion?.let { sb.append(", \"suggestion\": ${jsonString(it)}") }
            sb.append("}")
            if (idx < msgs.size - 1) sb.append(",")
            sb.appendLine()
        }

        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }

    private fun jsonString(s: String): String {
        val escaped =
            s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
