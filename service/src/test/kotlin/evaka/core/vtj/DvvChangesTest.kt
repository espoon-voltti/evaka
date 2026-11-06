// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.DefaultTietoryhma
import evaka.core.dvv.DvvModification
import evaka.core.dvv.DvvModificationsResponse
import evaka.core.identity.Ssn
import evaka.core.shared.config.defaultJsonMapperBuilder
import evaka.core.shared.domain.DateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import tools.jackson.module.kotlin.readValue

/**
 * The fixtures are the person test cases of the DVV sandbox. Each test applies the modifications of
 * one person on the day of the modification.
 */
class DvvChangesTest {
    @Test
    fun `a name change gives the new name`() {
        val result = apply("name-change.json", "300158-974J", stored("300158-974J"))

        assertEquals("Mathilda Helena Sophie", result.person.firstName)
        assertEquals("Aslan Tes", result.person.lastName)
    }

    @Test
    fun `a move inside a municipality ends the old address and gives the new one`() {
        val before =
            stored("260582-9631")
                .copy(
                    streetAddress = "Öljymäki 11 C 3",
                    postalCode = "00780",
                    postOffice = "HELSINKI",
                    residenceCode = "900000051P1C003 ",
                    municipalityOfResidence = "Helsinki",
                )

        val result = apply("move-within-municipality.json", "260582-9631", before)

        assertEquals("Lauttasaarentie 32 B 23", result.person.streetAddress)
        assertEquals("00200", result.person.postalCode)
        assertEquals("900000046J1B023 ", result.person.residenceCode)
        assertEquals("Helsinki", result.person.municipalityOfResidence)
    }

    @Test
    fun `a move between municipalities gives the new municipality`() {
        val before =
            stored("070504A9208")
                .copy(
                    streetAddress = "Lauttasaarentie 32 A 4",
                    municipalityOfResidence = "Helsinki",
                )

        val result = apply("move-between-municipalities.json", "070504A9208", before)

        assertEquals("Gabriella 6a-6b A 2a", result.person.streetAddress)
        assertEquals("MYRKKY", result.person.postOffice)
        assertEquals("90000015964A002a", result.person.residenceCode)
        assertEquals("Kristiinankaupunki", result.person.municipalityOfResidence)
    }

    @Test
    fun `a corrected move gives the corrected address`() {
        val before =
            stored("051096-9977")
                .copy(streetAddress = "Ristinsalmi B 2", municipalityOfResidence = "Liperi")

        val result = apply("move-correction.json", "051096-9977", before)

        assertEquals("Ristinsalmi B 2", result.person.streetAddress)
        assertEquals("Liperi", result.person.municipalityOfResidence)
        assertEquals("90000028361B002 ", result.person.residenceCode)
    }

    @Test
    fun `a cancelled move gives the old address and municipality back`() {
        val before =
            stored("031057-999X")
                .copy(
                    streetAddress = "St'Mariankatu 00110 A 2",
                    residenceCode = "90000000133A002 ",
                    municipalityOfResidence = "Helsinki",
                )

        val result = apply("move-cancellation.json", "031057-999X", before)

        assertEquals("Kristiinankatu 12", result.person.streetAddress)
        assertEquals("900000242V1 000 ", result.person.residenceCode)
        assertEquals("Kristiinankaupunki", result.person.municipalityOfResidence)
    }

    @Test
    fun `a new mailing address ends the old period`() {
        val oldPeriod =
            AddressPeriodRow(
                AddressPeriodType.MAILING,
                DateRange(LocalDate.of(2011, 1, 15), null),
                "Poste restante",
                "65101",
                "VAASA",
            )
        val before = stored("140965-924V").copy(addressPeriods = setOf(oldPeriod))

        val result = apply("mailing-address-change.json", "140965-924V", before)

        assertEquals(
            listOf(LocalDate.of(2020, 1, 27) to "Vaasagatan 34 016"),
            result.person.addressPeriods.map { it.validDuring.start to it.streetAddress },
        )
    }

