// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

fun jackson2JsonMapperBuilder(): JsonMapper.Builder =
    JsonMapper.builder()
        .addModules(
            KotlinModule.Builder()
                // Without this, Kotlin singletons are not actually singletons when deserialized.
                // For example, a sealed class `sealed class Foo` where one variant is `object
                // OneVariant: Foo()`
                .enable(KotlinFeature.SingletonSupport)
                .build(),
            JavaTimeModule(),
            Jdk8Module(),
            ParameterNamesModule(),
        )
        // We never want to serialize timestamps as numbers but use ISO formats instead.
        // Our custom types (e.g. HelsinkiDateTime) already have custom serializers that handle
        // this, but it's still a good idea to ensure global defaults are sane.
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

@Configuration
class Jackson2Config {
    // This replaces default JsonMapper provided by Spring Boot autoconfiguration
    @Bean
    fun jackson2jsonMapper(): JsonMapper =
        jackson2JsonMapperBuilder()
            // Disabled by default in Spring Boot autoconfig
            .disable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            // Disabled by default in Spring Boot autoconfig
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .build()
}
