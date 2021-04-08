// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.util.UUID

// Custom deserializer to temporarily support legacy JSON
class AuthenticatedUserJsonDeserializer : JsonDeserializer<AuthenticatedUser>() {
    private data class AllFields(val type: String?, val id: UUID?, val roles: Set<UserRole> = emptySet(), val globalRoles: Set<UserRole> = emptySet(), val allScopedRoles: Set<UserRole> = emptySet())

    override fun deserialize(p: JsonParser, ctx: DeserializationContext): AuthenticatedUser {
        val user = p.readValueAs(AllFields::class.java)
        return when (user.type) {
            "citizen" -> AuthenticatedUser.Citizen(user.id!!)
            "employee" -> AuthenticatedUser.Employee(user.id!!, user.globalRoles + user.allScopedRoles)
            "mobile" -> AuthenticatedUser.MobileDevice(user.id!!)
            "system" -> AuthenticatedUser.SystemInternalUser
            else -> {
                when {
                    user.id == AuthenticatedUser.SystemInternalUser.id ->
                        AuthenticatedUser.SystemInternalUser
                    user.roles.contains(UserRole.END_USER) ->
                        AuthenticatedUser.Citizen(user.id!!)
                    user.roles.contains(UserRole.MOBILE) ->
                        AuthenticatedUser.MobileDevice(user.id!!)
                    else ->
                        AuthenticatedUser.Employee(user.id!!, user.roles)
                }
            }
        }
    }
}
