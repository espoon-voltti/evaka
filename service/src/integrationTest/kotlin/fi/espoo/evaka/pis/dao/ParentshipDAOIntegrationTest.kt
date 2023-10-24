// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insert
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class ParentshipDAOIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    @Test
    fun `test creating parentship`() {
        val child = testPerson1()
        val parent = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(100)
        val parentship =
            db.transaction { tx -> tx.createParentship(child.id, parent.id, startDate, endDate) }
        assertNotNull(parentship.id)
        assertEquals(
            child.copy(updatedFromVtj = null),
            parentship.child.copy(updatedFromVtj = null)
        )
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
            db.transaction { tx -> tx.createParentship(it.id, parent.id, startDate, endDate) }
        }

        val fetchedRelations =
            db.read { r -> r.getParentships(headOfChildId = parent.id, childId = null) }
        assertEquals(3, fetchedRelations.size)
    }

    @Test
    fun `test moving child under different parent`() {
        val child = testPerson4()
        val now = LocalDate.now()
        val adult1 = testPerson1()
        val adult2 = testPerson2()

        listOf(adult1, adult2).mapIndexed { index, parent ->
            val startDate = now.plusDays(index * 51.toLong())
            val endDate = startDate.plusDays((index + 1) * 50.toLong())
            db.transaction { tx -> tx.createParentship(child.id, parent.id, startDate, endDate) }
        }

        db.read { r ->
            val fetchedRelationsByChild = r.getParentships(headOfChildId = null, childId = child.id)
            assertEquals(2, fetchedRelationsByChild.size)

            val fetchedRelationsByParent1 =
                r.getParentships(headOfChildId = adult1.id, childId = child.id)
            assertEquals(1, fetchedRelationsByParent1.size)

            val fetchedRelationsByParent2 =
                r.getParentships(headOfChildId = adult2.id, childId = child.id)
            assertEquals(1, fetchedRelationsByParent2.size)
        }
    }

    @Test
    fun `test fetching parentships by head of child, child or both`() {
        val child = testPerson1()
        val adult = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(100)
        db.transaction { tx ->
            val parentship = tx.createParentship(child.id, adult.id, startDate, endDate)

            assertEquals(
                setOf(parentship),
                tx.getParentships(headOfChildId = adult.id, childId = child.id).toHashSet()
            )
            assertEquals(
                setOf(parentship),
                tx.getParentships(headOfChildId = adult.id, childId = null).toHashSet()
            )
            assertEquals(
                setOf(parentship),
                tx.getParentships(headOfChildId = null, childId = child.id).toHashSet()
            )

            assertEquals(
                emptySet<Parentship>(),
                tx.getParentships(headOfChildId = child.id, childId = adult.id).toHashSet()
            )
            assertEquals(
                emptySet<Parentship>(),
                tx.getParentships(headOfChildId = child.id, childId = null).toHashSet()
            )
            assertEquals(
                emptySet<Parentship>(),
                tx.getParentships(headOfChildId = null, childId = adult.id).toHashSet()
            )
        }
    }

    private fun createPerson(ssn: String, firstName: String): PersonJSON =
        db.transaction { tx ->
                tx.getPersonBySSN(ssn)
                    ?: tx.insert(
                            DevPerson(
                                ssn = ssn,
                                dateOfBirth = getDobFromSsn(ssn),
                                firstName = firstName,
                                lastName = "Meikäläinen",
                                email = "${firstName.lowercase()}.meikalainen@example.com",
                                language = "fi"
                            )
                        )
                        .let { tx.getPersonById(it)!! }
            }
            .let { PersonJSON.from(it) }

    private fun testPerson1() = createPerson("140881-172X", "Aku")

    private fun testPerson2() = createPerson("150786-1766", "Iines")

    private fun testPerson4() = createPerson("120915A931W", "Tupu")

    private fun testPerson5() = createPerson("120915A9074", "Hupu")

    private fun testPerson6() = createPerson("120915A983K", "Lupu")
}
