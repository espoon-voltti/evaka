// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.config.defaultJsonMapperBuilder
import evaka.core.vtjclient.dto.VtjPerson
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import tools.jackson.databind.json.JsonMapper

/**
 * Parity checks for [toVtjPerson], asserting the DVV REST `/perustiedot` mapping reproduces what
 * the SOAP `VtjHenkiloMapper` builds. The fixture is a real (synthetic) DVV sandbox response
 * captured from the `mutpT1x` demo account ("maximal person customer with turvakielto handling"),
 * which censors protected persons the way production does — see the turvakielto test below.
 */
class DvvPerustiedotMapperTest {
    private val jsonMapper: JsonMapper = defaultJsonMapperBuilder().build()
    // A date after every fixture address' alkupv; regular (vakinainen) addresses are not
    // validity-checked anyway, so the exact value does not affect these assertions.
    private val today = LocalDate.of(2026, 7, 22)

    private val persons: Map<String, VtjPerson> by lazy {
        val json =
            checkNotNull(
                javaClass.getResourceAsStream(
                    "/__files/perustiedot-client/perustiedot-response-ok.json"
                )
            ) {
                "fixture not found"
            }
        json.use {
            jsonMapper.readValue(it, DvvPerustiedotResponse::class.java).perustiedot.associate { p
                ->
                p.henkilotunnus to p.toVtjPerson(today)
            }
        }
    }

    @Test
    fun `maps a full person - name, address, language, nationality, municipality, dependants`() {
        val p = assertNotNull(persons["010192-921W"])
        assertEquals("Hanna", p.firstNames)
        assertEquals("Kolehmainen Tes", p.lastName)
        assertEquals("010192-921W", p.socialSecurityNumber)

        // street re-assembled from the components REST splits apart; fi/sv into the Se fields.
        // SOAP delivers this pre-joined including the apartment ("Kauppa Puistikko 6 B 23"), so
        // dropping huoneistokirjain/huoneistonumero here would silently shorten stored addresses.
        val address = assertNotNull(p.address)
        assertEquals("Mikkolantie 30 F 028", address.streetAddress)
        assertEquals("Mickelsvägen 30 F 028", address.streetAddressSe)
        assertEquals("00640", address.postalCode)
        assertEquals("HELSINKI", address.postOffice)
        assertEquals("HELSINGFORS", address.postOfficeSe)

        assertEquals("900000041C1F028 ", p.residenceCode) // trailing space preserved, as in SOAP
        assertEquals("fi", p.nativeLanguage?.code)
        assertEquals("", p.nativeLanguage?.languageName) // REST carries no name for known languages
        assertEquals(listOf("246"), p.nationalities.map { it.countryCode })
        assertEquals("091", p.municipalityOfResidence) // kuntakoodi, not the SOAP plaintext name
        assertNull(p.municipalityOfResidenceSe)
        assertEquals(false, p.restrictedDetails?.enabled)

        // dependants are name+SSN stubs (HUOLLETTAVA), like the SOAP first hop
        assertEquals(
            setOf("120514A9501", "010515A957D"),
            p.dependants.map { it.socialSecurityNumber }.toSet(),
        )
        assertTrue(p.guardians.isEmpty())
    }

    @Test
    fun `maps turvakielto with end date, and censors protected location data`() {
        val p = assertNotNull(persons["020275-9862"])
        assertEquals(true, p.restrictedDetails?.enabled)
        assertEquals(LocalDate.of(2120, 2, 28), p.restrictedDetails?.endDate)
        // Identity data is not turvakielto-censored: name and nationality come through.
        assertEquals("Sisko Pauliina", p.firstNames)
        assertEquals(listOf("233"), p.nationalities.map { it.countryCode })
        assertNull(p.nativeLanguage) // no AIDINKIELI group
        // Under mutpT1x, location groups are present but their values are censored to
        // {"turvakiellonAlaisetKentat": [...]}, so every location field maps to null — this is
        // exactly what eVaka must see for a protected person (mutpT1 would leak the real values).
        assertNull(p.address)
        assertNull(p.residenceCode)
        assertNull(p.municipalityOfResidence) // KOTIKUNTA present but kuntakoodi censored
    }

