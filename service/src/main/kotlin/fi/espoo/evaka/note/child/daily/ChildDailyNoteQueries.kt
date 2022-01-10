// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Read.getChildDailyNote(childId: ChildId): ChildDailyNote? {
    return createQuery(
        """
        SELECT 
            id, child_id, modified_at,
            note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note
        FROM child_daily_note 
        WHERE child_id = :childId
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<ChildDailyNote>()
        .firstOrNull()
}

fun Database.Read.getChildDailyNotesInGroup(groupId: GroupId): List<ChildDailyNote> {
    return getChildDailyNotesInGroups(listOf(groupId))
}

fun Database.Read.getChildDailyNotesInUnit(unitId: DaycareId): List<ChildDailyNote> {
    return createQuery("SELECT id FROM daycare_group WHERE daycare_id = :unitId")
        .bind("unitId", unitId)
        .mapTo<GroupId>()
        .list()
        .let { groupIds -> getChildDailyNotesInGroups(groupIds) }
}

private fun Database.Read.getChildDailyNotesInGroups(groupIds: List<GroupId>): List<ChildDailyNote> {
    return createQuery(
        """
        SELECT 
            id, child_id, modified_at,
            note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note
        FROM child_daily_note 
        WHERE child_id IN (
            SELECT pl.child_id
            FROM daycare_group_placement gpl
            JOIN placement pl ON pl.id = gpl.daycare_placement_id AND daterange(pl.start_date, pl.end_date, '[]') @> :today
            WHERE gpl.daycare_group_id = ANY(:groupIds) AND daterange(gpl.start_date, gpl.end_date, '[]') @> :today
            
            UNION 
            
            SELECT bc.child_id
            FROM backup_care bc
            WHERE bc.group_id = ANY(:groupIds) AND daterange(bc.start_date, bc.end_date, '[]') @> :today
        )
        """.trimIndent()
    )
        .bind("groupIds", groupIds.toTypedArray())
        .bind("today", LocalDate.now())
        .mapTo<ChildDailyNote>()
        .list()
}

fun Database.Transaction.createChildDailyNote(childId: ChildId, note: ChildDailyNoteBody): ChildDailyNoteId {
    return createUpdate(
        """
INSERT INTO child_daily_note (child_id, note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note)
VALUES(:childId, :note, :feedingNote, :sleepingNote, :sleepingMinutes, :reminders::child_daily_note_reminder[], :reminderNote)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(note)
        .bind("childId", childId)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildDailyNoteId>()
        .one()
}

fun Database.Transaction.updateChildDailyNote(id: ChildDailyNoteId, note: ChildDailyNoteBody): ChildDailyNote {
    return createUpdate(
        """
UPDATE child_daily_note SET
    note = :note, 
    feeding_note = :feedingNote, 
    sleeping_note = :sleepingNote,
    sleeping_minutes = :sleepingMinutes,
    reminders = :reminders::child_daily_note_reminder[],
    reminder_note = :reminderNote,
    modified_at = now()
WHERE id = :id
RETURNING *
        """.trimIndent()
    )
        .bind("id", id)
        .bindKotlin(note)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildDailyNote>()
        .one()
}

fun Database.Transaction.deleteChildDailyNote(noteId: ChildDailyNoteId) {
    createUpdate("DELETE from child_daily_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}

fun Database.Transaction.deleteExpiredNotes(now: HelsinkiDateTime) {
    createUpdate("DELETE FROM child_daily_note WHERE modified_at < :now - INTERVAL '14 hours'")
        .bind("now", now)
        .execute()

    createUpdate("DELETE FROM child_sticky_note WHERE expires < :thresholdDate")
        .bind("thresholdDate", now.toLocalDate())
        .execute()

    createUpdate("DELETE FROM group_note WHERE expires < :thresholdDate")
        .bind("thresholdDate", now.toLocalDate())
        .execute()
}
