// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.backupcare.GroupBackupCare
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesWithId
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.mapRow
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.mapper.Nested
import java.time.LocalDate
import java.time.LocalTime

data class AbsenceUpsert(
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory,
    val absenceType: AbsenceType
)

fun Database.Transaction.upsertAbsences(clock: EvakaClock, absences: List<AbsenceUpsert>, modifiedBy: EvakaUserId): List<AbsenceId> {
    val sql =
        """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by)
        VALUES (:childId, :date, :category, :absenceType, :modifiedBy)
        ON CONFLICT (child_id, date, category)
        DO UPDATE SET absence_type = :absenceType, modified_by = :modifiedBy, modified_at = :now
        RETURNING id
        """.trimIndent()

    val batch = prepareBatch(sql)
    for (absence in absences) {
        batch
            .bindKotlin(absence)
            .bind("now", clock.now())
            .bind("modifiedBy", modifiedBy)
            .add()
    }

    return batch.executeAndReturn()
        .mapTo<AbsenceId>()
        .toList()
}

data class AbsenceDelete(
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory
)

fun Database.Transaction.batchDeleteAbsences(deletions: List<AbsenceDelete>): List<AbsenceId> {
    val sql =
        """
        DELETE FROM absence
        WHERE category = :category AND date = :date AND child_id = :childId
        RETURNING id
        """.trimIndent()

    val batch = prepareBatch(sql)
    deletions.forEach {
        batch.bindKotlin(it).add()
    }
    return batch.executeAndReturn()
        .mapTo<AbsenceId>()
        .toList()
}

fun Database.Transaction.deleteChildAbsences(childId: ChildId, date: LocalDate): List<AbsenceId> =
    createUpdate("DELETE FROM absence WHERE child_id = :childId AND date = :date RETURNING id")
        .bind("childId", childId)
        .bind("date", date)
        .executeAndReturnGeneratedKeys()
        .mapTo<AbsenceId>()
        .toList()

fun Database.Read.getGroupName(groupId: GroupId): String? {
    val sql =
        """
            SELECT daycare_group.name
            FROM daycare_group
            WHERE id = :groupId;
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .mapTo<String>()
        .firstOrNull()
}

fun Database.Read.getDaycareIdByGroup(groupId: GroupId): DaycareId {
    val sql =
        """
            SELECT daycare.id
            FROM daycare_group
                LEFT JOIN daycare ON daycare_group.daycare_id = daycare.id
            WHERE daycare_group.id = :groupId;
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .mapTo<DaycareId>()
        .first()
}

// language=sql
private const val placementsQuery = """
SELECT p.child_id, daterange(p.start_date, p.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') AS date_range, p.type
FROM daycare_group_placement AS gp
JOIN placement p ON gp.daycare_placement_id = p.id AND daterange(p.start_date, p.end_date, '[]') && daterange(gp.start_date, gp.end_date, '[]')
WHERE daterange(p.start_date, p.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') && :dateRange
AND gp.daycare_group_id = :groupId

UNION ALL

SELECT bc.child_id, daterange(p.start_date, p.end_date, '[]') * daterange(bc.start_date, bc.end_date, '[]') AS date_range, p.type
FROM backup_care bc
JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
WHERE daterange(p.start_date, p.end_date, '[]') * daterange(bc.start_date, bc.end_date, '[]') && :dateRange
AND group_id = :groupId
"""

fun Database.Read.getPlacementsByRange(
    groupId: GroupId,
    range: FiniteDateRange
): Map<Child, List<AbsencePlacement>> {
    data class QueryResult(
        @Nested("child")
        val child: Child,
        val dateRange: FiniteDateRange,
        val type: PlacementType
    )
    val sql =
        """
WITH all_placements AS (
  $placementsQuery
)
SELECT
  all_placements.*,
  person.first_name AS child_first_name,
  person.last_name AS child_last_name,
  person.date_of_birth AS child_date_of_birth
FROM all_placements
JOIN person ON child_id = person.id
"""

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("dateRange", range)
        .mapTo<QueryResult>()
        .toList()
        .groupBy { it.child }
        .map { (child, queryResults) ->
            child to queryResults.map { AbsencePlacement(it.dateRange, it.type.absenceCategories()) }
        }
        .toMap()
}

