// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.apitypes

import evaka.codegen.fileHeader
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.ExcludeCodeGen
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.io.path.createDirectory
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.system.exitProcess

fun generateApiTypes() {
    val root = locateRoot()
    root.toFile().deleteRecursively()
    root.createDirectory()

    getApiTypesInTypeScript(root).entries.forEach { (path, content) ->
        path.writeText(content)
    }
}

fun getApiTypesInTypeScript(root: Path): Map<Path, String> {
    return analyzeClasses().entries.sortedBy { (name, _) -> name }.associate { (mainPackage, classes) ->
        val path = root / "$mainPackage.ts"
        val content = """$fileHeader
${getImports(classes).sorted().joinToString("\n")}

${classes.sortedBy { it.name.substringAfterLast('.') }.joinToString("\n\n") { it.toTs() }}
"""

        path to content
    }
}

private fun getImports(classes: List<AnalyzedClass>): List<String> {
    val classesToImport = classes
        .flatMap { if (it is AnalyzedClass.DataClass) it.properties else emptyList() }
        .flatMap { prop -> prop.type.declarableTypes.map { it.qualifiedName!! } }
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
            analyzed.properties.flatMap { it.type.declarableTypes }.forEach { type ->
                if (!knownClasses.contains(type.qualifiedName) && waiting.none { it == type }) {
                    waiting.addLast(type)
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
    return path / "api-types"
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
            values = clazz.java.enumConstants.map { it.toString() },
            constList = clazz.java.annotations
                .find { it.annotationClass == ConstList::class }
                ?.let { it as? ConstList }
                ?.name
        )
        clazz.isData -> AnalyzedClass.DataClass(
            name = clazz.qualifiedName ?: error("no class name"),
            properties = clazz.declaredMemberProperties.map { analyzeMemberProperty(it) }
        )
        clazz.isSealed -> null // Not yet supported
        else -> error("unhandled case")
    }
}

private fun analyzeMemberProperty(prop: KProperty1<out Any, *>) =
    AnalyzedProperty(prop.name, analyzeType(prop.returnType))

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
${properties.joinToString("\n") { "  " + it.toTs() }}
}"""
        }
    }

    class EnumClass(
        name: String,
        val values: List<String>,
        val constList: String?
    ) : AnalyzedClass(name) {
        override fun toTs(): String {
            val doc = """/**
* Generated from $name
*/"""
            if (constList != null) return doc + """
export const $constList = [
${values.joinToString(",\n") { "  '$it'" }}
] as const

export type ${name.split('.').last()} = typeof $constList[number]"""

            return doc + """
export type ${name.split('.').last()} = 
${values.joinToString("\n") { "  | '$it'" }}"""
        }
    }
}

data class AnalyzedProperty(
    val name: String,
    val type: AnalyzedType
) {
    fun toTs() = "$name: ${type.toTs()}"
}

fun analyzeType(type: KType): AnalyzedType = when {
    isMap(type) -> TsMap(type)
    isCollection(type) -> TsArray(type)
    else -> TsPlain(type)
}

sealed interface AnalyzedType {
    val declarableTypes: List<KClass<*>>
    fun toTs(): String
}

data class TsPlain(val type: KType) : AnalyzedType {
    override val declarableTypes = listOf(type.jvmErasure)
    override fun toTs() = toTs(type)
}

data class TsArray(val type: KType) : AnalyzedType {
    private val typeParameter = unwrapCollection(type)
    override val declarableTypes = listOf(typeParameter.jvmErasure)
    override fun toTs() = toTs(typeParameter)
        .let { if (typeParameter.isMarkedNullable) "($it)" else it }
        .let { "$it[]" }
        .let { if (type.isMarkedNullable) "$it | null" else it }

    private fun unwrapCollection(type: KType): KType {
        return when (type) {
            IntArray::class -> Int::class.createType()
            DoubleArray::class -> Double::class.createType()
            BooleanArray::class -> Boolean::class.createType()
            else -> type.arguments.first().type!!
        }
    }
}

data class TsMap(val type: KType) : AnalyzedType {
    private val keyType: KType = run {
        val keyType = type.arguments[0].type!!
        val isEnumType = keyType.jvmErasure.java.enumConstants?.isNotEmpty() ?: false
        if (validMapKeyTypes.none { it == keyType.jvmErasure } && !isEnumType) {
            // Key is not an enum or an allowed type
            error("Unsupported Map key type $keyType")
        }

        if (isEnumType) keyType else String::class.createType()
    }
    private val valueType = analyzeType(type.arguments[1].type!!)

    override val declarableTypes = valueType.declarableTypes + keyType.jvmErasure
    override fun toTs(): String = "Record<${toTs(keyType)}, ${valueType.toTs()}>"
        .let { if (type.isMarkedNullable) "$it | null" else it }
}

private fun toTs(type: KType): String {
    val className = tsMapping[type.jvmErasure.qualifiedName]?.type ?: type.jvmErasure.simpleName!!
    return if (type.isMarkedNullable) "$className | null" else className
}

private fun isMap(type: KType): Boolean {
    return type.jvmErasure.isSubclassOf(Map::class)
}

private fun isCollection(type: KType): Boolean {
    return kotlinCollectionClasses.any { type.jvmErasure.isSubclassOf(it) }
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
