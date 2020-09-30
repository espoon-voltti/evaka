// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.auth

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

// Custom serializer to avoid Jackson serializing "fields" that are actually helper functions (e.g. isAdmin)
class AuthenticatedUserJsonSerializer : JsonSerializer<AuthenticatedUser>() {
    override fun serialize(value: AuthenticatedUser, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeObjectField("id", value.id.toString())
        gen.writeObjectField("roles", value.roles)
        gen.writeEndObject()
    }
}
