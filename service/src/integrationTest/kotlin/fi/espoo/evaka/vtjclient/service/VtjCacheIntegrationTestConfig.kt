// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.espoo.evaka.vtjclient.service.cache.VtjCache
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import java.text.SimpleDateFormat

@ComponentScan(basePackageClasses = [VtjCache::class])
@TestConfiguration
class VtjCacheIntegrationTestConfig {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
        .registerKotlinModule()
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())
        .setDateFormat(SimpleDateFormat("yyyy-MM-dd"))
}
