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
    -- a long EXCEPT chain of reasons to *keep* the person
    EXCEPT
    SELECT DISTINCT guardian_id AS id FROM application
    EXCEPT
    SELECT DISTINCT child_id FROM application
    EXCEPT
    SELECT DISTINCT guardian_id FROM application_other_guardian
    EXCEPT
    SELECT DISTINCT child_id FROM placement
    EXCEPT
    SELECT DISTINCT head_of_family_id AS id FROM fee_decision
    EXCEPT
    SELECT DISTINCT partner_id FROM fee_decision
    EXCEPT
    SELECT DISTINCT child_id FROM fee_decision_child
    EXCEPT
    SELECT DISTINCT head_of_family_id FROM voucher_value_decision
    EXCEPT
    SELECT DISTINCT partner_id FROM voucher_value_decision
    EXCEPT
    SELECT DISTINCT child_id FROM voucher_value_decision
    EXCEPT
    SELECT DISTINCT head_of_family_id FROM invoice_correction WHERE NOT applied_completely
    EXCEPT
    SELECT DISTINCT person_id FROM income_statement
    EXCEPT
    SELECT DISTINCT child_id FROM curriculum_document
    EXCEPT
    SELECT DISTINCT child_id FROM absence
    EXCEPT
    SELECT DISTINCT child_id FROM child_attendance
    EXCEPT
    SELECT DISTINCT child_id FROM assistance_need_decision
    EXCEPT
    SELECT DISTINCT person_id FROM assistance_need_decision_guardian
    EXCEPT
    SELECT DISTINCT child_id FROM assistance_need_preschool_decision
    EXCEPT
    SELECT DISTINCT child_id FROM pedagogical_document
    EXCEPT
    SELECT DISTINCT person_id FROM pedagogical_document_read
    EXCEPT
    SELECT DISTINCT person_id FROM message_account
    WHERE person_id IS NOT NULL
    AND (
        EXISTS (SELECT FROM message WHERE message.sender_id = message_account.id)
        OR EXISTS (SELECT FROM message_content WHERE message_content.author_id = message_account.id)
    )
)
DELETE FROM person p
WHERE p.id IN (
    SELECT id FROM people_with_no_archive_data
    EXCEPT
    -- guardianship
    SELECT DISTINCT guardian_id FROM guardian
    WHERE NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- guardianship
    SELECT DISTINCT child_id FROM guardian WHERE NOT guardian_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- blocked guardianship
    SELECT DISTINCT guardian_id FROM guardian_blocklist
    EXCEPT
    -- own fridge child
    SELECT DISTINCT head_of_child FROM fridge_child WHERE NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- partner
    SELECT DISTINCT p1.person_id FROM fridge_partner p1
    JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    WHERE NOT p2.person_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- partner's child
    SELECT DISTINCT p1.person_id FROM fridge_partner p1
    JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    JOIN fridge_child ON fridge_child.head_of_child = p2.person_id AND daterange(p2.start_date, p2.end_date, '[]') && daterange(fridge_child.start_date, fridge_child.end_date, '[]')
    WHERE NOT fridge_child.child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- own head of family
    SELECT DISTINCT child_id FROM fridge_child
    WHERE NOT head_of_child IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- own head of family's partner
    SELECT DISTINCT child_id FROM fridge_child
    JOIN fridge_partner p1 ON fridge_child.head_of_child = p1.person_id AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(p1.start_date, p1.end_date, '[]')
    JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    WHERE NOT p2.person_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- siblings through own head of family
    SELECT DISTINCT fridge_child.child_id FROM fridge_child
    JOIN fridge_child sibling ON fridge_child.head_of_child = sibling.head_of_child AND fridge_child.child_id != sibling.child_id AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(sibling.start_date, sibling.end_date, '[]')
    WHERE NOT sibling.child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- siblings through own head of family's partner
    SELECT DISTINCT fridge_child.child_id FROM fridge_child
    JOIN fridge_partner p1 ON fridge_child.head_of_child = p1.person_id AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(p1.start_date, p1.end_date, '[]')
    JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.indx != p2.indx
    JOIN fridge_child sibling ON p2.person_id = sibling.head_of_child AND daterange(fridge_child.start_date, fridge_child.end_date, '[]') && daterange(sibling.start_date, sibling.end_date, '[]')
    WHERE NOT sibling.child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- contact person for a child
    SELECT DISTINCT contact_person_id FROM family_contact
    WHERE NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    -- foster parent or child
    SELECT DISTINCT parent_id FROM foster_parent WHERE NOT child_id IN (SELECT id FROM people_with_no_archive_data)
    EXCEPT
    SELECT DISTINCT child_id FROM foster_parent WHERE NOT parent_id IN (SELECT id FROM people_with_no_archive_data)
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
