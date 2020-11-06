// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.dao.PersonDAO
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.domain.Conflict
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class PartnershipServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var partnershipService: PartnershipService

    @Autowired
    lateinit var personDAO: PersonDAO

    @Test
    fun `creating an overlapping partnership throws conflict`() = jdbi.handle { h ->
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(300)

        partnershipService.createPartnership(h, person1.id, person2.id, startDate, endDate)
        assertThrows<Conflict> {
            partnershipService.createPartnership(h, person1.id, person3.id, startDate, endDate)
        }
    }

    private fun createPerson(ssn: String, firstName: String): PersonDTO {
        return personDAO.getOrCreatePersonIdentity(
            PersonIdentityRequest(
                identity = ExternalIdentifier.SSN.getInstance(ssn),
                firstName = firstName,
                lastName = "Meikäläinen",
                email = "",
                language = "fi"
            )
        )
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")
    private fun testPerson2() = createPerson("150786-1766", "Iines")
    private fun testPerson3() = createPerson("170679-601K", "Hannu")
}
