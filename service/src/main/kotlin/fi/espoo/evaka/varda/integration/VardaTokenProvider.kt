// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.utils.responseStringWithRetries
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.springframework.stereotype.Service

interface VardaTokenProvider {
    /**
     * Wrapper that provides a valid Varda API token and a method for refreshing it in case the
     * current one gets invalidated.
     */
    fun <T> withToken(action: (token: String, refresh: () -> String) -> T): T
}

/**
 * Provide a temporary token fetched with basic authentication credentials from the Varda API.
 *
 * The API only has a single active token per user at a time, making it shared mutable (and remote)
 * state which needs to be locked to prevent invalidating a token about to be used. This is handled
 * automatically and internally in this provider with a mutex.
 */
@Service
class VardaTempTokenProvider(
    private val jsonMapper: JsonMapper,
    env: VardaEnv
) : VardaTokenProvider {
    private val fuel = FuelManager()
    private val basicAuth = "Basic ${env.basicAuth.value}"
    private val apiTokenUrl = "${env.url}/user/apikey/"

    // TODO: How to actually enforce that token is mutable only within a single shared mutex?
    private val tokenLock = ReentrantLock()
    private var token: VardaApiToken? = null

    // TODO: Could we provide an algebraic effect like method for catching token errors and
    // continuing after a refresh?
    override fun <T> withToken(action: (token: String, refreshToken: () -> String) -> T): T =
        tokenLock.withLock {
            action(getValidToken().token) { getValidToken(forceNew = true).token }
        }

    private fun getValidToken(forceNew: Boolean = false): VardaApiToken =
        when (!forceNew && VardaApiToken.isValid(token)) {
            true -> token!!
            false -> {
                token = fetchToken()
                token!!
            }
        }

    private fun fetchToken(): VardaApiToken =
        fuel
            .get(apiTokenUrl)
            .header(Headers.AUTHORIZATION, basicAuth)
            .header(Headers.ACCEPT, "application/json")
            .header(Headers.CONTENT_TYPE, "application/json")
            .responseStringWithRetries(3, 300L)
            .third
            .fold(
                { d -> VardaApiToken.from(jsonMapper.readTree(d).get("token").asText()) },
                { err ->
                    throw IllegalStateException(
                        "Requesting Varda API token failed: status code ${err.response.statusCode}, error ${String(
                            err.errorData
                        )}. Aborting update",
                        err
                    )
                }
            )
}

private data class VardaApiToken(
    val token: String,
    val createdAt: Instant
) {
    companion object {
        fun from(token: String) = VardaApiToken(token, Instant.now())

        // NOTE: This is just our perception of a token's validity. Varda API can invalidate a token
        // at any point.
        fun isValid(token: VardaApiToken?): Boolean = token?.createdAt?.isAfter(Instant.now().minus(12, ChronoUnit.HOURS)) ?: false
    }
}
