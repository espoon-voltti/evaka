// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.backupcare.GroupBackupCare
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Period
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters.lastDayOfMonth
import java.util.UUID

private val logger = KotlinLogging.logger { }

@Service
class AbsenceService(private val db: Jdbi, private val personService: PersonService) {
    fun getAbsencesByMonth(groupId: UUID, year: Int, month: Int): AbsenceGroup {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.with(lastDayOfMonth())
        val period = ClosedPeriod(startDate, endDate)

        return db.handle { h ->
            val groupName = getGroupName(groupId, h) ?: throw BadRequest("Couldn't find group with id $groupId")
            val daycareName = getDaycareName(groupId, h)
            val placementList = getPlacementsByRange(groupId, period, h)
            val absenceList = getAbsencesByRange(groupId, period, h)
            val backupCareList = h.getBackupCaresAffectingGroup(groupId, period)

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

            AbsenceGroup(groupId, daycareName, groupName, children)
        }
    }

    fun upsertAbsences(absences: List<Absence>, groupId: UUID, userId: UUID) {
        db.handle { h ->
            try {
                val userName = getEmployeeNameById(userId, h)
                upsertAbsences(absences, userName, h)
            } catch (e: Exception) {
                logger.error { "Error: Updating absences by user $userId failed" }
                throw BadRequest("Error: Updating absences failed: ${e.message}")
            }
        }
    }

    fun upsertChildAbsence(childId: UUID, absenceType: AbsenceType, careType: CareType, userId: UUID) {
        db.handle { h ->
            try {
                upsertChildAbsence(childId, absenceType, careType, userId, h)
            } catch (e: Exception) {
                logger.error { "Error: Updating absences by user $userId failed" }
                throw BadRequest("Error: Updating absences failed: ${e.message}")
            }
        }
    }

