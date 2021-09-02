package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.AttachmentId

data class Attachment(
    val id: AttachmentId,
    val name: String,
    val contentType: String,
)

enum class AttachmentType {
    URGENCY,
    EXTENDED_CARE
}
