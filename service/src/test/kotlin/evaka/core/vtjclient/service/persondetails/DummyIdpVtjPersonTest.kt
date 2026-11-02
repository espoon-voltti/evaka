// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import evaka.core.identity.Ssn
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.VtjRelative
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DummyIdpVtjPersonTest {
    private val child =
        DummyIdpVtjPerson(
            firstNames = "Child",
            lastName = "A",
            socialSecurityNumber = "050120A910J",
            address = PersonAddress("Kotikatu 1", "02100", "Espoo", null, null),
        )

    @Test
    fun `a relative is a stub with the SSN, the date of birth and the names`() {
        assertEquals(
            VtjRelative(
                ssn = Ssn.parse("050120A910J")!!,
                dateOfBirth = LocalDate.of(2020, 1, 5),
                firstNames = "Child",
                lastName = "A",
            ),
            child.toVtjRelative(),
        )
    }

    @Test
    fun `a person keeps the basic details`() {
        val result = child.toVtjPerson()
        assertEquals("050120A910J", result.socialSecurityNumber)
        assertEquals("Kotikatu 1", result.residentialAddress?.streetAddress)
    }
}
