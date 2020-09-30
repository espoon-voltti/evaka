// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import fi.espoo.evaka.identity.ExternalIdentifier.SSN.Companion.getInstance
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.vtjclient.dto.Nationality
import fi.espoo.evaka.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.PersonDetails
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class MockDataServiceTest {
    private val mockDetailsService = MockPersonDetailsService()

    @Test
    fun `check that second child is less than three years old (2)`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("270372-905L"))

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)

        val kaarinaAnttola = (result as PersonDetails.Result).vtjPerson

        val children = kaarinaAnttola.dependants
        val childTwo = children[1]
        val year = childTwo.socialSecurityNumber.substring(4, 6).toInt()
        val difference = 18 - year

        assertTrue(difference < 3)
    }

    @Test
    fun `check that second child is less than three years old (1)`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("060195-966B"))

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)
        val parent = (result as PersonDetails.Result).vtjPerson

        val children = parent.dependants
        val childTwo = children[1]
        val year = childTwo.socialSecurityNumber.substring(4, 6).toInt()
        val difference = 18 - year

        assertTrue(difference < 3)
    }

    @Test
    fun `check that second child is less than three years old (0)`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("081181-9984"))

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)
        val parent = (result as PersonDetails.Result).vtjPerson

        val children = parent.dependants
        val childTwo = children[1]
        val year = childTwo.socialSecurityNumber.substring(4, 6).toInt()
        val difference = 18 - year

        assertTrue(difference < 3)
    }

    @Test
    fun `check that normal user and their children have addresses`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("081181-9984"))

        val expectedAddress = "Raivaajantie 13"

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)
        val parent = (result as PersonDetails.Result).vtjPerson

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", false)
        assertThat(parent.address!!.streetAddress).isEqualTo(expectedAddress)

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children).allMatch(
            { !it.restrictedDetails!!.enabled },
            "restricted details should not be enabled"
        )
        assertThat(children).extracting("address.streetAddress").containsExactly(expectedAddress, expectedAddress)
    }

    @Test
    fun `there should be a user with both them and their children having details restrictions`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("150978-9025"))

        val expectedEmptyAddress = ""

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)
        val parent = (result as PersonDetails.Result).vtjPerson

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", true)
        assertThat(parent.address!!.streetAddress).isBlank()

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children).allMatch(
            { it.restrictedDetails!!.enabled },
            "restricted details should be enabled"
        )
        assertThat(children).extracting("address.streetAddress")
            .containsExactly(expectedEmptyAddress, expectedEmptyAddress)
    }

    @Test
    fun `there should be a user with no restrictions but whose children have details restrictions`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("300190-9257"))

        val expectedGuardianAddress = "Raivaajantie 18"
        val expectedEmptyAddress = ""

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)
        val parent = (result as PersonDetails.Result).vtjPerson

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", false)
        assertThat(parent.address!!.streetAddress).isEqualTo(expectedGuardianAddress)

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children).allMatch(
            { it.restrictedDetails!!.enabled },
            "restricted details should be enabled"
        )
        assertThat(children).extracting("address.streetAddress")
            .containsExactly(expectedEmptyAddress, expectedEmptyAddress)
    }

    @Test
    fun `there should be a user with details restriction but whose children don't have details restrictions`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("031083-910S"))

        val expectedChildAddress = "Raivaajantie 19"

        assertThat(result).isExactlyInstanceOf(PersonDetails.Result::class.java)
        val parent = (result as PersonDetails.Result).vtjPerson

        assertThat(parent).hasFieldOrPropertyWithValue("restrictedDetails.enabled", true)
        assertThat(parent.address!!.streetAddress).isBlank()

        val children = parent.dependants
        assertThat(children).size().isEqualTo(2)
        assertThat(children).allMatch(
            { !it.restrictedDetails!!.enabled },
            "restricted details should not enabled"
        )
        assertThat(children).extracting("address.streetAddress")
            .containsExactly(expectedChildAddress, expectedChildAddress)
    }

    @Test
    fun `mr Karhula has two children`() {
        val result = mockDetailsService.getPersonWithDependants(mapToQuery("070644-937X"))
        val parent = (result as PersonDetails.Result).vtjPerson

        assertEquals(2, parent.dependants.size)
    }

    @Test
    fun `nationalities come through`() {
        val result = mockDetailsService.getBasicDetailsFor(mapToQuery("070644-937X"))

        val expectedNationality = Nationality("Suomi", "FIN")

        val parent = (result as PersonDetails.Result).vtjPerson

        assertEquals(listOf(expectedNationality), parent.nationalities)
    }

    private fun mapToQuery(ssn: String) = DetailsQuery(
        targetIdentifier = getInstance(ssn),
        requestingUser = AuthenticatedUser(UUID.randomUUID(), setOf())
    )
}
