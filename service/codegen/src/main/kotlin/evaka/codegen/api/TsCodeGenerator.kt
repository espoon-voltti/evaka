// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.codegen.api

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

abstract class TsCodeGenerator(val metadata: TypeMetadata) {
    fun namedTypes(): List<TsNamedType<*>> =
        metadata.tsRepresentationMap.values.mapNotNull { it as? TsNamedType }

    abstract fun locateNamedType(namedType: TsNamedType<*>): TsFile

    private fun typeRef(namedType: TsNamedType<*>): TsImport =
        TsImport.Type(locateNamedType(namedType), namedType.name)

    private fun typeRef(sealedVariant: TsSealedVariant): TsImport =
        TsImport.Type(locateNamedType(sealedVariant.parent), sealedVariant.name)

    private fun deserializerRef(namedType: TsNamedType<*>): TsImport =
        TsImport.Named(locateNamedType(namedType), "deserializeJson${namedType.name}")

    private fun deserializerRef(sealedVariant: TsSealedVariant): TsImport =
        TsImport.Named(
            locateNamedType(sealedVariant.parent),
            "deserializeJson${sealedVariant.parent.name}${sealedVariant.obj.name}",
        )

    fun arrayType(elementType: KType?, compact: Boolean): TsCode {
        val elementTs =
            if (elementType == null) TsCode("never")
            else
                tsType(elementType, compact).let {
                    if (elementType.isMarkedNullable) it.copy(text = "(${it.text})") else it
                }
        return elementTs + "[]"
    }

    fun tsProperty(name: String, prop: TsProperty, value: TsCode): TsCode =
        TsCode("$name${if (prop.isOptional) "?" else ""}: ") + value

    fun recordType(type: Pair<KType?, KType?>, compact: Boolean): TsCode {
        val keyTs = type.first?.let { keyType(it) } ?: TsCode("never")
        val valueTs = type.second?.let { tsType(it, compact) } ?: TsCode("never")
        return TsCode { "Partial<Record<${inline(keyTs)}, ${inline(valueTs)}>>" }
    }

    fun tupleType(elementTypes: List<KType?>, compact: Boolean): TsCode =
        TsCode.join(
            elementTypes.map { type -> type?.let { tsType(it, compact) } ?: TsCode("never") },
            separator = ", ",
            prefix = "[",
            postfix = "]",
        )

