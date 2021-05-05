// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.shared.db.transaction
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class FamilySchemaConstraintsIntegrationTest : AbstractIntegrationTest() {
    @Test
    fun `basic partnership is ok`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        val partnershipId = UUID.randomUUID()
        createPartnerRecord(partnershipId, 1, person1.id, startDate, endDate)
        createPartnerRecord(partnershipId, 2, person2.id, startDate, endDate)
    }

    @Test
    fun `one person having two partnerships not overlapping is ok`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate1 = LocalDate.now()
        val endDate1 = startDate1.plusDays(200)
        UUID.randomUUID().let { partnershipId ->
            createPartnerRecord(partnershipId, 1, person1.id, startDate1, endDate1)
            createPartnerRecord(partnershipId, 2, person2.id, startDate1, endDate1)
        }

        val startDate2 = endDate1.plusDays(1)
        val endDate2 = startDate2.plusDays(200)
        UUID.randomUUID().let { partnershipId ->
            createPartnerRecord(partnershipId, 1, person1.id, startDate2, endDate2)
            createPartnerRecord(partnershipId, 2, person2.id, startDate2, endDate2)
        }
    }

    @Test
    fun `one person having two partnerships overlapping is NOT ok`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate1 = LocalDate.now()
        val endDate1 = startDate1.plusDays(200)
        UUID.randomUUID().let { partnershipId ->
            createPartnerRecord(partnershipId, 1, person1.id, startDate1, endDate1)
            createPartnerRecord(partnershipId, 2, person2.id, startDate1, endDate1)
        }

        val startDate2 = endDate1.plusDays(0)
        val endDate2 = startDate2.plusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate2, endDate2)
            createPartnerRecord(partnershipId, 2, person2.id, startDate2, endDate2)
        }
    }

    @Test
    fun `partnership having more than two partners is NOT ok - index over 2`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate, endDate)
            createPartnerRecord(partnershipId, 2, person2.id, startDate, endDate)
            createPartnerRecord(partnershipId, 3, person3.id, startDate, endDate)
        }
    }

    @Test
    fun `partnership having more than two partners is NOT ok - index under 1`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 0, person1.id, startDate, endDate)
            createPartnerRecord(partnershipId, 1, person2.id, startDate, endDate)
            createPartnerRecord(partnershipId, 2, person3.id, startDate, endDate)
        }
    }

    @Test
    fun `partnership having more than two partners is NOT ok - duplicate index`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate, endDate)
            createPartnerRecord(partnershipId, 2, person2.id, startDate, endDate)
            createPartnerRecord(partnershipId, 2, person3.id, startDate, endDate)
        }
    }

    @Test
    fun `partnership having one person twice is NOT ok`() {
        val person1 = testPerson1()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate, endDate)
            createPartnerRecord(partnershipId, 2, person1.id, startDate, endDate)
        }
    }

    @Test
    fun `partnership where partners have a different start date is NOT ok`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate1 = LocalDate.now()
        val startDate2 = startDate1.plusDays(1)
        val endDate = startDate1.plusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate1, endDate)
            createPartnerRecord(partnershipId, 2, person2.id, startDate2, endDate)
        }
    }

    @Test
    fun `partnership where partners have a different end date is NOT ok`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate = LocalDate.now()
        val endDate1 = startDate.plusDays(200)
        val endDate2 = endDate1.plusDays(1)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate, endDate1)
            createPartnerRecord(partnershipId, 2, person2.id, startDate, endDate2)
        }
    }

    @Test
    fun `partnership ending before starting is NOT ok`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.minusDays(200)

        assertThatThrownBy {
            val partnershipId = UUID.randomUUID()
            createPartnerRecord(partnershipId, 1, person1.id, startDate, endDate)
            createPartnerRecord(partnershipId, 2, person2.id, startDate, endDate)
        }
    }

    @Test
    fun `one child having two different head of childs not overlapping is ok`() {
        val child = testPerson1()
        val parent1 = testPerson2()
        val parent2 = testPerson3()

        val startDate1 = LocalDate.now()
        val endDate1 = startDate1.plusDays(200)
        createParentshipRecord(child.id, parent1.id, startDate1, endDate1)

        val startDate2 = endDate1.plusDays(1)
        val endDate2 = startDate2.plusDays(200)
        createParentshipRecord(child.id, parent2.id, startDate2, endDate2)
    }

    @Test
    fun `one child having same head of child twice and not overlapping is ok`() {
        val child = testPerson1()
        val parent = testPerson2()

        val startDate1 = LocalDate.now()
        val endDate1 = startDate1.plusDays(200)
        createParentshipRecord(child.id, parent.id, startDate1, endDate1)

        val startDate2 = endDate1.plusDays(1)
        val endDate2 = startDate2.plusDays(200)
        createParentshipRecord(child.id, parent.id, startDate2, endDate2)
    }

    @Test
    fun `one person being head of child for multiple children at the same time is ok`() {
        val child1 = testPerson1()
        val child2 = testPerson2()
        val parent = testPerson3()

        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        createParentshipRecord(child1.id, parent.id, startDate, endDate)
        createParentshipRecord(child2.id, parent.id, startDate, endDate)
    }

    @Test
    fun `one child having two head of childs at the same time is NOT ok`() {
        val child = testPerson1()
        val parent1 = testPerson2()
        val parent2 = testPerson3()

        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(200)
        createParentshipRecord(child.id, parent1.id, startDate, endDate)
        assertThatThrownBy { createParentshipRecord(child.id, parent2.id, startDate, endDate) }
    }

    private fun createPartnerRecord(
        partnershipId: UUID,
        indx: Int,
        personId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        // language=SQL
        val sql =
            """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:partnershipId, :indx, :personId, :startDate, :endDate)
            """.trimIndent()

        db.transaction { tx ->
            tx.createUpdate(sql)
                .bind("partnershipId", partnershipId)
                .bind("indx", indx)
                .bind("personId", personId)
                .bind("startDate", startDate)
                .bind("endDate", endDate)
                .execute()
        }
    }

    private fun createParentshipRecord(childId: UUID, parentId: UUID, startDate: LocalDate, endDate: LocalDate) =
        db.transaction { it.createParentship(childId, parentId, startDate, endDate) }

    private fun createPerson(ssn: String, firstName: String): PersonDTO {
        return db.transaction {
            it.createPerson(
                PersonIdentityRequest(
                    identity = ExternalIdentifier.SSN.getInstance(ssn),
                    firstName = firstName,
                    lastName = "Meikäläinen",
                    email = "${firstName.toLowerCase()}.meikalainen@example.com",
                    language = "fi"
                )
            )
        }
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")
    private fun testPerson2() = createPerson("150786-1766", "Iines")
    private fun testPerson3() = createPerson("170679-601K", "Hannu")
}
