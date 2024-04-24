// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import java.time.LocalDate

private data class GuardianChildPair(val guardianId: PersonId, val childId: ChildId)

fun Database.Transaction.insertGuardian(guardianId: PersonId, childId: ChildId) =
    insertGuardians(listOf(GuardianChildPair(guardianId, childId)))

fun Database.Transaction.insertGuardianChildren(guardianId: PersonId, childIds: List<ChildId>) =
    insertGuardians(childIds.map { GuardianChildPair(guardianId, it) })

fun Database.Transaction.insertChildGuardians(childId: ChildId, guardianIds: List<PersonId>) =
    insertGuardians(guardianIds.map { GuardianChildPair(it, childId) })

fun Database.Read.isGuardianBlocked(guardianId: PersonId, childId: ChildId): Boolean =
    createQuery {
            sql(
                """
SELECT EXISTS (
    SELECT 1
    FROM guardian_blocklist
    WHERE guardian_id = ${bind(guardianId)}
    AND child_id = ${bind(childId)}
)
"""
            )
        }
        .exactlyOne<Boolean>()

fun Database.Read.getBlockedGuardians(childId: ChildId): List<PersonId> {
    return createQuery {
            sql("SELECT guardian_id FROM guardian_blocklist WHERE child_id = ${bind(childId)}")
        }
        .toList<PersonId>()
}

/**
 * Blocks a child <-> VTJ guardian relationship by removing it if it exists, and preventing it from
 * getting recreated by adding it to the blocklist
 */
fun Database.Transaction.blockGuardian(childId: ChildId, guardianId: PersonId) {
    deleteGuardianRelationship(childId = childId, guardianId = guardianId)
    createUpdate {
            sql(
                "INSERT INTO guardian_blocklist (child_id, guardian_id) VALUES (${bind(childId)}, ${bind(guardianId)})"
            )
        }
        .execute()
}

/** Unblocks a child <-> VTJ guardian relationship by removing it from the blocklist if it exists */
fun Database.Transaction.unblockGuardian(childId: ChildId, guardianId: PersonId) {
    createUpdate {
            sql(
                "DELETE FROM guardian_blocklist WHERE child_id = ${bind(childId)} AND guardian_id = ${bind(guardianId)}"
            )
        }
        .execute()
}

fun Database.Transaction.deleteGuardianRelationship(childId: ChildId, guardianId: PersonId) {
    createUpdate {
            sql(
                "DELETE FROM guardian WHERE child_id = ${bind(childId)} AND guardian_id = ${bind(guardianId)}"
            )
        }
        .execute()
}

private fun Database.Transaction.insertGuardians(guardianIdChildIdPairs: List<GuardianChildPair>) {
    executeBatch(guardianIdChildIdPairs) {
        sql(
            """
INSERT INTO guardian (guardian_id, child_id)
VALUES (${bind { it.guardianId }}, ${bind { it.childId }}) 
"""
        )
    }
}

fun Database.Read.getChildGuardians(childId: ChildId): List<PersonId> {
    return createQuery {
            sql(
                """
                select guardian_id
                from guardian
                where child_id = ${bind(childId)}
                """
            )
        }
        .toList<PersonId>()
}

fun Database.Read.getChildGuardiansAndFosterParents(
    childId: ChildId,
    today: LocalDate
): List<PersonId> {
    return createQuery {
            sql(
                """
SELECT guardian_id AS id FROM guardian WHERE child_id = ${bind(childId)}
UNION
SELECT parent_id AS id FROM foster_parent WHERE child_id = ${bind(childId)} AND valid_during @> ${bind(today)}
"""
            )
        }
        .toList<PersonId>()
}

fun Database.Read.getGuardianChildIds(guardianId: PersonId): List<ChildId> {
    return createQuery {
            sql(
                """
                select child_id
                from guardian
                where guardian_id = ${bind(guardianId)}
                """
            )
        }
        .toList<ChildId>()
}

fun Database.Transaction.deleteGuardianChildRelationShips(guardianId: PersonId): Int {
    return createUpdate {
            sql(
                """
                DELETE FROM guardian
                WHERE guardian_id = ${bind(guardianId)}
                """
            )
        }
        .execute()
}

fun Database.Transaction.deleteChildGuardianRelationships(childId: ChildId): Int {
    return createUpdate {
            sql(
                """
                DELETE FROM guardian
                WHERE child_id = ${bind(childId)}
                """
            )
        }
        .execute()
}
