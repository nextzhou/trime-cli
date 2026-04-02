// SPDX-FileCopyrightText: 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.util

fun CharSequence.splitWithSurrogates(): List<String> = buildList {
    var sur = Char(0)
    for (ch in this@splitWithSurrogates) {
        if (ch.isHighSurrogate()) {
            sur = ch
        } else if (ch.isLowSurrogate()) {
            add(String(charArrayOf(sur, ch)))
        } else {
            add(ch.toString())
        }
    }
}
