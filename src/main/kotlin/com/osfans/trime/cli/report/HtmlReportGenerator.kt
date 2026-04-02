// SPDX-FileCopyrightText: 2015 - 2024 Rime community
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.cli.report

import com.osfans.trime.cli.validator.Level
import com.osfans.trime.cli.validator.ValidationReport
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO

object HtmlReportGenerator {
    fun generate(
        report: ValidationReport,
        keyboardImages: Map<String, BufferedImage>,
        outputFile: File,
    ) {
        val html = buildHtml(report, keyboardImages)
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(html, Charsets.UTF_8)
    }

    private fun buildHtml(
        report: ValidationReport,
        keyboardImages: Map<String, BufferedImage>,
    ): String {
        val sb = StringBuilder()
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html lang=\"zh-CN\">")
        sb.appendLine("<head>")
        sb.appendLine("<meta charset=\"utf-8\">")
        sb.appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
        sb.appendLine("<title>trime.yaml 校验报告</title>")
        sb.appendLine("<style>")
        sb.appendLine(CSS)
        sb.appendLine("</style>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")

        val statusClass = if (report.isValid) "valid" else "invalid"
        val statusText = if (report.isValid) "通过" else "未通过"
        sb.appendLine("<h1>配置校验报告</h1>")
        sb.appendLine("<p class=\"file-path\">${escapeHtml(report.configFile)}</p>")

        sb.appendLine("<section id=\"summary\">")
        sb.appendLine("<h2>摘要</h2>")
        sb.appendLine("<div class=\"status $statusClass\">$statusText</div>")
        sb.appendLine("<div class=\"counts\">")
        sb.appendLine("<span class=\"count error\">${report.errorCount} 个错误</span>")
        sb.appendLine("<span class=\"count warning\">${report.warningCount} 个警告</span>")
        sb.appendLine("<span class=\"count info\">${report.infoCount} 条提示</span>")
        sb.appendLine("</div>")
        sb.appendLine("</section>")

        if (report.messages.isNotEmpty()) {
            sb.appendLine("<section id=\"validation\">")
            sb.appendLine("<h2>校验详情</h2>")

            val errors = report.messages.filter { it.level == Level.ERROR }
            val warnings = report.messages.filter { it.level == Level.WARNING }
            val infos = report.messages.filter { it.level == Level.INFO }

            if (errors.isNotEmpty()) {
                sb.appendLine("<h3>错误</h3>")
                appendMessageList(sb, errors, "error")
            }

            if (warnings.isNotEmpty()) {
                sb.appendLine("<h3>警告</h3>")
                appendMessageList(sb, warnings, "warning")
            }

            if (infos.isNotEmpty()) {
                sb.appendLine("<h3>提示</h3>")
                appendMessageList(sb, infos, "info")
            }

            sb.appendLine("</section>")
        }

        if (keyboardImages.isNotEmpty()) {
            sb.appendLine("<section id=\"keyboards\">")
            sb.appendLine("<h2>键盘预览</h2>")
            for ((name, image) in keyboardImages) {
                val base64 = imageToBase64(image)
                sb.appendLine("<div class=\"keyboard\">")
                sb.appendLine("<h3>${escapeHtml(name)}</h3>")
                sb.appendLine("<div class=\"keyboard-image-wrap\">")
                sb.appendLine(
                    "<img src=\"data:image/png;base64,$base64\"" +
                        " alt=\"${escapeHtml(name)}\" class=\"keyboard-img\"" +
                        " width=\"${image.width}\" height=\"${image.height}\">",
                )
                sb.appendLine("</div>")
                sb.appendLine("</div>")
            }
            sb.appendLine("</section>")
        }

        sb.appendLine("</body>")
        sb.appendLine("</html>")
        return sb.toString()
    }

    private fun appendMessageList(
        sb: StringBuilder,
        messages: List<com.osfans.trime.cli.validator.ValidationMessage>,
        cssClass: String,
    ) {
        sb.appendLine("<ul class=\"messages\">")
        for (msg in messages) {
            sb.appendLine("<li class=\"message $cssClass\">")
            sb.appendLine("<span class=\"code\">[${escapeHtml(msg.code)}]</span>")
            sb.appendLine("<span class=\"path\">${escapeHtml(msg.path)}</span>")
            sb.appendLine("<span class=\"text\">${escapeHtml(msg.message)}</span>")
            msg.suggestion?.let {
                sb.appendLine("<span class=\"suggestion\">建议：${escapeHtml(it)}</span>")
            }
            sb.appendLine("</li>")
        }
        sb.appendLine("</ul>")
    }

    private fun imageToBase64(image: BufferedImage): String {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return Base64.getEncoder().encodeToString(baos.toByteArray())
    }

    private fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private val CSS =
        """
        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; background: #f5f5f5; color: #333; }
        h1 { color: #1a1a1a; border-bottom: 2px solid #ddd; padding-bottom: 10px; }
        h2 { color: #444; margin-top: 30px; }
        h3 { color: #555; }
        .file-path { font-family: monospace; background: #e8e8e8; padding: 8px 12px; border-radius: 4px; }
        .status { display: inline-block; padding: 6px 16px; border-radius: 4px; font-weight: bold; font-size: 1.1em; margin: 10px 0; }
        .status.valid { background: #d4edda; color: #155724; }
        .status.invalid { background: #f8d7da; color: #721c24; }
        .counts { margin: 10px 0; }
        .count { display: inline-block; padding: 4px 10px; border-radius: 3px; margin-right: 8px; font-size: 0.9em; }
        .count.error { background: #f8d7da; color: #721c24; }
        .count.warning { background: #fff3cd; color: #856404; }
        .count.info { background: #d1ecf1; color: #0c5460; }
        .messages { list-style: none; padding: 0; }
        .message { padding: 10px 14px; margin: 6px 0; border-radius: 4px; border-left: 4px solid; }
        .message.error { background: #fff5f5; border-color: #dc3545; }
        .message.warning { background: #fffdf0; border-color: #ffc107; }
        .message.info { background: #f0f8ff; border-color: #17a2b8; }
        .code { font-family: monospace; font-weight: bold; margin-right: 8px; }
        .path { font-family: monospace; color: #666; margin-right: 8px; }
        .text { display: block; margin-top: 4px; }
        .suggestion { display: block; margin-top: 4px; font-style: italic; color: #666; font-size: 0.9em; }
        .keyboard { background: white; border-radius: 8px; padding: 16px; margin: 16px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        .keyboard-image-wrap { overflow-x: auto; }
        .keyboard-img { border-radius: 4px; display: block; max-width: none; width: auto; height: auto; }
        section { background: white; border-radius: 8px; padding: 20px; margin: 20px 0; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
        """.trimIndent()
}
