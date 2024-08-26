// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.ConstList
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

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
