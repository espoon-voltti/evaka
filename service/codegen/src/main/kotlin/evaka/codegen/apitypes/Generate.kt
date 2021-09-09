// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import fi.espoo.evaka.ExcludeCodeGen
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.exitProcess

private val header = """// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable prettier/prettier */

import { UUID } from '../types'
import LocalDate from '../local-date'
import FiniteDateRange from '../finite-date-range'
import DateRange from '../date-range'
import {DailyServiceTimes} from '../api-types/child/common'

"""

private val tsMapping = mapOf(
    "kotlin.String" to "string",
    "java.util.UUID" to "UUID",
    "fi.espoo.evaka.shared.Id" to "UUID",
    "kotlin.Int" to "number",
    "kotlin.Long" to "number",
    "kotlin.Double" to "number",
    "java.math.BigDecimal" to "number",
    "kotlin.Boolean" to "boolean",
    "java.time.LocalDate" to "LocalDate",
    "fi.espoo.evaka.shared.domain.FiniteDateRange" to "FiniteDateRange",
    "fi.espoo.evaka.shared.domain.DateRange" to "DateRange",
    "java.time.LocalTime" to "string",
    "java.time.LocalDateTime" to "Date",
    "fi.espoo.evaka.shared.domain.HelsinkiDateTime" to "Date",
    "java.time.Instant" to "Date",
    "java.time.OffsetDateTime" to "Date"
)

fun generateApiTypes() {
    val path = locateRoot() / "api-types.d.ts"
    path.writeText(getApiTypesInTypeScript())
}

fun getApiTypesInTypeScript(): String {
    val knownClasses = tsMapping.keys.toMutableSet()
    val analyzedClasses = mutableListOf<AnalyzedClass>()

    val waiting = ArrayDeque<KClass<*>>()
    waiting.addAll(getApiClasses("fi.espoo.evaka").filter { !knownClasses.contains(it.qualifiedName) })

    while (waiting.isNotEmpty()) {
        val analyzed = analyzeClass(waiting.removeFirst()) ?: continue
        analyzedClasses.add(analyzed)
        knownClasses.add(analyzed.name)
        if (analyzed is AnalyzedClass.DataClass) {
            analyzed.properties.forEach { prop ->
                if (!knownClasses.contains(prop.type.qualifiedName) && waiting.none { it == prop.type }) {
                    waiting.addLast(prop.type)
                }
            }
        }
    }

    return header + analyzedClasses.sortedBy { it.name }.joinToString("\n\n") { it.toTs() }
}

fun locateRoot(): Path {
    // the working directory is expected to be the "service" directory
    val workingDir = Path("")
    val path = (workingDir / "../frontend/src/lib-common/generated").absolute().normalize()
    if (!path.exists()) {
        exitProcess(1)
    }
    return path
}

private fun analyzeClass(clazz: KClass<*>): AnalyzedClass? {
    if ((clazz.allSupertypes.map { it.jvmErasure } + clazz).any { cls ->
        cls.annotations.any { it.annotationClass == ExcludeCodeGen::class }
    }
    ) {
        return null
    }

    if (clazz.qualifiedName?.startsWith("kotlin.") == true || clazz.qualifiedName?.startsWith("java.") == true)
        error("Kotlin/Java class ${clazz.qualifiedName} not handled, add to tsMapping")

    return when {
        clazz.java.enumConstants?.isNotEmpty() == true -> AnalyzedClass.EnumClass(
            name = clazz.qualifiedName ?: error("no class name"),
            values = clazz.java.enumConstants.map { it.toString() }
        )
        clazz.isData -> AnalyzedClass.DataClass(
            name = clazz.qualifiedName ?: error("no class name"),
            properties = clazz.declaredMemberProperties.map { analyzeMemberProperty(it) }
        )
        clazz.isSealed -> null // Not yet supported
        else -> error("unhandled case")
    }
}

private fun analyzeMemberProperty(prop: KProperty1<out Any, *>): AnalyzedProperty {
    return AnalyzedProperty(
        name = prop.name,
        type = if (isCollection(prop.returnType)) unwrapCollection(prop.returnType) else prop.returnType.jvmErasure,
        nullable = prop.returnType.isMarkedNullable,
        collection = isCollection(prop.returnType)
    )
}

private fun isCollection(type: KType): Boolean {
    return type.jvmErasure.isSubclassOf(Collection::class) || type.jvmErasure.isSubclassOf(Array::class) ||
        type.jvmErasure.isSubclassOf(IntArray::class) || type.jvmErasure.isSubclassOf(DoubleArray::class) ||
        type.jvmErasure.isSubclassOf(BooleanArray::class)
}

private fun unwrapCollection(type: KType): KClass<*> {
    return when (type) {
        IntArray::class -> Int::class
        DoubleArray::class -> Double::class
        BooleanArray::class -> Boolean::class
        else -> type.arguments.first().type!!.jvmErasure
    }
}

private sealed class AnalyzedClass(
    val name: String
) {
    abstract fun toTs(): String

    class DataClass(
        name: String,
        val properties: List<AnalyzedProperty>
    ) : AnalyzedClass(name) {
        override fun toTs(): String {
            return """/**
* Generated from $name
*/
export interface ${name.split('.').last()} {
${properties.joinToString("\n") { "    " + it.toTs() }}
}"""
        }
    }

    class EnumClass(
        name: String,
        val values: List<String>
    ) : AnalyzedClass(name) {
        override fun toTs(): String {
            return """/**
* Generated from $name
*/
export type ${name.split('.').last()} = ${values.joinToString(" | ") { "'$it'" }}"""
        }
    }
}

private data class AnalyzedProperty(
    val name: String,
    val type: KClass<*>,
    val nullable: Boolean,
    val collection: Boolean
) {
    fun toTs(): String {
        return "$name: ${tsMapping[type.qualifiedName] ?: type.simpleName}"
            .let { if (collection) "$it[]" else it }
            .let { if (nullable) "$it | null" else it }
    }
}
