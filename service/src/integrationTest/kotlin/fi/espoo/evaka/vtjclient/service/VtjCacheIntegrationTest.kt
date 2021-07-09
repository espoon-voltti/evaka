// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.cache.VtjCache
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.clients.jedis.JedisPool
import kotlin.test.assertEquals

class VtjCacheIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var service: VtjCache

    @Autowired
    lateinit var redisPool: JedisPool

    @BeforeEach
    protected fun beforeEach() {
        redisPool.resource.use {
            it.flushDB()
        }
    }

    @Test
    fun `service save returns the saved value`() {
        var savedPerson = service.getPerson(vtjPerson.socialSecurityNumber)
        assertEquals(null, savedPerson)

        service.save(
            vtjPerson.socialSecurityNumber,
            vtjPerson
        )

        savedPerson = service.getPerson(vtjPerson.socialSecurityNumber)
        assertEquals(vtjPerson, savedPerson)
    }
}

val vtjPerson = VtjPerson(
    firstNames = "etunimi",
    lastName = "sukunimi",
    socialSecurityNumber = "010101-010A",
    address = null,
    dependants = emptyList(),
    nationalities = emptyList(),
    nativeLanguage = null,
    restrictedDetails = null
)
