// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class ServerSmokeTest : FullApplicationTest() {
    @Test
    fun `test server startup`() {
        val (_, _, res) = http.get("/health").responseString()
        JSONAssert.assertEquals(
            // language=json
            """
{
    "status": "UP"
}
        """,
            res.get(), false
        )
    }
}
