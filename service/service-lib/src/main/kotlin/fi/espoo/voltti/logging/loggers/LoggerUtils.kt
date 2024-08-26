// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.voltti.logging.loggers

@Suppress("NOTHING_TO_INLINE", "TooGenericExceptionCaught")
internal inline fun (() -> Any?).toStringSafe(): String =
    try {
        invoke().toString()
    } catch (e: Exception) {
        "Log message invocation failed: $e"
    }
