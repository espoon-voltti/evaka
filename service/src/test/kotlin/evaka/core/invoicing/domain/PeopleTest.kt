// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.domain

import evaka.core.decision.DecisionSendAddress
import evaka.core.shared.PersonId
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
            mailingStreetAddress = "Kamreerintie 2",
            mailingPostalCode = "02770",
            mailingPostOffice = "Espoo",
        )

    @Test
    fun `DecisionSendAddress basic case`() {
        val address = DecisionSendAddress.fromPerson(testPerson)
        val expected =
            DecisionSendAddress(
                testPerson.mailingStreetAddress,
                testPerson.mailingPostalCode,
                testPerson.mailingPostOffice,
                testPerson.mailingStreetAddress,
                "${testPerson.mailingPostalCode} ${testPerson.mailingPostOffice}",
                "",
            )
        assertEquals(expected, address)
    }

    @Test
    fun `DecisionSendAddress handles missing fields`() {
        val person =
            testPerson.copy(
                mailingStreetAddress = "",
                mailingPostalCode = "",
                mailingPostOffice = "",
            )
        val address = DecisionSendAddress.fromPerson(person)
        assertNull(address)
    }
}
