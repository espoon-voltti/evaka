// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.dao

import fi.espoo.evaka.shared.domain.Conflict
import org.postgresql.util.PSQLException
import java.time.LocalDate

object PGConstants {
    val minDate: LocalDate = LocalDate.of(0, 1, 1)
    val maxDate: LocalDate = LocalDate.of(9999, 1, 1)
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
