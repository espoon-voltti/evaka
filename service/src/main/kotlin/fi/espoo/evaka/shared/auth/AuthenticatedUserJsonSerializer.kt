// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import fi.espoo.evaka.application.utils.exhaust
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer

// Custom serializer to avoid Jackson serializing "fields" that are actually helper functions (e.g.
// isAdmin)
class AuthenticatedUserJsonSerializer : ValueSerializer<AuthenticatedUser>() {
    override fun serialize(
        value: AuthenticatedUser?,
        gen: JsonGenerator?,
        ctxt: SerializationContext?,
    ) {
        if (value == null || gen == null) return

        gen.writeStartObject()
        gen.writePOJOProperty("type", value.type.toString())
        when (value) {
            is AuthenticatedUser.Citizen -> {
                gen.writePOJOProperty("id", value.id.toString())
            }

            is AuthenticatedUser.Employee -> {
                gen.writePOJOProperty("id", value.id.toString())
                gen.writePOJOProperty("globalRoles", value.globalRoles)
                gen.writePOJOProperty("allScopedRoles", value.allScopedRoles)
            }

            is AuthenticatedUser.MobileDevice -> {
                gen.writePOJOProperty("id", value.id.toString())
                value.employeeId?.let {
                    gen.writePOJOProperty("employeeId", value.employeeId.toString())
                }
            }

            is AuthenticatedUser.Integration -> {}

            is AuthenticatedUser.SystemInternalUser -> {}
        }.exhaust()
        gen.writeEndObject()
    }
}

class AuthenticatedUserJsonSerializerJackson2 :
    com.fasterxml.jackson.databind.JsonSerializer<AuthenticatedUser>() {
    override fun serialize(
        value: AuthenticatedUser,
        gen: com.fasterxml.jackson.core.JsonGenerator,
        serializers: com.fasterxml.jackson.databind.SerializerProvider,
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
