package fi.espoo.evaka.dailyservicetimes

import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DailyServiceTimesController(
    private val acl: AccessControlList
) {

    data class DailyServiceTimesResponse(
        val dailyServiceTimes: DailyServiceTimes?
    )

    @GetMapping("/children/{childId}/daily-service-times")
    fun getDailyServiceTimes(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<DailyServiceTimesResponse> {
        // todo audit
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.SPECIAL_EDUCATION_TEACHER, UserRole.STAFF
        )

        return db.read { it.getChildDailyServiceTimes(childId) }.let {
            ResponseEntity.ok(
                DailyServiceTimesResponse(it)
            )
        }
    }

    @PutMapping("/children/{childId}/daily-service-times")
    fun putDailyServiceTimes(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID,
        @RequestBody body: DailyServiceTimes
    ): ResponseEntity<Unit> {
        // todo audit
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR
        )

        db.transaction { it.upsertChildDailyServiceTimes(childId, body) }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/children/{childId}/daily-service-times")
    fun deleteDailyServiceTimes(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable childId: UUID
    ): ResponseEntity<Unit> {
        // todo audit
        acl.getRolesForChild(user, childId).requireOneOfRoles(
            UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR
        )

        db.transaction { it.deleteChildDailyServiceTimes(childId) }

        return ResponseEntity.noContent().build()
    }
}
