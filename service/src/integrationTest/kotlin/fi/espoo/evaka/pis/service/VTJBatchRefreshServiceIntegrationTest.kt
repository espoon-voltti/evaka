// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import java.time.LocalDate
import java.util.UUID

class VTJBatchRefreshServiceIntegrationTest : FullApplicationTest() {

    @MockBean(name = "personService")
    lateinit var personService: PersonService

    @MockBean
    lateinit var parentshipService: ParentshipService

    @Autowired
    lateinit var fridgeFamilyService: FridgeFamilyService

    @MockBean
    protected lateinit var asyncJobRunner: AsyncJobRunner

    lateinit var service: VTJBatchRefreshService

    val lastDayBefore18YearsOld = testChild_1.dateOfBirth.plusYears(18).minusDays(1)
    private val user = AuthenticatedUser.Employee(UUID.randomUUID(), emptySet())

    @BeforeEach
    internal fun setUp() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
        service = VTJBatchRefreshService(
            fridgeFamilyService = fridgeFamilyService,
            asyncJobRunner = asyncJobRunner
        )
    }

    fun getDto(person: PersonData.Detailed) = PersonWithChildrenDTO(
        id = person.id,
        socialSecurityNumber = person.ssn,
        dateOfBirth = person.dateOfBirth,
        dateOfDeath = person.dateOfDeath,
        firstName = person.firstName,
        lastName = person.lastName,
        addresses = setOf(
            PersonAddressDTO(
                origin = PersonAddressDTO.Origin.VTJ,
                streetAddress = person.streetAddress!!,
                postalCode = person.postalCode!!,
                city = person.postOffice!!,
                residenceCode = "1"
            )
        ),
        residenceCode = person.residenceCode,
        children = emptyList(),
        nationalities = emptySet(),
        nativeLanguage = null,
        restrictedDetails = RestrictedDetails(enabled = false)
    )

    @Test
    fun `children in same address are added`() {
        val dto = getDto(testAdult_1).copy(
            children = listOf(getDto(testChild_1))
        )
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any())).thenReturn(dto)

        service.doVTJRefresh(dbInstance(), VTJRefresh(testAdult_1.id, user.id))
        verify(parentshipService).createParentship(
            any(),
            eq(testChild_1.id),
            eq(testAdult_1.id),
            eq(LocalDate.now()),
            eq(lastDayBefore18YearsOld)
        )
    }

    @Test
    fun `children in different address are not added`() {
        val dto = getDto(testAdult_1).copy(
            children = listOf(getDto(testChild_1)),
            addresses = setOf(
                PersonAddressDTO(
                    origin = PersonAddressDTO.Origin.VTJ,
                    streetAddress = "different",
                    postalCode = "02770",
                    city = "Espoo",
                    residenceCode = "2"
                )
            )
        )
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any())).thenReturn(dto)
        service.doVTJRefresh(dbInstance(), VTJRefresh(testAdult_1.id, user.id))
        verifyZeroInteractions(parentshipService)
    }

    @Test
    fun `children from partner in same address are added`() {
        // language=sql
        val sql =
            """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:partnershipId, :index, :personId, :startDate, 'infinity')
            """.trimIndent()
        val partnershipId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createUpdate(sql)
                .bind("partnershipId", partnershipId)
                .bind("index", 1)
                .bind("personId", testAdult_1.id)
                .bind("startDate", LocalDate.of(2000, 1, 1))
                .execute()

            tx.createUpdate(sql)
                .bind("partnershipId", partnershipId)
                .bind("index", 2)
                .bind("personId", testAdult_2.id)
                .bind("startDate", LocalDate.of(2000, 1, 1))
                .execute()
        }

        val dto1 = getDto(testAdult_1)
        val dto2 = getDto(testAdult_2).copy(
            children = listOf(getDto(testChild_1))
        )
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any())).thenReturn(dto1)
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_2.id), any())).thenReturn(dto2)

        service.doVTJRefresh(dbInstance(), VTJRefresh(testAdult_1.id, user.id))
        verify(parentshipService).createParentship(
            any(),
            eq(testChild_1.id),
            eq(testAdult_1.id),
            eq(LocalDate.now()),
            eq(lastDayBefore18YearsOld)
        )
    }

    @Test
    fun `children from ex-partners are not added`() {
        // language=sql
        val sql =
            """
            INSERT INTO fridge_partner (partnership_id, indx, person_id, start_date, end_date)
            VALUES (:partnershipId, :index, :personId, :startDate, :endDate)
            """.trimIndent()
        val partnershipId = UUID.randomUUID()
        db.transaction { tx ->
            tx.createUpdate(sql)
                .bind("partnershipId", partnershipId)
                .bind("index", 1)
                .bind("personId", testAdult_1.id)
                .bind("startDate", LocalDate.of(2000, 1, 1))
                .bind("endDate", LocalDate.of(2010, 1, 1))
                .execute()

            tx.createUpdate(sql)
                .bind("partnershipId", partnershipId)
                .bind("index", 2)
                .bind("personId", testAdult_2.id)
                .bind("startDate", LocalDate.of(2000, 1, 1))
                .bind("endDate", LocalDate.of(2010, 1, 1))
                .execute()
        }

        val dto1 = getDto(testAdult_1)
        val dto2 = getDto(testAdult_2).copy(
            children = listOf(getDto(testChild_1))
        )
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_1.id), any())).thenReturn(dto1)
        whenever(personService.getPersonWithChildren(any(), eq(user), eq(testAdult_2.id), any())).thenReturn(dto2)

        service.doVTJRefresh(dbInstance(), VTJRefresh(testAdult_1.id, user.id))
        verifyZeroInteractions(parentshipService)
    }
}
