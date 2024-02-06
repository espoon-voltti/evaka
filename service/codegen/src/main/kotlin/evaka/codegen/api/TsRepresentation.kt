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

sealed interface TsRepresentation

/** A declared type that has a name and is exported by some file and can be imported by others. */
sealed interface TsNamedType : TsRepresentation {
    val clazz: KClass<*>
    val name: String
        get() = clazz.simpleName ?: error("no class name: $clazz")

    val source: String
        get() = clazz.qualifiedName ?: clazz.jvmName
}

data object Excluded : TsRepresentation

data class TsExternalTypeRef(
    val type: String,
    val keyRepresentation: TsCode?,
    val jsonDeserializeExpression: ((jsonExpr: String) -> TsCode)?,
    val imports: Set<TsImport>,
) : TsRepresentation {
    constructor(
        type: String,
        keyRepresentation: TsCode?,
        jsonDeserializeExpression: ((jsonExpr: String) -> TsCode)? = null,
        vararg imports: TsImport,
    ) : this(type, keyRepresentation, jsonDeserializeExpression, imports.toSet())
}

/** A plain TS type */
data class TsPlain(val type: String) : TsRepresentation

data object TsArray : TsRepresentation

data class TsPlainObject(
    override val clazz: KClass<*>,
    override val name: String,
    val properties: Map<String, KType>
) : TsRepresentation, TsNamedType {
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
}

/** Sealed class, represented as a union of several variants */
data class TsSealedClass(
    val obj: TsPlainObject,
    val variants: Set<KClass<*>>,
    val jacksonSerializer: TypeSerializer
) : TsRepresentation, TsNamedType {
    override val clazz: KClass<*> = obj.clazz
}

/** One variant of a sealed class, represented as a TS plain object */
data class TsSealedVariant(val parent: TsSealedClass, val obj: TsPlainObject) : TsRepresentation

/** TS "string enum", e.g. 'A' | 'B' | 'C' */
data class TsStringEnum(
    override val clazz: KClass<*>,
    override val name: String,
    val values: List<String>,
    val constList: ConstList?
) : TsRepresentation, TsNamedType {
    constructor(
        clazz: KClass<*>
    ) : this(
        clazz,
        name = clazz.simpleName ?: error("no class name: $clazz"),
        values = clazz.java.enumConstants.map { it.toString() },
        constList = clazz.findAnnotation()
    )
}

/** TS record with 2 type parameters: Record<K, V> */
data object TsRecord : TsRepresentation
