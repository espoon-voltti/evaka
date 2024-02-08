// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.starProjectedType

/** Immutable metadata about Kotlin classes and their TS representations */
@JvmInline
value class TypeMetadata(val tsRepresentationMap: Map<KClass<*>, TsRepresentation>) {
    constructor(vararg classes: Pair<KClass<*>, TsRepresentation>) : this(classes.toMap())

    operator fun get(clazz: KClass<*>) = tsRepresentationMap[clazz]

    operator fun get(type: KType) = type.classifier?.let { tsRepresentationMap[it] }

    operator fun plus(other: TypeMetadata) =
        TypeMetadata(this.tsRepresentationMap + other.tsRepresentationMap)

    fun namedTypes(): List<TsNamedType> =
        tsRepresentationMap.values.mapNotNull { it as? TsNamedType }
}

/**
 * Discovers metadata starting from the given root types and recursively discovering all related
 * types.
 */
fun discoverMetadata(initial: TypeMetadata, rootTypes: Sequence<KType>): TypeMetadata {
    val tsReprMap = initial.tsRepresentationMap.toMutableMap()
    val analyzedTypes: MutableSet<KType> = mutableSetOf()

    fun createTsRepr(clazz: KClass<*>): TsRepresentation {
        if (
            clazz.qualifiedName?.startsWith("fi.espoo.") != true &&
                clazz.qualifiedName?.startsWith("evaka.") != true
        ) {
            error("Unsupported $clazz")
        }
        if (clazz.hasAnnotation<ExcludeCodeGen>()) return Excluded

        val parentSealedClass =
            clazz.allSuperclasses.firstNotNullOfOrNull { tsReprMap[it] as? TsSealedClass }
        return when {
            parentSealedClass != null -> TsSealedVariant(parentSealedClass, TsPlainObject(clazz))
            clazz.isData -> TsPlainObject(clazz)
            clazz.java.isEnum -> TsStringEnum(clazz)
            clazz.isSealed -> {
                val serializer =
                    typeSerializerFor(clazz) ?: error("No Jackson serializer found for $clazz")
                TsSealedClass(TsPlainObject(clazz), clazz.sealedSubclasses.toSet(), serializer)
            }
            else -> error("Unsupported $clazz")
        }
    }

    fun KType.discover() {
        val clazz = classifier as? KClass<*> ?: return
        if (!analyzedTypes.add(this)) return
        val tsRepr = tsReprMap.getOrPut(clazz) { createTsRepr(clazz) }

        fun discoverProperties(obj: TsPlainObject) {
            obj.applyTypeArguments(arguments).values.forEach { it.discover() }
        }

        fun discoverTypeParameters() = arguments.forEach { it.type?.discover() }

        fun TsNamedType.discoverRelatedTypes() =
            when (this) {
                is TsPlainObject -> discoverProperties(this)
                is TsSealedClass -> {
                    discoverProperties(obj)
                    variants.forEach { it.starProjectedType.discover() }
                }
                is TsStringEnum -> {}
            }

        when (tsRepr) {
            is TsNamedType -> tsRepr.discoverRelatedTypes()
            is TsSealedVariant -> discoverProperties(tsRepr.obj)
            is TsArray -> {
                require(arguments.size == 1) { "Expected 1 type argument, got $this" }
                discoverTypeParameters()
            }
            is TsRecord -> {
                require(arguments.size == 2) { "Expected 2 type arguments, got $this" }
                discoverTypeParameters()
            }
            is Excluded,
            is TsPlain,
            is TsExternalTypeRef -> {}
        }
    }
    rootTypes.forEach { it.discover() }

    return TypeMetadata(tsReprMap)
}

private val jsonMapper = defaultJsonMapperBuilder().build()

private fun typeSerializerFor(clazz: KClass<*>): TypeSerializer? =
    jsonMapper.serializerProviderInstance.findTypeSerializer(
        jsonMapper.typeFactory.constructType(clazz.java)
    )

fun TypeSerializer.discriminantValue(clazz: KClass<*>): String =
    typeIdResolver.idFromValueAndType(null, clazz.java)
