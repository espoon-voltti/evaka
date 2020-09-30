// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.invoicing.domain.PersonData
import fi.espoo.evaka.shared.JdbcIntegrationTest
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testAdult_2
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.vtjclient.dto.PersonDataSource
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import java.time.LocalDate
import java.util.UUID

class VTJBatchRefreshServiceIntegrationTest : JdbcIntegrationTest() {
    @MockBean
    lateinit var personService: PersonService

    @MockBean
    lateinit var parentshipService: ParentshipService

    @Autowired
    lateinit var asyncJobRunner: AsyncJobRunner

    @Autowired
    lateinit var jdbc: NamedParameterJdbcTemplate

    @Autowired
    lateinit var txManager: PlatformTransactionManager

    lateinit var service: VTJBatchRefreshService

    val lastDayBefore18YearsOld = testChild_1.dateOfBirth.plusYears(18).minusDays(1)
    private val user = AuthenticatedUser(UUID.fromString("00000000-0000-0000-0000-000000000000"), setOf())

    @BeforeEach
    internal fun setUp() {
        service = VTJBatchRefreshService(
            personService = personService,
            parentshipService = parentshipService,
            asyncJobRunner = asyncJobRunner,
            jdbc = jdbc,
            txManager = txManager
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
        restrictedDetails = RestrictedDetails(enabled = false),
        source = PersonDataSource.VTJ
    )

    @Test
    fun `children in same address are added`() {
        val dto = getDto(testAdult_1).copy(
            children = listOf(getDto(testChild_1))
        )
        whenever(personService.getUpToDatePersonWithChildren(user, testAdult_1.id)).thenReturn(dto)

        service.doVTJRefresh(VTJRefresh(testAdult_1.id, user.id))
        verify(parentshipService).createParentship(
            headOfChildId = testAdult_1.id,
            childId = testChild_1.id,
            startDate = LocalDate.now(),
            endDate = lastDayBefore18YearsOld
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
        whenever(personService.getUpToDatePersonWithChildren(user, testAdult_1.id)).thenReturn(dto)

        service.doVTJRefresh(VTJRefresh(testAdult_1.id, user.id))
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
        jdbc.update(
            sql,
            mapOf(
                "partnershipId" to partnershipId,
                "index" to 1,
                "personId" to testAdult_1.id,
                "startDate" to LocalDate.of(2000, 1, 1)
            )
        )
        jdbc.update(
            sql,
            mapOf(
                "partnershipId" to partnershipId,
                "index" to 2,
                "personId" to testAdult_2.id,
                "startDate" to LocalDate.of(2000, 1, 1)
            )
        )

        val dto1 = getDto(testAdult_1)
        val dto2 = getDto(testAdult_2).copy(
            children = listOf(getDto(testChild_1))
        )
        whenever(personService.getUpToDatePersonWithChildren(user, testAdult_1.id)).thenReturn(dto1)
        whenever(personService.getUpToDatePersonWithChildren(user, testAdult_2.id)).thenReturn(dto2)

        service.doVTJRefresh(VTJRefresh(testAdult_1.id, user.id))
        verify(parentshipService).createParentship(
            headOfChildId = testAdult_1.id,
            childId = testChild_1.id,
            startDate = LocalDate.now(),
            endDate = lastDayBefore18YearsOld
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
        jdbc.update(
            sql,
            mapOf(
                "partnershipId" to partnershipId,
                "index" to 1,
                "personId" to testAdult_1.id,
                "startDate" to LocalDate.of(2000, 1, 1),
                "endDate" to LocalDate.of(2010, 1, 1)
            )
        )
        jdbc.update(
            sql,
            mapOf(
                "partnershipId" to partnershipId,
                "index" to 2,
                "personId" to testAdult_2.id,
                "startDate" to LocalDate.of(2000, 1, 1),
                "endDate" to LocalDate.of(2010, 1, 1)
            )
        )

        val dto1 = getDto(testAdult_1)
        val dto2 = getDto(testAdult_2).copy(
            children = listOf(getDto(testChild_1))
        )
        whenever(personService.getUpToDatePersonWithChildren(user, testAdult_1.id)).thenReturn(dto1)
        whenever(personService.getUpToDatePersonWithChildren(user, testAdult_2.id)).thenReturn(dto2)

        service.doVTJRefresh(VTJRefresh(testAdult_1.id, user.id))
        verifyZeroInteractions(parentshipService)
    }
}
