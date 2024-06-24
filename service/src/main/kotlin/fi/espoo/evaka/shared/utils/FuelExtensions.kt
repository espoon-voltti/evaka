// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.AuthenticatedRequest
import com.github.kittinunf.result.Result
import java.util.concurrent.TimeUnit
import mu.KotlinLogging

typealias ErrorResponseResultOf = Triple<Request, Response, Result.Failure<FuelError>>

private val logger = KotlinLogging.logger {}

/**
 * Collection of miscellaneous extensions to Fuel core classes. For example: extra status code
 * checks, headers.
 */

/**
 * Response status sent to throttle a requester. A Retry-After header might be included to this
 * response indicating how long to wait before making a new request.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429">MDN: 429 Too Many
 *   Requests</a>
 */
val Response.isTooManyRequests
    get() = statusCode == 429

/**
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After">MDN:
 *   Retry-After</a>
 */
val Headers.Companion.RETRY_AFTER: String
    get() = "Retry-After"

/** Add token authentication to the request */
fun AuthenticatedRequest.token(token: String): Request {
    this[Headers.AUTHORIZATION] = "Token $token"
    return request
}

/**
 * Execute the [Request] synchronously, into a [Charsets.UTF_8] [String]. If a HTTP 429 response is
 * received, request is retried the specified amount of times.
 *
 * Throws an [IllegalStateException] if a valid Retry-After header is not received in throttled
 * response. Throws an [IllegalStateException] if a non-throttled response isn't received after all
 * retries.
 *
 * @return [ResponseResultOf] The response result of [String]
 * @note This is a synchronous execution and can not be cancelled
 *
 * TODO: Set a maximum wait time TODO: Support http-date format Retry-After headers, not only
 *   delay-seconds
 */
fun Request.responseStringWithRetries(
    remainingTries: Int = 5,
    maxRetryAfterWaitSeconds: Long = 600L,
    errorCallback: (r: ErrorResponseResultOf, remainingTries: Int) -> ResponseResultOf<String> =
        { r, _ ->
            r
        }
): ResponseResultOf<String> {
    val retryWaitLoggingThresholdSeconds = 10L
    val responseResult = responseString()
    val (request, response, result) = responseResult

    return when (result) {
        is Result.Success -> responseResult
        is Result.Failure ->
            when (response.isTooManyRequests) {
                true ->
                    if (remainingTries <= 0) {
                        return ResponseResultOf(
                            request,
                            response,
                            Result.error(
                                FuelError.wrap(
                                    IllegalStateException(
                                        "Failed to receive a non-throttled response after all retries"
                                    )
                                )
                            )
                        )
                    } else {
                        val retryAfter =
                            response[Headers.RETRY_AFTER].firstOrNull()?.toLong()
                                ?: throw IllegalStateException(
                                    "Failed to find a valid Retry-After header with throttle response"
                                )

                        if (retryAfter > maxRetryAfterWaitSeconds) {
                            throw IllegalStateException(
                                "Aborting fuel request after too big Retry-After value: $retryAfter > $maxRetryAfterWaitSeconds seconds"
                            )
                        }

                        if (retryAfter > retryWaitLoggingThresholdSeconds) {
                            logger.warn(
                                "Waiting for a large RETRY_AFTER as requested: $retryAfter seconds"
                            )
                        }

                        TimeUnit.SECONDS.sleep(retryAfter)
                        this.responseStringWithRetries(remainingTries - 1, maxRetryAfterWaitSeconds)
                    }
                false -> {
                    return errorCallback(
                        ErrorResponseResultOf(request, response, result),
                        remainingTries - 1
                    )
                }
            }
    }
}
