// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class PersonIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `creating an empty person creates a message account`() {
        val person = db.transaction { createEmptyPerson(it, RealEvakaClock()) }
        assertTrue(personHasMessageAccount(person.id))
    }

    @Test
    fun `creating a person creates a message account`() {
        val person =
            db.transaction {
                createPersonFromVtj(
                    it,
                    PersonDTO(
                        id = PersonId(UUID.randomUUID()),
                        duplicateOf = null,
                        identity = ExternalIdentifier.SSN.getInstance("080512A918W"),
                        ssnAddingDisabled = false,
                        dateOfBirth = LocalDate.of(2012, 5, 8),
                        firstName = "Matti",
                        lastName = "Meikäläinen",
                        preferredName = "",
                        email = "matti.meikalainen@example.com",
                        phone = "1234567890",
                        backupPhone = "",
                        language = "fi",
                        streetAddress = "",
                        postalCode = "",
                        postOffice = "",
                        residenceCode = ""
                    )
                )
            }

        assertTrue(personHasMessageAccount(person.id))
    }

    @Test
    fun `creating a person from request body creates a message account`() {
        val body =
            CreatePersonBody(
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

        assertTrue(personHasMessageAccount(personId))
    }

    /**
     * NOTE: If this test fails you have likely added a new table referencing person/child.
     *
     * Ask yourself what should happen if person is merged to another person and fix accordingly.
     * 1) if data should be transferred to the other person
     * - fix this test to include the new reference
     * - add new translation to duplicate people report on employee-frontend
     * 2) if data does not need to be transferred but can be deleted
     * - edit the query in getTransferablePersonReferences to exclude this table
     * - edit deleteEmptyPerson in MergeService so that the row is deleted before trying to delete
     *   person
     */
    @Test
    fun `getTransferablePersonReferences returns references to person and child tables`() {
        val references = db.read { it.getTransferablePersonReferences() }
        val expected =
            listOf(
                PersonReference("absence", "child_id"),
                PersonReference("application", "child_id"),
                PersonReference("application", "guardian_id"),
                PersonReference("application_other_guardian", "guardian_id"),
                PersonReference("assistance_action", "child_id"),
                PersonReference("assistance_factor", "child_id"),
                PersonReference("assistance_need", "child_id"),
                PersonReference("assistance_need_decision", "child_id"),
                PersonReference("assistance_need_decision_guardian", "person_id"),
                PersonReference("assistance_need_preschool_decision", "child_id"),
                PersonReference("assistance_need_preschool_decision_guardian", "person_id"),
                PersonReference("assistance_need_voucher_coefficient", "child_id"),
                PersonReference("attendance_reservation", "child_id"),
                PersonReference("backup_care", "child_id"),
                PersonReference("backup_pickup", "child_id"),
                PersonReference("calendar_event_attendee", "child_id"),
                PersonReference("calendar_event_time", "child_id"),
                PersonReference("child_attendance", "child_id"),
                PersonReference("child_daily_note", "child_id"),
                PersonReference("child_document", "child_id"),
                PersonReference("child_document_read", "person_id"),
                PersonReference("child_sticky_note", "child_id"),
                PersonReference("curriculum_document", "child_id"),
                PersonReference("daily_service_time", "child_id"),
                PersonReference("daily_service_time_notification", "guardian_id"),
                PersonReference("daycare_assistance", "child_id"),
                PersonReference("evaka_user", "citizen_id"),
                PersonReference("family_contact", "child_id"),
                PersonReference("family_contact", "contact_person_id"),
                PersonReference("fee_alteration", "person_id"),
                PersonReference("fee_decision", "head_of_family_id"),
                PersonReference("fee_decision", "partner_id"),
                PersonReference("fee_decision_child", "child_id"),
                PersonReference("foster_parent", "child_id"),
                PersonReference("foster_parent", "parent_id"),
                PersonReference("fridge_child", "child_id"),
                PersonReference("fridge_child", "head_of_child"),
                PersonReference("fridge_partner", "person_id"),
                PersonReference("holiday_questionnaire_answer", "child_id"),
                PersonReference("income", "person_id"),
                PersonReference("income_statement", "person_id"),
                PersonReference("invoice", "codebtor"),
                PersonReference("invoice", "head_of_family"),
                PersonReference("invoice_correction", "child_id"),
                PersonReference("invoice_correction", "head_of_family_id"),
                PersonReference("invoice_row", "child"),
                PersonReference("koski_study_right", "child_id"),
                PersonReference("message_thread_children", "child_id"),
                PersonReference("other_assistance_measure", "child_id"),
                PersonReference("pedagogical_document", "child_id"),
                PersonReference("pedagogical_document_read", "person_id"),
                PersonReference("placement", "child_id"),
                PersonReference("preschool_assistance", "child_id"),
                PersonReference("varda_service_need", "evaka_child_id"),
                PersonReference("varda_state", "child_id"),
                PersonReference("voucher_value_decision", "child_id"),
                PersonReference("voucher_value_decision", "head_of_family_id"),
                PersonReference("voucher_value_decision", "partner_id")
            )
        assertEquals(expected, references)
        assertEquals(expected.size, references.size)
    }

    private fun personHasMessageAccount(personId: PersonId): Boolean {
        return db.read {
            it.createQuery {
                    sql(
                        """
SELECT EXISTS(
    SELECT * FROM message_account WHERE person_id = ${bind(personId)} AND active = true
)
"""
                    )
                }
                .exactlyOne<Boolean>()
        }
    }
}
