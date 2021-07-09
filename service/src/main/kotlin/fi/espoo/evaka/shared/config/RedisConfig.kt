// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.config

import fi.espoo.evaka.RedisEnv
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import redis.clients.jedis.JedisPool
import redis.clients.jedis.Protocol

@Configuration
class RedisConfig {
    @Bean
    fun redisPool(env: RedisEnv) = JedisPool(
        GenericObjectPoolConfig(),
        env.url,
        env.port,
        Protocol.DEFAULT_TIMEOUT,
        env.password.value.ifEmpty { null },
        Protocol.DEFAULT_DATABASE,
        env.useSsl
    )
}
