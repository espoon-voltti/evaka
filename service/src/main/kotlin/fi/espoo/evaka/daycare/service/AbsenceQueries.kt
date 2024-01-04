// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimeRow
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.mapper.Nested

data class AbsenceUpsert(
    val childId: ChildId,
    val date: LocalDate,
    val category: AbsenceCategory,
    val absenceType: AbsenceType
)

/** Fails if any of the absences already exists */
fun Database.Transaction.insertAbsences(
    now: HelsinkiDateTime,
    userId: EvakaUserId,
    absences: List<AbsenceUpsert>
) {
    val sql =
        """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by, modified_at)
        VALUES (:childId, :date, :category, :absenceType, :userId, :now)
        """

    val batch = prepareBatch(sql)
    for (absence in absences) {
        batch.bindKotlin(absence).bind("userId", userId).bind("now", now).add()
    }

    batch.execute()
}

/** Updates the details if an absence already exists */
fun Database.Transaction.upsertAbsences(
    now: HelsinkiDateTime,
    userId: EvakaUserId,
    absences: List<AbsenceUpsert>
): List<AbsenceId> {
    val sql =
        """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by, modified_at)
        VALUES (:childId, :date, :category, :absenceType, :userId, :now)
        ON CONFLICT (child_id, date, category)
        DO UPDATE SET absence_type = :absenceType, modified_by = :userId, modified_at = :now
        RETURNING id
        """
            .trimIndent()

    val batch = prepareBatch(sql)
    for (absence in absences) {
        batch.bindKotlin(absence).bind("userId", userId).bind("now", now).add()
    }

    return batch.executeAndReturn().toList<AbsenceId>()
}

/** If the absence already exists, updates only if was generated */
fun Database.Transaction.upsertGeneratedAbsences(
    now: HelsinkiDateTime,
    absences: List<AbsenceUpsert>
): List<AbsenceId> {
    val sql =
        """
        INSERT INTO absence AS a (child_id, date, category, absence_type, modified_by, modified_at)
        VALUES (:childId, :date, :category, :absenceType, :userId, :now)
        ON CONFLICT (child_id, date, category)
        DO UPDATE SET absence_type = :absenceType, modified_at = :now WHERE a.modified_by = :userId
        RETURNING id
        """
            .trimIndent()

    val batch = prepareBatch(sql)
    for (absence in absences) {
        batch
            .bindKotlin(absence)
            .bind("userId", AuthenticatedUser.SystemInternalUser.evakaUserId)
            .bind("now", now)
            .add()
    }

    return batch.executeAndReturn().toList<AbsenceId>()
}

data class Presence(val childId: ChildId, val date: LocalDate, val category: AbsenceCategory)

fun Database.Transaction.batchDeleteAbsences(deletions: List<Presence>): List<AbsenceId> {
    val sql =
        """
        DELETE FROM absence
        WHERE category = :category AND date = :date AND child_id = :childId
        RETURNING id
        """
            .trimIndent()

    val batch = prepareBatch(sql)
    deletions.forEach { batch.bindKotlin(it).add() }
    return batch.executeAndReturn().toList<AbsenceId>()
}

