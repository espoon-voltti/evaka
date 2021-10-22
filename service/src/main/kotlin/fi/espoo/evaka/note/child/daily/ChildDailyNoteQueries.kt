// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.daily

import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getLatestChildDailyNote(childId: UUID): ChildDailyNote? {
    return createQuery(
        """
        SELECT 
            id, child_id, date, modified_at, modified_by, 
            note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note
        FROM child_daily_note 
        WHERE child_id = :childId
        ORDER BY date DESC 
        LIMIT 1
        """.trimIndent()
    )
        .bind("childId", childId)
        .mapTo<ChildDailyNote>()
        .firstOrNull()
}

fun Database.Read.getLatestChildDailyNotesInGroup(groupId: GroupId): List<ChildDailyNote> {
    return getLatestChildDailyNotesInGroups(listOf(groupId))
}

fun Database.Read.getLatestChildDailyNotesInUnit(unitId: DaycareId): List<ChildDailyNote> {
    return createQuery("SELECT id FROM daycare_group WHERE daycare_id = :unitId")
        .bind("unitId", unitId)
        .mapTo<GroupId>()
        .list()
        .let { groupIds -> getLatestChildDailyNotesInGroups(groupIds) }
}

private fun Database.Read.getLatestChildDailyNotesInGroups(groupIds: List<GroupId>): List<ChildDailyNote> {
    return createQuery(
        """
        WITH ranked AS (
            SELECT 
                id, child_id, date, modified_at, modified_by, 
                note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note,
                rank() OVER (PARTITION BY child_id ORDER BY date DESC) AS rank
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
        )
        SELECT * FROM ranked WHERE rank = 1
        """.trimIndent()
    )
        .bind("groupIds", groupIds.toTypedArray())
        .bind("today", LocalDate.now())
        .mapTo<ChildDailyNote>()
        .list()
}

fun Database.Transaction.createChildDailyNote(user: AuthenticatedUser, childId: UUID, note: ChildDailyNoteBody): ChildDailyNoteId {
    return createUpdate(
        """
INSERT INTO child_daily_note (child_id, date, note, feeding_note, sleeping_note, sleeping_minutes, reminders, reminder_note, modified_by)
VALUES(:childId, :date, :note, :feedingNote, :sleepingNote, :sleepingMinutes, :reminders::child_daily_note_reminder[], :reminderNote, :modifiedBy)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(note)
        .bind("childId", childId)
        .bind("date", LocalDate.now())
        .bind("modifiedBy", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildDailyNoteId>()
        .one()
}

fun Database.Transaction.updateChildDailyNote(user: AuthenticatedUser, id: ChildDailyNoteId, note: ChildDailyNoteBody): ChildDailyNote {
    return createUpdate(
        """
UPDATE child_daily_note SET
    date = :date, 
    note = :note, 
    feeding_note = :feedingNote, 
    sleeping_note = :sleepingNote,
    sleeping_minutes = :sleepingMinutes,
    reminders = :reminders::child_daily_note_reminder[],
    reminder_note = :reminderNote,
    modified_by = :modifiedBy,
    modified_at = now()
WHERE id = :id
RETURNING *
        """.trimIndent()
    )
        .bind("id", id)
        .bindKotlin(note)
        .bind("date", LocalDate.now())
        .bind("modifiedBy", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildDailyNote>()
        .one()
}

fun Database.Transaction.deleteChildDailyNote(noteId: ChildDailyNoteId) {
    createUpdate("DELETE from child_daily_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}

fun Database.Transaction.deleteExpiredChildDailyNotes(now: HelsinkiDateTime) {
    createUpdate("DELETE FROM child_daily_note WHERE modified_at < :now - INTERVAL '16 hours'")
        .bind("now", now)
        .execute()
}
