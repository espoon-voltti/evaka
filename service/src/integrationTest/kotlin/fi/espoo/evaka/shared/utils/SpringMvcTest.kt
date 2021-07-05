// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class SpringMvcTest : FullApplicationTest() {
    private val employee = AuthenticatedUser.Employee(UUID.randomUUID(), emptySet())

    @Autowired
    private lateinit var controller: SpringMvcTestController

    @BeforeEach
    @AfterEach
    protected fun clearState() = controller.lastDbConnection.set(null)

    @Test
    fun `a database connection passed to a controller is closed automatically on success`() {
        val (_, res, _) = http.get("/integration-test/db-connection-pass").asUser(AuthenticatedUser.SystemInternalUser)
            .response()
        assertTrue(res.isSuccessful)
        val connection = controller.lastDbConnection.get()
        assertNotNull(connection)
        assertFalse(connection!!.isConnected())
    }

    @Test
    fun `a database connection passed to a controller is closed automatically on failure`() {
        val (_, res, _) = http.get("/integration-test/db-connection-fail").asUser(AuthenticatedUser.SystemInternalUser)
            .response()
        assertEquals(500, res.statusCode)
        val connection = controller.lastDbConnection.get()
        assertNotNull(connection)
        assertFalse(connection!!.isConnected())
    }

    @Test
    fun `AuthenticatedUser as a parameter requires authentication`() {
        http.get("/integration-test/require-auth").asUser(AuthenticatedUser.SystemInternalUser)
            .response()
            .let { (_, res, _) -> assertTrue(res.isSuccessful) }
        http.get("/integration-test/require-auth-employee").asUser(AuthenticatedUser.SystemInternalUser)
            .response()
            .let { (_, res, _) -> assertEquals(401, res.statusCode) }

        http.get("/integration-test/require-auth").asUser(employee)
            .response()
            .let { (_, res, _) -> assertTrue(res.isSuccessful) }
        http.get("/integration-test/require-auth-employee").asUser(employee)
            .response()
            .let { (_, res, _) -> assertTrue(res.isSuccessful) }
    }
}
