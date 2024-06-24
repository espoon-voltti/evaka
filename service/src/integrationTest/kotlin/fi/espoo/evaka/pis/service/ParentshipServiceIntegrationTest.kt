// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ParentshipServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var parentshipService: ParentshipService

    @Test
    fun `fetch child with two heads`() {
        val parent1 = testPerson1()
        val parent2 = testPerson2()

        val child = testPerson4()

        val startDate1 = LocalDate.now()
        val endDate1 = startDate1.plusDays(50)

        db.transaction { tx ->
            parentshipService.createParentship(
                tx,
                RealEvakaClock(),
                child.id,
                parent1.id,
                startDate1,
                endDate1,
                Creator.DVV
            )

            val startDate2 = endDate1.plusDays(1)
            val endDate2 = startDate2.plusDays(50)

            parentshipService.createParentship(
                tx,
                RealEvakaClock(),
                child.id,
                parent2.id,
                startDate2,
                endDate2,
                Creator.DVV
            )

            val headsByChild = tx.getParentships(headOfChildId = null, childId = child.id)

            assertEquals(2, headsByChild.size)

            val childByHeads =
                headsByChild.map {
                    tx.getParentships(headOfChildId = it.headOfChildId, childId = null)
                }

            assertEquals(2, childByHeads.size)
        }
    }

    private fun createPerson(
        ssn: String,
        firstName: String
    ): PersonDTO =
        db.transaction {
            val id =
                it.insert(
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
            it.getPersonById(id)!!
        }

    private fun testPerson1() = createPerson("140881-172X", "Aku")

    private fun testPerson2() = createPerson("150786-1766", "Iines")

    private fun testPerson4() = createPerson("120915A931W", "Tupu")
}
