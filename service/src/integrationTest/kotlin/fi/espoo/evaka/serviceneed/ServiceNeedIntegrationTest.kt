// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestServiceNeed
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ServiceNeedIntegrationTest : FullApplicationTest() {
    private val unitSupervisor = AuthenticatedUser.Employee(unitSupervisorOfTestDaycare.id, setOf(UserRole.UNIT_SUPERVISOR))
    private val admin = AuthenticatedUser.Employee(UUID.randomUUID(), setOf(UserRole.ADMIN))
    private val serviceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

    @BeforeEach
    private fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            insertTestPlacement(
                h,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now().minusDays(1),
                endDate = LocalDate.now().plusDays(3)
            )
        }
    }

    @Test
    fun `post first service need with end date`() {
        val serviceNeed = whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(20),
                hoursPerWeek = 37.5,
                partDay = true,
                partWeek = false,
                shiftCare = true
            )
        )

        assertEquals(
            ServiceNeed(
                id = serviceNeed.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                hoursPerWeek = 37.5,
                partDay = true,
                partWeek = false,
                shiftCare = true,
                updated = serviceNeed.updated,
                updatedByName = "${unitSupervisorOfTestDaycare.firstName} ${unitSupervisorOfTestDaycare.lastName}"
            ),
            serviceNeed
        )
    }

    @Test
    fun `post first service need without end date`() {
        val serviceNeed = whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(10),
                endDate = null,
                hoursPerWeek = 37.5,
                partDay = true,
                partWeek = false,
                shiftCare = true
            )
        )

        assertEquals(
            ServiceNeed(
                id = serviceNeed.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = null,
                hoursPerWeek = 37.5,
                partDay = true,
                partWeek = false,
                shiftCare = true,
                updated = serviceNeed.updated,
                updatedByName = "${unitSupervisorOfTestDaycare.firstName} ${unitSupervisorOfTestDaycare.lastName}"
            ),
            serviceNeed
        )
    }

    @Test
    fun `post service need, no overlap`() {
        givenServiceNeed(1, 15)
        whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(16),
                endDate = testDate(30),
                hoursPerWeek = 37.5
            )
        )

        val serviceNeeds = jdbi.handle { h -> getServiceNeedsByChild(h, testChild_1.id) }
        assertEquals(2, serviceNeeds.size)
        assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) })
        assertTrue(serviceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) })
    }

    @Test
    fun `post service need, fully encloses previous - responds 409`() {
        givenServiceNeed(10, 20)
        whenPostServiceNeedThenExpectError(
            ServiceNeedRequest(
                startDate = testDate(1),
                endDate = testDate(30),
                hoursPerWeek = 37.5
            ),
            409
        )
    }

    @Test
    fun `post service need, starts on same day, ends later - responds 409`() {
        givenServiceNeed(10, 20)
        whenPostServiceNeedThenExpectError(
            ServiceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(25),
                hoursPerWeek = 37.5
            ),
            409
        )
    }

    @Test
    fun `post service need, starts on same day, no end date - responds 409`() {
        givenServiceNeed(10, 20)
        whenPostServiceNeedThenExpectError(
            ServiceNeedRequest(
                startDate = testDate(10),
                endDate = null,
                hoursPerWeek = 37.5
            ),
            409
        )
    }

    @Test
    fun `post service need, overlaps start of previous - responds 409`() {
        givenServiceNeed(10, 20)
        whenPostServiceNeedThenExpectError(
            ServiceNeedRequest(
                startDate = testDate(1),
                endDate = testDate(10),
                hoursPerWeek = 37.5
            ),
            409
        )
    }

    @Test
    fun `post service need, overlaps end of previous with end date - previous gets shortened`() {
        givenServiceNeed(10, 20)
        whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(20),
                endDate = testDate(30),
                hoursPerWeek = 37.5
            )
        )

        val serviceNeeds = jdbi.handle { h -> getServiceNeedsByChild(h, testChild_1.id) }
        assertEquals(2, serviceNeeds.size)
        assertTrue(serviceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(19) })
        assertTrue(serviceNeeds.any { it.startDate == testDate(20) && it.endDate == testDate(30) })
    }

    @Test
    fun `post service need, overlaps end of previous without end date - previous gets shortened`() {
        givenServiceNeed(10, null)
        whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(20),
                endDate = testDate(30),
                hoursPerWeek = 37.5
            )
        )

        val serviceNeeds = jdbi.handle { h -> getServiceNeedsByChild(h, testChild_1.id) }
        assertEquals(2, serviceNeeds.size)
        assertTrue(serviceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(19) })
        assertTrue(serviceNeeds.any { it.startDate == testDate(20) && it.endDate == testDate(30) })
    }

    @Test
    fun `post service need with end date, is within previous - previous gets shortened`() {
        givenServiceNeed(10, 20)
        whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(11),
                endDate = testDate(15),
                hoursPerWeek = 37.5
            )
        )

        val serviceNeeds = jdbi.handle { h -> getServiceNeedsByChild(h, testChild_1.id) }
        assertEquals(2, serviceNeeds.size)
        assertTrue(serviceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(10) })
        assertTrue(serviceNeeds.any { it.startDate == testDate(11) && it.endDate == testDate(15) })
    }

    @Test
    fun `post service need without end date, is within previous - previous gets shortened`() {
        givenServiceNeed(10, null)
        whenPostServiceNeedThenExpectSuccess(
            ServiceNeedRequest(
                startDate = testDate(11),
                endDate = testDate(15),
                hoursPerWeek = 37.5
            )
        )

        val serviceNeeds = jdbi.handle { h -> getServiceNeedsByChild(h, testChild_1.id) }
        assertEquals(2, serviceNeeds.size)
        assertTrue(serviceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(10) })
        assertTrue(serviceNeeds.any { it.startDate == testDate(11) && it.endDate == testDate(15) })
    }

    @Test
    fun `get service needs`() {
        givenServiceNeed(1, 5, testChild_1.id)
        givenServiceNeed(25, null, testChild_1.id)
        givenServiceNeed(25, 30, testChild_2.id)

        val serviceNeeds = whenGetServiceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(2, serviceNeeds.size)
        with(serviceNeeds[0]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(25), startDate)
            assertNull(endDate)
        }
        with(serviceNeeds[1]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(1), startDate)
            assertEquals(testDate(5), endDate)
        }
    }

    @Test
    fun `update service need`() {
        val id1 = givenServiceNeed(1, 5)
        val id2 = givenServiceNeed(10, 20)
        val updated = whenPutServiceNeedThenExpectSuccess(
            id2,
            ServiceNeedRequest(
                startDate = testDate(9),
                endDate = testDate(22),
                hoursPerWeek = 20.0
            )
        )

        assertEquals(id2, updated.id)
        assertEquals(testDate(9), updated.startDate)
        assertEquals(testDate(22), updated.endDate)
        assertEquals(20.0, updated.hoursPerWeek)

        val fetched = whenGetServiceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(2, fetched.size)
        assertTrue(fetched.any { it.id == id1 && it.startDate == testDate(1) && it.endDate == testDate(5) })
        assertTrue(fetched.any { it.id == id2 && it.startDate == testDate(9) && it.endDate == testDate(22) })
    }

    @Test
    fun `update service need, not found responds 404`() {
        whenPutServiceNeedThenExpectError(
            UUID.randomUUID(),
            ServiceNeedRequest(
                startDate = testDate(9),
                endDate = testDate(22),
                hoursPerWeek = 20.0
            ),
            404
        )
    }

    @Test
    fun `update service need, conflict returns 409`() {
        givenServiceNeed(1, 5)
        val id2 = givenServiceNeed(10, 20)

        whenPutServiceNeedThenExpectError(
            id2,
            ServiceNeedRequest(
                startDate = testDate(5),
                endDate = testDate(22),
                hoursPerWeek = 20.0
            ),
            409
        )
    }

    @Test
    fun `delete service need`() {
        val id1 = givenServiceNeed(1, 5)
        val id2 = givenServiceNeed(10, 20)

        whenDeleteServiceNeedThenExpectSuccess(id2)

        val serviceNeeds = whenGetServiceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(1, serviceNeeds.size)
        assertEquals(id1, serviceNeeds.first().id)
    }

    @Test
    fun `delete service need, not found responds 404`() {
        whenDeleteServiceNeedThenExpectError(UUID.randomUUID(), 404, admin)
    }

    @Test
    fun `when service worker tries to remove service need, respond 403`() {
        whenDeleteServiceNeedThenExpectError(UUID.randomUUID(), 403, serviceWorker)
    }

    @Test
    fun `getServiceNeedsByChildDuringPeriod works with null end date in DB`() {
        jdbi.handle { h ->
            val startDate = 5
            givenServiceNeed(startDate, null, testChild_1.id)

            val results =
                getServiceNeedsByChildDuringPeriod(h, testChild_1.id, testDate(startDate), testDate(startDate + 5))
            assertEquals(1, results.size)

            val serviceNeed = results[0]
            assertEquals(testDate(startDate), serviceNeed.startDate)
            assertNull(serviceNeed.endDate)
        }
    }

    @Test
    fun `getServiceNeedsByChildDuringPeriod works with null end date in query`() {
        jdbi.handle { h ->
            val startDate = 5
            val endDate = 10
            givenServiceNeed(startDate, endDate, testChild_1.id)

            val results = getServiceNeedsByChildDuringPeriod(h, testChild_1.id, testDate(startDate), null)
            assertEquals(1, results.size)

            val serviceNeed = results[0]
            assertEquals(testDate(startDate), serviceNeed.startDate)
            assertEquals(testDate(endDate), serviceNeed.endDate)
        }
    }

    @Test
    fun `getServiceNeedsByChildDuringPeriod works with null end date in query and DB`() {
        jdbi.handle { h ->
            val startDate = 5
            givenServiceNeed(startDate, null, testChild_1.id)

            val results = getServiceNeedsByChildDuringPeriod(h, testChild_1.id, testDate(startDate), null)
            assertEquals(1, results.size)

            val serviceNeed = results[0]
            assertEquals(testDate(startDate), serviceNeed.startDate)
            assertNull(serviceNeed.endDate)
        }
    }

    @Test
    fun `getServiceNeedsByChildDuringPeriod does not return anything when query dates doesn't match`() {
        jdbi.handle { h ->
            val startDate = 5
            givenServiceNeed(startDate, null, testChild_1.id)

            val results = getServiceNeedsByChildDuringPeriod(h, testChild_1.id, testDate(1), testDate(4))
            assertEquals(0, results.size)
        }
    }

    private fun testDate(day: Int) = LocalDate.of(2000, 1, day)

    private fun givenServiceNeed(start: Int, end: Int?, childId: UUID = testChild_1.id): UUID {
        return jdbi.handle { h ->
            insertTestServiceNeed(
                h = h,
                childId = childId,
                startDate = testDate(start),
                endDate = if (end == null) null else testDate(end),
                updatedBy = unitSupervisor.id
            )
        }
    }

    private fun whenPostServiceNeedThenExpectSuccess(request: ServiceNeedRequest): ServiceNeed {
        val (_, res, result) = http.post("/children/${testChild_1.id}/service-needs")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(unitSupervisor)
            .responseObject<ServiceNeed>(objectMapper)

        assertEquals(201, res.statusCode)
        return result.get()
    }

    private fun whenPostServiceNeedThenExpectError(request: ServiceNeedRequest, status: Int) {
        val (_, res, _) = http.post("/children/${testChild_1.id}/service-needs")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(unitSupervisor)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenGetServiceNeedsThenExpectSuccess(childId: UUID): List<ServiceNeed> {
        val (_, res, result) = http.get("/children/$childId/service-needs")
            .asUser(unitSupervisor)
            .responseObject<List<ServiceNeed>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutServiceNeedThenExpectSuccess(id: UUID, request: ServiceNeedRequest): ServiceNeed {
        val (_, res, result) = http.put("/service-needs/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(unitSupervisor)
            .responseObject<ServiceNeed>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutServiceNeedThenExpectError(id: UUID, request: ServiceNeedRequest, status: Int) {
        val (_, res, _) = http.put("/service-needs/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(admin)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenDeleteServiceNeedThenExpectSuccess(id: UUID) {
        val (_, res, _) = http.delete("/service-needs/$id")
            .asUser(unitSupervisor)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun whenDeleteServiceNeedThenExpectError(id: UUID, status: Int, authenticatedUser: AuthenticatedUser) {
        val (_, res, _) = http.delete("/service-needs/$id")
            .asUser(authenticatedUser)
            .response()

        assertEquals(status, res.statusCode)
    }
}
