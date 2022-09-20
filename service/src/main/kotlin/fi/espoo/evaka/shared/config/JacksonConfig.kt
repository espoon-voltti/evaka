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
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

fun defaultJsonMapper(): JsonMapper =
    jacksonMapperBuilder()
        .addModules(JavaTimeModule(), JaxbAnnotationModule(), Jdk8Module(), ParameterNamesModule())
        .disable(
            MapperFeature.DEFAULT_VIEW_INCLUSION
        ) // Disabled by default in Spring Boot autoconfig
        .disable(
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        ) // Disabled by default in Spring Boot autoconfig
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
        .build()

@Configuration
class JacksonConfig {
    // This replaces default JsonMapper provided by Spring Boot autoconfiguration
    @Bean fun jsonMapper() = defaultJsonMapper()
}
