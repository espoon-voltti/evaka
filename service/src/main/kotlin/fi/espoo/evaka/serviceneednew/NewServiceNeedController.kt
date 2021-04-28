package fi.espoo.evaka.serviceneednew

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
class NewServiceNeedController(
    private val acl: AccessControlList
) {

    data class NewServiceNeedCreateRequest(
        val placementId: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val optionId: UUID,
        val shiftCare: Boolean
    )

    @PostMapping("/new-service-needs")
    fun postServiceNeed(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: NewServiceNeedCreateRequest
    ): ResponseEntity<Unit> {
        Audit.PlacementServiceNeedCreate.log(targetId = body.placementId)
        acl.getRolesForPlacement(user, body.placementId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { tx ->
            createNewServiceNeed(tx, body.placementId, body.startDate, body.endDate, body.optionId, body.shiftCare)
        }

        return ResponseEntity.noContent().build()
    }

    data class NewServiceNeedUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val optionId: UUID,
        val shiftCare: Boolean
    )

    @PutMapping("/new-service-needs/{id}")
    fun putServiceNeed(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: NewServiceNeedUpdateRequest
    ): ResponseEntity<Unit> {
        Audit.PlacementServiceNeedUpdate.log(targetId = id)
        acl.getRolesForNewServiceNeed(user, id).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { tx ->
            updateNewServiceNeed(tx, id, body.startDate, body.endDate, body.optionId, body.shiftCare)
        }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/new-service-needs/{id}")
    fun deleteServiceNeed(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.PlacementServiceNeedDelete.log(targetId = id)
        acl.getRolesForNewServiceNeed(user, id).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction { tx ->
            tx.deleteNewServiceNeed(id)
        }

        return ResponseEntity.noContent().build()
    }
}
