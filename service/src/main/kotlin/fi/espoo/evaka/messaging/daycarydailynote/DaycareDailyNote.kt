package fi.espoo.evaka.messaging.daycarydailynote

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DaycareDailyNote(
    val id: UUID,
    val childId: UUID?,
    val groupId: UUID?,
    val date: LocalDate,
    val note: String?,
    val feedingNote: DaycareDailyNoteLevelInfo?,
    val sleepingNote: DaycareDailyNoteLevelInfo?,
    val sleepingHours: Double?,
    val reminders: List<DaycareDailyNoteReminder> = emptyList(),
    val reminderNote: String?,
    val modifiedAt: Instant? = null,
    val modifiedBy: String? = "evaka"
)

enum class DaycareDailyNoteLevelInfo {
    GOOD, MEDIUM, NONE
}

enum class DaycareDailyNoteReminder {
    DIAPERS, CLOTHES, LAUNDRY
}
