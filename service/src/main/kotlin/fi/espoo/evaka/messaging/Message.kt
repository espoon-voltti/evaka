package fi.espoo.evaka.messaging

import java.time.LocalDateTime
import java.util.UUID

data class ReceivedMessage(
    val messageId: UUID,
    val sentAt: LocalDateTime,
    val title: String,
    val content: String
)
