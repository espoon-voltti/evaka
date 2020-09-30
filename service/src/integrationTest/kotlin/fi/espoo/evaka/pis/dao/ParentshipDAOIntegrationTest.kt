// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.AbstractIntegrationTest
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.PersonIdentityRequest
import fi.espoo.evaka.pis.service.PersonJSON
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ParentshipDAOIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    lateinit var parentshipDAO: ParentshipDAO

    @Autowired
    lateinit var personDAO: PersonDAO

    @Test
    fun `test creating parentship`() {
        val child = testPerson1()
        val parent = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(100)
        val parentship = parentshipDAO.createParentship(child.id, parent.id, startDate, endDate)
        assertNotNull(parentship.id)
        assertEquals(child, parentship.child)
        assertEquals(parent.id, parentship.headOfChildId)
        assertEquals(startDate, parentship.startDate)
        assertEquals(endDate, parentship.endDate)
    }

    @Test
    fun `test creating a few parentships`() {
        val parent = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(100)

        listOf(testPerson4(), testPerson5(), testPerson6()).map {
            parentshipDAO.createParentship(it.id, parent.id, startDate, endDate)
        }

        val fetchedRelations = parentshipDAO.getParentships(headOfChildId = parent.id, childId = null)
        assertEquals(3, fetchedRelations.size)
    }

    @Test
    fun `test moving child under different parent`() {
        val child = testPerson4()
        val now = LocalDate.now()

        listOf(testPerson1(), testPerson2()).mapIndexed { index, parent ->
            val startDate = now.plusDays(index * 51.toLong())
            val endDate = startDate.plusDays((index + 1) * 50.toLong())
            parentshipDAO.createParentship(child.id, parent.id, startDate, endDate)
        }

        val fetchedRelationsByChild = parentshipDAO.getParentships(headOfChildId = null, childId = child.id)
        assertEquals(2, fetchedRelationsByChild.size)

        val fetchedRelationsByParent1 = parentshipDAO.getParentships(headOfChildId = testPerson1().id, childId = child.id)
        assertEquals(1, fetchedRelationsByParent1.size)

        val fetchedRelationsByParent2 = parentshipDAO.getParentships(headOfChildId = testPerson2().id, childId = child.id)
        assertEquals(1, fetchedRelationsByParent2.size)
    }

    @Test
    fun `test fetching parentships by head of child, child or both`() {
        val child = testPerson1()
        val adult = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(100)
        val parentship = parentshipDAO.createParentship(child.id, adult.id, startDate, endDate)

        assertEquals(setOf(parentship), parentshipDAO.getParentships(headOfChildId = adult.id, childId = child.id))
        assertEquals(setOf(parentship), parentshipDAO.getParentships(headOfChildId = adult.id, childId = null))
        assertEquals(setOf(parentship), parentshipDAO.getParentships(headOfChildId = null, childId = child.id))

        assertEquals(emptySet<Parentship>(), parentshipDAO.getParentships(headOfChildId = child.id, childId = adult.id))
        assertEquals(emptySet<Parentship>(), parentshipDAO.getParentships(headOfChildId = child.id, childId = null))
        assertEquals(emptySet<Parentship>(), parentshipDAO.getParentships(headOfChildId = null, childId = adult.id))
    }

    private fun createPerson(ssn: String, firstName: String): PersonJSON {
        return personDAO.getOrCreatePersonIdentity(
            PersonIdentityRequest(
                identity = ExternalIdentifier.SSN.getInstance(ssn),
                firstName = firstName,
                lastName = "Meikäläinen",
                email = "${firstName.toLowerCase()}.meikalainen@example.com",
                language = "fi"
            )
        ).let { PersonJSON.from(it) }
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")
    private fun testPerson2() = createPerson("150786-1766", "Iines")
    private fun testPerson4() = createPerson("120915A931W", "Tupu")
    private fun testPerson5() = createPerson("120915A9074", "Hupu")
    private fun testPerson6() = createPerson("120915A983K", "Lupu")
}
