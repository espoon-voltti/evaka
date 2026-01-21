// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import fi.espoo.evaka.shared.ConfiguredHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue

private val logger = KotlinLogging.logger {}

val Response.isTooManyRequests: Boolean
    get() = code == 429

fun Response.getRetryAfterSeconds(): Long? {
    return header("Retry-After")?.toLongOrNull()
}

fun basicAuthInterceptor(username: String, password: String): okhttp3.Interceptor {
    val credentials = Credentials.basic(username, password)
    return okhttp3.Interceptor { chain ->
        val authenticatedRequest =
            chain.request().newBuilder().header("Authorization", credentials).build()
        chain.proceed(authenticatedRequest)
    }
}

fun tokenAuthInterceptor(token: String): okhttp3.Interceptor {
    return okhttp3.Interceptor { chain ->
        val authenticatedRequest =
            chain.request().newBuilder().header("Authorization", "Token $token").build()
        chain.proceed(authenticatedRequest)
    }
}

fun OkHttpClient.executeWithRetries(
    request: Request,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
): Response {
    val retryWaitLoggingThresholdSeconds = 10L
    val response = newCall(request).execute()

    return when {
        response.isSuccessful -> {
            response
        }

        response.isTooManyRequests -> {
            if (remainingTries <= 0) {
                response.close()
                throw IllegalStateException(
                    "Failed to receive a non-throttled response after all retries"
                )
            }

            val retryAfter =
                response.getRetryAfterSeconds()
                    ?: run {
                        response.close()
                        throw IllegalStateException(
                            "Failed to find a valid Retry-After header with throttle response"
                        )
                    }

            if (retryAfter > maxRetryAfterWaitSeconds) {
                response.close()
                throw IllegalStateException(
                    "Aborting request after too big Retry-After value: $retryAfter > $maxRetryAfterWaitSeconds seconds"
                )
            }

            if (retryAfter > retryWaitLoggingThresholdSeconds) {
                logger.warn { "Waiting for a large Retry-After as requested: $retryAfter seconds" }
            }

            response.close()
            TimeUnit.SECONDS.sleep(retryAfter)
            executeWithRetries(request, remainingTries - 1, maxRetryAfterWaitSeconds)
        }

        else -> {
            val code = response.code
            val body = response.body.string()
            response.close()
            throw IllegalStateException("Request failed with status $code: $body")
        }
    }
}

fun OkHttpClient.executeWithRetriesForString(
    request: Request,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
): String {
    val response = executeWithRetries(request, remainingTries, maxRetryAfterWaitSeconds)
    return response.use { it.body.string() }
}

inline fun <reified R> OkHttpClient.executeWithRetriesForObject(
    request: Request,
    jsonMapper: JsonMapper,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
): R {
    val jsonBody = executeWithRetriesForString(request, remainingTries, maxRetryAfterWaitSeconds)
    return jsonMapper.readValue(jsonBody)
}

fun OkHttpClient.executeWithRetriesPostJson(
    url: String,
    body: Any?,
    jsonMapper: JsonMapper,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
) {
    val jsonBody =
        jsonMapper.writeValueAsString(body).toRequestBody("application/json".toMediaType())
    val request = Request.Builder().url(url).post(jsonBody).build()
    executeWithRetriesForString(request, remainingTries, maxRetryAfterWaitSeconds)
}

fun buildUrl(rootUrl: URI, endpoint: String): String {
    return rootUrl.resolve(endpoint).toString()
}

fun buildGetRequest(rootUrl: URI, endpoint: String): Request {
    val url = buildUrl(rootUrl, endpoint)
    return Request.Builder().url(url).get().build()
}

fun buildPostRequest(rootUrl: URI, endpoint: String, body: Any?, jsonMapper: JsonMapper): Request {
    val url = buildUrl(rootUrl, endpoint)
    val jsonBody =
        jsonMapper.writeValueAsString(body).toRequestBody("application/json".toMediaType())
    return Request.Builder().url(url).post(jsonBody).build()
}

fun ConfiguredHttpClient.getRootUrl(): URI {
    return this.rootUrl ?: error("Root URL not configured for this OkHttpClient")
}

fun ConfiguredHttpClient.getJsonMapper(): JsonMapper {
    return this.jsonMapper ?: error("JsonMapper not configured for this OkHttpClient")
}

inline fun <reified R> ConfiguredHttpClient.executeGetRequest(
    endpoint: String,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
): R {
    val rootUrl = getRootUrl()
    val jsonMapper = getJsonMapper()
    val request = buildGetRequest(rootUrl, endpoint)
    return client.executeWithRetriesForObject(
        request,
        jsonMapper,
        remainingTries,
        maxRetryAfterWaitSeconds,
    )
}

fun ConfiguredHttpClient.executePostRequest(
    endpoint: String,
    body: Any?,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
) {
    val rootUrl = getRootUrl()
    val jsonMapper = getJsonMapper()
    val request = buildPostRequest(rootUrl, endpoint, body, jsonMapper)
    client.executeWithRetriesForString(request, remainingTries, maxRetryAfterWaitSeconds)
}
