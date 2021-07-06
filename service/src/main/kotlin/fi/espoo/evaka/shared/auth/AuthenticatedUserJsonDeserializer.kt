// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.util.UUID

class AuthenticatedUserJsonDeserializer : JsonDeserializer<AuthenticatedUser>() {
    private data class AllFields(val type: AuthenticatedUserType, val id: UUID?, val globalRoles: Set<UserRole> = emptySet(), val allScopedRoles: Set<UserRole> = emptySet())

    override fun deserialize(p: JsonParser, ctx: DeserializationContext): AuthenticatedUser {
        val user = p.readValueAs(AllFields::class.java)
        return when (user.type) {
            AuthenticatedUserType.citizen -> AuthenticatedUser.Citizen(user.id!!)
            AuthenticatedUserType.citizen_weak -> AuthenticatedUser.WeakCitizen(user.id!!)
            AuthenticatedUserType.employee -> AuthenticatedUser.Employee(user.id!!, user.globalRoles + user.allScopedRoles)
            AuthenticatedUserType.mobile -> AuthenticatedUser.MobileDevice(user.id!!)
            AuthenticatedUserType.system -> AuthenticatedUser.SystemInternalUser
        }
    }
}
