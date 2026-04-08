// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.FullApplicationTest
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.getPersonById
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PartnershipServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var partnershipService: PartnershipService
    private val clock = MockEvakaClock(HelsinkiDateTime.now())
    private val employee = DevEmployee()

    @Test
    fun `creating an overlapping partnership throws conflict`() {
        val person1 = testPerson1()
        val person2 = testPerson2()
        val person3 = testPerson3()
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(300)
        db.transaction {
            it.insert(employee)
            partnershipService.createPartnership(
                it,
                person1.id,
                person2.id,
                startDate,
                endDate,
                employee.evakaUserId,
                clock.now(),
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
                    employee.evakaUserId,
                    clock.now(),
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
                        language = "fi",
                    ),
                    DevPersonType.RAW_ROW,
                )
                .let { tx.getPersonById(it)!! }
        }
    }

    private fun testPerson1() = createPerson("140881-172X", "Aku")

    private fun testPerson2() = createPerson("150786-1766", "Iines")

    private fun testPerson3() = createPerson("170679-601K", "Hannu")
}
