// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.sticky

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

fun Database.Read.getChildStickyNotesForChild(childId: ChildId): List<ChildStickyNote> = createQuery(
    """
    SELECT id, child_id, note, modified_at, expires
    FROM child_sticky_note
    WHERE child_id = :childId
    ORDER BY created DESC
    """.trimIndent()
)
    .bind("childId", childId)
    .mapTo<ChildStickyNote>()
    .list()

fun Database.Read.getChildStickyNotesForGroup(groupId: GroupId, today: LocalDate): List<ChildStickyNote> {
    return getChildStickyNotesForGroups(listOf(groupId), today)
}

fun Database.Read.getChildStickyNotesForUnit(unitId: DaycareId, today: LocalDate): List<ChildStickyNote> {
    return createQuery("SELECT id FROM daycare_group WHERE daycare_id = :unitId")
        .bind("unitId", unitId)
        .mapTo<GroupId>()
        .list()
        .let { groupIds -> getChildStickyNotesForGroups(groupIds, today) }
}

private fun Database.Read.getChildStickyNotesForGroups(groupIds: List<GroupId>, today: LocalDate): List<ChildStickyNote> = createQuery(
    """
    SELECT id, child_id, note, modified_at, expires
    FROM child_sticky_note csn
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
    .bind("today", today)
    .mapTo<ChildStickyNote>()
    .list()

fun Database.Transaction.createChildStickyNote(childId: ChildId, note: ChildStickyNoteBody): ChildStickyNoteId {
    return createUpdate(
        """
INSERT INTO child_sticky_note (child_id, note, expires)
VALUES (:childId, :note, :expires)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(note)
        .bind("childId", childId)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildStickyNoteId>()
        .one()
}

fun Database.Transaction.updateChildStickyNote(id: ChildStickyNoteId, note: ChildStickyNoteBody): ChildStickyNote {
    return createUpdate(
        """
UPDATE child_sticky_note SET
    note = :note,
    expires = :expires,
    modified_at = now()
WHERE id = :id
RETURNING *
        """.trimIndent()
    )
        .bind("id", id)
        .bindKotlin(note)
        .executeAndReturnGeneratedKeys()
        .mapTo<ChildStickyNote>()
        .one()
}

fun Database.Transaction.deleteChildStickyNote(noteId: ChildStickyNoteId) {
    createUpdate("DELETE FROM child_sticky_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}