fun Database.Read.getAbsencesInGroupByRange(groupId: GroupId, range: FiniteDateRange): List<AbsenceWithModifierInfo> {
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.category, a.absence_type, eu.type AS modified_by_type, a.modified_at AS modified_at
        FROM absence a
        LEFT JOIN evaka_user eu ON eu.id = a.modified_by
        WHERE child_id IN (SELECT child_id FROM ($placementsQuery) p)
        AND between_start_and_end(:dateRange, date)
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("dateRange", range)
        .mapTo<AbsenceWithModifierInfo>()
        .list()
}

fun Database.Read.getAbsencesOfChildByRange(childId: ChildId, range: DateRange): List<Absence> {
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.category, a.absence_type
        FROM absence a
        WHERE between_start_and_end(:range, date)
        AND a.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("range", range)
        .mapTo<Absence>()
        .list()
}

fun Database.Read.getBackupCaresAffectingGroup(
    groupId: GroupId,
    period: FiniteDateRange
): List<GroupBackupCare> =
    createQuery(
        """
SELECT bc.id, bc.child_id, daterange(bc.start_date, bc.end_date, '[]') AS period
FROM daycare_group_placement AS gp
JOIN placement
ON daycare_placement_id = placement.id
JOIN backup_care AS bc
ON bc.child_id = placement.child_id
AND coalesce(bc.group_id != gp.daycare_group_id, true)
WHERE daycare_group_id = :groupId
AND daterange(gp.start_date, gp.end_date, '[]') && :period
"""
    )
        .bind("groupId", groupId)
        .bind("period", period)
        .mapTo<GroupBackupCare>()
        .list()

data class ChildReservation(
    val childId: ChildId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
)

fun Database.Read.getGroupReservations(groupId: GroupId, dateRange: FiniteDateRange): List<ChildReservation> =
    createQuery(
        """
WITH all_placements AS (
  $placementsQuery
)
SELECT r.child_id, r.date, r.start_time, r.end_time FROM attendance_reservation r
WHERE between_start_and_end(:dateRange, r.date)
AND EXISTS (
    SELECT 1 FROM all_placements p
    WHERE r.child_id = p.child_id
    AND between_start_and_end(p.date_range, r.date)
)
"""
    )
        .bind("groupId", groupId)
        .bind("dateRange", dateRange)
        .mapTo<ChildReservation>()
        .toList()

data class ChildAttendance(
    val childId: ChildId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
)

fun Database.Read.getGroupAttendances(groupId: GroupId, dateRange: FiniteDateRange): List<ChildAttendance> =
    createQuery(
        """
WITH all_placements AS (
  $placementsQuery
)
SELECT a.child_id, a.date, a.start_time, a.end_time FROM child_attendance a
WHERE between_start_and_end(:dateRange, a.date) AND a.end_time IS NOT NULL
AND EXISTS (
    SELECT 1 FROM all_placements p
    WHERE a.child_id = p.child_id
    AND between_start_and_end(p.date_range, a.date)
)
"""
    )
        .bind("groupId", groupId)
        .bind("dateRange", dateRange)
        .mapTo<ChildAttendance>()
        .toList()

fun Database.Read.getGroupDailyServiceTimes(
    groupId: GroupId,
    dateRange: FiniteDateRange
): Map<ChildId, List<DailyServiceTimesWithId>> = createQuery(
    """
WITH all_placements AS (
  $placementsQuery
)
SELECT st.* FROM daily_service_time st
WHERE EXISTS (SELECT 1 FROM all_placements p WHERE st.child_id = p.child_id)
"""
)
    .bind("groupId", groupId)
    .bind("dateRange", dateRange)
    .map { rv ->
        rv.mapColumn<ChildId>("child_id") to toDailyServiceTimes(rv.mapRow())
    }
    .toList()
    .groupBy { it.first }
    .mapValues { it.value.map { it.second } }
