// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PartnershipServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var partnershipService: PartnershipService
    private val clock = MockEvakaClock(HelsinkiDateTime.now())

    @Test
    fun `creating an overlapping partnership throws conflict`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(300)

        db.transaction {
            partnershipService.createPartnership(
                it,
                person1.id,
                person2.id,
                startDate,
                endDate,
                null,
                clock.now()
            )
        }
        assertThrows<Conflict> {
            db.transaction {
                partnershipService.createPartnership(
                    it,
                    person1.id,
                    person3.id,
                    startDate,
                    endDate,
                    null,
                    clock.now()
                )
            }
        }
    }

    private fun createPerson(ssn: String, firstName: String): PersonDTO {
        return db.transaction { tx ->
            tx.insert(
                    DevPerson(
                        ssn = ssn,
                        dateOfBirth = getDobFromSsn(ssn),
                        firstName = firstName,
                        lastName = "Meikäläinen",
                        email = "",
                        language = "fi"
                    ),
                    DevPersonType.RAW_ROW
                )
                .let { tx.getPersonById(it)!! }
        }
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")

    private fun testPerson2() = createPerson("150786-1766", "Iines")

    private fun testPerson3() = createPerson("170679-601K", "Hannu")
}
