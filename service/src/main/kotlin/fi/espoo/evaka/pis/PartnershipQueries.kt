// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.PGConstants
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.DateRange
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

fun Database.Read.getPartnership(id: UUID): Partnership? {
    // language=SQL
    val sql =
        """
        SELECT
            fp1.partnership_id,
            fp1.start_date,
            fp1.end_date,
            fp1.conflict,
            ${aliasedPersonColumns("p1")},
            ${aliasedPersonColumns("p2")}
        FROM fridge_partner fp1
        JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx = 1 AND fp2.indx = 2
        JOIN person p1 ON fp1.person_id = p1.id
        JOIN person p2 ON fp2.person_id = p2.id
        WHERE fp1.partnership_id = :id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .map(toPartnership("p1", "p2"))
        .firstOrNull()
}

fun Database.Read.getPartnershipsForPerson(personId: UUID, includeConflicts: Boolean = false): List<Partnership> {
    // language=SQL
    val sql =
        """
        SELECT
            fp1.partnership_id,
            fp1.start_date,
            fp1.end_date,
            fp1.conflict,
            ${aliasedPersonColumns("p1")},
            ${aliasedPersonColumns("p2")}
        FROM fridge_partner fp1
        JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx = 1 AND fp2.indx = 2
        JOIN person p1 ON fp1.person_id = p1.id
        JOIN person p2 ON fp2.person_id = p2.id
        WHERE fp1.person_id = :personId OR fp2.person_id = :personId
        AND (:includeConflicts OR fp1.conflict = false)
        """.trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bind("includeConflicts", includeConflicts)
        .map(toPartnership("p1", "p2"))
        .toList()
}

fun Database.Read.getPartnersForPerson(personId: UUID, includeConflicts: Boolean, period: DateRange? = null): List<Partner> {
    // language=SQL
    val sql =
        """
        SELECT
            fp.*,
            ${aliasedPersonColumns("p")}
        FROM fridge_partner fp
        JOIN fridge_partner partner ON fp.partnership_id = partner.partnership_id AND fp.indx != partner.indx
        JOIN person p ON partner.person_id = p.id
        WHERE fp.person_id = :personId
        AND daterange(fp.start_date, fp.end_date, '[]') && daterange(:from, :to, '[]')
        AND (:includeConflicts OR fp.conflict = false)
        """.trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bindNullable("from", period?.start)
        .bindNullable("to", period?.end)
        .bind("includeConflicts", includeConflicts)
        .map(toPartner("p"))
        .toList()
}

fun Database.Transaction.createPartnership(
    personId1: UUID,
    personId2: UUID,
    startDate: LocalDate,
    endDate: LocalDate?,
    conflict: Boolean = false
): Partnership {
    // language=SQL
    val sql =
        """
        WITH new_fridge_partner AS (
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date, conflict)
            VALUES
                (:partnershipId, 1, :person1, :startDate, :endDate, :conflict),
                (:partnershipId, 2, :person2, :startDate, :endDate, :conflict)
            RETURNING *
        )
        SELECT
            fp1.partnership_id,
            fp1.start_date,
            fp1.end_date,
            fp1.conflict,
            ${aliasedPersonColumns("p1")},
            ${aliasedPersonColumns("p2")}
        FROM new_fridge_partner fp1
        JOIN new_fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx = 1 AND fp2.indx = 2
        JOIN person p1 ON fp1.person_id = p1.id
        JOIN person p2 ON fp2.person_id = p2.id
        """.trimIndent()

    return createQuery(sql)
        .bind("partnershipId", UUID.randomUUID())
        .bind("person1", personId1)
        .bind("person2", personId2)
        .bind("startDate", startDate)
        .bind("endDate", endDate ?: PGConstants.infinity)
        .bind("conflict", conflict)
        .map(toPartnership("p1", "p2"))
        .first()
}

fun Database.Transaction.updatePartnershipDuration(id: UUID, startDate: LocalDate, endDate: LocalDate?): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE fridge_partner SET start_date = :startDate, end_date = :endDate
        WHERE partnership_id = :id
        RETURNING partnership_id
        """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate ?: PGConstants.infinity)
        .mapTo<UUID>()
        .firstOrNull() != null
}

fun Database.Transaction.retryPartnership(id: UUID) {
    // language=SQL
    val sql = "UPDATE fridge_partner SET conflict = false WHERE partnership_id = :id"

    createUpdate(sql)
        .bind("id", id)
        .execute()
}

fun Database.Transaction.deletePartnership(id: UUID): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_partner WHERE partnership_id = :id RETURNING partnership_id"

    return createQuery(sql)
        .bind("id", id)
        .mapTo<UUID>()
        .firstOrNull() != null
}

private val toPartnership: (String, String) -> (ResultSet, StatementContext) -> Partnership =
    { partner1Alias, partner2Alias ->
        { rs, _ ->
            Partnership(
                id = rs.getUUID("partnership_id"),
                partners = setOf(
                    toPersonJSON(partner1Alias, rs),
                    toPersonJSON(partner2Alias, rs)
                ),
                startDate = rs.getDate("start_date").toLocalDate(),
                endDate = rs.getDate("end_date").toLocalDate()
                    .let { if (it.isBefore(PGConstants.infinity)) it else null },
                conflict = rs.getBoolean("conflict")
            )
        }
    }

private val toPartner: (String) -> (ResultSet, StatementContext) -> Partner = { tableAlias ->
    { rs, _ ->
        Partner(
            partnershipId = rs.getUUID("partnership_id"),
            person = toPersonJSON(tableAlias, rs),
            startDate = rs.getDate("start_date").toLocalDate(),
            endDate = rs.getDate("end_date").toLocalDate()
                .let { if (it.isBefore(PGConstants.infinity)) it else null },
            conflict = rs.getBoolean("conflict")
        )
    }
}
