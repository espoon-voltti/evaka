package fi.espoo.evaka.attendance

import java.time.OffsetDateTime
import java.util.UUID

data class ChildAttendance(
    val id: UUID,
    val childId: UUID,
    val arrived: OffsetDateTime,
    val departed: OffsetDateTime?
)