    @Test
    fun `a removed mailing address leaves no period`() {
        val before =
            stored("150327-977K")
                .copy(
                    addressPeriods =
                        setOf(
                            AddressPeriodRow(
                                AddressPeriodType.MAILING,
                                DateRange(LocalDate.of(2012, 7, 19), null),
                                "Övägen 172, Sottunga by",
                                "22720",
                                "SOTTUNGA",
                            )
                        )
                )

        val result = apply("mailing-address-removal.json", "150327-977K", before)

        assertEquals(emptySet(), result.person.addressPeriods)
    }

    @Test
    fun `restricted details clear the addresses and the municipality`() {
        val before =
            stored("020275-9862")
                .copy(
                    streetAddress = "Kotikatu 1",
                    postalCode = "00100",
                    postOffice = "HELSINKI",
                    residenceCode = "900000046J1B018 ",
                    municipalityOfResidence = "Helsinki",
                )

        val result = apply("restricted-details-start.json", "020275-9862", before)

        assertEquals(true, result.person.restrictedDetailsEnabled)
        assertEquals("", result.person.streetAddress)
        assertEquals("", result.person.postalCode)
        assertEquals("", result.person.residenceCode)
        assertEquals("", result.person.municipalityOfResidence)
    }

    @Test
    fun `at the end of restricted details, the address comes back`() {
        val before =
            stored("180883-998N")
                .copy(restrictedDetailsEnabled = true, municipalityOfResidence = "")

        val result = apply("restricted-details-end.json", "180883-998N", before)

        assertEquals(false, result.person.restrictedDetailsEnabled)
        assertEquals(null, result.person.restrictedDetailsEndDate)
        assertEquals("Lauttasaarentie 32 B 18", result.person.streetAddress)
        assertEquals("900000046J1B018 ", result.person.residenceCode)
    }

    @Test
    fun `a death, a corrected date of death and a removed death`() {
        assertEquals(
            LocalDate.of(2019, 6, 6),
            apply("death.json", "110670-9687", stored("110670-9687")).person.dateOfDeath,
        )
        assertEquals(
            LocalDate.of(2018, 10, 28),
            apply(
                    "death-date-correction.json",
                    "010978-9932",
                    stored("010978-9932").copy(dateOfDeath = LocalDate.of(2018, 11, 8)),
                )
                .person
                .dateOfDeath,
        )
        assertEquals(
            null,
            apply(
                    "death-removal.json",
                    "160185-980N",
                    stored("160185-980N").copy(dateOfDeath = LocalDate.of(2018, 5, 1)),
                )
                .person
                .dateOfDeath,
        )
    }

    @Test
    fun `an SSN correction gives the active SSN`() {
        assertEquals(
            Ssn.from("010869-9983"),
            apply("ssn-correction.json", "010568-998D", stored("010568-998D")).person.ssn,
        )
    }

    @Test
    fun `a native language change gives the new language`() {
        val result =
            apply(
                "native-language-change.json",
                "301062-907N",
                stored("301062-907N").copy(language = "fi"),
            )

        assertEquals("98", result.person.language)
    }

    @Test
    fun `a guardianship that ends and a new one of the same guardian keep the guardian`() {
        val child = stored("280709A909B").copy(guardians = setOf(Ssn.from("280790-9696")))
        val guardian = stored("280790-9696").copy(dependants = setOf(Ssn.from("280709A909B")))

        assertEquals(
            setOf(Ssn.from("280790-9696")),
            apply("guardianship-change.json", "280709A909B", child).person.guardians,
        )
        assertEquals(
            setOf(Ssn.from("280709A909B")),
            apply("guardianship-change.json", "280790-9696", guardian).person.dependants,
        )
    }

    @Test
    fun `an ended guardianship removes the guardian, and a guardian without an SSN is left out`() {
        val child =
            stored("140515A9342")
                .copy(guardians = setOf(Ssn.from("100391-9566"), Ssn.from("140891-9642")))

        val result = apply("guardianship-other-guardian-added.json", "140515A9342", child)

        assertEquals(setOf(Ssn.from("100391-9566")), result.person.guardians)
        assertEquals(emptySet(), result.newRelatives)
    }

