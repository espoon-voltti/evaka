package fi.espoo.evaka.messaging.bulletin

import java.time.Instant
import java.util.UUID

data class Bulletin(
    val id: UUID,
    val title: String,
    val content: String,
    val createdByEmployee: UUID,
    val groupId: UUID?,
    val sentAt: Instant?
)

data class ReceivedBulletin(
    val id: UUID,
    val sentAt: Instant,
    val sender: String,
    val title: String,
    val content: String,
    val isRead: Boolean
)
