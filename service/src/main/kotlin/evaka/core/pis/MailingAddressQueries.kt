// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.vtjclient.dto.AddressPeriodType
import java.time.LocalDate

data class AddressPeriodRow(
    val type: AddressPeriodType,
    val validDuring: DateRange,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
)

fun Database.Transaction.replaceAddressPeriods(
    personId: PersonId,
    periods: List<AddressPeriodRow>,
) {
    execute { sql("DELETE FROM person_address_period WHERE person_id = ${bind(personId)}") }
    executeBatch(periods) {
        sql(
            """
INSERT INTO person_address_period (person_id, type, valid_during, street_address, postal_code, post_office)
VALUES (${bind(personId)}, ${bind { it.type }}, ${bind { it.validDuring }}, ${bind { it.streetAddress }}, ${bind { it.postalCode }}, ${bind { it.postOffice }})
"""
        )
    }
}

/**
 * Sets the mailing address of the persons to the mailing address period that is valid on [date], or
 * to the residential address if no period is valid.
 */
fun Database.Transaction.updateMailingAddresses(
    date: LocalDate,
    personIds: Collection<PersonId>,
): Int = createUpdate {
    sql(
        """
WITH new_address AS (
    SELECT
        p.id,
        coalesce(period.street_address, p.street_address) AS street_address,
        coalesce(period.postal_code, p.postal_code) AS postal_code,
        coalesce(period.post_office, p.post_office) AS post_office
    FROM person p
    LEFT JOIN LATERAL (
        SELECT street_address, postal_code, post_office
        FROM person_address_period
        WHERE person_id = p.id AND valid_during @> ${bind(date)}
        ORDER BY CASE type WHEN 'MAILING' THEN 1 WHEN 'TEMPORARY' THEN 2 END
        LIMIT 1
    ) period ON TRUE
    WHERE p.id = ANY(${bind(personIds)})
)
UPDATE person p SET
    mailing_street_address = new_address.street_address,
    mailing_postal_code = new_address.postal_code,
    mailing_post_office = new_address.post_office
FROM new_address
WHERE p.id = new_address.id
AND (p.mailing_street_address, p.mailing_postal_code, p.mailing_post_office) IS DISTINCT FROM
    (new_address.street_address, new_address.postal_code, new_address.post_office)
"""
    )
}
    .executeAndReturnCount()

/**
 * Updates the mailing addresses of all persons that have mailing address periods, and deletes the
 * periods that have ended before [today].
 */
fun Database.Transaction.updateMailingAddressesFromPeriods(today: LocalDate): Int {
    val personIds = createQuery {
        sql("SELECT DISTINCT person_id FROM person_address_period")
    }
        .toList<PersonId>()
    val updated = updateMailingAddresses(today, personIds)
    execute { sql("DELETE FROM person_address_period WHERE upper(valid_during) <= ${bind(today)}") }
    return updated
}
