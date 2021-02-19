package fi.espoo.evaka.messaging

import java.util.UUID

enum class MessageType {
    MESSAGE,
    RELEASE
}

data class ReceivedMessage(
    val messageId: UUID,
    val type: MessageType,
    val title: String,
    val content: String,
    val isRead: Boolean,
    val instances: List<ReceivedMessageInstance>
)

data class ReceivedMessageInstance(
    val instanceId: UUID,
    val unitId: UUID,
    val unitName: String,
    val groupId: UUID,
    val groupName: String,
    val childId: UUID,
    val childName: String
)

data class MessageRecipient(
    val unitId: UUID,
    val groupId: UUID,
    val childId: UUID,
    val guardianId: UUID
)
