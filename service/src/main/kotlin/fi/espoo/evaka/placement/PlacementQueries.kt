// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.backupcare.recreateBackupCares
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.user.EvakaUser
import java.time.LocalDate
import org.jdbi.v3.core.mapper.Nested

fun Database.Read.getPlacement(id: PlacementId): Placement? {
    return createQuery {
            sql(
                """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by, p.place_guarantee
FROM placement p
WHERE p.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<Placement>()
}

fun Database.Read.getPlacementSummary(childId: ChildId): List<PlacementSummary> {
    return createQuery {
            sql(
                """
SELECT p.id, p.type, p.child_id, d.id AS unit_id, d.name AS unit_name, p.start_date, p.end_date
FROM placement p
JOIN daycare d on p.unit_id = d.id
WHERE p.child_id = ${bind(childId)}
"""
            )
        }
        .toList<PlacementSummary>()
}

fun Database.Read.getPlacementsForChild(childId: ChildId): List<Placement> {
    return createQuery {
            sql(
                """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by, p.place_guarantee
FROM placement p
WHERE p.child_id = ${bind(childId)}
"""
            )
        }
        .toList<Placement>()
}

fun Database.Read.getPlacementsForChildDuring(
    childId: ChildId,
    start: LocalDate,
    end: LocalDate?
): List<Placement> {
    return createQuery {
            sql(
                """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by, p.place_guarantee
FROM placement p
WHERE p.child_id = ${bind(childId)}
AND daterange(p.start_date, p.end_date, '[]') && daterange(${bind(start)}, ${bind(end)}, '[]')
"""
            )
        }
        .toList<Placement>()
}

fun Database.Read.getCurrentPlacementForChild(clock: EvakaClock, childId: ChildId): Placement? {
    return createQuery {
            sql(
                """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by, p.place_guarantee
FROM placement p
WHERE p.child_id = ${bind(childId)}
AND daterange(p.start_date, p.end_date, '[]') @> ${bind(clock.today())}
"""
            )
        }
        .exactlyOneOrNull<Placement>()
}

fun Database.Transaction.insertPlacement(
    type: PlacementType,
    childId: ChildId,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate,
    placeGuarantee: Boolean
): Placement {
    return createQuery {
            sql(
                """
INSERT INTO placement (type, child_id, unit_id, start_date, end_date, place_guarantee)
VALUES (${bind(type)}::placement_type, ${bind(childId)}, ${bind(unitId)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(placeGuarantee)})
RETURNING *
        """
            )
        }
        .toList<Placement>()
        .first()
}

data class PlacementChildAndRange(
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unitId: DaycareId
)

fun Database.Read.getPlacementChildAndRange(placementId: PlacementId) =
    createQuery {
            sql(
                "SELECT child_id, start_date, end_date, unit_id FROM placement WHERE id = ${bind(placementId)}"
            )
        }
        .exactlyOne<PlacementChildAndRange>()

fun Database.Transaction.updatePlacementStartDate(placementId: PlacementId, date: LocalDate) {
    val placement = getPlacementChildAndRange(placementId)

    createUpdate {
            sql("UPDATE placement SET start_date = ${bind(date)} WHERE id = ${bind(placementId)}")
        }
        .execute()

    recreateBackupCares(placement.childId)
}

fun Database.Transaction.updatePlacementEndDate(placementId: PlacementId, date: LocalDate) {
    val placement = getPlacementChildAndRange(placementId)
    createUpdate {
            sql("UPDATE placement SET end_date = ${bind(date)} WHERE id = ${bind(placementId)}")
        }
        .execute()

    recreateBackupCares(
        placement.childId,
    )
}

fun Database.Transaction.updatePlacementStartAndEndDate(
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate
) {
    val placement = getPlacementChildAndRange(placementId)
    createUpdate {
            sql(
                "UPDATE placement SET start_date = ${bind(startDate)}, end_date = ${bind(endDate)} WHERE id = ${bind(placementId)}"
            )
        }
        .execute()

    recreateBackupCares(
        placement.childId,
    )
}

fun Database.Transaction.updatePlacementType(placementId: PlacementId, type: PlacementType) {
    createUpdate {
            sql("UPDATE placement SET type = ${bind(type)} WHERE id = ${bind(placementId)}")
        }
        .execute()
}

fun Database.Transaction.deleteServiceNeedsFromPlacement(placementId: PlacementId) {
    createUpdate { sql("DELETE FROM service_need WHERE placement_id = ${bind(placementId)}") }
        .execute()
}

data class CancelPlacementResult(
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

fun Database.Transaction.cancelPlacement(id: PlacementId): CancelPlacementResult {
    val placement = getPlacement(id) ?: throw NotFound("Placement $id not found")
    clearCalendarEventAttendees(
        placement.childId,
        placement.unitId,
        FiniteDateRange(placement.startDate, placement.endDate)
    )

    createUpdate {
            sql("DELETE FROM daycare_group_placement WHERE daycare_placement_id = ${bind(id)}")
        }
        .execute()

    deleteServiceNeedsFromPlacement(id)

    createUpdate { sql("DELETE FROM placement WHERE id = ${bind(id)}") }.execute()

    recreateBackupCares(
        placement.childId,
    )

    return CancelPlacementResult(
        placement.childId,
        placement.unitId,
        placement.startDate,
        placement.endDate
    )
}

fun Database.Transaction.clearGroupPlacementsAfter(placementId: PlacementId, date: LocalDate) {
    createUpdate {
            sql(
                """
                DELETE from daycare_group_placement
                WHERE daycare_placement_id = ${bind(placementId)} AND start_date > ${bind(date)}
                """
            )
        }
        .execute()

    createUpdate {
            sql(
                """
            UPDATE daycare_group_placement SET end_date = ${bind(date)}
            WHERE daycare_placement_id = ${bind(placementId)} AND start_date <= ${bind(date)} AND end_date > ${bind(date)}
        """
            )
        }
        .execute()

    val placement = getPlacement(placementId)

    if (placement != null) {
        clearCalendarEventAttendees(
            placement.childId,
            placement.unitId,
            FiniteDateRange(date, LocalDate.MAX)
        )
    }
}

fun Database.Transaction.clearGroupPlacementsBefore(placementId: PlacementId, date: LocalDate) {
    createUpdate {
            sql(
                """
        DELETE from daycare_group_placement
        WHERE daycare_placement_id = ${bind(placementId)} AND end_date < ${bind(date)}
        """
            )
        }
        .execute()

    createUpdate {
            sql(
                """
            UPDATE daycare_group_placement SET start_date = ${bind(date)}
            WHERE daycare_placement_id = ${bind(placementId)} AND start_date < ${bind(date)} AND end_date >= ${bind(date)}
        """
            )
        }
        .execute()

    val placement = getPlacement(placementId)

    if (placement != null) {
        clearCalendarEventAttendees(
            placement.childId,
            placement.unitId,
            FiniteDateRange(LocalDate.MIN, date)
        )
    }
}

fun Database.Transaction.clearCalendarEventAttendees(
    childId: ChildId,
    unitId: DaycareId,
    range: FiniteDateRange?
) {
    val startDate = range?.start?.takeIf { !it.isEqual(LocalDate.MIN) }
    val endDate = range?.end?.takeIf { !it.isEqual(LocalDate.MAX) }
    createUpdate {
            sql(
                """
DELETE FROM calendar_event_attendee cea
WHERE cea.child_id = ${bind(childId)}
  AND cea.unit_id = ${bind(unitId)}
  AND (
    ${bind(startDate)} IS NULL OR
    (SELECT lower(ce.period) FROM calendar_event ce WHERE ce.id = cea.calendar_event_id) >= ${bind(startDate)}
  )
  AND (
    ${bind(endDate)} IS NULL OR
    (SELECT upper(ce.period) FROM calendar_event ce WHERE ce.id = cea.calendar_event_id) < ${bind(endDate)}
  )
"""
            )
        }
        .execute()

    // clear events that no longer have any attendees
    createUpdate {
            sql(
                """
            DELETE FROM calendar_event ce
            WHERE NOT EXISTS(
                SELECT 1 FROM calendar_event_attendee cea WHERE cea.calendar_event_id = ce.id
            )
        """
            )
        }
        .execute()
}

fun Database.Read.getDaycarePlacements(
    daycareId: DaycareId?,
    childId: ChildId?,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<DaycarePlacementDetails> {
    return createQuery {
            sql(
                """
        SELECT
            pl.id, pl.start_date, pl.end_date, pl.type, pl.child_id, pl.termination_requested_date,
            terminated_by.id AS terminated_by_id,
            terminated_by.name AS terminated_by_name,
            terminated_by.type AS terminated_by_type,
            pl.unit_id AS daycare_id,
            pl.place_guarantee,
            d.name AS daycare_name,
            d.provider_type AS daycare_provider_type,
            d.enabled_pilot_features AS daycare_enabled_pilot_features,
            d.language AS daycare_language,
            a.name AS daycare_area,
            ch.first_name as child_first_name,
            ch.last_name as child_last_name,
            ch.social_security_number as child_social_security_number,
            ch.date_of_birth as child_date_of_birth,
            CASE
                WHEN (SELECT every(default_option) FROM service_need_option WHERE valid_placement_type = pl.type) THEN 0
                ELSE (
                    SELECT count(*)
                    FROM generate_series(greatest(pl.start_date, '2020-03-01'::date), pl.end_date, '1 day') t
                    LEFT JOIN service_need sn ON pl.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
                    WHERE sn.id IS NULL
                )
            END AS missing_service_need_days
        FROM placement pl
        JOIN daycare d on pl.unit_id = d.id
        JOIN person ch on pl.child_id = ch.id
        JOIN care_area a ON d.care_area_id = a.id
        LEFT JOIN evaka_user terminated_by ON pl.terminated_by = terminated_by.id
        WHERE daterange(pl.start_date, pl.end_date, '[]') && daterange(${bind(startDate)}, ${bind(endDate)}, '[]')
        AND (${bind(daycareId)}::uuid IS NULL OR pl.unit_id = ${bind(daycareId)})
        AND (${bind(childId)}::uuid IS NULL OR pl.child_id = ${bind(childId)})
        """
            )
        }
        .toList<DaycarePlacementDetails>()
}

fun Database.Read.getDaycarePlacement(id: PlacementId): DaycarePlacement? {
    return createQuery {
            sql(
                """
                SELECT
                    p.id AS placement_id,
                    p.start_date AS placement_start,
                    p.end_date AS placement_end,
                    p.type AS placement_type,
                    c.id AS child_id,
                    c.social_security_number AS child_ssn,
                    c.first_name AS child_first_name,
                    c.last_name AS child_last_name,
                    c.date_of_birth AS child_date_of_birth,
                    u.id AS unit_id,
                    u.name AS unit_name,
                    u.provider_type,
                    u.enabled_pilot_features,
                    u.language,
                    a.name AS area_name
                FROM placement p
                JOIN daycare u ON p.unit_id = u.id
                JOIN person c ON p.child_id = c.id
                JOIN care_area a ON u.care_area_id = a.id
                WHERE p.id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull(toDaycarePlacement)
}

fun Database.Read.getTerminatedPlacements(
    today: LocalDate,
    daycareId: DaycareId,
    terminationRequestedMinDate: LocalDate?,
    terminationRequestedMaxDate: LocalDate?
): List<TerminatedPlacement> =
    createQuery {
            sql(
                """
SELECT
    pl.id, pl.end_date, pl.type, pl.termination_requested_date, 
    ch.id AS child_id,
    ch.first_name AS child_first_name,
    ch.last_name AS child_last_name,
    ch.social_security_number AS child_social_security_number,
    ch.date_of_birth AS child_date_of_birth,
    terminated_by.id AS terminated_by_id,
    terminated_by.name AS terminated_by_name,
    terminated_by.type AS terminated_by_type,
    dg.name AS current_daycare_group_name,
    (pl.type = 'PRESCHOOL_DAYCARE' OR pl.type = 'PREPARATORY_DAYCARE') AND EXISTS (
        SELECT 1 FROM placement pl_next
        WHERE pl_next.child_id = pl.child_id
        AND pl_next.unit_id = pl.unit_id
        AND pl_next.type = CASE pl.type WHEN 'PRESCHOOL_DAYCARE' THEN 'PRESCHOOL'::placement_type WHEN 'PREPARATORY_DAYCARE' THEN 'PREPARATORY'::placement_type END
        AND pl_next.start_date = pl.end_date + 1
    ) AS connected_daycare_only
    FROM placement pl
    JOIN daycare d on pl.unit_id = d.id
    JOIN person ch on pl.child_id = ch.id
    LEFT JOIN evaka_user terminated_by ON pl.terminated_by = terminated_by.id
    LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> ${bind(today)}::date
    LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
    WHERE pl.unit_id = ${bind(daycareId)}
    AND daterange(${bind(terminationRequestedMinDate)}, ${bind(terminationRequestedMaxDate)}, '[]') @> pl.termination_requested_date 
    """
            )
        }
        .toList<TerminatedPlacement>()

data class TerminatedPlacement(
    val id: PlacementId,
    val endDate: LocalDate,
    val type: PlacementType,
    val terminationRequestedDate: LocalDate?,
    @Nested("child") val child: ChildBasics,
    @Nested("terminated_by") val terminatedBy: EvakaUser?,
    val currentDaycareGroupName: String?,
    val connectedDaycareOnly: Boolean
)

data class ChildPlacement(
    val childId: ChildId,
    val id: PlacementId,
    val type: PlacementType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unitId: DaycareId,
    val unitName: String,
    val terminatable: Boolean,
    val terminationRequestedDate: LocalDate?,
    @Nested("terminated_by") val terminatedBy: EvakaUser?
)

fun Database.Read.getCitizenChildPlacements(
    today: LocalDate,
    childId: ChildId
): List<ChildPlacement> =
    createQuery {
            sql(
                """
SELECT
    child.id AS child_id,
    p.id,
    p.type,
    p.start_date,
    p.end_date,
    p.termination_requested_date,
    d.id AS unit_id,
    d.name AS unit_name,
    'PLACEMENT_TERMINATION' = ANY(enabled_pilot_features) as terminatable,
    evaka_user.id as terminated_by_id,
    evaka_user.name as terminated_by_name,
    evaka_user.type as terminated_by_type
FROM placement p
JOIN daycare d ON p.unit_id = d.id
JOIN person child ON p.child_id = child.id
LEFT JOIN evaka_user ON p.terminated_by = evaka_user.id
WHERE
    p.child_id = ${bind(childId)}
    AND p.end_date >= ${bind(today)}::date
    """
            )
        }
        .toList<ChildPlacement>()

fun Database.Read.getDaycareGroupPlacement(id: GroupPlacementId): DaycareGroupPlacement? {
    return createQuery {
            sql(
                """
                SELECT 
                    gp.id, 
                    gp.daycare_group_id AS group_id,
                    dg.name AS group_name,
                    gp.daycare_placement_id, 
                    gp.start_date, 
                    gp.end_date
                FROM daycare_group_placement gp
                JOIN daycare_group dg ON dg.id = gp.daycare_group_id
                WHERE gp.id = ${bind(id)}
                """
            )
        }
        .exactlyOneOrNull<DaycareGroupPlacement>()
}

fun Database.Read.getIdenticalPrecedingGroupPlacement(
    daycarePlacementId: PlacementId,
    groupId: GroupId,
    startDate: LocalDate
): DaycareGroupPlacement? {
    return createQuery {
            sql(
                """
SELECT 
    gp.id, 
    gp.daycare_group_id AS group_id,
    dg.name AS group_name,
    gp.daycare_placement_id, 
    gp.start_date, 
    gp.end_date
FROM daycare_group_placement gp
JOIN daycare_group dg ON dg.id = gp.daycare_group_id
WHERE daycare_placement_id = ${bind(daycarePlacementId)} AND daycare_group_id = ${bind(groupId)} AND gp.end_date = ${bind(startDate.minusDays(1))}
"""
            )
        }
        .exactlyOneOrNull<DaycareGroupPlacement>()
}

fun Database.Read.getIdenticalPostcedingGroupPlacement(
    daycarePlacementId: PlacementId,
    groupId: GroupId,
    endDate: LocalDate
): DaycareGroupPlacement? {
    return createQuery {
            sql(
                """
SELECT 
    gp.id, 
    gp.daycare_group_id AS group_id,
    dg.name AS group_name,
    gp.daycare_placement_id, 
    gp.start_date, 
    gp.end_date
FROM daycare_group_placement gp
JOIN daycare_group dg ON dg.id = gp.daycare_group_id
WHERE daycare_placement_id = ${bind(daycarePlacementId)} AND daycare_group_id = ${bind(groupId)} AND gp.start_date = ${bind(endDate.plusDays(1))}
"""
            )
        }
        .exactlyOneOrNull<DaycareGroupPlacement>()
}

fun Database.Read.hasGroupPlacements(groupId: GroupId): Boolean =
    createQuery {
            sql(
                "SELECT EXISTS (SELECT 1 FROM daycare_group_placement WHERE daycare_group_id = ${bind(groupId)})"
            )
        }
        .exactlyOne<Boolean>()

fun Database.Read.getGroupPlacementsAtDaycare(
    daycareId: DaycareId,
    placementRange: DateRange
): List<DaycareGroupPlacement> {
    return createQuery {
            sql(
                """
SELECT 
    gp.id, 
    gp.daycare_group_id AS group_id,
    dg.name AS group_name,
    gp.daycare_placement_id, 
    gp.start_date, 
    gp.end_date
FROM daycare_group_placement gp
JOIN daycare_group dg ON dg.id = gp.daycare_group_id
WHERE EXISTS (
    SELECT 1 FROM placement p
    WHERE p.id = daycare_placement_id
    AND p.unit_id = ${bind(daycareId)}
    AND daterange(p.start_date, p.end_date, '[]') && ${bind(placementRange)}
)
"""
            )
        }
        .toList<DaycareGroupPlacement>()
}

fun Database.Read.getGroupPlacementsByChildren(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, ChildDaycareGroupPlacement> {
    return createQuery {
            sql(
                """
SELECT pl.child_id, gp.daycare_group_id AS group_id, dg.name AS group_name
FROM daycare_group_placement gp
JOIN daycare_group dg ON dg.id = gp.daycare_group_id
JOIN placement pl ON pl.id = gp.daycare_placement_id
WHERE pl.child_id = ANY(${bind(childIds)}) AND daterange(gp.start_date, gp.end_date, '[]') && ${bind(range)}
ORDER BY gp.start_date
"""
            )
        }
        .toList<ChildDaycareGroupPlacement>()
        .associateBy { it.childId }
}

fun Database.Read.getChildGroupPlacements(childId: ChildId): List<DaycareGroupPlacement> {
    return createQuery {
            sql(
                """
SELECT 
    gp.id, 
    gp.daycare_group_id AS group_id,
    dg.name AS group_name,
    gp.daycare_placement_id, 
    gp.start_date, 
    gp.end_date
FROM daycare_group_placement gp
JOIN daycare_group dg ON dg.id = gp.daycare_group_id
JOIN placement pl ON pl.id = gp.daycare_placement_id
WHERE pl.child_id = ${bind(childId)}
"""
            )
        }
        .toList<DaycareGroupPlacement>()
}

fun Database.Read.getGroupPlacementChildren(
    groupId: GroupId,
    range: FiniteDateRange
): List<ChildId> {
    return createQuery {
            sql(
                """
        SELECT DISTINCT pl.child_id
        FROM daycare_group_placement gp
        JOIN placement pl ON pl.id = gp.daycare_placement_id
        WHERE gp.daycare_group_id = ${bind(groupId)} AND daterange(gp.start_date, gp.end_date, '[]') && ${bind(range)}
        """
            )
        }
        .toList<ChildId>()
}

fun Database.Transaction.createGroupPlacement(
    placementId: PlacementId,
    groupId: GroupId,
    startDate: LocalDate,
    endDate: LocalDate
): GroupPlacementId {
    return createQuery {
            sql(
                """
INSERT INTO daycare_group_placement (daycare_placement_id, daycare_group_id, start_date, end_date)
VALUES (${bind(placementId)}, ${bind(groupId)}, ${bind(startDate)}, ${bind(endDate)})
RETURNING id
"""
            )
        }
        .exactlyOne<GroupPlacementId>()
}

fun Database.Transaction.updateGroupPlacementStartDate(
    id: GroupPlacementId,
    startDate: LocalDate
): Boolean {
    return createQuery {
            sql(
                "UPDATE daycare_group_placement SET start_date = ${bind(startDate)} WHERE id = ${bind(id)} RETURNING id"
            )
        }
        .exactlyOneOrNull<GroupPlacementId>() != null
}

fun Database.Transaction.updateGroupPlacementEndDate(
    id: GroupPlacementId,
    endDate: LocalDate
): Boolean {
    return createQuery {
            sql(
                "UPDATE daycare_group_placement SET end_date = ${bind(endDate)} WHERE id = ${bind(id)} RETURNING id"
            )
        }
        .exactlyOneOrNull<GroupPlacementId>() != null
}

fun Database.Transaction.deleteGroupPlacement(id: GroupPlacementId): Boolean {
    val dgPlacement = getDaycareGroupPlacement(id)
    if (dgPlacement != null) {
        val placement = getPlacement(dgPlacement.daycarePlacementId)

        if (placement != null) {
            clearCalendarEventAttendees(placement.childId, placement.unitId, null)
        }
    }
    return createQuery {
            sql("DELETE FROM daycare_group_placement WHERE id = ${bind(id)} RETURNING id")
        }
        .exactlyOneOrNull<GroupPlacementId>() != null
}

private val toDaycarePlacement: Row.() -> DaycarePlacement = {
    DaycarePlacement(
        id = column("placement_id"),
        child =
            ChildBasics(
                id = ChildId(column("child_id")),
                socialSecurityNumber = column("child_ssn"),
                firstName = column("child_first_name"),
                lastName = column("child_last_name"),
                dateOfBirth = column("child_date_of_birth")
            ),
        daycare =
            DaycareBasics(
                id = DaycareId(column("unit_id")),
                name = column("unit_name"),
                area = column("area_name"),
                providerType = column("provider_type"),
                enabledPilotFeatures = column("enabled_pilot_features"),
                language = column("language")
            ),
        startDate = column("placement_start"),
        endDate = column("placement_end"),
        type = column("placement_type")
    )
}

fun Database.Transaction.deleteServiceNeedsFromPlacementAfter(id: PlacementId, date: LocalDate) {
    createUpdate {
            sql(
                """
DELETE FROM service_need
WHERE placement_id = ${bind(id)} AND start_date > ${bind(date)}
"""
            )
        }
        .execute()

    createUpdate {
            sql(
                """
UPDATE service_need
SET end_date = ${bind(date)}
WHERE placement_id = ${bind(id)}
    AND daterange(start_date, end_date, '[]') @> ${bind(date)}::date
"""
            )
        }
        .execute()
}

fun Database.Transaction.terminatePlacementFrom(
    terminationRequestedDate: LocalDate,
    placementId: PlacementId,
    terminationDate: LocalDate,
    terminatedBy: EvakaUserId?
) {
    clearGroupPlacementsAfter(placementId, terminationDate)
    deleteServiceNeedsFromPlacementAfter(placementId, terminationDate)

    val placement = getPlacementChildAndRange(placementId)

    createUpdate {
            sql(
                """
UPDATE placement
SET termination_requested_date = ${bind(if (terminatedBy == null) null else terminationRequestedDate)},
    terminated_by = ${bind(terminatedBy)},
    end_date = ${bind(terminationDate)}
WHERE id = ${bind(placementId)}
        """
            )
        }
        .execute()

    recreateBackupCares(
        placement.childId,
    )
}

fun Database.Transaction.updatePlacementTermination(
    placementId: PlacementId,
    terminationDate: LocalDate,
    terminatedBy: EvakaUserId
) {
    createUpdate {
            sql(
                """
UPDATE placement SET
    termination_requested_date = ${bind(terminationDate)},
    terminated_by = ${bind(terminatedBy)}
WHERE id = ${bind(placementId)}
"""
            )
        }
        .execute()
}

fun Database.Read.childPlacementsHasConsecutiveRange(
    childId: ChildId,
    range: FiniteDateRange
): Boolean =
    createQuery {
            sql(
                """
    SELECT (
        SELECT range_agg(daterange(start_date, end_date, '[]')) FROM placement
        WHERE child_id = ${bind(childId)}
    ) @> ${bind(range)}
    """
            )
        }
        .exactlyOne<Boolean>()

fun Database.Read.getChildPlacementUnitLanguage(childId: ChildId, date: LocalDate): Language? =
    createQuery {
            sql(
                """
SELECT d.language
FROM placement pl
JOIN daycare d on d.id = pl.unit_id
WHERE pl.child_id = ${bind(childId)} AND daterange(pl.start_date, pl.end_date, '[]') @> ${bind(date)}
"""
            )
        }
        .exactlyOneOrNull<Language>()
