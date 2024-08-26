// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.varda.VardaChildRequest
import fi.espoo.evaka.varda.VardaDecision
import fi.espoo.evaka.varda.VardaFeeData
import fi.espoo.evaka.varda.VardaPersonRequest
import fi.espoo.evaka.varda.VardaPlacement
import fi.espoo.evaka.varda.VardaUnitRequest
import java.time.LocalDate
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
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

private val logger = KotlinLogging.logger {}

@Profile("enable_varda_mock_integration_endpoint")
@RestController
@RequestMapping("/mock-integration/varda/api")
class MockVardaIntegrationEndpoint {
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
    var feeDataCalls = 0
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
            feeDataCalls = 0
            feeData.clear()
            childId = 0L
            children.clear()
        }
    }

    @GetMapping("/user/apikey/")
    fun getApiKey(): ByteArray =
        lock.withLock {
            logger.info { "Mock varda integration endpoint GET /users/apiKey called" }
            "{\"token\": \"49921df1b823e6fbaeb14dc23fd42325213187ad\"}".toByteArray()
        }

    @PostMapping("/v1/toimipaikat/")
    fun createUnit(
        @RequestBody unit: VardaUnitRequest,
        @RequestHeader(name = "Authorization") auth: String,
    ): ByteArray =
        lock.withLock {
            logger.info { "Mock varda integration endpoint POST /toimipaikat received body: $unit" }
            unitId = unitId.inc()
            units[unitId] = unit
            getMockUnitResponse(unitId).toByteArray()
        }

    @PutMapping("/v1/toimipaikat/{vardaId}/")
    fun updateUnit(
        @PathVariable vardaId: Long?,
        @RequestBody unit: VardaUnitRequest,
        @RequestHeader(name = "Authorization") auth: String,
    ): ByteArray =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint PUT /toimipaikat/$vardaId received body: $unit"
            }
            val id =
                if (vardaId == null) {
                    unitId = unitId.inc()
                    unitId
                } else {
                    vardaId
                }
            units.replace(id, unit)
            getMockUnitResponse(id).toByteArray()
        }

    @PostMapping("/v1/henkilot/")
    fun createPerson(
        @RequestBody body: VardaPersonRequest,
        @RequestHeader(name = "Authorization") auth: String,
    ): ByteArray =
        lock.withLock {
            logger.info { "Mock varda integration endpoint POST /henkilot received body: $body" }
            personId = personId.inc()
            people[personId] = body
            getMockPersonResponse(personId).toByteArray()
        }

    @PostMapping("/v1/lapset/")
    fun createChild(
        @RequestBody body: VardaChildRequest,
        @RequestHeader(name = "Authorization") auth: String,
    ): ByteArray =
        lock.withLock {
            logger.info { "Mock varda integration endpoint POST /lapset received body: $body" }
            childId = childId.inc()
            this.children[childId] = body
            getMockChildResponse(childId).toByteArray()
        }

    @PostMapping("/v1/varhaiskasvatuspaatokset/")
    fun createDecision(
        @RequestBody body: VardaDecision,
        @RequestHeader(name = "Authorization") auth: String,
    ): ResponseEntity<ByteArray> =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint POST /varhaiskasvatuspaatokset received body: $body"
            }

            if (shouldFailRequest(VardaCallType.DECISION)) return failRequest()

            decisionId = decisionId.inc()
            decisions[decisionId] = body
            ResponseEntity.ok(getMockDecisionResponse(decisionId).toByteArray())
        }

    @PutMapping("/v1/varhaiskasvatuspaatokset/{vardaId}/")
    fun updateDecision(
        @PathVariable vardaId: Long,
        @RequestBody body: VardaDecision,
        @RequestHeader(name = "Authorization") auth: String,
    ): ResponseEntity<ByteArray> =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint PUT /varhaiskasvatuspaatokset/$vardaId/ received body: $body"
            }

            if (shouldFailRequest(VardaCallType.DECISION)) return failRequest()

            decisions.replace(vardaId, body)
            ResponseEntity.ok(getMockDecisionResponse(vardaId).toByteArray())
        }

    @DeleteMapping("/v1/varhaiskasvatuspaatokset/{vardaId}/")
    fun deleteDecision(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String,
    ) =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint DELETE /varhaiskasvatuspaatokset/$vardaId/ called"
            }
            decisions.remove(vardaId)
        }

    @PostMapping("/v1/varhaiskasvatussuhteet/")
    fun createPlacement(
        @RequestBody body: VardaPlacement,
        @RequestHeader(name = "Authorization") auth: String,
    ): ByteArray =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint POST /varhaiskasvatussuhteet received body: $body"
            }
            placementId = placementId.inc()
            placements[placementId] = body
            getMockPlacementResponse(placementId).toByteArray()
        }

    @PutMapping("/v1/varhaiskasvatussuhteet/{vardaId}/")
    fun updatePlacement(
        @PathVariable vardaId: Long,
        @RequestBody body: VardaPlacement,
        @RequestHeader(name = "Authorization") auth: String,
    ): ByteArray =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint PUT /varhaiskasvatussuhteet/$vardaId/ received body: $body"
            }
            placements.replace(vardaId, body)
            getMockPlacementResponse(vardaId).toByteArray()
        }

    @DeleteMapping("/v1/varhaiskasvatussuhteet/{vardaId}/")
    fun deletePlacement(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String,
    ) =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint DELETE /varhaiskasvatussuhteet/$vardaId/ called"
            }
            placements.remove(vardaId)
        }

    @PostMapping("/v1/maksutiedot/")
    fun createFeeData(
        @RequestBody body: VardaFeeData,
        @RequestHeader(name = "Authorization") auth: String,
    ): ResponseEntity<ByteArray> =
        lock.withLock {
            if (shouldFailRequest(VardaCallType.FEE_DATA)) return failRequest()

            this.feeDataId = feeDataId.inc()
            this.feeData[feeDataId] = body
            this.feeDataCalls += 1
            logger.info { "Mock varda integration endpoint POST /maksutiedot received body: $body" }
            ResponseEntity.ok(getMockFeeDataResponse(feeDataId, body).toByteArray())
        }

    @PutMapping("/v1/maksutiedot/{vardaId}")
    fun updateFeeData(
        @PathVariable vardaId: Long,
        @RequestBody body: VardaFeeData,
        @RequestHeader(name = "Authorization") auth: String,
    ): ResponseEntity<ByteArray> =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint PUT /maksutiedot/$vardaId received body: $body"
            }

            if (shouldFailRequest(VardaCallType.FEE_DATA)) return failRequest()

            this.feeData.replace(vardaId, body)
            ResponseEntity.ok(getMockFeeDataResponse(vardaId, body).toByteArray())
        }

    @DeleteMapping("/v1/maksutiedot/{vardaId}/")
    fun deleteFeeData(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String,
    ): ResponseEntity<ByteArray> =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint DELETE /maksutiedot received id: $vardaId"
            }

            if (shouldFailRequest(VardaCallType.FEE_DATA)) return failRequest()

            this.feeData.remove(vardaId)
            ResponseEntity.noContent().build()
        }

    @DeleteMapping("/v1/lapset/{vardaId}/")
    fun deleteChild(
        @PathVariable vardaId: Long,
        @RequestHeader(name = "Authorization") auth: String,
    ) {
        lock.withLock {
            logger.info { "Mock varda integration endpoint DELETE /lapset received id: $vardaId" }
            this.children.remove(vardaId)
        }
    }

    private fun vardaIdFromVardaUrl(childUrl: String) = childUrl.split("/").reversed()[1].toLong()

    @DeleteMapping("/v1/lapset/{vardaChildId}/delete-all/")
    fun deleteAllChild(
        @PathVariable vardaChildId: Long,
        @RequestHeader(name = "Authorization") auth: String,
    ) {
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint DELETE ALL /lapset received id: $vardaChildId"
            }
            val feeDataKeysToRemove =
                this.feeData.filter { vardaIdFromVardaUrl(it.value.lapsi) == vardaChildId }.keys
            val decisionKeysToRemove =
                this.decisions.filter { vardaIdFromVardaUrl(it.value.lapsi) == vardaChildId }.keys
            val placementKeysToRemove =
                this.placements
                    .filter {
                        decisionKeysToRemove.contains(
                            vardaIdFromVardaUrl(it.value.varhaiskasvatuspaatos)
                        )
                    }
                    .keys
            logger.info(
                "Mock varda integration endpoint DELETE ALL deleting ${feeDataKeysToRemove.size} fee data, ${decisionKeysToRemove.size} decisions, ${placementKeysToRemove.size}"
            )
            feeDataKeysToRemove.forEach { this.feeData.remove(it) }
            decisionKeysToRemove.forEach { this.decisions.remove(it) }
            placementKeysToRemove.forEach { this.placements.remove(it) }
            this.children.remove(vardaChildId)
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DecisionPeriod(
        val id: Long,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate,
    )

    @GetMapping("/v1/lapset/{childId}/varhaiskasvatuspaatokset/")
    fun getChildDecisions(
        @PathVariable childId: Long
    ): VardaClient.PaginatedResponse<DecisionPeriod> =
        lock.withLock {
            logger.info {
                "Mock varda integration endpoint GET /lapset/$childId/varhaiskasvatuspaatokset received id: $childId"
            }
            val childDecisions =
                decisions.entries.filter { (_, decision) ->
                    decision.lapsi.contains("/lapset/$childId/")
                }
            VardaClient.PaginatedResponse(
                count = childDecisions.size,
                next = null,
                previous = null,
                results =
                    childDecisions.map { (vardaId, decision) ->
                        DecisionPeriod(vardaId, decision.alkamis_pvm, decision.paattymis_pvm)
                    },
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
        """
            .trimIndent()
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
        """
            .trimIndent()
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
        """
            .trimIndent()
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
        """
            .trimIndent()
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
        """
            .trimIndent()
    }

    fun getMockErrorResponseForFeeData() =
        """
        {
           "huoltajat":[
              {
                 "error_code":"MA003",
                 "description":"No matching huoltaja found.",
                 "translations":[
                    {
                       "language":"FI",
                       "description":"Väestötietojärjestelmästä ei löydy lapsen ilmoitettua huoltajaa."
                    },
                    {
                       "language":"SV",
                       "description":"Det gick inte att hitta vårdnadshavaren till barnet i Befolkningsdatasystemet."
                    }
                 ]
              }
           ]
        }
        """

    fun getMockErrorResponses() =
        mapOf(
            "DY004" to
                """{"tuntimaara_viikossa":[{"error_code":"DY004","description":"Ensure this value is greater than or equal to 1.","translations":[{"language":"SV","description":"Värdet ska vara större eller lika stort som {{}}."},{"language":"FI","description":"Arvon pitää olla suurempi tai yhtäsuuri kuin {{}}."}]}]}""",
            "VS009" to
                """{"errors":[{"error_code":"VS009","description":"Varhaiskasvatussuhde alkamis_pvm cannot be after Toimipaikka paattymis_pvm.","translations":[{"language":"FI","description":"Varhaiskasvatussuhteen alkamispäivämäärä ei voi olla toimipaikan päättymispäivämäärän jälkeen."},{"language":"SV","description":"Begynnelsedatumet för deltagande i småbarnspedagogik kan inte vara senare än verksamhetsställets slutdatum."}]}]}""",
            "MA003" to
                """{"huoltajat":[{"error_code":"MA003","description":"No matching huoltaja found.","translations":[{"language":"FI","description":"Väestötietojärjestelmästä ei löydy lapsen ilmoitettua huoltajaa."},{"language":"SV","description":"Det gick inte att hitta vårdnadshavaren till barnet i Befolkningsdatasystemet."}]}]}""",
            "HE012" to
                """{"huoltajat":{"1":{"etunimet":[{"error_code":"HE012","description":"Name has disallowed characters.","translations":[{"language":"SV","description":"Namnet innehåller förbjudna tecken. "},{"language":"FI","description":"Nimessä on merkkejä, jotka eivät ole sallittuja."}]}]}}}""",
        )

    private fun getMockDecisionResponse(id: Long): String {
        return """
{
    "id": $id
}
        """
            .trimIndent()
    }

    private fun getMockPlacementResponse(id: Long): String {
        return """
{
    "id": $id
}
        """
            .trimIndent()
    }

    enum class VardaCallType {
        DECISION,
        FEE_DATA,
    }

    private var failNextRequest: VardaCallType? = null
    private var failResponseCode = 200
    private var failResponseMessage = ""

    fun failNextVardaCall(failWithHttpCode: Int, ofType: VardaCallType, message: String) {
        failNextRequest = ofType
        failResponseCode = failWithHttpCode
        failResponseMessage = message
    }

    private fun shouldFailRequest(type: VardaCallType) = type == failNextRequest

    private fun failRequest(): ResponseEntity<ByteArray> {
        logger.info(
            "MockVardaIntegrationEndpoint: failing $failNextRequest varda call as requested with $failResponseCode"
        )
        failNextRequest = null
        return ResponseEntity.status(failResponseCode).body(failResponseMessage.toByteArray())
    }
}
