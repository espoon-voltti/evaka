// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OkHttpExtensionsTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        client =
            OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    // ===== Response Extension Tests =====

    @Test
    fun `isTooManyRequests returns true for HTTP 429 response`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.newCall(request).execute()

        assertTrue(response.isTooManyRequests)
    }

    @Test
    fun `isTooManyRequests returns false for non-429 response`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.newCall(request).execute()

        assertEquals(false, response.isTooManyRequests)
    }

    @Test
    fun `getRetryAfterSeconds returns the Retry-After header value as Long`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "30"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.newCall(request).execute()

        assertEquals(30L, response.getRetryAfterSeconds())
    }

    @Test
    fun `getRetryAfterSeconds returns null when Retry-After header is missing`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.newCall(request).execute()

        assertEquals(null, response.getRetryAfterSeconds())
    }

    @Test
    fun `getRetryAfterSeconds returns null for non-numeric Retry-After header`() {
        mockWebServer.enqueue(
            MockResponse().setResponseCode(429).addHeader("Retry-After", "invalid")
        )

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.newCall(request).execute()

        assertEquals(null, response.getRetryAfterSeconds())
    }

    // ===== Interceptor Tests =====

    @Test
    fun `basicAuthInterceptor adds Basic Authorization header to requests`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val expectedAuthValue = Credentials.basic("testuser", "testpass")
        val clientWithAuth =
            client.newBuilder().addInterceptor(basicAuthInterceptor("testuser", "testpass")).build()

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        clientWithAuth.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(expectedAuthValue, recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `tokenAuthInterceptor adds Token Authorization header to requests`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val clientWithAuth =
            client.newBuilder().addInterceptor(tokenAuthInterceptor("mytoken")).build()

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        clientWithAuth.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Token mytoken", recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `basicAuthInterceptor handles special characters in credentials`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val expectedAuthValue = Credentials.basic("user@example.com", "p@ss:word")
        val clientWithAuth =
            client
                .newBuilder()
                .addInterceptor(basicAuthInterceptor("user@example.com", "p@ss:word"))
                .build()

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        clientWithAuth.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(expectedAuthValue, recordedRequest.getHeader("Authorization"))
    }

    @Test
    fun `interceptors add authorization to all requests automatically`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("First"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Second"))

        val clientWithAuth =
            client.newBuilder().addInterceptor(tokenAuthInterceptor("token123")).build()

        // Make two different requests
        clientWithAuth.newCall(Request.Builder().url(mockWebServer.url("/first")).build()).execute()
        clientWithAuth
            .newCall(Request.Builder().url(mockWebServer.url("/second")).build())
            .execute()

        // Both should have the authorization header
        val firstRequest = mockWebServer.takeRequest()
        val secondRequest = mockWebServer.takeRequest()

        assertEquals("Token token123", firstRequest.getHeader("Authorization"))
        assertEquals("Token token123", secondRequest.getHeader("Authorization"))
    }

    // ===== Retry Logic Tests =====

    @Test
    fun `executeWithRetries returns successful response on first try`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.executeWithRetries(request)

        assertEquals(200, response.code)
        assertEquals("Success", response.body.string())
    }

    @Test
    fun `executeWithRetries retries on HTTP 429 with Retry-After`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "1") // 1 second retry delay
        )
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success after retry"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response =
            client.executeWithRetries(request, remainingTries = 5, maxRetryAfterWaitSeconds = 600L)

        assertEquals(200, response.code)
        assertEquals("Success after retry", response.body.string())
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun `executeWithRetries throws IllegalStateException when retry-after exceeds max wait time`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "700") // Exceeds default maxRetryAfterWaitSeconds (600)
        )

        val request = Request.Builder().url(mockWebServer.url("/test")).build()

        val exception =
            assertThrows<IllegalStateException> {
                client.executeWithRetries(
                    request,
                    remainingTries = 5,
                    maxRetryAfterWaitSeconds = 600L,
                )
            }

        assertTrue(exception.message?.contains("too big Retry-After value") ?: false)
    }

    @Test
    fun `executeWithRetries throws IllegalStateException when no Retry-After header in 429 response`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429)) // No Retry-After header

        val request = Request.Builder().url(mockWebServer.url("/test")).build()

        val exception =
            assertThrows<IllegalStateException> {
                client.executeWithRetries(request, remainingTries = 5)
            }

        assertTrue(exception.message?.contains("Retry-After header") ?: false)
    }

    @Test
    fun `executeWithRetries respects remaining tries limit`() {
        // Queue 3 responses with 429 status codes
        repeat(3) {
            mockWebServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))
        }
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()

        // With remainingTries=2, it can only retry twice, so it will fail
        val exception =
            assertThrows<IllegalStateException> {
                client.executeWithRetries(
                    request,
                    remainingTries = 2,
                    maxRetryAfterWaitSeconds = 600L,
                )
            }

        assertEquals(
            "Failed to receive a non-throttled response after all retries",
            exception.message,
        )
    }

    @Test
    fun `executeWithRetriesForString returns response body as string on success`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Response body text"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val responseBody = client.executeWithRetriesForString(request)

        assertEquals("Response body text", responseBody)
    }

    @Test
    fun `executeWithRetriesForString retries on HTTP 429 and returns success body`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success text"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val responseBody = client.executeWithRetriesForString(request, remainingTries = 5)

        assertEquals("Success text", responseBody)
    }

    @Test
    fun `executeWithRetriesForString throws for unsuccessful non-429 response by default`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not found"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()

        val exception =
            assertThrows<IllegalStateException> { client.executeWithRetriesForString(request) }

        assertTrue(exception.message?.contains("Request failed with status 404") ?: false)
    }

    @Test
    fun `executeWithRetries handles multiple consecutive 429 responses`() {
        // Queue 2 429 responses, then success
        mockWebServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))
        mockWebServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Finally!"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.executeWithRetries(request, remainingTries = 5)

        assertEquals(200, response.code)
        assertEquals("Finally!", response.body.string())
        assertEquals(3, mockWebServer.requestCount)
    }
}
