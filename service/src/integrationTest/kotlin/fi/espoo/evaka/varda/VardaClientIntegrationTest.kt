// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.github.kittinunf.result.Result
import fi.espoo.evaka.FullApplicationTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

@TestPropertySource(properties = ["fi.espoo.voltti.vtj.xroad.trustStore.location=classpath:test-certificate/localhost.truststore"])
class VardaClientIntegrationTest : FullApplicationTest() {
    private lateinit var httpsServer: VardaClientIntegrationMockHttpsServer

    @BeforeAll
    fun initDependencies() {
        httpsServer = VardaClientIntegrationMockHttpsServer.start()
    }

    @AfterAll
    fun cleanup() {
        httpsServer.close()
    }

    @Test
    fun `certificate handling`() {
        val (_, _, result) = http.get("https://localhost:${httpsServer.app.port()}").responseString()
        when (result) {
            !is Result.Success -> {
                fail<Any>("Failed to fetch URL")
            }
        }
    }
}
