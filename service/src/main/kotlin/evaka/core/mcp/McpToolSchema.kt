// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.shared.Id
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/** Human-readable description for a tool input class or parameter, shown to the AI assistant */
@Target(AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpDoc(val value: String)

/**
 * Generates a JSON Schema (as plain maps/lists) from a Kotlin data class so that the same class can
 * be used both for documenting a tool's `inputSchema` and for parsing its arguments with Jackson.
 */
object McpToolSchema {
    fun forClass(clazz: KClass<*>): Map<String, Any> = objectSchema(clazz)

    private fun objectSchema(clazz: KClass<*>): Map<String, Any> {
        val constructor =
            clazz.primaryConstructor ?: error("${clazz.simpleName} has no primary constructor")
        val properties = linkedMapOf<String, Any>()
        val required = mutableListOf<String>()
        for (param in constructor.parameters) {
            val name = param.name ?: continue
            val schema = typeSchema(param.type).toMutableMap()
            param.findAnnotation<McpDoc>()?.let { schema["description"] = it.value }
            properties[name] = schema
            if (!param.isOptional && !param.type.isMarkedNullable) required += name
        }
        val result =
            linkedMapOf<String, Any>(
                "type" to "object",
                "properties" to properties,
                "additionalProperties" to false,
            )
        if (required.isNotEmpty()) result["required"] = required
        clazz.findAnnotation<McpDoc>()?.let { result["description"] = it.value }
        return result
    }

    private fun typeSchema(type: KType): Map<String, Any> {
        val clazz = type.jvmErasure
        return when {
            clazz == String::class -> mapOf("type" to "string")
            clazz == Boolean::class -> mapOf("type" to "boolean")
            clazz == Int::class || clazz == Long::class || clazz == Short::class ->
                mapOf("type" to "integer")
            clazz == Double::class || clazz == Float::class || clazz == BigDecimal::class ->
                mapOf("type" to "number")
            clazz == LocalDate::class ->
                mapOf("type" to "string", "format" to "date", "pattern" to "^\\d{4}-\\d{2}-\\d{2}$")
            clazz == LocalTime::class ->
                mapOf("type" to "string", "pattern" to "^\\d{2}:\\d{2}$", "description" to "HH:mm")
            clazz == UUID::class || clazz == Id::class ->
                mapOf("type" to "string", "format" to "uuid")
            clazz.java.isEnum ->
                mapOf(
                    "type" to "string",
                    "enum" to clazz.java.enumConstants.map { (it as Enum<*>).name },
                )
            clazz == List::class || clazz == Set::class || clazz == Collection::class -> {
                val itemType =
                    type.arguments.firstOrNull()?.type ?: error("Missing collection item type")
                mapOf("type" to "array", "items" to typeSchema(itemType))
            }
            clazz.isData -> objectSchema(clazz)
            else -> error("Unsupported tool parameter type: $type")
        }
    }
}
