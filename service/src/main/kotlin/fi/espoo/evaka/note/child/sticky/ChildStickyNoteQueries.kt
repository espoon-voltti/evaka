// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.child.sticky

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate

private fun Database.Read.getChildStickyNotes(
    predicate: Predicate<DatabaseTable.ChildStickyNote> = Predicate.alwaysTrue()
) =
    createQuery<DatabaseTable.ChildStickyNote> {
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
    today: LocalDate
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
    note: ChildStickyNoteBody
): ChildStickyNoteId {
    @Suppress("DEPRECATION")
    return createUpdate(
            """
INSERT INTO child_sticky_note (child_id, note, expires)
VALUES (:childId, :note, :expires)
RETURNING id
        """
                .trimIndent()
        )
        .bindKotlin(note)
        .bind("childId", childId)
        .executeAndReturnGeneratedKeys()
        .exactlyOne<ChildStickyNoteId>()
}

fun Database.Transaction.updateChildStickyNote(
    clock: EvakaClock,
    id: ChildStickyNoteId,
    note: ChildStickyNoteBody
): ChildStickyNote {
    @Suppress("DEPRECATION")
    return createUpdate(
            """
UPDATE child_sticky_note SET
    note = :note,
    expires = :expires,
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
        .exactlyOne<ChildStickyNote>()
}

fun Database.Transaction.deleteChildStickyNote(noteId: ChildStickyNoteId) {
    @Suppress("DEPRECATION")
    createUpdate("DELETE FROM child_sticky_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}
