// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.DatabindContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.superclasses
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

fun defaultJsonMapperBuilder(): JsonMapper.Builder =
    JsonMapper
        .builder()
        .addModules(
            KotlinModule
                .Builder()
                // Without this, Kotlin singletons are not actually singletons when deserialized.
                // For example, a sealed class `sealed class Foo` where one variant is `object
                // OneVariant: Foo()`
                .enable(KotlinFeature.SingletonSupport)
                .build(),
            JavaTimeModule(),
            Jdk8Module(),
            ParameterNamesModule()
        )
        // We never want to serialize timestamps as numbers but use ISO formats instead.
        // Our custom types (e.g. HelsinkiDateTime) already have custom serializers that handle
        // this, but it's still a good idea to ensure global defaults are sane.
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

@Configuration
class JacksonConfig {
    // This replaces default JsonMapper provided by Spring Boot autoconfiguration
    @Bean
    fun jsonMapper(): JsonMapper =
        defaultJsonMapperBuilder()
            // Disabled by default in Spring Boot autoconfig
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            // Disabled by default in Spring Boot autoconfig
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .build()
}

/**
 * Custom resolver that uses the simple subclass names as discriminants in sealed classes
 *
 * Example where a property called "type" will be either "Sub1", "OTHER", or "Sub3" depending on the
 * actual variant:
 * ```
 * @JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "type")
 * @JsonTypeIdResolver(SealedClassSimpleName::class)
 * sealed interface Super {
 *   data class Sub1(val a: String): Super
 *   @JsonTypeName("OTHER") // overrides the default name
 *   data class Sub2(val b: String): Super
 *   data object Sub3: Super
 * }
 * ```
 */
class SealedSubclassSimpleName : TypeIdResolverBase() {
    data class Mapping(
        val superClass: KClass<*>
    ) {
        val typeIds = superClass.sealedSubclasses.map { subClass -> subClass to subClass.typeId() }

        operator fun get(clazz: KClass<*>): String? = typeIds.find { it.first == clazz }?.second

        operator fun get(id: String): KClass<*>? = typeIds.find { it.second == id }?.first
    }

    private lateinit var mapping: Mapping

    override fun init(bt: JavaType) {
        super.init(bt)
        mapping = mappingOf(bt.rawClass.kotlin)
    }

    override fun idFromValue(value: Any): String? = mapping[value.javaClass.kotlin]

    override fun idFromValueAndType(
        value: Any?,
        suggestedType: Class<*>
    ): String? = mapping[(value?.javaClass ?: suggestedType).kotlin]

    override fun getMechanism(): JsonTypeInfo.Id = JsonTypeInfo.Id.CUSTOM

    override fun typeFromId(
        context: DatabindContext,
        id: String
    ): JavaType? = context.constructType(mapping[id]?.java)

    companion object {
        private val cache: ConcurrentMap<KClass<*>, Mapping> = ConcurrentHashMap()

        private fun KClass<*>.locateRelevantSealedClass(): KClass<*> =
            when {
                this.isRelevantSealedJsonClass() -> this
                else ->
                    superclasses.singleOrNull { it.isRelevantSealedJsonClass() }
                        ?: error("No valid sealed superclass found for $this")
            }

        private fun KClass<*>.isRelevantSealedJsonClass() = isSealed && findAnnotation<JsonTypeInfo>()?.use == JsonTypeInfo.Id.CUSTOM

        private fun KClass<*>.typeId(): String = findAnnotation<JsonTypeName>()?.value ?: simpleName ?: java.name

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
