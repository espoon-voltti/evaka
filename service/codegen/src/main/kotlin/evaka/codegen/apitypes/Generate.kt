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

fun generateApiTypes() {
    getApiTypesInTypeScript().entries.forEach { (path, content) ->
        path.writeText(content)
    }
}

fun getApiTypesInTypeScript(): Map<Path, String> {
    val root = locateRoot()
    return analyzeClasses().entries.associate { (mainPackage, classes) ->
        val path = root / "api-types" / "$mainPackage.d.ts"
        val content = """$header
${getImports(classes).sorted().joinToString("\n")}

${classes.sortedBy { it.name.substringAfterLast('.') }.joinToString("\n\n") { it.toTs() }}
        """.trimMargin()

        path to content
    }
}

private fun getImports(classes: List<AnalyzedClass>): List<String> {
    val classesToImport = classes
        .flatMap { if (it is AnalyzedClass.DataClass) it.properties else emptyList() }
        .map { it.type.qualifiedName!! }
        .filter { classes.none { c -> c.name == it } }
        .toSet()

    return classesToImport.mapNotNull {
        if (tsMapping.containsKey(it)) {
            tsMapping[it]?.import
        } else {
            "import { ${it.substringAfterLast('.')} } from './${getBasePackage(it)}'"
        }
    }.distinct()
}

private fun analyzeClasses(): Map<String, List<AnalyzedClass>> {
    val knownClasses = tsMapping.keys.toMutableSet()
    val analyzedClasses = mutableListOf<AnalyzedClass>()

    val waiting = ArrayDeque<KClass<*>>()
    waiting.addAll(getApiClasses(basePackage).filter { !knownClasses.contains(it.qualifiedName) })

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

    return analyzedClasses.groupBy { getBasePackage(it.name) }
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
    return kotlinCollectionClasses.any { type.jvmErasure.isSubclassOf(it) }
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
        return "$name: ${tsMapping[type.qualifiedName]?.type ?: type.simpleName}"
            .let { if (collection) "$it[]" else it }
            .let { if (nullable) "$it | null" else it }
    }
}

private fun getBasePackage(fullyQualifiedName: String): String {
    val pkg = fullyQualifiedName.substringBeforeLast('.')
    val relativePackage = when {
        pkg == basePackage -> return "base"
        pkg.startsWith("$basePackage.") -> pkg.substring(basePackage.length + 1)
        else -> error("class not under base package")
    }
    return relativePackage.substringBefore('.')
}
