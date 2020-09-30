// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.vtjclient.service

import fi.espoo.evaka.shared.config.SharedIntegrationTestConfig
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.cache.VtjCache
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@Import(VtjCacheIntegrationTestConfig::class, SharedIntegrationTestConfig::class)
class VtjCacheIntegrationTest {
    @Autowired
    lateinit var service: VtjCache

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
