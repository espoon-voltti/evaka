// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports.patu

import fi.espoo.evaka.EspooPatuIntegrationEnv
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import kotlin.test.assertEquals
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EspooPatuIntegrationClientTest {

    private lateinit var mockWebServer: MockWebServer
    private val jsonMapper = defaultJsonMapperBuilder().build()

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `send posts JSON with basic auth and accept header`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val env =
            EspooPatuIntegrationEnv(
                url = mockWebServer.url("/").toString(),
                username = "testuser",
                password = Sensitive("testpass"),
            )
        val client = EspooPatuIntegrationClient(env, jsonMapper)

        client.send(emptyList())

        val recorded = mockWebServer.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/report", recorded.path)
        assertEquals(Credentials.basic("testuser", "testpass"), recorded.getHeader("Authorization"))
        assertEquals("application/json", recorded.getHeader("Accept"))
        assertEquals("application/json; charset=utf-8", recorded.getHeader("Content-Type"))
    }

    @Test
    fun `send throws on server error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500).setBody("Internal Server Error"))

        val env =
            EspooPatuIntegrationEnv(
                url = mockWebServer.url("/").toString(),
                username = "testuser",
                password = Sensitive("testpass"),
            )
        val client = EspooPatuIntegrationClient(env, jsonMapper)

        assertThrows<IllegalStateException> { client.send(emptyList()) }
    }
}
