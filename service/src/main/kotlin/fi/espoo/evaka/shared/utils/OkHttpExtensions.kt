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

fun headerInterceptor(name: String, value: String): okhttp3.Interceptor {
    return okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder().header(name, value).build()
        chain.proceed(request)
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

    if (!response.isTooManyRequests) {
        return response
    }

    if (remainingTries <= 0) {
        response.close()
        throw IllegalStateException("Failed to receive a non-throttled response after all retries")
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
    return executeWithRetries(request, remainingTries - 1, maxRetryAfterWaitSeconds)
}

fun buildUrl(
    rootUrl: URI,
    endpoint: String,
    queryParams: Map<String, String> = emptyMap(),
): String {
    require(!endpoint.startsWith("/")) {
        "Endpoint must be a relative path, not an absolute path: $endpoint"
    }
    val resolved = rootUrl.resolve(endpoint)
    require(resolved.host == rootUrl.host) {
        "Resolved URL host '${resolved.host}' does not match root URL host '${rootUrl.host}'"
    }
    require(resolved.path.startsWith(rootUrl.path)) {
        "Resolved URL path '${resolved.path}' escapes root URL path '${rootUrl.path}'"
    }
    val urlBuilder = resolved.toString().toHttpUrl().newBuilder()
    queryParams.forEach { (key, value) -> urlBuilder.addQueryParameter(key, value) }
    return urlBuilder.build().toString()
}

fun ConfiguredHttpClient.getRootUrl(): URI {
    return this.rootUrl ?: error("Root URL not configured for this OkHttpClient")
}

fun ConfiguredHttpClient.getJsonMapper(): JsonMapper {
    return this.jsonMapper ?: error("JsonMapper not configured for this OkHttpClient")
}

@PublishedApi
internal fun ConfiguredHttpClient.resolveBody(body: RequestBody?, jsonBody: Any?): RequestBody {
    require(body != null || jsonBody != null) { "Either body or jsonBody must be provided" }
    require(body == null || jsonBody == null) { "Only one of body or jsonBody can be provided" }
    return body
        ?: getJsonMapper()
            .writeValueAsString(jsonBody)
            .toRequestBody("application/json".toMediaType())
}

@PublishedApi
internal inline fun <reified R> ConfiguredHttpClient.executeAndHandleResponse(
    request: Request,
    noinline responseHandler: ((Response) -> R)?,
): R =
    client.executeWithRetries(request).use { response ->
        if (responseHandler != null) {
            responseHandler(response)
        } else {
            if (!response.isSuccessful) {
                error("Request failed with status ${response.code}: ${response.body.string()}")
            }
            when (R::class) {
                Unit::class -> Unit as R
                else -> getJsonMapper().readValue<R>(response.body.string())
            }
        }
    }

inline fun <reified R> ConfiguredHttpClient.get(
    endpoint: String,
    queryParams: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    noinline responseHandler: ((Response) -> R)? = null,
): R {
    val url = buildUrl(getRootUrl(), endpoint, queryParams)
    val request =
        Request.Builder()
            .url(url)
            .get()
            .apply { headers.forEach { (n, v) -> header(n, v) } }
            .build()
    return executeAndHandleResponse(request, responseHandler)
}

inline fun <reified R> ConfiguredHttpClient.post(
    endpoint: String,
    body: RequestBody? = null,
    jsonBody: Any? = null,
    queryParams: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    noinline responseHandler: ((Response) -> R)? = null,
): R {
    val url = buildUrl(getRootUrl(), endpoint, queryParams)
    val requestBody = resolveBody(body, jsonBody)
    val request =
        Request.Builder()
            .url(url)
            .post(requestBody)
            .apply { headers.forEach { (n, v) -> header(n, v) } }
            .build()
    return executeAndHandleResponse(request, responseHandler)
}

inline fun <reified R> ConfiguredHttpClient.put(
    endpoint: String,
    body: RequestBody? = null,
    jsonBody: Any? = null,
    queryParams: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    noinline responseHandler: ((Response) -> R)? = null,
): R {
    val url = buildUrl(getRootUrl(), endpoint, queryParams)
    val requestBody = resolveBody(body, jsonBody)
    val request =
        Request.Builder()
            .url(url)
            .put(requestBody)
            .apply { headers.forEach { (n, v) -> header(n, v) } }
            .build()
    return executeAndHandleResponse(request, responseHandler)
}
