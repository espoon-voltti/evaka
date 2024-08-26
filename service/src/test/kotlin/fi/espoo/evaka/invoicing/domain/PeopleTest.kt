// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.decision.DecisionSendAddress
import fi.espoo.evaka.shared.PersonId
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class PeopleTest {
    private val testPerson =
        PersonDetailed(
            id = PersonId(UUID.randomUUID()),
            dateOfBirth = LocalDate.of(1980, 1, 1),
            firstName = "John",
            lastName = "Doe",
            streetAddress = "Kamreerintie 2",
            postalCode = "02770",
            postOffice = "Espoo",
            restrictedDetailsEnabled = false,
        )

    @Test
    fun `DecisionSendAddress basic case`() {
        val address = DecisionSendAddress.fromPerson(testPerson)
        val expected =
            DecisionSendAddress(
                testPerson.streetAddress,
                testPerson.postalCode,
                testPerson.postOffice,
                testPerson.streetAddress,
                "${testPerson.postalCode} ${testPerson.postOffice}",
                "",
            )
        assertEquals(expected, address)
    }

    @Test
    fun `DecisionSendAddress handles missing fields`() {
        val person = testPerson.copy(streetAddress = "", postalCode = "", postOffice = "")
        val address = DecisionSendAddress.fromPerson(person)
        assertNull(address)
    }
}
