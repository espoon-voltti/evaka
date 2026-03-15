// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

data class AuditedResult<T>(val result: T, val context: AuditContext) {
    fun logThenResult(log: (context: AuditContext) -> Unit): T {
        log(context)
        return result
    }
}
