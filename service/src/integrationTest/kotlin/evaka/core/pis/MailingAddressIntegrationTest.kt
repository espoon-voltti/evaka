// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis

import evaka.core.PureJdbiTest
import evaka.core.shared.PersonId
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.dto.AddressPeriodType
import evaka.core.vtjclient.dto.MailingAddressPeriod
import evaka.core.vtjclient.dto.NativeLanguage
import evaka.core.vtjclient.dto.PersonAddress
import evaka.core.vtjclient.dto.RestrictedDetails
import evaka.core.vtjclient.dto.VtjPerson
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class MailingAddressIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2026, 3, 2)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))

    private val residentialAddress = Address("Kotikatu 1", "02100", "Espoo")

    private val vtjPerson =
        VtjPerson(
            firstNames = "Matti",
            lastName = "Meikäläinen",
            socialSecurityNumber = "010199-8137",
            residentialAddress =
                PersonAddress("Kotikatu 1", "02100", "Espoo", "Hemgatan 1", "Esbo"),
            nativeLanguage = NativeLanguage(languageName = "suomi", code = "fi"),
            restrictedDetails = RestrictedDetails(enabled = false),
        )

    @Test
    fun `the residential address is the mailing address when there are no periods`() {
        val id = db.transaction { it.applyPerson(vtjPerson, now) }.id

        assertEquals(residentialAddress, residentialAddressOf(id))
        assertEquals(residentialAddress, mailingAddressOf(id))
    }

    @Test
    fun `a mailing address is used before a temporary address that is valid on the same day`() {
        val id =
            db.transaction {
                    it.applyPerson(
                        vtjPerson.copy(
                            mailingAddressPeriods =
                                listOf(
                                    period(AddressPeriodType.TEMPORARY, today, null, "Mökkitie 2"),
                                    period(AddressPeriodType.MAILING, today, null, "PL 10"),
                                )
                        ),
                        now,
                    )
                }
                .id

        assertEquals(Address("PL 10", "02101", "Espoo"), mailingAddressOf(id))
        assertEquals(residentialAddress, residentialAddressOf(id))
    }

    @Test
    fun `a temporary address is used when no mailing address is valid`() {
        val id =
            db.transaction {
                    it.applyPerson(
                        vtjPerson.copy(
                            mailingAddressPeriods =
                                listOf(
                                    period(
                                        AddressPeriodType.MAILING,
                                        today.plusDays(1),
                                        null,
                                        "PL 10",
                                    ),
                                    period(AddressPeriodType.TEMPORARY, today, null, "Mökkitie 2"),
                                )
                        ),
                        now,
                    )
                }
                .id

        assertEquals(Address("Mökkitie 2", "02101", "Espoo"), mailingAddressOf(id))
    }

    @Test
    fun `a future mailing address is taken into use on its start date`() {
        val start = today.plusDays(7)
        val id =
            db.transaction {
                    it.applyPerson(
                        vtjPerson.copy(
                            mailingAddressPeriods =
                                listOf(period(AddressPeriodType.MAILING, start, null, "PL 10"))
                        ),
                        now,
                    )
                }
                .id
        assertEquals(residentialAddress, mailingAddressOf(id))

        db.transaction { it.updateMailingAddressesFromPeriods(start.minusDays(1)) }
        assertEquals(residentialAddress, mailingAddressOf(id))

        db.transaction { it.updateMailingAddressesFromPeriods(start) }
        assertEquals(Address("PL 10", "02101", "Espoo"), mailingAddressOf(id))
    }

    @Test
    fun `the mailing address returns to the residential address when the temporary address ends`() {
        val end = today.plusDays(3)
        val id =
            db.transaction {
                    it.applyPerson(
                        vtjPerson.copy(
                            mailingAddressPeriods =
                                listOf(
                                    period(AddressPeriodType.TEMPORARY, today, end, "Mökkitie 2")
                                )
                        ),
                        now,
                    )
                }
                .id

        db.transaction { it.updateMailingAddressesFromPeriods(end) }
        assertEquals(Address("Mökkitie 2", "02101", "Espoo"), mailingAddressOf(id))
        assertEquals(1, periodCount())

        db.transaction { it.updateMailingAddressesFromPeriods(end.plusDays(1)) }
        assertEquals(residentialAddress, mailingAddressOf(id))
        assertEquals(0, periodCount())
    }

    @Test
    fun `periods that have already ended are not stored`() {
        db.transaction {
            it.applyPerson(
                vtjPerson.copy(
                    mailingAddressPeriods =
                        listOf(
                            period(
                                AddressPeriodType.TEMPORARY,
                                today.minusDays(10),
                                today.minusDays(1),
                                "Mökkitie 2",
                            )
                        )
                ),
                now,
            )
        }

        assertEquals(0, periodCount())
    }

    @Test
    fun `a new VTJ fetch replaces the periods of the person`() {
        db.transaction {
            it.applyPerson(
                vtjPerson.copy(
                    mailingAddressPeriods =
                        listOf(period(AddressPeriodType.MAILING, today, null, "PL 10"))
                ),
                now,
            )
        }

        val id = db.transaction { it.applyPerson(vtjPerson, now) }.id

        assertEquals(residentialAddress, mailingAddressOf(id))
        assertEquals(0, periodCount())
    }

    @Test
    fun `a Swedish speaker gets the Swedish mailing address`() {
        val id =
            db.transaction {
                    it.applyPerson(
                        vtjPerson.copy(
                            nativeLanguage = NativeLanguage(languageName = "ruotsi", code = "sv"),
                            mailingAddressPeriods =
                                listOf(period(AddressPeriodType.MAILING, today, null, "PL 10")),
                        ),
                        now,
                    )
                }
                .id

        assertEquals(Address("PB 10", "02101", "Esbo"), mailingAddressOf(id))
        assertEquals(Address("Hemgatan 1", "02100", "Esbo"), residentialAddressOf(id))
    }

    private data class Address(
        val streetAddress: String,
        val postalCode: String,
        val postOffice: String,
    )

    private fun period(
        type: AddressPeriodType,
        start: LocalDate,
        end: LocalDate?,
        street: String,
    ) =
        MailingAddressPeriod(
            type,
            DateRange(start, end),
            PersonAddress(street, "02101", "Espoo", street.replace("PL", "PB"), "Esbo"),
        )

    private fun mailingAddressOf(id: PersonId) =
        db.read { it.getPersonById(id)!! }
            .let { Address(it.mailingStreetAddress, it.mailingPostalCode, it.mailingPostOffice) }

    private fun residentialAddressOf(id: PersonId) =
        db.read { it.getPersonById(id)!! }
            .let { Address(it.streetAddress, it.postalCode, it.postOffice) }

    private fun periodCount() = db.read {
        it.createQuery { sql("SELECT count(*) FROM person_address_period") }.exactlyOne<Int>()
    }
}
