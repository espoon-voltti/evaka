// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import com.github.kittinunf.fuel.core.isSuccessful
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.asUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SpringMvcTest : FullApplicationTest() {
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
}
