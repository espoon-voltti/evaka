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
    createQuery(
            """
SELECT EXISTS (
    SELECT 1
    FROM guardian_blocklist
    WHERE guardian_id = :guardianId
    AND child_id = :childId
)
        """
                .trimIndent()
        )
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .exactlyOne<Boolean>()

fun Database.Read.getBlockedGuardians(childId: ChildId): List<PersonId> {
    return createQuery("SELECT guardian_id FROM guardian_blocklist WHERE child_id = :childId")
        .bind("childId", childId)
        .toList<PersonId>()
}

fun Database.Transaction.addToGuardianBlocklist(childId: ChildId, guardianId: PersonId) {
    createUpdate(
            "INSERT INTO guardian_blocklist (child_id, guardian_id) VALUES (:childId, :guardianId)"
        )
        .bind("childId", childId)
        .bind("guardianId", guardianId)
        .execute()
}

fun Database.Transaction.deleteFromGuardianBlocklist(childId: ChildId, guardianId: PersonId) {
    createUpdate(
            "DELETE FROM guardian_blocklist WHERE child_id = :childId AND guardian_id = :guardianId"
        )
        .bind("childId", childId)
        .bind("guardianId", guardianId)
        .execute()
}

fun Database.Transaction.deleteGuardianRelationship(childId: ChildId, guardianId: PersonId) {
    createUpdate("DELETE FROM guardian WHERE child_id = :childId AND guardian_id = :guardianId")
        .bind("childId", childId)
        .bind("guardianId", guardianId)
        .execute()
}

private fun Database.Transaction.insertGuardians(guardianIdChildIdPairs: List<GuardianChildPair>) {
    val batch =
        prepareBatch("INSERT INTO guardian (guardian_id, child_id) VALUES (:guardianId, :childId)")
    guardianIdChildIdPairs.forEach {
        batch.bind("guardianId", it.guardianId).bind("childId", it.childId).add()
    }
    batch.execute()
}

fun Database.Read.getChildGuardians(childId: ChildId): List<PersonId> {
    // language=sql
    val sql =
        """
        select guardian_id
        from guardian
        where child_id = :childId
        """
            .trimIndent()

    return createQuery(sql).bind("childId", childId).toList<PersonId>()
}

fun Database.Read.getChildGuardiansAndFosterParents(
    childId: ChildId,
    today: LocalDate
): List<PersonId> {
    return createQuery(
            """
    SELECT guardian_id AS id FROM guardian WHERE child_id = :childId
    UNION
    SELECT parent_id AS id FROM foster_parent WHERE child_id = :childId AND valid_during @> :today
    """
        )
        .bind("childId", childId)
        .bind("today", today)
        .toList<PersonId>()
}

fun Database.Read.getGuardianChildIds(guardianId: PersonId): List<ChildId> {
    // language=sql
    val sql =
        """
        select child_id
        from guardian
        where guardian_id = :guardianId
        """
            .trimIndent()

    return createQuery(sql).bind("guardianId", guardianId).toList<ChildId>()
}

fun Database.Transaction.deleteGuardianChildRelationShips(guardianId: PersonId): Int {
    // language=sql
    val sql =
        """
        DELETE FROM guardian
        WHERE guardian_id = :guardianId
        """
            .trimIndent()

    return createUpdate(sql).bind("guardianId", guardianId).execute()
}

fun Database.Transaction.deleteChildGuardianRelationships(childId: ChildId): Int {
    // language=sql
    val sql =
        """
        DELETE FROM guardian
        WHERE child_id = :childId
        """
            .trimIndent()

    return createUpdate(sql).bind("childId", childId).execute()
}
