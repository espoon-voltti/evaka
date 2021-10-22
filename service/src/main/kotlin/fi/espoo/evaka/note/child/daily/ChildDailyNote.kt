// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID

data class ChildDailyNote(
    val id: ChildDailyNoteId,
    val childId: UUID,
    val date: LocalDate,
    val note: String,
    val feedingNote: ChildDailyNoteLevel?,
    val sleepingNote: ChildDailyNoteLevel?,
    val sleepingMinutes: Int?,
    val reminders: List<ChildDailyNoteReminder> = emptyList(),
    val reminderNote: String,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: String
)

data class ChildDailyNoteBody(
    val note: String,
    val feedingNote: ChildDailyNoteLevel?,
    val sleepingNote: ChildDailyNoteLevel?,
    val sleepingMinutes: Int?,
    val reminders: List<ChildDailyNoteReminder>,
    val reminderNote: String
)

@ConstList("childDailyNoteLevelValues")
enum class ChildDailyNoteLevel {
    GOOD, MEDIUM, NONE
}

@ConstList("childDailyNoteReminderValues")
enum class ChildDailyNoteReminder {
    DIAPERS, CLOTHES, LAUNDRY
}
