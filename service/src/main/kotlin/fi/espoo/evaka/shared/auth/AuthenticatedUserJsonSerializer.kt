// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import fi.espoo.evaka.application.utils.exhaust

// Custom serializer to avoid Jackson serializing "fields" that are actually helper functions (e.g.
// isAdmin)
class AuthenticatedUserJsonSerializer : JsonSerializer<AuthenticatedUser>() {
    override fun serialize(
        value: AuthenticatedUser,
        gen: JsonGenerator,
        serializers: SerializerProvider,
    ) {
        gen.writeStartObject()
        gen.writeObjectField("type", value.type.toString())
        when (value) {
            is AuthenticatedUser.Citizen -> {
                gen.writeObjectField("id", value.id.toString())
            }
            is AuthenticatedUser.Employee -> {
                gen.writeObjectField("id", value.id.toString())
                gen.writeObjectField("globalRoles", value.globalRoles)
                gen.writeObjectField("allScopedRoles", value.allScopedRoles)
            }
            is AuthenticatedUser.MobileDevice -> {
                gen.writeObjectField("id", value.id.toString())
                value.employeeId?.let {
                    gen.writeObjectField("employeeId", value.employeeId.toString())
                }
            }
            is AuthenticatedUser.Integration -> {}
            is AuthenticatedUser.SystemInternalUser -> {}
        }.exhaust()
        gen.writeEndObject()
    }
}
