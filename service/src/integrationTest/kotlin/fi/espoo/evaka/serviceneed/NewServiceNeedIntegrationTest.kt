// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.DaycarePlacementWithDetails
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.serviceneednew.NewServiceNeed
import fi.espoo.evaka.serviceneednew.NewServiceNeedController
import fi.espoo.evaka.serviceneednew.ServiceNeedOptionSummary
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.insertTestNewServiceNeed
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDaycareFullDay25to35
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snPreschoolDaycare45
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.unitSupervisorOfTestDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class NewServiceNeedIntegrationTest : FullApplicationTest() {
    private val unitSupervisor = AuthenticatedUser.Employee(unitSupervisorOfTestDaycare.id, setOf(UserRole.UNIT_SUPERVISOR))

    lateinit var placementId: UUID

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
            placementId = tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = testDate(1),
                endDate = testDate(30)
            )
        }
    }

    @Test
    fun `post first service need`() {
        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(30),
                optionId = snDefaultDaycare.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { res ->
            assertEquals(1, res.size)
            res.first().let { sn ->
                assertEquals(testDate(1), sn.startDate)
                assertEquals(testDate(30), sn.endDate)
                assertEquals(placementId, sn.placementId)
                assertEquals(ServiceNeedOptionSummary(snDefaultDaycare.id, snDefaultDaycare.name), sn.option)
            }
        }
    }

    @Test
    fun `post service need with inverted range`() {
        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(5),
                endDate = testDate(3),
                optionId = snDefaultDaycare.id,
                shiftCare = false
            ),
            expectedStatus = 400
        )
    }

    @Test
    fun `post service need with range outside placement`() {
        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(31),
                optionId = snDefaultDaycare.id,
                shiftCare = false
            ),
            expectedStatus = 400
        )
    }

    @Test
    fun `post service need with invalid option`() {
        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(31),
                optionId = snPreschoolDaycare45.id,
                shiftCare = false
            ),
            expectedStatus = 400
        )
    }

    @Test
    fun `post service need, no overlap`() {
        givenServiceNeed(1, 15, placementId)

        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(16),
                endDate = testDate(30),
                optionId = snDaycareFullDay35.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) })
        }
    }

    @Test
    fun `post service need, fully encloses previous`() {
        givenServiceNeed(10, 20, placementId)

        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(30),
                optionId = snDaycareFullDay35.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { res ->
            assertEquals(1, res.size)
            res.first().let { sn ->
                assertEquals(testDate(1), sn.startDate)
                assertEquals(testDate(30), sn.endDate)
                assertEquals(placementId, sn.placementId)
                assertEquals(ServiceNeedOptionSummary(snDaycareFullDay35.id, snDaycareFullDay35.name), sn.option)
            }
        }
    }

    @Test
    fun `post service need, starts during existing`() {
        givenServiceNeed(1, 30, placementId)

        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(16),
                endDate = testDate(30),
                optionId = snDaycareFullDay35.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) })
        }
    }

    @Test
    fun `post service need, ends during existing`() {
        givenServiceNeed(10, 30, placementId)

        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(1),
                endDate = testDate(20),
                optionId = snDaycareFullDay35.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(20) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(21) && it.endDate == testDate(30) })
        }
    }

    @Test
    fun `post service need, is inside existing`() {
        givenServiceNeed(1, 30, placementId)

        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(10),
                endDate = testDate(20),
                optionId = snDaycareFullDay35.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(3, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(9) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(20) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(21) && it.endDate == testDate(30) })
        }
    }

    @Test
    fun `post service need, spans multiple`() {
        givenServiceNeed(1, 9, placementId)
        givenServiceNeed(10, 19, placementId)
        givenServiceNeed(20, 30, placementId)

        postServiceNeed(
            NewServiceNeedController.NewServiceNeedCreateRequest(
                placementId = placementId,
                startDate = testDate(5),
                endDate = testDate(25),
                optionId = snDaycareFullDay35.id,
                shiftCare = false
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(3, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(4) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(5) && it.endDate == testDate(25) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(26) && it.endDate == testDate(30) })
        }
    }

    @Test
    fun `delete service need`() {
        givenServiceNeed(1, 9, placementId)
        val idToDelete = givenServiceNeed(10, 19, placementId)
        givenServiceNeed(20, 30, placementId)

        deleteServiceNeed(idToDelete)

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(2, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(9) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(20) && it.endDate == testDate(30) })
        }
    }

    @Test
    fun `update service need`() {
        givenServiceNeed(1, 9, placementId)
        val idToUpdate = givenServiceNeed(10, 19, placementId)
        givenServiceNeed(20, 30, placementId)

        putServiceNeed(
            idToUpdate,
            NewServiceNeedController.NewServiceNeedUpdateRequest(
                startDate = testDate(5),
                endDate = testDate(25),
                optionId = snDaycareFullDay25to35.id,
                shiftCare = true
            )
        )

        getServiceNeeds(testChild_1.id, placementId).let { serviceNeeds ->
            assertEquals(3, serviceNeeds.size)
            assertTrue(serviceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(4) })
            assertTrue(serviceNeeds.any { it.startDate == testDate(5) && it.endDate == testDate(25) && it.shiftCare && it.option.id == snDaycareFullDay25to35.id })
            assertTrue(serviceNeeds.any { it.startDate == testDate(26) && it.endDate == testDate(30) })
        }
    }

    private fun testDate(day: Int) = LocalDate.now().plusDays(day.toLong())

    private fun givenServiceNeed(start: Int, end: Int, placementId: UUID, optionId: UUID = snDefaultDaycare.id): UUID {
        return db.transaction { tx ->
            tx.insertTestNewServiceNeed(
                confirmedBy = unitSupervisor.id,
                placementId = placementId,
                period = FiniteDateRange(testDate(start), testDate(end)),
                optionId = optionId
            )
        }
    }

    private fun postServiceNeed(request: NewServiceNeedController.NewServiceNeedCreateRequest, expectedStatus: Int = 204) {
        val (_, res, _) = http.post("/new-service-needs")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(unitSupervisor)
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun getServiceNeeds(childId: UUID, placementId: UUID): List<NewServiceNeed> {
        val (_, res, result) = http.get("/placements?childId=$childId")
            .asUser(unitSupervisor)
            .responseObject<List<DaycarePlacementWithDetails>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get().first { it.id == placementId }.serviceNeeds
    }

    private fun putServiceNeed(id: UUID, request: NewServiceNeedController.NewServiceNeedUpdateRequest, expectedStatus: Int = 204) {
        val (_, res, _) = http.put("/new-service-needs/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(unitSupervisor)
            .response()

        assertEquals(expectedStatus, res.statusCode)
    }

    private fun deleteServiceNeed(id: UUID) {
        val (_, res, _) = http.delete("/new-service-needs/$id")
            .asUser(unitSupervisor)
            .response()

        assertEquals(204, res.statusCode)
    }
}
