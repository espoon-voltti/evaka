// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.DvvBasicInfo
import evaka.core.dvv.DvvBasicInfoResponse
import evaka.core.dvv.DvvDate
import evaka.core.dvv.DvvSsn
import evaka.core.dvv.HuoltajaSuppea
import evaka.core.identity.Ssn
import evaka.core.shared.config.defaultJsonMapperBuilder
import evaka.core.shared.domain.DateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import tools.jackson.module.kotlin.readValue

/** The fixtures are basic info responses of the DVV sandbox. */
class DvvPersonMapperTest {
    private val today = LocalDate.of(2026, 11, 6)

    @Test
    fun `an apartment number has no leading zeros, and 000 means no apartment, with or without a stair letter`() {
        val rows = basicInfo("apartment-number-000.json")

        val withoutStair = person(rows, "031057-999X")
        assertEquals(
            PersonAddress(
                streetAddress = "Kristiinankatu 12",
                postalCode = "64100",
                postOffice = "KRISTIINANKAUPUNKI",
                streetAddressSe = "Kristinagatan 12",
                postOfficeSe = "KRISTINESTAD",
            ),
            withoutStair.residentialAddress,
        )
        assertEquals("900000242V1 000 ", withoutStair.residenceCode)
        assertEquals("Kristiinankaupunki", withoutStair.municipalityOfResidence)
        assertEquals("Kristinestad", withoutStair.municipalityOfResidenceSe)

        assertEquals(
            "Pohjanpellonkuja 2 C",
            person(rows, "230989-9612").residentialAddress?.streetAddress,
        )

        assertEquals(
            PersonAddress(
                streetAddress = "Kiisajärvi 1 4",
                postalCode = "99950",
                postOffice = "KARIGASNIEMI",
                streetAddressSe = null,
                postOfficeSe = "KARIGASNIEMI",
            ),
            person(basicInfo("apartment-number-004.json"), "260582-9631").residentialAddress,
        )
    }

    @Test
    fun `an address can have only a street name`() {
        val rows = basicInfo("street-name-only.json")

        assertEquals("Rajala", person(rows, "280709A909B").residentialAddress?.streetAddress)
        assertEquals(
            listOf(Ssn.from("280790-9696")),
            mapDvvGuardians(groups(rows, "280709A909B"), today),
        )
    }

    @Test
    fun `a Swedish speaker has Swedish names, and a street with only a Swedish name is used for both languages`() {
        val rows = basicInfo("swedish-names.json")

        val swedishSpeaker = person(rows, "150752-999Y")
        assertEquals("sv", swedishSpeaker.nativeLanguage?.code)
        assertEquals("Maringatan 14 C 12", swedishSpeaker.residentialAddress?.streetAddressSe)
        assertEquals("Helsingfors", swedishSpeaker.municipalityOfResidenceSe)

        val swedishStreet = person(rows, "160185-980N").residentialAddress
        assertEquals("Norrbölevägen 1 106t", swedishStreet?.streetAddress)
        assertEquals("Norrbölevägen 1 106t", swedishStreet?.streetAddressSe)
    }

    @Test
    fun `the row of a corrected SSN points to the active SSN, and the row of the active SSN does not`() {
        assertEquals(
            Ssn.from("010192-921W"),
            basicInfo("corrected-ssn.json").single().correctedToSsn(),
        )

        val active = basicInfo("corrected-ssn-active.json")
        assertNull(active.single().correctedToSsn())
        assertEquals(
            listOf(Ssn.from("120514A9501"), Ssn.from("010515A957D")),
            mapDvvDependants(groups(active, "010192-921W"), today),
        )
    }

    @Test
    fun `a guardian without an SSN is left out`() {
        assertEquals(
            listOf(Ssn.from("100391-9566")),
            mapDvvGuardians(groups(basicInfo("guardian-without-ssn.json"), "140515A9342"), today),
        )
    }

