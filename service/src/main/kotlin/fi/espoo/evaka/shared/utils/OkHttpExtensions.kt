// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

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
    errorCallback: (response: Response, remainingTries: Int) -> Response = { response, _ ->
        response
    },
): Response {
    val retryWaitLoggingThresholdSeconds = 10L
    val response = newCall(request).execute()

    return when {
        response.isSuccessful -> response

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
            executeWithRetries(request, remainingTries - 1, maxRetryAfterWaitSeconds, errorCallback)
        }

        else -> errorCallback(response, remainingTries - 1)
    }
}

fun OkHttpClient.executeWithRetriesForString(
    request: Request,
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
    errorCallback: (response: Response, remainingTries: Int) -> String = { response, _ ->
        val code = response.code
        val body = response.body.string()
        response.close()
        throw IllegalStateException("Request failed with status $code: $body")
    },
): String {
    val response =
        executeWithRetries(
            request,
            remainingTries,
            maxRetryAfterWaitSeconds,
            errorCallback = { r, remaining ->
                // For string responses, we need to read the body in the error callback
                // and then throw, since we can't return a Response here
                errorCallback(r, remaining)
                r // This line won't be reached if errorCallback throws
            },
        )

    return response.use { it.body.string() }
}
