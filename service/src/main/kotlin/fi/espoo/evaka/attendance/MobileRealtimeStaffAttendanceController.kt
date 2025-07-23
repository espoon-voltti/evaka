// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.absence.getDaycareIdByGroup
import fi.espoo.evaka.attendance.RealtimeStaffAttendanceController.OpenGroupAttendanceResponse
import fi.espoo.evaka.changes
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.StaffAttendanceExternalId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.core.JdbiException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee-mobile/realtime-staff-attendances")
class MobileRealtimeStaffAttendanceController(private val ac: AccessControl) {
    @GetMapping
    fun getAttendancesByUnit(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam startDate: LocalDate? = null,
        @RequestParam endDate: LocalDate? = null,
    ): CurrentDayStaffAttendanceResponse {
        val today = clock.today()
        val dateRange = FiniteDateRange(startDate ?: today, endDate ?: today)
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_REALTIME_STAFF_ATTENDANCES,
                        unitId,
                    )
                    val unit = tx.getDaycare(unitId) ?: throw NotFound("Unit $unitId not found")
                    val unitOperationalDays =
                        dateRange
                            .dates()
                            .filter { unit.operationDays.contains(it.dayOfWeek.value) }
                            .toList()

                    CurrentDayStaffAttendanceResponse(
                        staff =
                            tx.getStaffAttendances(
                                unitId = unitId,
                                dateRange = dateRange,
                                now = clock.now(),
                            ),
                        extraAttendances = tx.getExternalStaffAttendances(unitId),
                        operationalDays = unitOperationalDays,
                    )
                }
            }
            .also {
                Audit.UnitStaffAttendanceRead.log(
                    targetId = AuditId(unitId),
                    meta =
                        mapOf(
                            "staffCount" to it.staff.size,
                            "externalStaffCount" to it.extraAttendances.size,
                        ),
                )
            }
    }

    @GetMapping("/employee")
    fun getEmployeeAttendances(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestParam employeeId: EmployeeId,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
    ): StaffMemberWithOperationalDays {
        return db.connect { dbc ->
                dbc.read { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_REALTIME_STAFF_ATTENDANCES,
                        unitId,
                    )
                    val unit = tx.getDaycare(unitId) ?: throw NotFound("Unit $unitId not found")
                    val unitOperationalDays =
                        FiniteDateRange(from, to)
                            .dates()
                            .filter { unit.operationDays.contains(it.dayOfWeek.value) }
                            .toList()

                    val staffMember =
                        tx.getStaffAttendances(
                                unitId = unitId,
                                dateRange = FiniteDateRange(from, to),
                                now = clock.now(),
                                employeeId = employeeId,
                            )
                            .find { it.employeeId == employeeId } ?: throw NotFound()

                    StaffMemberWithOperationalDays(
                        staffMember = staffMember,
                        operationalDays = unitOperationalDays,
                    )
                }
            }
            .also {
                Audit.UnitStaffAttendanceRead.log(
                    targetId = AuditId(unitId),
                    objectId = AuditId(employeeId),
                )
            }
    }

    data class StaffArrivalRequest(
        val employeeId: EmployeeId,
        val pinCode: String,
        val groupId: GroupId,
        val time: LocalTime,
        val type: StaffAttendanceType?,
        val hasStaffOccupancyEffect: Boolean?,
    )

    @PostMapping("/arrival")
    fun markArrival(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestBody body: StaffArrivalRequest,
    ) {
        val (updates, changes) =
            try {
                db.connect { dbc ->
                    dbc.transaction { tx ->
                        ac.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Group.MARK_ARRIVAL,
                            body.groupId,
                        )
                    }
                    ac.verifyPinCodeAndThrow(dbc, body.employeeId, body.pinCode, clock)

                    // todo: check that employee has access to a unit related to the group?
                    dbc.transaction { tx ->
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
                                body,
                            )
                        val occupancyCoefficient =
                            body.hasStaffOccupancyEffect?.let {
                                if (it) occupancyCoefficientSeven else occupancyCoefficientZero
                            }
                                ?: tx.getOccupancyCoefficientForEmployee(
                                    body.employeeId,
                                    body.groupId,
                                )
                                ?: BigDecimal.ZERO
                        val updates =
                            attendances.mapNotNull { attendance ->
                                tx.upsertStaffAttendance(
                                    attendance.id,
                                    attendance.employeeId,
                                    attendance.groupId,
                                    attendance.arrived,
                                    attendance.departed,
                                    occupancyCoefficient,
                                    attendance.type,
                                    false,
                                    clock.now(),
                                    user.evakaUserId,
                                )
                            }
                        updates to
                            updates.map {
                                changes(it.old, it.new, StaffAttendanceRealtimeAudit.fields)
                            }
                    }
                }
            } catch (e: JdbiException) {
                throw mapPSQLException(e)
            }
        Audit.StaffAttendanceArrivalCreate.log(
            targetId = AuditId(listOf(body.groupId, body.employeeId)),
            objectId = AuditId(updates.mapNotNull { it.new.id }),
            meta = mapOf("changes" to changes),
        )
    }

    data class StaffDepartureRequest(
        val employeeId: EmployeeId,
        val groupId: GroupId,
        val pinCode: String,
        val time: LocalTime,
        val type: StaffAttendanceType?,
    )

    @PostMapping("/departure")
    fun markDeparture(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestBody body: StaffDepartureRequest,
    ) {
        val (updates, changes) =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Group.MARK_DEPARTURE,
                        body.groupId,
                    )
                }
                ac.verifyPinCodeAndThrow(dbc, body.employeeId, body.pinCode, clock)

                dbc.transaction { tx ->
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
                            body,
                        )
                    val occupancyCoefficient = ongoingAttendance.occupancyCoefficient
                    val updates =
                        attendances.mapNotNull { attendance ->
                            tx.upsertStaffAttendance(
                                attendance.id,
                                attendance.employeeId,
                                attendance.groupId,
                                attendance.arrived,
                                attendance.departed,
                                occupancyCoefficient,
                                attendance.type,
                                false,
                                clock.now(),
                                user.evakaUserId,
                            )
                        }
                    updates to
                        updates.map { changes(it.old, it.new, StaffAttendanceRealtimeAudit.fields) }
                }
            }
        Audit.StaffAttendanceDepartureCreate.log(
            targetId = AuditId(listOf(body.groupId, body.employeeId)),
            objectId = AuditId(updates.mapNotNull { it.new.id }),
            meta = mapOf("changes" to changes),
        )
    }

    data class StaffAttendanceUpdateRequest(
        val employeeId: EmployeeId,
        val pinCode: String,
        val date: LocalDate,
        val rows: List<RealtimeStaffAttendanceController.StaffAttendanceUpsert>,
    )

    data class StaffAttendanceUpdateResponse(
        val deleted: List<StaffAttendanceRealtimeId>,
        val inserted: List<StaffAttendanceRealtimeId>,
    )

    @PutMapping
    fun setAttendances(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam unitId: DaycareId,
        @RequestBody body: StaffAttendanceUpdateRequest,
    ): StaffAttendanceUpdateResponse {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    ac.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.UPDATE_STAFF_ATTENDANCES,
                        unitId,
                    )
                }
                ac.verifyPinCodeAndThrow(dbc, body.employeeId, body.pinCode, clock)

                if (body.rows.any { it.arrived.toLocalDate() != body.date })
                    throw BadRequest("Attendances outside given date")

                dbc.transaction { tx ->
                    val groupIds = body.rows.mapNotNull { it.groupId }.distinct()
                    if (!groupIds.all { groupId -> tx.getDaycareIdByGroup(groupId) == unitId }) {
                        throw BadRequest("Group is not in unit")
                    }

                    val existing =
                        tx.getEmployeeAttendancesByArrivalDateDate(
                            unitId,
                            body.employeeId,
                            body.date,
                        )

                    val ids = body.rows.mapNotNull { it.id }
                    if (ids.any { id -> !existing.map { it.id }.contains(id) }) {
                        throw BadRequest("Unknown id was given")
                    }

                    val deletedIds = existing.map { it.id }
                    val deleted = deletedIds.map { tx.deleteStaffAttendance(it) }

                    val updated =
                        body.rows.mapNotNull { attendance ->
                            tx.upsertStaffAttendance(
                                attendanceId = null,
                                employeeId = body.employeeId,
                                groupId = attendance.groupId,
                                arrivalTime = attendance.arrived,
                                departureTime = attendance.departed,
                                occupancyCoefficient =
                                    if (attendance.hasStaffOccupancyEffect)
                                        occupancyCoefficientSeven
                                    else occupancyCoefficientZero,
                                type = attendance.type,
                                departedAutomatically = false,
                                modifiedAt = clock.now(),
                                modifiedBy = user.evakaUserId,
                            )
                        }

                    val updates = updated + deleted.map { StaffAttendanceRealtimeChange(old = it) }
                    updates to
                        updates.map { changes(it.old, it.new, StaffAttendanceRealtimeAudit.fields) }
                }
            }
            .also { (updates, changes) ->
                Audit.StaffAttendanceUpdate.log(
                    targetId = AuditId(body.employeeId),
                    objectId =
                        AuditId(
                            (updates.mapNotNull { it.new.id } + updates.mapNotNull { it.old.id })
                                .distinct()
                        ),
                    meta = mapOf("date" to body.date, "changes" to changes),
                )
            }
            .let { (updates) ->
                StaffAttendanceUpdateResponse(
                    deleted =
                        updates.mapNotNull {
                            if (it.old.id != null && it.new.id == null) it.old.id else null
                        },
                    inserted =
                        updates.mapNotNull {
                            if (it.old.id != null && it.new.id == null) null else it.new.id
                        },
                )
            }
    }

    data class ExternalStaffArrivalRequest(
        val name: String,
        val groupId: GroupId,
        val arrived: LocalTime,
        val hasStaffOccupancyEffect: Boolean,
    )

    @PostMapping("/arrival-external")
    fun markExternalArrival(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestBody body: ExternalStaffArrivalRequest,
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
                        body.groupId,
                    )

                    it.markExternalStaffArrival(
                        ExternalStaffArrival(
                            name = body.name,
                            groupId = body.groupId,
                            arrived = arrivedTimeOrDefault,
                            occupancyCoefficient =
                                if (body.hasStaffOccupancyEffect) occupancyCoefficientSeven
                                else occupancyCoefficientZero,
                        )
                    )
                }
            }
            .also { staffAttendanceExternalId ->
                Audit.StaffAttendanceArrivalExternalCreate.log(
                    targetId = AuditId(body.groupId),
                    objectId = AuditId(staffAttendanceExternalId),
                )
            }
    }

    data class ExternalStaffDepartureRequest(
        val attendanceId: StaffAttendanceExternalId,
        val time: LocalTime,
    )

    @PostMapping("/departure-external")
    fun markExternalDeparture(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestBody body: ExternalStaffDepartureRequest,
    ) {
        db.connect { dbc ->
            // todo: convert to action auth
            val attendance =
                dbc.read { it.getExternalStaffAttendance(body.attendanceId) }
                    ?: throw NotFound("attendance not found")
            dbc.read {
                ac.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Group.MARK_EXTERNAL_DEPARTURE,
                    attendance.groupId,
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
        Audit.StaffAttendanceDepartureExternalCreate.log(targetId = AuditId(body.attendanceId))
    }

    val ALLOWED_TIME_DRIFT_MINUTES = 1
    val ATTENDANCE_MARKING_ALLOWED_THRESHOLD_MINUTES = 30L + ALLOWED_TIME_DRIFT_MINUTES
    val ALLOWED_DIFF_FROM_PLAN_MINUTES = 5L

    fun createAttendancesFromArrival(
        now: HelsinkiDateTime,
        plans: List<PlannedStaffAttendance>,
        ongoingAttendance: StaffAttendance?,
        latestDepartureToday: StaffAttendance?,
        arrival: StaffArrivalRequest,
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
            type: StaffAttendanceType,
        ) =
            StaffAttendance(
                id = null,
                employeeId = arrival.employeeId,
                groupId = if (type.presentInGroup()) arrival.groupId else null,
                arrived = arrived,
                departed = departed,
                occupancyCoefficient = BigDecimal.ZERO,
                type = type,
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
                            ?: if (arrival.type == null || latestDepartureToday != null) null
                            else createNewAttendance(planStart, arrivalTime, arrival.type),
                        createNewAttendance(arrivalTime, null, StaffAttendanceType.PRESENT),
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
        departure: StaffDepartureRequest,
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
            type: StaffAttendanceType,
        ) =
            StaffAttendance(
                id = null,
                employeeId = departure.employeeId,
                groupId = null,
                arrived = arrived,
                departed = departed,
                occupancyCoefficient = BigDecimal.ZERO,
                type = type,
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
                        },
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
                        createNewAttendance(departureTime, null, departure.type),
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

    @GetMapping("open-attendance")
    fun getOpenGroupAttendance(
        db: Database,
        user: AuthenticatedUser.MobileDevice,
        clock: EvakaClock,
        @RequestParam userId: EmployeeId,
    ): OpenGroupAttendanceResponse {
        val openAttendance =
            db.connect { dbc ->
                    dbc.read { tx ->
                        ac.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Employee.READ_OPEN_GROUP_ATTENDANCE,
                            userId,
                        )
                        tx.getOpenGroupAttendancesForEmployee(userId)
                    }
                }
                .also { Audit.StaffOpenAttendanceRead.log(targetId = AuditId(userId)) }
        return OpenGroupAttendanceResponse(openAttendance)
    }
}