    @Test
    fun `maps a child's guardians as stubs`() {
        val child = assertNotNull(persons["010121A962W"])
        assertEquals("", child.firstNames) // no HENKILON_NIMI group on this person
        val guardian = child.guardians.single()
        assertEquals("Malla", guardian.firstNames)
        assertEquals("Heikkinen Tes", guardian.lastName)
        assertEquals("010188-916P", guardian.socialSecurityNumber)
        assertNull(guardian.restrictedDetails) // stub carries no restricted details
    }

    private fun guardian(ssn: String, alku: String?, loppu: String?): String {
        val period =
            listOfNotNull(
                    alku?.let { """"huoltosuhteenAlkupv":{"arvo":"$it","tarkkuus":"PAIVA"}""" },
                    loppu?.let { """"huoltosuhteenLoppupv":{"arvo":"$it","tarkkuus":"PAIVA"}""" },
                )
                .joinToString(",", prefix = if (alku == null && loppu == null) "" else ",")
        return """{"tietoryhma":"HUOLTAJA","huoltaja":{"henkilotunnus":"$ssn"}$period}"""
    }

    /** Builds a person from synthetic tietoryhmä JSON, for cases the sandbox fixture lacks. */
    private fun personWith(vararg tietoryhmat: String): VtjPerson =
        jsonMapper
            .readValue(
                """{"henkilotunnus":"010101-1230","tietoryhmat":[${tietoryhmat.joinToString(",")}]}""",
                DvvPerustieto::class.java,
            )
            .toVtjPerson(today)

    @Test
    fun `maps dateOfDeath when kuollut is true`() {
        val p =
            personWith(
                """{"tietoryhma":"KUOLINPAIVA","kuollut":true,"kuolinpv":{"arvo":"2024-03-01","tarkkuus":"PAIVA"}}"""
            )
        assertEquals(LocalDate.of(2024, 3, 1), p.dateOfDeath)
    }

    @Test
    fun `ignores KUOLINPAIVA when kuollut is false`() {
        val p =
            personWith(
                """{"tietoryhma":"KUOLINPAIVA","kuollut":false,"kuolinpv":{"arvo":"2024-03-01","tarkkuus":"PAIVA"}}"""
            )
        assertNull(p.dateOfDeath)
    }

    @Test
    fun `drops a guardianship whose period has ended`() {
        val p =
            personWith(
                guardian("010188-916P", alku = "2010-01-01", loppu = "2020-01-01"),
                guardian("020275-9862", alku = "2010-01-01", loppu = "2039-01-01"),
            )
        assertEquals(listOf("020275-9862"), p.guardians.map { it.socialSecurityNumber })
    }

    @Test
    fun `keeps a guardianship with no stated period`() {
        val p = personWith(guardian("010188-916P", alku = null, loppu = null))
        assertEquals(listOf("010188-916P"), p.guardians.map { it.socialSecurityNumber })
    }

    @Test
    fun `drops nationalities marked PASSIIVI`() {
        val p =
            personWith(
                """{"tietoryhma":"KANSALAISUUS","henkilonKansalaisuudet":[
                     {"kansalaisuuskoodi":"246","voimassaolo":"AKTIIVI"},
                     {"kansalaisuuskoodi":"233","voimassaolo":"PASSIIVI"},
                     {"kansalaisuuskoodi":"752"}]}"""
            )
        // no voimassaolo at all is kept: the field is optional in DVV's schema
        assertEquals(listOf("246", "752"), p.nationalities.map { it.countryCode })
    }

    @Test
    fun `tolerates a dependant stub with no name`() {
        // 010188-916P's HUOLLETTAVA entry (child 010121A962W) has no name fields in the sandbox
        val parent = assertNotNull(persons["010188-916P"])
        val dependant = parent.dependants.single()
        assertEquals("010121A962W", dependant.socialSecurityNumber)
        assertEquals("", dependant.firstNames)
        assertEquals("", dependant.lastName)
    }
}
