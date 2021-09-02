// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import java.util.UUID
import kotlin.test.assertEquals

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

    @Test
    fun `a valid JWT token is required in API requests`() {
        val (_, res, _) = http.get("/daycares").responseString()
        assertEquals(401, res.statusCode)

        val user = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.SERVICE_WORKER))
        val (_, res2, _) = http.get("/daycares").asUser(user).responseString()
        assertEquals(200, res2.statusCode)
    }
}
