// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.user.EvakaUser
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

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

fun Database.Read.getPlacementDraftPlacements(childId: UUID): List<PlacementDraftPlacement> {
    data class QueryResult(
        val id: PlacementId,
        val type: PlacementType,
        val childId: UUID,
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

fun Database.Read.getPlacementsForChild(childId: UUID): List<Placement> {
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

fun Database.Read.getPlacementsForChildDuring(childId: UUID, start: LocalDate, end: LocalDate?): List<Placement> {
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
        .bindNullable("end", end)
        .mapTo<Placement>()
        .list()
}

fun Database.Read.getCurrentPlacementForChild(childId: UUID): Placement? {
    return createQuery(
        """
SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date, p.termination_requested_date, p.terminated_by
FROM placement p
WHERE p.child_id = :childId
AND daterange(p.start_date, p.end_date, '[]') @> now()::date
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<Placement>()
        .firstOrNull()
}

fun Database.Transaction.insertPlacement(
    type: PlacementType,
    childId: UUID,
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

fun Database.Transaction.updatePlacementStartDate(placementId: PlacementId, date: LocalDate) {
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
    createUpdate("UPDATE placement SET start_date = :start, end_date = :end WHERE id = :id")
        .bind("id", placementId)
        .bind("start", startDate)
        .bind("end", endDate)
        .execute()
}

fun Database.Transaction.cancelPlacement(id: PlacementId): Triple<UUID, LocalDate, LocalDate> {
    data class QueryResult(
        val childId: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    getPlacement(id) ?: throw NotFound("Placement $id not found")

    createUpdate(
        //language=SQL
        """DELETE FROM daycare_group_placement WHERE daycare_placement_id = :id""".trimIndent()
    )
        .bind("id", id)
        .execute()

    createUpdate(
        //language=SQL
        """DELETE FROM service_need WHERE placement_id = :id""".trimIndent()
    )
        .bind("id", id)
        .execute()

    return createUpdate(
        //language=SQL
        """DELETE FROM placement WHERE id = :id RETURNING child_id, start_date, end_date""".trimIndent()
    )
        .bind("id", id)
        .executeAndReturnGeneratedKeys()
        .mapTo<QueryResult>()
        .findFirst()
        .orElse(null)
        .let {
            Triple(
                it.childId,
                it.startDate,
                it.endDate
            )
        }
}

fun Database.Transaction.clearGroupPlacementsAfter(placementId: PlacementId, date: LocalDate) {
    createUpdate(
        //language=SQL
        """
            DELETE from daycare_group_placement
            WHERE daycare_placement_id = :id AND start_date > :date
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()

    createUpdate(
        //language=SQL
        """
            UPDATE daycare_group_placement SET end_date = :date
            WHERE daycare_placement_id = :id AND start_date <= :date AND end_date > :date
        """.trimIndent()
    )
        .bind("id", placementId)
        .bind("date", date)
        .execute()
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
}

fun Database.Read.getDaycarePlacements(
    daycareId: DaycareId?,
    childId: UUID?,
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
        .bindNullable("daycareId", daycareId)
        .bindNullable("childId", childId)
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

data class ChildPlacement(
    val childId: ChildId,
    val placementId: PlacementId,
    val placementType: PlacementType,
    val placementStartDate: LocalDate,
    val placementEndDate: LocalDate,
    val placementUnitName: String,
    val terminationRequestedDate: LocalDate?,
    @Nested("terminated_by")
    val terminatedBy: EvakaUser?,
    val currentGroupName: String
)

fun Database.Read.getCitizenChildPlacements(today: LocalDate, childId: UUID): List<ChildPlacement> = createQuery(
    """
SELECT
    child.id AS child_id,
    p.id AS placement_id,
    p.type AS placement_type,
    p.start_date AS placement_start_date,
    p.end_date AS placement_end_date,
    p.termination_requested_date,
    d.name AS placement_unit_name,
    COALESCE(dg.name, '') AS current_group_name,
    evaka_user.id as terminated_by_id,
    evaka_user.name as terminated_by_name,
    evaka_user.type as terminated_by_type
FROM placement p
JOIN daycare d ON p.unit_id = d.id
JOIN person child ON p.child_id = child.id
LEFT JOIN evaka_user ON p.terminated_by = evaka_user.id
LEFT JOIN daycare_group_placement dgp on dgp.daycare_placement_id = p.id AND daterange(dgp.start_date, dgp.end_date, '[]') @> :today
LEFT JOIN daycare_group dg ON dg.id = dgp.daycare_group_id
WHERE child.id = :childId            
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
    childId: UUID
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
        .mapTo<UUID>()
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
    // language=SQL
    val sql = "DELETE FROM daycare_group_placement WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<GroupPlacementId>()
        .firstOrNull() != null
}

private val toDaycarePlacement: (ResultSet, StatementContext) -> DaycarePlacement = { rs, ctx ->
    DaycarePlacement(
        id = PlacementId(rs.getUUID("placement_id")),
        child = ChildBasics(
            id = rs.getUUID("child_id"),
            socialSecurityNumber = rs.getString("child_ssn"),
            firstName = rs.getString("child_first_name"),
            lastName = rs.getString("child_last_name"),
            dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate()
        ),
        daycare = DaycareBasics(
            id = DaycareId(rs.getUUID("unit_id")),
            name = rs.getString("unit_name"),
            area = rs.getString("area_name"),
            providerType = rs.getEnum("provider_type"),
            enabledPilotFeatures = ctx.mapColumn<Array<PilotFeature>>(rs, "enabled_pilot_features").toList()
        ),
        startDate = rs.getDate("placement_start").toLocalDate(),
        endDate = rs.getDate("placement_end").toLocalDate(),
        type = rs.getEnum("placement_type")
    )
}
