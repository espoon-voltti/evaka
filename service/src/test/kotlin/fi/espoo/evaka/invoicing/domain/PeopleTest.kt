// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.decision.DecisionSendAddress
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeopleTest {
    private val testPerson = PersonData.Detailed(
        id = UUID.randomUUID(),
        dateOfBirth = LocalDate.of(1980, 1, 1),
        firstName = "John",
        lastName = "Doe",
        streetAddress = "Kamreerintie 2",
        postalCode = "02770",
        postOffice = "Espoo",
        restrictedDetailsEnabled = false
    )

    @Test
    fun `DecisionSendAddress basic case`() {
        val address = DecisionSendAddress.fromPerson(testPerson)
        val expected = DecisionSendAddress(
            testPerson.streetAddress!!,
            testPerson.postalCode!!,
            testPerson.postOffice!!,
            testPerson.streetAddress!!,
            "${testPerson.postalCode} ${testPerson.postOffice}",
            ""
        )
        assertEquals(expected, address)
    }

    @Test
    fun `DecisionSendAddress handles missing fields`() {
        val testPeople = listOf(
            testPerson.copy(streetAddress = null, postalCode = null, postOffice = null),
            testPerson.copy(streetAddress = "", postalCode = null, postOffice = null),
            testPerson.copy(streetAddress = null, postalCode = "", postOffice = null),
            testPerson.copy(streetAddress = null, postalCode = null, postOffice = ""),
            testPerson.copy(streetAddress = "", postalCode = "", postOffice = "")
        )

        testPeople.forEach {
            val address = DecisionSendAddress.fromPerson(it)
            assertNull(address)
        }
    }
}
