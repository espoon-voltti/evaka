// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.shared.dev.resetDatabase
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class PersonIntegrationTest : PureJdbiTest() {
    @BeforeEach
    fun beforeEach() {
        db.transaction {
            it.resetDatabase()
        }
    }

    @Test
    fun `creating an empty person creates a message account`() {
        val person = db.transaction { createEmptyPerson(it) }
        Assertions.assertTrue(personHasMessageAccount(person.id))
    }

    @Test
    fun `creating a person creates a message account`() {
        val validSSN = "080512A918W"
        val req = PersonIdentityRequest(
            identity = ExternalIdentifier.SSN.getInstance(validSSN),
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "matti.meikalainen@example.com",
            language = "fi"
        )
        val person = db.transaction { createPerson(it, req) }

        Assertions.assertTrue(personHasMessageAccount(person.id))
    }

    @Test
    fun `creating a person from request body creates a message account`() {
        val body = CreatePersonBody(
            firstName = "Matti",
            lastName = "Meikäläinen",
            email = "matti.meikalainen@example.com",
            dateOfBirth = LocalDate.of(1920, 1, 1),
            phone = "123456",
            streetAddress = "Testikatu 1",
            postalCode = "12345",
            postOffice = "Espoo"
        )
        val personId = db.transaction { createPerson(it, body) }

        Assertions.assertTrue(personHasMessageAccount(personId))
    }

    /**
     * NOTE: If this test fails you have likely added a new table referencing person/child.
     *
     * Ask yourself what should happen if person is merged to another person and fix accordingly.
     *
     * 1) if data should be transferred to the other person
     * - fix this test to include the new reference
     * - add new translation to duplicate people report on employee-frontend
     *
     * 2) if data does not need to be transferred but can be deleted
     * - edit the query in getTransferablePersonReferences to exclude this table
     * - edit deleteEmptyPerson in MergeService so that the row is deleted before trying to delete person
     */
    @Test
    fun `getTransferablePersonReferences returns references to person and child tables`() {
        val references = db.read { it.getTransferablePersonReferences() }
        Assertions.assertEquals(39, references.size)
        Assertions.assertEquals(
            listOf(
                PersonReference("absence", "child_id"),
                PersonReference("application", "child_id"),
                PersonReference("application", "guardian_id"),
                PersonReference("application", "other_guardian_id"),
                PersonReference("assistance_action", "child_id"),
                PersonReference("assistance_need", "child_id"),
                PersonReference("attachment", "uploaded_by_person"),
                PersonReference("backup_care", "child_id"),
                PersonReference("backup_pickup", "child_id"),
                PersonReference("bulletin_instance", "receiver_person_id"),
                PersonReference("bulletin_receiver", "child_id"),
                PersonReference("child_attendance", "child_id"),
                PersonReference("child_images", "child_id"),
                PersonReference("daily_service_time", "child_id"),
                PersonReference("daycare_daily_note", "child_id"),
                PersonReference("family_contact", "child_id"),
                PersonReference("family_contact", "contact_person_id"),
                PersonReference("fee_alteration", "person_id"),
                PersonReference("fee_decision", "head_of_family"),
                PersonReference("fee_decision", "partner"),
                PersonReference("fee_decision_part", "child"),
                PersonReference("fridge_child", "child_id"),
                PersonReference("fridge_child", "head_of_child"),
                PersonReference("fridge_partner", "person_id"),
                PersonReference("income", "person_id"),
                PersonReference("invoice", "head_of_family"),
                PersonReference("invoice_row", "child"),
                PersonReference("koski_study_right", "child_id"),
                PersonReference("messaging_blocklist", "blocked_recipient"),
                PersonReference("messaging_blocklist", "child_id"),
                PersonReference("new_fee_decision", "head_of_family_id"),
                PersonReference("new_fee_decision", "partner_id"),
                PersonReference("new_fee_decision_child", "child_id"),
                PersonReference("placement", "child_id"),
                PersonReference("service_need", "child_id"),
                PersonReference("varda_child", "person_id"),
                PersonReference("voucher_value_decision", "child_id"),
                PersonReference("voucher_value_decision", "head_of_family_id"),
                PersonReference("voucher_value_decision", "partner_id")
            ),
            references
        )
    }

    private fun personHasMessageAccount(personId: UUID): Boolean {
        // language=SQL
        val sql = """
            SELECT EXISTS(
                SELECT * FROM message_account WHERE person_id = :personId AND active = true
            )
        """.trimIndent()
        return db.read {
            it.createQuery(sql)
                .bind("personId", personId)
                .mapTo<Boolean>().first()
        }
    }
}
