// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.domain.Conflict
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState

/** Locates the closest PSQLException (if any) in the cause chain of the throwable */
fun Throwable.psqlCause(): PSQLException? {
    var cause = this.cause
    while (cause != null && cause !is PSQLException) {
        cause = cause.cause
    }
    return cause as? PSQLException
}

fun mapPSQLException(e: Exception): Exception =
    if (e.cause is PSQLException) {
        val ex = e.cause as PSQLException
        when (ex.sqlState) {
            PSQLState.UNIQUE_VIOLATION.state,
            PSQLState.EXCLUSION_VIOLATION.state ->
                Conflict("Unique or exclusion constraint violation in database", cause = e)
            else -> ex
        }
    } else {
        e
    }
