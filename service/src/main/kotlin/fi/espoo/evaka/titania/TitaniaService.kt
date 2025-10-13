// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.attendance.*
import fi.espoo.evaka.pis.getEmployeeIdsByNumbers
import fi.espoo.evaka.pis.getEmployeeIdsByNumbersMapById
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

private val MAX_DRIFT: Duration = Duration.ofMinutes(5)

@Service
class TitaniaService(private val idConverter: TitaniaEmployeeIdConverter) {

    fun updateWorkingTimeEvents(
        tx: Database.Transaction,
        request: UpdateWorkingTimeEventsRequest,
    ): UpdateWorkingTimeEventsServiceResponse {
        logger.debug { "Titania request: $request" }
        val internal = updateWorkingTimeEventsInternal(tx, request)
        logger.debug { "Titania internal response: $internal" }
        val response =
            if (internal.overLappingShifts.isEmpty()) UpdateWorkingTimeEventsResponse.ok()
            else UpdateWorkingTimeEventsResponse.validationFailed()
        logger.debug { "Titania response: $response" }
        return UpdateWorkingTimeEventsServiceResponse(response, internal.overLappingShifts)
    }

    fun updateWorkingTimeEventsInternal(
        tx: Database.Transaction,
        request: UpdateWorkingTimeEventsRequest,
    ): TitaniaUpdateResponse {
        val requestTime = HelsinkiDateTime.now()

        val period = request.period.toDateRange()
        val persons =
            request.schedulingUnit.flatMap { unit ->
                unit.occupation.flatMap { occupation ->
                    occupation.person.mapNotNull { person ->
                        val employeeNumber = idConverter.fromTitania(person.employeeId)
                        if (employeeNumber == "") {
                            logger.warn { "Invalid employee number: (empty string)" }
                            return@mapNotNull null
                        }
                        employeeNumber to person.copy(employeeId = employeeNumber)
                    }
                }
            }
        val employeeNumbers = persons.map { (employeeNumber, _) -> employeeNumber }.distinct()
        val employeeNumberToId = tx.getEmployeeIdsByNumbers(employeeNumbers)

        var unmergedPlans = mutableListOf<StaffAttendancePlan>()
        val overlappingShifts = mutableListOf<TitaniaOverLappingShifts>()

        val newPlans =
            persons
                .sortedBy { it.first }
                .flatMap { (employeeNumber, person) ->
                    person.actualWorkingTimeEvents.event
                        .filter { event -> !IGNORED_EVENT_CODES.contains(event.code) }
                        .filter { event -> event.beginTime != null && event.endTime != null }
                        .sortedWith(compareBy({ it.date }, { it.beginTime }))
                        .fold(mutableListOf<StaffAttendancePlan>()) { plans, event ->
                            if (!period.includes(event.date)) {
                                throw TitaniaException(
                                    TitaniaErrorDetail(
                                        errorcode = TitaniaError.EVENT_DATE_OUT_OF_PERIOD,
                                        message =
                                            "Event date ${event.date} is out of period (${period.start} - ${period.end})",
                                    )
                                )
                            }
                            val previous = plans.lastOrNull()
                            val next =
                                StaffAttendancePlan(
                                    employeeNumberToId[employeeNumber] ?: return@fold plans,
                                    event.code?.let { staffAttendanceTypeFromTitaniaEventCode(it) }
                                        ?: StaffAttendanceType.PRESENT,
                                    HelsinkiDateTime.of(
                                        event.date,
                                        LocalTime.parse(
                                            event.beginTime!!,
                                            DateTimeFormatter.ofPattern(TITANIA_TIME_FORMAT),
                                        ),
                                    ),
                                    HelsinkiDateTime.of(
                                        event.date,
                                        event.endTime!!.let {
                                            when (it) {
                                                "2400" -> LocalTime.of(23, 59)
                                                else ->
                                                    LocalTime.parse(
                                                        it,
                                                        DateTimeFormatter.ofPattern(
                                                            TITANIA_TIME_FORMAT
                                                        ),
                                                    )
                                            }
                                        },
                                    ),
                                    event.description,
                                )
                            if (previous?.canMerge(next) == true) {
                                plans.remove(previous)
                                plans.add(previous.copy(endTime = next.endTime))
                            } else {
                                plans.add(next)
                            }

                            if (unmergedPlans.isEmpty()) {
                                unmergedPlans = mutableListOf(next)
                            } else {
                                // identical shifts are deduplicated later, ignore them here
                                if (next !in unmergedPlans) {
                                    unmergedPlans
                                        .filter { it.employeeId == next.employeeId }
                                        .filter {
                                            HelsinkiDateTimeRange(next.startTime, next.endTime)
                                                .overlaps(
                                                    HelsinkiDateTimeRange(it.startTime, it.endTime)
                                                )
                                        }
                                        .forEach {
                                            overlappingShifts.add(
                                                TitaniaOverLappingShifts(
                                                    employeeNumberToId[employeeNumber]!!,
                                                    next.startTime.toLocalDate(),
                                                    it.startTime.toLocalTime(),
                                                    it.endTime.toLocalTime(),
                                                    next.startTime.toLocalTime(),
                                                    next.endTime.toLocalTime(),
                                                )
                                            )
                                        }

                                    unmergedPlans.add(next)
                                }
                            }

                            plans.distinct().toMutableList()
                        }
                }

        if (overlappingShifts.isNotEmpty()) {
            tx.insertReportRows(requestTime, overlappingShifts)
            return TitaniaUpdateResponse(listOf(), listOf(), overlappingShifts)
        }

        logger.info {
            "Removing staff attendance plans for ${employeeNumberToId.size} employees in period $period"
        }
        val deleted =
            tx.deleteStaffAttendancePlansBy(
                employeeIds = employeeNumberToId.values,
                period = period,
            )
        logger.info { "Adding ${newPlans.size} new staff attendance plans" }
        tx.insertStaffAttendancePlans(newPlans)

        return TitaniaUpdateResponse(deleted, newPlans, listOf())
    }

