// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.varda.VardaDecision
import fi.espoo.evaka.varda.VardaFeeData
import fi.espoo.evaka.varda.VardaPersonRequest
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.ServletRequestBindingException
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger {}

@Profile("enable_varda_mock_integration_endpoint")
@RestController
@RequestMapping("/mock-integration/varda/api")
class MockVardaIntegrationEndpoint(private val mapper: ObjectMapper) {
    val decisions = mutableListOf<VardaDecision>()
    val feeData = mutableListOf<VardaFeeData>()

    @GetMapping("/user/apikey/")
    fun getApiKey(): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint GET /users/apiKey called" }
        return ResponseEntity.ok("{\"token\": \"49921df1b823e6fbaeb14dc23fd42325213187ad\"}")
    }

    @PostMapping("/v1/toimipaikat/")
    fun createUnit(
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint POST /toimipaikat received body: $body" }
        return ResponseEntity.ok(getMockUnitResponse())
    }

    @PutMapping("/v1/toimipaikat/{vardaId}/")
    fun updateUnit(
        @PathVariable("vardaId") vardaId: Long?,
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint PUT /toimipaikat/$vardaId recieved body: $body" }
        return ResponseEntity.ok(getMockUnitResponse(vardaId))
    }

    @PutMapping("/v1/vakajarjestajat/{vardaId}")
    fun updateOrganizer(
        @PathVariable("vardaId") vardaId: Long?,
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint PUT /vakajarjestaja/$vardaId recieved body: $body" }
        return ResponseEntity.ok(getMockOrganizerResponse(vardaId))
    }

    @PostMapping("/v1/henkilot/")
    fun createPerson(
        @RequestBody body: VardaPersonRequest,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint POST /henkilot received body: $body" }
        return ResponseEntity.ok(getMockPersonResponse())
    }

    @PostMapping("/v1/lapset/")
    fun createChild(
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint POST /lapset received body: $body" }
        return ResponseEntity.ok(getMockChildResponse())
    }

    @PostMapping("/v1/varhaiskasvatuspaatokset/")
    fun postDecision(
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint POST /varhaiskasvatuspaatokset received body: $body" }
        decisions.add(mapper.readValue(body))
        return ResponseEntity.ok(getMockDecisionResponse())
    }

    @PutMapping("/v1/varhaiskasvatuspaatokset/{vardaId}/")
    fun updateDecision(
        @PathVariable vardaId: Long,
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint PUT /varhaiskasvatuspaatokset/$vardaId/ received body: $body" }
        val newDecision: VardaDecision = mapper.readValue(body)
        // applicationDate is not perfect identifier but it's the best we have for VardaDecision
        decisions.removeAt(decisions.indexOfFirst { decision -> decision.applicationDate == newDecision.applicationDate })
        decisions.add(newDecision)
        return ResponseEntity.ok(getMockDecisionResponse(vardaId))
    }

    @DeleteMapping("/v1/varhaiskasvatuspaatokset/{vardaId}/")
    fun deleteDecision(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<Unit> {
        logger.info { "Mock varda integration endpoint DELETE /varhaiskasvatuspaatokset/$vardaId/ called" }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("v1/lapset/{childId}/varhaiskasvatuspaatokset/")
    fun getDecisions(
        @PathVariable childId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        val result =
            """{
            "results": 
            ${mapper.writeValueAsString(
                decisions.filter {
                    it.childUrl.contains(childId.toString())
                }
            )}
            }
            """.trimIndent()
        return ResponseEntity.ok(result)
    }

    @PostMapping("/v1/varhaiskasvatussuhteet/")
    fun createPlacement(
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint POST /varhaiskasvatussuhteet received body: $body" }
        return ResponseEntity.ok(getMockPlacementResponse())
    }

    @PutMapping("/v1/varhaiskasvatussuhteet/{vardaId}/")
    fun updatePlacement(
        @PathVariable vardaId: Long,
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint PUT /varhaiskasvatussuhteet/$vardaId/ received body: $body" }
        return ResponseEntity.ok(getMockPlacementResponse(vardaId))
    }

    @DeleteMapping("/v1/varhaiskasvatussuhteet/{vardaId}/")
    fun deletePlacement(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<Unit> {
        logger.info { "Mock varda integration endpoint DELETE /varhaiskasvatussuhteet/$vardaId/ called" }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/v1/maksutiedot/")
    fun createFeeData(
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        val feeData = mapper.readValue<VardaFeeData>(body)
        this.feeData.add(feeData)
        logger.info { "Mock varda integration endpoint POST /maksutiedot received body: $body" }
        return ResponseEntity.ok(getMockFeeDataResponse(null, feeData))
    }

    @PutMapping("/v1/maksutiedot/{vardaId}")
    fun updateFeeData(
        @PathVariable vardaId: Long,
        @RequestBody body: String,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint PUT /maksutiedot/$vardaId received body: $body" }
        val newFeeDataEntry = mapper.readValue<VardaFeeData>(body)
        // childUrl is not a perfect identifier but it's the best we have for VardaFeeData
        this.feeData.removeAt(feeData.indexOfFirst { data -> data.childUrl == newFeeDataEntry.childUrl })
        this.feeData.add(newFeeDataEntry)
        return ResponseEntity.ok(getMockFeeDataResponse(vardaId, newFeeDataEntry))
    }

    @DeleteMapping("/v1/maksutiedot/{vardaId}/")
    fun deleteFeeData(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> {
        logger.info { "Mock varda integration endpoint DELETE /maksutiedot received id: $vardaId" }
        return ResponseEntity.noContent().build()
    }

    // Avoid creating a whole spring security setup for just this mock controller but still simulate Varda endpoints
    // requiring authorization to more completely test Varda clients.
    // Usage: add parameter to a mapping: @RequestHeader(name = "Authorization") auth: String
    // -> missing this header will throw ServletRequestBindingException and gets handled here.
    @ExceptionHandler(ServletRequestBindingException::class)
    fun handleServletRequestBindingException(
        req: HttpServletRequest,
        ex: ServletRequestBindingException
    ): ResponseEntity<String> {
        return if (req.getHeader("Authorization").isNullOrBlank()) {
            ResponseEntity.status(403).body("{\"detail\": \"Invalid token.\"}")
        } else {
            ResponseEntity.badRequest().body(ex.message)
        }
    }

    private fun getMockUnitResponse(id: Long? = null): String {
        val responseId = id ?: (100000..999999).random().toLong()
        return """
            {
            "url": "https://varda.api/v1/toimipaikat/$responseId/",
            "id": $responseId,
            "vakajarjestaja": "https://varda.api/v1/vakajarjestajat/298/",
            "organisaatio_oid": "1.2.246.562.10.47181946360",
            "kayntiosoite": "kayntiosoite",
            "postiosoite": "postiosoite",
            "toiminnallisetpainotukset_top": [],
            "kielipainotukset_top": [],
            "ohjaajasuhteet_top": [],
            "varhaiskasvatussuhteet_top": [],
            "lahdejarjestelma": "VARDA",
            "nimi": "Testi",
            "kayntiosoite_postinumero": "00001",
            "kayntiosoite_postitoimipaikka": "postitoimipaikka",
            "postinumero": "00001",
            "postitoimipaikka": "postitoimipaikka",
            "kunta_koodi": "049",
            "puhelinnumero": "+358123456789",
            "sahkopostiosoite": "toimipaikka@example.com",
            "kasvatusopillinen_jarjestelma_koodi": "kj01",
            "toimintamuoto_koodi": "tm01",
            "asiointikieli_koodi": ["FI"],
            "jarjestamismuoto_koodi": ["jm01"],
            "varhaiskasvatuspaikat": 1,
            "toiminnallinenpainotus_kytkin": false,
            "kielipainotus_kytkin": false,
            "alkamis_pvm": "2019-01-01",
            "paattymis_pvm": null,
            "muutos_pvm": "2019-11-04T11:31:36.218854Z"
            }
        """.trimIndent()
    }

    private fun getMockOrganizerResponse(id: Long? = null): String {
        return """
            {
              "url": "http://localhost:8888/mock-integration/varda/api/v1/vakajarjestajat/298/",
              "id": ${id ?: (100000..999999).random().toLong()},
              "nimi": "Espoon kaupunki",
              "y_tunnus": "0101263-6",
              "yritysmuoto": "KUNTA",
              "kunnallinen_kytkin": true,
              "organisaatio_oid": "1.2.246.562.10.90008375488",
              "kunta_koodi": "049",
              "kayntiosoite": "Brogatan 11",
              "kayntiosoite_postinumero": "02770",
              "kayntiosoite_postitoimipaikka": "ESBO",
              "postiosoite": "PL 661",
              "postinumero": "02070",
              "postitoimipaikka": "ESPOON KAUPUNKI",
              "alkamis_pvm": "1978-03-15",
              "paattymis_pvm": null,
              "muutos_pvm": "2019-12-18 14:31:07.350419+00:00",
              "toimipaikat_top": [],
              "sahkopostiosoite": "sposti@email.moi",
              "tilinumero": "",
              "ipv4_osoitteet": null,
              "ipv6_osoitteet": null,
              "puhelinnumero": "+358400132233"
            }
        """.trimIndent()
    }

    private fun getMockChildResponse(id: Long? = null): String {
        val responseId = id ?: (100000..999999).random().toLong()
        return """
            {
              "url": "https://backend-qa.varda-db.csc.fi/api/v1/lapset/$responseId/",
              "id": $responseId,
              "henkilo": "https://backend-qa.varda-db.csc.fi/api/v1/henkilot/687426/",
              "vakatoimija": "https://backend-qa.varda-db.csc.fi/api/v1/vakajarjestajat/299/",
              "oma_organisaatio": null,
              "oma_organisaatio_oid": null,
              "paos_organisaatio": null,
              "paos_organisaatio_oid": null,
              "paos_kytkin": false,
              "varhaiskasvatuspaatokset_top": [],
              "muutos_pvm": "2020-04-15T08:22:02.364895Z"
            }
        """.trimIndent()
    }

    private fun getMockPersonResponse(id: Long? = null): String {
        val responseId = id ?: (100000..999999).random().toLong()
        return """
            {
              "url": "http://localhost:8888/mock-integration/varda/api/v1/henkilot/$responseId/",
              "id": $responseId,
              "etunimet": "Testaaja Tessa",
              "kutsumanimi": "Testaaja",
              "sukunimi": "Holopainen",
              "henkilo_oid": "1.2.246.562.24.$responseId",
              "syntyma_pvm": null,
              "lapsi": []
            }
        """.trimIndent()
    }

    private fun getMockFeeDataResponse(id: Long? = null, feeData: VardaFeeData): String {
        val responseId = id ?: (100000..999999).random().toLong()
        return """
        {
          "url": "https://backend-qa.varda-db.csc.fi/api/v1/maksutiedot/$responseId/",
          "id": $responseId,
          "huoltajat": [
            {
              "henkilo_oid": "1.2.246.562.24.23736347564",
              "etunimet": "Saara",
              "sukunimi": "Saaranen"
            }
          ],
          "lapsi": "https://backend-qa.varda-db.csc.fi/api/v1/lapset/292149/",
          "maksun_peruste_koodi": "${feeData.feeCode}",
          "palveluseteli_arvo": "${feeData.voucherAmount}",
          "asiakasmaksu": "${feeData.feeAmount}",
          "perheen_koko": ${feeData.familySize},
          "alkamis_pvm": "${feeData.startDate}",
          "paattymis_pvm": "${feeData.endDate}",
          "tallennetut_huoltajat_count": 1,
          "ei_tallennetut_huoltajat_count": 0
        }
        """.trimIndent()
    }

    private fun getMockDecisionResponse(id: Long? = null): String {
        return """
{
    "id": ${id ?: (100000..999999).random().toLong()}
}
        """.trimIndent()
    }

    private fun getMockPlacementResponse(id: Long? = null): String {
        return """
{
    "id": ${id ?: (100000..999999).random().toLong()}
}
        """.trimIndent()
    }
}
