// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.shared.config.defaultObjectMapper
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

class AuthenticatedUserTest {
    private val id = UUID.fromString("e4d7a125-2b2c-465c-994f-38c2915534a6")

    @Test
    fun `legacy authenticated citizens can be deserialized`() {
        val json = """
            {
                "id": "$id",
                "roles": ["END_USER"]
            }
        """.trimIndent()
        val user = defaultObjectMapper().readValue(json, AuthenticatedUser::class.java)
        assertEquals(AuthenticatedUser.Citizen(id), user)
    }

    @Test
    fun `legacy authenticated system user can be deserialized`() {
        val json = """
            {
                "id": "${AuthenticatedUser.SystemInternalUser.id}",
                "roles": []
            }
        """.trimIndent()
        val user = defaultObjectMapper().readValue(json, AuthenticatedUser::class.java)
        assertEquals(AuthenticatedUser.SystemInternalUser, user)
    }

    @Test
    fun `legacy authenticated mobile device can be deserialized`() {
        val json = """
            {
                "id": "$id",
                "roles": ["MOBILE"]
            }
        """.trimIndent()
        val user = defaultObjectMapper().readValue(json, AuthenticatedUser::class.java)
        assertEquals(AuthenticatedUser.MobileDevice(id), user)
    }

    @Test
    fun `legacy authenticated employee can be deserialized`() {
        val json = """
            {
                "id": "$id",
                "roles": ["SERVICE_WORKER"]
            }
        """.trimIndent()
        val user = defaultObjectMapper().readValue(json, AuthenticatedUser::class.java)
        assertEquals(AuthenticatedUser.Employee(id, setOf(UserRole.SERVICE_WORKER)), user)
    }
}
