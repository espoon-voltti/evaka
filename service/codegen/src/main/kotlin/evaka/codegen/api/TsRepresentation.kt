// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.ForceCodeGenType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.jvmName

sealed interface TsRepresentation<TypeArgs> {
    fun getTypeArgs(typeArgs: List<KTypeProjection>): TypeArgs =
        error("Type arguments are not supported")
}

/** A declared type that has a name and is exported by some file and can be imported by others. */
sealed interface TsNamedType<TypeArgs> : TsRepresentation<TypeArgs> {
    val clazz: KClass<*>
    val name: String
        get() = clazz.simpleName ?: error("no class name: $clazz")

    val source: String
        get() = clazz.qualifiedName ?: clazz.jvmName
}

data object Excluded : TsRepresentation<Nothing>

data class TsExternalTypeRef(
    val type: String,
    val keyRepresentation: TsCode?,
    val jsonDeserializeExpression: ((jsonExpr: String) -> TsCode)?,
    val imports: Set<TsImport>,
) : TsRepresentation<Nothing> {
    constructor(
        type: String,
        keyRepresentation: TsCode?,
        jsonDeserializeExpression: ((jsonExpr: String) -> TsCode)? = null,
        vararg imports: TsImport,
    ) : this(type, keyRepresentation, jsonDeserializeExpression, imports.toSet())
}

/** A plain TS type */
data class TsPlain(val type: String) : TsRepresentation<Nothing>

data object TsArray : TsRepresentation<KType?> {
    override fun getTypeArgs(typeArgs: List<KTypeProjection>): KType? {
        require(typeArgs.size == 1) { "Expected 1 type argument, got $typeArgs" }
        return typeArgs.single().type
    }
}

/** TS record with 2 type parameters: Record<K, V> */
data object TsRecord : TsRepresentation<Pair<KType?, KType?>> {
    override fun getTypeArgs(typeArgs: List<KTypeProjection>): Pair<KType?, KType?> {
        require(typeArgs.size == 2) { "Expected 2 type arguments, got $typeArgs" }
        return Pair(typeArgs[0].type, typeArgs[1].type)
    }
}

data class TsPlainObject(
    override val clazz: KClass<*>,
    override val name: String,
    val properties: Map<String, KType>
) : TsNamedType<List<KType?>> {
    constructor(
        clazz: KClass<*>
    ) : this(
        clazz,
        clazz.simpleName ?: error("no class name: $clazz"),
        clazz.declaredMemberProperties
            .filterNot {
                (it.findAnnotation<JsonIgnore>() ?: it.getter.findAnnotation<JsonIgnore>())
                    ?.value == true
            }
            .associate { prop ->
                prop.name to
                    (prop.javaField
                        ?.getAnnotation(ForceCodeGenType::class.java)
                        ?.type
                        ?.createType(nullable = prop.returnType.isMarkedNullable)
                        ?: prop.returnType)
            }
    )

    fun applyTypeArguments(arguments: List<KTypeProjection>): Map<String, KType> {
        val mapping = clazz.typeParameters.zip(arguments).toMap()
        return properties.mapValues { (_, propType) ->
            mapping[propType.classifier]?.type
                ?: propType.classifier?.createType(
                    propType.arguments.map { mapping[it.type?.classifier] ?: it }
                )
                ?: propType
        }
    }

    override fun getTypeArgs(typeArgs: List<KTypeProjection>): List<KType?> {
        require(typeArgs.size == clazz.typeParameters.size) {
            "Expected ${clazz.typeParameters.size} type arguments, got $typeArgs"
        }
        return typeArgs.map { it.type }
    }
}

/** Sealed class, represented as a union of several variants */
data class TsSealedClass(
    val obj: TsPlainObject,
    val variants: Set<KClass<*>>,
    val jacksonSerializer: TypeSerializer
) : TsNamedType<Nothing> {
    override val clazz: KClass<*> = obj.clazz
}

/** One variant of a sealed class, represented as a TS plain object */
data class TsSealedVariant(val parent: TsSealedClass, val obj: TsPlainObject) :
    TsRepresentation<Nothing> {
    val name: String = "${parent.name}.${obj.name}"
}

/** TS "string enum", e.g. 'A' | 'B' | 'C' */
data class TsStringEnum(
    override val clazz: KClass<*>,
    override val name: String,
    val values: List<String>,
    val constList: ConstList?
) : TsNamedType<Nothing> {
    constructor(
        clazz: KClass<*>
    ) : this(
        clazz,
        name = clazz.simpleName ?: error("no class name: $clazz"),
        values = clazz.java.enumConstants.map { it.toString() },
        constList = clazz.findAnnotation()
    )
}

/**
 * A fully specified TS type.
 *
 * For example, a Kotlin List<String>? could be represented by `TsType(TsArray, true,
 * listOf(KTypeProjection(type = String::class)))`
 */
data class TsType(
    val representation: TsRepresentation<*>,
    val isNullable: Boolean,
    val typeArguments: List<KTypeProjection>
)
