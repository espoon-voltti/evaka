package fi.espoo.evaka.messaging

import java.time.LocalDate
import java.util.Date
import java.util.UUID

data class DaycareDailyNote(
    val id: UUID?,
    val childId: UUID?,
    val groupId: UUID?,
    val date: LocalDate?,
    val note: String?,
    val feedingNote: DaycareDailyNoteLevelInfo?,
    val sleepingNote: DaycareDailyNoteLevelInfo?,
    val reminders: List<DaycareDailyNoteReminder>,
    val reminderNote: String?,
    val modifiedAt: Date? = null,
    val modifiedBy: String? = null
)

enum class DaycareDailyNoteLevelInfo {
    GOOD, MEDIUM, NONE
}

enum class DaycareDailyNoteReminder {
    DIAPERS, CLOTHES, LAUNDRY
}
