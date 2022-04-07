// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.github.kittinunf.fuel.core.isSuccessful
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

@TestPropertySource(properties = ["fi.espoo.voltti.vtj.xroad.trustStore.location=classpath:test-certificate/localhost.truststore"])
class VardaClientIntegrationTest : VardaIntegrationTest(resetDbBeforeEach = false) {
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
        val (_, res, _) = http.get("https://localhost:${httpsServer.app.port()}").response()
        assertTrue(res.isSuccessful)
    }
}
