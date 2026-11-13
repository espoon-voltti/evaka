// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import evaka.core.shared.config.defaultJsonMapperBuilder
import evaka.core.vtjclient.dto.VtjPerson
import evaka.core.vtjclient.mapper.mapToVtjPerson
import evaka.core.vtjclient.soap.VTJHenkiloVastaussanoma
import jakarta.xml.bind.JAXBContext
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import tools.jackson.databind.json.JsonMapper

/**
 * Executes **both** sides of the SOAP → REST migration comparison against real captured payloads: a
 * VTJkysely `PERUSSANOMA 3` response through [mapToVtjPerson], and a DVV muutosrajapinta
 * `/perustiedot` response through [toVtjPerson].
 *
 * The two fixtures are **different people from different DVV test populations** — Test VTJ and
 * hiekkalaatikko share no synthetic persons — so this can only assert structural correspondence,
 * never equal values. Same-person value parity is impossible in any test environment and belongs to
 * a dual-read comparison during rollout.
 */
class DvvSoapRestParityTest {
    private val today = LocalDate.of(2026, 7, 22)

    /** VTJkysely PERUSSANOMA 3, captured 2019-06-20: 020501A999T, resident of Vaasa. */
    private val soap: VtjPerson by lazy {
        val stream =
            checkNotNull(
                javaClass.getResourceAsStream("/__files/person-client/perussanoma3-response-ok.xml")
            ) {
                "SOAP fixture not found"
            }
        val sanoma = stream.use {
            JAXBContext.newInstance(VTJHenkiloVastaussanoma::class.java)
                .createUnmarshaller()
                .unmarshal(it) as VTJHenkiloVastaussanoma
        }
        sanoma.asiakasinfoOrPaluukoodiOrHakuperusteet
            .filterIsInstance<VTJHenkiloVastaussanoma.Henkilo>()
            .single()
            .mapToVtjPerson()
    }

    /** muutosrajapinta /perustiedot, mutpT1x: 010192-921W, resident of Helsinki. */
    private val rest: VtjPerson by lazy {
        val jsonMapper: JsonMapper = defaultJsonMapperBuilder().build()
        val stream =
            checkNotNull(
                javaClass.getResourceAsStream(
                    "/__files/perustiedot-client/perustiedot-response-ok.json"
                )
            ) {
                "REST fixture not found"
            }
        stream
            .use { jsonMapper.readValue(it, DvvPerustiedotResponse::class.java) }
            .perustiedot
            .single { it.henkilotunnus == "010192-921W" }
            .toVtjPerson(today)
    }

    @Test
    fun `both interfaces populate every field eVaka persists from VTJ`() {
        listOf("SOAP" to soap, "REST" to rest).forEach { (label, p) ->
            assertTrue(p.firstNames.isNotBlank(), "$label firstNames")
            assertTrue(p.lastName.isNotBlank(), "$label lastName")
            assertTrue(p.socialSecurityNumber.isNotBlank(), "$label ssn")
            assertNotNull(p.address, "$label address")
            assertTrue(!p.residenceCode.isNullOrBlank(), "$label residenceCode")
            assertTrue(p.nationalities.isNotEmpty(), "$label nationalities")
            assertTrue(!p.nativeLanguage?.code.isNullOrBlank(), "$label nativeLanguage.code")
            assertNotNull(p.restrictedDetails, "$label restrictedDetails")
            assertTrue(!p.municipalityOfResidence.isNullOrBlank(), "$label municipalityOfResidence")
        }
    }

    @Test
    fun `street address carries the apartment on both interfaces`() {
        // SOAP delivers one pre-joined string; REST splits it and the mapper re-assembles. Guards
        // against a mapping that joins only katunimi+katunumero.
        assertEquals("Kauppa Puistikko 6 B 23", soap.address?.streetAddress)
        assertEquals("Handels Esplanaden 6 B 23", soap.address?.streetAddressSe)
        assertEquals("Mikkolantie 30 F 028", rest.address?.streetAddress)
        assertEquals("Mickelsvägen 30 F 028", rest.address?.streetAddressSe)
    }

    @Test
    fun `both interfaces preserve DVV's trailing whitespace in the residence code`() {
        assertEquals("90000009871B023 ", soap.residenceCode)
        assertEquals("900000041C1F028 ", rest.residenceCode)
    }

    @Test
    fun `municipality is a plaintext name over SOAP but a kuntakoodi over REST`() {
        assertEquals("Vaasa", soap.municipalityOfResidence)
        assertEquals("091", rest.municipalityOfResidence)
    }

    @Test
    fun `the swedish municipality name has no REST source`() {
        assertEquals("Vasa", soap.municipalityOfResidenceSe)
        assertNull(rest.municipalityOfResidenceSe)
    }

    @Test
    fun `SOAP also carries the kuntakoodi, which its mapper discards`() {
        // Storing the code rather than the name was therefore always available on the SOAP path —
        // it is not something REST forces.
        val henkilo =
            checkNotNull(
                    javaClass.getResourceAsStream(
                        "/__files/person-client/perussanoma3-response-ok.xml"
                    )
                )
                .use {
                    JAXBContext.newInstance(VTJHenkiloVastaussanoma::class.java)
                        .createUnmarshaller()
                        .unmarshal(it) as VTJHenkiloVastaussanoma
                }
                .asiakasinfoOrPaluukoodiOrHakuperusteet
                .filterIsInstance<VTJHenkiloVastaussanoma.Henkilo>()
                .single()

        assertEquals("905", henkilo.kotikunta.kuntanumero)
        assertEquals("Vaasa", soap.municipalityOfResidence)
    }

    @Test
    fun `plaintext names accompany the codes over SOAP but not over REST`() {
        // Both are write-only in eVaka (persistence uses .code / .countryCode), so this difference
        // is inert.
        assertEquals("suomi", soap.nativeLanguage?.languageName)
        assertEquals("", rest.nativeLanguage?.languageName)
        assertEquals("Suomi", soap.nationalities.single().countryName)
        assertEquals("", rest.nationalities.single().countryName)
        assertEquals(
            soap.nationalities.single().countryCode,
            rest.nationalities.single().countryCode,
        )
    }
}
