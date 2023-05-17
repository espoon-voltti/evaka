// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.financelog.LogOperation
import fi.espoo.evaka.financelog.logPartnership
import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import java.util.UUID
import org.jdbi.v3.core.result.RowView

fun Database.Read.getPartnership(id: PartnershipId): Partnership? {
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
        """
            .trimIndent()

    return createQuery(sql).bind("id", id).map(toPartnership("p1", "p2")).firstOrNull()
}

fun Database.Read.getPartnershipsForPerson(
    personId: PersonId,
    includeConflicts: Boolean = false
): List<Partnership> {
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
        """
            .trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bind("includeConflicts", includeConflicts)
        .map(toPartnership("p1", "p2"))
        .toList()
}

fun Database.Read.getPartnersForPerson(
    personId: PersonId,
    includeConflicts: Boolean,
    period: DateRange? = null
): List<Partner> {
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
        """
            .trimIndent()

    return createQuery(sql)
        .bind("personId", personId)
        .bind("from", period?.start)
        .bind("to", period?.end)
        .bind("includeConflicts", includeConflicts)
        .map(toPartner("p"))
        .toList()
}

fun Database.Transaction.createPartnership(
    user: AuthenticatedUser,
    personId1: PersonId,
    personId2: PersonId,
    startDate: LocalDate,
    endDate: LocalDate?,
    conflict: Boolean = false
): Partnership {
    // language=SQL
    val sql =
        """
        WITH new_fridge_partner AS (
            INSERT INTO fridge_partner (partnership_id, indx, other_indx, person_id, start_date, end_date, conflict)
            VALUES
                (:partnershipId, 1, 2, :person1, :startDate, :endDate, :conflict),
                (:partnershipId, 2, 1, :person2, :startDate, :endDate, :conflict)
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
        """
            .trimIndent()

    return createQuery(sql)
        .bind("partnershipId", UUID.randomUUID())
        .bind("person1", personId1)
        .bind("person2", personId2)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .bind("conflict", conflict)
        .map(toPartnership("p1", "p2"))
        .first()
        .also { logPartnership(user, LogOperation.INSERT, it.id) }
}

fun Database.Transaction.updatePartnershipDuration(
    user: AuthenticatedUser,
    id: PartnershipId,
    startDate: LocalDate,
    endDate: LocalDate?
): Boolean {
    // language=SQL
    val sql =
        """
        UPDATE fridge_partner SET start_date = :startDate, end_date = :endDate
        WHERE partnership_id = :id
        RETURNING partnership_id
        """
            .trimIndent()

    logPartnership(user, LogOperation.DELETE, id)
    return createQuery(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<PartnershipId>()
        .firstOrNull()
        .let { it != null }
        .also { success -> if(success) logPartnership(user, LogOperation.INSERT, id) }
}

fun Database.Transaction.retryPartnership(id: PartnershipId) {
    // language=SQL
    val sql = "UPDATE fridge_partner SET conflict = false WHERE partnership_id = :id"

    createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.deletePartnership(user: AuthenticatedUser, id: PartnershipId): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_partner WHERE partnership_id = :id RETURNING partnership_id"

    logPartnership(user, LogOperation.DELETE, id)
    return createQuery(sql).bind("id", id).mapTo<PartnershipId>().firstOrNull() != null
}

private val toPartnership: (String, String) -> (RowView) -> Partnership =
    { partner1Alias, partner2Alias ->
        { row ->
            Partnership(
                id = row.mapColumn("partnership_id"),
                partners =
                    setOf(toPersonJSON(partner1Alias, row), toPersonJSON(partner2Alias, row)),
                startDate = row.mapColumn("start_date"),
                endDate = row.mapColumn("end_date"),
                conflict = row.mapColumn("conflict")
            )
        }
    }

private val toPartner: (String) -> (RowView) -> Partner = { tableAlias ->
    { row ->
        Partner(
            partnershipId = row.mapColumn("partnership_id"),
            person = toPersonJSON(tableAlias, row),
            startDate = row.mapColumn("start_date"),
            endDate = row.mapColumn("end_date"),
            conflict = row.mapColumn("conflict")
        )
    }
}
