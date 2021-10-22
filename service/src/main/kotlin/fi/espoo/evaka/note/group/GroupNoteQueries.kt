// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.note.group

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getGroupNotesForChild(childId: UUID): List<GroupNote> = createQuery(
    """
    SELECT gn.id, gn.group_id, gn.note, gn.modified_by, gn.modified_at
    FROM group_note gn
    WHERE group_id = (
        SELECT (
            CASE 
                WHEN bc.id IS NOT NULL THEN bc.group_id
                ELSE gpl.daycare_group_id
            END 
        ) AS group_id
        FROM person ch
        JOIN placement pl ON pl.child_id = ch.id AND daterange(pl.start_date, pl.end_date, '[]') @> :today
        JOIN daycare_group_placement gpl ON gpl.daycare_placement_id = pl.id AND daterange(gpl.start_date, gpl.end_date, '[]') @> :today
        LEFT JOIN backup_care bc ON bc.child_id = ch.id AND daterange(bc.start_date, bc.end_date, '[]') @> :today
        WHERE ch.id = :childId
    )
    ORDER BY gn.created
    """.trimIndent()
)
    .bind("childId", childId)
    .bind("today", LocalDate.now())
    .mapTo<GroupNote>()
    .list()

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
    SELECT gn.id, gn.group_id, gn.note, gn.modified_by, gn.modified_at
    FROM group_note gn
    WHERE group_id = ANY(:groupIds)
    """.trimIndent()
)
    .bind("groupIds", groupIds.toTypedArray())
    .mapTo<GroupNote>()
    .list()

fun Database.Transaction.createGroupNote(user: AuthenticatedUser, groupId: GroupId, note: GroupNoteBody): GroupNoteId {
    return createUpdate(
        """
INSERT INTO group_note (group_id, note, modified_by)
VALUES (:groupId, :note, :modifiedBy)
RETURNING id
        """.trimIndent()
    )
        .bindKotlin(note)
        .bind("groupId", groupId)
        .bind("modifiedBy", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<GroupNoteId>()
        .one()
}

fun Database.Transaction.updateGroupNote(user: AuthenticatedUser, id: GroupNoteId, note: GroupNoteBody): GroupNote {
    return createUpdate(
        """
UPDATE group_note SET
    note = :note, 
    modified_by = :modifiedBy,
    modified_at = now()
WHERE id = :id
RETURNING *
        """.trimIndent()
    )
        .bind("id", id)
        .bindKotlin(note)
        .bind("modifiedBy", user.id)
        .executeAndReturnGeneratedKeys()
        .mapTo<GroupNote>()
        .one()
}

fun Database.Transaction.deleteGroupNote(noteId: GroupNoteId) {
    createUpdate("DELETE from group_note WHERE id = :noteId")
        .bind("noteId", noteId)
        .execute()
}
