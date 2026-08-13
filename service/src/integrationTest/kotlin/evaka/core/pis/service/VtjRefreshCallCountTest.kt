// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.VTJPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

/**
 * A guardian refresh costs one HUOLTAJA_HUOLLETTAVA plus one PERUSSANOMA3 per dependant; reading
 * `upsertVtjChildren` alone suggests otherwise, because hydration happens a layer above it.
 */
class VtjRefreshCallCountTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val guardian =
        DevPerson(
            ssn = "220281-9456",
            dateOfBirth = LocalDate.of(1981, 2, 22),
            firstName = "Mikael",
            lastName = "Högfors",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    private val children =
        listOf("071013A960W", "120915A931W", "101221A999S").mapIndexed { index, ssn ->
            DevPerson(
                ssn = ssn,
                dateOfBirth = LocalDate.of(2013, 10, 7),
                firstName = "Lapsi$index",
                lastName = "Högfors",
                streetAddress = "Kamreerintie 4",
                postalCode = "02100",
                postOffice = "Espoo",
            )
        }

    @Test
    fun `refreshing a guardian with three dependants costs one query plus one per dependant`() {
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            children.forEach { tx.insert(it, DevPersonType.CHILD) }
        }
        MockVtjClientService.resetQueryCounts()
        MockVtjClientService.addHUOLTAJAHUOLLETTAVARequestExpectation(guardian, children)
        children.forEach { MockVtjClientService.addPERUSSANOMA3RequestExpectation(it) }
        val personService =
            PersonService(VTJPersonDetailsService(MockVtjClientService(), VtjHenkiloMapper()))

        db.transaction {
            personService.getPersonWithChildren(
                it,
                AuthenticatedUser.SystemInternalUser,
                HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0)),
                guardian.id,
                forceRefresh = true,
            )
        }

        assertEquals(1, MockVtjClientService.getHUOLTAJAHUOLLETTAVARequestCount(guardian))
        children.forEach { assertEquals(1, MockVtjClientService.getPERUSSANOMA3RequestCount(it)) }
    }
}
