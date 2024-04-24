// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import fi.espoo.evaka.identity.ExternalIdentifier.SSN.Companion.getInstance
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.legacyMockVtjDataset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class MockDataServiceTest {
    private val mockDetailsService =
        MockPersonDetailsService().also { MockPersonDetailsService.add(legacyMockVtjDataset()) }

    @Test
    fun `check that second child is less than three years old (2)`() {
        val kaarinaAnttola = mockDetailsService.getPersonWithDependants(mapToQuery("270372-905L"))

        val children = kaarinaAnttola.dependants
        val childTwo = children[1]
        val year = childTwo.socialSecurityNumber.substring(4, 6).toInt()
        val difference = 18 - year

        assertTrue(difference < 3)
    }

    @Test
    fun `check that second child is less than three years old (1)`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("060195-966B"))

        val children = parent.dependants
        val childTwo = children[1]
        val year = childTwo.socialSecurityNumber.substring(4, 6).toInt()
        val difference = 18 - year

        assertTrue(difference < 3)
    }

    @Test
    fun `check that second child is less than three years old (0)`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("081181-9984"))

        val children = parent.dependants
        val childTwo = children[1]
        val year = childTwo.socialSecurityNumber.substring(4, 6).toInt()
        val difference = 18 - year

        assertTrue(difference < 3)
    }

    @Test
    fun `check that normal user and their children have addresses`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("081181-9984"))

        val expectedAddress = "Raivaajantie 13"

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", false)
        assertThat(parent.address!!.streetAddress).isEqualTo(expectedAddress)

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children)
            .allMatch(
                { !it.restrictedDetails!!.enabled },
                "restricted details should not be enabled"
            )
        assertThat(children)
            .extracting("address.streetAddress")
            .containsExactly(expectedAddress, expectedAddress)
    }

    @Test
    fun `there should be a user with both them and their children having details restrictions`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("150978-9025"))

        val expectedEmptyAddress = ""

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", true)
        assertThat(parent.address!!.streetAddress).isBlank

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children)
            .allMatch({ it.restrictedDetails!!.enabled }, "restricted details should be enabled")
        assertThat(children)
            .extracting("address.streetAddress")
            .containsExactly(expectedEmptyAddress, expectedEmptyAddress)
    }

    @Test
    fun `there should be a user with no restrictions but whose children have details restrictions`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("300190-9257"))

        val expectedGuardianAddress = "Raivaajantie 18"
        val expectedEmptyAddress = ""

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", false)
        assertThat(parent.address!!.streetAddress).isEqualTo(expectedGuardianAddress)

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children)
            .allMatch({ it.restrictedDetails!!.enabled }, "restricted details should be enabled")
        assertThat(children)
            .extracting("address.streetAddress")
            .containsExactly(expectedEmptyAddress, expectedEmptyAddress)
    }

    @Test
    fun `there should be a user with details restriction but whose children don't have details restrictions`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("031083-910S"))

        val expectedChildAddress = "Raivaajantie 19"

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", true)
        assertThat(parent.address!!.streetAddress).isBlank

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children)
            .allMatch({ !it.restrictedDetails!!.enabled }, "restricted details should not enabled")
        assertThat(children)
            .extracting("address.streetAddress")
            .containsExactly(expectedChildAddress, expectedChildAddress)
    }

    @Test
    fun `mr Karhula has 3 children`() {
        val parent = mockDetailsService.getPersonWithDependants(mapToQuery("070644-937X"))

        assertEquals(3, parent.dependants.size)
    }

    @Test
    fun `nationalities come through`() {
        val parent = mockDetailsService.getBasicDetailsFor(mapToQuery("070644-937X"))

        val expectedNationality = Nationality("Suomi", "FIN")

        assertEquals(listOf(expectedNationality), parent.nationalities)
    }

    private fun mapToQuery(ssn: String) =
        DetailsQuery(
            targetIdentifier = getInstance(ssn),
            requestingUser = EvakaUserId(UUID.randomUUID())
        )
}
