package fi.espoo.evaka.messaging.bulletin

import java.time.LocalDateTime
import java.util.UUID

data class Bulletin(
    val id: UUID,
    val title: String,
    val content: String,
    val createdBy: UUID,
    val sentAt: LocalDateTime?
)

data class ReceivedBulletin(
    val id: UUID,
    val sentAt: LocalDateTime,
    val title: String,
    val content: String,
    val isRead: Boolean
)
