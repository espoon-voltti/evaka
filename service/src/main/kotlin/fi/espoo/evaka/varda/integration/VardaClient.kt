// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.result.Result
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.utils.responseStringWithRetries
import fi.espoo.evaka.shared.utils.token
import fi.espoo.evaka.varda.VardaChildPayload
import fi.espoo.evaka.varda.VardaChildRequest
import fi.espoo.evaka.varda.VardaChildResponse
import fi.espoo.evaka.varda.VardaDecision
import fi.espoo.evaka.varda.VardaDecisionResponse
import fi.espoo.evaka.varda.VardaFeeData
import fi.espoo.evaka.varda.VardaFeeDataResponse
import fi.espoo.evaka.varda.VardaPaosChildPayload
import fi.espoo.evaka.varda.VardaPersonRequest
import fi.espoo.evaka.varda.VardaPersonResponse
import fi.espoo.evaka.varda.VardaPlacement
import fi.espoo.evaka.varda.VardaPlacementResponse
import fi.espoo.evaka.varda.VardaUnitRequest
import fi.espoo.evaka.varda.VardaUnitResponse
import fi.espoo.evaka.varda.VardaUpdateOrganizer
import fi.espoo.voltti.logging.loggers.error
import mu.KotlinLogging
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

class VardaClient(
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val objectMapper: ObjectMapper,
    env: VardaEnv,
) {
    private val organizerUrl = "${env.url}/v1/vakajarjestajat/"
    private val unitUrl = "${env.url}/v1/toimipaikat/"
    private val personUrl = "${env.url}/v1/henkilot/"
    private val personSearchUrl = "${env.url}/v1/hae-henkilo/"
    private val childUrl = "${env.url}/v1/lapset/"
    private val decisionUrl = "${env.url}/v1/varhaiskasvatuspaatokset/"
    private val placementUrl = "${env.url}/v1/varhaiskasvatussuhteet/"
    private val feeDataUrl = "${env.url}/v1/maksutiedot/"

    val getPersonUrl = { personId: Long -> "$personUrl$personId/" }
    val getChildUrl = { childId: Long -> "$childUrl$childId/" }
    val getDecisionUrl = { decisionId: Long -> "$decisionUrl$decisionId/" }
    val getPlacementUrl = { placementId: Long -> "$placementUrl$placementId/" }
    val sourceSystem: String = env.sourceSystem

    fun createUnit(unit: VardaUnitRequest): VardaUnitResponse? {
        logger.info { "Creating a new unit ${unit.nimi} to Varda" }
        val (request, _, result) = fuel.post(unitUrl)
            .jsonBody(objectMapper.writeValueAsString(unit))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Creating a new unit ${unit.nimi} to Varda succeeded" }
                objectMapper.readValue<VardaUnitResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun updateUnit(unit: VardaUnitRequest): VardaUnitResponse? {
        logger.info { "Updating unit ${unit.nimi} to Varda" }
        val url = "$unitUrl${unit.id}/"
        val (request, _, result) = fuel.put(url)
            .jsonBody(objectMapper.writeValueAsString(unit))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Updating unit ${unit.nimi} to Varda succeeded" }
                objectMapper.readValue<VardaUnitResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun updateOrganizer(organizer: VardaUpdateOrganizer): Boolean {
        logger.info { "Updating organizer to Varda" }
        val (request, _, result) = fuel.put("$organizerUrl${organizer.vardaOrganizerId}")
            .header(Headers.CONTENT_TYPE, "application/json")
            .jsonBody(objectMapper.writeValueAsString(organizer))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Updating organizer to Varda succeeded" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                false
            }
        }
    }

    fun createPerson(newPerson: VardaPersonRequest): VardaPersonResponse? {
        logger.info { "Creating a new person ${newPerson.id} to Varda" }
        val (request, _, result) = fuel.post(personUrl)
            .jsonBody(objectMapper.writeValueAsString(newPerson)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Creating a new person ${newPerson.id} to Varda succeeded" }
                objectMapper.readValue<VardaPersonResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    data class VardaPersonSearchRequest(val henkilotunnus: String?, val henkilo_oid: String?) {
        init {
            check(henkilotunnus != null || henkilo_oid != null) {
                "Both params ssn and oid shouldn't be null"
            }
        }
    }

    fun getPersonFromVardaBySsnOrOid(body: VardaPersonSearchRequest): VardaPersonResponse? {
        logger.info { "Fetching person from Varda" }
        val (request, _, result) = fuel.post(personSearchUrl)
            .jsonBody(objectMapper.writeValueAsString(body)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Fetching person from Varda succeeded" }
                objectMapper.readValue<VardaPersonResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun createChild(child: VardaChildRequest): VardaChildResponse? {
        logger.info { "Creating child to Varda (body: $child)" }
        val (request, _, result) = fuel.post(childUrl)
            .jsonBody(objectMapper.writeValueAsString(child)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Creating child to Varda succeeded (body: $child)" }
                objectMapper.readValue<VardaChildResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun createFeeData(feeData: VardaFeeData): VardaFeeDataResponse? {
        logger.info { "Creating fee data for child ${feeData.lapsi} to Varda" }
        val (request, _, result) = fuel.post(feeDataUrl)
            .jsonBody(objectMapper.writeValueAsString(feeData)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Creating fee data for child ${feeData.lapsi} to Varda succeeded" }
                objectMapper.readValue<VardaFeeDataResponse>(
                    objectMapper.readTree(result.get()).toString()
                )
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun updateFeeData(vardaFeeDataId: Long, feeData: VardaFeeData): Boolean {
        logger.info { "Updating fee data $vardaFeeDataId to Varda" }
        val (request, _, result) = fuel.put("$feeDataUrl$vardaFeeDataId")
            .jsonBody(objectMapper.writeValueAsString(feeData)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Updating fee data $vardaFeeDataId to Varda succeeded" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                false
            }
        }
    }

    fun deleteFeeData(vardaId: Long): Boolean {
        logger.info { "Deleting fee data $vardaId from Varda" }
        val (request, _, result) = fuel.delete("$feeDataUrl$vardaId/").authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Deleting fee data $vardaId from Varda succeeded" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                false
            }
        }
    }

    fun deleteFeeDataV2(vardaId: Long): Boolean {
        logger.info { "VardaUpdate: Deleting fee data $vardaId from Varda" }
        val (request, _, result) = fuel.delete("$feeDataUrl$vardaId/").authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "VardaUpdate: Deleting fee data $vardaId from Varda succeeded" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                throw error(result.error)
            }
        }
    }

    fun createDecision(newDecision: VardaDecision): VardaDecisionResponse? {
        logger.info { "Creating a new decision to Varda (body: $newDecision)" }
        val (request, _, result) = fuel.post(decisionUrl)
            .jsonBody(objectMapper.writeValueAsString(newDecision)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Creating a new decision to Varda succeeded (body: $newDecision)" }
                objectMapper.readValue(result.get())
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun updateDecision(vardaDecisionId: Long, updatedDecision: VardaDecision): VardaDecisionResponse? {
        logger.info { "Updating a decision to Varda (vardaId: $vardaDecisionId, body: $updatedDecision)" }
        val (request, _, result) = fuel.put(getDecisionUrl(vardaDecisionId))
            .jsonBody(objectMapper.writeValueAsString(updatedDecision)).authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Updating a decision to Varda succeeded (vardaId: $vardaDecisionId, body: $updatedDecision)" }
                objectMapper.readValue(result.get())
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun deleteDecision(vardaDecisionId: Long): Boolean {
        logger.info { "Deleting decision from Varda (id: $vardaDecisionId)" }
        val (request, _, result) = fuel.delete(getDecisionUrl(vardaDecisionId))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Deleting decision from Varda succeeded (id: $vardaDecisionId)" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                false
            }
        }
    }

    fun deleteDecisionV2(vardaDecisionId: Long): Boolean {
        logger.info { "VardaUpdate: Deleting decision from Varda (id: $vardaDecisionId)" }
        val (request, _, result) = fuel.delete(getDecisionUrl(vardaDecisionId))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "VardaUpdate: Deleting decision from Varda succeeded (id: $vardaDecisionId)" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                throw error(result.error)
            }
        }
    }

    fun createPlacement(newPlacement: VardaPlacement): VardaPlacementResponse? {
        logger.info { "Creating a new placement to Varda (body: $newPlacement)" }
        val (request, _, result) = fuel.post(placementUrl)
            .jsonBody(objectMapper.writeValueAsString(newPlacement))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Creating a new placement to Varda succeeded (body: $newPlacement)" }
                objectMapper.readValue(result.get())
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun updatePlacement(
        vardaPlacementId: Long,
        updatedPlacement: VardaPlacement
    ): VardaPlacementResponse? {
        logger.info { "Updating a placement to Varda (body: $updatedPlacement)" }
        val (request, _, result) = fuel.put(getPlacementUrl(vardaPlacementId))
            .jsonBody(objectMapper.writeValueAsString(updatedPlacement))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Updating a placement to Varda succeeded (body: $updatedPlacement)" }
                objectMapper.readValue(result.get())
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                null
            }
        }
    }

    fun deletePlacement(vardaPlacementId: Long): Boolean {
        logger.info { "Deleting placement from Varda (id: $vardaPlacementId)" }
        val (request, _, result) = fuel.delete(getPlacementUrl(vardaPlacementId))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Deleting placement from Varda succeeded (id: $vardaPlacementId)" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                false
            }
        }
    }

    fun deletePlacementV2(vardaPlacementId: Long): Boolean {
        logger.info { "VardaUpdate: Deleting placement from Varda (id: $vardaPlacementId)" }
        val (request, _, result) = fuel.delete(getPlacementUrl(vardaPlacementId))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "VardaUpdate: Deleting placement from Varda succeeded (id: $vardaPlacementId)" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                throw error(result.error)
            }
        }
    }

    fun deleteChild(vardaChildId: Long): Boolean {
        val (request, _, result) = fuel.delete(getChildUrl(vardaChildId))
            .authenticatedResponseStringWithRetries()

        return when (result) {
            is Result.Success -> {
                logger.info { "Deleting child from Varda succeeded (id: $vardaChildId)" }
                true
            }
            is Result.Failure -> {
                logRequestError(request, result.error)
                false
            }
        }
    }

    data class VardaResultId(
        val id: Long
    )

    fun getFeeDataByChild(vardaChildId: Long): List<Long> {
        logger.info { "Getting fee data from Varda (child id: $vardaChildId)" }
        return getAllPages("$feeDataUrl?lapsi=$vardaChildId") {
            objectMapper.readValue<PaginatedResponse<VardaResultId>>(it)
        }.map { it.id }
    }

    fun getPlacementsByDecision(vardaDecisionId: Long): List<Long> {
        logger.info { "Getting placements from Varda (decision id: $vardaDecisionId)" }
        return getAllPages("$placementUrl?varhaiskasvatuspaatos=$vardaDecisionId") {
            objectMapper.readValue<PaginatedResponse<VardaResultId>>(it)
        }.map { it.id }
    }

    fun getDecisionsByChild(vardaChildId: Long): List<Long> {
        logger.info { "Getting decisions from Varda (child id: $vardaChildId)" }
        return getAllPages("$decisionUrl?lapsi=$vardaChildId") {
            objectMapper.readValue<PaginatedResponse<VardaResultId>>(it)
        }.map { it.id }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DecisionPeriod(
        val id: Long,
        val alkamis_pvm: LocalDate,
        val paattymis_pvm: LocalDate
    )

    fun getChildDecisions(vardaChildId: Long): List<DecisionPeriod> {
        return getAllPages("$childUrl$vardaChildId/varhaiskasvatuspaatokset/") {
            objectMapper.readValue<PaginatedResponse<DecisionPeriod>>(it)
        }
    }

    data class PaginatedResponse<T>(
        val count: Int,
        val next: String?,
        val previous: String?,
        val results: List<T>
    )

    private fun <T> getAllPages(
        initialUrl: String,
        parseJson: (String) -> PaginatedResponse<T>
    ): List<T> {
        fun fetchNext(acc: List<T>, next: String?): List<T> {
            return if (next == null) acc
            else {
                val (request, _, result) = fuel.get(next).authenticatedResponseStringWithRetries()
                when (result) {
                    is Result.Success -> {
                        val response = parseJson(result.value)
                        fetchNext(acc + response.results, response.next)
                    }
                    is Result.Failure -> {
                        logRequestError(request, result.error)
                        acc
                    }
                }
            }
        }

        return fetchNext(listOf(), initialUrl)
    }

    /**
     * Wrapper for Fuel Request.responseString() that handles API token refreshes and retries when throttled.
     *
     * API token refreshes are only attempted once and don't count as a try of the original request.
     *
     * TODO: Make API token usage thread-safe. Now nothing prevents another thread from invalidating the token about to be used by another thread.
     */
    private fun Request.authenticatedResponseStringWithRetries(maxTries: Int = 3): ResponseResultOf<String> =
        tokenProvider.withToken { token, refreshToken ->
            this
                .authentication().token(token)
                .header(Headers.ACCEPT, "application/json")
                .responseStringWithRetries(maxTries) { r, remainingTries ->
                    when (r.second.statusCode) {
                        403 -> when {
                            objectMapper.readTree(r.third.error.errorData).get("errors")
                                ?.any { it.get("error_code").asText() == "PE007" }
                                ?: false -> {
                                logger.info { "Varda API token invalid. Refreshing token and retrying original request." }
                                val newToken = refreshToken()
                                // API token refresh should only be attempted once -> don't pass an error handler to let
                                // any subsequent errors fall through.
                                this
                                    .authentication().token(newToken)
                                    .responseStringWithRetries(remainingTries)
                            }
                            else -> r
                        }
                        else -> r
                    }
                }
        }
}

