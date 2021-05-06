// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

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

fun Database.Read.getPlacement(id: UUID): Placement? {
    // language=SQL
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
        val unitId: UUID,
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
    unitId: UUID,
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

fun Database.Transaction.updatePlacementStartDate(placementId: UUID, date: LocalDate) {
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

fun Database.Transaction.updatePlacementEndDate(placementId: UUID, date: LocalDate) {
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

fun Database.Transaction.updatePlacementStartAndEndDate(placementId: UUID, startDate: LocalDate, endDate: LocalDate) {
    createUpdate("UPDATE placement SET start_date = :start, end_date = :end WHERE id = :id")
        .bind("id", placementId)
        .bind("start", startDate)
        .bind("end", endDate)
        .execute()
}

fun Database.Transaction.cancelPlacement(id: UUID): Triple<UUID, LocalDate, LocalDate> {
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
        """DELETE FROM new_service_need WHERE placement_id = :id""".trimIndent()
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

fun Database.Transaction.clearGroupPlacementsAfter(placementId: UUID, date: LocalDate) {
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

fun Database.Transaction.clearGroupPlacementsBefore(placementId: UUID, date: LocalDate) {
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
    daycareId: UUID?,
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
            (
                SELECT count(*)
                FROM generate_series(greatest(pl.start_date, '2020-03-01'::date), pl.end_date, '1 day') t
                LEFT OUTER JOIN service_need sn
                    ON sn.child_id = pl.child_id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
                WHERE sn.id IS NULL
            ) AS missing_service_need,
            CASE
                WHEN (SELECT every(default_option) FROM service_need_option WHERE valid_placement_type = pl.type) THEN 0
                ELSE (
                    SELECT count(*)
                    FROM generate_series(greatest(pl.start_date, '2020-03-01'::date), pl.end_date, '1 day') t
                    LEFT JOIN new_service_need sn ON pl.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
                    WHERE sn.id IS NULL
                )
            END AS missing_new_service_need
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

fun Database.Read.getDaycarePlacement(id: UUID): DaycarePlacement? {
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

fun Database.Read.getDaycareGroupPlacement(id: UUID): DaycareGroupPlacement? {
    // language=SQL
    val sql =
        """
        SELECT id, daycare_group_id AS group_id, daycare_placement_id, start_date, end_date
        FROM daycare_group_placement
        WHERE id = :id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<DaycareGroupPlacement>()
        .firstOrNull()
}

fun Database.Read.getIdenticalPrecedingGroupPlacement(
    daycarePlacementId: UUID,
    groupId: UUID,
    startDate: LocalDate
): DaycareGroupPlacement? {
    // language=SQL
    val sql =
        """
        SELECT id, daycare_group_id AS group_id, daycare_placement_id, start_date, end_date
        FROM daycare_group_placement
        WHERE daycare_placement_id = :placementId AND daycare_group_id = :groupId AND end_date = :endDate
        """.trimIndent()

    return createQuery(sql)
        .bind("placementId", daycarePlacementId)
        .bind("groupId", groupId)
        .bind("endDate", startDate.minusDays(1))
        .mapTo<DaycareGroupPlacement>()
        .firstOrNull()
}

fun Database.Read.getIdenticalPostcedingGroupPlacement(
    daycarePlacementId: UUID,
    groupId: UUID,
    endDate: LocalDate
): DaycareGroupPlacement? {
    // language=SQL
    val sql =
        """
        SELECT id, daycare_group_id AS group_id, daycare_placement_id, start_date, end_date
        FROM daycare_group_placement
        WHERE daycare_placement_id = :placementId AND daycare_group_id = :groupId AND start_date = :startDate
        """.trimIndent()

    return createQuery(sql)
        .bind("placementId", daycarePlacementId)
        .bind("groupId", groupId)
        .bind("startDate", endDate.plusDays(1))
        .mapTo<DaycareGroupPlacement>()
        .firstOrNull()
}

fun Database.Read.getDaycareGroupPlacements(
    daycareId: UUID,
    startDate: LocalDate?,
    endDate: LocalDate?,
    groupId: UUID? = null
): List<DaycareGroupPlacement> {
    // language=SQL
    val sql =
        """
        SELECT id, daycare_group_id AS group_id, daycare_placement_id, start_date, end_date
        FROM daycare_group_placement
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
        SELECT dgp.id, dgp.daycare_group_id AS group_id, dgp.daycare_placement_id, dgp.start_date, dgp.end_date
        FROM daycare_group_placement dgp
        JOIN placement pl ON pl.id = dgp.daycare_placement_id
        WHERE pl.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<DaycareGroupPlacement>()
        .toList()
}

fun Database.Read.getMissingGroupPlacements(
    unitId: UUID
): List<MissingGroupPlacement> {
    // language=SQL
    val sql =
        """
        WITH gaps AS (
            SELECT placement.id AS placement_id, gap * daterange(placement.start_date, placement.end_date, '[]') AS gap
            FROM placement
            JOIN (
                SELECT
                    daycare_placement_id,
                    (CASE WHEN rank() OVER w = 1 THEN daterange(NULL, start_date, '()') END) AS gap
                FROM daycare_group_placement
                WINDOW w AS (PARTITION BY daycare_placement_id ORDER BY start_date)
        
                UNION ALL
        
                SELECT
                    daycare_placement_id,
                    nullif(daterange(end_date, lead(start_date) OVER w, '()'), 'empty') AS gap
                FROM daycare_group_placement
                WINDOW w AS (PARTITION BY daycare_placement_id ORDER BY start_date)
            ) all_gaps
            ON daycare_placement_id = placement.id
            AND NOT isempty(gap * daterange(placement.start_date, placement.end_date, '[]'))
        
            UNION ALL
        
            SELECT placement.id, daterange(placement.start_date, placement.end_date, '[]') AS gap
            FROM placement
            WHERE NOT EXISTS (SELECT 1 FROM daycare_group_placement WHERE daycare_placement_id = placement.id)
        )
        SELECT
            pl.id AS placement_id,
            pl.type AS placement_type,
            daterange(pl.start_date, pl.end_date, '[]') AS placement_period,
            c.id AS child_id,
            c.first_name,
            c.last_name,
            c.date_of_birth,
            g.gap,
            FALSE AS backup
        FROM gaps g
        JOIN placement pl ON pl.id = g.placement_id
        JOIN person c on c.id = pl.child_id
        WHERE g.gap && daterange('2020-03-01', NULL) AND pl.unit_id = :unitId
        
        UNION ALL 
        
        SELECT 
            bc.id AS placement_id,
            NULL AS placement_type,
            daterange(bc.start_date, bc.end_date, '[]') AS placement_period,
            c.id AS child_id,
            c.first_name,
            c.last_name,
            c.date_of_birth,
            daterange(bc.start_date, bc.end_date, '[]') AS gap,
            TRUE AS backup
        FROM backup_care bc
        JOIN person c on c.id = bc.child_id
        WHERE bc.end_date > '2020-03-01'::date AND bc.unit_id = :unitId AND bc.group_id IS NULL
        """.trimIndent()

    return createQuery(sql)
        .bind("unitId", unitId)
        .mapTo<MissingGroupPlacement>()
        .toList()
}

fun Database.Transaction.createGroupPlacement(
    placementId: UUID,
    groupId: UUID,
    startDate: LocalDate,
    endDate: LocalDate
): DaycareGroupPlacement {
    // language=SQL
    val sql =
        """
        INSERT INTO daycare_group_placement (daycare_placement_id, daycare_group_id, start_date, end_date)
        VALUES (:placementId, :groupId, :startDate, :endDate)
        RETURNING id, daycare_group_id AS group_id, daycare_placement_id, start_date, end_date
        """.trimIndent()

    return createQuery(sql)
        .bind("placementId", placementId)
        .bind("groupId", groupId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<DaycareGroupPlacement>()
        .first()
}

fun Database.Transaction.updateGroupPlacementStartDate(id: UUID, startDate: LocalDate): Boolean {
    // language=SQL
    val sql = "UPDATE daycare_group_placement SET start_date = :startDate WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Database.Transaction.updateGroupPlacementEndDate(id: UUID, endDate: LocalDate): Boolean {
    // language=SQL
    val sql = "UPDATE daycare_group_placement SET end_date = :endDate WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .bind("endDate", endDate)
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Database.Transaction.deleteGroupPlacement(id: UUID): Boolean {
    // language=SQL
    val sql = "DELETE FROM daycare_group_placement WHERE id = :id RETURNING id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<UUID>()
        .firstOrNull() != null
}

private val toDaycarePlacement: (ResultSet, StatementContext) -> DaycarePlacement = { rs, _ ->
    DaycarePlacement(
        id = rs.getUUID("placement_id"),
        child = ChildBasics(
            id = rs.getUUID("child_id"),
            socialSecurityNumber = rs.getString("child_ssn"),
            firstName = rs.getString("child_first_name"),
            lastName = rs.getString("child_last_name"),
            dateOfBirth = rs.getDate("child_date_of_birth").toLocalDate()
        ),
        daycare = DaycareBasics(
            id = rs.getUUID("unit_id"),
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
        id = rs.getUUID("id"),
        child = ChildBasics(
            id = rs.getUUID("child_id"),
            socialSecurityNumber = rs.getString("social_security_number"),
            firstName = rs.getString("first_name"),
            lastName = rs.getString("last_name"),
            dateOfBirth = rs.getDate("date_of_birth").toLocalDate()
        ),
        daycare = DaycareBasics(
            id = rs.getUUID("unit_id"),
            name = rs.getString("daycare_name"),
            area = rs.getString("area_name"),
            providerType = rs.getEnum("provider_type")
        ),
        startDate = rs.getDate("start_date").toLocalDate(),
        endDate = rs.getDate("end_date").toLocalDate(),
        type = rs.getEnum("type"),
        missingServiceNeedDays = rs.getInt("missing_service_need"),
        missingNewServiceNeedDays = rs.getInt("missing_new_service_need")
    )
}
