// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.LinkityEnv
import fi.espoo.evaka.attendance.*
import fi.espoo.evaka.espoo.EspooAsyncJob
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.utils.partitionIndexed
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.LocalDate
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}
private val MAX_DRIFT: Duration = Duration.ofMinutes(5)

@Service
class LinkitySyncService(val linkityEnv: LinkityEnv?, val jsonMapper: JsonMapper) {
    fun getStaffAttendancePlans(
        db: Database.Connection,
        clock: EvakaClock,
        msg: EspooAsyncJob.GetStaffAttendancePlansFromLinkity,
    ) {
        if (linkityEnv == null) {
            logger.warn { "Linkity environment not configured" }
            return
        }
        val client = LinkityHttpClient(linkityEnv!!, jsonMapper)
        updateStaffAttendancePlansFromLinkity(msg.period, db, client)
    }
}

fun generateDateRangesForStaffAttendancePlanRequests(
    startDate: LocalDate,
    endDate: LocalDate,
    chunkSizeDays: Long,
): Sequence<FiniteDateRange> {
    return generateSequence(startDate) { it.plusDays(chunkSizeDays) }
        .takeWhile { it <= endDate }
        .map { FiniteDateRange(it, minOf(it.plusDays(chunkSizeDays - 1), endDate)) }
}

fun updateStaffAttendancePlansFromLinkity(
    period: FiniteDateRange,
    db: Database.Connection,
    client: LinkityClient,
) {
    val linkityShifts = client.getShifts(period)

    val sarastiaIds = linkityShifts.map { it.sarastiaId }.toSet()

    db.transaction { tx ->
        val sarastiaIdToEmployeeId = tx.getEmployeeIdsForEnabledDaycares(sarastiaIds)

        val shifts = filterValidShifts(linkityShifts, sarastiaIdToEmployeeId)

        val staffAttendancePlans =
            shifts.mapNotNull {
                StaffAttendancePlan(
                    employeeId = sarastiaIdToEmployeeId[it.sarastiaId] ?: return@mapNotNull null,
                    type =
                        when (it.type) {
                            ShiftType.PRESENT -> StaffAttendanceType.PRESENT
                            ShiftType.TRAINING -> StaffAttendanceType.TRAINING
                        },
                    startTime = it.startDateTime,
                    endTime = it.endDateTime,
                    description = it.notes,
                )
            }

        tx.deleteStaffAttendancePlansBy(period = period)
        logger.debug { "Deleted all staff attendance plans for period $period" }

        tx.insertStaffAttendancePlans(staffAttendancePlans)
        logger.debug { "Inserted ${staffAttendancePlans.size} staff attendance plans" }
    }
}

private fun filterValidShifts(
    shifts: List<Shift>,
    sarastiaIdToEmployeeId: Map<String, EmployeeId>,
): List<Shift> {
    val (withKnownSarastiaId, withUnknownSarastiaId) =
        shifts.partition { sarastiaIdToEmployeeId.containsKey(it.sarastiaId) }
    if (withUnknownSarastiaId.isNotEmpty()) {
        logger.info {
            "No employee found from any enabled daycare for Sarastia IDs: ${withUnknownSarastiaId.map { it.sarastiaId }}"
        }
    }
    val (validTimesShifts, invalidTimesShifts) =
        withKnownSarastiaId.partition { it.startDateTime < it.endDateTime }
    if (invalidTimesShifts.isNotEmpty()) {
        logger.info { "Shifts with invalid times: ${invalidTimesShifts.map { it.workShiftId }}" }
    }
    val sortedShifts =
        validTimesShifts.sortedWith(compareBy<Shift> { it.sarastiaId }.thenBy { it.startDateTime })
    val (validShifts, overlappingShifts) =
        sortedShifts.partitionIndexed { i, shift ->
            i == 0 ||
                shift.sarastiaId != sortedShifts[i - 1].sarastiaId ||
                shift.startDateTime >= sortedShifts[i - 1].endDateTime
        }
    if (overlappingShifts.isNotEmpty()) {
        logger.info { "Overlapping shifts: ${overlappingShifts.map { it.workShiftId }}" }
    }
    return validShifts
}

fun sendStaffAttendancesToLinkity(
    period: FiniteDateRange,
    db: Database.Connection,
    client: LinkityClient,
) {
    lateinit var attendances: Map<EmployeeId, List<ExportableAttendance>>
    lateinit var plans: Map<EmployeeId, List<StaffAttendancePlan>>

    db.transaction { tx ->
        attendances = tx.getStaffAttendancesForEnabledDaycares(period).groupBy { it.employeeId }
        val employeeIds = attendances.keys
        plans =
            tx.findStaffAttendancePlansBy(period = period, employeeIds = employeeIds).groupBy {
                it.employeeId
            }
    }
    val stampings = roundAttendancesToPlans(attendances, plans)
    client.postStampings(
        StampingBatch(
            HelsinkiDateTime.atStartOfDay(period.start),
            HelsinkiDateTime.atStartOfDay(period.end.plusDays(1)),
            stampings,
        )
    )
    logger.debug { "Posted ${stampings.size} stampings to Linkity" }
}

private fun roundAttendancesToPlans(
    attendances: Map<EmployeeId, List<ExportableAttendance>>,
    plans: Map<EmployeeId, List<StaffAttendancePlan>>,
): List<Stamping> {
    return attendances.flatMap { (employeeId, attendances) ->
        val plannedTimes =
            plans[employeeId]?.flatMap { listOf(it.startTime, it.endTime) }?.toSet() ?: emptySet()
        attendances.mapNotNull { attendance ->
            if (attendance.departed == null) {
                return@mapNotNull null
            }
            val roundedArrivalTime =
                plannedTimes.find { it.durationSince(attendance.arrived).abs() <= MAX_DRIFT }
                    ?: attendance.arrived
            val roundedDepartureTime =
                plannedTimes.find { it.durationSince(attendance.departed).abs() <= MAX_DRIFT }
                    ?: attendance.departed

            Stamping(
                stampingId = attendance.id.toString(),
                sarastiaId = attendance.sarastiaId,
                startTime = roundedArrivalTime,
                endTime = roundedDepartureTime,
                type =
                    when (attendance.type) {
                        StaffAttendanceType.PRESENT -> StampingType.PRESENT
                        StaffAttendanceType.OVERTIME -> StampingType.OVERTIME
                        StaffAttendanceType.JUSTIFIED_CHANGE -> StampingType.JUSTIFIED_CHANGE
                        StaffAttendanceType.TRAINING -> StampingType.TRAINING
                        StaffAttendanceType.OTHER_WORK -> StampingType.OTHER_WORK
                    },
            )
        }
    }
}
