// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.JdbiException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import java.util.UUID

@RestController
@RequestMapping("/child-attendances")
class ChildAttendanceController(private val acl: AccessControlList) {

    @PostMapping("/arrive")
    fun childArrives(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: ArrivalRequest
    ): ResponseEntity<ChildAttendance> {
        Audit.ChildAttendanceArrive.log(targetId = body.childId)
        acl.getRolesForChild(user, body.childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        try {
            return db.transaction {
                it.handle.createAttendance(
                    childId = body.childId,
                    arrived = body.time ?: OffsetDateTime.now()
                )
            }.let { ResponseEntity.ok(it) }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @PostMapping("/depart")
    fun childDeparts(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: DepartureRequest
    ): ResponseEntity<Unit> {
        Audit.ChildAttendanceDepart.log(targetId = body.childId)
        acl.getRolesForChild(user, body.childId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        try {
            db.transaction {
                it.handle.updateCurrentAttendanceEnd(
                    childId = body.childId,
                    departed = body.time ?: OffsetDateTime.now()
                )
            }
            return ResponseEntity.status(HttpStatus.OK).build()
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    @GetMapping("/current")
    fun getDaycareAttendances(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam daycareId: UUID
    ): ResponseEntity<List<ChildInGroup>> {
        Audit.ChildAttendanceReadUnit.log(targetId = daycareId)
        acl.getRolesForUnit(user, daycareId)
            .requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        return db.read { it.handle.getDaycareAttendances(daycareId) }
            .let { ResponseEntity.ok(it) }
    }

    @DeleteMapping("/{attendanceId}")
    fun deleteAttendance(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "attendanceId") attendanceId: UUID
    ): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteAttendance(attendanceId) }
        return ResponseEntity.noContent().build()
    }
}

data class ArrivalRequest(
    val childId: UUID,
    val time: OffsetDateTime? = null
)

data class DepartureRequest(
    val childId: UUID,
    val time: OffsetDateTime? = null
)

enum class AttendanceStatus {
    COMING, PRESENT, DEPARTED, ABSENT
}

data class ChildInGroup(
    val childId: UUID,
    val firstName: String,
    val lastName: String,
    val status: AttendanceStatus,
    val daycareGroupId: UUID,
    val arrived: OffsetDateTime?,
    val departed: OffsetDateTime?,
    val childAttendanceId: UUID?
)
