// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.db.Database
import java.util.UUID

private data class GuardianChildPair(val guardianId: UUID, val childId: UUID)

fun Database.Transaction.insertGuardian(guardianId: UUID, childId: UUID) =
    insertGuardians(listOf(GuardianChildPair(guardianId, childId)))

fun Database.Transaction.insertGuardianChildren(guardianId: UUID, childIds: List<UUID>) =
    insertGuardians(childIds.map { GuardianChildPair(guardianId, it) })

fun Database.Transaction.insertChildGuardians(childId: UUID, guardianIds: List<UUID>) =
    insertGuardians(guardianIds.map { GuardianChildPair(it, childId) })

private fun Database.Transaction.insertGuardians(guardianIdChildIdPairs: List<GuardianChildPair>) {
    val batch = prepareBatch("INSERT INTO guardian (guardian_id, child_id) VALUES (:guardianId, :childId)")
    guardianIdChildIdPairs.forEach {
        batch.bind("guardianId", it.guardianId).bind("childId", it.childId).add()
    }
    batch.execute()
}

fun Database.Read.getChildGuardians(childId: UUID): List<UUID> {
    //language=sql
    val sql =
        """
        select guardian_id
        from guardian
        where child_id = :childId
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .mapTo(UUID::class.java)
        .list()
}

fun Database.Read.getGuardianChildIds(guardianId: UUID): List<UUID> {
    //language=sql
    val sql =
        """
        select child_id
        from guardian
        where guardian_id = :guardianId
        """.trimIndent()

    return createQuery(sql)
        .bind("guardianId", guardianId)
        .mapTo(UUID::class.java)
        .list()
}

fun Database.Transaction.deleteGuardianChildRelationShips(guardianId: UUID): Int {
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

fun Database.Transaction.deleteChildGuardianRelationships(childId: UUID): Int {
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
