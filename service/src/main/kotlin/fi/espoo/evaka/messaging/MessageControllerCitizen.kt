package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

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
}
