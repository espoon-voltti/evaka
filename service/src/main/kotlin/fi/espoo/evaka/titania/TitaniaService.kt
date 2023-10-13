// SPDX-FileCopyrightText: 2021-2022 City of Tampere
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.titania

import fi.espoo.evaka.attendance.RawAttendance
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.pis.NewEmployee
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.HelsinkiDateTimeRange
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

private val MAX_DRIFT: Duration = Duration.ofMinutes(5)

@Service
class TitaniaService(private val idConverter: TitaniaEmployeeIdConverter) {

    fun updateWorkingTimeEvents(
        tx: Database.Transaction,
        request: UpdateWorkingTimeEventsRequest
    ): UpdateWorkingTimeEventsResponse {
        logger.debug { "Titania request: $request" }
        val internal = updateWorkingTimeEventsInternal(tx, request)
        logger.debug { "Titania internal response: $internal" }
        val response = UpdateWorkingTimeEventsResponse.ok()
        logger.debug { "Titania response: $response" }
        return response
    }

    fun updateWorkingTimeEventsInternal(
        tx: Database.Transaction,
        request: UpdateWorkingTimeEventsRequest
    ): TitaniaUpdateResponse {
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
        val unknownEmployeeNumbers = employeeNumbers - employeeNumberToId.keys
        val unknownEmployees: List<NewEmployee> =
            unknownEmployeeNumbers
                .map { employeeNumber ->
                    persons.find { person -> person.first == employeeNumber }!!.second
                }
                .map { person ->
                    NewEmployee(
                        firstName = person.firstName(),
                        lastName = person.lastName(),
                        email = null,
                        externalId = null,
                        employeeNumber = person.employeeId,
                        temporaryInUnitId = null,
                        active = true
                    )
                }
        val allEmployeeNumberToId = employeeNumberToId + tx.createEmployees(unknownEmployees)
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
                                            "Event date ${event.date} is out of period (${period.start} - ${period.end})"
                                    )
                                )
                            }
                            val previous = plans.lastOrNull()
                            val next =
                                StaffAttendancePlan(
                                    allEmployeeNumberToId[employeeNumber]!!,
                                    event.code?.let { staffAttendanceTypeFromTitaniaEventCode(it) }
                                        ?: StaffAttendanceType.PRESENT,
                                    HelsinkiDateTime.of(
                                        event.date,
                                        LocalTime.parse(
                                            event.beginTime!!,
                                            DateTimeFormatter.ofPattern(TITANIA_TIME_FORMAT)
                                        )
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
                                                        )
                                                    )
                                            }
                                        }
                                    ),
                                    event.description
                                )
                            if (previous?.canMerge(next) == true) {
                                plans.remove(previous)
                                plans.add(previous.copy(endTime = next.endTime))
                            } else {
                                plans.add(next)
                            }
                            plans
                        }
                }

        logger.info {
            "Removing staff attendance plans for ${employeeNumberToId.size} employees in period $period"
        }
        val deleted =
            tx.deleteStaffAttendancePlansBy(
                employeeIds = employeeNumberToId.values,
                period = period
            )
        logger.info { "Adding ${newPlans.size} new staff attendance plans" }
        tx.insertStaffAttendancePlans(newPlans)

        return TitaniaUpdateResponse(deleted, newPlans)
    }

    fun getStampedWorkingTimeEvents(
        tx: Database.Read,
        request: GetStampedWorkingTimeEventsRequest
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
                .map { (employee, attendances) ->
                    val employeePlans = plans[employee.id]
                    val convertedEmployeeNumber = employeeIdToNumber[employee.id]!!
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
                                    attendances
                                        .flatMap(::splitOvernight)
                                        .sortedBy { it.arrived }
                                        .mapNotNull { attendance ->
                                            val (arrived, arrivedPlan) =
                                                calculateFromPlans(
                                                    employeePlans,
                                                    attendance.arrived
                                                )
                                            if (!period.includes(arrived.toLocalDate())) {
                                                return@mapNotNull null
                                            }
                                            val (departed, departedPlan) =
                                                calculateFromPlan(
                                                    employeePlans,
                                                    attendance.departed
                                                ) ?: Pair(null, null)
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
                                                        StaffAttendanceType.PRESENT -> null
                                                        StaffAttendanceType.OTHER_WORK -> "TA"
                                                        StaffAttendanceType.TRAINING -> "KO"
                                                        StaffAttendanceType.OVERTIME ->
                                                            if (arrivedPlan != null) null else "YT"
                                                        StaffAttendanceType.JUSTIFIED_CHANGE ->
                                                            if (
                                                                isNotFirstInPlan(
                                                                    arrived,
                                                                    arrivedPlan,
                                                                    attendances
                                                                )
                                                            )
                                                                null
                                                            else "PM"
                                                    },
                                                endTime =
                                                    when (departed?.toLocalTime()) {
                                                        null -> null
                                                        LocalTime.MAX.truncatedTo(
                                                            ChronoUnit.MICROS
                                                        ) -> "2400"
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
                                                        StaffAttendanceType.PRESENT -> null
                                                        StaffAttendanceType.OTHER_WORK -> null
                                                        StaffAttendanceType.TRAINING -> null
                                                        StaffAttendanceType.OVERTIME ->
                                                            if (
                                                                departed == null ||
                                                                    departedPlan != null
                                                            )
                                                                null
                                                            else "YT"
                                                        StaffAttendanceType.JUSTIFIED_CHANGE ->
                                                            if (
                                                                departed == null ||
                                                                    isNotLastInPlan(
                                                                        departed,
                                                                        departedPlan,
                                                                        attendances
                                                                    )
                                                            )
                                                                null
                                                            else "PM"
                                                    }
                                            )
                                        }
                            )
                    )
                }

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
                                }
                        )
                    }
            )
        logger.debug { "Titania response: $response" }
        return response
    }

    private fun calculateFromPlan(
        plans: List<StaffAttendancePlan>?,
        event: HelsinkiDateTime?
    ): Pair<HelsinkiDateTime?, StaffAttendancePlan?>? = event?.let { calculateFromPlans(plans, it) }

    private fun calculateFromPlans(
        plans: List<StaffAttendancePlan>?,
        event: HelsinkiDateTime
    ): Pair<HelsinkiDateTime, StaffAttendancePlan?> {
        return plans?.firstNotNullOfOrNull { plan ->
            when {
                event.durationSince(plan.startTime).abs() <= MAX_DRIFT -> Pair(plan.startTime, plan)
                event.durationSince(plan.endTime).abs() <= MAX_DRIFT -> Pair(plan.endTime, plan)
                else -> null
            }
        }
            ?: Pair(
                event,
                plans?.find { plan ->
                    HelsinkiDateTimeRange(plan.startTime, plan.endTime)
                        .contains(HelsinkiDateTimeRange(event, event))
                }
            )
    }

    private fun isNotFirstInPlan(
        event: HelsinkiDateTime,
        plan: StaffAttendancePlan?,
        attendances: List<RawAttendance>
    ) = plan != null && attendances.any { isInPlan(it, plan) && it.arrived < event }

    private fun isNotLastInPlan(
        event: HelsinkiDateTime,
        plan: StaffAttendancePlan?,
        attendances: List<RawAttendance>
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
    val inserted: List<StaffAttendancePlan>
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
