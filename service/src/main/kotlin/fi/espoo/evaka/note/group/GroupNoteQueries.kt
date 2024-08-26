// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock

fun Database.Read.getGroupNotesForGroup(groupId: GroupId): List<GroupNote> {
    return getGroupNotesForGroups(listOf(groupId))
}

private fun Database.Read.getGroupNotesForGroups(groupIds: List<GroupId>): List<GroupNote> =
    createQuery {
            sql(
                """
SELECT gn.id, gn.group_id, gn.note, gn.modified_at, gn.expires
FROM group_note gn
WHERE group_id = ANY(${bind(groupIds)})
"""
            )
        }
        .toList<GroupNote>()

fun Database.Transaction.createGroupNote(groupId: GroupId, note: GroupNoteBody): GroupNoteId {
    return createUpdate {
            sql(
                """
INSERT INTO group_note (group_id, note, expires)
VALUES (${bind(groupId)}, ${bind(note.note)}, ${bind(note.expires)})
RETURNING id
        """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<GroupNoteId>()
}

fun Database.Transaction.updateGroupNote(
    clock: EvakaClock,
    id: GroupNoteId,
    note: GroupNoteBody,
): GroupNote {
    val now = clock.now()
    return createUpdate {
            sql(
                """
UPDATE group_note SET
    note = ${bind(note.note)},
    expires = ${bind(note.expires)},
    modified_at = ${bind(now)}
WHERE id = ${bind(id)}
RETURNING *
        """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<GroupNote>()
}

fun Database.Transaction.deleteGroupNote(noteId: GroupNoteId) {
    createUpdate { sql("DELETE FROM group_note WHERE id = ${bind(noteId)}") }.execute()
}
