// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.sticky

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate

private fun Database.Read.getChildStickyNotes(predicate: Predicate = Predicate.alwaysTrue()) =
    createQuery {
            sql(
                """
SELECT id, child_id, note, modified_at, expires
FROM child_sticky_note csn
WHERE ${predicate(predicate.forTable("csn"))}
"""
            )
        }
        .mapTo<ChildStickyNote>()

fun Database.Read.getChildStickyNotesForChild(childId: ChildId): List<ChildStickyNote> =
    getChildStickyNotes(Predicate { where("$it.child_id = ${bind(childId)}") }).toList()

fun Database.Read.getChildStickyNotesForChildren(
    children: Collection<ChildId>
): List<ChildStickyNote> =
    getChildStickyNotes(Predicate { where("$it.child_id = ANY(${bind(children)})") }).toList()

fun Database.Read.getChildStickyNotesForGroup(
    groupId: GroupId,
    today: LocalDate,
): List<ChildStickyNote> =
    getChildStickyNotes(
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

fun Database.Transaction.createChildStickyNote(
    childId: ChildId,
    note: ChildStickyNoteBody,
): ChildStickyNoteId {
    return createUpdate {
            sql(
                """
INSERT INTO child_sticky_note (child_id, note, expires)
VALUES (${bind(childId)}, ${bind(note.note)}, ${bind(note.expires)})
RETURNING id
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ChildStickyNoteId>()
}

fun Database.Transaction.updateChildStickyNote(
    clock: EvakaClock,
    id: ChildStickyNoteId,
    note: ChildStickyNoteBody,
): ChildStickyNote {
    val now = clock.now()
    return createUpdate {
            sql(
                """
UPDATE child_sticky_note SET
    note = ${bind(note.note)},
    expires = ${bind(note.expires)},
    modified_at = ${bind(now)}
WHERE id = ${bind(id)}
RETURNING *
"""
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ChildStickyNote>()
}

fun Database.Transaction.deleteChildStickyNote(noteId: ChildStickyNoteId) {
    createUpdate { sql("DELETE FROM child_sticky_note WHERE id = ${bind(noteId)}") }.execute()
}
