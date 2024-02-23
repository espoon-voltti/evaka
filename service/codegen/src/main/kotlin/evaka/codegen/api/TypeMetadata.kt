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
value class TypeMetadata(val tsRepresentationMap: Map<KClass<*>, TsRepresentation<*>>) {
    constructor(vararg classes: Pair<KClass<*>, TsRepresentation<*>>) : this(classes.toMap())

    operator fun contains(clazz: KClass<*>) = tsRepresentationMap.contains(clazz)

    operator fun get(clazz: KClass<*>) = tsRepresentationMap[clazz]

    operator fun get(type: KType) = type.classifier?.let { tsRepresentationMap[it] }

    operator fun plus(other: TypeMetadata) =
        TypeMetadata(this.tsRepresentationMap + other.tsRepresentationMap)

    operator fun minus(classes: Set<KClass<*>>) = TypeMetadata(this.tsRepresentationMap - classes)
}

/**
 * Discovers metadata starting from the given root types and recursively discovering all related
 * types.
 */
fun discoverMetadata(initial: TypeMetadata, rootTypes: Sequence<KType>): TypeMetadata {
    val tsReprMap = initial.tsRepresentationMap.toMutableMap()
    val analyzedTypes: MutableSet<KType> = mutableSetOf()

    fun createTsRepr(clazz: KClass<*>): TsRepresentation<*> {
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
            clazz.java.isEnum -> TsStringEnum(clazz)
            clazz.isSealed -> {
                val serializer =
                    typeSerializerFor(clazz) ?: error("No Jackson serializer found for $clazz")
                TsSealedClass(TsPlainObject(clazz), clazz.sealedSubclasses.toSet(), serializer)
            }
            else -> TsPlainObject(clazz)
        }
    }

    fun KType.discover() {
        fun TsType.discover(): Unit =
            when (representation) {
                is TsArray,
                is TsRecord,
                is TsTuple -> typeArguments.forEach { it.type?.discover() }
                is TsPlainObject ->
                    representation.applyTypeArguments(typeArguments).values.forEach {
                        it.discover()
                    }
                is TsSealedVariant ->
                    TsType(representation.obj, isNullable = false, typeArguments = emptyList())
                        .discover()
                is TsSealedClass -> {
                    TsType(representation.obj, isNullable = false, typeArguments = emptyList())
                        .discover()
                    representation.variants.forEach { it.starProjectedType.discover() }
                }
                is TsObjectLiteral ->
                    representation.properties.values.forEach { it.type.discover() }
                is TsStringEnum,
                is Excluded,
                is TsPlain,
                is TsExternalTypeRef -> {}
            }

        try {
            val clazz = classifier as? KClass<*> ?: return
            if (!analyzedTypes.add(this)) return
            val tsRepr = tsReprMap.getOrPut(clazz) { createTsRepr(clazz) }

            TsType(tsRepr, isMarkedNullable, arguments).discover()
        } catch (e: Exception) {
            throw RuntimeException("Failed to discover type $this", e)
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
