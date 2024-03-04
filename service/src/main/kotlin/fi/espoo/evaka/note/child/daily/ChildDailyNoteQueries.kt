// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate

private fun Database.Read.getChildDailyNotes(predicate: Predicate = Predicate.alwaysTrue()) =
    createQuery {
        sql(
            """
SELECT 
    id, child_id, modified_at,
    note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note
FROM child_daily_note cdn
WHERE ${predicate(predicate.forTable("cdn"))}
"""
        )
    }

fun Database.Read.getChildDailyNoteForChild(childId: ChildId): ChildDailyNote? =
    getChildDailyNotes(Predicate { where("$it.child_id = ${bind(childId)}") }).exactlyOneOrNull()

fun Database.Read.getChildDailyNotesForChildren(
    children: Collection<ChildId>,
): List<ChildDailyNote> =
    getChildDailyNotes(Predicate { where("$it.child_id = ANY(${bind(children)})") }).toList()

fun Database.Read.getChildDailyNotesForGroup(
    groupId: GroupId,
    today: LocalDate
): List<ChildDailyNote> =
    getChildDailyNotes(
            Predicate {
                where(
                    """
$it.child_id IN (
    SELECT child_id
    FROM realized_placement_all(${bind(today)})
    WHERE group_id = ${bind(groupId)}
)
"""
                )
            }
        )
        .toList()

fun Database.Transaction.createChildDailyNote(
    childId: ChildId,
    note: ChildDailyNoteBody
): ChildDailyNoteId {
    @Suppress("DEPRECATION")
    return createUpdate(
            """
INSERT INTO child_daily_note (child_id, note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note)
VALUES(:childId, :note, :feedingNote, :sleepingNote, :sleepingMinutes, :reminders::child_daily_note_reminder[], :reminderNote)
RETURNING id
        """
                .trimIndent()
        )
        .bindKotlin(note)
        .bind("childId", childId)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ChildDailyNoteId>()
}

fun Database.Transaction.updateChildDailyNote(
    clock: EvakaClock,
    id: ChildDailyNoteId,
    note: ChildDailyNoteBody
): ChildDailyNote {
    @Suppress("DEPRECATION")
    return createUpdate(
            """
UPDATE child_daily_note SET
    note = :note, 
    feeding_note = :feedingNote, 
    sleeping_note = :sleepingNote,
    sleeping_minutes = :sleepingMinutes,
    reminders = :reminders::child_daily_note_reminder[],
    reminder_note = :reminderNote,
    modified_at = :now
WHERE id = :id
RETURNING *
        """
                .trimIndent()
        )
        .bind("id", id)
        .bind("now", clock.now())
        .bindKotlin(note)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ChildDailyNote>()
}

fun Database.Transaction.deleteChildDailyNote(noteId: ChildDailyNoteId) {
    @Suppress("DEPRECATION")
    createUpdate("DELETE from child_daily_note WHERE id = :noteId").bind("noteId", noteId).execute()
}

fun Database.Transaction.deleteExpiredNotes(now: HelsinkiDateTime) {
    @Suppress("DEPRECATION")
    createUpdate("DELETE FROM child_daily_note WHERE modified_at < :now - INTERVAL '14 hours'")
        .bind("now", now)
        .execute()

    @Suppress("DEPRECATION")
    createUpdate("DELETE FROM child_sticky_note WHERE expires < :thresholdDate")
        .bind("thresholdDate", now.toLocalDate())
        .execute()

    @Suppress("DEPRECATION")
    createUpdate("DELETE FROM group_note WHERE expires < :thresholdDate")
        .bind("thresholdDate", now.toLocalDate())
        .execute()
}
