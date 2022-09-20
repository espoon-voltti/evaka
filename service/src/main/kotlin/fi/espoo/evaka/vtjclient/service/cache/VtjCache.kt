// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service.cache

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import mu.KotlinLogging
import org.springframework.stereotype.Service
import redis.clients.jedis.JedisPool

@Service
class VtjCache(private val jsonMapper: JsonMapper, private val redisPool: JedisPool) {
    private val logger = KotlinLogging.logger {}
    private val timeToLive = 60 * 60 * 12L // in seconds

    fun getPerson(ssn: String): VtjPerson? {
        logger.debug { "Getting data from VTJ cache" }
        redisPool.resource.use { redis ->
            val person = redis.get(ssn) ?: return null
            return jsonMapper.readValue<VtjPerson>(person)
        }
    }

    fun save(ssn: String, person: VtjPerson?) {
        logger.debug { "Setting data to VTJ cache" }
        redisPool.resource.use { redis ->
            redis.set(ssn, jsonMapper.writeValueAsString(person))
            redis.expire(ssn, timeToLive)
        }
    }
}
