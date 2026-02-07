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
import org.springframework.test.context.bean.override.mockito.MockitoBean

class DVVBatchRefreshServiceIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {

    @MockitoBean(name = "personService") lateinit var personService: PersonService

    @MockitoBean lateinit var parentshipService: ParentshipService

    @Autowired lateinit var fridgeFamilyService: FridgeFamilyService

    @MockitoBean protected lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    lateinit var service: VTJBatchRefreshService

    private val adult1 = DevPerson(ssn = "010180-1232")
    private val adult2 = DevPerson(ssn = "010279-123L")
    private val child = DevPerson(ssn = "010617A123U", dateOfBirth = LocalDate.of(2017, 6, 1))

    private val lastDayBefore18YearsOld = child.dateOfBirth.plusYears(18).minusDays(1)
    private val user = AuthenticatedUser.SystemInternalUser

    @BeforeEach
    fun setUp() {
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.ADULT)
            tx.insert(adult2, DevPersonType.ADULT)
        }
        service =
            VTJBatchRefreshService(
                fridgeFamilyService = fridgeFamilyService,
                asyncJobRunner = asyncJobRunner,
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
                    postOffice = person.postOffice,
                    residenceCode = "1",
                ),
            residenceCode = person.residenceCode,
            municipalityOfResidence = person.municipalityOfResidence,
            phone = "",
            backupPhone = "",
            email = null,
            children = emptyList(),
            nationalities = emptySet(),
            nativeLanguage = null,
            restrictedDetails = RestrictedDetails(enabled = false),
        )

    @Test
    fun `children in same address are added`() {
        val dto = getDto(adult1).copy(children = listOf(getDto(child)))
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(adult1.id), any()))
            .thenReturn(dto)
        whenever(personService.personsLiveInTheSameAddress(any(), any())).thenReturn(true)

        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(adult1.id))
        verify(parentshipService)
            .createParentship(
                any(),
                any(),
                eq(child.id),
                eq(adult1.id),
                eq(LocalDate.now()),
                eq(lastDayBefore18YearsOld),
                eq(Creator.DVV),
            )
    }

    @Test
    fun `children in different address are not added`() {
        val dto =
            getDto(adult1)
                .copy(
                    children = listOf(getDto(child)),
                    address =
                        PersonAddressDTO(
                            origin = PersonAddressDTO.Origin.VTJ,
                            streetAddress = "different",
                            postalCode = "02770",
                            postOffice = "Espoo",
                            residenceCode = "2",
                        ),
                )
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(adult1.id), any()))
            .thenReturn(dto)
        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(adult1.id))
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
                    personId = adult1.id,
                    startDate = startDate,
                    createdAt = createdAt,
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = adult2.id,
                    startDate = startDate,
                    createdAt = createdAt,
                )
            )
        }

        val dto1 = getDto(adult1)
        val dto2 = getDto(adult2).copy(children = listOf(getDto(child)))
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(adult1.id), any()))
            .thenReturn(dto1)
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(adult2.id), any()))
            .thenReturn(dto2)
        whenever(personService.personsLiveInTheSameAddress(any(), any())).thenReturn(true)

        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(adult1.id))
        verify(parentshipService)
            .createParentship(
                any(),
                any(),
                eq(child.id),
                eq(adult1.id),
                eq(LocalDate.now()),
                eq(lastDayBefore18YearsOld),
                eq(Creator.DVV),
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
                    personId = adult1.id,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = createdAt,
                )
            )
            tx.insert(
                DevFridgePartner(
                    partnershipId = partnershipId,
                    indx = 2,
                    otherIndx = 1,
                    personId = adult2.id,
                    startDate = startDate,
                    endDate = endDate,
                    createdAt = createdAt,
                )
            )
        }

        val dto1 = getDto(adult1)
        val dto2 = getDto(adult2).copy(children = listOf(getDto(child)))
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(adult1.id), any()))
            .thenReturn(dto1)
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(adult2.id), any()))
            .thenReturn(dto2)

        service.doVTJRefresh(db, RealEvakaClock(), AsyncJob.VTJRefresh(adult1.id))
        verifyNoInteractions(parentshipService)
    }
}
