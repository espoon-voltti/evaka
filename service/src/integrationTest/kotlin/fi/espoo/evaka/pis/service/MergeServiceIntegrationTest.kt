// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import fi.espoo.evaka.shared.JdbcIntegrationTest
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestIncome
import fi.espoo.evaka.shared.dev.insertTestParentship
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.testDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.dao.DataAccessException
import java.time.LocalDate
import java.util.UUID

class MergeServiceIntegrationTest : JdbcIntegrationTest() {
    lateinit var mergeService: MergeService

    @MockBean
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    internal fun setUp() {
        mergeService = MergeService(jdbcTemplate, asyncJobRunnerMock)
        whenever(objectMapper.writeValueAsString(any())).thenReturn("{}")
    }

    @Test
    fun `empty person can be deleted`() {
        val id = UUID.randomUUID()
        withSpringHandle(dataSource) {
            it.insertTestPerson(DevPerson(id = id))
            it.insertTestChild(DevChild(id))
        }

        val countBefore = jdbcTemplate.query("SELECT 1 FROM person WHERE id = :id", mapOf("id" to id)) { _, _ -> 1 }.size
        assertEquals(1, countBefore)

        mergeService.deleteEmptyPerson(id)

        val countAfter = jdbcTemplate.query("SELECT 1 FROM person WHERE id = :id", mapOf("id" to id)) { _, _ -> 1 }.size
        assertEquals(0, countAfter)
    }

    @Test
    fun `cannot delete person with data - placement`() {
        val id = UUID.randomUUID()
        withSpringHandle(dataSource) {
            it.insertTestPerson(DevPerson(id = id))
            it.insertTestChild(DevChild(id))
            it.insertTestPlacement(
                DevPlacement(
                    childId = id,
                    unitId = testDaycare.id
                )
            )
        }

        assertThrows<DataAccessException> { mergeService.deleteEmptyPerson(id) }
    }

    @Test
    fun `merging adult moves incomes and sends update event`() {
        val adultId = UUID.randomUUID()
        val adultIdDuplicate = UUID.randomUUID()
        val childId = UUID.randomUUID()
        withSpringHandle(dataSource) {
            it.insertTestPerson(DevPerson(id = childId))
            it.insertTestPerson(DevPerson(id = adultId))
            it.insertTestPerson(DevPerson(id = adultIdDuplicate))
        }
        insertTestParentship(jdbcTemplate, headOfChild = adultId, childId = childId, startDate = LocalDate.of(2015, 1, 1), endDate = LocalDate.of(2030, 1, 1))

        val validFrom = LocalDate.of(2010, 1, 1)
        val validTo = LocalDate.of(2020, 12, 30)
        insertTestIncome(jdbcTemplate, objectMapper, adultIdDuplicate, validFrom = validFrom, validTo = validTo)

        val countBefore = jdbcTemplate.query("SELECT 1 FROM income WHERE person_id = :id", mapOf("id" to adultIdDuplicate)) { _, _ -> 1 }.size
        assertEquals(1, countBefore)

        mergeService.mergePeople(adultId, adultIdDuplicate)

        val countAfter = jdbcTemplate.query("SELECT 1 FROM income WHERE person_id = :id", mapOf("id" to adultId)) { _, _ -> 1 }.size
        assertEquals(1, countAfter)

        verify(asyncJobRunnerMock).plan(
            eq(listOf(NotifyFamilyUpdated(adultId, validFrom, validTo))), any(), any(), any()
        )
    }

    @Test
    fun `merging child moves placements and service needs`() {
        val childId = UUID.randomUUID()
        val childIdDuplicate = UUID.randomUUID()
        withSpringHandle(dataSource) {
            it.insertTestPerson(DevPerson(id = childId))
            it.insertTestPerson(DevPerson(id = childIdDuplicate))
            it.insertTestChild(DevChild(childIdDuplicate))
        }
        val employeeId = withSpringHandle(dataSource) { it.insertTestEmployee(DevEmployee()) }
        val from = LocalDate.of(2010, 1, 1)
        val to = LocalDate.of(2020, 12, 30)
        withSpringHandle(dataSource) { it.insertTestPlacement(DevPlacement(childId = childIdDuplicate, unitId = testDaycare.id, startDate = from, endDate = to)) }
        insertTestServiceNeed(jdbcTemplate, childIdDuplicate, startDate = from, endDate = to, updatedBy = employeeId)

        val countBefore = jdbcTemplate.query("SELECT 1 FROM placement WHERE child_id = :id", mapOf("id" to childIdDuplicate)) { _, _ -> 1 }.size
        assertEquals(1, countBefore)

        mergeService.mergePeople(childId, childIdDuplicate)

        val countAfter = jdbcTemplate.query("SELECT 1 FROM placement WHERE child_id = :id", mapOf("id" to childId)) { _, _ -> 1 }.size
        assertEquals(1, countAfter)

        verifyZeroInteractions(asyncJobRunnerMock)
    }

    @Test
    fun `merging child sends update event to head of child`() {
        val adultId = UUID.randomUUID()
        val childId = UUID.randomUUID()
        val childIdDuplicate = UUID.randomUUID()
        withSpringHandle(dataSource) {
            it.insertTestPerson(DevPerson(id = adultId))
            it.insertTestPerson(DevPerson(id = childId))
            it.insertTestChild(DevChild(childId))
            it.insertTestPerson(DevPerson(id = childIdDuplicate))
            it.insertTestChild(DevChild(childIdDuplicate))
        }
        insertTestParentship(jdbcTemplate, headOfChild = adultId, childId = childId, startDate = LocalDate.of(2015, 1, 1), endDate = LocalDate.of(2030, 1, 1))
        val placementStart = LocalDate.of(2017, 1, 1)
        val placementEnd = LocalDate.of(2020, 12, 30)
        withSpringHandle(dataSource) { it.insertTestPlacement(DevPlacement(childId = childIdDuplicate, unitId = testDaycare.id, startDate = placementStart, endDate = placementEnd)) }

        mergeService.mergePeople(childId, childIdDuplicate)

        verify(asyncJobRunnerMock).plan(
            eq(listOf(NotifyFamilyUpdated(adultId, placementStart, placementEnd))), any(), any(), any()
        )
    }
}
