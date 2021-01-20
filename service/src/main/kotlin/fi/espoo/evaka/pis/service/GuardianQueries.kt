// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import org.jdbi.v3.core.Handle
import java.util.UUID

fun insertGuardian(h: Handle, guardianId: UUID, childId: UUID) {
    //language=sql
    val sql =
        """
      INSERT INTO guardian (
        guardian_id,
        child_id
      )
      VALUES (
        :guardianId,
        :childId
      )
      ON CONFLICT DO NOTHING
        """.trimIndent()

    h.createUpdate(sql)
        .bind("guardianId", guardianId)
        .bind("childId", childId)
        .execute()
}

fun getChildGuardians(h: Handle, childId: UUID): List<UUID> {
    //language=sql
    val sql =
        """
        select guardian_id
        from guardian
        where child_id = :childId
        """.trimIndent()

    return h.createQuery(sql)
        .bind("childId", childId)
        .mapTo(UUID::class.java)
        .list()
}

fun getGuardianChildIds(h: Handle, guardianId: UUID): List<UUID> {
    //language=sql
    val sql =
        """
        select child_id
        from guardian
        where guardian_id = :guardianId
        """.trimIndent()

    return h.createQuery(sql)
        .bind("guardianId", guardianId)
        .mapTo(UUID::class.java)
        .list()
}

fun deleteGuardianChildRelationShips(h: Handle, guardianId: UUID): Int {
    //language=sql
    val sql =
        """
        DELETE FROM guardian
        WHERE guardian_id = :guardianId
        """.trimIndent()

    return h.createUpdate(sql)
        .bind("guardianId", guardianId)
        .execute()
}

fun deleteChildGuardianRelationships(h: Handle, childId: UUID): Int {
    //language=sql
    val sql =
        """
        DELETE FROM guardian
        WHERE child_id = :childId
        """.trimIndent()

    return h.createUpdate(sql)
        .bind("childId", childId)
        .execute()
}
