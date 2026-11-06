// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.domain.DateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class VtjComparisonTest {
    private val person =
        VtjPerson(
            firstNames = "Matti Juhani",
            lastName = "Meikäläinen",
            ssn = Ssn.from("070644-937X"),
            residentialAddress = PersonAddress("Kamreerintie 4 A 9", "02100", "Espoo"),
            nativeLanguage = NativeLanguage(code = "fi"),
            restrictedDetails = RestrictedDetails(enabled = false),
            municipalityOfResidence = "Espoo",
        )

    @Test
    fun `masks letters and digits and keeps the other characters`() {
        assertEquals("Aaaaaaaaaaaa 99 A 9", maskText("Kamreerintie 44 B 1"))
        assertEquals("Aaaaaaaaaaa-Aaaaa", maskText("Meikäläinen-Öljyä"))
        assertEquals("**aa", maskText("Čłaa"))
    }

    @Test
    fun `a blank address field equals a missing one`() {
        assertEquals(
            emptyList(),
            compareVtjPersons(
                person.copy(residentialAddress = PersonAddress("", "", "")),
                person.copy(residentialAddress = null),
            ),
        )
    }

    @Test
    fun `a text difference has the masks and tells if only the case or the spaces differ`() {
        val dvv =
            person.copy(
                residentialAddress = PersonAddress("Kamreerintie 4 A 009", "02100", "ESPOO")
            )

        assertEquals(
            listOf(
                VtjFieldDifference(
                    "residentialAddress.streetAddress",
                    mapOf(
                        "kind" to "TEXT",
                        "soap" to "Aaaaaaaaaaaa 9 A 9",
                        "dvv" to "Aaaaaaaaaaaa 9 A 999",
                        "onlyCase" to false,
                        "onlyWhitespace" to false,
                    ),
                ),
                VtjFieldDifference(
                    "residentialAddress.postOffice",
                    mapOf(
                        "kind" to "TEXT",
                        "soap" to "Aaaaa",
                        "dvv" to "AAAAA",
                        "onlyCase" to true,
                        "onlyWhitespace" to false,
                    ),
                ),
            ),
            compareVtjPersons(person, dvv),
        )
    }

    @Test
    fun `codes are logged as such and dates as the difference in days`() {
        val dvv =
            person.copy(
                nativeLanguage = NativeLanguage(code = "98"),
                dateOfDeath = LocalDate.of(2026, 1, 10),
            )

        assertEquals(
            listOf(
                VtjFieldDifference(
                    "nativeLanguage",
                    mapOf("kind" to "CODE", "soap" to "fi", "dvv" to "98"),
                ),
                VtjFieldDifference(
                    "dateOfDeath",
                    mapOf("kind" to "DATE", "missing" to "SOAP", "days" to null),
                ),
            ),
            compareVtjPersons(person, dvv),
        )
        assertEquals(
            listOf(
                VtjFieldDifference(
                    "dateOfDeath",
                    mapOf("kind" to "DATE", "missing" to null, "days" to 9L),
                )
            ),
            compareVtjPersons(
                person.copy(dateOfDeath = LocalDate.of(2026, 1, 1)),
                person.copy(dateOfDeath = LocalDate.of(2026, 1, 10)),
            ),
        )
    }

    @Test
    fun `with restricted details on either side, a difference has only the field name`() {
        val soap = person.copy(restrictedDetails = RestrictedDetails(enabled = true))
        val dvv = soap.copy(residentialAddress = null, municipalityOfResidence = "Vantaa")

        assertEquals(
            listOf(
                "residentialAddress.streetAddress",
                "residentialAddress.postalCode",
                "residentialAddress.postOffice",
                "municipalityOfResidence",
            ),
            compareVtjPersons(soap, dvv).map { it.field },
        )
        assertEquals(emptyMap(), compareVtjPersons(soap, dvv).first().details)
    }

    @Test
    fun `mailing address periods are compared one by one when the counts match`() {
        val period =
            MailingAddressPeriod(
                AddressPeriodType.MAILING,
                DateRange(LocalDate.of(2026, 1, 1), null),
                PersonAddress("PL 12", "00100", "Helsinki"),
            )
        val soap = person.copy(mailingAddressPeriods = listOf(period))

        assertEquals(
            listOf(
                VtjFieldDifference(
                    "mailingAddressPeriods.MAILING[0].start",
                    mapOf("kind" to "DATE", "missing" to null, "days" to 1L),
                )
            ),
            compareVtjPersons(
                soap,
                soap.copy(
                    mailingAddressPeriods =
                        listOf(period.copy(validDuring = DateRange(LocalDate.of(2026, 1, 2), null)))
                ),
            ),
        )
        assertEquals(
            listOf(
                VtjFieldDifference(
                    "mailingAddressPeriods.MAILING",
                    mapOf("kind" to "SET", "soapCount" to 1, "dvvCount" to 0),
                )
            ),
            compareVtjPersons(soap, person),
        )
    }

    @Test
    fun `relatives are compared as sets of SSNs`() {
        val a = Ssn.from("150385-9013")
        val b = Ssn.from("200687-903A")
        val c = Ssn.from("050120A910J")

        assertEquals(
            emptyList(),
            compareVtjPersons(
                VtjPersonAndGuardians(person, listOf(a, b)),
                VtjPersonAndGuardians(person, listOf(b, a)),
            ),
        )
        assertEquals(
            listOf(
                VtjFieldDifference(
                    "dependants",
                    mapOf("kind" to "SET", "soapCount" to 2, "dvvCount" to 2, "commonCount" to 1),
                )
            ),
            compareVtjPersons(
                VtjPersonAndDependants(person, listOf(a, b)),
                VtjPersonAndDependants(person, listOf(a, c)),
            ),
        )
    }
}
