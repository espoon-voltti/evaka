// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.FullApplicationTest
import evaka.core.identity.getDobFromSsn
import evaka.core.pis.Creator
import evaka.core.pis.createPartnership
import evaka.core.pis.getParentships
import evaka.core.pis.getPersonById
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.user.updateLastStrongLogin
import evaka.core.vtjclient.mapper.VtjHenkiloMapper
import evaka.core.vtjclient.service.persondetails.MockVtjClient
import evaka.core.vtjclient.service.persondetails.SoapVtjClient
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport
import evaka.core.vtjclient.service.vtjclient.VtjSoapTransport.RequestType.HUOLTAJA_HUOLLETTAVA
import java.time.LocalDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FridgeFamilyServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mockToday = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    private val employee = DevEmployee()

    lateinit var adult1: PersonDTO
    lateinit var adult2: PersonDTO
    lateinit var child1: PersonDTO
    lateinit var child2: PersonDTO
    private val partnershipCreator = employee.evakaUserId

    @BeforeEach
    fun setup() {
        adult1 = createPerson("140881-172X", "Isä")
        adult2 = createPerson("150786-1766", "Äiti")
        child1 = createPerson("120915A931W", "Lapsi1")
        child2 = createPerson("101221A999S", "Lapsi2")
        MockVtjClient.addPersons(adult1, adult2, child1, child2)
    }

    @Autowired lateinit var fridgeFamilyService: FridgeFamilyService

    @Test
    fun `First adult becomes the head of all common children`() {
        MockVtjClient.addDependants(adult1, child1, child2)
        MockVtjClient.addDependants(adult2, child1, child2)

        db.transaction {
            it.insert(employee)
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

        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            adult1.id,
        )

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
    fun `Do not add fridge parentships automatically for children with foster parent`() {
        MockVtjClient.addDependants(adult1, child1, child2)

        db.transaction { tx ->
            tx.updateLastStrongLogin(mockToday.now(), adult1.id)
            tx.insert(
                DevFosterParent(
                    childId = child1.id,
                    parentId = adult2.id,
                    validDuring =
                        DateRange(
                            mockToday.today().minusMonths(1),
                            mockToday.today().plusMonths(1),
                        ),
                    modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                    modifiedAt = mockToday.now(),
                )
            )
        }

        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            adult1.id,
        )

        db.read {
            assertEquals(
                0,
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
        MockVtjClient.addDependants(adult1, child1)
        MockVtjClient.addDependants(adult2, child1)

        db.transaction {
            it.insert(employee)
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

        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            adult1.id,
        )

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size,
            )
        }

        MockVtjClient.addDependants(adult1, child2)
        MockVtjClient.addDependants(adult2, child2)

        // Note: the person to update here is the other adult2. Implementation should
        // detect that her partner already has a fridge family and add the child to that one instead
        // of
        // creating a new fridge family for this other adult2
        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            adult1.id,
        )

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
        MockVtjClient.addDependants(adult1, child1)
        MockVtjClient.addDependants(adult2, child1)

        fridgeFamilyService.refreshFamily(
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

    @Test
    fun `refresh keeps existing guardian relations and removes ended ones`() {
        MockVtjClient.addDependants(adult1, child1)
        MockVtjClient.addDependants(adult2, child1)
        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            child1.id,
        )
        val createdBefore = db.read { it.getGuardianCreated(adult1.id, child1.id) }

        // adult2 is no longer a guardian of child1 in VTJ
        MockVtjClient.reset()
        MockVtjClient.addPersons(adult1, adult2, child1, child2)
        MockVtjClient.addDependants(adult1, child1)
        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            child1.id,
        )

        db.read { tx ->
            assertEquals(listOf(adult1.id), tx.getChildGuardians(child1.id))
            assertEquals(createdBefore, tx.getGuardianCreated(adult1.id, child1.id))
        }
    }

    @Test
    fun `refreshing a child adds it once to the family of its guardians who are partners`() {
        MockVtjClient.addDependants(adult1, child1)
        MockVtjClient.addDependants(adult2, child1)

        db.transaction {
            it.insert(employee)
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

        fridgeFamilyService.refreshFamily(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            child1.id,
        )

        db.read {
            assertEquals(
                listOf(adult1.id),
                it.getParentships(null, child1.id, false, DateRange(mockToday.today(), null)).map {
                    parentship ->
                    parentship.headOfChildId
                },
            )
        }
    }

    @Autowired lateinit var parentshipService: ParentshipService

    @Test
    fun testVtjRequestCounts() {
        val mockVtjSoapTransport: VtjSoapTransport = MockVtjSoapTransport()
        MockVtjSoapTransport.resetQueryCounts()

        val vtjSyncService = VtjSyncService(SoapVtjClient(mockVtjSoapTransport, VtjHenkiloMapper()))
        val service =
            FridgeFamilyService(
                PersonService(
                    SoapVtjClient(mockVtjSoapTransport, VtjHenkiloMapper()),
                    vtjSyncService,
                ),
                vtjSyncService,
                parentshipService,
            )

        val parent = DevPerson(ssn = adult1.ssn!!.value)
        val child = DevPerson(ssn = child1.ssn!!.value)

        db.transaction { it.updateLastStrongLogin(mockToday.now(), adult1.id) }
        MockVtjSoapTransport.addHUOLTAJAHUOLLETTAVARequestExpectation(parent, listOf(child))
        service.refreshFamily(db, AuthenticatedUser.SystemInternalUser, mockToday, adult1.id)

        MockVtjSoapTransport.assertRequests(HUOLTAJA_HUOLLETTAVA to parent)
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
                        streetAddress = "Katu 1",
                        postalCode = "20100",
                        postOffice = "Espoo",
                    ),
                    DevPersonType.RAW_ROW,
                )
            it.getPersonById(id)!!
        }
    }

    private fun Database.Read.getGuardianCreated(guardianId: PersonId, childId: PersonId) =
        createQuery {
            sql(
                "SELECT created FROM guardian WHERE guardian_id = ${bind(guardianId)} AND child_id = ${bind(childId)}"
            )
        }
        .exactlyOne<HelsinkiDateTime>()
}
