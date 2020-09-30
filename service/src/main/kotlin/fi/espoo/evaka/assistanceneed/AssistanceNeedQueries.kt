// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Handle
import java.time.LocalDate
import java.util.UUID

fun insertAssistanceNeed(h: Handle, user: AuthenticatedUser, childId: UUID, data: AssistanceNeedRequest): AssistanceNeed {
    //language=sql
    val sql =
        """
        INSERT INTO assistance_need (
            child_id, 
            start_date, 
            end_date, 
            updated_by, 
            capacity_factor, 
            description, 
            bases,          
            other_basis                   
        )
        VALUES (
            :childId, 
            :startDate, 
            :endDate, 
            :updatedBy, 
            :capacityFactor, 
            :description,
            :bases::assistance_basis[],
            :otherBasis
        )
        RETURNING *
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("childId", childId)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("capacityFactor", data.capacityFactor)
        .bind("description", data.description)
        .bind("bases", data.bases.map { it.toString() }.toTypedArray())
        .bind("otherBasis", data.otherBasis)
        .mapTo(AssistanceNeed::class.java)
        .first()
}

fun getAssistanceNeedsByChild(h: Handle, childId: UUID): List<AssistanceNeed> {
    //language=sql
    val sql = "SELECT * FROM assistance_need WHERE child_id = :childId ORDER BY start_date DESC "
    return h.createQuery(sql)
        .bind("childId", childId)
        .mapTo(AssistanceNeed::class.java)
        .list()
}

fun updateAssistanceNeed(h: Handle, user: AuthenticatedUser, id: UUID, data: AssistanceNeedRequest): AssistanceNeed {
    //language=sql
    val sql =
        """
        UPDATE assistance_need SET 
            start_date = :startDate,
            end_date = :endDate,
            updated_by = :updatedBy,
            capacity_factor = :capacityFactor,
            description = :description,
            bases = :bases::assistance_basis[],
            other_basis = :otherBasis
        WHERE id = :id
        RETURNING *
        """.trimIndent()

    return h.createQuery(sql)
        .bind("id", id)
        .bind("startDate", data.startDate)
        .bind("endDate", data.endDate)
        .bind("updatedBy", user.id)
        .bind("capacityFactor", data.capacityFactor)
        .bind("description", data.description)
        .bind("bases", data.bases.map { it.toString() }.toTypedArray())
        .bind("otherBasis", data.otherBasis)
        .mapTo(AssistanceNeed::class.java)
        .firstOrNull() ?: throw NotFound("Assistance need $id not found")
}

fun shortenOverlappingAssistanceNeed(h: Handle, user: AuthenticatedUser, childId: UUID, startDate: LocalDate, endDate: LocalDate) {
    //language=sql
    val sql =
        """
        UPDATE assistance_need 
        SET end_date = :startDate - interval '1 day', updated_by = :updatedBy
        WHERE child_id = :childId AND daterange(start_date, end_date, '[]') @> :startDate AND start_date <> :startDate
        RETURNING *
        """.trimIndent()

    h.createUpdate(sql)
        .bind("childId", childId)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("updatedBy", user.id)
        .execute()
}

fun deleteAssistanceNeed(h: Handle, id: UUID) {
    //language=sql
    val sql = "DELETE FROM assistance_need WHERE id = :id"
    val deleted = h.createUpdate(sql).bind("id", id).execute()
    if (deleted == 0) throw NotFound("Assistance need $id not found")
}
