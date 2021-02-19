package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/citizen/messages")
class MessageControllerCitizen {

    @GetMapping
    fun getReceivedMessages(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<List<ReceivedMessage>> {
        Audit.MessagesReadReceived.log(user.id)
        user.requireOneOfRoles(UserRole.END_USER)

        return db.read {
            it.getReceivedMessagesByGuardian(user)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/{messageId}/read")
    fun markMessageRead(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable messageId: UUID
    ): ResponseEntity<Unit> {
        Audit.MessagesMarkRead.log(messageId)
        user.requireOneOfRoles(UserRole.END_USER)

        db.transaction { it.markMessageRead(user, messageId) }

        return ResponseEntity.noContent().build()
    }
}
