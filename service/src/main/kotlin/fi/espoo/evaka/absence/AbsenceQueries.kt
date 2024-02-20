// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.absence

import fi.espoo.evaka.attendance.ChildAttendanceRow
import fi.espoo.evaka.attendance.getChildAttendances
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimeRow
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.Reservation
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUserType
import java.time.LocalDate
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
): List<AbsenceId> {
    val sql =
        """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by, modified_at)
        VALUES (:childId, :date, :category, :absenceType, :userId, :now)
        RETURNING id
        """

    val batch = prepareBatch(sql)
    for (absence in absences) {
        batch.bindKotlin(absence).bind("userId", userId).bind("now", now).add()
    }

    return batch.executeAndReturn().toList()
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

data class FullDayAbsenseUpsert(
    val childId: ChildId,
    val date: LocalDate,
    val absenceType: AbsenceType,
    val questionnaireId: HolidayQuestionnaireId? = null
)

/**
 * Creates absences for all absences categories of the child's placement on the given date ("full
 * day absence"). Does nothing if an absence already exists on the given date.
 */
fun Database.Transaction.upsertFullDayAbsences(
    userId: EvakaUserId,
    now: HelsinkiDateTime,
    absenceInserts: List<FullDayAbsenseUpsert>
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
        INSERT INTO absence (child_id, date, category, absence_type, modified_at, modified_by, questionnaire_id)
        SELECT
            :childId,
            :date,
            category,
            :absenceType,
            :now,
            :userId,
            :questionnaireId
        FROM (
            SELECT unnest(absence_categories(type)) AS category
            FROM placement
            WHERE child_id = :childId AND :date BETWEEN start_date AND end_date
        ) care_type
        ON CONFLICT DO NOTHING
        RETURNING id
        """
                .trimIndent()
        )

    absenceInserts.forEach { (childId, date, absenceType, questionnaireId) ->
        batch
            .bind("now", now)
            .bind("userId", userId)
            .bind("childId", childId)
            .bind("date", date)
            .bind("absenceType", absenceType)
            .bind("questionnaireId", questionnaireId)
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

fun Database.Transaction.deleteChildAbsences(
    childId: ChildId,
    date: LocalDate,
    categories: Set<AbsenceCategory> = AbsenceCategory.entries.toSet()
): List<AbsenceId> =
    createUpdate<Any> {
            sql(
                """
DELETE FROM absence
WHERE child_id = ${bind(childId)} AND date = ${bind(date)} AND category = ANY(${bind(categories)})
RETURNING id
    """
            )
        }
        .executeAndReturnGeneratedKeys()
        .toList()

fun Database.Transaction.deleteNonSystemGeneratedAbsencesByCategoryInRange(
    childId: ChildId,
    range: DateRange,
    categories: Set<AbsenceCategory>
): List<AbsenceId> =
    createQuery<Any> {
            sql(
                """
DELETE FROM absence
WHERE
    child_id = ${bind(childId)} AND
    between_start_and_end(${bind(range)}, date) AND
    category = ANY (${bind(categories)}::absence_category[]) AND
    modified_by != ${bind(AuthenticatedUser.SystemInternalUser.evakaUserId)}
RETURNING id
"""
            )
        }
        .toList()

fun Database.Transaction.deleteOldGeneratedAbsencesInRange(
    now: HelsinkiDateTime,
    childId: ChildId,
    range: DateRange
): List<AbsenceId> =
    createQuery<Any> {
            sql(
                """
DELETE FROM absence
WHERE
    child_id = ${bind(childId)} AND
    between_start_and_end(${bind(range)}, date) AND
    modified_by = ${bind(AuthenticatedUser.SystemInternalUser.evakaUserId)} AND
    modified_at < ${bind(now)}
RETURNING id
"""
            )
        }
        .toList()

/**
 * A citizen is allowed to edit:
 * - absences added by a citizen
 * - other than FREE_ABSENCE
 */
fun Database.Transaction.clearOldCitizenEditableAbsences(
    childDatePairs: List<Pair<ChildId, LocalDate>>,
    reservableRange: FiniteDateRange
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
DELETE FROM absence a
WHERE child_id = :childId
AND date = :date
AND absence_type <> 'FREE_ABSENCE'::absence_type
-- Planned absences cannot be deleted from confirmed range if the child has a contract days service need
AND (:reservableRange @> date OR absence_type <> 'PLANNED_ABSENCE'::absence_type OR category = 'NONBILLABLE' OR NOT EXISTS (
    SELECT
    FROM service_need_option sno
    JOIN service_need sn ON sn.option_id = sno.id AND a.date BETWEEN sn.start_date AND sn.end_date
    JOIN placement p ON p.id = sn.placement_id AND p.child_id = a.child_id AND a.date BETWEEN p.start_date AND p.end_date
    WHERE sno.contract_days_per_month IS NOT NULL
))
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
    createUpdate<Any> {
            sql(
                """
DELETE FROM absence
WHERE between_start_and_end(${bind(range)}, date)
AND absence_type <> 'FREE_ABSENCE'::absence_type
AND modified_by IN (SELECT id FROM evaka_user where type = 'CITIZEN')
"""
            )
        }
        .execute()
}

fun Database.Read.absenceExists(
    date: LocalDate,
    childId: ChildId,
    category: AbsenceCategory,
    type: AbsenceType
): Boolean =
    createQuery<Any> {
            sql(
                """
SELECT exists(
    SELECT 1 FROM absence
    WHERE child_id = ${bind(childId)}
        AND date = ${bind(date)}
        AND category = ${bind(category)}
        AND absence_type = ${bind(type)}
)"""
            )
        }
        .exactlyOne()

fun Database.Read.getGroupName(groupId: GroupId): String? =
    createQuery<Any> {
            sql("""
SELECT daycare_group.name
FROM daycare_group
WHERE id = ${bind(groupId)}
""")
        }
        .exactlyOneOrNull()

fun Database.Read.getDaycareIdByGroup(groupId: GroupId): DaycareId =
    createQuery<Any> {
            sql(
                """
SELECT daycare.id
FROM daycare_group
LEFT JOIN daycare ON daycare_group.daycare_id = daycare.id
WHERE daycare_group.id = ${bind(groupId)}
"""
            )
        }
        .exactlyOne()

private fun placementsQuery(range: FiniteDateRange, groupId: GroupId) =
    QuerySql.of<Any> {
        sql(
            """
SELECT p.child_id, daterange(p.start_date, p.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') AS date_range, p.type
FROM daycare_group_placement AS gp
JOIN placement p ON gp.daycare_placement_id = p.id AND daterange(p.start_date, p.end_date, '[]') && daterange(gp.start_date, gp.end_date, '[]')
WHERE daterange(p.start_date, p.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') && ${bind(range)}
AND gp.daycare_group_id = ${bind(groupId)}

UNION ALL

SELECT bc.child_id, daterange(p.start_date, p.end_date, '[]') * daterange(bc.start_date, bc.end_date, '[]') AS date_range, p.type
FROM backup_care bc
JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
WHERE daterange(p.start_date, p.end_date, '[]') * daterange(bc.start_date, bc.end_date, '[]') && ${bind(range)}
AND group_id = ${bind(groupId)}
"""
        )
    }

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
    return createQuery<Any> {
            sql(
                """
WITH all_placements AS (
  ${subquery(placementsQuery(range, groupId))}
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
            )
        }
        .toList<QueryResult>()
        .groupBy { it.child }
        .map { (child, queryResults) ->
            child to queryResults.map { AbsencePlacement(it.dateRange, it.type) }
        }
        .toMap()
}

fun Database.Read.getAbsences(where: Predicate<DatabaseTable.Absence>): List<Absence> =
    createQuery<Any> {
            sql(
                """
SELECT
    a.child_id,
    a.date,
    a.absence_type,
    a.category,
    eu.type <> 'CITIZEN' AS modified_by_staff,
    a.modified_at
FROM absence a
JOIN evaka_user eu ON eu.id = a.modified_by
WHERE ${predicate(where.forTable("a"))}
""",
            )
        }
        .toList()

fun Database.Read.getAbsencesInGroupByRange(
    groupId: GroupId,
    range: FiniteDateRange
): Map<Pair<ChildId, LocalDate>, List<Absence>> =
    getAbsences(
            Predicate {
                where(
                    """
$it.child_id IN (SELECT child_id FROM (${subquery(placementsQuery(range, groupId))}) p) AND
between_start_and_end(${bind(range)}, $it.date)
"""
                )
            }
        )
        .groupBy { it.childId to it.date }

fun Database.Read.getAbsencesOfChildByRange(childId: ChildId, range: DateRange): List<Absence> =
    getAbsences(
        Predicate {
            where(
                "between_start_and_end(${bind(range)}, $it.date) AND $it.child_id = ${bind(childId)}"
            )
        }
    )

fun Database.Read.getAbsencesOfChildByDate(childId: ChildId, date: LocalDate): List<Absence> =
    getAbsences(Predicate { where("$it.date = ${bind(date)} AND $it.child_id = ${bind(childId)}") })

fun Database.Read.getAbsencesCitizen(
    today: LocalDate,
    guardianId: PersonId,
    range: FiniteDateRange
): List<Absence> =
    getAbsences(
        Predicate {
            where(
                """
between_start_and_end(${bind(range)}, $it.date) AND $it.child_id = ANY (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(guardianId)}
    UNION ALL
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(guardianId)} AND valid_during @> ${bind(today)}
)
"""
            )
        }
    )

fun Database.Read.getAbsenceDatesForChildrenInRange(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, Set<LocalDate>> =
    createQuery<Any> {
            sql(
                """
SELECT a.child_id, a.date
FROM absence a
WHERE between_start_and_end(${bind(range)}, date)
AND a.child_id = ANY(${bind(childIds)})
"""
            )
        }
        .toList { column<ChildId>("child_id") to column<LocalDate>("date") }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, dates) -> dates.toSet() }

fun Database.Read.getBackupCaresAffectingGroup(
    groupId: GroupId,
    period: FiniteDateRange
): Map<ChildId, List<FiniteDateRange>> =
    createQuery<Any> {
            sql(
                """
SELECT bc.child_id, daterange(bc.start_date, bc.end_date, '[]') AS period
FROM daycare_group_placement AS gp
JOIN placement ON daycare_placement_id = placement.id
JOIN backup_care AS bc ON bc.child_id = placement.child_id
WHERE daycare_group_id = ${bind(groupId)}
AND (bc.group_id IS NULL OR bc.group_id != gp.daycare_group_id)
AND daterange(gp.start_date, gp.end_date, '[]') && ${bind(period)}
AND daterange(bc.start_date, bc.end_date, '[]') && ${bind(period)}
"""
            )
        }
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
    return createQuery<Any> {
            sql(
                """
WITH all_placements AS (
  ${subquery(placementsQuery(dateRange, groupId))}
)
SELECT r.child_id, r.date, r.start_time, r.end_time, e.type AS created_by_evaka_user_type, r.created AS created_date
FROM attendance_reservation r
JOIN evaka_user e ON r.created_by = e.id
WHERE between_start_and_end(${bind(dateRange)}, r.date)
AND EXISTS (
    SELECT 1 FROM all_placements p
    WHERE r.child_id = p.child_id
    AND between_start_and_end(p.date_range, r.date)
)
"""
            )
        }
        .toList {
            val childId: ChildId = column("child_id")
            val date: LocalDate = column("date")
            val reservation =
                ChildReservation(
                    Reservation.of(column("start_time"), column("end_time")),
                    column("created_by_evaka_user_type"),
                    column("created_date")
                )
            Pair(childId, date) to reservation
        }
        .groupBy({ it.first }, { it.second })
}

fun Database.Read.getGroupAttendances(
    groupId: GroupId,
    dateRange: FiniteDateRange
): List<ChildAttendanceRow> =
    getChildAttendances(
        Predicate {
            where(
                """
between_start_and_end(${bind(dateRange)}, $it.date)
AND EXISTS (
    SELECT FROM (${subquery(placementsQuery(dateRange, groupId))}) p
    WHERE $it.child_id = p.child_id AND between_start_and_end(p.date_range, $it.date)
)
"""
            )
        }
    )

fun Database.Read.getGroupDailyServiceTimes(
    groupId: GroupId,
    dateRange: FiniteDateRange
): Map<ChildId, List<DailyServiceTimes>> =
    createQuery<Any> {
            sql(
                """
WITH all_placements AS (
  ${subquery(placementsQuery(dateRange, groupId))}
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
        }
        .mapTo<DailyServiceTimeRow>()
        .useIterable { rows -> rows.map { toDailyServiceTimes(it) }.groupBy { it.childId } }
