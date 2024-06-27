// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.pis.Creator
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.dev.DevFridgePartner
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean

class DVVBatchRefreshServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @MockBean(name = "personService") lateinit var personService: PersonService

    @MockBean lateinit var parentshipService: ParentshipService

    @Autowired lateinit var fridgeFamilyService: FridgeFamilyService

    @MockBean protected lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    lateinit var service: VTJBatchRefreshService

    val lastDayBefore18YearsOld = testChild_1.dateOfBirth.plusYears(18).minusDays(1)
    private val user = AuthenticatedUser.SystemInternalUser

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            listOf(testAdult_1, testAdult_2).forEach { tx.insert(it, DevPersonType.ADULT) }
        }
        service =
            VTJBatchRefreshService(
                fridgeFamilyService = fridgeFamilyService,
                asyncJobRunner = asyncJobRunner
            )
    }

    fun getDto(person: DevPerson) =
        PersonWithChildrenDTO(
            id = person.id,
            duplicateOf = person.duplicateOf,
            socialSecurityNumber = person.ssn,
            dateOfBirth = person.dateOfBirth,
            dateOfDeath = person.dateOfDeath,
            firstName = person.firstName,
            lastName = person.lastName,
            preferredName = "",
            address =
                PersonAddressDTO(
                    origin = PersonAddressDTO.Origin.VTJ,
                    streetAddress = person.streetAddress,
                    postalCode = person.postalCode,
                    city = person.postOffice,
                    residenceCode = "1"
                ),
            residenceCode = person.residenceCode,
            phone = "",
            backupPhone = "",
            email = null,
            children = emptyList(),
            nationalities = emptySet(),
            nativeLanguage = null,
            restrictedDetails = RestrictedDetails(enabled = false)
        )

    @Test
    fun `children in same address are added`() {
        val dto = getDto(testAdult_1).copy(children = listOf(getDto(testChild_1)))
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any()))
            .thenReturn(dto)

        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(testAdult_1.id))
        verify(parentshipService)
            .createParentship(
                any(),
                any(),
                eq(testChild_1.id),
                eq(testAdult_1.id),
                eq(LocalDate.now()),
                eq(lastDayBefore18YearsOld),
                eq(Creator.DVV)
            )
    }

    @Test
    fun `children in different address are not added`() {
        val dto =
            getDto(testAdult_1)
                .copy(
                    children = listOf(getDto(testChild_1)),
                    address =
                        PersonAddressDTO(
                            origin = PersonAddressDTO.Origin.VTJ,
                            streetAddress = "different",
                            postalCode = "02770",
                            city = "Espoo",
                            residenceCode = "2"
                        )
                )
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any()))
            .thenReturn(dto)
        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(testAdult_1.id))
        verifyNoInteractions(parentshipService)
    }

    @Test
    fun `children from partner in same address are added`() {
        val partnershipId = PartnershipId(UUID.randomUUID())
        db.transaction { tx ->
            val startDate = LocalDate.of(2000, 1, 1)
            val createdAt = HelsinkiDateTime.of(startDate, LocalTime.of(12, 0, 0))
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = testAdult_1.id,
                    startDate = startDate,
                    createdAt = createdAt
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = testAdult_2.id,
                    startDate = startDate,
                    createdAt = createdAt
                )
            )
        }

        val dto1 = getDto(testAdult_1)
        val dto2 = getDto(testAdult_2).copy(children = listOf(getDto(testChild_1)))
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any()))
            .thenReturn(dto1)
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_2.id), any()))
            .thenReturn(dto2)

        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(testAdult_1.id))
        verify(parentshipService)
            .createParentship(
                any(),
                any(),
                eq(testChild_1.id),
                eq(testAdult_1.id),
                eq(LocalDate.now()),
                eq(lastDayBefore18YearsOld),
                eq(Creator.DVV)
            )
    }

    @Test
    fun `children from ex-partners are not added`() {
        val partnershipId = PartnershipId(UUID.randomUUID())
        db.transaction { tx ->
            val startDate = LocalDate.of(2000, 1, 1)
            val endDate = LocalDate.of(2010, 1, 1)
            val createdAt = HelsinkiDateTime.of(startDate, LocalTime.of(12, 0, 0))
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 1,
                    otherIndx = 2,
                    personId = testAdult_1.id,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = createdAt
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = testAdult_2.id,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = createdAt
                )
            )
        }

        val dto1 = getDto(testAdult_1)
        val dto2 = getDto(testAdult_2).copy(children = listOf(getDto(testChild_1)))
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any()))
            .thenReturn(dto1)
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_2.id), any()))
            .thenReturn(dto2)

        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(testAdult_1.id))
        verifyNoInteractions(parentshipService)
    }
}