private fun logRequestError(request: Request, error: FuelError) {
    val meta = mapOf(
        "method" to request.method,
        "url" to request.url,
        "body" to request.body.asString("application/json"),
        "errorMessage" to error.errorData.decodeToString()
    )
    logger.error(error, meta) { "Varda request to ${request.url} failed, status ${error.response.statusCode}" }
}

fun convertToVardaChildRequest(evakaPersonId: UUID, payload: VardaChildPayload): VardaChildRequest {
    return VardaChildRequest(
        id = evakaPersonId,
        henkilo = payload.personUrl,
        henkilo_oid = payload.personOid,
        vakatoimija_oid = payload.organizerOid,
        oma_organisaatio_oid = null,
        paos_organisaatio_oid = null,
        lahdejarjestelma = payload.sourceSystem
    )
}

fun convertToVardaChildRequest(evakaPersonId: UUID, payload: VardaPaosChildPayload): VardaChildRequest {
    return VardaChildRequest(
        id = evakaPersonId,
        henkilo = payload.personUrl,
        henkilo_oid = payload.personOid,
        vakatoimija_oid = null,
        oma_organisaatio_oid = payload.organizerOid,
        paos_organisaatio_oid = payload.paosOrganizationOid,
        lahdejarjestelma = payload.sourceSystem
    )
}
