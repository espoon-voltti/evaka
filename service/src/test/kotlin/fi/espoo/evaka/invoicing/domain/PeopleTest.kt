// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.domain

import fi.espoo.evaka.shared.message.EvakaMessageProvider
import fi.espoo.evaka.shared.message.MessageLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

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
    fun `MailAddress basic case`() {
        val address = MailAddress.fromPerson(testPerson, EvakaMessageProvider())
        val expected = MailAddress(
            testPerson.streetAddress!!,
            testPerson.postalCode!!,
            testPerson.postOffice!!
        )
        assertEquals(expected, address)
    }

    @Test
    fun `MailAddress handles missing fields`() {
        val testPeople = listOf(
            testPerson.copy(streetAddress = null, postalCode = null, postOffice = null),
            testPerson.copy(streetAddress = "", postalCode = null, postOffice = null),
            testPerson.copy(streetAddress = null, postalCode = "", postOffice = null),
            testPerson.copy(streetAddress = null, postalCode = null, postOffice = ""),
            testPerson.copy(streetAddress = "", postalCode = "", postOffice = "")
        )

        testPeople.forEach {
            val messageProvider = EvakaMessageProvider()
            val address = MailAddress.fromPerson(it, messageProvider)
            assertEquals(messageProvider.getDefaultFeeDecisionAddress(MessageLanguage.FI), address)
        }
    }
}
