// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.dao.PersonDAO
import fi.espoo.evaka.shared.db.handle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ParentshipServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var parentshipService: ParentshipService

    @Autowired
    lateinit var personDAO: PersonDAO

    @Test
    fun `fetch child with two heads`() {
        val parent1 = testPerson1()
        val parent2 = testPerson2()

        val child = testPerson4()

        val startDate1 = LocalDate.now()
        val endDate1 = startDate1.plusDays(50)

        jdbi.handle { h ->

            parentshipService.createParentship(h, child.id, parent1.id, startDate1, endDate1)

            val startDate2 = endDate1.plusDays(1)
            val endDate2 = startDate2.plusDays(50)

            parentshipService.createParentship(h, child.id, parent2.id, startDate2, endDate2)

            val headsByChild = parentshipService.getParentshipsByChildId(h, child.id)

            assertEquals(2, headsByChild.size)

            val childByHeads = headsByChild.map { parentshipService.getParentshipsByHeadOfChildId(h, it.headOfChildId) }

            assertEquals(2, childByHeads.size)
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
    private fun testPerson4() = createPerson("120915A931W", "Tupu")
}
