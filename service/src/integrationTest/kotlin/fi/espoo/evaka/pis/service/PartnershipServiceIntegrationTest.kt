// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.shared.domain.Conflict
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PartnershipServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var partnershipService: PartnershipService

    @Test
    fun `creating an overlapping partnership throws conflict`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(300)

        db.transaction {
            partnershipService.createPartnership(it, person1.id, person2.id, startDate, endDate)
        }
        assertThrows<Conflict> {
            db.transaction {
                partnershipService.createPartnership(it, person1.id, person3.id, startDate, endDate)
            }
        }
    }

    private fun createPerson(ssn: String, firstName: String): PersonDTO {
        return db.transaction {
            it.createPerson(
                PersonIdentityRequest(
                    identity = ExternalIdentifier.SSN.getInstance(ssn),
                    firstName = firstName,
                    lastName = "Meikäläinen",
                    email = "",
                    language = "fi"
                )
            )
        }
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")
    private fun testPerson2() = createPerson("150786-1766", "Iines")
    private fun testPerson3() = createPerson("170679-601K", "Hannu")
}
