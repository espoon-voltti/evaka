// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare.service

import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.IncludeCodeGen
import fi.espoo.evaka.backupcare.GroupBackupCare
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimes
import fi.espoo.evaka.dailyservicetimes.toDailyServiceTimes
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import fi.espoo.evaka.user.EvakaUserType
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.Nested
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalTime
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
        val reservations = tx.getGroupReservations(groupId, range).groupBy { it.childId }.toMap()
        val attendances = tx.getGroupAttendances(groupId, range).groupBy { it.childId }.toMap()
        val dailyServiceTimes = tx.getGroupDailyServiceTimes(groupId, range)

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
        val setOfOperationalDays = operationalDays.toSet()

        val children = placementList.map { (child, placements) ->
            val placementDateRanges = placements.map { it.dateRange }
            val supplementedReservations = supplementReservationsWithDailyServiceTimes(
                placementDateRanges,
                setOfOperationalDays,
                reservations[child.id],
                dailyServiceTimes[child.id],
                absenceList[child.id]
            )

            AbsenceChild(
                child = child,
                placements = range.dates().mapNotNull { date ->
                    placements.find { it.dateRange.includes(date) }?.let { date to it.types }
                }.toMap(),
                absences = absenceList[child.id]?.groupBy { it.date } ?: mapOf(),
                backupCares = backupCareList[child.id]?.flatMap {
                    it.period.dates().map { date -> date to true }
                }?.toMap() ?: mapOf(),
                reservationTotalHours = sumOfHours(supplementedReservations, placementDateRanges, range),
                attendanceTotalHours = attendances[child.id]
                    ?.map { HelsinkiDateTimeRange(it.arrived, it.departed) }
                    ?.let { sumOfHours(it, placementDateRanges, range) }
            )
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

private fun supplementReservationsWithDailyServiceTimes(
    placementDateRanges: List<FiniteDateRange>,
    unitOperationalDays: Set<LocalDate>,
    reservations: List<ChildReservation>?,
    dailyServiceTimes: DailyServiceTimes?,
    absences: List<AbsenceWithModifierInfo>?
): List<HelsinkiDateTimeRange> {
    val absenceDates = absences?.map { it.date }?.toSet() ?: setOf()

    val reservationRanges = reservations
        ?.filterNot { absenceDates.contains(it.startTime.toLocalDate()) }
        ?.map { HelsinkiDateTimeRange(it.startTime, it.endTime) }
        ?: listOf()

    val reservationStartDates = reservationRanges.map { it.start.toLocalDate() }.toSet()

    val dailyServiceTimeRanges =
        if (dailyServiceTimes == null) listOf()
        else placementDateRanges
            .flatMap { it.dates() }
            .filterNot { reservationStartDates.contains(it) }
            .filterNot { absenceDates.contains(it) }
            .filter { unitOperationalDays.contains(it) }
            .mapNotNull { date ->
                val times = when (dailyServiceTimes) {
                    is DailyServiceTimes.RegularTimes ->
                        dailyServiceTimes.regularTimes.start to dailyServiceTimes.regularTimes.end
                    is DailyServiceTimes.IrregularTimes -> {
                        val times = dailyServiceTimes.timesForDayOfWeek(date.dayOfWeek)
                        if (times != null) times.start to times.end else null
                    }
                    is DailyServiceTimes.VariableTimes -> null
                }
                times?.let { (start, end) ->
                    HelsinkiDateTimeRange(
                        HelsinkiDateTime.of(date, start),
                        HelsinkiDateTime.of(if (end < start) date.plusDays(1) else date, end)
                    )
                }
            }

    return (reservationRanges + dailyServiceTimeRanges)
        .sortedBy { it.start }
        .fold(listOf()) { timeRanges, timeRange ->
            val nonOverlappingTimeRange =
                if (timeRanges.lastOrNull()?.overlaps(timeRange) == true) timeRange.copy(start = timeRanges.last().end)
                else timeRange
            timeRanges + nonOverlappingTimeRange
        }
}

private fun sumOfHours(
    dateTimeRanges: List<HelsinkiDateTimeRange>,
    placementDateRanges: List<FiniteDateRange>,
    spanningDateRange: FiniteDateRange
): Int {
    val placementDateTimeRanges = placementDateRanges
        .mapNotNull { it.intersection(spanningDateRange) }
        .map {
            HelsinkiDateTimeRange(
                HelsinkiDateTime.of(it.start, LocalTime.of(0, 0)),
                HelsinkiDateTime.of(it.end.plusDays(1), LocalTime.of(0, 0))
            )
        }

    return dateTimeRanges
        .flatMap { timeRange -> placementDateTimeRanges.mapNotNull { timeRange.intersection(it) } }
        .fold(0L) { sum, (start, end) -> sum + end.durationSince(start).toMinutes() }
        .let { minutes -> BigDecimal(minutes).divide(BigDecimal(60), 0, RoundingMode.FLOOR).toInt() }
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
    val backupCares: Map<LocalDate, Boolean>,
    val reservationTotalHours: Int?,
    val attendanceTotalHours: Int?
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
)

enum class AbsenceType : DatabaseEnum {
    OTHER_ABSENCE,
    SICKLEAVE,
    UNKNOWN_ABSENCE,
    PLANNED_ABSENCE,
    PARENTLEAVE,
    FORCE_MAJEURE;

    override val sqlType: String = "absence_type"
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
        .mapTo<String>()
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
    //language=sql
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
            child to queryResults.map { AbsencePlacement(it.dateRange, getAbsenceCareTypes(it.type)) }
        }
        .toMap()
}

