// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.kittinunf.fuel.core.Client
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.requests.DefaultBody
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import fi.espoo.evaka.Sensitive
import fi.espoo.evaka.VardaEnv
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.`when`
import java.io.ByteArrayInputStream
import java.net.URL
import kotlin.test.assertTrue

class MockVardaTokenProvider : VardaTokenProvider {
    override fun <T> withToken(action: (token: String, refresh: () -> String) -> T): T =
        action("initial") { "refreshed" }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VardaClientTest {
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private lateinit var mockTokenProvider: VardaTokenProvider

    @BeforeAll
    fun beforeAll() {
        mockTokenProvider = MockVardaTokenProvider()
    }

    @Test
    fun `refreshes API token on invalid token error and retries request`() {
        val fuel = FuelManager()

        fuel.client = mock<Client>()
        `when`(fuel.client.executeRequest(any()))
            .thenReturn(
                Response(
                    statusCode = 403,
                    url = URL("https://example.com"),
                    body = DefaultBody.from(
                        { ByteArrayInputStream("""{ "errors": [{ "error_code": "PE007" }] }""".toByteArray()) },
                        null
                    )
                ) // This should trigger the token refresh
            )
            .thenReturn(Response(statusCode = 204, url = URL("https://example.com")))

        val client = VardaClient(
            mockTokenProvider, fuel, objectMapper,
            VardaEnv(
                organizer = "",
                url = "https://example.com/mock-integration/varda/api",
                basicAuth = Sensitive(""),
                sourceSystem = "SourceSystemVarda"
            )
        )

        assertTrue(client.deleteFeeData(0))
        // NOTE: As the original Request is modified instead of replaced in a retry
        // the _new_ authorization header appears to be used twice.
        verify(
            fuel.client,
            times(2)
        ).executeRequest(argThat { this[Headers.AUTHORIZATION].first() == "Token refreshed" })
    }
}
