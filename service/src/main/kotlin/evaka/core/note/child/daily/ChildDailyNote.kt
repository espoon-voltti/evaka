// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.note.child.daily

import evaka.core.ConstList
import evaka.core.shared.ChildDailyNoteId
import evaka.core.shared.ChildId
import evaka.core.shared.db.DatabaseEnum
import evaka.core.shared.domain.HelsinkiDateTime

data class ChildDailyNote(
    val id: ChildDailyNoteId,
    val childId: ChildId,
    val note: String,
    val feedingNote: ChildDailyNoteLevel?,
    val sleepingNote: ChildDailyNoteLevel?,
    val sleepingMinutes: Int?,
    val reminders: List<ChildDailyNoteReminder> = emptyList(),
    val reminderNote: String,
    val modifiedAt: HelsinkiDateTime,
)

data class ChildDailyNoteBody(
    val note: String,
    val feedingNote: ChildDailyNoteLevel?,
    val sleepingNote: ChildDailyNoteLevel?,
    val sleepingMinutes: Int?,
    val reminders: List<ChildDailyNoteReminder>,
    val reminderNote: String,
)

@ConstList("childDailyNoteLevelValues")
enum class ChildDailyNoteLevel : DatabaseEnum {
    GOOD,
    MEDIUM,
    NONE;

    override val sqlType: String = "child_daily_note_level"
}

@ConstList("childDailyNoteReminderValues")
enum class ChildDailyNoteReminder : DatabaseEnum {
    DIAPERS,
    CLOTHES,
    LAUNDRY;

    override val sqlType: String = "child_daily_note_reminder"
}
