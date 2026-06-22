// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service.persondetails

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class DummyIdpVtjPersonTest {
    private val child =
        DummyIdpVtjPerson(firstNames = "Child", lastName = "A", socialSecurityNumber = "C1")
    private val guardian =
        DummyIdpVtjPerson(
            firstNames = "Guardian",
            lastName = "One",
            socialSecurityNumber = "G1",
            dependants = listOf(child),
        )

    @Test
    fun `basic details drop relationships`() {
        val result = guardian.toVtjPerson(includeGuardians = false, includeDependants = false)
        assertEquals("G1", result.socialSecurityNumber)
        assertEquals(emptyList(), result.dependants)
        assertEquals(emptyList(), result.guardians)
    }

    @Test
    fun `dependants are included one level only`() {
        val result = guardian.toVtjPerson(includeGuardians = false, includeDependants = true)
        assertEquals(listOf("C1"), result.dependants.map { it.socialSecurityNumber })
        assertEquals(emptyList(), result.dependants.single().dependants)
    }

    @Test
    fun `guardians are included one level only`() {
        // childWithGuardian has guardian G1; verify guardians list and that nested guardian carries
        // no further relations
        val childWithGuardian =
            DummyIdpVtjPerson(
                firstNames = "Child",
                lastName = "A",
                socialSecurityNumber = "C1",
                guardians = listOf(guardian),
            )
        val result =
            childWithGuardian.toVtjPerson(includeGuardians = true, includeDependants = false)
        assertEquals(listOf("G1"), result.guardians.map { it.socialSecurityNumber })
        assertEquals(emptyList(), result.guardians.single().guardians)
        assertEquals(emptyList(), result.guardians.single().dependants)
    }
}