fun Database.Read.getAbsencesByRange(groupId: GroupId, range: FiniteDateRange): List<AbsenceWithModifierInfo> {
    //language=SQL
    val sql =
        """
        SELECT a.id, a.child_id, a.date, a.care_type, a.absence_type, eu.type AS modified_by_type, a.modified_at AS modified_at
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

data class ChildReservation(
    val childId: ChildId,
    val startTime: HelsinkiDateTime,
    val endTime: HelsinkiDateTime
)

private fun Database.Read.getGroupReservations(groupId: GroupId, dateRange: FiniteDateRange): List<ChildReservation> =
    createQuery(
        """
WITH all_placements AS (
  $placementsQuery
)
SELECT r.child_id, r.start_time, r.end_time FROM attendance_reservation r
WHERE :dateRange && daterange((r.start_time at time zone 'Europe/Helsinki')::date, (r.end_time at time zone 'Europe/Helsinki')::date, '[]')
AND EXISTS (
    SELECT 1 FROM all_placements p
    WHERE r.child_id = p.child_id
    AND p.date_range && daterange((r.start_time at time zone 'Europe/Helsinki')::date, (r.end_time at time zone 'Europe/Helsinki')::date, '[]')
)
"""
    )
        .bind("groupId", groupId)
        .bind("dateRange", dateRange)
        .mapTo<ChildReservation>()
        .toList()

data class ChildAttendance(
    val childId: ChildId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime
)

private fun Database.Read.getGroupAttendances(groupId: GroupId, dateRange: FiniteDateRange): List<ChildAttendance> =
    createQuery(
        """
WITH all_placements AS (
  $placementsQuery
)
SELECT a.child_id, a.arrived, a.departed FROM child_attendance a
WHERE a.departed IS NOT NULL
AND :dateRange && daterange((a.arrived at time zone 'Europe/Helsinki')::date, (a.departed at time zone 'Europe/Helsinki')::date, '[]')
AND EXISTS (
    SELECT 1 FROM all_placements p
    WHERE a.child_id = p.child_id
    AND p.date_range && daterange((a.arrived at time zone 'Europe/Helsinki')::date, (a.departed at time zone 'Europe/Helsinki')::date, '[]')
)
"""
    )
        .bind("groupId", groupId)
        .bind("dateRange", dateRange)
        .mapTo<ChildAttendance>()
        .toList()

private fun Database.Read.getGroupDailyServiceTimes(
    groupId: GroupId,
    dateRange: FiniteDateRange
): Map<ChildId, DailyServiceTimes?> = createQuery(
    """
WITH all_placements AS (
  $placementsQuery
)
SELECT st.* FROM daily_service_time st WHERE EXISTS (SELECT 1 FROM all_placements p WHERE st.child_id = p.child_id)
"""
)
    .bind("groupId", groupId)
    .bind("dateRange", dateRange)
    .map { rv ->
        rv.mapColumn<ChildId>("child_id") to toDailyServiceTimes(rv)
    }
    .toMap()
