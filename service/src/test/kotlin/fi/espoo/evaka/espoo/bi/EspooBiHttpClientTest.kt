// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.espoo.bi

import fi.espoo.evaka.EspooBiEnv
import fi.espoo.evaka.Sensitive
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import okhttp3.Credentials
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EspooBiHttpClientTest {

    private lateinit var mockWebServer: MockWebServer

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
    fun `sendBiCsvFile sends PUT request with correct method, path, auth, and body`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        val env =
            EspooBiEnv(
                url = mockWebServer.url("/").toString(),
                username = "testuser",
                password = Sensitive("testpass"),
            )
        val client = EspooBiHttpClient(env)

        val csvContent = "header1,header2\nvalue1,value2\n"
        val csvStream = EspooBiJob.CsvInputStream(StandardCharsets.UTF_8, sequenceOf(csvContent))

        client.sendBiCsvFile("test-report.csv", csvStream)

        val recorded = mockWebServer.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/report?filename=test-report.csv", recorded.path)
        assertEquals(Credentials.basic("testuser", "testpass"), recorded.getHeader("Authorization"))
        assertEquals("text/csv", recorded.getHeader("Content-Type"))
        assertEquals(csvContent, recorded.body.readUtf8())
    }
}