    fun getAbscencesByChild(childId: UUID, year: Int, month: Int): AbsenceChildMinimal {
        val startDate = LocalDate.of(year, month, 1)
        val endDate = startDate.with(lastDayOfMonth())
        val period = ClosedPeriod(startDate, endDate)

        return db.handle { h ->
            val absenceList = getAbsencesByChildByRange(childId, period, h)
            val backupCareList = h.getBackupCaresAffectingChild(childId, period)
            val child =
                personService.getPerson(childId) ?: throw BadRequest("Error: Could not find child with id: $childId")
            AbsenceChildMinimal(
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
    }

    private fun composeAbsenceMap(
        period: ClosedPeriod,
        absenceListByChild: List<Absence>
    ): Map<LocalDate, List<Absence>> =
        absenceListByChild.groupBy { it.date }.let { absences ->
            period.dates()
                .map { it to absences.getOrDefault(it, listOf()) }
                .toMap()
        }

    private fun composePlacementMap(
        period: ClosedPeriod,
        placementListByChild: List<AbsencePlacement>
    ): Map<LocalDate, List<CareType>> = period.dates()
        .map {
            it to placementListByChild
                .filter { placement -> Period(placement.startDate, placement.endDate).includes(it) }
                .flatMap { placement -> getCareType(placement, it) }
        }
        .toMap()

    private fun composeBackupCareMap(
        period: ClosedPeriod,
        backupCares: List<GroupBackupCare>
    ): Map<LocalDate, AbsenceBackupCare?> = period.dates()
        .map {
            it to backupCares.find { backupCare -> backupCare.period.includes(it) }
                ?.let { backupCare -> AbsenceBackupCare(childId = backupCare.childId, date = it) }
        }
        .toMap()

    private fun getCareType(placement: AbsencePlacement, placementDate: LocalDate): List<CareType> =
        when (placement.type) {
            PlacementType.CLUB -> listOf(CareType.CLUB)
            PlacementType.PRESCHOOL,
            PlacementType.PREPARATORY -> listOf(CareType.PRESCHOOL)
            PlacementType.PRESCHOOL_DAYCARE,
            PlacementType.PREPARATORY_DAYCARE -> listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE)
            PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME -> {
                val placementTerm = if (placementDate <= LocalDate.of(placementDate.year, 7, 31)) {
                    Period(
                        LocalDate.of(placementDate.year - 1, 8, 1),
                        LocalDate.of(placementDate.year, 7, 31)
                    )
                } else {
                    Period(
                        LocalDate.of(placementDate.year, 8, 1),
                        LocalDate.of(placementDate.year + 1, 7, 31)
                    )
                }

                val entitledToFree5yoDaycare = placementTerm.includes(
                    placement.dob.plusYears(5).withMonth(12).withDayOfMonth(31)
                )

                if (entitledToFree5yoDaycare)
                    listOf(CareType.DAYCARE_5YO_FREE, CareType.DAYCARE)
                else listOf(CareType.DAYCARE)
            }
        }
}

enum class CareType {
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
    DAYCARE_5YO_FREE,
    DAYCARE,
    CLUB
}

// objects needed
data class AbsenceGroup(
    val groupId: UUID,
    val daycareName: String,
    var groupName: String,
    var children: List<AbsenceChild>
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
    FORCE_MAJEURE,
    PRESENCE
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
private fun upsertAbsences(absences: List<Absence>, modifiedBy: String, h: Handle) {
    //language=SQL
    val sql =
        """
        INSERT INTO absence (child_id, date, care_type, absence_type, modified_by)
        VALUES (:childId, :date, :careType, :absenceType, :modifiedBy)
        ON CONFLICT (child_id, date, care_type)
            DO UPDATE SET absence_type = :absenceType, modified_by = :modifiedBy, modified_at = now()
        """.trimIndent()

    val batch = h.prepareBatch(sql)
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

private fun upsertChildAbsence(childId: UUID, absenceType: AbsenceType, careType: CareType, userId: UUID, h: Handle) {
    //language=SQL
    val sql =
        """
        INSERT INTO absence (child_id, date, care_type, absence_type, modified_by)
        VALUES (:childId, :date, :careType, :absenceType, :modifiedBy)
        ON CONFLICT (child_id, date, care_type)
            DO UPDATE SET absence_type = :absenceType, modified_at = now()
        """.trimIndent()

    h.createUpdate(sql)
        .bind("childId", childId)
        .bind("absenceType", absenceType)
        .bind("date", LocalDate.now())
        .bind("modifiedBy", userId)
        .bind("careType", careType)
        .execute()
}

fun getEmployeeNameById(employeeId: UUID, h: Handle): String {
    //language=SQL
    val sql =
        """
        SELECT concat(first_name, ' ', last_name)
        FROM employee
        WHERE id = :employeeId
        """.trimIndent()

    return h.createQuery(sql)
        .bind("employeeId", employeeId)
        .mapTo(String::class.java)
        .first()
}

fun getUserNameById(userId: UUID, h: Handle): String {
    //language=SQL
    val sql =
        """
        SELECT concat(first_name, ' ', last_name)
        FROM person
        WHERE id = :userId
        """.trimIndent()

    return h.createQuery(sql)
        .bind("userId", userId)
        .mapTo(String::class.java)
        .first()
}

fun getGroupName(groupId: UUID, h: Handle): String? {
    //language=SQL
    val sql =
        """
            SELECT daycare_group.name 
            FROM daycare_group 
            WHERE id = :groupId;
        """.trimIndent()

    return h.createQuery(sql)
        .bind("groupId", groupId)
        .mapTo(String::class.java)
        .firstOrNull()
}

fun getDaycareName(groupId: UUID, h: Handle): String {
    //language=SQL
    val sql =
        """
            SELECT daycare.name 
            FROM daycare_group 
                LEFT JOIN daycare ON daycare_group.daycare_id = daycare.id
            WHERE daycare_group.id = :groupId;
        """.trimIndent()

    return h.createQuery(sql)
        .bind("groupId", groupId)
        .mapTo(String::class.java)
        .first()
}

fun getPlacementsByRange(groupId: UUID, period: ClosedPeriod, h: Handle): List<AbsencePlacement> {
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
        ORDER BY person.last_name ASC, person.first_name ASC;
    """

    return h.createQuery(sql)
        .bind("groupId", groupId)
        .bind("period", period)
        .mapTo<AbsencePlacement>()
        .list()
}

fun getAbsencesByRange(groupId: UUID, period: ClosedPeriod, h: Handle): List<Absence> {
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

    return h.createQuery(sql)
        .bind("groupId", groupId)
        .bind("period", period)
        .mapTo<Absence>()
        .list()
}

fun getAbsencesByChildByRange(childId: UUID, period: ClosedPeriod, h: Handle): List<Absence> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type, a.modified_at, a.modified_by
        FROM absence a
        WHERE date <@ :period
        AND a.child_id = :childId
        """.trimIndent()

    return h.createQuery(sql)
        .bind("childId", childId)
        .bind("period", period)
        .mapTo<Absence>()
        .list()
}

private fun Handle.getBackupCaresAffectingGroup(groupId: UUID, period: ClosedPeriod): List<GroupBackupCare> =
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

private fun Handle.getBackupCaresAffectingChild(childId: UUID, period: ClosedPeriod): List<GroupBackupCare> =
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
