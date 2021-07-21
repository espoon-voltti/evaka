// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.db.Database
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate
import java.util.UUID

val logger = KotlinLogging.logger {}

fun cleanUpInactivePeople(tx: Database.Transaction, queryDate: LocalDate): Set<UUID> {
    // The list of data that needs to be archived is not final
    val peopleToCleanUp = tx.createQuery(
        """
WITH people_with_no_archive_data AS (
    SELECT id FROM person
    WHERE (last_login IS NULL OR last_login::date < :twoWeeksAgo)
    AND NOT EXISTS (SELECT 1 FROM application WHERE application.guardian_id = person.id OR application.child_id = person.id OR application.other_guardian_id = person.id)
    AND NOT EXISTS (SELECT 1 FROM placement WHERE placement.child_id = person.id)
    AND NOT EXISTS (
        SELECT 1 FROM fee_decision JOIN fee_decision_child ON fee_decision.id = fee_decision_child.fee_decision_id
        WHERE fee_decision.head_of_family_id = person.id OR fee_decision.partner_id = person.id OR fee_decision_child.child_id = person.id
    )
    AND NOT EXISTS (
        SELECT 1 FROM voucher_value_decision
        WHERE voucher_value_decision.head_of_family_id = person.id OR voucher_value_decision.partner_id = person.id OR voucher_value_decision.child_id = person.id
    )
    AND NOT EXISTS (
        SELECT 1 FROM vasu_document
        WHERE vasu_document.child_id = person.id
    )
)
SELECT id FROM people_with_no_archive_data p
-- guardianship
WHERE NOT EXISTS (
    SELECT child_id FROM guardian WHERE guardian_id = p.id AND NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    UNION ALL
    SELECT guardian_id FROM guardian WHERE child_id = p.id AND NOT guardian_id IN (SELECT id FROM people_with_no_archive_data)
)
-- own fridge child
AND NOT EXISTS (
    SELECT child_id FROM fridge_child
    WHERE head_of_child = p.id AND NOT child_id IN (SELECT id FROM people_with_no_archive_data)
)
-- partner
AND NOT EXISTS (
    SELECT p2.person_id
    FROM fridge_partner p1
    JOIN fridge_partner p2 ON p1.person_id = p.id AND p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    WHERE NOT p2.person_id IN (SELECT id FROM people_with_no_archive_data)
)
-- partner's child
AND NOT EXISTS (
    SELECT fridge_child.child_id
    FROM fridge_partner p1
    JOIN fridge_partner p2 ON p1.person_id = p.id AND p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    JOIN fridge_child ON fridge_child.head_of_child = p2.person_id AND daterange(p2.start_date, p2.end_date, '[]') && daterange(fridge_child.start_date, fridge_child.end_date, '[]')
    WHERE NOT fridge_child.child_id IN (SELECT id FROM people_with_no_archive_data)
)
-- own head of family
AND NOT EXISTS (
    SELECT head_of_child FROM fridge_child
    WHERE child_id = p.id AND NOT head_of_child IN (SELECT id FROM people_with_no_archive_data)
)
-- own head of family's partner
AND NOT EXISTS (
    SELECT p2.person_id
    FROM fridge_child
    JOIN fridge_partner p1 ON fridge_child.head_of_child = p1.person_id AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(p1.start_date, p1.end_date, '[]')
    JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    WHERE fridge_child.child_id = p.id AND NOT p2.person_id IN (SELECT id FROM people_with_no_archive_data)
)
-- siblings through own head of family
AND NOT EXISTS (
    SELECT sibling.child_id
    FROM fridge_child
    JOIN fridge_child sibling ON fridge_child.head_of_child = sibling.head_of_child AND fridge_child.child_id != sibling.child_id AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(sibling.start_date, sibling.end_date, '[]')
    WHERE fridge_child.child_id = p.id AND NOT sibling.child_id IN (SELECT id FROM people_with_no_archive_data)
)
-- siblings through own head of family's partner
AND NOT EXISTS (
    SELECT sibling.child_id
    FROM fridge_child
    JOIN fridge_partner p1 ON fridge_child.head_of_child = p1.person_id AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(p1.start_date, p1.end_date, '[]')
    JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    JOIN fridge_child sibling ON p2.person_id = sibling.head_of_child AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(sibling.start_date, sibling.end_date, '[]')
    WHERE fridge_child.child_id = p.id AND NOT sibling.child_id IN (SELECT id FROM people_with_no_archive_data)
)
-- contact person for a child
AND NOT EXISTS (
    SELECT family_contact.child_id FROM family_contact
    WHERE contact_person_id = p.id AND NOT family_contact.child_id IN (SELECT id FROM people_with_no_archive_data)
)
"""
    )
        .bind("twoWeeksAgo", queryDate.minusDays(14))
        .mapTo<UUID>()
        .toSet()

    logger.info(mapOf("somePersonIds" to peopleToCleanUp.take(10))) {
        "Count of inactive people with no data to archive: ${peopleToCleanUp.size}"
    }

    return peopleToCleanUp
}
