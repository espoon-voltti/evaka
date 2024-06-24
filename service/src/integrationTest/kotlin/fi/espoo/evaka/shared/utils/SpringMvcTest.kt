// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class SpringMvcTest : FullApplicationTest(resetDbBeforeEach = false) {
    private val employee = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), emptySet())

    @Test
    fun `AuthenticatedUser as a parameter requires authentication`() {
        http
            .get("/integration-test/require-auth")
            .asUser(AuthenticatedUser.SystemInternalUser)
            .response()
            .let { (_, res, _) -> assertTrue(res.isSuccessful) }
        http
            .get("/integration-test/require-auth-employee")
            .asUser(AuthenticatedUser.SystemInternalUser)
            .response()
            .let { (_, res, _) -> assertEquals(401, res.statusCode) }

        http.get("/integration-test/require-auth").asUser(employee).response().let { (_, res, _) ->
            assertTrue(res.isSuccessful)
        }
        http.get("/integration-test/require-auth-employee").asUser(employee).response().let { (_, res, _) ->
            assertTrue(res.isSuccessful)
        }
    }
}
