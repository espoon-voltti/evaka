// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

fun runSanityChecks(tx: Database.Read, clock: EvakaClock) {
    logResult("child attendances on future days", tx.sanityCheckAttendancesInFuture(clock.today()))
    logResult("service need outside placement", tx.sanityCheckServiceNeedOutsidePlacement())
    logResult("group placement outside placement", tx.sanityCheckGroupPlacementOutsidePlacement())
    logResult("backup care outside placement", tx.sanityCheckBackupCareOutsidePlacement())
    logResult(
        "reservations for fixed period placements",
        tx.sanityCheckReservationsDuringFixedSchedulePlacements(),
    )
    logResult(
        "same child in overlapping draft fee decisions",
        tx.sanityCheckChildInOverlappingFeeDecisions(listOf(FeeDecisionStatus.DRAFT)),
    )
    logResult(
        "same child in overlapping sent fee decisions",
        tx.sanityCheckChildInOverlappingFeeDecisions(
            listOf(
                FeeDecisionStatus.SENT,
                FeeDecisionStatus.WAITING_FOR_SENDING,
                FeeDecisionStatus.WAITING_FOR_MANUAL_SENDING,
            )
        ),
    )
}

private fun logResult(description: String, violations: Int) {
    if (violations > 0) {
        logger.warn { "Sanity check failed - $description: $violations instances" }
    } else {
        logger.info { "Sanity check passed - $description" }
    }
}

fun Database.Read.sanityCheckAttendancesInFuture(today: LocalDate): Int {
    return createQuery {
            sql(
                """
        SELECT count(*)
        FROM child_attendance 
        WHERE date > ${bind(today)}
    """
            )
        }
        .exactlyOne()
}

fun Database.Read.sanityCheckServiceNeedOutsidePlacement(): Int {
    return createQuery {
            sql(
                """
        SELECT count(*)
        FROM service_need sn
        JOIN placement pl on pl.id = sn.placement_id
        WHERE sn.start_date < pl.start_date OR sn.end_date > pl.end_date
    """
            )
        }
        .exactlyOne()
}

fun Database.Read.sanityCheckGroupPlacementOutsidePlacement(): Int {
    return createQuery {
            sql(
                """
        SELECT count(*)
        FROM daycare_group_placement gpl
        JOIN placement pl on pl.id = gpl.daycare_placement_id
        WHERE gpl.start_date < pl.start_date OR gpl.end_date > pl.end_date
    """
            )
        }
        .exactlyOne()
}

fun Database.Read.sanityCheckBackupCareOutsidePlacement(): Int {
    return createQuery {
            sql(
                """
        SELECT count(*)
        FROM backup_care bc
        WHERE NOT isempty(
            datemultirange(daterange(bc.start_date, bc.end_date, '[]')) - (
                SELECT coalesce(range_agg(daterange(p.start_date, p.end_date, '[]')), '{}'::datemultirange)
                FROM placement p
                WHERE p.child_id = bc.child_id
            )
        )
    """
            )
        }
        .exactlyOne()
}

fun Database.Read.sanityCheckReservationsDuringFixedSchedulePlacements(): Int {
    return createQuery {
            sql(
                """
        SELECT count(*)
        FROM attendance_reservation ar
        JOIN placement pl ON pl.child_id = ar.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ar.date
        WHERE pl.type IN ('PRESCHOOL', 'PREPARATORY')
    """
            )
        }
        .exactlyOne()
}

fun Database.Read.sanityCheckChildInOverlappingFeeDecisions(
    statuses: List<FeeDecisionStatus>
): Int {
    return createQuery {
            sql(
                """
        SELECT count(DISTINCT fdc.child_id)
        FROM fee_decision fd
        JOIN fee_decision_child fdc on fdc.fee_decision_id = fd.id
        WHERE
            fd.status = ANY(${bind(statuses)}) AND
            EXISTS (
                SELECT FROM fee_decision fd_overlapping
                JOIN fee_decision_child fdc_overlapping ON fdc_overlapping.fee_decision_id = fd_overlapping.id
                WHERE
                    fd_overlapping.id != fd.id AND
                    fd.valid_during && fd_overlapping.valid_during AND
                    fdc_overlapping.child_id = fdc.child_id AND
                    fd_overlapping.status = ANY(${bind(statuses)})
            )
    """
            )
        }
        .exactlyOne()
}
