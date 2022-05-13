// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevAssistanceNeed
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestAssistanceNeed
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssistanceNeedIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val assistanceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val testDaycareId = testDaycare.id

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `post first assistance need, no bases`() {
        val assistanceNeed = whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5
            )
        )

        assertEquals(
            AssistanceNeed(
                id = assistanceNeed.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5,
                bases = emptySet(),
            ),
            assistanceNeed
        )
    }

    @Test
    fun `post first assistance need, with bases`() {
        val allBases = db.transaction { it.getAssistanceBasisOptions() }.map { it.value }.toSet()

        val assistanceNeed = whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5,
                bases = allBases,
            )
        )

        assertEquals(
            AssistanceNeed(
                id = assistanceNeed.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                capacityFactor = 1.5,
                bases = allBases,
            ),
            assistanceNeed
        )
    }

    @Test
    fun `post assistance need, no overlap`() {
        givenAssistanceNeed(testDate(1), testDate(15))
        whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(16),
                endDate = testDate(30),
                capacityFactor = 1.5
            )
        )

        val assistanceNeeds = db.read { it.getAssistanceNeedsByChild(testChild_1.id) }
        assertEquals(2, assistanceNeeds.size)
        assertTrue(assistanceNeeds.any { it.startDate == testDate(1) && it.endDate == testDate(15) })
        assertTrue(assistanceNeeds.any { it.startDate == testDate(16) && it.endDate == testDate(30) })
    }

    @Test
    fun `post assistance need, fully encloses previous - responds 409`() {
        givenAssistanceNeed(testDate(10), testDate(20))
        whenPostAssistanceNeedThenExpectError(
            AssistanceNeedRequest(
                startDate = testDate(1),
                endDate = testDate(30),
                capacityFactor = 1.5
            ),
            409
        )
    }

    @Test
    fun `post assistance need, starts on same day, ends later - responds 409`() {
        givenAssistanceNeed(testDate(10), testDate(20))
        whenPostAssistanceNeedThenExpectError(
            AssistanceNeedRequest(
                startDate = testDate(10),
                endDate = testDate(25),
                capacityFactor = 1.5
            ),
            409
        )
    }

    @Test
    fun `post assistance need, overlaps start of previous - responds 409`() {
        givenAssistanceNeed(testDate(10), testDate(20))
        whenPostAssistanceNeedThenExpectError(
            AssistanceNeedRequest(
                startDate = testDate(1),
                endDate = testDate(10),
                capacityFactor = 1.5
            ),
            409
        )
    }

    @Test
    fun `post assistance need, overlaps end of previous - previous gets shortened`() {
        givenAssistanceNeed(testDate(10), testDate(20))
        whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(20),
                endDate = testDate(30),
                capacityFactor = 1.5
            )
        )

        val assistanceNeeds = db.read { it.getAssistanceNeedsByChild(testChild_1.id) }
        assertEquals(2, assistanceNeeds.size)
        assertTrue(assistanceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(19) })
        assertTrue(assistanceNeeds.any { it.startDate == testDate(20) && it.endDate == testDate(30) })
    }

    @Test
    fun `post assistance need, is within previous - previous gets shortened`() {
        givenAssistanceNeed(testDate(10), testDate(20))
        whenPostAssistanceNeedThenExpectSuccess(
            AssistanceNeedRequest(
                startDate = testDate(11),
                endDate = testDate(15),
                capacityFactor = 1.5
            )
        )

        val assistanceNeeds = db.read { it.getAssistanceNeedsByChild(testChild_1.id) }
        assertEquals(2, assistanceNeeds.size)
        assertTrue(assistanceNeeds.any { it.startDate == testDate(10) && it.endDate == testDate(10) })
        assertTrue(assistanceNeeds.any { it.startDate == testDate(11) && it.endDate == testDate(15) })
    }

    @Test
    fun `get assistance needs`() {
        givenAssistanceNeed(testDate(1), testDate(5), testChild_1.id)
        givenAssistanceNeed(testDate(25), testDate(30), testChild_1.id)
        givenAssistanceNeed(testDate(25), testDate(30), testChild_2.id)

        val assistanceNeeds = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(2, assistanceNeeds.size)
        with(assistanceNeeds[0]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(25), startDate)
            assertEquals(testDate(30), endDate)
        }
        with(assistanceNeeds[1]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(1), startDate)
            assertEquals(testDate(5), endDate)
        }
    }

    @Test
    fun `update assistance need`() {
        val id1 = givenAssistanceNeed(testDate(1), testDate(5))
        val id2 = givenAssistanceNeed(testDate(10), testDate(20))
        val updated = whenPutAssistanceNeedThenExpectSuccess(
            id2,
            AssistanceNeedRequest(
                startDate = testDate(9),
                endDate = testDate(22),
                capacityFactor = 1.5
            )
        )

        assertEquals(id2, updated.id)
        assertEquals(testDate(9), updated.startDate)
        assertEquals(testDate(22), updated.endDate)
        assertEquals(1.5, updated.capacityFactor)

        val fetched = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(2, fetched.size)
        assertTrue(fetched.any { it.id == id1 && it.startDate == testDate(1) && it.endDate == testDate(5) })
        assertTrue(fetched.any { it.id == id2 && it.startDate == testDate(9) && it.endDate == testDate(22) })
    }

    @Test
    fun `update assistance need, not found responds 404`() {
        whenPutAssistanceNeedThenExpectError(
            AssistanceNeedId(UUID.randomUUID()),
            AssistanceNeedRequest(
                startDate = testDate(9),
                endDate = testDate(22),
                capacityFactor = 20.0
            ),
            404
        )
    }

    @Test
    fun `update assistance need, conflict returns 409`() {
        givenAssistanceNeed(testDate(1), testDate(5))
        val id2 = givenAssistanceNeed(testDate(10), testDate(20))

        whenPutAssistanceNeedThenExpectError(
            id2,
            AssistanceNeedRequest(
                startDate = testDate(5),
                endDate = testDate(22),
                capacityFactor = 20.0
            ),
            409
        )
    }

    @Test
    fun `delete assistance need`() {
        val id1 = givenAssistanceNeed(testDate(1), testDate(5))
        val id2 = givenAssistanceNeed(testDate(10), testDate(20))

        whenDeleteAssistanceNeedThenExpectSuccess(id2)

        val assistanceNeeds = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceNeeds.size)
        assertEquals(id1, assistanceNeeds.first().id)
    }

    @Test
    fun `delete assistance need, not found responds 404`() {
        whenDeleteAssistanceNeedThenExpectError(AssistanceNeedId(UUID.randomUUID()), 404)
    }

    @Test
    fun `if child is in preschool, only show preschool assistance needs if employee is not an admin`() {
        val today = LocalDate.now()
        // Assistance need in the past
        givenAssistanceNeed(today.minusDays(1), today.minusDays(1), testChild_1.id)
        val assistanceNeedNow = givenAssistanceNeed(today, today, testChild_1.id)

        // No preschool placement, so expect both
        assertEquals(2, whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id).size)

        // With a non preschool placement expect seeing both
        val placement = givenPlacement(today, today, PlacementType.DAYCARE)
        assertEquals(2, whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id).size)
        db.transaction {
            it.createUpdate("delete from placement where id = :id")
                .bind("id", placement.raw)
                .execute()
        }

        // With a preschool placement, expect only seeing the assistance need starting after the placement
        givenPlacement(today, today, PlacementType.PRESCHOOL)
        val assistanceNeeds = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceNeeds.size)

        with(assistanceNeeds[0]) {
            assertEquals(assistanceNeedNow, id)
        }

        // Admin sees all
        assertEquals(2, whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id, admin).size)
    }

    @Test
    fun `if child will be in preschool in the future, show pre preschool assistance needs if employee is not an admin`() {
        val today = LocalDate.now()
        givenAssistanceNeed(today, today, testChild_1.id)

        // With a preschool placement in the future, expect seeing current assistance need
        givenPlacement(today.plusDays(1), today.plusDays(1), PlacementType.PRESCHOOL)
        val assistanceNeeds = whenGetAssistanceNeedsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceNeeds.size)
    }

    private fun testDate(day: Int) = LocalDate.now().withMonth(1).withDayOfMonth(day)

    private fun givenAssistanceNeed(startDate: LocalDate, endDate: LocalDate, childId: ChildId = testChild_1.id): AssistanceNeedId {
        return db.transaction {
            it.insertTestAssistanceNeed(
                DevAssistanceNeed(
                    childId = childId,
                    startDate = startDate,
                    endDate = endDate,
                    updatedBy = assistanceWorker.evakaUserId
                )
            )
        }
    }

    private fun givenPlacement(startDate: LocalDate, endDate: LocalDate, type: PlacementType, childId: ChildId = testChild_1.id): PlacementId {
        return db.transaction {
            it.insertTestPlacement(
                DevPlacement(
                    childId = childId,
                    startDate = startDate,
                    endDate = endDate,
                    type = type,
                    unitId = testDaycareId
                )
            )
        }
    }

    private fun whenPostAssistanceNeedThenExpectSuccess(request: AssistanceNeedRequest): AssistanceNeed {
        val (_, res, result) = http.post("/children/${testChild_1.id}/assistance-needs")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceNeed>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPostAssistanceNeedThenExpectError(request: AssistanceNeedRequest, status: Int) {
        val (_, res, _) = http.post("/children/${testChild_1.id}/assistance-needs")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenGetAssistanceNeedsThenExpectSuccess(childId: ChildId, asUser: AuthenticatedUser = assistanceWorker): List<AssistanceNeed> {
        val (_, res, result) = http.get("/children/$childId/assistance-needs")
            .asUser(asUser)
            .responseObject<List<AssistanceNeedResponse>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get().map { it.need }
    }

    private fun whenPutAssistanceNeedThenExpectSuccess(id: AssistanceNeedId, request: AssistanceNeedRequest): AssistanceNeed {
        val (_, res, result) = http.put("/assistance-needs/$id")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceNeed>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceNeedThenExpectError(id: AssistanceNeedId, request: AssistanceNeedRequest, status: Int) {
        val (_, res, _) = http.put("/assistance-needs/$id")
            .jsonBody(jsonMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenDeleteAssistanceNeedThenExpectSuccess(id: AssistanceNeedId) {
        val (_, res, _) = http.delete("/assistance-needs/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(200, res.statusCode)
    }

    private fun whenDeleteAssistanceNeedThenExpectError(id: AssistanceNeedId, status: Int) {
        val (_, res, _) = http.delete("/assistance-needs/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }
}
