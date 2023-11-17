// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.dao

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test

class PartnershipDAOIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val clock = MockEvakaClock(HelsinkiDateTime.now())

    @Test
    fun `test creating partnership`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(100)
        val partnership =
            db.transaction { tx ->
                tx.createPartnership(
                    person1.id,
                    person2.id,
                    startDate,
                    endDate,
                    false,
                    null,
                    clock.now()
                )
            }
        assertNotNull(partnership.id)
        assertEquals(2, partnership.partners.size)
        assertEquals(startDate, partnership.startDate)
        assertEquals(endDate, partnership.endDate)
    }

    @Test
    fun `test fetching partnerships by person`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()

        val partnership1 =
            db.transaction {
                it.createPartnership(
                    person1.id,
                    person2.id,
                    LocalDate.now(),
                    LocalDate.now().plusDays(200),
                    false,
                    null,
                    clock.now()
                )
            }
        val partnership2 =
            db.transaction {
                it.createPartnership(
                    person2.id,
                    person3.id,
                    LocalDate.now().plusDays(300),
                    LocalDate.now().plusDays(400),
                    false,
                    null,
                    clock.now()
                )
            }

        val person1Partnerships = db.read { it.getPartnershipsForPerson(person1.id) }
        assertEquals(listOf(partnership1), person1Partnerships)

        val person2Partnerships = db.read { it.getPartnershipsForPerson(person2.id) }
        assertEquals(
            listOf(partnership1, partnership2).sortedBy { it.id },
            person2Partnerships.sortedBy { it.id }
        )

        val person3Partnerships = db.read { it.getPartnershipsForPerson(person3.id) }
        assertEquals(listOf(partnership2), person3Partnerships)
    }

    @Test
    fun `test partnership without endDate`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val startDate = LocalDate.now()
        val partnership =
            db.transaction {
                it.createPartnership(
                    person1.id,
                    person2.id,
                    startDate,
                    endDate = null,
                    false,
                    null,
                    clock.now()
                )
            }
        assertNotNull(partnership.id)
        assertEquals(2, partnership.partners.size)
        assertEquals(startDate, partnership.startDate)
        assertEquals(null, partnership.endDate)

        val fetched = db.read { it.getPartnershipsForPerson(person1.id).first() }
        assertEquals(partnership.id, fetched.id)
        assertEquals(2, fetched.partners.size)
        assertEquals(startDate, fetched.startDate)
        assertEquals(null, fetched.endDate)
    }

    private fun createPerson(ssn: String, firstName: String): PersonDTO =
        db.transaction { tx ->
            tx.insert(
                    DevPerson(
                        ssn = ssn,
                        dateOfBirth = getDobFromSsn(ssn),
                        firstName = firstName,
                        lastName = "Meikäläinen",
                        email = "${firstName.lowercase()}.meikalainen@example.com",
                        language = "fi"
                    ),
                    DevPersonType.RAW_ROW
                )
                .let { tx.getPersonById(it)!! }
        }

    private fun testPerson1() = createPerson("140881-172X", "Aku")

    private fun testPerson2() = createPerson("150786-1766", "Iines")

    private fun testPerson3() = createPerson("170679-601K", "Hannu")
}
