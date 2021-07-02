// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.backupcare.GroupBackupCare
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.UUID

private val logger = KotlinLogging.logger { }

@Service
class AbsenceService {
    fun getAbsencesByMonth(tx: Database.Read, groupId: GroupId, year: Int, month: Int): AbsenceGroup {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.with(lastDayOfMonth())
        val period = FiniteDateRange(startDate, endDate)

        val daycare = tx.getDaycare(tx.getDaycareIdByGroup(groupId)) ?: throw BadRequest("Couldn't find daycare with group with id $groupId")
        val groupName = tx.getGroupName(groupId) ?: throw BadRequest("Couldn't find group with id $groupId")
        val placementList = tx.getPlacementsByRange(groupId, period)
        val absenceList = tx.getAbsencesByRange(groupId, period)
        val backupCareList = tx.getBackupCaresAffectingGroup(groupId, period)

        val children = placementList
            .map { placement ->
                AbsenceChild(
                    id = placement.childId,
                    firstName = placement.firstName,
                    lastName = placement.lastName,
                    dob = placement.dob,
                    placements = composePlacementMap(
                        period,
                        placementList.filter { it.childId == placement.childId }
                    ),
                    absences = composeAbsenceMap(
                        period,
                        absenceList.filter { it.childId == placement.childId }
                    ),
                    backupCares = composeBackupCareMap(
                        period,
                        backupCareList.filter { it.childId == placement.childId }
                    )
                )
            }
            .distinct()

        val operationalDays = run {
            val fullMonth = generateSequence(startDate) { it.plusDays(1) }.takeWhile { it <= endDate }
            // Units that are operational every day of the week are operational during holidays as well
            if (daycare.operationDays == setOf(1, 2, 3, 4, 5, 6, 7)) fullMonth.toList()
            else {
                val holidays = tx.getHolidays(DateRange(startDate, endDate))
                fullMonth
                    .filter { daycare.operationDays.contains(it.dayOfWeek.value) }
                    .filterNot { holidays.contains(it) }
                    .toList()
            }
        }

        return AbsenceGroup(groupId, daycare.name, groupName, children, operationalDays)
    }

    fun upsertAbsences(tx: Database.Transaction, absences: List<Absence>, userId: UUID) {
        try {
            val userName = tx.getEmployeeNameById(userId)
            tx.upsertAbsences(absences, userName)
        } catch (e: Exception) {
            logger.error(e) { "Error: Updating absences by user $userId failed" }
            throw BadRequest("Error: Updating absences failed: ${e.message}")
        }
    }

    fun getAbscencesByChild(tx: Database.Read, childId: UUID, year: Int, month: Int): AbsenceChildMinimal {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.with(lastDayOfMonth())
        val period = FiniteDateRange(startDate, endDate)

        val absenceList = tx.getAbsencesByChildByRange(childId, period)
        val backupCareList = tx.getBackupCaresAffectingChild(childId, period)
        val child =
            tx.getPersonById(childId) ?: throw BadRequest("Error: Could not find child with id: $childId")
        return AbsenceChildMinimal(
            id = child.id,
            firstName = child.firstName ?: "",
            lastName = child.lastName ?: "",
            absences = composeAbsenceMap(
                period,
                absenceList
            ),
            backupCares = composeBackupCareMap(
                period,
                backupCareList
            )
        )
    }

    fun getFutureAbsencesByChild(tx: Database.Read, childId: UUID): List<Absence> {
        val period = DateRange(LocalDate.now().plusDays(1), null)
        return tx.getAbsencesByChildByPeriod(childId, period)
    }

    private fun composeAbsenceMap(
        period: FiniteDateRange,
        absenceListByChild: List<Absence>
    ): Map<LocalDate, List<Absence>> =
        absenceListByChild.groupBy { it.date }.let { absences ->
            period.dates()
                .map { it to absences.getOrDefault(it, listOf()) }
                .toMap()
        }

    private fun composePlacementMap(
        period: FiniteDateRange,
        placementListByChild: List<AbsencePlacement>
    ): Map<LocalDate, List<CareType>> = period.dates()
        .map {
            it to placementListByChild
                .filter { placement -> DateRange(placement.startDate, placement.endDate).includes(it) }
                .flatMap { placement -> getAbsenceCareTypes(placement.type) }
        }
        .toMap()

    private fun composeBackupCareMap(
        period: FiniteDateRange,
        backupCares: List<GroupBackupCare>
    ): Map<LocalDate, AbsenceBackupCare?> = period.dates()
        .map {
            it to backupCares.find { backupCare -> backupCare.period.includes(it) }
                ?.let { backupCare -> AbsenceBackupCare(childId = backupCare.childId, date = it) }
        }
        .toMap()
}

fun getAbsenceCareTypes(placementType: PlacementType): List<CareType> = when (placementType) {
    PlacementType.CLUB -> listOf(CareType.CLUB)
    PlacementType.SCHOOL_SHIFT_CARE -> listOf(CareType.SCHOOL_SHIFT_CARE)
    PlacementType.PRESCHOOL,
    PlacementType.PREPARATORY -> listOf(CareType.PRESCHOOL)
    PlacementType.PRESCHOOL_DAYCARE,
    PlacementType.PREPARATORY_DAYCARE -> listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE)
    PlacementType.DAYCARE -> listOf(CareType.DAYCARE)
    PlacementType.DAYCARE_PART_TIME -> listOf(CareType.DAYCARE)
    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS -> listOf(CareType.DAYCARE_5YO_FREE, CareType.DAYCARE)
    PlacementType.TEMPORARY_DAYCARE, PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
        listOf(CareType.DAYCARE)
}

enum class CareType {
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
    var groupName: String,
    var children: List<AbsenceChild>,
    val operationDays: List<LocalDate>
)

