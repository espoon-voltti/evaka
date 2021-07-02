// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.daycarydailynote

import fi.espoo.evaka.shared.GroupId
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class DaycareDailyNote(
    val id: UUID?,
    val childId: UUID?,
    val groupId: GroupId?,
    val date: LocalDate,
    val note: String?,
    val feedingNote: DaycareDailyNoteLevelInfo?,
    val sleepingNote: DaycareDailyNoteLevelInfo?,
    val sleepingMinutes: Int?,
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
