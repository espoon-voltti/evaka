package fi.espoo.evaka.messaging.bulletin

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/citizen/bulletins")
class BulletinControllerCitizen {

    @GetMapping
    fun getReceivedBulletins(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<List<ReceivedBulletin>> {
        Audit.MessagingBulletinRead.log()
        user.requireOneOfRoles(UserRole.END_USER)

        return db.read {
            it.getReceivedBulletinsByGuardian(user)
        }.let { ResponseEntity.ok(it) }
    }

    @PutMapping("/{id}/read")
    fun markBulletinRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable id: UUID
    ): ResponseEntity<List<ReceivedBulletin>> {
        Audit.MessagingBulletinMarkRead.log(id)
        user.requireOneOfRoles(UserRole.END_USER)

        db.transaction {
            it.markBulletinRead(user, id)
        }

        return ResponseEntity.noContent().build()
    }
}
