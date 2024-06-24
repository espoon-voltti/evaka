// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun runSanityChecks(
    tx: Database.Read,
    clock: EvakaClock
) {
    logResult("child attendances on future days", tx.sanityCheckAttendancesInFuture(clock.today()))
    logResult("service need outside placement", tx.sanityCheckServiceNeedOutsidePlacement())
    logResult("group placement outside placement", tx.sanityCheckGroupPlacementOutsidePlacement())
}

private fun logResult(
    description: String,
    violations: Int
) {
    if (violations > 0) {
        logger.warn("Sanity check failed - $description: $violations instances")
    } else {
        logger.info("Sanity check passed - $description")
    }
}

fun Database.Read.sanityCheckAttendancesInFuture(today: LocalDate): Int =
    createQuery {
        sql(
            """
        SELECT count(*)
        FROM child_attendance 
        WHERE date > ${bind(today)}
    """
        )
    }.exactlyOne()

fun Database.Read.sanityCheckServiceNeedOutsidePlacement(): Int =
    createQuery {
        sql(
            """
        SELECT count(*)
        FROM service_need sn
        JOIN placement pl on pl.id = sn.placement_id
        WHERE sn.start_date < pl.start_date OR sn.end_date > pl.end_date
    """
        )
    }.exactlyOne()

fun Database.Read.sanityCheckGroupPlacementOutsidePlacement(): Int =
    createQuery {
        sql(
            """
        SELECT count(*)
        FROM daycare_group_placement gpl
        JOIN placement pl on pl.id = gpl.daycare_placement_id
        WHERE gpl.start_date < pl.start_date OR gpl.end_date > pl.end_date
    """
        )
    }.exactlyOne()
