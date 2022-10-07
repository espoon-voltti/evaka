// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.backupcare.deleteOrMoveConflictingBackupCares
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.result.RowView
import java.time.LocalDate

fun Database.Read.getPlacement(id: PlacementId): Placement? {
    return createQuery(
        """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by
FROM placement p
WHERE p.id = :id
        """.trimIndent()
    )
        .bind("id", id)
        .mapTo<Placement>()
        .first()
}

fun Database.Read.getPlacementDraftPlacements(childId: ChildId): List<PlacementDraftPlacement> {
    data class QueryResult(
        val id: PlacementId,
        val type: PlacementType,
        val childId: ChildId,
        val unitId: DaycareId,
        val unitName: String,
        val startDate: LocalDate,
        val endDate: LocalDate
    )
    return createQuery(
        """
        SELECT p.id, p.type, p.child_id, d.id AS unit_id, d.name AS unit_name, p.start_date, p.end_date
        FROM placement p
        LEFT JOIN daycare d on p.unit_id = d.id
        WHERE p.child_id = :childId
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<QueryResult>()
        .list()
        .map { r ->
            PlacementDraftPlacement(
                id = r.id,
                type = r.type,
                unit = PlacementDraftUnit(
                    r.unitId,
                    r.unitName
                ),
                childId = r.childId,
                startDate = r.startDate,
                endDate = r.endDate
            )
        }
}

fun Database.Read.getPlacementsForChild(childId: ChildId): List<Placement> {
    return createQuery(
        """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by
FROM placement p
WHERE p.child_id = :childId
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<Placement>()
        .list()
}

fun Database.Read.getPlacementsForChildDuring(childId: ChildId, start: LocalDate, end: LocalDate?): List<Placement> {
    return createQuery(
        """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by
FROM placement p
WHERE p.child_id = :childId
AND daterange(p.start_date, p.end_date, '[]') && daterange(:start, :end, '[]')
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("start", start)
        .bind("end", end)
        .mapTo<Placement>()
        .list()
}

fun Database.Read.getCurrentPlacementForChild(clock: EvakaClock, childId: ChildId): Placement? {
    return createQuery(
        """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by
FROM placement p
WHERE p.child_id = :childId
AND daterange(p.start_date, p.end_date, '[]') @> :today
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("today", clock.today())
        .mapTo<Placement>()
        .firstOrNull()
}

data class ChildPlacementType(
    val childId: ChildId,
    val unitId: DaycareId,
    val period: FiniteDateRange,
    val placementType: PlacementType
)

fun Database.Read.getChildPlacementTypesByRange(childId: ChildId, period: DateRange): List<ChildPlacementType> {
    return createQuery(
        """
SELECT
    child_id,
    unit_id,
    daterange(start_date, end_date, '[]') AS period,
    type AS placement_type
FROM placement
WHERE child_id = :childId
AND daterange(start_date, end_date, '[]') && :period
"""
    )
        .bind("childId", childId)
        .bind("period", period)
        .mapTo<ChildPlacementType>()
        .list()
}

fun Database.Transaction.insertPlacement(
    type: PlacementType,
    childId: ChildId,
    unitId: DaycareId,
    startDate: LocalDate,
    endDate: LocalDate
): Placement {
    return createQuery(
        """
            INSERT INTO placement (type, child_id, unit_id, start_date, end_date) 
            VALUES (:type::placement_type, :childId, :unitId, :startDate, :endDate)
            RETURNING *
        """.trimIndent()
    )
        .bind("type", type)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<Placement>()
        .list()
        .first()
}

data class PlacementChildAndRange(
    val childId: ChildId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val unitId: DaycareId
)

fun Database.Read.getPlacementChildAndRange(placementId: PlacementId) = createQuery(
    "SELECT child_id, start_date, end_date, unit_id FROM placement WHERE id = :id"
)
    .bind("id", placementId)
    .mapTo<PlacementChildAndRange>()
    .one()

fun Database.Transaction.updatePlacementStartDate(placementId: PlacementId, date: LocalDate) {
    val placement = getPlacementChildAndRange(placementId)
    deleteOrMoveConflictingBackupCares(
        placement.childId,
        FiniteDateRange(placement.startDate, placement.endDate),
        FiniteDateRange(date, placement.endDate)
    )

    createUpdate(
        //language=SQL
        """
            UPDATE placement SET start_date = :date WHERE id = :id
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()
}

fun Database.Transaction.updatePlacementEndDate(placementId: PlacementId, date: LocalDate) {
    val placement = getPlacementChildAndRange(placementId)
    deleteOrMoveConflictingBackupCares(
        placement.childId,
        FiniteDateRange(placement.startDate, placement.endDate),
        FiniteDateRange(placement.startDate, date)
    )

    createUpdate(
        //language=SQL
        """
            UPDATE placement SET end_date = :date WHERE id = :id
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()
}

fun Database.Transaction.updatePlacementStartAndEndDate(placementId: PlacementId, startDate: LocalDate, endDate: LocalDate) {
    val placement = getPlacementChildAndRange(placementId)
    deleteOrMoveConflictingBackupCares(
        placement.childId,
        FiniteDateRange(placement.startDate, placement.endDate),
        FiniteDateRange(startDate, endDate)
    )

    createUpdate("UPDATE placement SET start_date = :start, end_date = :end WHERE id = :id")
        .bind("id", placementId)
        .bind("start", startDate)
        .bind("end", endDate)
        .execute()
}

fun Database.Transaction.updatePlacementType(placementId: PlacementId, type: PlacementType) {
    createUpdate("UPDATE placement SET type = :type WHERE id = :id")
        .bind("id", placementId)
        .bind("type", type)
        .execute()
}

fun Database.Transaction.deleteServiceNeedsFromPlacement(placementId: PlacementId) {
    createUpdate("DELETE FROM service_need WHERE placement_id = :id")
        .bind("id", placementId)
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
    clearCalendarEventAttendees(placement.childId, placement.unitId, FiniteDateRange(placement.startDate, placement.endDate))

    createUpdate("DELETE FROM daycare_group_placement WHERE daycare_placement_id = :id")
        .bind("id", id)
        .execute()

    deleteServiceNeedsFromPlacement(id)

    deleteOrMoveConflictingBackupCares(
        placement.childId,
        FiniteDateRange(placement.startDate, placement.endDate),
        null
    )

    createUpdate("DELETE FROM placement WHERE id = :id")
        .bind("id", id)
        .execute()

    return CancelPlacementResult(placement.childId, placement.unitId, placement.startDate, placement.endDate)
}

fun Database.Transaction.clearGroupPlacementsAfter(placementId: PlacementId, date: LocalDate) {
    createUpdate(
        """
            DELETE from daycare_group_placement
            WHERE daycare_placement_id = :id AND start_date > :date
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()

    createUpdate(
        """
            UPDATE daycare_group_placement SET end_date = :date
            WHERE daycare_placement_id = :id AND start_date <= :date AND end_date > :date
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()

    val placement = getPlacement(placementId)

    if (placement != null) {
        clearCalendarEventAttendees(placement.childId, placement.unitId, FiniteDateRange(date, LocalDate.MAX))
    }
}

fun Database.Transaction.clearGroupPlacementsBefore(placementId: PlacementId, date: LocalDate) {
    createUpdate(
        //language=SQL
        """
        DELETE from daycare_group_placement
        WHERE daycare_placement_id = :id AND end_date < :date
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()

    createUpdate(
        //language=SQL
        """
            UPDATE daycare_group_placement SET start_date = :date
            WHERE daycare_placement_id = :id AND start_date < :date AND end_date >= :date
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()

    val placement = getPlacement(placementId)

    if (placement != null) {
        clearCalendarEventAttendees(placement.childId, placement.unitId, FiniteDateRange(LocalDate.MIN, date))
    }
}

fun Database.Transaction.clearCalendarEventAttendees(childId: ChildId, unitId: DaycareId, range: FiniteDateRange?) {
    createUpdate(
        //language=SQL
        """
            DELETE FROM calendar_event_attendee cea
            WHERE cea.child_id = :childId
              AND cea.unit_id = :unitId
              AND (
                :startDate IS NULL OR
                (SELECT lower(ce.period) FROM calendar_event ce WHERE ce.id = cea.calendar_event_id) >= :startDate
              )
              AND (
                :endDate IS NULL OR
                (SELECT upper(ce.period) FROM calendar_event ce WHERE ce.id = cea.calendar_event_id) < :endDate
              )
        """.trimIndent()
    )
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("startDate", range?.start?.takeIf { !it.isEqual(LocalDate.MIN) })
        .bind("endDate", range?.end?.takeIf { !it.isEqual(LocalDate.MAX) })
        .execute()

    // clear events that no longer have any attendees
    createUpdate(
        //language=SQL
        """
            DELETE FROM calendar_event ce
            WHERE NOT EXISTS(
                SELECT 1 FROM calendar_event_attendee cea WHERE cea.calendar_event_id = ce.id
            )
        """.trimIndent()
    ).execute()
}

fun Database.Read.getDaycarePlacements(
    daycareId: DaycareId?,
    childId: ChildId?,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<DaycarePlacementDetails> {
    // language=SQL
    val sql =
        """
        SELECT
            pl.id, pl.start_date, pl.end_date, pl.type, pl.child_id, pl.termination_requested_date, 
            terminated_by.id AS terminated_by_id,
            terminated_by.name AS terminated_by_name,
            terminated_by.type AS terminated_by_type,
            pl.unit_id AS daycare_id,
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
        WHERE daterange(pl.start_date, pl.end_date, '[]') && daterange(:from, :to, '[]')
        AND (:daycareId::uuid IS NULL OR pl.unit_id = :daycareId)
        AND (:childId::uuid IS NULL OR pl.child_id = :childId)
        """.trimIndent()

    return createQuery(sql)
        .bind("from", startDate)
        .bind("to", endDate)
        .bind("daycareId", daycareId)
        .bind("childId", childId)
        .mapTo<DaycarePlacementDetails>()
        .toList()
}

fun Database.Read.getDaycarePlacement(id: PlacementId): DaycarePlacement? {
    // language=SQL
    val sql =
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
        WHERE p.id = :id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .map(toDaycarePlacement)
        .firstOrNull()
}

fun Database.Read.getTerminatedPlacements(
    today: LocalDate,
    daycareId: DaycareId,
    terminationRequestedMinDate: LocalDate?,
    terminationRequestedMaxDate: LocalDate?
): List<TerminatedPlacement> = createQuery(
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
    dg.name AS current_daycare_group_name
    FROM placement pl
    JOIN daycare d on pl.unit_id = d.id
    JOIN person ch on pl.child_id = ch.id
    LEFT JOIN evaka_user terminated_by ON pl.terminated_by = terminated_by.id
    LEFT JOIN daycare_group_placement dgp ON pl.id = dgp.daycare_placement_id AND daterange(dgp.start_date, dgp.end_date, '[]') @> :today::date
    LEFT JOIN daycare_group dg ON dgp.daycare_group_id = dg.id
    WHERE (pl.unit_id = :daycareId)
    AND daterange(:terminationRequestedMinDate, :terminationRequestedMaxDate, '[]') @> pl.termination_requested_date 
    """.trimIndent()
)
    .bind("today", today)
    .bind("daycareId", daycareId)
    .bind("terminationRequestedMinDate", terminationRequestedMinDate)
    .bind("terminationRequestedMaxDate", terminationRequestedMaxDate)
    .mapTo<TerminatedPlacement>()
    .list()

data class TerminatedPlacement(
    val id: PlacementId,
    val endDate: LocalDate,
    val type: PlacementType,
    val terminationRequestedDate: LocalDate?,
    @Nested("child")
    val child: ChildBasics,
    @Nested("terminated_by")
    val terminatedBy: EvakaUser?,
    var currentDaycareGroupName: String?
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
    @Nested("terminated_by")
    val terminatedBy: EvakaUser?,
)

fun Database.Read.getCitizenChildPlacements(today: LocalDate, childId: ChildId): List<ChildPlacement> = createQuery(
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
    p.child_id = :childId
    AND p.end_date >= :today::date
    """.trimIndent()
)
    .bind("childId", childId)
    .bind("today", today)
    .mapTo<ChildPlacement>()
    .list()

fun Database.Read.getDaycareGroupPlacement(id: GroupPlacementId): DaycareGroupPlacement? {
    // language=SQL
    val sql =
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
        WHERE gp.id = :id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<DaycareGroupPlacement>()
        .firstOrNull()
}

fun Database.Read.getIdenticalPrecedingGroupPlacement(
    daycarePlacementId: PlacementId,
    groupId: GroupId,
    startDate: LocalDate
): DaycareGroupPlacement? {
    // language=SQL
    val sql =
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
        WHERE daycare_placement_id = :placementId AND daycare_group_id = :groupId AND gp.end_date = :endDate
        """.trimIndent()

    return createQuery(sql)
        .bind("placementId", daycarePlacementId)
        .bind("groupId", groupId)
        .bind("endDate", startDate.minusDays(1))
        .mapTo<DaycareGroupPlacement>()
        .firstOrNull()
}

fun Database.Read.getIdenticalPostcedingGroupPlacement(
    daycarePlacementId: PlacementId,
    groupId: GroupId,
    endDate: LocalDate
): DaycareGroupPlacement? {
    // language=SQL
    val sql =
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
        WHERE daycare_placement_id = :placementId AND daycare_group_id = :groupId AND gp.start_date = :startDate
        """.trimIndent()

    return createQuery(sql)
        .bind("placementId", daycarePlacementId)
        .bind("groupId", groupId)
        .bind("startDate", endDate.plusDays(1))
        .mapTo<DaycareGroupPlacement>()
        .firstOrNull()
}

fun Database.Read.hasGroupPlacements(groupId: GroupId): Boolean = createQuery(
    "SELECT EXISTS (SELECT 1 FROM daycare_group_placement WHERE daycare_group_id = :groupId)"
)
    .bind("groupId", groupId)
    .mapTo<Boolean>()
    .one()

fun Database.Read.getGroupPlacementsAtDaycare(daycareId: DaycareId, placementRange: DateRange): List<DaycareGroupPlacement> {
    // language=SQL
    val sql =
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
            AND p.unit_id = :daycareId
            AND daterange(p.start_date, p.end_date, '[]') && :placementRange
        )
        """.trimIndent()

    return createQuery(sql)
        .bind("daycareId", daycareId)
        .bind("placementRange", placementRange)
        .mapTo<DaycareGroupPlacement>()
        .toList()
}

fun Database.Read.getChildGroupPlacements(
    childId: ChildId
): List<DaycareGroupPlacement> {
    // language=SQL
    val sql =
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
        WHERE pl.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<DaycareGroupPlacement>()
        .toList()
}

fun Database.Read.getGroupPlacementChildren(
    groupId: GroupId,
    range: FiniteDateRange
): List<ChildId> {
    // language=SQL
    val sql =
        """
        SELECT DISTINCT pl.child_id
        FROM daycare_group_placement gp
        JOIN placement pl ON pl.id = gp.daycare_placement_id
        WHERE gp.daycare_group_id = :groupId AND daterange(gp.start_date, gp.end_date, '[]') && :range
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("range", range)
        .mapTo<ChildId>()
        .toList()
}

fun Database.Transaction.createGroupPlacement(
    placementId: PlacementId,
    groupId: GroupId,
    startDate: LocalDate,
    endDate: LocalDate
): GroupPlacementId {
    // language=SQL
    val sql =
        """
        INSERT INTO daycare_group_placement (daycare_placement_id, daycare_group_id, start_date, end_date)
        VALUES (:placementId, :groupId, :startDate, :endDate)
        RETURNING id
        """.trimIndent()

    return createQuery(sql)
        .bind("placementId", placementId)
        .bind("groupId", groupId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<GroupPlacementId>()
        .one()
}

fun Database.Transaction.updateGroupPlacementStartDate(id: GroupPlacementId, startDate: LocalDate): Boolean {
    // language=SQL
    val sql = "UPDATE daycare_group_placement SET start_date = :startDate WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .mapTo<GroupPlacementId>()
        .firstOrNull() != null
}

fun Database.Transaction.updateGroupPlacementEndDate(id: GroupPlacementId, endDate: LocalDate): Boolean {
    // language=SQL
    val sql = "UPDATE daycare_group_placement SET end_date = :endDate WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .bind("endDate", endDate)
        .mapTo<GroupPlacementId>()
        .firstOrNull() != null
}

fun Database.Transaction.deleteGroupPlacement(id: GroupPlacementId): Boolean {
    val dgPlacement = getDaycareGroupPlacement(id)
    if (dgPlacement != null) {
        val placement = getPlacement(dgPlacement.daycarePlacementId)

        if (placement != null) {
            clearCalendarEventAttendees(placement.childId, placement.unitId, null)
        }
    }

    // language=SQL
    val sql = "DELETE FROM daycare_group_placement WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<GroupPlacementId>()
        .firstOrNull() != null
}

private val toDaycarePlacement: (RowView) -> DaycarePlacement = { row ->
    DaycarePlacement(
        id = row.mapColumn("placement_id"),
        child = ChildBasics(
            id = ChildId(row.mapColumn("child_id")),
            socialSecurityNumber = row.mapColumn("child_ssn"),
            firstName = row.mapColumn("child_first_name"),
            lastName = row.mapColumn("child_last_name"),
            dateOfBirth = row.mapColumn("child_date_of_birth")
        ),
        daycare = DaycareBasics(
            id = DaycareId(row.mapColumn("unit_id")),
            name = row.mapColumn("unit_name"),
            area = row.mapColumn("area_name"),
            providerType = row.mapColumn("provider_type"),
            enabledPilotFeatures = row.mapColumn("enabled_pilot_features"),
            language = row.mapColumn("language")
        ),
        startDate = row.mapColumn("placement_start"),
        endDate = row.mapColumn("placement_end"),
        type = row.mapColumn("placement_type")
    )
}

fun Database.Transaction.deleteServiceNeedsFromPlacementAfter(id: PlacementId, date: LocalDate) {
    createUpdate(
        """
DELETE FROM service_need
WHERE placement_id = :placementId
    AND start_date > :date
        """.trimIndent()
    )
        .bind("placementId", id)
        .bind("date", date)
        .execute()

    createUpdate(
        """
UPDATE service_need
SET end_date = :date
WHERE placement_id = :placementId
    AND daterange(start_date, end_date, '[]') @> :date::date
        """.trimIndent()
    )
        .bind("placementId", id)
        .bind("date", date)
        .execute()
}

fun Database.Transaction.terminatePlacementFrom(terminationRequestedDate: LocalDate, placementId: PlacementId, terminationDate: LocalDate, terminatedBy: EvakaUserId?) {
    clearGroupPlacementsAfter(placementId, terminationDate)
    deleteServiceNeedsFromPlacementAfter(placementId, terminationDate)

    val placement = getPlacementChildAndRange(placementId)
    deleteOrMoveConflictingBackupCares(
        placement.childId,
        FiniteDateRange(placement.startDate, placement.endDate),
        FiniteDateRange(placement.startDate, terminationDate)
    )

    createUpdate(
        """
UPDATE placement
SET termination_requested_date = :terminationRequestedDate,
    terminated_by = :terminationRequestedBy,
    end_date = :placementTerminationDate
WHERE id = :placementId
        """.trimIndent()
    )
        .bind("terminationRequestedDate", if (terminatedBy == null) null else terminationRequestedDate)
        .bind("terminationRequestedBy", terminatedBy)
        .bind("placementTerminationDate", terminationDate)
        .bind("placementId", placementId)
        .execute()
}

fun Database.Read.childPlacementsHasConsecutiveRange(childId: ChildId, range: FiniteDateRange): Boolean = createQuery(
    // language=SQL
    """
    SELECT (
        SELECT range_agg(daterange(start_date, end_date, '[]')) FROM placement
        WHERE child_id = :childId
    ) @> :range
    """.trimIndent()
)
    .bind("childId", childId)
    .bind("range", range)
    .mapTo<Boolean>()
    .one()
