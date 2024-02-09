// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

abstract class TsCodeGenerator(private val metadata: TypeMetadata) {
    abstract fun locateNamedType(namedType: TsNamedType<*>): TsFile

    private fun typeRef(namedType: TsNamedType<*>): TsImport =
        TsImport.Named(locateNamedType(namedType), namedType.name)

    private fun typeRef(sealedVariant: TsSealedVariant): TsImport =
        TsImport.Named(locateNamedType(sealedVariant.parent), sealedVariant.name)

    private fun deserializerRef(namedType: TsNamedType<*>): TsImport =
        TsImport.Named(locateNamedType(namedType), "deserializeJson${namedType.name}")

    private fun deserializerRef(sealedVariant: TsSealedVariant): TsImport =
        TsImport.Named(
            locateNamedType(sealedVariant.parent),
            "deserializeJson${sealedVariant.parent.name}${sealedVariant.obj.name}"
        )

    fun arrayType(elementType: KType?): TsCode {
        val elementTs =
            if (elementType == null) TsCode("never")
            else
                tsType(elementType).let {
                    if (elementType.isMarkedNullable) it.copy(text = "(${it.text})") else it
                }
        return elementTs + TsCode("[]")
    }

    fun recordType(type: Pair<KType?, KType?>): TsCode {
        val keyTs = type.first?.let(::keyType) ?: TsCode("never")
        val valueTs = type.second?.let(this::tsType) ?: TsCode("never")
        return TsCode { ts("Record<${inline(keyTs)}, ${inline(valueTs)}>") }
    }

    private fun typeToTsCode(type: KType, f: (tsType: TsType) -> TsCode): TsCode =
        when (val clazz = type.classifier) {
            is KClass<*> ->
                f(
                    TsType(
                        metadata[clazz] ?: error("No TS type found for $type"),
                        type.isMarkedNullable,
                        type.arguments
                    )
                )
            is KTypeParameter -> TsCode(clazz.name)
            // Not possible, but KClassifier is not a sealed interface so can't be proven at compile
            // time
            else -> error("Unsupported classifier")
        }

    fun keyType(type: KType): TsCode = typeToTsCode(type, ::keyType)

    fun keyType(tsType: TsType): TsCode =
        when (val tsRepr = tsType.representation) {
            is TsPlain -> TsCode(tsRepr.type)
            is TsStringEnum -> TsCode(typeRef(tsRepr))
            is TsExternalTypeRef -> tsRepr.keyRepresentation
            is Excluded,
            is TsArray,
            is TsRecord,
            is TsPlainObject,
            is TsSealedClass,
            is TsSealedVariant -> null
        }?.takeUnless { tsType.isNullable } ?: error("$tsType is not supported as a key type")

    fun tsType(type: KType): TsCode = typeToTsCode(type, ::tsType)

    fun tsType(tsType: TsType): TsCode =
        when (val tsRepr = tsType.representation) {
            is TsPlain -> TsCode(tsRepr.type)
            is TsArray -> arrayType(tsRepr.getTypeArgs(tsType.typeArguments))
            is TsRecord -> recordType(tsRepr.getTypeArgs(tsType.typeArguments))
            is TsPlainObject -> {
                val typeArguments =
                    tsRepr.getTypeArgs(tsType.typeArguments).map {
                        it?.let(::tsType) ?: TsCode("never")
                    }
                TsCode(typeRef(tsRepr)) +
                    if (typeArguments.isEmpty()) TsCode("")
                    else TsCode.join(typeArguments, separator = ", ", prefix = "<", postfix = ">")
            }
            is TsSealedClass -> TsCode(typeRef(tsRepr))
            is TsStringEnum -> TsCode(typeRef(tsRepr))
            is TsSealedVariant -> TsCode(typeRef(tsRepr))
            is TsExternalTypeRef -> TsCode(tsRepr.type, tsRepr.imports)
            is Excluded -> TsCode("never")
        }.let { if (tsType.isNullable) it + TsCode(" | null") else it }

