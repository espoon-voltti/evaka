// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core

import evaka.core.shared.EmployeeId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.auth.asUser
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
    fun `requests from browsers configured with many languages are accepted`() {
        val acceptLanguage =
            "fi-FI,fi;q=0.9,sv-FI;q=0.8,sv;q=0.7,en-US;q=0.6,en-GB;q=0.5,en;q=0.4," +
                "de-DE;q=0.3,de;q=0.2,et-EE;q=0.1,et;q=0.1,ru;q=0.1"
        val response =
            http
                .newCall(
                    Request.Builder()
                        .url("http://localhost:$httpPort/health")
                        .header("Accept-Language", acceptLanguage)
                        .build()
                )
                .execute()
        assertEquals(200, response.code)
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
