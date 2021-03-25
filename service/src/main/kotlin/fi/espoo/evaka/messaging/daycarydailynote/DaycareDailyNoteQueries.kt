package fi.espoo.evaka.messaging.daycarydailynote

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.util.UUID

fun Database.Read.getDaycareDailyNote(noteId: UUID): DaycareDailyNote? {
    return createQuery("SELECT * FROM daycare_daily_note WHERE id = :id")
        .bind("id", noteId)
        .mapTo<DaycareDailyNote>()
        .first()
}

fun Database.Read.getChildDaycareDailyNotes(childId: UUID): List<DaycareDailyNote> {
    return createQuery("SELECT * FROM daycare_daily_note WHERE child_id = :id")
        .bind("id", childId)
        .mapTo<DaycareDailyNote>()
        .list()
}

fun Database.Read.getDaycareDailyNotesForChildrenPlacedInUnit(unitId: UUID): List<DaycareDailyNote> {
    return createQuery(
        """
SELECT note.* 
FROM daycare_daily_note note
    LEFT JOIN placement p on p.child_id = note.child_id
    LEFT JOIN daycare d ON d.id = p.unit_id
WHERE 
    note.child_id IS NOT NULL
    AND d.id = :id
        """.trimIndent()
    )
        .bind("id", unitId)
        .mapTo<DaycareDailyNote>()
        .list()
}

fun Database.Read.getDaycareDailyNotesForDaycareGroups(unitId: UUID): List<DaycareDailyNote> {
    return createQuery(
        """
SELECT note.* 
FROM daycare_daily_note note
    LEFT JOIN daycare_group ON daycare_group.id = note.group_id
    LEFT JOIN daycare d ON d.id = daycare_group.daycare_id
WHERE 
    d.id = :id
        """.trimIndent()
    )
        .bind("id", unitId)
        .mapTo<DaycareDailyNote>()
        .list()
}

fun Database.Read.getGroupDaycareDailyNotes(groupId: UUID): List<DaycareDailyNote> {
    return createQuery("SELECT * FROM daycare_daily_note WHERE group_id = :id")
        .bind("id", groupId)
        .mapTo<DaycareDailyNote>()
        .list()
}

fun Database.Read.getDaycareDailyNotesForChildrenInGroup(groupId: UUID): List<DaycareDailyNote> {
    return createQuery(
        """
SELECT * FROM daycare_daily_note 
WHERE child_id IN (        
    SELECT p.child_id
    FROM placement p LEFT JOIN daycare_group_placement dgp ON p.id = dgp.daycare_placement_id
    WHERE dgp.daycare_group_id = :groupId)        
        """.trimIndent()
    )
        .bind("groupId", groupId)
        .mapTo<DaycareDailyNote>()
        .list()
}

fun Database.Transaction.createDaycareDailyNote(note: DaycareDailyNote): UUID {
    return createUpdate(
        """
INSERT INTO daycare_daily_note (id, child_id, group_id, date, note, feeding_note, sleeping_note, sleeping_hours, reminders, reminder_note, modified_by, modified_at)
VALUES(:id, :childId, :groupId, :date, :note, :feedingNote, :sleepingNote, :sleepingHours, :reminders::daycare_daily_note_reminder[], :reminderNote, :modifiedBy, COALESCE(:modifiedAt, now()))
RETURNING id
        """.trimIndent()
    )
        .bind("id", note.id ?: UUID.randomUUID())
        .bind("childId", note.childId)
        .bind("groupId", note.groupId)
        .bind("date", note.date)
        .bind("note", note.note)
        .bind("feedingNote", note.feedingNote)
        .bind("sleepingNote", note.sleepingNote)
        .bind("sleepingHours", note.sleepingHours)
        .bind("reminders", note.reminders.map { it.name }.toTypedArray())
        .bind("reminderNote", note.reminderNote)
        .bind("modifiedBy", note.modifiedBy)
        .bind("modifiedAt", note.modifiedAt)
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .first()
}

fun Database.Transaction.updateDaycareDailyNote(note: DaycareDailyNote): DaycareDailyNote {
    return createUpdate(
        """
UPDATE daycare_daily_note SET 
    child_id = :childId, 
    group_id = :groupId, 
    date = :date, 
    note = :note, 
    feeding_note = :feedingNote, 
    sleeping_note = :sleepingNote,
    sleeping_hours = :sleepingHours,
    reminders = :reminders::daycare_daily_note_reminder[],
    reminder_note = :reminderNote,
    modified_by = :modifiedBy,
    modified_at = now()
WHERE id = :id
RETURNING *
        """.trimIndent()
    )
        .bind("childId", note.childId)
        .bind("groupId", note.groupId)
        .bind("date", note.date)
        .bind("note", note.note)
        .bind("feedingNote", note.feedingNote)
        .bind("sleepingNote", note.sleepingNote)
        .bind("sleepingHours", note.sleepingHours)
        .bind("reminders", note.reminders.map { it.name }.toTypedArray())
        .bind("reminderNote", note.reminderNote)
        .bind("modifiedBy", note.modifiedBy)
        .bind("id", note.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<DaycareDailyNote>()
        .first()
}

fun Database.Transaction.deleteDaycareDailyNote(noteId: UUID) {
    createUpdate("DELETE from daycare_daily_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}

fun Database.Transaction.deleteChildDaycareDailyNotes(childId: UUID) {
    createUpdate("DELETE from daycare_daily_note WHERE childId = :id")
        .bind("id", childId)
        .execute()
}

fun Database.Transaction.deleteExpiredDaycareDailyNotes(now: Instant) {
    createUpdate(
        """
WITH expired_child_notes AS (
    SELECT id, child_id
    FROM daycare_daily_note
    WHERE child_id IS NOT null
        AND modified_at < :now::timestamp - INTERVAL '12 hours'
)
DELETE FROM daycare_daily_note
WHERE id IN (
  SELECT note.id
  FROM expired_child_notes note
    LEFT JOIN placement p on p.child_id = note.child_id
    LEFT JOIN daycare d ON d.id = p.unit_id
  WHERE
    d.round_the_clock IS NOT TRUE);    
        """.trimIndent()
    )
        .bind("now", now)
        .execute()
}
