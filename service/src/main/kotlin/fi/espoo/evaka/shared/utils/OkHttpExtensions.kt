// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import fi.espoo.evaka.shared.ConfiguredHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.source
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

fun streamRequestBody(contentType: MediaType, inputStream: InputStream): RequestBody =
    object : RequestBody() {
        override fun contentType() = contentType

        override fun isOneShot() = true

        override fun writeTo(sink: BufferedSink) {
            inputStream.source().use { source -> sink.writeAll(source) }
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

            if (request.body?.isOneShot() == true) {
                response.close()
                throw IllegalStateException("Cannot retry a request with a one-shot body")
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

fun buildUrl(
    rootUrl: URI,
    endpoint: String,
    queryParams: Map<String, String> = emptyMap(),
): String {
    val urlBuilder = rootUrl.resolve(endpoint).toString().toHttpUrl().newBuilder()
    queryParams.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
    return urlBuilder.build().toString()
}

fun ConfiguredHttpClient.getRootUrl(): URI {
    return this.rootUrl ?: error("Root URL not configured for this OkHttpClient")
}

fun ConfiguredHttpClient.getJsonMapper(): JsonMapper {
    return this.jsonMapper ?: error("JsonMapper not configured for this OkHttpClient")
}

fun ConfiguredHttpClient.jsonBody(body: Any?): RequestBody =
    getJsonMapper().writeValueAsString(body).toRequestBody("application/json".toMediaType())

inline fun <reified R> ConfiguredHttpClient.executeGetRequest(
    endpoint: String,
    queryParams: Map<String, String> = emptyMap(),
): R {
    val url = buildUrl(getRootUrl(), endpoint, queryParams)
    val request = Request.Builder().url(url).get().build()
    val jsonBody = client.executeWithRetriesForString(request)
    return getJsonMapper().readValue(jsonBody)
}

fun ConfiguredHttpClient.executePostRequest(
    endpoint: String,
    body: RequestBody,
    queryParams: Map<String, String> = emptyMap(),
) {
    val url = buildUrl(getRootUrl(), endpoint, queryParams)
    val request = Request.Builder().url(url).post(body).build()
    client.executeWithRetries(request).close()
}

fun ConfiguredHttpClient.executePostJsonRequest(
    endpoint: String,
    body: Any?,
    queryParams: Map<String, String> = emptyMap(),
) = executePostRequest(endpoint, jsonBody(body), queryParams)

fun ConfiguredHttpClient.executePutRequest(
    endpoint: String,
    body: RequestBody,
    queryParams: Map<String, String> = emptyMap(),
) {
    val url = buildUrl(getRootUrl(), endpoint, queryParams)
    val request = Request.Builder().url(url).put(body).build()
    client.executeWithRetries(request).close()
}