fun Database.Transaction.deleteAbsencesFromHolidayPeriodDates(
    deletions: List<Pair<ChildId, LocalDate>>
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
        DELETE FROM absence
        WHERE child_id = :childId
        AND date = :date
        AND EXISTS (SELECT 1 FROM holiday_period WHERE period @> date)
        RETURNING id
    """
        )
    deletions.forEach { batch.bind("childId", it.first).bind("date", it.second).add() }
    return batch.executeAndReturn().toList<AbsenceId>()
}

data class HolidayReservationCreate(
    val childId: ChildId,
    val date: LocalDate,
)

fun Database.Transaction.addMissingHolidayReservations(
    createdBy: EvakaUserId,
    additions: List<HolidayReservationCreate>
) {
    val batch =
        prepareBatch(
            """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, NULL, NULL, :createdBy
        WHERE
            EXISTS (SELECT 1 FROM holiday_period WHERE period @> :date) AND
            NOT EXISTS (SELECT 1 FROM attendance_reservation WHERE child_id = :childId AND date = :date)
        """
        )
    additions.forEach {
        batch.bind("childId", it.childId).bind("date", it.date).bind("createdBy", createdBy).add()
    }
    batch.execute()
}

fun Database.Transaction.deleteChildAbsences(childId: ChildId, date: LocalDate): List<AbsenceId> =
    createUpdate("DELETE FROM absence WHERE child_id = :childId AND date = :date RETURNING id")
        .bind("childId", childId)
        .bind("date", date)
        .executeAndReturnGeneratedKeys()
        .toList<AbsenceId>()

fun Database.Transaction.deleteNonSystemGeneratedAbsencesByCategoryInRange(
    childId: ChildId,
    range: DateRange,
    category: AbsenceCategory
): List<AbsenceId> =
    createQuery(
            """
DELETE FROM absence
WHERE
    child_id = :childId AND
    between_start_and_end(:range, date) AND
    category = :category AND
    modified_by != :systemUserId
RETURNING id
"""
        )
        .bind("childId", childId)
        .bind("range", range)
        .bind("category", category)
        .bind("systemUserId", AuthenticatedUser.SystemInternalUser.evakaUserId)
        .toList<AbsenceId>()

fun Database.Transaction.deleteOldGeneratedAbsencesInRange(
    now: HelsinkiDateTime,
    childId: ChildId,
    range: DateRange
): List<AbsenceId> =
    createQuery(
            """
DELETE FROM absence
WHERE
    child_id = :childId AND
    between_start_and_end(:range, date) AND
    modified_by = :systemUserId AND
    modified_at < :now
RETURNING id
"""
        )
        .bind("childId", childId)
        .bind("range", range)
        .bind("systemUserId", AuthenticatedUser.SystemInternalUser.evakaUserId)
        .bind("now", now)
        .toList<AbsenceId>()

/**
 * A citizen is allowed to edit:
 * - absences added by a citizen
 * - other than FREE_ABSENCE
 */
fun Database.Transaction.clearOldCitizenEditableAbsences(
    childDatePairs: List<Pair<ChildId, LocalDate>>,
    reservableRange: FiniteDateRange? = null
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
DELETE FROM absence a
WHERE child_id = :childId
AND date = :date
AND absence_type <> 'FREE_ABSENCE'::absence_type
${if (reservableRange != null) """
AND (:reservableRange @> date OR absence_type <> 'PLANNED_ABSENCE'::absence_type OR category = 'NONBILLABLE' OR NOT EXISTS (
    SELECT
    FROM service_need_option sno
    JOIN service_need sn ON sn.option_id = sno.id AND a.date BETWEEN sn.start_date AND sn.end_date
    JOIN placement p ON p.id = sn.placement_id AND p.child_id = a.child_id AND a.date BETWEEN p.start_date AND p.end_date
    WHERE sno.contract_days_per_month IS NOT NULL
))""" else ""}
AND modified_by IN (SELECT id FROM evaka_user where type = 'CITIZEN')
RETURNING id
"""
        )

    childDatePairs.forEach { (childId, date) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .bind("reservableRange", reservableRange)
            .add()
    }

    return batch.executeAndReturn().toList<AbsenceId>()
}

