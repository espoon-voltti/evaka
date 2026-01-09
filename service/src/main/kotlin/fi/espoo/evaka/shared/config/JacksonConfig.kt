// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.superclasses
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DatabindContext
import tools.jackson.databind.JavaType
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase
import tools.jackson.module.kotlin.KotlinModule

fun defaultJsonMapperBuilder(): JsonMapper.Builder =
    JsonMapper.builder().addModules(KotlinModule.Builder().build())

@Configuration
class JacksonConfig {
    // This replaces default JsonMapper provided by Spring Boot autoconfiguration
    @Bean
    fun jsonMapper(): JsonMapper =
        defaultJsonMapperBuilder()
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(EnumFeature.READ_ENUMS_USING_TO_STRING)
            .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING)
            .build()
}

/**
 * Custom resolver that uses the simple subclass names as discriminants in sealed classes
 *
 * Example where a property called "type" will be either "Sub1", "OTHER", or "Sub3" depending on the
 * actual variant:
 * ```
 * @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
 * @JsonTypeIdResolver(SealedSubclassSimpleName::class)
 * sealed interface Super {
 *   data class Sub1(val a: String): Super
 *   @JsonTypeName("OTHER") // overrides the default name
 *   data class Sub2(val b: String): Super
 *   data object Sub3: Super
 * }
 * ```
 */
class SealedSubclassSimpleName : TypeIdResolverBase() {
    data class Mapping(val superClass: KClass<*>) {
        val typeIds = superClass.sealedSubclasses.map { subClass -> subClass to subClass.typeId() }

        operator fun get(clazz: KClass<*>): String? = typeIds.find { it.first == clazz }?.second

        operator fun get(id: String): KClass<*>? = typeIds.find { it.second == id }?.first
    }

    private lateinit var mapping: Mapping

    override fun init(bt: JavaType) {
        super.init(bt)
        mapping = mappingOf(bt.rawClass.kotlin)
    }

    override fun idFromValue(ctxt: DatabindContext, value: Any): String? =
        mapping[value.javaClass.kotlin]

    override fun idFromValueAndType(
        ctxt: DatabindContext,
        value: Any?,
        suggestedType: Class<*>,
    ): String? = mapping[(value?.javaClass ?: suggestedType).kotlin]

    override fun getMechanism(): JsonTypeInfo.Id = JsonTypeInfo.Id.CUSTOM

    override fun typeFromId(context: DatabindContext, id: String): JavaType? =
        context.constructType(mapping[id]?.java)

    companion object {
        private val cache: ConcurrentMap<KClass<*>, Mapping> = ConcurrentHashMap()

        private fun KClass<*>.locateRelevantSealedClass(): KClass<*> =
            when {
                this.isRelevantSealedJsonClass() -> {
                    this
                }

                else -> {
                    superclasses.singleOrNull { it.isRelevantSealedJsonClass() }
                        ?: error("No valid sealed superclass found for $this")
                }
            }

        private fun KClass<*>.isRelevantSealedJsonClass() =
            isSealed && findAnnotation<JsonTypeInfo>()?.use == JsonTypeInfo.Id.CUSTOM

        private fun KClass<*>.typeId(): String =
            findAnnotation<JsonTypeName>()?.value ?: simpleName ?: java.name

        fun mappingOf(clazz: KClass<*>): Mapping =
            cache.getOrElse(clazz) {
                // clazz might be the sealed superclass or one of the variants, so find the
                // superclass first
                val superClass = clazz.locateRelevantSealedClass()
                Mapping(superClass).also {
                    cache[it.superClass] = it
                    it.typeIds.forEach { (subClass, _) -> cache[subClass] = it }
                }
            }
    }
}
