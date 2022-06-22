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
import org.jdbi.v3.core.JdbiException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalTime

@RestController
@RequestMapping("/mobile/realtime-staff-attendances")
class MobileRealtimeStaffAttendanceController(private val ac: AccessControl) {
    @GetMapping
    fun getAttendancesByUnit(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestParam unitId: DaycareId
    ): CurrentDayStaffAttendanceResponse {
        Audit.UnitStaffAttendanceRead.log(targetId = unitId)
        ac.requirePermissionFor(user, Action.Unit.READ_REALTIME_STAFF_ATTENDANCES, unitId)

        return db.connect { dbc ->
            dbc.read {
                CurrentDayStaffAttendanceResponse(
                    staff = it.getStaffAttendances(unitId, evakaClock.now()),
                    extraAttendances = it.getExternalStaffAttendances(unitId)
                )
            }
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
        evakaClock: EvakaClock,
        @RequestBody body: StaffArrivalRequest
    ) {
        Audit.StaffAttendanceArrivalCreate.log(targetId = body.groupId, objectId = body.employeeId)

        ac.requirePermissionFor(user, Action.Group.MARK_ARRIVAL, body.groupId)
        ac.verifyPinCode(body.employeeId, body.pinCode)
        // todo: check that employee has access to a unit related to the group?

        try {
            db.connect { dbc ->
                dbc.transaction { tx ->
                    val plannedAttendances = tx.getPlannedStaffAttendances(body.employeeId, evakaClock.now())
                    val ongoingAttendance = tx.getOngoingAttendance(body.employeeId)
                    val attendances = createAttendancesFromArrival(evakaClock.now(), plannedAttendances, ongoingAttendance, body)
                    val occupancyCoefficient = tx.getOccupancyCoefficientForEmployee(body.employeeId, body.groupId) ?: BigDecimal.ZERO
                    attendances.forEach { attendance ->
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
        evakaClock: EvakaClock,
        @RequestBody body: StaffDepartureRequest
    ) {
        Audit.StaffAttendanceDepartureCreate.log()

        ac.requirePermissionFor(user, Action.Group.MARK_DEPARTURE, body.groupId)
        ac.verifyPinCode(body.employeeId, body.pinCode)

        db.connect { dbc ->
            dbc.transaction { tx ->
                val plannedAttendances = tx.getPlannedStaffAttendances(body.employeeId, evakaClock.now())
                val ongoingAttendance = tx.getOngoingAttendance(body.employeeId)
                    ?: throw BadRequest("No ongoing staff attendance found")
                val attendances = createAttendancesFromDeparture(evakaClock.now(), plannedAttendances, ongoingAttendance, body)
                val occupancyCoefficient = tx.getOccupancyCoefficientForEmployee(body.employeeId, body.groupId) ?: BigDecimal.ZERO
                attendances.forEach { attendance ->
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
        evakaClock: EvakaClock,
        @RequestBody body: ExternalStaffArrivalRequest
    ): StaffAttendanceExternalId {
        Audit.StaffAttendanceArrivalExternalCreate.log(targetId = body.groupId)
        ac.requirePermissionFor(user, Action.Group.MARK_EXTERNAL_ARRIVAL, body.groupId)

        val arrivedTimeHDT = evakaClock.now().withTime(body.arrived)
        val nowHDT = evakaClock.now()
        val arrivedTimeOrDefault = if (arrivedTimeHDT.isBefore(nowHDT)) arrivedTimeHDT else nowHDT

        return db.connect { dbc ->
            dbc.transaction {
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
    }

    data class ExternalStaffDepartureRequest(
        val attendanceId: StaffAttendanceExternalId,
        val time: LocalTime
    )
    @PostMapping("/departure-external")
    fun markExternalDeparture(
        db: Database,
        user: AuthenticatedUser,
        evakaClock: EvakaClock,
        @RequestBody body: ExternalStaffDepartureRequest
    ) {
        Audit.StaffAttendanceDepartureExternalCreate.log(body.attendanceId)

        db.connect { dbc ->
            // todo: convert to action auth
            val attendance = dbc.read { it.getExternalStaffAttendance(body.attendanceId) }
                ?: throw NotFound("attendance not found")
            ac.requirePermissionFor(user, Action.Group.MARK_EXTERNAL_DEPARTURE, attendance.groupId)

            val departedTimeHDT = evakaClock.now().withTime(body.time)
            val nowHDT = evakaClock.now()
            val departedTimeOrDefault = if (departedTimeHDT.isBefore(nowHDT)) departedTimeHDT else nowHDT

            dbc.transaction {
                it.markExternalStaffDeparture(
                    ExternalStaffDeparture(
                        id = body.attendanceId,
                        departed = departedTimeOrDefault
                    )
                )
            }
        }
    }

    fun createAttendancesFromArrival(
        now: HelsinkiDateTime,
        plans: List<PlannedStaffAttendance>,
        ongoingAttendance: StaffAttendance?,
        arrival: StaffArrivalRequest
    ): List<StaffAttendance> {
        val arrivalTime = now.withTime(arrival.time).takeIf { it <= now }
            ?: throw BadRequest("Arrival time cannot be in the future")

        fun createNewAttendance(arrived: HelsinkiDateTime, departed: HelsinkiDateTime?, type: StaffAttendanceType) =
            StaffAttendance(
                id = null,
                employeeId = arrival.employeeId,
                groupId = arrival.groupId,
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
            return listOf(createNewAttendance(arrivalTime, null, StaffAttendanceType.JUSTIFIED_CHANGE))
        }

        val planStart = plans.minOf { it.start }
        if (arrivalTime < planStart.minusMinutes(15)) {
            return when (arrival.type) {
                StaffAttendanceType.OVERTIME -> listOf(createNewAttendance(arrivalTime, null, arrival.type))
                else -> throw BadRequest("Staff attendance type ${arrival.type} cannot be used when arrived 15 minutes before plan start")
            }
        }

        if (arrivalTime > planStart.plusMinutes(15)) {
            return when (arrival.type) {
                StaffAttendanceType.TRAINING, StaffAttendanceType.OTHER_WORK -> {
                    if (ongoingAttendance != null && ongoingAttendance.type != arrival.type) {
                        throw BadRequest("Arrival type ${arrival.type} does not match ongoing attendance type ${ongoingAttendance.type}")
                    }
                    listOf(
                        ongoingAttendance?.copy(departed = arrivalTime)
                            ?: createNewAttendance(planStart, arrivalTime, arrival.type),
                        createNewAttendance(arrivalTime, null, StaffAttendanceType.PRESENT)
                    )
                }
                else -> throw BadRequest("Staff attendance type ${arrival.type} cannot be used when arrived 15 minutes after plan start")
            }
        }

        return if (arrival.type == null) {
            listOf(createNewAttendance(arrivalTime, null, StaffAttendanceType.PRESENT))
        } else {
            throw BadRequest("Staff attendance type should not be used when arrived within 15 minutes of plan start")
        }
    }

    fun createAttendancesFromDeparture(
        now: HelsinkiDateTime,
        plans: List<PlannedStaffAttendance>,
        ongoingAttendance: StaffAttendance,
        departure: StaffDepartureRequest
    ): List<StaffAttendance> {
        val departureTime = now.withTime(departure.time).takeIf { it <= now }
            ?: throw BadRequest("Departure time cannot be in the future")

        fun createNewAttendance(arrived: HelsinkiDateTime, departed: HelsinkiDateTime?, type: StaffAttendanceType) =
            StaffAttendance(
                id = null,
                employeeId = departure.employeeId,
                groupId = departure.groupId,
                arrived = arrived,
                departed = departed,
                occupancyCoefficient = BigDecimal.ZERO,
                type = type
            )

        if (plans.isEmpty()) {
            return listOf(ongoingAttendance.copy(departed = departureTime))
        }

        if (!ongoingAttendance.type.presentInGroup()) {
            throw BadRequest("Trying to mark a departure for an employee that is not present in group")
        }

        if (departure.type == StaffAttendanceType.JUSTIFIED_CHANGE) {
            return listOf(
                ongoingAttendance.copy(
                    departed = departureTime,
                    type = if (ongoingAttendance.type == StaffAttendanceType.PRESENT)
                        StaffAttendanceType.JUSTIFIED_CHANGE
                    else ongoingAttendance.type
                )
            )
        }

        val planEnd = plans.maxOf { it.end }
        if (departureTime < planEnd.minusMinutes(15)) {
            return when (departure.type) {
                StaffAttendanceType.TRAINING, StaffAttendanceType.OTHER_WORK -> listOf(
                    ongoingAttendance.copy(departed = departureTime),
                    createNewAttendance(departureTime, null, departure.type)
                )
                else -> throw BadRequest("Staff attendance type ${departure.type} cannot be used when departed 15 minutes before plan end")
            }
        }

        if (departureTime > planEnd.plusMinutes(15)) {
            return when (departure.type) {
                StaffAttendanceType.OVERTIME ->
                    listOf(ongoingAttendance.copy(departed = departureTime, type = departure.type))
                else -> throw BadRequest("Staff attendance type ${departure.type} cannot be used when departed 15 minutes after plan end")
            }
        }

        return listOf(ongoingAttendance.copy(departed = departureTime))
    }
}
