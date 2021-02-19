package fi.espoo.evaka.messaging

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController("/employee/messages")
class MessageControllerEmployee(
    private val acl: AccessControlList
) {

    data class MessageSendRequestBody(
        val unitId: UUID,
        val groupIds: Set<UUID>,
        val title: String,
        val content: String
    )

    @PostMapping
    fun sendMessage(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: MessageSendRequestBody
    ): ResponseEntity<Unit> {
        Audit.MessagesSend.log(body.groupIds.joinToString(","))
        acl.getRolesForUnit(user, body.unitId).requireOneOfRoles(UserRole.UNIT_SUPERVISOR)

        db.transaction { tx ->
            tx.createMessage(
                user = user,
                title = body.title,
                content = body.content,
                recipients = body.groupIds.flatMap { groupId ->
                    tx.getMessageRecipients(
                        unitId = body.unitId,
                        groupId = groupId
                    )
                }.toSet()
            )
        }

        return ResponseEntity.noContent().build()
    }
}
