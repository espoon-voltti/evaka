// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

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
    fun `a person keeps the basic details`() {
        val result = child.toVtjPerson()
        assertEquals("050120A910J", result.ssn.value)
        assertEquals("Kotikatu 1", result.residentialAddress?.streetAddress)
    }
}