    fun getStampedWorkingTimeEvents(
        tx: Database.Read,
        request: GetStampedWorkingTimeEventsRequest,
    ): GetStampedWorkingTimeEventsResponse {
        logger.debug { "Titania request: $request" }
        val period = request.period.toDateRange()
        val convertedEmployeeNumbers =
            request.schedulingUnit
                .flatMap { unit -> unit.person.map { person -> person.employeeId } }
                .associateBy { idConverter.fromTitania(it) } // key=converted, value=original
        val employeeIdToNumber = tx.getEmployeeIdsByNumbersMapById(convertedEmployeeNumbers.keys)
        logger.info {
            "Finding staff attendances for ${employeeIdToNumber.size} employees in period $period"
        }
        val attendances =
            tx.findStaffAttendancesBy(employeeIds = employeeIdToNumber.keys, period = period)
        val plans =
            tx.findStaffAttendancePlansBy(employeeIds = employeeIdToNumber.keys, period = period)
                .groupBy { it.employeeId }

        data class EmployeeKey(val id: EmployeeId, val firstName: String, val lastName: String)

        val persons =
            attendances
                .groupBy { EmployeeKey(it.employeeId, it.firstName, it.lastName) }
                .asSequence()
                .sortedWith(compareBy({ it.key.id }, { it.key.firstName }, { it.key.lastName }))
                .map { (employee, attendances) ->
                    val employeePlans = plans[employee.id]
                    val convertedEmployeeNumber = employeeIdToNumber[employee.id]!!
                    val sortedAttendances =
                        attendances.flatMap(::splitOvernight).sortedBy { it.arrived }
                    TitaniaStampedPersonResponse(
                        employeeId =
                            convertedEmployeeNumbers[convertedEmployeeNumber]
                                ?: throw RuntimeException(
                                    "Cannot find original employee number for converted: $convertedEmployeeNumber"
                                ),
                        name = "${employee.lastName} ${employee.firstName}".uppercase(),
                        stampedWorkingTimeEvents =
                            TitaniaStampedWorkingTimeEvents(
                                event =
                                    sortedAttendances.mapIndexedNotNull { i, attendance ->
                                        // CHILD_SICKNESS doesn't create own stamping but alters
                                        // previous/next PRESENT reason code
                                        // from Titania Classic - eVaka Rajapintakuvaus 2025-09-12
                                        // (Ratkausukuvaus_Titania_eVaka.pdf)
                                        if (attendance.type == StaffAttendanceType.CHILD_SICKNESS) {
                                            return@mapIndexedNotNull null
                                        }
                                        val (arrived, arrivedPlan) =
                                            calculateFromPlans(employeePlans, attendance.arrived)
                                                .minBy { it.first }
                                        if (!period.includes(arrived.toLocalDate())) {
                                            return@mapIndexedNotNull null
                                        }
                                        val (departed, departedPlan) =
                                            calculateFromPlan(employeePlans, attendance.departed)
                                                ?.maxBy { it.first } ?: Pair(null, null)
                                        val prev =
                                            if (attendance == sortedAttendances.first()) null
                                            else if (
                                                !sortedAttendances[i - 1].isPrevious(attendance)
                                            )
                                                null
                                            else sortedAttendances[i - 1]
                                        val next =
                                            if (attendance == sortedAttendances.last()) null
                                            else if (!sortedAttendances[i + 1].isNext(attendance))
                                                null
                                            else sortedAttendances[i + 1]
                                        TitaniaStampedWorkingTimeEvent(
                                            date = attendance.arrived.toLocalDate(),
                                            beginTime =
                                                arrived
                                                    .toLocalTime()
                                                    .format(
                                                        DateTimeFormatter.ofPattern(
                                                            TITANIA_TIME_FORMAT
                                                        )
                                                    ),
                                            beginReasonCode =
                                                when (attendance.type) {
                                                    StaffAttendanceType.PRESENT ->
                                                        if (
                                                            prev?.type ==
                                                                StaffAttendanceType.CHILD_SICKNESS
                                                        )
                                                            "LS"
                                                        else null
                                                    StaffAttendanceType.OTHER_WORK -> "TA"
                                                    StaffAttendanceType.TRAINING -> "KO"
                                                    StaffAttendanceType.OVERTIME ->
                                                        if (arrivedPlan != null) null else "YT"
                                                    StaffAttendanceType.JUSTIFIED_CHANGE ->
                                                        if (
                                                            isNotFirstInPlan(
                                                                arrived,
                                                                arrivedPlan,
                                                                attendances,
                                                            )
                                                        )
                                                            null
                                                        else "PM"
                                                    StaffAttendanceType.SICKNESS -> "SA"
                                                    StaffAttendanceType.CHILD_SICKNESS -> null
                                                },
                                            endTime =
                                                when (departed?.toLocalTime()) {
                                                    null -> null
                                                    LocalTime.MAX.truncatedTo(ChronoUnit.MICROS) ->
                                                        "2400"
                                                    else ->
                                                        departed
                                                            .toLocalTime()
                                                            .format(
                                                                DateTimeFormatter.ofPattern(
                                                                    TITANIA_TIME_FORMAT
                                                                )
                                                            )
                                                },
                                            endReasonCode =
                                                when (attendance.type) {
                                                    StaffAttendanceType.PRESENT ->
                                                        if (
                                                            departed != null &&
                                                                next?.type ==
                                                                    StaffAttendanceType
                                                                        .CHILD_SICKNESS
                                                        )
                                                            "LS"
                                                        else null
                                                    StaffAttendanceType.OTHER_WORK -> null
                                                    StaffAttendanceType.TRAINING -> null
                                                    StaffAttendanceType.OVERTIME ->
                                                        if (
                                                            departed == null || departedPlan != null
                                                        )
                                                            null
                                                        else "YT"
                                                    StaffAttendanceType.JUSTIFIED_CHANGE ->
                                                        if (
                                                            departed == null ||
                                                                isNotLastInPlan(
                                                                    departed,
                                                                    departedPlan,
                                                                    attendances,
                                                                )
                                                        )
                                                            null
                                                        else "PM"
                                                    StaffAttendanceType.SICKNESS -> null
                                                    StaffAttendanceType.CHILD_SICKNESS -> null
                                                },
                                        )
                                    }
                            ),
                    )
                }
                .toList()

        val response =
            GetStampedWorkingTimeEventsResponse(
                schedulingUnit =
                    request.schedulingUnit.map { unit ->
                        TitaniaStampedUnitResponse(
                            code = unit.code,
                            name = unit.name,
                            person =
                                persons.filter {
                                    unit.person.find { person ->
                                        person.employeeId == it.employeeId
                                    } != null
                                },
                        )
                    }
            )
        logger.debug { "Titania response: $response" }
        return response
    }

