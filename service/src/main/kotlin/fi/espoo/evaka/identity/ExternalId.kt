// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.identity

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter

/**
 * An identifier originating from an external system.
 *
 * eVaka doesn't internally understand the meaning of the value, so it can be anything.
 */
@JsonSerialize(converter = ExternalId.ToJson::class)
@JsonDeserialize(converter = ExternalId.FromJson::class)
data class ExternalId private constructor(
    val namespace: String,
    val value: String
) {
    override fun toString(): String = "$namespace:$value"

    companion object {
        fun of(
            namespace: String,
            value: String
        ): ExternalId {
            if (namespace.contains(':')) {
                throw IllegalArgumentException("Invalid external id namespace $namespace")
            }
            return ExternalId(namespace, value)
        }

        fun parse(text: String): ExternalId {
            val idx = text.indexOf(':')
            if (idx == -1) throw IllegalArgumentException("Invalid external id $text")
            return ExternalId(namespace = text.substring(0, idx), value = text.substring(idx + 1))
        }
    }

    class FromJson : StdConverter<String, ExternalId>() {
        override fun convert(value: String): ExternalId = parse(value)
    }

    class ToJson : StdConverter<ExternalId, String>() {
        override fun convert(value: ExternalId): String = value.toString()
    }
}
