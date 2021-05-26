package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

data class Message(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val readAt: HelsinkiDateTime? = null
)

data class MessageThread(
    val id: UUID,
    val type: MessageType,
    val title: String,
    @Json
    val messages: List<Message>,
)

data class SentMessage(
    val id: UUID,
    val content: String,
    val sentAt: HelsinkiDateTime,
    val threadId: UUID,
    val threadTitle: String,
    val type: MessageType,
    @Json
    val recipients: Set<MessageAccount>,
)

enum class MessageType {
    MESSAGE,
    BULLETIN
}

data class MessageReceiversResponse(
    val groupId: UUID,
    val groupName: String,
    val receivers: List<MessageReceiver>
)

data class MessageReceiver(
    val childId: UUID,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val receiverPersons: List<MessageReceiverPerson>
)

data class MessageAccount(
    val id: UUID,
    val name: String,
)

data class Group(
    @PropagateNull
    val id: UUID,
    val name: String,
    val unitId: UUID,
    val unitName: String,
)

enum class AccountType {
    PERSONAL,
    GROUP
}

data class AuthorizedMessageAccount(
    val id: UUID,
    val name: String,
    @Nested("group_")
    val daycareGroup: Group?,
    val type: AccountType,
    val unreadCount: Int
)

data class MessageReceiverPerson(
    val accountId: UUID,
    val receiverFirstName: String,
    val receiverLastName: String
)
