// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class VardaUnitModelTest {
    private val mapper = defaultJsonMapperBuilder().build()

    @Test
    fun `data output is in correct form`() {
        assertEquals(listOf("jm01"), toRequest(testUnit).jarjestamismuoto_koodi)
        assertEquals("tm01", toRequest(testUnit).toimintamuoto_koodi)
        assertEquals(listOf("FI"), toRequest(testUnit).toimintakieli_koodi)
    }

    @Test
    fun `unit types are printed correctly`() {
        assertEquals("tm01", toRequest(testUnitPreschool).toimintamuoto_koodi)
        assertEquals("tm02", toRequest(testUnitFamily).toimintamuoto_koodi)
        assertEquals("tm03", toRequest(testUnitGroupFamily).toimintamuoto_koodi)
        assertEquals("tm02", toRequest(testUnitPreparatoryFamily).toimintamuoto_koodi)
        assertEquals("tm02", toRequest(testUnitPreschoolFamily).toimintamuoto_koodi)
    }

    @Test
    fun `also municipal school should have provider type printed`() {
        assertEquals(listOf("jm01"), toRequest(testUnitMunicipalSchool).jarjestamismuoto_koodi)
    }

    @Test
    fun `non Varda unit types returns null`() {
        assertEquals(null, toRequest(testUnitPreparatory).toimintamuoto_koodi)
    }

    @Test
    fun `json output is correct`() {
        val parsed = mapper.readValue(testUnitJson, VardaUnitRequest::class.java)
        assertEquals(parsed, toRequest(testUnit))
    }
}

val testUnit =
    VardaUnit(
        evakaDaycareId = DaycareId(UUID.randomUUID()),
        name = "Testipäiväkoti",
        streetAddress = "Testiosoite 6",
        postalCode = "00200",
        postOffice = "Espoo",
        mailingPoBox = "Postiosoite 4",
        mailingPostalCode = "00100",
        mailingPostOffice = "Espoo",
        unitManagerPhone = "+3581233222",
        unitManagerEmail = "testi@testi.com",
        capacity = 21,
        providerType = VardaUnitProviderType.MUNICIPAL,
        type = listOf(VardaUnitType.CENTRE),
        language = VardaLanguage.FI,
        languageEmphasisId = null,
        openingDate = LocalDate.of(2000, 1, 1),
        closingDate = null
    )

fun toRequest(unit: VardaUnit) =
    unit.toVardaUnitRequest(
        lahdejarjestelma = "ss",
        kuntakoodi = "049",
        vakajarjestaja = "http://path.to.organizer"
    )

val testUnitJson =
    """
{"vakajarjestaja":"http://path.to.organizer","kayntiosoite":"Testiosoite 6","postiosoite":"Postiosoite 4","nimi":"Testipäiväkoti","kayntiosoite_postinumero":"00200","kayntiosoite_postitoimipaikka":"Espoo","postinumero":"00100","postitoimipaikka":"Espoo","kunta_koodi":"049","puhelinnumero":"+3581233222","sahkopostiosoite":"testi@testi.com","kasvatusopillinen_jarjestelma_koodi":"kj98","toimintamuoto_koodi":"tm01","toimintakieli_koodi":["FI"],"jarjestamismuoto_koodi":["jm01"],"varhaiskasvatuspaikat":21,"toiminnallinenpainotus_kytkin":false,"kielipainotus_kytkin":false,"alkamis_pvm":"2000-01-01","lahdejarjestelma":"ss"}
    """
        .trimIndent()
val testUnitFamily = testUnit.copy(type = listOf(VardaUnitType.FAMILY))
val testUnitGroupFamily = testUnit.copy(type = listOf(VardaUnitType.GROUP_FAMILY))
val testUnitPreparatoryFamily =
    testUnit.copy(type = listOf(VardaUnitType.PREPARATORY_EDUCATION, VardaUnitType.FAMILY))
val testUnitPreschoolFamily =
    testUnit.copy(type = listOf(VardaUnitType.PRESCHOOL, VardaUnitType.FAMILY))
val testUnitPreparatory = testUnit.copy(type = listOf(VardaUnitType.PREPARATORY_EDUCATION))
val testUnitPreschool = testUnit.copy(type = listOf(VardaUnitType.PRESCHOOL))
val testUnitMunicipalSchool = testUnit.copy(providerType = VardaUnitProviderType.MUNICIPAL_SCHOOL)
