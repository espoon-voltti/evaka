// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.ResponseResultOf
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.github.kittinunf.fuel.core.requests.DefaultRequest
import com.github.kittinunf.result.Result
import java.io.ByteArrayInputStream
import java.net.URI
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

class FuelExtensionsTest {
    private val basicRetryResponse = createRetryResponse("0")

    @Test
    fun `token adds Token Authorization header to request`() {
        val request =
            DefaultRequest(Method.GET, URI("https://example.com/authentication").toURL())
                .authentication()
                .token("token")

        assertThat(request[Headers.AUTHORIZATION].lastOrNull(), equalTo("Token token"))
    }

    @Test
    fun `responseStringWithRetries returns final response when succeeding within retry count`() {
        val client = mock<Client>()
        `when`(client.executeRequest(any()))
            .thenReturn(basicRetryResponse)
            .thenReturn(
                Response(
                    statusCode = 200,
                    url = URI("https://example.com").toURL(),
                    body = DefaultBody.from({ ByteArrayInputStream("final".toByteArray()) }, null)
                )
            )
        FuelManager.instance.client = client

        val (_, response, result) = Fuel.get("https://example.com").responseStringWithRetries(1, 1L)

        assertEquals(200, response.statusCode)
        assertEquals("final", result.get())
    }

    @Test
    fun `responseStringWithRetries returns the last response when a non-throttle response is not received within retry count`() {
        val client = mock<Client>()
        `when`(client.executeRequest(any()))
            .thenReturn(basicRetryResponse)
            .thenReturn(basicRetryResponse)
            .thenReturn(basicRetryResponse) // One more than retry count
            .thenReturn(Response(statusCode = 200, url = URI("https://example.com").toURL()))
        FuelManager.instance.client = client

        val (_, response, result) =
            Fuel
                .get("https://example.com")
                .responseStringWithRetries(2, 1L) // Would have to be 3 to succeed
        assertEquals(429, response.statusCode) // Status code of last response
        assertTrue(result is Result.Failure)
    }

    @Test
    fun `responseStringWithRetries throws when an invalid Retry-After header is received`() {
        val client = mock<Client>()
        `when`(client.executeRequest(any())).thenReturn(createRetryResponse("INVALID VALUE"))
        FuelManager.instance.client = client

        assertThrows<NumberFormatException> {
            Fuel
                .get("https://example.com")
                .responseStringWithRetries(1, 1L) // Would have to be 2 to succeed
        }
    }

    @Test
    fun `responseStringWithRetries throws when no Retry-After header is received in HTTP 429 response`() {
        val client = mock<Client>()
        `when`(client.executeRequest(any()))
            .thenReturn(createRetryResponse("", Headers())) // No Retry-After header
        FuelManager.instance.client = client

        assertThrows<IllegalStateException> {
            Fuel.get("https://example.com").responseStringWithRetries(1)
        }
    }

    @Test
    fun `responseStringWithRetries returns non-throttled error by default`() {
        val client = mock<Client>()
        `when`(client.executeRequest(any()))
            .thenReturn(
                Response(
                    statusCode = 400,
                    url = URI("https://example.com").toURL(),
                    body =
                        DefaultBody.from(
                            { ByteArrayInputStream("unhandled error".toByteArray()) },
                            null
                        )
                )
            )
        FuelManager.instance.client = client

        val (_, response, _) = Fuel.get("https://example.com").responseStringWithRetries(1)
        assertEquals(400, response.statusCode)
        assertEquals("unhandled error", response.body().asString("text/html"))
    }

    @Test
    fun `responseStringWithRetries returns fallback error handler's response when faced with a non-throttled error response`() {
        val client = mock<Client>()
        `when`(client.executeRequest(any()))
            .thenReturn(
                Response(
                    statusCode = 400,
                    url = URI("https://example.com").toURL(),
                    body =
                        DefaultBody.from(
                            { ByteArrayInputStream("unhandled error".toByteArray()) },
                            null
                        )
                )
            )
        FuelManager.instance.client = client

        val (_, response, _) =
            Fuel.get("https://example.com").responseStringWithRetries(1) { r, _ ->
                ResponseResultOf(
                    r.first,
                    Response(
                        statusCode = 400,
                        url = URI("https://example.com").toURL(),
                        body =
                            DefaultBody.from(
                                { ByteArrayInputStream("handled error".toByteArray()) },
                                null
                            )
                    ),
                    r.third
                )
            }
        assertEquals(400, response.statusCode)
        assertEquals("handled error", response.body().asString("text/html"))
    }

    private fun createRetryResponse(
        retryAfter: String,
        headers: Headers = Headers.from(Headers.RETRY_AFTER to listOf(retryAfter))
    ) = Response(
        statusCode = 429,
        responseMessage = "RETRY",
        url = URI("https://example.com").toURL(),
        headers = headers
    )
}