    fun stringEnum(enum: TsStringEnum): TsCode = TsCode {
        if (enum.constList != null)
            ts(
                """${enum.docHeader()}
export const ${enum.constList.name} = [
${enum.values.joinToString(",\n") { "'$it'" }.prependIndent("  ")}
] as const

export type ${enum.name} = typeof ${enum.constList.name}[number]"""
            )
        else
            ts(
                """${enum.docHeader()}
export type ${enum.name} =
${enum.values.joinToString("\n") { "| '$it'" }.prependIndent("  ")}"""
            )
    }

    fun tsPlainObject(obj: TsPlainObject): TsCode {
        val typeParams =
            if (obj.clazz.typeParameters.isNotEmpty())
                obj.clazz.typeParameters.joinToString(",", prefix = "<", postfix = ">")
            else ""
        val props =
            obj.properties.entries
                .sortedBy { it.key }
                .map { (name, type) ->
                    val tsRepr = tsType(type)
                    TsCode { ts("$name: ${inline(tsRepr)}") }
                }
        return TsCode {
            ts(
                """${obj.docHeader()}
export interface ${obj.name}$typeParams {
${join(props, "\n").prependIndent("  ")}
}"""
            )
        }
    }

    fun sealedClass(sealed: TsSealedClass): TsCode {
        val serializer = sealed.jacksonSerializer
        val variants = sealed.variants.map { metadata[it] as TsSealedVariant }
        val tsVariants =
            variants.map { variant ->
                val discriminantProp =
                    serializer.propertyName?.let {
                        TsCode("$it: '${serializer.discriminantValue(variant.obj.clazz)}'")
                    }
                val props =
                    listOfNotNull(discriminantProp) +
                        variant.obj.properties.entries
                            .sortedBy { it.key }
                            .map { (name, type) -> TsCode { ts("$name: ${inline(tsType(type))}") } }
                TsCode {
                    ts(
                        """${variant.obj.docHeader()}
export interface ${variant.obj.name} {
${join(props, "\n").prependIndent("  ")}
}"""
                    )
                }
            }
        return TsCode {
            ts(
                """
export namespace ${sealed.name} {
${join(tsVariants, "\n\n").prependIndent("  ")}
}

${sealed.docHeader()}
export type ${sealed.name} = ${variants.joinToString(separator = " | ") { "${sealed.name}.${it.obj.name}" }}
"""
            )
        }
    }

    fun namedType(namedType: TsNamedType<*>): TsCode =
        when (namedType) {
            is TsStringEnum -> stringEnum(namedType)
            is TsPlainObject -> tsPlainObject(namedType)
            is TsSealedClass -> sealedClass(namedType)
        }

    private fun needsJsonDeserializer(type: KType): Boolean {
        val cache = mutableMapOf<KType, Boolean>()

        fun check(type: KType): Boolean =
            cache.getOrPut(type) {
                cache[type] = false // prevent problems with recursion by assuming false by default
                when (val tsRepr = metadata[type] ?: error("No TS type found for $type")) {
                    is TsArray -> tsRepr.getTypeArgs(type.arguments)?.let { check(it) } ?: false
                    is TsRecord ->
                        tsRepr.getTypeArgs(type.arguments).second?.let { check(it) } ?: false
                    is TsPlainObject ->
                        tsRepr.applyTypeArguments(type.arguments).values.any { check(it) }
                    is TsSealedClass ->
                        tsRepr.variants.any { variant ->
                            (metadata[variant] as TsSealedVariant).obj.properties.values.any {
                                check(it)
                            }
                        }
                    is TsSealedVariant ->
                        tsRepr.obj.applyTypeArguments(type.arguments).values.any { check(it) }
                    is TsExternalTypeRef -> tsRepr.jsonDeserializeExpression != null
                    is Excluded,
                    is TsStringEnum,
                    is TsPlain -> false
                }
            }

        return check(type)
    }