    @Test
    fun `an SSN correction of a guardian keeps the guardianships with the active SSN`() {
        val child = stored("010515A957D").copy(guardians = setOf(Ssn.from("010192-921W")))
        val guardian =
            stored("010192-921W")
                .copy(dependants = setOf(Ssn.from("010515A957D"), Ssn.from("120514A9501")))

        assertEquals(
            setOf(Ssn.from("010192-921W")),
            apply("ssn-correction-with-guardianships.json", "010515A957D", child).person.guardians,
        )
        assertEquals(
            setOf(Ssn.from("010515A957D"), Ssn.from("120514A9501")),
            apply("ssn-correction-with-guardianships.json", "010192-921W", guardian)
                .person
                .dependants,
        )
    }

    @Test
    fun `a new guardian is a new relative, and a blocked guardian is left out`() {
        val child = stored("280709A909B").copy(guardians = emptySet())

        assertEquals(
            setOf(Ssn.from("280790-9696")),
            apply("guardianship-change.json", "280709A909B", child).newRelatives,
        )
        assertEquals(
            emptySet(),
            apply(
                    "guardianship-change.json",
                    "280709A909B",
                    child.copy(blockedRelatives = setOf(Ssn.from("280790-9696"))),
                )
                .person
                .guardians,
        )
    }

    @Test
    fun `guardianships are left out when eVaka does not sync them for the person`() {
        assertEquals(
            null,
            apply("guardianship-change.json", "280709A909B", stored("280709A909B"))
                .person
                .guardians,
        )
    }

    @Test
    fun `applying the modifications again gives the same result`() {
        val before =
            stored("031057-999X")
                .copy(
                    streetAddress = "St'Mariankatu 00110 A 2",
                    municipalityOfResidence = "Helsinki",
                )
        val once = apply("move-cancellation.json", "031057-999X", before)

        assertEquals(once, apply("move-cancellation.json", "031057-999X", once.person))
    }

    @Test
    fun `an unknown group is listed, and the extra information is left out`() {
        val change =
            DvvModification(
                muutospv = "2026-01-01T00:00:00.000Z",
                henkilotunnus = "070644-937X",
                tietoryhmat =
                    listOf(
                        DefaultTietoryhma("UUSI_RYHMA", Muutosattribuutti.LISATTY),
                        DefaultTietoryhma("FOO_BAR", Muutosattribuutti.POISTETTU),
                    ),
            )
        val before = stored("070644-937X")

        val result = applyDvvChanges(before, listOf(change), LocalDate.of(2026, 1, 1))

        assertEquals(listOf("FOO_BAR:POISTETTU", "UUSI_RYHMA:LISATTY"), result.unknown)
        assertEquals(before.firstName, result.person.firstName)
    }

    private fun stored(ssn: String) =
        StoredVtjPerson(
            ssn = Ssn.from(ssn),
            firstName = "Etunimi",
            lastName = "Sukunimi",
            language = "fi",
            dateOfDeath = null,
            restrictedDetailsEnabled = false,
            restrictedDetailsEndDate = null,
            streetAddress = "",
            postalCode = "",
            postOffice = "",
            residenceCode = "",
            municipalityOfResidence = "",
            addressPeriods = emptySet(),
            guardians = null,
            dependants = null,
            blockedRelatives = emptySet(),
        )

    private fun apply(file: String, ssn: String, stored: StoredVtjPerson): DvvChangesResult {
        val changes =
            jsonMapper
                .readValue<DvvModificationsResponse>(
                    ClassPathResource("dvv/modifications/$file").inputStream.readAllBytes()
                )
                .muutokset
                .filter { it.henkilotunnus == ssn }
        val today = LocalDate.parse(checkNotNull(changes.first().muutospv).take(10))
        return applyDvvChanges(stored, changes, today)
    }

    companion object {
        private val jsonMapper = defaultJsonMapperBuilder().build()
    }
}