    private fun typeToTsCode(type: KType, f: (tsType: TsType) -> TsCode): TsCode =
        when (val clazz = type.classifier) {
            is KClass<*> ->
                f(
                    TsType(
                        metadata[clazz] ?: error("No TS type found for $type"),
                        type.isMarkedNullable,
                        type.arguments,
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
            is GenericWrapper -> keyType(tsRepr.getTypeArgs(tsType.typeArguments))
            is TsIdType -> TsCode(typeRef(tsRepr))
            is Excluded,
            is TsArray,
            is TsRecord,
            is TsTuple,
            is TsPlainObject,
            is TsObjectLiteral,
            is TsSealedClass,
            is TsSealedVariant -> null
        }?.takeUnless { tsType.isNullable } ?: error("$tsType is not supported as a key type")

    fun tsType(type: KType, compact: Boolean): TsCode = typeToTsCode(type) { tsType(it, compact) }

    fun tsType(tsType: TsType, compact: Boolean): TsCode =
        when (val tsRepr = tsType.representation) {
            is TsPlain -> TsCode(tsRepr.type)
            is TsArray -> arrayType(tsRepr.getTypeArgs(tsType.typeArguments), compact)
            is TsRecord -> recordType(tsRepr.getTypeArgs(tsType.typeArguments), compact)
            is TsTuple -> tupleType(tsRepr.getTypeArgs(tsType.typeArguments), compact)
            is GenericWrapper -> tsType(tsRepr.getTypeArgs(tsType.typeArguments), compact)
            is TsIdType -> TsCode(typeRef(tsRepr))
            is TsPlainObject -> {
                val typeArguments =
                    tsRepr.getTypeArgs(tsType.typeArguments).map { typeArg ->
                        if (typeArg != null) tsType(typeArg, compact) else TsCode("never")
                    }
                TsCode(typeRef(tsRepr)) +
                    if (typeArguments.isEmpty()) TsCode("")
                    else TsCode.join(typeArguments, separator = ", ", prefix = "<", postfix = ">")
            }
            is TsObjectLiteral ->
                TsCode.join(
                    tsRepr.properties.map { (name, prop) ->
                        tsProperty(name, prop, value = tsType(prop.type, compact)).let {
                            if (!compact) it.prependIndent("  ") else it
                        }
                    },
                    separator = if (compact) ", " else ",\n",
                    prefix = if (compact) "{ " else "{\n",
                    postfix = if (compact) " }" else "\n}",
                )
            is TsSealedClass -> TsCode(typeRef(tsRepr))
            is TsStringEnum -> TsCode(typeRef(tsRepr))
            is TsSealedVariant -> TsCode(typeRef(tsRepr))
            is TsExternalTypeRef -> TsCode(tsRepr.type, tsRepr.imports)
            is Excluded -> TsCode("never")
        }.let { if (tsType.isNullable) it + " | null" else it }

    fun stringEnum(enum: TsStringEnum): TsCode = TsCode {
        if (enum.constList != null)
            """${enum.docHeader()}
export const ${enum.constList.name} = [
${enum.values.joinToString(",\n") { "'$it'" }.prependIndent("  ")}
] as const

export type ${enum.name} = typeof ${enum.constList.name}[number]"""
        else
            """${enum.docHeader()}
export type ${enum.name} =
${enum.values.joinToString("\n") { "| '$it'" }.prependIndent("  ")}"""
    }

    fun tsPlainObject(obj: TsPlainObject): TsCode {
        val typeParams =
            if (obj.clazz.typeParameters.isNotEmpty())
                obj.clazz.typeParameters.joinToString(",", prefix = "<", postfix = ">")
            else ""
        val props =
            obj.properties.entries
                .sortedBy { it.key }
                .map { (name, prop) ->
                    tsProperty(name, prop, value = tsType(prop.type, compact = true))
                }
        return TsCode {
            """${obj.docHeader()}
export interface ${obj.name}$typeParams {
${join(props, "\n").prependIndent("  ")}
}"""
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
                            .map { (name, prop) ->
                                tsProperty(name, prop, value = tsType(prop.type, compact = true))
                            }
                TsCode {
                    """${variant.obj.docHeader()}
export interface ${variant.obj.name} {
${join(props, "\n").prependIndent("  ")}
}"""
                }
            }
        return TsCode {
            """
export namespace ${sealed.name} {
${join(tsVariants, "\n\n").prependIndent("  ")}
}

${sealed.docHeader()}
export type ${sealed.name} = ${variants.joinToString(separator = " | ") { "${sealed.name}.${it.obj.name}" }}
"""
        }
    }

    fun tsIdType(namedType: TsIdType): TsCode =
        TsCode("export type ${namedType.name} = Id<'${namedType.tableName}'>", Imports.id)

    fun namedType(namedType: TsNamedType<*>): TsCode =
        when (namedType) {
            is TsStringEnum -> stringEnum(namedType)
            is TsPlainObject -> tsPlainObject(namedType)
            is TsSealedClass -> sealedClass(namedType)
            is TsIdType -> tsIdType(namedType)
        }

    fun typeAliases(targetType: String, aliases: List<String>): List<TsCode> =
        aliases.map { alias -> TsCode("export type $alias = $targetType") }

    private fun needsJsonDeserializer(type: KType): Boolean {
        val cache = mutableMapOf<KType, Boolean>()

        fun check(type: KType): Boolean =
            cache.getOrPut(type) {
                cache[type] = false // prevent problems with recursion by assuming false by default
                when (val tsRepr = metadata[type]) {
                    is TsArray -> tsRepr.getTypeArgs(type.arguments)?.let { check(it) } ?: false
                    is TsRecord ->
                        tsRepr.getTypeArgs(type.arguments).second?.let { check(it) } ?: false
                    is TsTuple ->
                        tsRepr.getTypeArgs(type.arguments).filterNotNull().any { check(it) }
                    is GenericWrapper -> check(tsRepr.getTypeArgs(type.arguments))
                    is TsPlainObject ->
                        tsRepr.applyTypeArguments(type.arguments).values.any { check(it) }
                    is TsObjectLiteral -> tsRepr.properties.values.any { check(it.type) }
                    is TsSealedClass ->
                        tsRepr.variants.any { variant ->
                            (metadata[variant] as TsSealedVariant).obj.properties.values.any {
                                check(it.type)
                            }
                        }
                    is TsSealedVariant ->
                        tsRepr.obj.applyTypeArguments(type.arguments).values.any { check(it) }
                    is TsExternalTypeRef -> tsRepr.deserializeJson != null
                    is Excluded,
                    is TsIdType,
                    is TsStringEnum,
                    is TsPlain -> false
                    null ->
                        if (type.classifier is KTypeParameter) true
                        else error("No TS type found for $type")
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
                                    TsCode(typeRef(variant)),
                                    TsCode(deserializerRef(variant)),
                                    extraArguments = emptyList(),
                                    variant.obj.properties,
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
                        """
${join(variants.values, "\n")}
export function deserializeJson${namedType.name}(json: ${ref(Imports.jsonOf)}<${namedType.name}>): ${namedType.name} {
  switch (json.$discriminantProp) {
${cases.prependIndent("    ")}
    default: return json
  }
}"""
                    }
                }
            }
            is TsPlainObject -> {
                val typeParams =
                    if (namedType.clazz.typeParameters.isNotEmpty())
                        namedType.clazz.typeParameters.joinToString(
                            prefix = "<",
                            separator = ", ",
                            postfix = ">",
                        )
                    else ""
                jsonObjectDeserializer(
                    TsCode(typeRef(namedType)) + typeParams,
                    TsCode(deserializerRef(namedType)) + typeParams,
                    extraArguments =
                        namedType.clazz.typeParameters.map {
                            TsCode {
                                "deserialize${it.name}: (value: ${ref(Imports.jsonOf)}<${it.name}>) => ${it.name}"
                            }
                        },
                    namedType.properties,
                )
            }
            is TsIdType -> null
        }

    private fun jsonObjectDeserializer(
        type: TsCode,
        function: TsCode,
        extraArguments: List<TsCode>,
        props: Map<String, TsProperty>,
    ): TsCode? {
        val propDeserializers =
            props.mapNotNull { (name, prop) ->
                jsonDeserializerExpression(prop.type, TsCode("json.$name"))?.let { name to it }
            }
        if (propDeserializers.isEmpty()) return null
        val propCodes =
            listOf(TsCode("...json")) +
                propDeserializers.map { (name, code) -> TsCode { "$name: ${inline(code)}" } }
        val arguments = extraArguments + TsCode { "json: ${ref(Imports.jsonOf)}<${inline(type)}>" }
        return TsCode {
            """
export function ${inline(function)}(${join(arguments, separator = ", ")}): ${inline(type)} {
  return {
${join(propCodes, ",\n").prependIndent("    ")}
  }
}"""
        }
    }

    fun jsonDeserializerExpression(type: KType, jsonExpression: TsCode): TsCode? =
        if (!needsJsonDeserializer(type)) null
        else
            when (val tsRepr = metadata[type]) {
                is TsArray -> {
                    jsonDeserializerExpression(
                            requireNotNull(tsRepr.getTypeArgs(type.arguments)),
                            TsCode("e"),
                        )
                        ?.let { TsCode { "${inline(jsonExpression)}.map(e => ${inline(it)})" } }
                }
                is TsRecord -> {
                    val valueDeser =
                        jsonDeserializerExpression(
                            requireNotNull(tsRepr.getTypeArgs(type.arguments).second),
                            TsCode("v"),
                        )
                    if (valueDeser == null) null
                    else
                        TsCode {
                            """Object.fromEntries(Object.entries(${inline(jsonExpression)}).map(
  ([k, v]) => [k, v !== undefined ? ${inline(valueDeser)} : v]
))"""
                        }
                }
                is TsTuple -> {
                    TsCode.join(
                        tsRepr.getTypeArgs(type.arguments).withIndex().map {
                            (elementIndex, elementType) ->
                            val elementExpression = jsonExpression + "[$elementIndex]"
                            jsonDeserializerExpression(
                                requireNotNull(elementType),
                                elementExpression,
                            ) ?: elementExpression
                        },
                        separator = ",",
                        prefix = "[",
                        postfix = "]",
                    )
                }
                is GenericWrapper ->
                    jsonDeserializerExpression(tsRepr.getTypeArgs(type.arguments), jsonExpression)
                is TsObjectLiteral -> TODO()
                is TsSealedVariant ->
                    TsCode { "${ref(deserializerRef(tsRepr))}(${inline(jsonExpression)})" }
                is TsPlainObject -> {
                    val deserArguments =
                        type.arguments.map { typeArgument ->
                            val argType = requireNotNull(typeArgument.type)
                            val argDeserializer =
                                jsonDeserializerExpression(argType, TsCode("value"))
                                    ?: TsCode("value")
                            TsCode {
                                "(value: ${ref(Imports.jsonOf)}<${inline(tsType(argType, compact = true))}>) => ${inline(argDeserializer)}"
                            }
                        } + listOf(jsonExpression)
                    TsCode {
                        "${ref(deserializerRef(tsRepr))}(${join(deserArguments, separator = ", ")})"
                    }
                }
                is TsSealedClass ->
                    TsCode { "${ref(deserializerRef(tsRepr))}(${inline(jsonExpression)})" }
                is TsExternalTypeRef -> tsRepr.deserializeJson?.invoke(jsonExpression)
                is Excluded,
                is TsPlain,
                is TsIdType,
                is TsStringEnum -> null
                null ->
                    when (val clazz = type.classifier) {
                        is KTypeParameter ->
                            TsCode { "deserialize${clazz.name}(${inline(jsonExpression)})" }
                        else -> error("No TS type found for $type")
                    }
            }?.let {
                if (type.isMarkedNullable)
                    TsCode { "(${inline(jsonExpression)} != null) ? ${inline(it)} : null" }
                else it
            }

    fun serializePathVariable(type: KType, valueExpression: TsCode): TsCode =
        when (val tsRepr = metadata[type] ?: error("No TS type found for $type")) {
            is TsPlain,
            is TsArray,
            is TsStringEnum -> valueExpression
            is TsExternalTypeRef -> tsRepr.serializePathVariable?.invoke(valueExpression)
            is GenericWrapper ->
                serializePathVariable(tsRepr.getTypeArgs(type.arguments), valueExpression)
            is TsIdType -> valueExpression
            is TsPlainObject,
            is TsSealedClass,
            is TsObjectLiteral,
            is TsRecord,
            is TsTuple,
            is TsSealedVariant,
            is Excluded -> null
        }?.takeUnless { type.isMarkedNullable }
            ?: error("$type is not supported as a path variable parameter type")

    fun toRequestParamPairs(type: KType, name: String, valueExpression: TsCode): TsCode =
        when (val tsRepr = metadata[type] ?: error("No TS type found for $type")) {
            is TsArray -> {
                val elementType = requireNotNull(tsRepr.getTypeArgs(type.arguments))
                val serializeElement = serializeRequestParam(elementType, TsCode("e"))
                TsCode {
                    if (type.isMarkedNullable)
                        "...(${inline(valueExpression)}?.map((e): [string, string | null | undefined] => ['$name', ${inline(serializeElement)}]) ?? [])"
                    else
                        "...(${inline(valueExpression)}.map((e): [string, string | null | undefined] => ['$name', ${inline(serializeElement)}]))"
                }
            }
            else -> TsCode { "['$name', ${inline(serializeRequestParam(type, valueExpression))}]" }
        }

    fun serializeRequestParam(type: KType, valueExpression: TsCode): TsCode =
        when (val tsRepr = metadata[type] ?: error("No TS type found for $type")) {
            is TsPlain,
            is TsStringEnum ->
                if (type.isMarkedNullable) valueExpression + "?.toString()"
                else valueExpression + ".toString()"
            is TsIdType -> valueExpression
            is TsExternalTypeRef ->
                tsRepr.serializeRequestParam?.invoke(valueExpression, type.isMarkedNullable)
                    ?: if (type.isMarkedNullable) valueExpression + "?.toString()"
                    else valueExpression + ".toString()"
            is GenericWrapper ->
                serializeRequestParam(tsRepr.getTypeArgs(type.arguments), valueExpression)
            is TsArray,
            is TsPlainObject,
            is TsSealedClass,
            is TsObjectLiteral,
            is TsRecord,
            is TsSealedVariant,
            is TsTuple,
            is Excluded -> null
        } ?: error("$type is not supported as an API request parameter type")
}

private fun TsNamedType<*>.docHeader() =
    """/**
* Generated from $source
*/"""