    fun jsonDeserializer(namedType: TsNamedType<*>): TsCode? =
        when (namedType) {
            is TsStringEnum -> null
            is TsSealedClass -> {
                val variants =
                    namedType.variants
                        .mapNotNull {
                            val variant = metadata[it] as TsSealedVariant
                            val deserializer =
                                jsonObjectDeserializer(
                                    typeRef(variant),
                                    deserializerRef(variant),
                                    variant.obj.properties.toList()
                                )
                            if (deserializer != null) variant to deserializer else null
                        }
                        .toMap()
                val discriminantProp = namedType.jacksonSerializer.propertyName
                if (discriminantProp == null || variants.isEmpty())
                    null // TODO: how can we deserialize without a discriminant?
                else {
                    val cases =
                        variants.keys.joinToString("\n") { variant ->
                            val discriminant =
                                namedType.jacksonSerializer.discriminantValue(variant.obj.clazz)
                            "case '$discriminant': return deserializeJson${namedType.name}${variant.obj.name}(json)"
                        }
                    TsCode {
                        ts(
                            """
${join(variants.values, "\n")}
export function deserializeJson${namedType.name}(json: ${ref(Imports.jsonOf)}<${namedType.name}>): ${namedType.name} {
  switch (json.$discriminantProp) {
${cases.prependIndent("    ")}
    default: return json
  }
}"""
                        )
                    }
                }
            }
            is TsPlainObject ->
                if (namedType.clazz.typeParameters.isNotEmpty()) null
                else
                    jsonObjectDeserializer(
                        typeRef(namedType),
                        deserializerRef(namedType),
                        namedType.properties.toList()
                    )
        }

    private fun jsonObjectDeserializer(
        type: TsImport,
        function: TsImport,
        props: Iterable<Pair<String, KType>>
    ): TsCode? {
        val propDeserializers =
            props.mapNotNull { (name, type) ->
                jsonDeserializerExpression(type, "json.$name")?.let { name to it }
            }
        if (propDeserializers.isEmpty()) return null
        val propCodes =
            listOf(TsCode("...json")) +
                propDeserializers.map { (name, code) -> TsCode { ts("$name: ${inline(code)}") } }
        return TsCode {
            ts(
                """
export function ${ref(function)}(json: ${ref(Imports.jsonOf)}<${ref(type)}>): ${ref(type)} {
  return {
${join(propCodes, ",\n").prependIndent("    ")}
  }
}"""
            )
        }
    }

    fun jsonDeserializerExpression(type: KType, jsonExpression: String): TsCode? =
        if (!needsJsonDeserializer(type)) null
        else
            when (val tsRepr = metadata[type] ?: error("No TS type found for $type")) {
                is TsArray -> {
                    jsonDeserializerExpression(
                            requireNotNull(tsRepr.getTypeArgs(type.arguments)),
                            "e"
                        )
                        ?.let { it.copy(text = "$jsonExpression.map(e => ${it.text})") }
                }
                is TsRecord -> {
                    val valueDeser =
                        jsonDeserializerExpression(
                            requireNotNull(tsRepr.getTypeArgs(type.arguments).second),
                            "v"
                        )
                    if (valueDeser == null) null
                    else
                        TsCode {
                            ts(
                                """Object.fromEntries(Object.entries($jsonExpression).map(
  ([k, v]) => [k, ${inline(valueDeser)}]
))"""
                            )
                        }
                }
                is TsSealedVariant -> TODO()
                is TsPlainObject ->
                    TsCode { ts("${ref(deserializerRef(tsRepr))}($jsonExpression)") }
                is TsSealedClass ->
                    TsCode { ts("${ref(deserializerRef(tsRepr))}($jsonExpression)") }
                is TsExternalTypeRef -> tsRepr.jsonDeserializeExpression?.let { it(jsonExpression) }
                is Excluded,
                is TsPlain,
                is TsStringEnum -> null
            }?.let {
                if (type.isMarkedNullable)
                    it.copy(text = "($jsonExpression != null) ? ${it.text} : null")
                else it
            }
}

private fun TsNamedType<*>.docHeader() = """/**
* Generated from $source
*/"""