    @Test
    fun `language code 98 is kept, and an artificial municipality code has no name`() {
        val person = person(basicInfo("native-language-98.json"), "100349-988C")

        assertEquals("98", person.nativeLanguage?.code)
        assertNull(person.municipalityOfResidence)
    }

    @Test
    fun `with restricted details, the censored groups give no addresses or municipality`() {
        val person = person(basicInfo("restricted-details.json"), "020275-9862")

        assertEquals(
            RestrictedDetails(enabled = true, endDate = LocalDate.of(2120, 2, 28)),
            person.restrictedDetails,
        )
        assertNull(person.residentialAddress)
        assertEquals(emptyList(), person.mailingAddressPeriods)
        assertNull(person.residenceCode)
        assertNull(person.municipalityOfResidence)
        assertEquals("Sisko Pauliina", person.firstNames)
    }

    @Test
    fun `a dead person has the date of death`() {
        val rows = basicInfo("dead-person.json")

        assertEquals(LocalDate.of(2020, 4, 14), person(rows, "030117-999B").dateOfDeath)
        assertEquals(LocalDate.of(2019, 6, 6), person(rows, "110670-9687").dateOfDeath)
    }

    @Test
    fun `a temporary domestic address is a mailing address period`() {
        val person = person(basicInfo("temporary-and-foreign-addresses.json"), "120598-9999")

        assertNull(person.residentialAddress)
        assertEquals(
            listOf(
                MailingAddressPeriod(
                    AddressPeriodType.TEMPORARY,
                    DateRange(LocalDate.of(2013, 8, 26), null),
                    PersonAddress(
                        streetAddress = "Mikonkatu 8 J 1j",
                        postalCode = "00100",
                        postOffice = "HELSINKI",
                        streetAddressSe = "Mikaelsgatan 8 J 1j",
                        postOfficeSe = "HELSINGFORS",
                    ),
                )
            ),
            person.mailingAddressPeriods,
        )
    }

    @Test
    fun `only a guardianship that is valid today and gives custody is a relation`() {
        fun guardianship(ssn: String, type: String, start: LocalDate, end: LocalDate) =
            HuoltajaSuppea(
                tietoryhma = "HUOLTAJA_SUPPEA",
                huoltaja = DvvSsn(ssn),
                huoltajanRooli = "AITI",
                huoltajanLaji = type,
                huoltosuhteenAlkupv = DvvDate(start.toString(), "PAIVA"),
                huoltosuhteenLoppupv = DvvDate(end.toString(), "PAIVA"),
            )

        val groups =
            listOf(
                guardianship("280790-9696", "LAKISAATEINEN_HUOLTAJA", today, today),
                guardianship(
                    "150385-9013",
                    "LAKISAATEINEN_HUOLTAJA",
                    today.plusDays(1),
                    today.plusYears(10),
                ),
                guardianship(
                    "200687-903A",
                    "MAARATTY_HUOLTAJA",
                    today.minusYears(1),
                    today.minusDays(1),
                ),
                guardianship(
                    "070644-937X",
                    "TIEDONSAANTIOIKEUTETTU",
                    today.minusYears(1),
                    today.plusYears(10),
                ),
            )

        assertEquals(listOf(Ssn.from("280790-9696")), mapDvvGuardians(groups, today))
    }

    private fun basicInfo(file: String): List<DvvBasicInfo> =
        jsonMapper
            .readValue<DvvBasicInfoResponse>(
                ClassPathResource("dvv/basic-info/$file").inputStream.readAllBytes()
            )
            .perustiedot

    private fun groups(rows: List<DvvBasicInfo>, ssn: String) =
        rows.single { it.henkilotunnus == ssn }.tietoryhmat

    private fun person(rows: List<DvvBasicInfo>, ssn: String) =
        mapDvvPerson(Ssn.from(ssn), groups(rows, ssn), today)

    companion object {
        private val jsonMapper = defaultJsonMapperBuilder().build()
    }
}
