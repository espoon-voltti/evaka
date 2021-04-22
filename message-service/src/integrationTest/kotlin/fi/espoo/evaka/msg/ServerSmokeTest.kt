// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForEntity

class ServerSmokeTest : AbstractIntegrationTest() {
    @Test
    fun `test server startup`() {
        assert(port != 0)
        val health = RestTemplate().getForObject("http://localhost:$port/actuator/health", String::class.java)
        JSONAssert.assertEquals("""{"status": "UP"}""", health, false)
    }

    @Test
    fun `a valid JWT token is required in API requests`() {
        assert(port != 0)
        assertThrows<HttpClientErrorException.Unauthorized> {
            RestTemplate().postForEntity<Unit>("http://localhost:$port/message/send", mapOf<String, String>())
        }
    }
}
