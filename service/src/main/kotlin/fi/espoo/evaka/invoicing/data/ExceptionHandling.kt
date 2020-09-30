// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import mu.KotlinLogging
import org.postgresql.util.PSQLException

// PostgreSQL error codes
const val dataException = "22000"
const val checkViolation = "23514"
const val exclusionViolation = "23P01"

private val logger = KotlinLogging.logger {}

fun <T> handlingExceptions(fn: () -> T): T {
    try {
        return fn.invoke()
    } catch (e: Exception) {
        when (e.cause) {
            is PSQLException -> {
                with(e.cause as PSQLException) {
                    when (this.sqlState) {
                        dataException, checkViolation ->
                            throw BadRequest("Invalid data")
                        exclusionViolation ->
                            throw Conflict("Exclusion constraint violation in database")
                        else -> {
                            logger.warn("Unmapped PSQLException sqlState error code ${this.sqlState}")
                            throw this
                        }
                    }
                }
            }
            else -> throw e
        }
    }
}
