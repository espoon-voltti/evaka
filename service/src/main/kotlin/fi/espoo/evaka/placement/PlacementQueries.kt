// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PGConstants
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

// language=SQL
private val placementSelector =
    """
        SELECT p.id, p.type, p.child_id, p.unit_id, p.start_date, p.end_date
        FROM placement p
    """.trimIndent()

fun Database.Read.getPlacement(id: PlacementId): Placement? {
    return createQuery("$placementSelector WHERE p.id = :id")
        .bind("id", id)
        .mapTo<Placement>()
        .first()
}

fun Database.Read.getPlacementDraftPlacements(childId: UUID): List<PlacementDraftPlacement> {
    data class QueryResult(
        val id: UUID,
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
    return createQuery("$placementSelector WHERE p.child_id = :childId")
        .bind("childId", childId)
        .mapTo<Placement>()
        .list()
}

fun Database.Read.getPlacementsForChildDuring(childId: UUID, start: LocalDate, end: LocalDate?): List<Placement> {
    return createQuery("$placementSelector WHERE p.child_id = :childId AND daterange(p.start_date, p.end_date, '[]') && daterange(:start, :end, '[]')")
        .bind("childId", childId)
        .bind("start", start)
        .bindNullable("end", end)
        .mapTo<Placement>()
        .list()
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
            pl.id, pl.start_date, pl.end_date, pl.type, pl.child_id, pl.unit_id,
            d.name AS daycare_name, d.provider_type, a.name AS area_name,
            ch.first_name, ch.last_name, ch.social_security_number, ch.date_of_birth,
            CASE
                WHEN (SELECT every(default_option) FROM service_need_option WHERE valid_placement_type = pl.type) THEN 0
                ELSE (
                    SELECT count(*)
                    FROM generate_series(greatest(pl.start_date, '2020-03-01'::date), pl.end_date, '1 day') t
                    LEFT JOIN service_need sn ON pl.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
                    WHERE sn.id IS NULL
                )
            END AS missing_service_need
        FROM placement pl
        LEFT OUTER JOIN daycare d on pl.unit_id = d.id
        LEFT OUTER JOIN person ch on pl.child_id = ch.id
        LEFT JOIN care_area a ON d.care_area_id = a.id
        WHERE pl.start_date <= :to AND pl.end_date >= :from
        AND (:daycareId::uuid IS NULL OR pl.unit_id = :daycareId)
        AND (:childId::uuid IS NULL OR pl.child_id = :childId)
        """.trimIndent()

    return createQuery(sql)
        .bind("from", (startDate ?: PGConstants.minDate))
        .bind("to", (endDate ?: PGConstants.maxDate))
        .bindNullable("daycareId", daycareId)
        .bindNullable("childId", childId)
        .map(toDaycarePlacementDetails)
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

fun Database.Read.getDaycareGroupPlacements(
    daycareId: DaycareId,
    startDate: LocalDate?,
    endDate: LocalDate?,
    groupId: GroupId? = null
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
        WHERE (:groupId::uuid IS NULL OR daycare_group_id = :groupId)
        AND EXISTS (
            SELECT 1 FROM placement p
            WHERE p.id = daycare_placement_id
            AND p.unit_id = :daycareId
            AND daterange(p.start_date, p.end_date, '[]') && daterange(:startDate, :endDate, '[]')
        )
        """.trimIndent()

    return createQuery(sql)
        .bind("daycareId", daycareId)
        .bind("startDate", startDate ?: PGConstants.minDate)
        .bind("endDate", endDate ?: PGConstants.maxDate)
        .bindNullable("groupId", groupId)
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

private val toDaycarePlacement: (ResultSet, StatementContext) -> DaycarePlacement = { rs, _ ->
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
            providerType = rs.getEnum("provider_type")
        ),
        startDate = rs.getDate("placement_start").toLocalDate(),
        endDate = rs.getDate("placement_end").toLocalDate(),
        type = rs.getEnum("placement_type")
    )
}

private val toDaycarePlacementDetails: (ResultSet, StatementContext) -> DaycarePlacementDetails = { rs, _ ->
    DaycarePlacementDetails(
        id = PlacementId(rs.getUUID("id")),
        child = ChildBasics(
            id = rs.getUUID("child_id"),
            socialSecurityNumber = rs.getString("social_security_number"),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            dateOfBirth = rs.getDate("date_of_birth").toLocalDate()
        ),
        daycare = DaycareBasics(
            id = DaycareId(rs.getUUID("unit_id")),
            name = rs.getString("daycare_name"),
            area = rs.getString("area_name"),
            providerType = rs.getEnum("provider_type")
        ),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date").toLocalDate(),
        type = rs.getEnum("type"),
        missingServiceNeedDays = rs.getInt("missing_service_need")
    )
}
