// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.varda.VardaChildRequest
import fi.espoo.evaka.varda.VardaDecision
import fi.espoo.evaka.varda.VardaFeeData
import fi.espoo.evaka.varda.VardaPersonRequest
import fi.espoo.evaka.varda.VardaPlacement
import fi.espoo.evaka.varda.VardaUnitRequest
import fi.espoo.evaka.varda.VardaUpdateOrganizer
import mu.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = KotlinLogging.logger {}

@Profile("enable_varda_mock_integration_endpoint")
@RestController
@RequestMapping("/mock-integration/varda/api")
class MockVardaIntegrationEndpoint(private val mapper: ObjectMapper) {
    private val lock = ReentrantLock()

    var organizerId = 0L
    var unitId = 0L
    val units = mutableMapOf<Long, VardaUnitRequest>()
    var personId = 0L
    val people = mutableMapOf<Long, VardaPersonRequest>()
    var decisionId = 0L
    val decisions = mutableMapOf<Long, VardaDecision>()
    var placementId = 0L
    val placements = mutableMapOf<Long, VardaPlacement>()
    var feeDataId = 0L
    val feeData = mutableMapOf<Long, VardaFeeData>()
    var childId = 0L
    val children = mutableMapOf<Long, VardaChildRequest>()

    fun cleanUp() {
        lock.withLock {
            organizerId = 0L
            unitId = 0L
            units.clear()
            personId = 0L
            people.clear()
            decisionId = 0L
            decisions.clear()
            placementId = 0L
            placements.clear()
            feeDataId = 0L
            feeData.clear()
            childId = 0L
            children.clear()
        }
    }

