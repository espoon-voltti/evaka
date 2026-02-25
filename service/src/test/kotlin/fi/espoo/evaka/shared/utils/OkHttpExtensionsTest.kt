// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import fi.espoo.evaka.shared.ConfiguredHttpClient
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
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
    fun `headerInterceptor adds custom header to requests`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("Success"))

        val clientWithHeader =
            client
                .newBuilder()
                .addInterceptor(headerInterceptor("X-Custom", "custom-value"))
                .build()

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        clientWithHeader.newCall(request).execute()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("custom-value", recordedRequest.getHeader("X-Custom"))
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
    fun `executeWithRetries returns non-2xx non-429 response as-is`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("Not found"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.executeWithRetries(request)

        assertEquals(404, response.code)
        assertEquals("Not found", response.body.string())
    }

    @Test
    fun `executeWithRetries returns 500 response as-is`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val request = Request.Builder().url(mockWebServer.url("/test")).build()
        val response = client.executeWithRetries(request)

        assertEquals(500, response.code)
        assertEquals("Internal Server Error", response.body.string())
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
    fun `executeWithRetries throws immediately for one-shot body on 429`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))

        val body =
            streamRequestBody(
                "text/csv".toMediaType(),
                ByteArrayInputStream("test data".toByteArray()),
            )
        val request = Request.Builder().url(mockWebServer.url("/test")).put(body).build()

        val exception =
            assertThrows<IllegalStateException> {
                client.executeWithRetries(request, remainingTries = 5)
            }

        assertTrue(exception.message?.contains("one-shot body") ?: false)
        assertEquals(1, mockWebServer.requestCount)
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

    // ===== buildUrl Tests =====

    @Test
    fun `buildUrl constructs URL with endpoint and no query params`() {
        val url = buildUrl(URI("https://example.com/"), "api/data")
        assertEquals("https://example.com/api/data", url)
    }

    @Test
    fun `buildUrl constructs URL with query params`() {
        val url =
            buildUrl(
                URI("https://example.com/"),
                "api/data",
                mapOf("key" to "value", "page" to "1"),
            )
        assertEquals("https://example.com/api/data?key=value&page=1", url)
    }

    @Test
    fun `buildUrl constructs URL with empty query params map`() {
        val url = buildUrl(URI("https://example.com/"), "api/data", emptyMap())
        assertEquals("https://example.com/api/data", url)
    }

    @Test
    fun `buildUrl encodes special characters in query params`() {
        val url =
            buildUrl(
                URI("https://example.com/"),
                "api/data",
                mapOf("filename" to "report 2024.csv", "filter" to "a&b=c"),
            )
        // OkHttp URL-encodes special characters in query parameter values
        assertTrue(url.startsWith("https://example.com/api/data?"))
        assertTrue(
            url.contains("filename=report%202024.csv") || url.contains("filename=report+2024.csv")
        )
        assertTrue(url.contains("filter=a%26b%3Dc"))
    }

    @Test
    fun `buildUrl resolves endpoint against root URL with path`() {
        val url = buildUrl(URI("https://example.com/v1/"), "reports", mapOf("id" to "42"))
        assertEquals("https://example.com/v1/reports?id=42", url)
    }

    @Test
    fun `buildUrl rejects absolute endpoint paths`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                buildUrl(URI("https://example.com/v1/"), "/absolute/path")
            }
        assertTrue(exception.message?.contains("relative path") ?: false)
    }

    @Test
    fun `buildUrl rejects endpoints that resolve to a different host`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                buildUrl(URI("https://example.com/v1/"), "https://evil.com/path")
            }
        assertTrue(exception.message?.contains("does not match root URL host") ?: false)
    }

    @Test
    fun `buildUrl rejects endpoints that escape root URL path via path traversal`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                buildUrl(URI("https://example.com/v1/"), "../../admin/secret")
            }
        assertTrue(exception.message?.contains("escapes root URL path") ?: false)
    }

    // ===== streamRequestBody Tests =====

    @Test
    fun `streamRequestBody writes input stream content to request body`() {
        val content = "hello,world\n1,2\n"
        val body =
            streamRequestBody("text/csv".toMediaType(), ByteArrayInputStream(content.toByteArray()))

        assertEquals("text/csv", body.contentType().toString())
        assertTrue(body.isOneShot())

        val buffer = Buffer()
        body.writeTo(buffer)
        assertEquals(content, buffer.readUtf8())
    }

    // ===== ConfiguredHttpClient get/post/put Tests =====

    private val jsonMapper = defaultJsonMapperBuilder().build()

    private fun configuredClient(jsonMapper: tools.jackson.databind.json.JsonMapper? = null) =
        ConfiguredHttpClient(client, mockWebServer.url("/").toUri(), jsonMapper = jsonMapper)

    @Test
    fun `put sends PUT with correct body and query params`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val configuredClient = configuredClient()
        val body = "csv-data".toRequestBody("text/csv".toMediaType())

        configuredClient.put<Unit>(
            "upload",
            body = body,
            queryParams = mapOf("filename" to "test.csv"),
        )

        val recorded = mockWebServer.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/upload?filename=test.csv", recorded.path)
        assertEquals("csv-data", recorded.body.readUtf8())
    }

    @Test
    fun `post sends POST with correct body and query params`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val configuredClient = configuredClient()
        val body = "post-data".toRequestBody("text/plain".toMediaType())

        configuredClient.post<Unit>("submit", body = body, queryParams = mapOf("type" to "test"))

        val recorded = mockWebServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/submit?type=test", recorded.path)
        assertEquals("post-data", recorded.body.readUtf8())
    }

    @Test
    fun `put with Unit throws on non-successful response`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val configuredClient = configuredClient()
        val body = "data".toRequestBody("text/plain".toMediaType())

        val exception =
            assertThrows<IllegalStateException> {
                configuredClient.put<Unit>("upload", body = body)
            }

        assertTrue(exception.message?.contains("Request failed with status 500") ?: false)
    }

    data class TestItem(val id: Int, val name: String)

    @Test
    fun `get deserializes JSON response`() {
        val json = """[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]"""
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(json)
                .addHeader("Content-Type", "application/json")
        )

        val configuredClient = configuredClient(jsonMapper)
        val result: List<TestItem> = configuredClient.get("items")

        assertEquals(listOf(TestItem(1, "Alice"), TestItem(2, "Bob")), result)
    }

    @Test
    fun `post with custom responseHandler gives full control over response`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val configuredClient = configuredClient()
        val body = "data".toRequestBody("text/plain".toMediaType())

        val result: String =
            configuredClient.post("endpoint", body = body) { response -> "status=${response.code}" }

        assertEquals("status=404", result)
    }

    @Test
    fun `get with custom responseHandler gives full control over response`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("raw body"))

        val configuredClient = configuredClient()

        val result: String = configuredClient.get("endpoint") { response -> response.body.string() }

        assertEquals("raw body", result)
    }

    @Test
    fun `post sends custom headers`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val configuredClient = configuredClient()
        val body = "data".toRequestBody("text/plain".toMediaType())

        configuredClient.post<Unit>("endpoint", body = body, headers = mapOf("X-Custom" to "value"))

        val recorded = mockWebServer.takeRequest()
        assertEquals("value", recorded.getHeader("X-Custom"))
    }

    @Test
    fun `get sends custom headers`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"id":1,"name":"test"}""")
                .addHeader("Content-Type", "application/json")
        )

        val configuredClient = configuredClient(jsonMapper)

        configuredClient.get<TestItem>("endpoint", headers = mapOf("X-Custom" to "value"))

        val recorded = mockWebServer.takeRequest()
        assertEquals("value", recorded.getHeader("X-Custom"))
    }

    @Test
    fun `post with jsonBody serializes object as JSON`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val configuredClient = configuredClient(jsonMapper)
        val item = TestItem(1, "Alice")

        configuredClient.post<Unit>("items", jsonBody = item)

        val recorded = mockWebServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("application/json; charset=utf-8", recorded.getHeader("Content-Type"))
        assertEquals("""{"id":1,"name":"Alice"}""", recorded.body.readUtf8())
    }

    @Test
    fun `put with jsonBody serializes object as JSON`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val configuredClient = configuredClient(jsonMapper)
        val item = TestItem(1, "Updated")

        configuredClient.put<Unit>("items", jsonBody = item)

        val recorded = mockWebServer.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("application/json; charset=utf-8", recorded.getHeader("Content-Type"))
        assertEquals("""{"id":1,"name":"Updated"}""", recorded.body.readUtf8())
    }

    @Test
    fun `post rejects both body and jsonBody`() {
        val configuredClient = configuredClient(jsonMapper)
        val body = "data".toRequestBody("text/plain".toMediaType())

        assertThrows<IllegalArgumentException> {
            configuredClient.post<Unit>("endpoint", body = body, jsonBody = TestItem(1, "x"))
        }
    }

    @Test
    fun `post rejects neither body nor jsonBody`() {
        val configuredClient = configuredClient(jsonMapper)

        assertThrows<IllegalArgumentException> { configuredClient.post<Unit>("endpoint") }
    }
}
