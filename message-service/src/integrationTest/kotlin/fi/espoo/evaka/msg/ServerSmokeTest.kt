// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.msg

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.web.client.RestTemplate

class ServerSmokeTest : AbstractIntegrationTest() {
    @Test
    fun `test server startup`() {
        assert(port != 0)
        val health = RestTemplate().getForObject("http://localhost:$port/actuator/health", String::class.java)
        JSONAssert.assertEquals("""{"status": "UP"}""", health, false)
    }
}
