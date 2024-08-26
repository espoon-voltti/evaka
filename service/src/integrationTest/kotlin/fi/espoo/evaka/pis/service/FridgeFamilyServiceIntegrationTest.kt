// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.insertTestDecisionMaker
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.vtjclient.mapper.VtjHenkiloMapper
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import fi.espoo.evaka.vtjclient.service.persondetails.VTJPersonDetailsService
import fi.espoo.evaka.vtjclient.service.vtjclient.IVtjClientService
import java.time.LocalDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FridgeFamilyServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mockToday = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    lateinit var adult1: PersonDTO
    lateinit var adult2: PersonDTO
    lateinit var child1: PersonDTO
    lateinit var child2: PersonDTO
    private val partnershipCreator =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.FINANCE_ADMIN))
            .evakaUserId

    @BeforeEach
    fun setup() {
        adult1 = createPerson("140881-172X", "Isä")
        adult2 = createPerson("150786-1766", "Äiti")
        child1 = createPerson("120915A931W", "Lapsi1")
        child2 = createPerson("101221A999S", "Lapsi2")
        MockPersonDetailsService.addPersons(adult1, adult2, child1, child2)
    }

    @Autowired lateinit var fridgeFamilyService: FridgeFamilyService

    @Test
    fun `First adult becomes the head of all common children`() {
        MockPersonDetailsService.addDependants(adult1.identity, child1.identity, child2.identity)
        MockPersonDetailsService.addDependants(adult2.identity, child1.identity, child2.identity)

        db.transaction {
            it.insertTestDecisionMaker()
            it.createPartnership(
                adult1.id,
                adult2.id,
                mockToday.today(),
                null,
                false,
                Creator.User(partnershipCreator),
                mockToday.now(),
            )
        }

        fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(adult1.id), mockToday)

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size,
            )
            assertEquals(
                1,
                it.getParentships(adult1.id, child2.id, false, DateRange(mockToday.today(), null))
                    .size,
            )
        }
    }

    @Test
    fun `New child is added to existing head of family`() {
        MockPersonDetailsService.addDependants(adult1.identity, child1.identity)
        MockPersonDetailsService.addDependants(adult2.identity, child1.identity)

        db.transaction {
            it.insertTestDecisionMaker()
            it.createPartnership(
                adult1.id,
                adult2.id,
                mockToday.today(),
                null,
                false,
                Creator.User(partnershipCreator),
                mockToday.now(),
            )
        }

        fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(adult1.id), mockToday)

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size,
            )
        }

        MockPersonDetailsService.addDependants(adult1.identity, child2.identity)
        MockPersonDetailsService.addDependants(adult2.identity, child2.identity)

        // Note: the person to update here is the other adult2. Implementation should
        // detect that her partner already has a fridge family and add the child to that one instead
        // of
        // creating a new fridge family for this other adult2
        fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(adult1.id), mockToday)

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size,
            )
            assertEquals(
                1,
                it.getParentships(adult1.id, child2.id, false, DateRange(mockToday.today(), null))
                    .size,
            )
        }
    }

    @Test
    fun `guardians are added to child`() {
        MockPersonDetailsService.addDependants(adult1.identity, child1.identity)
        MockPersonDetailsService.addDependants(adult2.identity, child1.identity)

        fridgeFamilyService.updateGuardianOrChildFromVtj(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            child1.id,
        )

        assertEquals(
            setOf(adult1.id, adult2.id),
            db.read { tx -> tx.getChildGuardians(child1.id) }.toSet(),
        )
    }

    @Autowired lateinit var parentshipService: ParentshipService

    @Test
    fun testVtjRequestCounts() {
        val mockVtjClientService: IVtjClientService = MockVtjClientService()
        MockVtjClientService.resetQueryCounts()

        val service =
            FridgeFamilyService(
                PersonService(VTJPersonDetailsService(mockVtjClientService, VtjHenkiloMapper())),
                parentshipService,
            )

        val parent = DevPerson(ssn = adult1.identity.toString())
        val child = DevPerson(ssn = child1.identity.toString())

        MockVtjClientService.addHUOLTAJAHUOLLETTAVARequestExpectation(parent, listOf(child))
        MockVtjClientService.addPERUSSANOMA3RequestExpectation(child)
        service.updateGuardianOrChildFromVtj(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            adult1.id,
        )

        Assertions.assertEquals(1, MockVtjClientService.getPERUSSANOMA3RequestCount(child))
        Assertions.assertEquals(1, MockVtjClientService.getHUOLTAJAHUOLLETTAVARequestCount(parent))
    }

    private fun createPerson(ssn: String, firstName: String): PersonDTO {
        return db.transaction {
            val id =
                it.insert(
                    DevPerson(
                        ssn = ssn,
                        dateOfBirth = getDobFromSsn(ssn),
                        firstName = firstName,
                        lastName = "Meikäläinen",
                        email = "",
                        language = "fi",
                    ),
                    DevPersonType.RAW_ROW,
                )
            it.getPersonById(id)!!
        }
    }
}
