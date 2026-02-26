// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import java.util.UUID
import kotlin.test.assertEquals
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

class ServerSmokeTest : FullApplicationTest(resetDbBeforeEach = false) {
    private val http = OkHttpClient()

    @Test
    fun `test server startup`() {
        val response =
            http
                .newCall(Request.Builder().url("http://localhost:$httpPort/health").build())
                .execute()
        JSONAssert.assertEquals(
            // language=json
            """
{
    "status": "UP"
}
        """,
            response.body.string(),
            false,
        )
    }

    @Test
    fun `a valid JWT token and user header are required in API requests`() {
        val response =
            http
                .newCall(
                    Request.Builder().url("http://localhost:$httpPort/employee/daycares").build()
                )
                .execute()
        assertEquals(401, response.code)

        val user =
            AuthenticatedUser.Employee(
                EmployeeId(UUID.randomUUID()),
                setOf(UserRole.SERVICE_WORKER),
            )
        val response2 =
            http
                .newCall(
                    Request.Builder()
                        .url("http://localhost:$httpPort/employee/daycares")
                        .asUser(user)
                        .build()
                )
                .execute()
        assertEquals(200, response2.code)
    }
}
