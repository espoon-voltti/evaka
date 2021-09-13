// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class VardaUnitModelTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `data output is in correct form`() {
        assertEquals(listOf("jm01"), getVardaUnitRequest(testUnit).jarjestamismuoto_koodi)
        assertEquals("tm01", getVardaUnitRequest(testUnit).toimintamuoto_koodi)
        assertEquals(listOf("FI"), getVardaUnitRequest(testUnit).asiointikieli_koodi)
    }

    @Test
    fun `unit types are printed correctly`() {
        assertEquals("tm01", getVardaUnitRequest(testUnitPreschool).toimintamuoto_koodi)
        assertEquals("tm02", getVardaUnitRequest(testUnitFamily).toimintamuoto_koodi)
        assertEquals("tm03", getVardaUnitRequest(testUnitGroupFamily).toimintamuoto_koodi)
        assertEquals("tm02", getVardaUnitRequest(testUnitPreparatoryFamily).toimintamuoto_koodi)
        assertEquals("tm02", getVardaUnitRequest(testUnitPreschoolFamily).toimintamuoto_koodi)
    }

    @Test
    fun `also municipal school should have provider type printed`() {
        assertEquals(listOf("jm01"), getVardaUnitRequest(testUnitMunicipalSchool).jarjestamismuoto_koodi)
    }

    @Test
    fun `non Varda unit types returns null`() {
        assertEquals(null, getVardaUnitRequest(testUnitPreparatory).toimintamuoto_koodi)
    }

    @Test
    fun `json output is correct`() {
        val parsed = mapper.readValue(testUnitJson, VardaUnitRequest::class.java)
        assertEquals(parsed, getVardaUnitRequest(testUnit))
    }
}

val testUnit = VardaUnit(
    vardaUnitId = null,
    ophUnitOid = null,
    evakaDaycareId = null,
    municipalityCode = "049",
    name = "Testipäiväkoti",
    organizer = "http://path.to.organizer",
    address = "Testiosoite 6",
    postalCode = "00200",
    postOffice = "Espoo",
    mailingStreetAddress = "Postiosoite 4",
    mailingPostalCode = "00100",
    mailingPostOffice = "Espoo",
    phoneNumber = "+3581233222",
    email = "testi@testi.com",
    capacity = 21,
    unitProviderType = VardaUnitProviderType.MUNICIPAL,
    unitType = listOf(VardaUnitType.CENTRE),
    language = VardaLanguage.fi,
    languageEmphasisId = null,
    openingDate = "2000-01-01",
    closingDate = null,
    sourceSystem = "ss"
)

val testUnitJson =
    """
{"vakajarjestaja":"http://path.to.organizer","kayntiosoite":"Testiosoite 6","postiosoite":"Postiosoite 4","nimi":"Testipäiväkoti","kayntiosoite_postinumero":"00200","kayntiosoite_postitoimipaikka":"Espoo","postinumero":"00100","postitoimipaikka":"Espoo","kunta_koodi":"049","puhelinnumero":"+3581233222","sahkopostiosoite":"testi@testi.com","kasvatusopillinen_jarjestelma_koodi":"kj98","toimintamuoto_koodi":"tm01","asiointikieli_koodi":["FI"],"jarjestamismuoto_koodi":["jm01"],"varhaiskasvatuspaikat":21,"toiminnallinenpainotus_kytkin":false,"kielipainotus_kytkin":false,"alkamis_pvm":"2000-01-01","lahdejarjestelma":"ss"}
    """.trimIndent()
val testUnitFamily = testUnit.copy(unitType = listOf(VardaUnitType.FAMILY))
val testUnitGroupFamily = testUnit.copy(unitType = listOf(VardaUnitType.GROUP_FAMILY))
val testUnitPreparatoryFamily = testUnit.copy(unitType = listOf(VardaUnitType.PREPARATORY_EDUCATION, VardaUnitType.FAMILY))
val testUnitPreschoolFamily = testUnit.copy(unitType = listOf(VardaUnitType.PRESCHOOL, VardaUnitType.FAMILY))
val testUnitPreparatory = testUnit.copy(unitType = listOf(VardaUnitType.PREPARATORY_EDUCATION))
val testUnitPreschool = testUnit.copy(unitType = listOf(VardaUnitType.PRESCHOOL))
val testUnitMunicipalSchool = testUnit.copy(unitProviderType = VardaUnitProviderType.MUNICIPAL_SCHOOL)
