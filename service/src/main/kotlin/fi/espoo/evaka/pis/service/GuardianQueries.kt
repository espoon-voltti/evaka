// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

private data class GuardianChildPair(val guardianId: PersonId, val childId: ChildId)

fun Database.Transaction.insertGuardian(guardianId: PersonId, childId: ChildId) =
    insertGuardians(listOf(GuardianChildPair(guardianId, childId)))

fun Database.Transaction.insertGuardianChildren(guardianId: PersonId, childIds: List<ChildId>) =
    insertGuardians(childIds.map { GuardianChildPair(guardianId, it) })

fun Database.Transaction.insertChildGuardians(childId: ChildId, guardianIds: List<PersonId>) =
    insertGuardians(guardianIds.map { GuardianChildPair(it, childId) })

fun Database.Read.isGuardianBlocked(guardianId: PersonId, childId: ChildId): Boolean =
    createQuery(
        """
SELECT EXISTS (
    SELECT 1
    FROM guardian_blocklist
    WHERE guardian_id = :guardianId
    AND child_id = :childId
)
        """.trimIndent()
    )
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .mapTo<Boolean>()
        .one()

private fun Database.Transaction.insertGuardians(guardianIdChildIdPairs: List<GuardianChildPair>) {
    val batch = prepareBatch("INSERT INTO guardian (guardian_id, child_id) VALUES (:guardianId, :childId)")
    guardianIdChildIdPairs.forEach {
        batch.bind("guardianId", it.guardianId).bind("childId", it.childId).add()
    }
    batch.execute()
}

fun Database.Read.getChildGuardians(childId: ChildId): List<PersonId> {
    //language=sql
    val sql =
        """
        select guardian_id
        from guardian
        where child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo<PersonId>()
        .list()
}

fun Database.Read.getGuardianChildIds(guardianId: PersonId): List<ChildId> {
    //language=sql
    val sql =
        """
        select child_id
        from guardian
        where guardian_id = :guardianId
        """.trimIndent()

    return createQuery(sql)
        .bind("guardianId", guardianId)
        .mapTo<ChildId>()
        .list()
}

fun Database.Transaction.deleteGuardianChildRelationShips(guardianId: PersonId): Int {
    //language=sql
    val sql =
        """
        DELETE FROM guardian
        WHERE guardian_id = :guardianId
        """.trimIndent()

    return createUpdate(sql)
        .bind("guardianId", guardianId)
        .execute()
}

fun Database.Transaction.deleteChildGuardianRelationships(childId: ChildId): Int {
    //language=sql
    val sql =
        """
        DELETE FROM guardian
        WHERE child_id = :childId
        """.trimIndent()

    return createUpdate(sql)
        .bind("childId", childId)
        .execute()
}
