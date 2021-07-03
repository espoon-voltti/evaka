// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.helsinkiZone
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.messaging.daycarydailynote.getDaycareDailyNotesForChildrenPlacedInUnit
import fi.espoo.evaka.pis.employeePinIsCorrect
import fi.espoo.evaka.pis.getEmployeeUser
import fi.espoo.evaka.pis.resetEmployeePinFailureCount
import fi.espoo.evaka.pis.updateEmployeePinFailureCountAndCheckIfLocked
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.toHelsinkiDateTime
import fi.espoo.evaka.shared.utils.dateNow
import fi.espoo.evaka.shared.utils.europeHelsinki
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.UUID

val preschoolStart: LocalTime = LocalTime.of(9, 0)
val preschoolEnd: LocalTime = LocalTime.of(13, 0)
val preschoolMinimumDuration: Duration = Duration.ofHours(1)

val preparatoryStart: LocalTime = LocalTime.of(9, 0)
val preparatoryEnd: LocalTime = LocalTime.of(14, 0)
val preparatoryMinimumDuration: Duration = Duration.ofHours(1)

val connectedDaycareBuffer: Duration = Duration.ofMinutes(15)
val fiveYearOldFreeLimit: Duration = Duration.ofMinutes(4 * 60 + 15)

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/attendances")
class ChildAttendanceController(
    private val acl: AccessControlList
) {
    val authorizedRoles = arrayOf(
        UserRole.ADMIN,
        UserRole.SERVICE_WORKER,
        UserRole.UNIT_SUPERVISOR,
        UserRole.STAFF,
        UserRole.MOBILE
    )

    data class GetChildSensitiveInfoRequest(
        val pin: String,
        val staffId: UUID
    )

    @PostMapping("/child/{childId}")
    fun getChildSensitiveInfo(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: GetChildSensitiveInfoRequest
    ): ResponseEntity<ChildResult> {
        Audit.ChildSensitiveInfoRead.log(targetId = childId, objectId = body.staffId)

        val employeeUser = db.read { it.getEmployeeUser(body.staffId) }
        if (employeeUser != null) {
            try {
                acl.getRolesForChild(AuthenticatedUser.Employee(employeeUser), childId).requireOneOfRoles(*authorizedRoles)
            } catch (e: Forbidden) {
                logger.warn("Unallowed user ${body.staffId} tried to access child info for $childId")
                return ResponseEntity.ok(ChildResult(status = ChildResultStatus.WRONG_PIN, child = null))
            }
        } else {
            logger.warn("Unknown user $${body.staffId} tried to access child info for $childId")
            return ResponseEntity.ok(ChildResult(status = ChildResultStatus.WRONG_PIN, child = null))
        }

        val result = db.transaction { tx ->
            if (tx.employeePinIsCorrect(body.staffId, body.pin)) {
                tx.resetEmployeePinFailureCount(body.staffId)
                tx.getChildSensitiveInfo(childId)?.let {
                    ChildResult(
                        status = ChildResultStatus.SUCCESS,
                        child = it
                    )
                } ?: ChildResult(status = ChildResultStatus.NOT_FOUND)
            } else {
                if (tx.updateEmployeePinFailureCountAndCheckIfLocked(body.staffId)) {
                    ChildResult(status = ChildResultStatus.PIN_LOCKED, child = null)
                } else {
                    ChildResult(status = ChildResultStatus.WRONG_PIN, child = null)
                }
            }
        }

        return ResponseEntity.ok(result)
    }

    @GetMapping("/units/{unitId}")
    fun getAttendances(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesRead.log(targetId = unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { tx ->
            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class ArrivalRequest(
        @DateTimeFormat(pattern = "HH:mm") val arrived: LocalTime
    )
    @PostMapping("/units/{unitId}/children/{childId}/arrival")
    fun postArrival(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: ArrivalRequest
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesArrivalCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.fetchChildPlacementBasics(childId, unitId)

            if (tx.getChildCurrentDayAttendance(childId, unitId) != null)
                throw Conflict("Cannot arrive, already arrived today")

            tx.deleteAbsencesByDate(childId, LocalDate.now(europeHelsinki))
            try {
                tx.insertAttendance(
                    childId = childId,
                    unitId = unitId,
                    arrived = ZonedDateTime.of(LocalDate.now(europeHelsinki).atTime(body.arrived), europeHelsinki).toInstant(),
                    departed = null
                )
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-coming")
    fun returnToComing(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesReturnToComing.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.fetchChildPlacementBasics(childId, unitId)
            tx.deleteAbsencesByDate(childId, LocalDate.now(europeHelsinki))

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance != null) {
                if (attendance.departed == null) {
                    try {
                        tx.deleteAttendance(attendance.id)
                    } catch (e: Exception) {
                        throw mapPSQLException(e)
                    }
                } else {
                    throw Conflict("Already departed, did you mean return-to-present?")
                }
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class DepartureInfoResponse(
        val absentFrom: Set<CareType>
    )
    @GetMapping("/units/{unitId}/children/{childId}/departure")
    fun getChildDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestParam @DateTimeFormat(pattern = "HH:mm") time: LocalTime
    ): ResponseEntity<DepartureInfoResponse> {
        Audit.ChildAttendancesDepartureRead.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.read { tx ->
            val placementBasics = tx.fetchChildPlacementBasics(childId, unitId)

            val attendance = tx.getChildOngoingAttendance(childId, unitId)
            if (attendance == null) {
                throw Conflict("Cannot depart, has not yet arrived")
            } else if (attendance.departed != null) {
                throw Conflict("Cannot depart, already departed")
            }

            val arrived = LocalTime.ofInstant(attendance.arrived, europeHelsinki)

            // temporary hotfix for case where scheduled job was missing and child arrived yesterday
            val forgottenToDepart = attendance.arrived.toHelsinkiDateTime().toLocalDate() != LocalDate.now(helsinkiZone)
            DepartureInfoResponse(
                absentFrom = if (forgottenToDepart) emptySet() else getPartialAbsenceCareTypes(placementBasics, arrived, time)
            )
        }.let { ResponseEntity.ok(it) }
    }

    data class DepartureRequest(
        @DateTimeFormat(pattern = "HH:mm") val departed: LocalTime,
        val absenceType: AbsenceType?
    )
    @PostMapping("/units/{unitId}/children/{childId}/departure")
    fun postDeparture(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: DepartureRequest
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesDepartureCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            val placementBasics = tx.fetchChildPlacementBasics(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance == null) {
                // temporary hotfix for case where scheduled job was missing and child arrived yesterday
                val forgottenAttendance: ChildAttendance = tx.getChildOngoingAttendance(childId, unitId)
                    ?: throw Conflict("Cannot depart, has not yet arrived")
                tx.updateAttendanceEnd(
                    attendanceId = forgottenAttendance.id,
                    departed = forgottenAttendance.arrived.toHelsinkiDateTime().withTime(LocalTime.of(23, 59)).toInstant()
                )
                return@transaction tx.getAttendancesResponse(unitId)
            } else if (attendance.departed != null) {
                throw Conflict("Cannot depart, already departed")
            }

            val absentFrom = getPartialAbsenceCareTypes(placementBasics, LocalTime.ofInstant(attendance.arrived, europeHelsinki), body.departed)
            tx.deleteAbsencesByDate(childId, LocalDate.now(europeHelsinki))
            if (absentFrom.isNotEmpty()) {
                if (body.absenceType == null) {
                    throw BadRequest("Request had no absenceType but child was absent from ${absentFrom.joinToString(", ")}.")
                }

                absentFrom.forEach { careType ->
                    tx.insertAbsence(user, childId, LocalDate.now(europeHelsinki), careType, body.absenceType)
                }
            } else if (body.absenceType != null) {
                throw BadRequest("Request defines absenceType but child was not absent.")
            }

            try {
                tx.updateAttendanceEnd(
                    attendanceId = attendance.id,
                    departed = ZonedDateTime.of(LocalDate.now(europeHelsinki).atTime(body.departed), europeHelsinki).toInstant()
                )
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/units/{unitId}/children/{childId}/return-to-present")
    fun returnToPresent(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesReturnToPresent.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            tx.fetchChildPlacementBasics(childId, unitId)
            tx.deleteAbsencesByDate(childId, LocalDate.now(europeHelsinki))

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)

            if (attendance?.departed == null) {
                throw Conflict("Can not return to present since not yet departed")
            } else {
                try {
                    tx.updateAttendance(attendance.id, attendance.arrived, null)
                } catch (e: Exception) {
                    throw mapPSQLException(e)
                }
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class FullDayAbsenceRequest(
        val absenceType: AbsenceType
    )
    @PostMapping("/units/{unitId}/children/{childId}/full-day-absence")
    fun postFullDayAbsence(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: FullDayAbsenceRequest
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesFullDayAbsenceCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            val placementBasics = tx.fetchChildPlacementBasics(childId, unitId)

            val attendance = tx.getChildCurrentDayAttendance(childId, unitId)
            if (attendance != null) {
                throw Conflict("Cannot add full day absence, child already has attendance")
            }

            try {
                tx.deleteAbsencesByDate(childId, LocalDate.now(europeHelsinki))
                getCareTypes(placementBasics.placementType).forEach { careType ->
                    tx.insertAbsence(user, childId, LocalDate.now(), careType, body.absenceType)
                }
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }
    data class AbsenceRangeRequest(
        val absenceType: AbsenceType,
        val startDate: LocalDate,
        val endDate: LocalDate
    )
    @PostMapping("/units/{unitId}/children/{childId}/absence-range")
    fun postAbsenceRange(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable unitId: DaycareId,
        @PathVariable childId: UUID,
        @RequestBody body: AbsenceRangeRequest
    ): ResponseEntity<AttendanceResponse> {
        Audit.ChildAttendancesAbsenceRangeCreate.log(targetId = childId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(*authorizedRoles)

        return db.transaction { tx ->
            val typeOnDates = tx.fetchChildPlacementTypeDates(childId, unitId, body.startDate, body.endDate)

            try {
                for ((date, placementType) in typeOnDates) {
                    tx.deleteAbsencesByDate(childId, date)
                    getCareTypes(placementType).forEach { careType ->
                        tx.insertAbsence(user, childId, date, careType, body.absenceType)
                    }
                }
            } catch (e: Exception) {
                throw mapPSQLException(e)
            }

            tx.getAttendancesResponse(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    @DeleteMapping("/units/{unitId}/children/{childId}/absence-range")
    fun deleteAbsenceRange(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate,
        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate
    ): ResponseEntity<Unit> {
        Audit.AbsenceDeleteRange.log(targetId = childId)
        acl.getRolesForChild(user, childId).requireOneOfRoles(UserRole.MOBILE)
        return db.transaction { tx ->
            tx.deleteAbsencesByFiniteDateRange(childId, FiniteDateRange(from, to))
        }.let { ResponseEntity.ok(it) }
    }
}

data class ChildPlacementBasics(
    val placementType: PlacementType,
    val dateOfBirth: LocalDate
)
private fun Database.Read.fetchChildPlacementBasics(childId: UUID, unitId: DaycareId): ChildPlacementBasics {
    // language=sql
    val sql =
        """
        SELECT p.type AS placement_type, c.date_of_birth
        FROM person c 
        JOIN placement p 
            ON p.child_id = c.id AND daterange(p.start_date, p.end_date, '[]') @> :date
        LEFT JOIN backup_care bc
            ON bc.child_id = c.id AND daterange(bc.start_date, bc.end_date, '[]') @> :date
        WHERE c.id = :childId AND (p.unit_id = :unitId OR bc.unit_id = :unitId)
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("date", dateNow())
        .mapTo<ChildPlacementBasics>()
        .list()
        .firstOrNull() ?: throw BadRequest("Child $childId has no placement in unit $unitId on the given day")
}
data class PlacementTypeDate(
    val date: LocalDate,
    val placementType: PlacementType
)
private fun Database.Read.fetchChildPlacementTypeDates(childId: UUID, unitId: DaycareId, startDate: LocalDate, endDate: LocalDate): List<PlacementTypeDate> {

    // language=sql
    val sql = """
        SELECT DISTINCT d::date AS date, p.type AS placement_type
        FROM generate_series(:startDate, :endDate, '1 day') d
        JOIN placement p ON daterange(p.start_date, p.end_date, '[]') @> d::date
        LEFT JOIN backup_care bc
            ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> d::date
        WHERE p.child_id = :childId AND (p.unit_id = :unitId OR bc.unit_id = :unitId)
    """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("unitId", unitId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<PlacementTypeDate>()
        .list()
}

private fun Database.Read.getAttendancesResponse(unitId: DaycareId): AttendanceResponse {
    val unitInfo = fetchUnitInfo(unitId)
    val childrenBasics = fetchChildrenBasics(unitId)
    val childrenAttendances = fetchChildrenAttendances(unitId)
    val childrenAbsences = fetchChildrenAbsences(unitId)
    val daycareDailyNotesForChildrenPlacedInUnit = getDaycareDailyNotesForChildrenPlacedInUnit(unitId)

    val children = childrenBasics.map { child ->
        val attendance = childrenAttendances.firstOrNull { it.childId == child.id }
        val absences = childrenAbsences.filter { it.childId == child.id }
        val placementBasics = ChildPlacementBasics(child.placementType, child.dateOfBirth)
        val status = getChildAttendanceStatus(placementBasics, attendance, absences)
        val daycareDailyNote = daycareDailyNotesForChildrenPlacedInUnit.firstOrNull { it.childId == child.id }

        Child(
            id = child.id,
            firstName = child.firstName,
            lastName = child.lastName,
            preferredName = child.preferredName,
            placementType = child.placementType,
            groupId = child.groupId,
            backup = child.backup,
            status = status,
            attendance = attendance,
            absences = absences,
            dailyServiceTimes = child.dailyServiceTimes,
            dailyNote = daycareDailyNote,
            imageUrl = child.imageUrl
        )
    }

    return AttendanceResponse(unitInfo, children)
}

private fun getChildAttendanceStatus(placementBasics: ChildPlacementBasics, attendance: ChildAttendance?, absences: List<ChildAbsence>): AttendanceStatus {
    if (attendance != null) {
        return if (attendance.departed == null) AttendanceStatus.PRESENT else AttendanceStatus.DEPARTED
    }

    if (isFullyAbsent(placementBasics, absences)) {
        return AttendanceStatus.ABSENT
    }

    return AttendanceStatus.COMING
}

private fun isFullyAbsent(placementBasics: ChildPlacementBasics, absences: List<ChildAbsence>): Boolean {
    return getCareTypes(placementBasics.placementType).all { type -> absences.map { it.careType }.contains(type) }
}

private fun getCareTypes(placementType: PlacementType): List<CareType> =
    when (placementType) {
        PlacementType.SCHOOL_SHIFT_CARE ->
            listOf(CareType.SCHOOL_SHIFT_CARE)
        PlacementType.PRESCHOOL, PlacementType.PREPARATORY ->
            listOf(CareType.PRESCHOOL)
        PlacementType.PRESCHOOL_DAYCARE, PlacementType.PREPARATORY_DAYCARE ->
            listOf(CareType.PRESCHOOL, CareType.PRESCHOOL_DAYCARE)
        PlacementType.DAYCARE_FIVE_YEAR_OLDS,
        PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
            listOf(CareType.DAYCARE_5YO_FREE, CareType.DAYCARE)
        PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME,
        PlacementType.TEMPORARY_DAYCARE, PlacementType.TEMPORARY_DAYCARE_PART_DAY ->
            listOf(CareType.DAYCARE)
        PlacementType.CLUB ->
            listOf(CareType.CLUB)
    }

private fun getPartialAbsenceCareTypes(placementBasics: ChildPlacementBasics, arrived: LocalTime, departed: LocalTime): Set<CareType> {
    return listOfNotNull(
        CareType.PRESCHOOL.takeIf {
            wasAbsentFromPreschool(placementBasics.placementType, arrived, departed)
        },
        CareType.PRESCHOOL_DAYCARE.takeIf {
            wasAbsentFromPreschoolDaycare(placementBasics.placementType, arrived, departed)
        },
        CareType.DAYCARE.takeIf {
            wasAbsentFrom5yoPaidDaycare(placementBasics.placementType, arrived, departed)
        }
    ).toSet()
}

fun wasAbsentFromPreschool(placementType: PlacementType, arrived: LocalTime, departed: LocalTime): Boolean {
    if (placementType in listOf(PlacementType.PRESCHOOL, PlacementType.PRESCHOOL_DAYCARE)) {
        val presentAtPreschoolDuration = Duration.between(
            maxOf(arrived, preschoolStart),
            minOf(departed, preschoolEnd)
        )

        return presentAtPreschoolDuration < preschoolMinimumDuration
    }

    if (placementType in listOf(PlacementType.PREPARATORY, PlacementType.PREPARATORY_DAYCARE)) {
        val presentAtPreparatoryDuration = Duration.between(
            maxOf(arrived, preschoolStart),
            minOf(departed, preparatoryEnd)
        )

        return presentAtPreparatoryDuration < preparatoryMinimumDuration
    }

    return false
}

fun wasAbsentFromPreschoolDaycare(placementType: PlacementType, arrived: LocalTime, departed: LocalTime): Boolean {
    if (placementType == PlacementType.PRESCHOOL_DAYCARE) {
        val presentBeforePreschool = Duration.between(arrived, preschoolStart) > connectedDaycareBuffer
        val presentAfterPreschool = Duration.between(preschoolEnd, departed) > connectedDaycareBuffer

        return !presentBeforePreschool && !presentAfterPreschool
    }

    if (placementType == PlacementType.PREPARATORY_DAYCARE) {
        val presentBeforePreparatory = Duration.between(arrived, preparatoryStart) > connectedDaycareBuffer
        val presentAfterPreparatory = Duration.between(preparatoryEnd, departed) > connectedDaycareBuffer

        return !presentBeforePreparatory && !presentAfterPreparatory
    }

    return false
}

private val fiveYearOldPlacementTypes =
    listOf(PlacementType.DAYCARE_FIVE_YEAR_OLDS, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS)

fun wasAbsentFrom5yoPaidDaycare(placementType: PlacementType, arrived: LocalTime, departed: LocalTime): Boolean {
    if (fiveYearOldPlacementTypes.contains(placementType)) {
        return Duration.between(arrived, departed) <= fiveYearOldFreeLimit
    }

    return false
}
