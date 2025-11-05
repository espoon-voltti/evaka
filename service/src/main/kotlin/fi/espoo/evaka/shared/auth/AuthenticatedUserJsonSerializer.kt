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
        value: AuthenticatedUser,
        gen: JsonGenerator,
        serializers: SerializationContext,
    ) {
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