    private fun calculateFromPlan(
        plans: List<StaffAttendancePlan>?,
        event: HelsinkiDateTime?,
    ): List<Pair<HelsinkiDateTime, StaffAttendancePlan?>>? =
        event?.let { calculateFromPlans(plans, it) }

    private fun calculateFromPlans(
        plans: List<StaffAttendancePlan>?,
        event: HelsinkiDateTime,
    ): List<Pair<HelsinkiDateTime, StaffAttendancePlan?>> {
        return (plans ?: emptyList())
            .flatMap { plan ->
                listOfNotNull(
                    plan
                        .takeIf { event.durationSince(it.startTime).abs() <= MAX_DRIFT }
                        ?.let { it.startTime to it },
                    plan
                        .takeIf { event.durationSince(it.endTime).abs() <= MAX_DRIFT }
                        ?.let { it.endTime to it },
                )
            }
            .ifEmpty {
                listOf(
                    event to
                        plans?.find { plan ->
                            HelsinkiDateTimeRange(plan.startTime, plan.endTime)
                                .contains(HelsinkiDateTimeRange(event, event))
                        }
                )
            }
    }

    private fun isNotFirstInPlan(
        event: HelsinkiDateTime,
        plan: StaffAttendancePlan?,
        attendances: List<RawAttendance>,
    ) = plan != null && attendances.any { isInPlan(it, plan) && it.arrived < event }

    private fun isNotLastInPlan(
        event: HelsinkiDateTime,
        plan: StaffAttendancePlan?,
        attendances: List<RawAttendance>,
    ) =
        plan != null &&
            attendances.any { isInPlan(it, plan) && it.departed != null && it.departed > event }

    private fun isInPlan(attendance: RawAttendance, plan: StaffAttendancePlan) =
        attendance.departed != null &&
            HelsinkiDateTimeRange(plan.startTime, plan.endTime)
                .contains(HelsinkiDateTimeRange(attendance.arrived, attendance.departed))
}

data class TitaniaUpdateResponse(
    val deleted: List<StaffAttendancePlan>,
    val inserted: List<StaffAttendancePlan>,
    val overLappingShifts: List<TitaniaOverLappingShifts>,
)

interface TitaniaEmployeeIdConverter {
    fun fromTitania(employeeId: String): String

    companion object {
        fun default(): TitaniaEmployeeIdConverter =
            object : TitaniaEmployeeIdConverter {
                override fun fromTitania(employeeId: String): String = employeeId
            }
    }
}
