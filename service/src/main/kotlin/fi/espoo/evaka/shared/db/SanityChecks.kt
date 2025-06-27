// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.db

import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildAttendanceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.voltti.logging.loggers.warn
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

private val checkStart = LocalDate.of(2024, 1, 1)
private val checkRange = DateRange(checkStart, null)

fun runSanityChecks(tx: Database.Read, clock: EvakaClock) {
    val today = clock.today()
    logResult("child attendances on future days", tx.sanityCheckAttendancesInFuture(today))
    logResult("service need outside placement", tx.sanityCheckServiceNeedOutsidePlacement())
    logResult("group placement outside placement", tx.sanityCheckGroupPlacementOutsidePlacement())
    logResult("backup care outside placement", tx.sanityCheckBackupCareOutsidePlacement())
    logResult("reservations outside placements", tx.sanityCheckReservationsOutsidePlacements(today))
    logResult(
        "reservations for fixed period placements",
        tx.sanityCheckReservationsDuringFixedSchedulePlacements(today),
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

private fun logResult(description: String, violations: List<Id<*>>) {
    if (violations.isNotEmpty()) {
        logger.warn(mapOf("violations" to violations)) {
            "Sanity check failed - $description: $violations instances"
        }
    } else {
        logger.info { "Sanity check passed - $description" }
    }
}

fun Database.Read.sanityCheckAttendancesInFuture(today: LocalDate): List<ChildAttendanceId> {
    return createQuery {
            sql(
                """
        SELECT id
        FROM child_attendance 
        WHERE date > ${bind(today)}
    """
            )
        }
        .toList()
}

fun Database.Read.sanityCheckServiceNeedOutsidePlacement(): List<ServiceNeedId> {
    return createQuery {
            sql(
                """
        SELECT sn.id
        FROM (SELECT * FROM service_need WHERE start_date >= ${bind(checkStart)}) sn
        JOIN placement pl on pl.id = sn.placement_id
        WHERE sn.start_date < pl.start_date OR sn.end_date > pl.end_date
    """
            )
        }
        .toList()
}

fun Database.Read.sanityCheckGroupPlacementOutsidePlacement(): List<GroupPlacementId> {
    return createQuery {
            sql(
                """
        SELECT gpl.id
        FROM (SELECT * FROM daycare_group_placement WHERE start_date >= ${bind(checkStart)}) gpl
        JOIN placement pl on pl.id = gpl.daycare_placement_id
        WHERE gpl.start_date < pl.start_date OR gpl.end_date > pl.end_date
    """
            )
        }
        .toList()
}

fun Database.Read.sanityCheckBackupCareOutsidePlacement(): List<BackupCareId> {
    return createQuery {
            sql(
                """
        SELECT bc.id
        FROM (SELECT * FROM backup_care WHERE start_date >= ${bind(checkStart)}) bc
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
        .toList()
}

fun Database.Read.sanityCheckReservationsOutsidePlacements(
    today: LocalDate
): List<AttendanceReservationId> {
    return createQuery {
            sql(
                """
        SELECT ar.id
        FROM attendance_reservation ar
        WHERE
            ar.date >= ${bind(today)} AND
            NOT EXISTS (
                SELECT FROM placement p
                WHERE p.child_id = ar.child_id AND daterange(p.start_date, p.end_date, '[]') @> ar.date
            )
    """
            )
        }
        .toList()
}

fun Database.Read.sanityCheckReservationsDuringFixedSchedulePlacements(
    today: LocalDate
): List<AttendanceReservationId> {
    return createQuery {
            sql(
                """
        SELECT ar.id
        FROM attendance_reservation ar
        JOIN placement pl ON pl.child_id = ar.child_id AND daterange(pl.start_date, pl.end_date, '[]') @> ar.date
        WHERE ar.date >= ${bind(today)} AND pl.type IN ('PRESCHOOL', 'PREPARATORY')
    """
            )
        }
        .toList()
}

fun Database.Read.sanityCheckChildInOverlappingFeeDecisions(
    statuses: List<FeeDecisionStatus>
): List<ChildId> {
    return createQuery {
            sql(
                """
        SELECT DISTINCT fdc.child_id
        FROM (SELECT * FROM fee_decision WHERE ${bind(checkRange)} @> valid_during) fd
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
        .toList()
}
