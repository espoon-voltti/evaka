// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtjclient.service

import evaka.core.identity.ExternalIdentifier.SSN.Companion.getInstance
import evaka.core.shared.EvakaUserId
import evaka.core.vtjclient.dto.Nationality
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService.DetailsQuery
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.core.vtjclient.service.persondetails.MockVtjDataset
import evaka.core.vtjclient.service.persondetails.MockVtjPerson
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
        MockPersonDetailsService().also { MockPersonDetailsService.add(testDataset()) }

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
                "restricted details should not be enabled",
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
            requestingUser = EvakaUserId(UUID.randomUUID()),
        )

    private fun testDataset() =
        MockVtjDataset(
            persons =
                listOf(
                    MockVtjPerson(
                        "Sirkka-Liisa Marja-Leena Minna-Mari Anna-Kaisa",
                        "Korhonen-Hämäläinen",
                        "270372-905L",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Johannes Leo Elias Eemi",
                        "Korhonen-Hämäläinen",
                        "220314A983X",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Anneli Selma Iisa",
                        "Korhonen-Hämäläinen",
                        "051116A902A",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Hannele Kaarina Marjatta",
                        "Finström",
                        "060195-966B",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Marja-Leena Minna-Mari Sirkka-Liisa Anna-Kaisa",
                        "Finström",
                        "180213A909W",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Tapani Aatu Valtteri",
                        "Finström",
                        "040317A946W",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Sylvi Liisa Sofia",
                        "Marttila",
                        "081181-9984",
                        RestrictedDetails(enabled = false),
                        PersonAddress("Raivaajantie 13", "02100", "Espoo"),
                    ),
                    MockVtjPerson(
                        "Maria Ellen Aava Seela",
                        "Marttila",
                        "170714A911L",
                        RestrictedDetails(enabled = false),
                        PersonAddress("Raivaajantie 13", "02100", "Espoo"),
                    ),
                    MockVtjPerson(
                        "Tapani Niklas Luka",
                        "Marttila",
                        "040918A972U",
                        RestrictedDetails(enabled = false),
                        PersonAddress("Raivaajantie 13", "02100", "Espoo"),
                    ),
                    MockVtjPerson(
                        "Sirpa Maria Annikki",
                        "Silkkiuikku",
                        "150978-9025",
                        RestrictedDetails(enabled = true),
                        PersonAddress("", "", ""),
                    ),
                    MockVtjPerson(
                        "Liisa Oona Ilona",
                        "Silkkiuikku",
                        "061014A908U",
                        RestrictedDetails(enabled = true),
                        PersonAddress("", "", ""),
                    ),
                    MockVtjPerson(
                        "Matti Mikael Joel Kasper",
                        "Silkkiuikku",
                        "010713A933R",
                        RestrictedDetails(enabled = true),
                        PersonAddress("", "", ""),
                    ),
                    MockVtjPerson(
                        "Heikki Tapio Eemeli",
                        "Haahka",
                        "300190-9257",
                        RestrictedDetails(enabled = false),
                        PersonAddress("Raivaajantie 18", "02150", "Espoo"),
                    ),
                    MockVtjPerson(
                        "Marjatta Viola Anni",
                        "Haahka",
                        "140514A966U",
                        RestrictedDetails(enabled = true),
                        PersonAddress("", "", ""),
                    ),
                    MockVtjPerson(
                        "Ilmari Emil Eemil Joona",
                        "Haahka",
                        "260213A9125",
                        RestrictedDetails(enabled = true),
                        PersonAddress("", "", ""),
                    ),
                    MockVtjPerson(
                        "Seija Anna Kaarina",
                        "Sotka",
                        "031083-910S",
                        RestrictedDetails(enabled = true),
                        PersonAddress("", "", ""),
                    ),
                    MockVtjPerson(
                        "Anna Viivi Alma",
                        "Sotka",
                        "190513A9454",
                        RestrictedDetails(enabled = false),
                        PersonAddress("Raivaajantie 19", "02160", "Espoo"),
                    ),
                    MockVtjPerson(
                        "Mikael Daniel Lenni Benjamin",
                        "Sotka",
                        "100915A900L",
                        RestrictedDetails(enabled = false),
                        PersonAddress("Raivaajantie 19", "02160", "Espoo"),
                    ),
                    MockVtjPerson(
                        "Johannes Olavi Antero Tapio",
                        "Karhula",
                        "070644-937X",
                        RestrictedDetails(enabled = false),
                        nationalities =
                            listOf(Nationality(countryName = "Suomi", countryCode = "FIN")),
                    ),
                    MockVtjPerson(
                        "Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani",
                        "Karhula",
                        "070714A9126",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Kaarina Veera Nelli",
                        "Karhula",
                        "160616A978U",
                        RestrictedDetails(enabled = false),
                    ),
                    MockVtjPerson(
                        "Porri Hatter",
                        "Karhula",
                        "160620A999J",
                        RestrictedDetails(enabled = true),
                    ),
                ),
            guardianDependants =
                mapOf(
                    "270372-905L" to listOf("220314A983X", "051116A902A"),
                    "060195-966B" to listOf("180213A909W", "040317A946W"),
                    "081181-9984" to listOf("170714A911L", "040918A972U"),
                    "150978-9025" to listOf("061014A908U", "010713A933R"),
                    "300190-9257" to listOf("140514A966U", "260213A9125"),
                    "031083-910S" to listOf("190513A9454", "100915A900L"),
                    "070644-937X" to listOf("070714A9126", "160616A978U", "160620A999J"),
                ),
        )
}
