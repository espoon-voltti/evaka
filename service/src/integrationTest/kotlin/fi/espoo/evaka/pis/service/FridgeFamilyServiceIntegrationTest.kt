// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.identity.getDobFromSsn
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.time.LocalDateTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FridgeFamilyServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mockToday = MockEvakaClock(HelsinkiDateTime.of(LocalDateTime.of(2022, 1, 1, 12, 0)))

    lateinit var adult1: PersonDTO
    lateinit var adult2: PersonDTO
    lateinit var child1: PersonDTO
    lateinit var child2: PersonDTO
    lateinit var adult1VtjPerson: VtjPerson
    lateinit var adult2VtjPerson: VtjPerson
    lateinit var child1VtjPerson: VtjPerson
    lateinit var child2VtjPerson: VtjPerson

    @BeforeEach
    fun setup() {
        adult1 = createPerson("140881-172X", "Isä")
        adult1VtjPerson = adult1.toVtjPerson()
        adult2 = createPerson("150786-1766", "Äiti")
        adult2VtjPerson = adult2.toVtjPerson()
        child1 = createPerson("120915A931W", "Lapsi1")
        child1VtjPerson = child1.toVtjPerson()
        child2 = createPerson("101221A999S", "Lapsi2")
        child2VtjPerson = child2.toVtjPerson()
    }

    @Autowired lateinit var fridgeFamilyService: FridgeFamilyService

    @Test
    fun `First adult becomes the head of all common children`() {
        MockPersonDetailsService.addPerson(
            adult1VtjPerson.copy(dependants = listOf(child1VtjPerson, child2VtjPerson))
        )

        MockPersonDetailsService.addPerson(
            adult2VtjPerson.copy(dependants = listOf(child1VtjPerson, child2VtjPerson))
        )

        db.transaction {
            it.createPartnership(
                adult1.id,
                adult2.id,
                mockToday.today(),
                null,
                false,
                null,
                null,
                mockToday.now()
            )
        }

        fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(adult1.id), mockToday)

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size
            )
            assertEquals(
                1,
                it.getParentships(adult1.id, child2.id, false, DateRange(mockToday.today(), null))
                    .size
            )
        }
    }

    @Test
    fun `New child is added to existing head of family`() {
        MockPersonDetailsService.addPerson(
            adult1VtjPerson.copy(dependants = listOf(child1VtjPerson))
        )

        MockPersonDetailsService.addPerson(
            adult2VtjPerson.copy(dependants = listOf(child1VtjPerson))
        )

        db.transaction {
            it.createPartnership(
                adult1.id,
                adult2.id,
                mockToday.today(),
                null,
                false,
                null,
                null,
                mockToday.now()
            )
        }

        fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(adult1.id), mockToday)

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size
            )
        }

        MockPersonDetailsService.upsertPerson(
            adult1VtjPerson.copy(dependants = listOf(child1VtjPerson, child2VtjPerson))
        )

        MockPersonDetailsService.addPerson(
            adult2VtjPerson.copy(dependants = listOf(child1VtjPerson, child2VtjPerson))
        )

        // Note: the person to update here is the other adult2. Implementation should
        // detect that her partner already has a fridge family and add the child to that one instead
        // of
        // creating a new fridge family for this other adult2
        fridgeFamilyService.doVTJRefresh(db, AsyncJob.VTJRefresh(adult1.id), mockToday)

        db.read {
            assertEquals(
                1,
                it.getParentships(adult1.id, child1.id, false, DateRange(mockToday.today(), null))
                    .size
            )
            assertEquals(
                1,
                it.getParentships(adult1.id, child2.id, false, DateRange(mockToday.today(), null))
                    .size
            )
        }
    }

    @Test
    fun `guardians are added to child`() {
        MockPersonDetailsService.addPerson(
            child1VtjPerson.copy(guardians = listOf(adult1VtjPerson, adult2VtjPerson))
        )
        MockPersonDetailsService.addPerson(
            adult1VtjPerson.copy(dependants = listOf(child1VtjPerson))
        )
        MockPersonDetailsService.addPerson(
            adult2VtjPerson.copy(dependants = listOf(child1VtjPerson))
        )

        fridgeFamilyService.updateGuardianOrChildFromVtj(
            db,
            AuthenticatedUser.SystemInternalUser,
            mockToday,
            child1.id
        )

        assertEquals(
            setOf(adult1.id, adult2.id),
            db.read { tx -> tx.getChildGuardians(child1.id) }.toSet()
        )
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
                        language = "fi"
                    ),
                    DevPersonType.RAW_ROW
                )
            it.getPersonById(id)!!
        }
    }
}
