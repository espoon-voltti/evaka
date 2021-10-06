// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.daycarydailynote

import fi.espoo.evaka.shared.DaycareDailyNoteId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

data class DaycareDailyNote(
    val id: DaycareDailyNoteId,
    val childId: UUID?,
    val groupId: GroupId?,
    val date: LocalDate,
    val note: String?,
    val feedingNote: DaycareDailyNoteLevelInfo?,
    val sleepingNote: DaycareDailyNoteLevelInfo?,
    val sleepingMinutes: Int?,
    val reminders: List<DaycareDailyNoteReminder> = emptyList(),
    val reminderNote: String?,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: String
)

data class DaycareDailyNoteBody(
    val childId: UUID?,
    val groupId: GroupId?,
    val date: LocalDate,
    val note: String?,
    val feedingNote: DaycareDailyNoteLevelInfo?,
    val sleepingNote: DaycareDailyNoteLevelInfo?,
    val sleepingMinutes: Int?,
    val reminders: List<DaycareDailyNoteReminder>,
    val reminderNote: String?,
)

enum class DaycareDailyNoteLevelInfo {
    GOOD, MEDIUM, NONE
}

enum class DaycareDailyNoteReminder {
    DIAPERS, CLOTHES, LAUNDRY
}
