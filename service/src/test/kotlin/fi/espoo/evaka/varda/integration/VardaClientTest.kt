// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.requests.DefaultBody
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDate
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class MockVardaTokenProvider : VardaTokenProvider {
    override fun <T> withToken(action: (token: String, refresh: () -> String) -> T): T =
        action("initial") { "refreshed" }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VardaClientTest {
    private val jsonMapper: JsonMapper = defaultJsonMapperBuilder().build()
    private lateinit var mockTokenProvider: VardaTokenProvider

    @BeforeAll
    fun beforeAll() {
        mockTokenProvider = MockVardaTokenProvider()
    }

    @Test
    fun `refreshes API token on invalid token error and retries request`() {
        val fuel = FuelManager()

        fuel.client = mock()
        `when`(fuel.client.executeRequest(any()))
            .thenReturn(
                Response(
                    statusCode = 403,
                    url = URI("https://example.com").toURL(),
                    body =
                        DefaultBody.from(
                            {
                                ByteArrayInputStream(
                                    """{ "errors": [{ "error_code": "PE007" }] }""".toByteArray()
                                )
                            },
                            null
                        )
                ) // This should trigger the token refresh
            )
            .thenReturn(Response(statusCode = 204, url = URI("https://example.com").toURL()))

        val client =
            VardaClient(
                mockTokenProvider,
                fuel,
                jsonMapper,
                VardaEnv(
                    url = URI.create("https://example.com/mock-integration/varda/api"),
                    basicAuth = Sensitive(""),
                    sourceSystem = "SourceSystemVarda",
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = null,
                    localDevPort = null
                )
            )

        assertTrue(client.deleteFeeData(0))
        // NOTE: As the original Request is modified instead of replaced in a retry
        // the _new_ authorization header appears to be used twice.
        verify(fuel.client, times(2))
            .executeRequest(argThat { this[Headers.AUTHORIZATION].first() == "Token refreshed" })
    }
}
