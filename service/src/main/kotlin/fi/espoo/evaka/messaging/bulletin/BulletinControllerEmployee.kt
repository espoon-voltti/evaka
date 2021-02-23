package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/employee/bulletins")
class BulletinControllerEmployee(
    private val acl: AccessControlList
) {

    @PostMapping
    fun createBulletin(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinDraftCreate.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        db.transaction { tx ->
            tx.initBulletin(
                user = user
            )
        }

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/sent")
    fun getSentBulletins(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(required = true) unitId: UUID
    ): ResponseEntity<List<Bulletin>> {
        Audit.MessagingBulletinRead.log(unitId)
        acl.getRolesForUnit(user, unitId).requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        return db.transaction { tx ->
            tx.getSentBulletinsByUnit(unitId)
        }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/draft")
    fun getDraftBulletins(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<List<Bulletin>> {
        Audit.MessagingBulletinDraftRead.log()
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        return db.transaction { tx ->
            tx.getOwnBulletinDrafts(user)
        }.let { ResponseEntity.ok(it) }
    }

    data class BulletinUpdate(
        val groupId: UUID?,
        val title: String,
        val content: String
    )
    @PutMapping("/{id}")
    fun updateBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID,
        @RequestBody body: BulletinUpdate
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinDraftUpdate.log(id)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        db.transaction { tx ->
            tx.updateDraftBulletin(
                user = user,
                id = id,
                groupId = body.groupId,
                title = body.title,
                content = body.content
            )
        }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun deleteDraftBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinDraftDelete.log(id)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        db.transaction { tx ->
            tx.deleteDraftBulletin(user, id)
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/send")
    fun sendBulletin(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<Unit> {
        Audit.MessagingBulletinSend.log(id)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.UNIT_SUPERVISOR, UserRole.STAFF)

        db.transaction { tx ->
            tx.sendBulletin(user, id)
        }

        return ResponseEntity.noContent().build()
    }
}
