// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.shared.domain.Conflict
import org.postgresql.util.PSQLException
import java.time.LocalDate

object PGConstants {
    // Jooq can't handle the actual postgres infinity value well, so just using some date far in the future instead
    val infinity: LocalDate = LocalDate.of(9999, 1, 1)
}

// postgres does not seem to have a proper enum class for these so listing necessary ones here...
enum class PSQLErrorCodes(val code: String) {
    UNIQUE_VIOLATION("23505"),
    EXCLUSION_VIOLATION("23P01")
}

fun mapPSQLException(e: Exception): Exception {
    return if (e.cause is PSQLException) {
        val ex = e.cause as PSQLException
        when (ex.sqlState) {
            PSQLErrorCodes.UNIQUE_VIOLATION.code, PSQLErrorCodes.EXCLUSION_VIOLATION.code ->
                Conflict("Unique or exclusion constraint violation in database")
            else -> ex
        }
    } else {
        e
    }
}
