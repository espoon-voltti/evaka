// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPool
import redis.clients.jedis.Protocol

@Configuration
class RedisConfig {
    @Bean
    fun redisPool(
        @Value("\${redis.url}") redisUrl: String,
        @Value("\${redis.port}") redisPort: Int,
        @Value("\${redis.password}") redisPassword: String,
        @Value("\${redis.ssl}") useSsl: String
    ) = JedisPool(
        GenericObjectPoolConfig(),
        redisUrl,
        redisPort,
        Protocol.DEFAULT_TIMEOUT,
        redisPassword.ifEmpty { null },
        Protocol.DEFAULT_DATABASE,
        useSsl.toBoolean()
    )
}