fun Database.Transaction.clearOldAbsences(
    childDatePairs: List<Pair<ChildId, LocalDate>>
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
DELETE FROM absence a
WHERE child_id = :childId
AND date = :date
RETURNING id
"""
        )

    childDatePairs.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    return batch.executeAndReturn().toList<AbsenceId>()
}

fun Database.Transaction.deleteAllCitizenEditableAbsencesInRange(range: FiniteDateRange) {
    createUpdate(
            """
DELETE FROM absence
WHERE between_start_and_end(:range, date)
AND absence_type <> 'FREE_ABSENCE'::absence_type
AND modified_by IN (SELECT id FROM evaka_user where type = 'CITIZEN')
"""
        )
        .bind("range", range)
        .execute()
}

fun Database.Read.getGroupName(groupId: GroupId): String? {
    val sql =
        """
            SELECT daycare_group.name
            FROM daycare_group
            WHERE id = :groupId;
        """
            .trimIndent()

    return createQuery(sql).bind("groupId", groupId).exactlyOneOrNull<String>()
}

fun Database.Read.getDaycareIdByGroup(groupId: GroupId): DaycareId {
    val sql =
        """
            SELECT daycare.id
            FROM daycare_group
                LEFT JOIN daycare ON daycare_group.daycare_id = daycare.id
            WHERE daycare_group.id = :groupId;
        """
            .trimIndent()

    return createQuery(sql).bind("groupId", groupId).exactlyOne<DaycareId>()
}

// language=sql
private const val placementsQuery =
    """
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

data class Child(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

fun Database.Read.getPlacementsByRange(
    groupId: GroupId,
    range: FiniteDateRange
): Map<Child, List<AbsencePlacement>> {
    data class QueryResult(
        @Nested("child") val child: Child,
        val dateRange: FiniteDateRange,
        val type: PlacementType
    )
    val sql =
        """
WITH all_placements AS (
  $placementsQuery
)
SELECT
  all_placements.date_range,
  all_placements.type,
  all_placements.child_id,
  person.first_name AS child_first_name,
  person.last_name AS child_last_name,
  person.date_of_birth AS child_date_of_birth
FROM all_placements
JOIN person ON child_id = person.id
"""

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("dateRange", range)
        .toList<QueryResult>()
        .groupBy { it.child }
        .map { (child, queryResults) ->
            child to queryResults.map { AbsencePlacement(it.dateRange, it.type) }
        }
        .toMap()
}

fun Database.Read.getAbsencesInGroupByRange(
    groupId: GroupId,
    range: FiniteDateRange
): Map<Pair<ChildId, LocalDate>, List<AbsenceWithModifierInfo>> {
    val sql =
        """
        SELECT a.child_id, a.date, a.category, a.absence_type, eu.type AS modified_by_type, a.modified_at AS modified_at
        FROM absence a
        LEFT JOIN evaka_user eu ON eu.id = a.modified_by
        WHERE child_id IN (SELECT child_id FROM ($placementsQuery) p)
        AND between_start_and_end(:dateRange, date)
        """
            .trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("dateRange", range)
        .toList {
            val childId: ChildId = column("child_id")
            val date: LocalDate = column("date")
            val absence: AbsenceWithModifierInfo = row()
            Pair(childId, date) to absence
        }
        .groupBy({ it.first }, { it.second })
}

fun Database.Read.getAbsencesOfChildByRange(childId: ChildId, range: DateRange): List<Absence> {
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.category, a.absence_type
        FROM absence a
        WHERE between_start_and_end(:range, date)
        AND a.child_id = :childId
        """
            .trimIndent()

    return createQuery(sql).bind("childId", childId).bind("range", range).toList<Absence>()
}

data class ChildAbsenceDateRow(val childId: ChildId, val date: LocalDate)

fun Database.Read.getAbsenceDatesForChildrenInRange(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, Set<LocalDate>> {
    return createQuery(
            """
            SELECT a.child_id, a.date
            FROM absence a
            WHERE between_start_and_end(:range, date)
            AND a.child_id = ANY(:childIds)
            """
        )
        .bind("childIds", childIds)
        .bind("range", range)
        .toList<ChildAbsenceDateRow>()
        .groupBy({ it.childId }, { it.date })
        .mapValues { (_, dates) -> dates.toSet() }
}

fun Database.Read.getBackupCaresAffectingGroup(
    groupId: GroupId,
    period: FiniteDateRange
): Map<ChildId, List<FiniteDateRange>> =
    createQuery(
            """
SELECT bc.child_id, daterange(bc.start_date, bc.end_date, '[]') AS period
FROM daycare_group_placement AS gp
JOIN placement ON daycare_placement_id = placement.id
JOIN backup_care AS bc ON bc.child_id = placement.child_id
WHERE daycare_group_id = :groupId
AND (bc.group_id IS NULL OR bc.group_id != gp.daycare_group_id)
AND daterange(gp.start_date, gp.end_date, '[]') && :period
AND daterange(bc.start_date, bc.end_date, '[]') && :period
"""
        )
        .bind("groupId", groupId)
        .bind("period", period)
        .toList { column<ChildId>("child_id") to column<FiniteDateRange>("period") }
        .groupBy({ it.first }, { it.second })

data class ChildReservation(
    val reservation: Reservation,
    val createdByEvakaUserType: EvakaUserType,
    val created: HelsinkiDateTime
)

fun Database.Read.getGroupReservations(
    groupId: GroupId,
    dateRange: FiniteDateRange
): Map<Pair<ChildId, LocalDate>, List<ChildReservation>> {
    return createQuery(
            """
WITH all_placements AS (
  $placementsQuery
)
SELECT r.child_id, r.date, r.start_time, r.end_time, e.type AS created_by_evaka_user_type, r.created AS created_date
FROM attendance_reservation r
JOIN evaka_user e ON r.created_by = e.id
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
        .toList {
            val childId: ChildId = column("child_id")
            val date: LocalDate = column("date")
            val reservation =
                ChildReservation(
                    Reservation.fromLocalTimes(column("start_time"), column("end_time")),
                    column("created_by_evaka_user_type"),
                    column("created_date")
                )
            Pair(childId, date) to reservation
        }
        .groupBy({ it.first }, { it.second })
}

data class ChildAttendance(
    val childId: ChildId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime
)

fun Database.Read.getGroupAttendances(
    groupId: GroupId,
    dateRange: FiniteDateRange
): List<ChildAttendance> =
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
        .toList<ChildAttendance>()

fun Database.Read.getGroupDailyServiceTimes(
    groupId: GroupId,
    dateRange: FiniteDateRange
): Map<ChildId, List<DailyServiceTimes>> =
    createQuery(
            """
WITH all_placements AS (
  $placementsQuery
)
SELECT
    id,
    child_id,
    validity_period,
    type,
    regular_times,
    monday_times,
    tuesday_times,
    wednesday_times,
    thursday_times,
    friday_times,
    saturday_times,
    sunday_times
FROM daily_service_time dst
WHERE EXISTS (SELECT 1 FROM all_placements p WHERE dst.child_id = p.child_id)
"""
        )
        .bind("groupId", groupId)
        .bind("dateRange", dateRange)
        .mapTo<DailyServiceTimeRow>()
        .useIterable { rows -> rows.map { toDailyServiceTimes(it) }.groupBy { it.childId } }
