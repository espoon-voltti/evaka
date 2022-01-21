// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.IncludeCodeGen
import fi.espoo.evaka.backupcare.GroupBackupCare
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.user.EvakaUserType
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month

@Service
class AbsenceService {
    fun getAbsencesByMonth(tx: Database.Read, groupId: GroupId, year: Int, month: Int): AbsenceGroup {
        val range = FiniteDateRange.ofMonth(year, Month.of(month))

        val daycare = tx.getDaycare(tx.getDaycareIdByGroup(groupId)) ?: throw BadRequest("Couldn't find daycare with group with id $groupId")
        val groupName = tx.getGroupName(groupId) ?: throw BadRequest("Couldn't find group with id $groupId")
        val placementList = tx.getPlacementsByRange(groupId, range)
        val absenceList = tx.getAbsencesByRange(groupId, range).groupBy { it.childId }.toMap()
        val backupCareList = tx.getBackupCaresAffectingGroup(groupId, range).groupBy { it.childId }.toMap()

        val children = placementList.map { (child, placements) ->
            AbsenceChild(
                child = child,
                placements = range.dates().mapNotNull { date ->
                    placements.find { it.dateRange.includes(date) }?.let { date to it.types }
                }.toMap(),
                absences = absenceList[child.id]?.groupBy { it.date } ?: mapOf(),
                backupCares = backupCareList[child.id]?.flatMap {
                    it.period.dates().map { date -> date to true }
                }?.toMap() ?: mapOf()
            )
        }

        val operationalDays = run {
            // Units that are operational every day of the week are operational during holidays as well
            if (daycare.operationDays == setOf(1, 2, 3, 4, 5, 6, 7)) range.dates().toList()
            else {
                val holidays = tx.getHolidays(range)
                range.dates()
                    .filter { daycare.operationDays.contains(it.dayOfWeek.value) }
                    .filterNot { holidays.contains(it) }
                    .toList()
            }
        }

        return AbsenceGroup(groupId, daycare.name, groupName, children, operationalDays)
    }

    fun getAbsencesByChild(tx: Database.Read, childId: ChildId, year: Int, month: Int): List<Absence> {
        val range = FiniteDateRange.ofMonth(year, Month.of(month))
        return tx.getAbsencesByChildByRange(childId, range)
    }

    fun getFutureAbsencesByChild(tx: Database.Read, childId: ChildId): List<Absence> {
        val period = DateRange(LocalDate.now().plusDays(1), null)
        return tx.getAbsencesByChildByRange(childId, period)
    }
}

fun getAbsenceCareTypes(placementType: PlacementType): List<AbsenceCareType> = when (placementType) {
    PlacementType.CLUB -> listOf(AbsenceCareType.CLUB)
    PlacementType.SCHOOL_SHIFT_CARE -> listOf(AbsenceCareType.SCHOOL_SHIFT_CARE)
    PlacementType.PRESCHOOL,
    PlacementType.PREPARATORY -> listOf(AbsenceCareType.PRESCHOOL)
    PlacementType.PRESCHOOL_DAYCARE,
    PlacementType.PREPARATORY_DAYCARE -> listOf(AbsenceCareType.PRESCHOOL, AbsenceCareType.PRESCHOOL_DAYCARE)
    PlacementType.DAYCARE -> listOf(AbsenceCareType.DAYCARE)
    PlacementType.DAYCARE_PART_TIME -> listOf(AbsenceCareType.DAYCARE)
    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> listOf(AbsenceCareType.DAYCARE_5YO_FREE, AbsenceCareType.DAYCARE)
    PlacementType.TEMPORARY_DAYCARE, PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
        listOf(AbsenceCareType.DAYCARE)
}

@IncludeCodeGen
enum class AbsenceCareType {
    SCHOOL_SHIFT_CARE,
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
    DAYCARE_5YO_FREE,
    DAYCARE,
    CLUB
}

data class AbsenceGroup(
    val groupId: GroupId,
    val daycareName: String,
    val groupName: String,
    val children: List<AbsenceChild>,
    val operationDays: List<LocalDate>
)

@ExcludeCodeGen
data class AbsenceChild(
    val child: Child,
    val placements: Map<LocalDate, List<AbsenceCareType>>,
    val absences: Map<LocalDate, List<AbsenceWithModifierInfo>>,
    val backupCares: Map<LocalDate, Boolean>
)

@IncludeCodeGen
data class Child(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate
)

data class AbsencePlacement(
    val dateRange: FiniteDateRange,
    val types: List<AbsenceCareType>
)

data class Absence(
    val id: AbsenceId,
    val childId: ChildId,
    val date: LocalDate,
    val careType: AbsenceCareType,
    val absenceType: AbsenceType
)

@IncludeCodeGen
data class AbsenceWithModifierInfo(
    val id: AbsenceId,
    val childId: ChildId,
    val date: LocalDate,
    val careType: AbsenceCareType,
    val absenceType: AbsenceType,
    val modifiedByType: EvakaUserType,
    val modifiedAt: HelsinkiDateTime
) {
    fun asAbsence(): Absence = Absence(
        id = id,
        childId = childId,
        date = date,
        careType = careType,
        absenceType = absenceType
    )
}

