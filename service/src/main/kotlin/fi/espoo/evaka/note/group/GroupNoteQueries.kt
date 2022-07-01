// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.getGroupNotesForGroup(groupId: GroupId): List<GroupNote> {
    return getGroupNotesForGroups(listOf(groupId))
}

fun Database.Read.getGroupNotesForUnit(unitId: DaycareId): List<GroupNote> {
    return createQuery("SELECT id FROM daycare_group WHERE daycare_id = :unitId")
        .bind("unitId", unitId)
        .mapTo<GroupId>()
        .list()
        .let { groupIds -> getGroupNotesForGroups(groupIds) }
}

private fun Database.Read.getGroupNotesForGroups(groupIds: List<GroupId>): List<GroupNote> = createQuery(
    """
    SELECT gn.id, gn.group_id, gn.note, gn.modified_at, gn.expires
    FROM group_note gn
    WHERE group_id = ANY(:groupIds)
    """.trimIndent()
)
    .bind("groupIds", groupIds.toTypedArray())
    .mapTo<GroupNote>()
    .list()

fun Database.Transaction.createGroupNote(groupId: GroupId, note: GroupNoteBody): GroupNoteId {
    return createUpdate(
        """
INSERT INTO group_note (group_id, note, expires)
VALUES (:groupId, :note, :expires)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(note)
        .bind("groupId", groupId)
        .executeAndReturnGeneratedKeys()
        .mapTo<GroupNoteId>()
        .one()
}

fun Database.Transaction.updateGroupNote(id: GroupNoteId, note: GroupNoteBody): GroupNote {
    return createUpdate(
        """
UPDATE group_note SET
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
        .mapTo<GroupNote>()
        .one()
}

fun Database.Transaction.deleteGroupNote(noteId: GroupNoteId) {
    createUpdate("DELETE FROM group_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}
