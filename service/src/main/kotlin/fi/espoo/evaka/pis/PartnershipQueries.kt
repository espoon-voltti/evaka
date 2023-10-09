// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.pis.service.Partner
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.DateRange
import java.time.LocalDate
import java.util.UUID

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

    return createQuery(sql).bind("id", id).map(toPartnership("p1", "p2")).exactlyOneOrNull()
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
        .toList(toPartnership("p1", "p2"))
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
        .toList(toPartner("p"))
}

fun Database.Transaction.createPartnership(
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
        .exactlyOne()
}

fun Database.Transaction.updatePartnershipDuration(
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

    return createQuery(sql)
        .bind("id", id)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
        .mapTo<PartnershipId>()
        .useIterable { it.firstOrNull() } != null
}

fun Database.Transaction.retryPartnership(id: PartnershipId) {
    // language=SQL
    val sql = "UPDATE fridge_partner SET conflict = false WHERE partnership_id = :id"

    createUpdate(sql).bind("id", id).execute()
}

fun Database.Transaction.deletePartnership(id: PartnershipId): Boolean {
    // language=SQL
    val sql = "DELETE FROM fridge_partner WHERE partnership_id = :id RETURNING partnership_id"

    return createQuery(sql).bind("id", id).mapTo<PartnershipId>().useIterable {
        it.firstOrNull()
    } != null
}

private val toPartnership: (String, String) -> Row.() -> Partnership =
    { partner1Alias, partner2Alias ->
        {
            Partnership(
                id = column("partnership_id"),
                partners = setOf(toPersonJSON(partner1Alias), toPersonJSON(partner2Alias)),
                startDate = column("start_date"),
                endDate = column("end_date"),
                conflict = column("conflict")
            )
        }
    }

private val toPartner: (String) -> Row.() -> Partner = { tableAlias ->
    {
        Partner(
            partnershipId = column("partnership_id"),
            person = toPersonJSON(tableAlias),
            startDate = column("start_date"),
            endDate = column("end_date"),
            conflict = column("conflict")
        )
    }
}