enum class AbsenceType {
    OTHER_ABSENCE,
    SICKLEAVE,
    UNKNOWN_ABSENCE,
    PLANNED_ABSENCE,
    PARENTLEAVE,
    FORCE_MAJEURE;
}

data class AbsenceUpsert(
    val childId: ChildId,
    val date: LocalDate,
    val careType: AbsenceCareType,
    val absenceType: AbsenceType
)

// database functions
fun Database.Transaction.upsertAbsences(absences: List<AbsenceUpsert>, modifiedBy: EvakaUserId) {
    //language=SQL
    val sql =
        """
        INSERT INTO absence (child_id, date, care_type, absence_type, modified_by)
        VALUES (:childId, :date, :careType, :absenceType, :modifiedBy)
        ON CONFLICT (child_id, date, care_type)
            DO UPDATE SET absence_type = :absenceType, modified_by = :modifiedBy, modified_at = now()
        """.trimIndent()

    val batch = prepareBatch(sql)
    for (absence in absences) {
        batch
            .bind("childId", absence.childId)
            .bind("date", absence.date)
            .bind("careType", absence.careType)
            .bind("absenceType", absence.absenceType)
            .bind("modifiedBy", modifiedBy)
            .add()
    }

    batch.execute()
}

data class AbsenceDelete(
    val childId: ChildId,
    val date: LocalDate,
    val careType: AbsenceCareType
)

fun Database.Transaction.batchDeleteAbsences(deletions: List<AbsenceDelete>) {
    //language=SQL
    val sql =
        """
        DELETE FROM absence
        WHERE care_type = :careType AND date = :date AND child_id = :childId
        """.trimIndent()

    val batch = prepareBatch(sql)
    deletions.forEach {
        batch
            .bind("childId", it.childId)
            .bind("date", it.date)
            .bind("careType", it.careType)
            .add()
    }
    batch.execute()
}

fun Database.Read.getGroupName(groupId: GroupId): String? {
    //language=SQL
    val sql =
        """
            SELECT daycare_group.name 
            FROM daycare_group 
            WHERE id = :groupId;
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .mapTo(String::class.java)
        .firstOrNull()
}

fun Database.Read.getDaycareIdByGroup(groupId: GroupId): DaycareId {
    //language=SQL
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

fun Database.Read.getPlacementsByRange(
    groupId: GroupId,
    range: FiniteDateRange
): List<Pair<Child, List<AbsencePlacement>>> {
    data class QueryResult(
        @Nested("child")
        val child: Child,
        val dateRange: FiniteDateRange,
        val type: PlacementType
    )
    //language=SQL
    val sql =
        """
WITH all_placements AS (
  SELECT child_id, daterange(gp.start_date, gp.end_date, '[]') AS date_range, type
  FROM daycare_group_placement AS gp
  JOIN placement ON daycare_placement_id = placement.id
  WHERE daterange(gp.start_date, gp.end_date, '[]') && :range
  AND daycare_group_id = :groupId

  UNION ALL

  SELECT bc.child_id, daterange(p.start_date, p.end_date, '[]') * daterange(bc.start_date, bc.end_date, '[]') AS date_range, p.type
  FROM backup_care bc
  JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
  WHERE daterange(bc.start_date, bc.end_date, '[]') && :range
  AND group_id = :groupId
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
        .bind("range", range)
        .mapTo<QueryResult>()
        .toList()
        .groupBy { it.child }
        .map { (child, queryResults) ->
            child to queryResults.map { AbsencePlacement(it.dateRange, getAbsenceCareTypes(it.type)) }
        }
}

fun Database.Read.getAbsencesByRange(groupId: GroupId, range: FiniteDateRange): List<AbsenceWithModifierInfo> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type, eu.type AS modified_by_type, a.modified_at AS modified_at
        FROM absence a
        LEFT JOIN evaka_user eu ON eu.id = a.modified_by 
        WHERE child_id IN (
          SELECT child_id
          FROM daycare_group_placement AS gp
          JOIN placement ON daycare_placement_id = placement.id
          WHERE daterange(gp.start_date, gp.end_date, '[]') && :range
          AND daycare_group_id = :groupId

          UNION ALL

          SELECT child_id
          FROM backup_care
          WHERE daterange(start_date, end_date, '[]') && :range
          AND group_id = :groupId
        )
        AND between_start_and_end(:range, date)
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("range", range)
        .mapTo<AbsenceWithModifierInfo>()
        .list()
}

fun Database.Read.getAbsencesByChildByRange(childId: ChildId, range: FiniteDateRange): List<Absence> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type
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

fun Database.Read.getAbsencesByChildByRange(childId: ChildId, range: DateRange): List<Absence> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type
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

private fun Database.Read.getBackupCaresAffectingGroup(groupId: GroupId, period: FiniteDateRange): List<GroupBackupCare> =
    createQuery(
        // language=SQL
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

private fun Database.Read.getHolidays(range: FiniteDateRange): List<LocalDate> = createQuery(
    "SELECT date FROM holiday WHERE between_start_and_end(:range, date)"
)
    .bind("range", range)
    .mapTo<LocalDate>()
    .list()