    @GetMapping("/user/apikey/")
    fun getApiKey(): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint GET /users/apiKey called" }
        ResponseEntity.ok("{\"token\": \"49921df1b823e6fbaeb14dc23fd42325213187ad\"}")
    }

    @PostMapping("/v1/toimipaikat/")
    fun createUnit(
        @RequestBody unit: VardaUnitRequest,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint POST /toimipaikat received body: $unit" }
        unitId = unitId.inc()
        units.put(unitId, unit)
        ResponseEntity.ok(getMockUnitResponse(unitId))
    }

    @PutMapping("/v1/toimipaikat/{vardaId}/")
    fun updateUnit(
        @PathVariable("vardaId") vardaId: Long?,
        @RequestBody unit: VardaUnitRequest,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint PUT /toimipaikat/$vardaId recieved body: $unit" }
        val id = if (vardaId == null) {
            unitId = unitId.inc()
            unitId
        } else {
            vardaId
        }
        units.replace(id, unit)
        ResponseEntity.ok(getMockUnitResponse(id))
    }

    @PutMapping("/v1/vakajarjestajat/{vardaId}")
    fun updateOrganizer(
        @PathVariable("vardaId") vardaId: Long?,
        @RequestBody body: VardaUpdateOrganizer,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint PUT /vakajarjestaja/$vardaId recieved body: $body" }
        val id = if (vardaId == null) {
            organizerId = organizerId.inc()
            organizerId
        } else {
            vardaId
        }
        ResponseEntity.ok(getMockOrganizerResponse(id))
    }

    @PostMapping("/v1/henkilot/")
    fun createPerson(
        @RequestBody body: VardaPersonRequest,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint POST /henkilot received body: $body" }
        personId = personId.inc()
        people.put(personId, body)
        ResponseEntity.ok(getMockPersonResponse(personId))
    }

    @PostMapping("/v1/lapset/")
    fun createChild(
        @RequestBody body: VardaChildRequest,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint POST /lapset received body: $body" }
        childId = childId.inc()
        this.children.put(childId, body)
        ResponseEntity.ok(getMockChildResponse(childId))
    }

    @PostMapping("/v1/varhaiskasvatuspaatokset/")
    fun postDecision(
        @RequestBody body: VardaDecision,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint POST /varhaiskasvatuspaatokset received body: $body" }
        decisionId = decisionId.inc()
        decisions.put(decisionId, body)
        ResponseEntity.ok(getMockDecisionResponse(decisionId))
    }

    @PutMapping("/v1/varhaiskasvatuspaatokset/{vardaId}/")
    fun updateDecision(
        @PathVariable vardaId: Long,
        @RequestBody body: VardaDecision,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint PUT /varhaiskasvatuspaatokset/$vardaId/ received body: $body" }
        decisions.replace(vardaId, body)
        ResponseEntity.ok(getMockDecisionResponse(vardaId))
    }

    @DeleteMapping("/v1/varhaiskasvatuspaatokset/{vardaId}/")
    fun deleteDecision(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<Unit> = lock.withLock {
        logger.info { "Mock varda integration endpoint DELETE /varhaiskasvatuspaatokset/$vardaId/ called" }
        decisions.remove(vardaId)
        ResponseEntity.noContent().build()
    }

    @PostMapping("/v1/varhaiskasvatussuhteet/")
    fun createPlacement(
        @RequestBody body: VardaPlacement,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint POST /varhaiskasvatussuhteet received body: $body" }
        placementId = placementId.inc()
        placements.put(placementId, body)
        ResponseEntity.ok(getMockPlacementResponse(placementId))
    }

    @PutMapping("/v1/varhaiskasvatussuhteet/{vardaId}/")
    fun updatePlacement(
        @PathVariable vardaId: Long,
        @RequestBody body: VardaPlacement,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint PUT /varhaiskasvatussuhteet/$vardaId/ received body: $body" }
        placements.replace(vardaId, body)
        ResponseEntity.ok(getMockPlacementResponse(vardaId))
    }

    @DeleteMapping("/v1/varhaiskasvatussuhteet/{vardaId}/")
    fun deletePlacement(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<Unit> = lock.withLock {
        logger.info { "Mock varda integration endpoint DELETE /varhaiskasvatussuhteet/$vardaId/ called" }
        placements.remove(vardaId)
        ResponseEntity.noContent().build()
    }

    @PostMapping("/v1/maksutiedot/")
    fun createFeeData(
        @RequestBody body: VardaFeeData,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        this.feeDataId = feeDataId.inc()
        this.feeData.put(feeDataId, body)
        logger.info { "Mock varda integration endpoint POST /maksutiedot received body: $body" }
        ResponseEntity.ok(getMockFeeDataResponse(feeDataId, body))
    }

    @PutMapping("/v1/maksutiedot/{vardaId}")
    fun updateFeeData(
        @PathVariable vardaId: Long,
        @RequestBody body: VardaFeeData,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint PUT /maksutiedot/$vardaId received body: $body" }
        this.feeData.replace(vardaId, body)
        ResponseEntity.ok(getMockFeeDataResponse(vardaId, body))
    }

    @DeleteMapping("/v1/maksutiedot/{vardaId}/")
    fun deleteFeeData(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint DELETE /maksutiedot received id: $vardaId" }
        this.feeData.remove(vardaId)
        ResponseEntity.noContent().build()
    }

    @DeleteMapping("/v1/lapset/{vardaId}/")
    fun deleteChild(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String
    ): ResponseEntity<String> = lock.withLock {
        logger.info { "Mock varda integration endpoint DELETE /lapset received id: $vardaId" }
        this.children.remove(vardaId)
        ResponseEntity.noContent().build()
    }

    @GetMapping("/v1/lapset/{childId}/varhaiskasvatuspaatokset/")
    fun getChildDecisions(@PathVariable childId: Long): ResponseEntity<VardaClient.PaginatedResponse<VardaClient.DecisionPeriod>> =
        lock.withLock {
            logger.info { "Mock varda integration endpoint GET /lapset/$childId/varhaiskasvatuspaatokset received id: $childId" }
            val childDecisions = decisions.entries.filter { (_, decision) ->
                decision.childUrl.contains("/lapset/$childId/")
            }
            ResponseEntity.ok(
                VardaClient.PaginatedResponse(
                    count = childDecisions.size,
                    next = null,
                    previous = null,
                    results = childDecisions.map { (vardaId, decision) ->
                        VardaClient.DecisionPeriod(vardaId, decision.startDate, decision.endDate)
                    }
                )
            )
        }

    private fun getMockUnitResponse(id: Long): String {
        return """
            {
            "url": "https://varda.api/v1/toimipaikat/$id/",
            "id": $id,
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

    private fun getMockOrganizerResponse(id: Long): String {
        return """
            {
              "url": "http://localhost:8888/mock-integration/varda/api/v1/vakajarjestajat/$id/",
              "id": $id,
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

    private fun getMockChildResponse(id: Long): String {
        return """
            {
              "url": "https://backend-qa.varda-db.csc.fi/api/v1/lapset/$id/",
              "id": $id,
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

    private fun getMockPersonResponse(id: Long): String {
        return """
            {
              "url": "http://localhost:8888/mock-integration/varda/api/v1/henkilot/$id/",
              "id": $id,
              "etunimet": "Testaaja Tessa",
              "kutsumanimi": "Testaaja",
              "sukunimi": "Holopainen",
              "henkilo_oid": "1.2.246.562.24.$id",
              "syntyma_pvm": null,
              "lapsi": []
            }
        """.trimIndent()
    }

    private fun getMockFeeDataResponse(id: Long, feeData: VardaFeeData): String {
        return """
        {
          "url": "https://backend-qa.varda-db.csc.fi/api/v1/maksutiedot/$id/",
          "id": $id,
          "huoltajat": [
            {
              "henkilo_oid": "1.2.246.562.24.23736347564",
              "etunimet": "Saara",
              "sukunimi": "Saaranen"
            }
          ],
          "lapsi": "https://backend-qa.varda-db.csc.fi/api/v1/lapset/292149/",
          "maksun_peruste_koodi": "${feeData.maksun_peruste_koodi}",
          "palveluseteli_arvo": "${feeData.palveluseteli_arvo}",
          "asiakasmaksu": "${feeData.asiakasmaksu}",
          "perheen_koko": ${feeData.perheen_koko},
          "alkamis_pvm": "${feeData.alkamis_pvm}",
          "paattymis_pvm": "${feeData.paattymis_pvm}",
          "tallennetut_huoltajat_count": ${feeData.huoltajat.size},
          "ei_tallennetut_huoltajat_count": 0
        }
        """.trimIndent()
    }

    private fun getMockDecisionResponse(id: Long): String {
        return """
{
    "id": $id
}
        """.trimIndent()
    }

    private fun getMockPlacementResponse(id: Long): String {
        return """
{
    "id": $id
}
        """.trimIndent()
    }
}