data class AbsenceChild(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val dob: LocalDate,
    val placements: Map<LocalDate, List<CareType>>,
    val absences: Map<LocalDate, List<Absence>>,
    val backupCares: Map<LocalDate, AbsenceBackupCare?>
)

data class AbsenceChildMinimal(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val absences: Map<LocalDate, List<Absence>>,
    val backupCares: Map<LocalDate, AbsenceBackupCare?>
)

data class Absence(
    val id: UUID? = null,
    val childId: UUID,
    val date: LocalDate,
    var careType: CareType,
    val absenceType: AbsenceType,
    val modifiedAt: String? = null,
    val modifiedBy: String? = null
)

data class AbsenceBackupCare(
    val childId: UUID,
    val date: LocalDate
)

enum class AbsenceType {
    OTHER_ABSENCE,
    SICKLEAVE,
    UNKNOWN_ABSENCE,
    PLANNED_ABSENCE,

    @Deprecated("replaced by backup cares")
    TEMPORARY_RELOCATION,

    @Deprecated("replaced by backup cares")
    TEMPORARY_VISITOR,
    PARENTLEAVE,
    FORCE_MAJEURE;
}

data class AbsencePlacement(
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val type: PlacementType,
    val dob: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate
)

// database functions
private fun Database.Transaction.upsertAbsences(absences: List<Absence>, modifiedBy: String) {
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
    val childId: UUID,
    val date: LocalDate,
    var careType: CareType
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

fun Database.Read.getEmployeeNameById(employeeId: UUID): String {
    //language=SQL
    val sql =
        """
        SELECT concat(first_name, ' ', last_name)
        FROM employee
        WHERE id = :employeeId
        """.trimIndent()

    return createQuery(sql)
        .bind("employeeId", employeeId)
        .mapTo(String::class.java)
        .first()
}

fun Database.Read.getUserNameById(userId: UUID): String {
    //language=SQL
    val sql =
        """
        SELECT concat(first_name, ' ', last_name)
        FROM person
        WHERE id = :userId
        """.trimIndent()

    return createQuery(sql)
        .bind("userId", userId)
        .mapTo(String::class.java)
        .first()
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

fun Database.Read.getDaycareIdByGroup(groupId: GroupId): UUID {
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
        .mapTo<UUID>()
        .first()
}

fun Database.Read.getPlacementsByRange(groupId: GroupId, period: FiniteDateRange): List<AbsencePlacement> {
    //language=SQL
    val sql =
        """
        WITH all_placements AS (
          SELECT child_id, gp.start_date, gp.end_date, type
          FROM daycare_group_placement AS gp
          JOIN placement ON daycare_placement_id = placement.id
          WHERE daterange(gp.start_date, gp.end_date, '[]') && :period
          AND daycare_group_id = :groupId

          UNION ALL

          SELECT bc.child_id, GREATEST(p.start_date, bc.start_date), LEAST(p.end_date, bc.end_date), p.type
          FROM backup_care bc
          JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
          WHERE daterange(bc.start_date, bc.end_date, '[]') && :period
          AND group_id = :groupId
        )
        SELECT
          all_placements.*,
          person.first_name,
          person.last_name,
          person.date_of_birth AS dob
        FROM all_placements
        JOIN person
        ON child_id = person.id
        ORDER BY person.last_name, person.first_name;
    """

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("period", period)
        .mapTo<AbsencePlacement>()
        .list()
}

fun Database.Read.getAbsencesByRange(groupId: GroupId, period: FiniteDateRange): List<Absence> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type, a.modified_at, a.modified_by
        FROM absence a
        WHERE child_id IN (
          SELECT child_id
          FROM daycare_group_placement AS gp
          JOIN placement ON daycare_placement_id = placement.id
          WHERE daterange(gp.start_date, gp.end_date, '[]') && :period
          AND daycare_group_id = :groupId

          UNION ALL

          SELECT child_id
          FROM backup_care
          WHERE daterange(start_date, end_date, '[]') && :period
          AND group_id = :groupId
        )
        AND date <@ :period
        """.trimIndent()

    return createQuery(sql)
        .bind("groupId", groupId)
        .bind("period", period)
        .mapTo<Absence>()
        .list()
}

fun Database.Read.getAbsencesByChildByRange(childId: UUID, period: FiniteDateRange): List<Absence> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type, a.modified_at, a.modified_by
        FROM absence a
        WHERE date <@ :period
        AND a.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("period", period)
        .mapTo<Absence>()
        .list()
}

fun Database.Read.getAbsencesByChildByPeriod(childId: UUID, period: DateRange): List<Absence> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type, a.modified_at, a.modified_by
        FROM absence a
        WHERE date <@ :period
        AND a.child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("period", period)
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

private fun Database.Read.getBackupCaresAffectingChild(childId: UUID, period: FiniteDateRange): List<GroupBackupCare> =
    createQuery(
        // language=SQL
        """
SELECT bc.id, bc.child_id, daterange(bc.start_date, bc.end_date, '[]') AS period
FROM daycare_group_placement AS gp
JOIN placement
ON daycare_placement_id = placement.id
JOIN backup_care AS bc
ON bc.child_id = placement.child_id
AND bc.unit_id != placement.unit_id
WHERE bc.child_id = :childId
AND daterange(gp.start_date, gp.end_date, '[]') && :period
"""
    )
        .bind("childId", childId)
        .bind("period", period)
        .mapTo<GroupBackupCare>()
        .list()

private fun Database.Read.getHolidays(dateRange: DateRange): List<LocalDate> = createQuery(
    "SELECT date FROM holiday WHERE :dateRange @> date"
)
    .bind("dateRange", dateRange)
    .mapTo<LocalDate>()
    .list()
