// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalTime
import org.jdbi.v3.core.JdbiException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/mobile/realtime-staff-attendances")
class MobileRealtimeStaffAttendanceController(private val ac: AccessControl) {
    @GetMapping
    fun getAttendancesByUnit(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId
    ): CurrentDayStaffAttendanceResponse {
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_REALTIME_STAFF_ATTENDANCES,
                        unitId
                    )
                    CurrentDayStaffAttendanceResponse(
                        staff = tx.getStaffAttendances(unitId, clock.now()),
                        extraAttendances = tx.getExternalStaffAttendances(unitId, clock.now())
                    )
                }
            }
            .also {
                Audit.UnitStaffAttendanceRead.log(
                    targetId = unitId,
                    meta =
                        mapOf(
                            "staffCount" to it.staff.size,
                            "externalStaffCount" to it.extraAttendances.size
                        )
                )
            }
    }

    data class StaffArrivalRequest(
        val employeeId: EmployeeId,
        val pinCode: String,
        val groupId: GroupId,
        val time: LocalTime,
        val type: StaffAttendanceType?
    )

    @PostMapping("/arrival")
    fun markArrival(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: StaffArrivalRequest
    ) {
        val staffAttendanceIds =
            try {
                db.connect { dbc ->
                    dbc.transaction { tx ->
                        ac.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Group.MARK_ARRIVAL,
                            body.groupId
                        )
                        ac.verifyPinCodeAndThrow(tx, body.employeeId, body.pinCode, clock)
                        // todo: check that employee has access to a unit related to the group?

                        val plannedAttendances =
                            tx.getPlannedStaffAttendances(body.employeeId, clock.now())
                        val ongoingAttendance = tx.getOngoingAttendance(body.employeeId)
                        val latestDepartureToday =
                            tx.getLatestDepartureToday(body.employeeId, clock.now())
                        val attendances =
                            createAttendancesFromArrival(
                                clock.now(),
                                plannedAttendances,
                                ongoingAttendance,
                                latestDepartureToday,
                                body
                            )
                        val occupancyCoefficient =
                            tx.getOccupancyCoefficientForEmployee(body.employeeId, body.groupId)
                                ?: BigDecimal.ZERO
                        attendances.map { attendance ->
                            tx.upsertStaffAttendance(
                                attendance.id,
                                attendance.employeeId,
                                attendance.groupId,
                                attendance.arrived,
                                attendance.departed,
                                occupancyCoefficient,
                                attendance.type
                            )
                        }
                    }
                }
            } catch (e: JdbiException) {
                throw mapPSQLException(e)
            }
        Audit.StaffAttendanceArrivalCreate.log(
            targetId = listOf(body.groupId, body.employeeId),
            objectId = staffAttendanceIds
        )
    }

    data class StaffDepartureRequest(
        val employeeId: EmployeeId,
        val groupId: GroupId,
        val pinCode: String,
        val time: LocalTime,
        val type: StaffAttendanceType?
    )

    @PostMapping("/departure")
    fun markDeparture(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: StaffDepartureRequest
    ) {
        val staffAttendanceIds =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Group.MARK_DEPARTURE,
                        body.groupId
                    )
                    ac.verifyPinCodeAndThrow(tx, body.employeeId, body.pinCode, clock)

                    val plannedAttendances =
                        tx.getPlannedStaffAttendances(body.employeeId, clock.now())
                    val ongoingAttendance =
                        tx.getOngoingAttendance(body.employeeId)
                            ?: throw BadRequest("No ongoing staff attendance found")
                    val attendances =
                        createAttendancesFromDeparture(
                            clock.now(),
                            plannedAttendances,
                            ongoingAttendance,
                            body
                        )
                    val occupancyCoefficient =
                        tx.getOccupancyCoefficientForEmployee(body.employeeId, body.groupId)
                            ?: BigDecimal.ZERO
                    attendances.map { attendance ->
                        tx.upsertStaffAttendance(
                            attendance.id,
                            attendance.employeeId,
                            attendance.groupId,
                            attendance.arrived,
                            attendance.departed,
                            occupancyCoefficient,
                            attendance.type
                        )
                    }
                }
            }
        Audit.StaffAttendanceDepartureCreate.log(
            targetId = listOf(body.groupId, body.employeeId),
            objectId = staffAttendanceIds
        )
    }

    data class ExternalStaffArrivalRequest(
        val name: String,
        val groupId: GroupId,
        val arrived: LocalTime
    )

    @PostMapping("/arrival-external")
    fun markExternalArrival(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: ExternalStaffArrivalRequest
    ): StaffAttendanceExternalId {
        val arrivedTimeHDT = clock.now().withTime(body.arrived)
        val nowHDT = clock.now()
        val arrivedTimeOrDefault = if (arrivedTimeHDT.isBefore(nowHDT)) arrivedTimeHDT else nowHDT

        return db.connect { dbc ->
                dbc.transaction {
                    ac.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Group.MARK_EXTERNAL_ARRIVAL,
                        body.groupId
                    )

                    it.markExternalStaffArrival(
                        ExternalStaffArrival(
                            name = body.name,
                            groupId = body.groupId,
                            arrived = arrivedTimeOrDefault,
                            occupancyCoefficient = occupancyCoefficientSeven
                        )
                    )
                }
            }
            .also { staffAttendanceExternalId ->
                Audit.StaffAttendanceArrivalExternalCreate.log(
                    targetId = body.groupId,
                    objectId = staffAttendanceExternalId
                )
            }
    }

    data class ExternalStaffDepartureRequest(
        val attendanceId: StaffAttendanceExternalId,
        val time: LocalTime
    )

    @PostMapping("/departure-external")
    fun markExternalDeparture(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: ExternalStaffDepartureRequest
    ) {
        db.connect { dbc ->
            // todo: convert to action auth
            val attendance =
                dbc.read { it.getExternalStaffAttendance(body.attendanceId, clock.now()) }
                    ?: throw NotFound("attendance not found")
            dbc.read {
                ac.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Group.MARK_EXTERNAL_DEPARTURE,
                    attendance.groupId
                )
            }

            val departedTimeHDT = clock.now().withTime(body.time)
            val nowHDT = clock.now()
            val departedTimeOrDefault =
                if (departedTimeHDT.isBefore(nowHDT)) departedTimeHDT else nowHDT

            dbc.transaction {
                it.markExternalStaffDeparture(
                    ExternalStaffDeparture(id = body.attendanceId, departed = departedTimeOrDefault)
                )
            }
        }
        Audit.StaffAttendanceDepartureExternalCreate.log(targetId = body.attendanceId)
    }

    val ATTENDANCE_MARKING_ALLOWED_THRESHOLD_MINUTES = 30L
    val ALLOWED_DIFF_FROM_PLAN_MINUTES = 5L

    fun createAttendancesFromArrival(
        now: HelsinkiDateTime,
        plans: List<PlannedStaffAttendance>,
        ongoingAttendance: StaffAttendance?,
        latestDepartureToday: StaffAttendance?,
        arrival: StaffArrivalRequest
    ): List<StaffAttendance> {
        val arrivalTime =
            now.withTime(arrival.time).takeIf {
                it <= now.plusMinutes(ATTENDANCE_MARKING_ALLOWED_THRESHOLD_MINUTES) &&
                    it >= now.minusMinutes(ATTENDANCE_MARKING_ALLOWED_THRESHOLD_MINUTES)
            }
                ?: throw BadRequest(
                    "Arrival time is not allowed to differ from now for more than $ATTENDANCE_MARKING_ALLOWED_THRESHOLD_MINUTES minutes"
                )

        fun createNewAttendance(
            arrived: HelsinkiDateTime,
            departed: HelsinkiDateTime?,
            type: StaffAttendanceType
        ) =
            StaffAttendance(
                id = null,
                employeeId = arrival.employeeId,
                groupId = if (type.presentInGroup()) arrival.groupId else null,
                arrived = arrived,
                departed = departed,
                occupancyCoefficient = BigDecimal.ZERO,
                type = type
            )

        if (plans.isEmpty()) {
            return listOf(createNewAttendance(arrivalTime, null, StaffAttendanceType.PRESENT))
        }

        if (ongoingAttendance != null && ongoingAttendance.type.presentInGroup()) {
            throw BadRequest("Trying to mark an arrival for an employee that is already present")
        }

        if (arrival.type == StaffAttendanceType.JUSTIFIED_CHANGE) {
            return listOf(
                createNewAttendance(arrivalTime, null, StaffAttendanceType.JUSTIFIED_CHANGE)
            )
        }

        val planStart = plans.minOf { it.start }
        if (arrivalTime < planStart.minusMinutes(ALLOWED_DIFF_FROM_PLAN_MINUTES)) {
            return when (arrival.type) {
                StaffAttendanceType.OVERTIME ->
                    listOf(createNewAttendance(arrivalTime, null, arrival.type))
                else ->
                    throw BadRequest(
                        "Staff attendance type ${arrival.type} cannot be used when arrived $ALLOWED_DIFF_FROM_PLAN_MINUTES minutes before plan start"
                    )
            }
        }

        if (arrivalTime > planStart.plusMinutes(ALLOWED_DIFF_FROM_PLAN_MINUTES)) {
            return when (arrival.type) {
                StaffAttendanceType.JUSTIFIED_CHANGE,
                StaffAttendanceType.TRAINING,
                StaffAttendanceType.OTHER_WORK,
                null -> {
                    if (ongoingAttendance != null && ongoingAttendance.type != arrival.type) {
                        throw BadRequest(
                            "Arrival type ${arrival.type} does not match ongoing attendance type ${ongoingAttendance.type}"
                        )
                    }
                    listOfNotNull(
                        ongoingAttendance?.copy(departed = arrivalTime)
                            ?: if (latestDepartureToday != null) null
                            else
                                createNewAttendance(
                                    planStart,
                                    arrivalTime,
                                    arrival.type ?: StaffAttendanceType.PRESENT
                                ),
                        createNewAttendance(arrivalTime, null, StaffAttendanceType.PRESENT)
                    )
                }
                else ->
                    throw BadRequest(
                        "Staff attendance type ${arrival.type} cannot be used when arrived $ALLOWED_DIFF_FROM_PLAN_MINUTES minutes after plan start"
                    )
            }
        }

        return if (arrival.type == null) {
            listOf(createNewAttendance(arrivalTime, null, StaffAttendanceType.PRESENT))
        } else {
            throw BadRequest(
                "Staff attendance type should not be used when arrived within $ALLOWED_DIFF_FROM_PLAN_MINUTES minutes of plan start"
            )
        }
    }

    fun createAttendancesFromDeparture(
        now: HelsinkiDateTime,
        plans: List<PlannedStaffAttendance>,
        ongoingAttendance: StaffAttendance,
        departure: StaffDepartureRequest
    ): List<StaffAttendance> {
        val departureTime =
            now.withTime(departure.time).takeIf { it <= now }
                ?: throw BadRequest("Departure time cannot be in the future")

        if (departureTime <= ongoingAttendance.arrived) {
            throw BadRequest("Departure time must be after arrival time")
        }

        fun createNewAttendance(
            arrived: HelsinkiDateTime,
            departed: HelsinkiDateTime?,
            type: StaffAttendanceType
        ) =
            StaffAttendance(
                id = null,
                employeeId = departure.employeeId,
                groupId = null,
                arrived = arrived,
                departed = departed,
                occupancyCoefficient = BigDecimal.ZERO,
                type = type
            )

        if (plans.isEmpty()) {
            return listOf(ongoingAttendance.copy(departed = departureTime))
        }

        if (!ongoingAttendance.type.presentInGroup()) {
            throw BadRequest(
                "Trying to mark a departure for an employee that is not present in group"
            )
        }

        // If no reason is given, just end whatever is going on
        if (departure.type == null) {
            return listOf(ongoingAttendance.copy(departed = departureTime))
        }

        if (departure.type == StaffAttendanceType.JUSTIFIED_CHANGE) {
            return listOf(
                ongoingAttendance.copy(
                    departed = departureTime,
                    type =
                        if (ongoingAttendance.type == StaffAttendanceType.PRESENT) {
                            StaffAttendanceType.JUSTIFIED_CHANGE
                        } else {
                            ongoingAttendance.type
                        }
                )
            )
        }

        val planEnd = plans.maxOf { it.end }
        if (departureTime < planEnd.minusMinutes(ALLOWED_DIFF_FROM_PLAN_MINUTES)) {
            return when (departure.type) {
                StaffAttendanceType.TRAINING,
                StaffAttendanceType.OTHER_WORK ->
                    listOf(
                        ongoingAttendance.copy(departed = departureTime),
                        createNewAttendance(departureTime, null, departure.type)
                    )
                else ->
                    throw BadRequest(
                        "Staff attendance type ${departure.type} cannot be used when departed $ALLOWED_DIFF_FROM_PLAN_MINUTES minutes or more before plan end"
                    )
            }
        }

        if (departureTime > planEnd.plusMinutes(ALLOWED_DIFF_FROM_PLAN_MINUTES)) {
            return when (departure.type) {
                StaffAttendanceType.OVERTIME ->
                    listOf(ongoingAttendance.copy(departed = departureTime, type = departure.type))
                else ->
                    throw BadRequest(
                        "Staff attendance type ${departure.type} cannot be used when departed $ALLOWED_DIFF_FROM_PLAN_MINUTES minutes after plan end"
                    )
            }
        }

        return listOf(ongoingAttendance.copy(departed = departureTime))
    }
}
