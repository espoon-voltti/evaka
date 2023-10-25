// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef

@JvmInline
value class JsonWriter<T> private constructor(private val writer: ObjectWriter) {
    constructor(
        jsonMapper: JsonMapper,
        rootType: TypeReference<T>
    ) : this(jsonMapper.writerFor(rootType))

    fun writeValueAsString(value: T): String = writer.writeValueAsString(value)

    fun writeValueAsBytes(value: T): ByteArray = writer.writeValueAsBytes(value)
}

/**
 * Returns a specialized JSON writer, which retains exact type information and any
 * JSON-serialization configuration for the type.
 *
 * Just using `JsonMapper.writeValueAsString(someValue)` may lose type information and serialize
 * values incorrectly, but `JsonMapper.writerFor<SomeType>().writeValueAsString(someValue)` fixes
 * this problem.
 */
inline fun <reified T> JsonMapper.writerFor(): JsonWriter<T> = JsonWriter(this, jacksonTypeRef<T>())
