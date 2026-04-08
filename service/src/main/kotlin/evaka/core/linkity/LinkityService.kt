// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.linkity

import evaka.core.EvakaEnv
import evaka.core.attendance.*
import evaka.core.shared.EmployeeId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.utils.partitionIndexed
import evaka.instance.espoo.EspooAsyncJob
import evaka.instance.espoo.LinkityEnv
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.LocalDate
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

private val logger = KotlinLogging.logger {}

@Service
class LinkitySyncService(
    val linkityEnv: LinkityEnv?,
    val jsonMapper: JsonMapper,
    evakaEnv: EvakaEnv,
) {

    private val maxDrift: Duration = evakaEnv.staffAttendanceDriftMinutes

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

    val employeeNumbers = linkityShifts.map { sarastiaIdToEmployeeNumber(it.sarastiaId) }.toSet()

    db.transaction { tx ->
        val sarastiaIdToEmployeeId =
            tx.getEmployeeIdsForEnabledDaycares(employeeNumbers).mapKeys { (k, _) ->
                employeeNumberToSarastiaId(k)
            }

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
        logger.info { "Inserted ${staffAttendancePlans.size} staff attendance plans from Linkity" }
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
            "Got shifts from Linkity for unknown Sarastia IDs: ${withUnknownSarastiaId.map { it.sarastiaId }.toSet()}"
        }
    }
    val (validTimesShifts, invalidTimesShifts) =
        withKnownSarastiaId.partition { it.startDateTime < it.endDateTime }
    if (invalidTimesShifts.isNotEmpty()) {
        logger.info {
            "Got shifts from Linkity with invalid times: ${invalidTimesShifts.map { it.workShiftId }}"
        }
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
        logger.info {
            "Got overlapping shifts from Linkity: ${overlappingShifts.map { it.workShiftId }}"
        }
    }
    return validShifts
}

fun sendStaffAttendancesToLinkity(
    period: FiniteDateRange,
    db: Database.Connection,
    client: LinkityClient,
    maxDrift: Duration,
) {
    lateinit var attendances: Map<EmployeeId, List<ExportableAttendance>>
    lateinit var plans: Map<EmployeeId, List<StaffAttendancePlan>>

    db.transaction { tx ->
        attendances =
            tx.getStaffAttendancesForEnabledDaycares(period)
                .map { it.copy(sarastiaId = employeeNumberToSarastiaId(it.sarastiaId)) }
                .groupBy { it.employeeId }
        val employeeIds = attendances.keys
        plans =
            tx.findStaffAttendancePlansBy(period = period, employeeIds = employeeIds).groupBy {
                it.employeeId
            }
    }
    val stampings = roundAttendancesToPlans(attendances, plans, maxDrift)
    client.postStampings(
        StampingBatch(
            HelsinkiDateTime.atStartOfDay(period.start),
            HelsinkiDateTime.atStartOfDay(period.end.plusDays(1)),
            stampings,
        )
    )
    logger.info { "Posted ${stampings.size} stampings to Linkity" }
}

private fun roundAttendancesToPlans(
    attendances: Map<EmployeeId, List<ExportableAttendance>>,
    plans: Map<EmployeeId, List<StaffAttendancePlan>>,
    maxDrift: Duration,
): List<Stamping> {
    return attendances.flatMap { (employeeId, attendances) ->
        val plannedTimes =
            plans[employeeId]?.flatMap { listOf(it.startTime, it.endTime) }?.toSet() ?: emptySet()
        attendances.mapNotNull { attendance ->
            if (attendance.departed == null) {
                return@mapNotNull null
            }
            val roundedArrivalTime =
                plannedTimes.find { it.durationSince(attendance.arrived).abs() <= maxDrift }
                    ?: attendance.arrived
            val roundedDepartureTime =
                plannedTimes.find { it.durationSince(attendance.departed).abs() <= maxDrift }
                    ?: attendance.departed

            Stamping(
                stampingId = attendance.id.toString(),
                sarastiaId = attendance.sarastiaId,
                startTime = roundedArrivalTime,
                endTime = roundedDepartureTime,
                stampingType =
                    when (attendance.type) {
                        StaffAttendanceType.PRESENT -> StampingType.PRESENT
                        StaffAttendanceType.OVERTIME -> StampingType.OVERTIME
                        StaffAttendanceType.JUSTIFIED_CHANGE -> StampingType.JUSTIFIED_CHANGE
                        StaffAttendanceType.TRAINING -> StampingType.TRAINING
                        StaffAttendanceType.OTHER_WORK -> StampingType.OTHER_WORK
                        StaffAttendanceType.SICKNESS -> StampingType.OTHER_WORK
                        StaffAttendanceType.CHILD_SICKNESS -> StampingType.OTHER_WORK
                    },
            )
        }
    }
}

fun employeeNumberToSarastiaId(employeeNumber: String): String = "9$employeeNumber"

fun sarastiaIdToEmployeeNumber(sarastiaId: String): String = sarastiaId.removePrefix("9")
