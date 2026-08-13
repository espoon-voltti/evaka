// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.PureJdbiTest
import evaka.core.pis.getPersonById
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.IPersonDetailsService
import evaka.core.vtjclient.service.persondetails.MockPersonDetailsService
import evaka.core.vtjclient.service.persondetails.VTJPersonDetailsService
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Relatives must come back fully populated: `upsertVtjPerson` writes each one into `person` with an
 * unconditional SET, so a name+SSN stub blanks their address, language, nationalities and
 * turvakielto. Subclass this for every new [IPersonDetailsService].
 */
abstract class PersonDetailsServiceContract : PureJdbiTest(resetDbBeforeEach = true) {
    private val user = AuthenticatedUser.SystemInternalUser
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 1, 1), LocalTime.of(12, 0))

    protected val guardian =
        DevPerson(
            dateOfBirth = LocalDate.of(1981, 2, 22),
            firstName = "Mikael",
            lastName = "Högfors",
            ssn = "220281-9456",
            language = "fi",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    protected val child =
        DevPerson(
            dateOfBirth = LocalDate.of(2013, 10, 7),
            firstName = "Antero",
            lastName = "Högfors",
            ssn = "071013A960W",
            language = "fi",
            streetAddress = "Kamreerintie 4",
            postalCode = "02100",
            postOffice = "Espoo",
        )

    /** Seed the implementation's backing store with [guardian] as the parent of [child]. */
    protected abstract fun createServiceReturningTheFamily(): IPersonDetailsService

    @BeforeEach
    fun insertFamily() {
        db.transaction {
            it.insert(guardian, DevPersonType.ADULT)
            it.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `refreshing a guardian preserves their dependant's address`() {
        val personService = PersonService(createServiceReturningTheFamily())

        db.transaction {
            personService.getPersonWithChildren(it, user, now, guardian.id, forceRefresh = true)
        }

        val refreshed = db.read { it.getPersonById(child.id) }!!
        assertEquals(child.streetAddress, refreshed.streetAddress)
        assertEquals(child.postalCode, refreshed.postalCode)
        assertEquals(child.postOffice, refreshed.postOffice)
    }

    @Test
    fun `refreshing a child preserves their guardian's address`() {
        val personService = PersonService(createServiceReturningTheFamily())

        db.transaction { personService.getGuardians(it, user, now, child.id, forceRefresh = true) }

        val refreshed = db.read { it.getPersonById(guardian.id) }!!
        assertEquals(guardian.streetAddress, refreshed.streetAddress)
        assertEquals(guardian.postalCode, refreshed.postalCode)
        assertEquals(guardian.postOffice, refreshed.postOffice)
    }
}

/** The production implementation, which hydrates by re-querying each relative with PERUSSANOMA3. */
class VtjSoapPersonDetailsServiceContractTest : PersonDetailsServiceContract() {
    override fun createServiceReturningTheFamily(): IPersonDetailsService {
        MockVtjClientService.resetQueryCounts()
        MockVtjClientService.addPERUSSANOMA3RequestExpectation(guardian)
        MockVtjClientService.addPERUSSANOMA3RequestExpectation(child)
        MockVtjClientService.addHUOLTAJAHUOLLETTAVARequestExpectation(guardian, listOf(child))
        MockVtjClientService.addHUOLLETTAVAHUOLTAJATRequestExpectation(child, listOf(guardian))
        return VTJPersonDetailsService(MockVtjClientService(), VtjHenkiloMapper())
    }
}

/** Enrolled because the rest of the integration suite depends on this double staying hydrated. */
class MockPersonDetailsServiceContractTest : PersonDetailsServiceContract() {
    override fun createServiceReturningTheFamily(): IPersonDetailsService {
        MockPersonDetailsService.reset()
        MockPersonDetailsService.addPersons(guardian, child)
        MockPersonDetailsService.addDependants(guardian, child)
        return MockPersonDetailsService()
    }
}
