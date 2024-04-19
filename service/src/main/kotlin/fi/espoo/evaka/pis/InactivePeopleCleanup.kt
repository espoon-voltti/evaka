// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.voltti.logging.loggers.info
import java.time.Duration
import java.time.LocalDate
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun cleanUpInactivePeople(tx: Database.Transaction, queryDate: LocalDate): Set<PersonId> {
    val twoMonthsAgo = queryDate.minusMonths(2)
    tx.setStatementTimeout(Duration.ofMinutes(20))
    val deletedPeople =
        tx.createUpdate {
                sql(
                    """
WITH people_with_no_archive_data AS (
    SELECT id FROM person
    WHERE (last_login IS NULL OR last_login::date < ${bind(twoMonthsAgo)})
    AND NOT EXISTS (SELECT 1 FROM application WHERE application.guardian_id = person.id OR application.child_id = person.id)
    AND NOT EXISTS (SELECT 1 FROM application_other_guardian aog WHERE aog.guardian_id = person.id)
    AND NOT EXISTS (SELECT 1 FROM placement WHERE placement.child_id = person.id)
    AND NOT EXISTS (
        SELECT 1 FROM fee_decision JOIN fee_decision_child ON fee_decision.id = fee_decision_child.fee_decision_id
        WHERE fee_decision.head_of_family_id = person.id OR fee_decision.partner_id = person.id OR fee_decision_child.child_id = person.id
    )
    AND NOT EXISTS (
        SELECT 1 FROM voucher_value_decision
        WHERE voucher_value_decision.head_of_family_id = person.id OR voucher_value_decision.partner_id = person.id OR voucher_value_decision.child_id = person.id
    )
    AND NOT EXISTS (SELECT 1 FROM invoice_correction WHERE head_of_family_id = person.id AND NOT applied_completely)
    AND NOT EXISTS (
        SELECT 1 FROM curriculum_document
        WHERE curriculum_document.child_id = person.id
    )
    AND NOT EXISTS (SELECT 1 FROM absence WHERE absence.child_id = person.id)
    AND NOT EXISTS (SELECT 1 FROM child_attendance WHERE child_attendance.child_id = person.id)
    AND NOT EXISTS (SELECT 1 FROM income_statement WHERE income_statement.person_id = person.id)
    AND NOT EXISTS (
        SELECT 1 FROM message
        WHERE message.sender_id = (SELECT id FROM message_account a WHERE a.person_id = person.id)
    )
    AND NOT EXISTS (
        SELECT 1 FROM message_content
        WHERE message_content.author_id = (SELECT id FROM message_account a WHERE a.person_id = person.id)
    )
)
DELETE FROM person p
WHERE id IN (SELECT id FROM people_with_no_archive_data)
-- guardianship
AND NOT EXISTS (
    SELECT child_id FROM guardian WHERE guardian_id = p.id AND NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    UNION ALL
    SELECT guardian_id FROM guardian WHERE child_id = p.id AND NOT guardian_id IN (SELECT id FROM people_with_no_archive_data)
)
-- blocked guardianship
AND NOT EXISTS (
    SELECT guardian_id FROM guardian_blocklist WHERE guardian_id = p.id
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
-- foster parent or child
AND NOT EXISTS (
    SELECT child_id FROM foster_parent WHERE parent_id = p.id AND NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    UNION ALL
    SELECT parent_id FROM foster_parent WHERE child_id = p.id AND NOT parent_id IN (SELECT id FROM people_with_no_archive_data)
)
RETURNING id
"""
                )
            }
            .executeAndReturnGeneratedKeys()
            .toSet<PersonId>()

    logger.info(mapOf("deletedPeople" to deletedPeople)) {
        "Inactive people clean up complete, deleted people count: ${deletedPeople.size}"
    }

    return deletedPeople
}
