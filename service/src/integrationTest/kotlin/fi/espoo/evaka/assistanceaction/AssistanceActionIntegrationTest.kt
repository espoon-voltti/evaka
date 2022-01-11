// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestAssistanceAction
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
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

class AssistanceActionIntegrationTest : FullApplicationTest() {
    private val assistanceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id.raw, setOf(UserRole.SERVICE_WORKER))
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id.raw, setOf(UserRole.ADMIN))
    private val testDaycareId = testDaycare.id

    @BeforeEach
    private fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `post first assistance action, no action types or measures`() {
        val assistanceAction = whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(
                startDate = testDate(10),
                endDate = testDate(20)
            )
        )

        assertEquals(
            AssistanceAction(
                id = assistanceAction.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                actions = emptySet(),
                otherAction = "",
                measures = emptySet()
            ),
            assistanceAction
        )
    }

    @Test
    fun `post first assistance action, with action types and measures`() {
        val allActionTypes = db.transaction { it.getAssistanceActionOptions() }.map { it.value }.toSet()
        val allMeasures = setOf(
            AssistanceMeasure.SPECIAL_ASSISTANCE_DECISION,
            AssistanceMeasure.INTENSIFIED_ASSISTANCE,
            AssistanceMeasure.EXTENDED_COMPULSORY_EDUCATION,
            AssistanceMeasure.CHILD_SERVICE,
            AssistanceMeasure.CHILD_ACCULTURATION_SUPPORT,
            AssistanceMeasure.TRANSPORT_BENEFIT
        )

        val assistanceAction = whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(
                startDate = testDate(10),
                endDate = testDate(20),
                actions = allActionTypes,
                otherAction = "foo",
                measures = allMeasures
            )
        )

        assertEquals(
            AssistanceAction(
                id = assistanceAction.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                actions = allActionTypes,
                otherAction = "foo",
                measures = allMeasures
            ),
            assistanceAction
        )
    }

    @Test
    fun `post assistance action, no overlap`() {
        givenAssistanceAction(testDate(1), testDate(15))
        whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(
                startDate = testDate(16),
                endDate = testDate(30)
            )
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(testChild_1.id) }
        assertEquals(2, assistanceActions.size)
        assertTrue(assistanceActions.any { it.startDate == testDate(1) && it.endDate == testDate(15) })
        assertTrue(assistanceActions.any { it.startDate == testDate(16) && it.endDate == testDate(30) })
    }

    @Test
    fun `post assistance action, fully encloses previous - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectError(
            AssistanceActionRequest(
                startDate = testDate(1),
                endDate = testDate(30)
            ),
            409
        )
    }

    @Test
    fun `post assistance action, starts on same day, ends later - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectError(
            AssistanceActionRequest(
                startDate = testDate(10),
                endDate = testDate(25)
            ),
            409
        )
    }

    @Test
    fun `post assistance action, overlaps start of previous - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectError(
            AssistanceActionRequest(
                startDate = testDate(1),
                endDate = testDate(10)
            ),
            409
        )
    }

    @Test
    fun `post assistance action, overlaps end of previous - previous gets shortened`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(
                startDate = testDate(20),
                endDate = testDate(30)
            )
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(testChild_1.id) }
        assertEquals(2, assistanceActions.size)
        assertTrue(assistanceActions.any { it.startDate == testDate(10) && it.endDate == testDate(19) })
        assertTrue(assistanceActions.any { it.startDate == testDate(20) && it.endDate == testDate(30) })
    }

    @Test
    fun `post assistance action, is within previous - previous gets shortened`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(
                startDate = testDate(11),
                endDate = testDate(15)
            )
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(testChild_1.id) }
        assertEquals(2, assistanceActions.size)
        assertTrue(assistanceActions.any { it.startDate == testDate(10) && it.endDate == testDate(10) })
        assertTrue(assistanceActions.any { it.startDate == testDate(11) && it.endDate == testDate(15) })
    }

    @Test
    fun `get assistance actions`() {
        givenAssistanceAction(testDate(1), testDate(5), testChild_1.id)
        givenAssistanceAction(testDate(25), testDate(30), testChild_1.id)
        givenAssistanceAction(testDate(25), testDate(30), testChild_2.id)

        val assistanceActions = whenGetAssistanceActionsThenExpectSuccess(testChild_1.id)
        assertEquals(2, assistanceActions.size)
        with(assistanceActions[0]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(25), startDate)
            assertEquals(testDate(30), endDate)
        }
        with(assistanceActions[1]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(testDate(1), startDate)
            assertEquals(testDate(5), endDate)
        }
    }

    @Test
    fun `update assistance action`() {
        val id1 = givenAssistanceAction(testDate(1), testDate(5))
        val id2 = givenAssistanceAction(testDate(10), testDate(20))
        val updated = whenPutAssistanceActionThenExpectSuccess(
            id2,
            AssistanceActionRequest(
                startDate = testDate(9),
                endDate = testDate(22)
            )
        )

        assertEquals(id2, updated.id)
        assertEquals(testDate(9), updated.startDate)
        assertEquals(testDate(22), updated.endDate)

        val fetched = whenGetAssistanceActionsThenExpectSuccess(testChild_1.id)
        assertEquals(2, fetched.size)
        assertTrue(fetched.any { it.id == id1 && it.startDate == testDate(1) && it.endDate == testDate(5) })
        assertTrue(fetched.any { it.id == id2 && it.startDate == testDate(9) && it.endDate == testDate(22) })
    }

    @Test
    fun `update assistance action, not found responds 404`() {
        whenPutAssistanceActionThenExpectError(
            AssistanceActionId(UUID.randomUUID()),
            AssistanceActionRequest(
                startDate = testDate(9),
                endDate = testDate(22)
            ),
            404
        )
    }

    @Test
    fun `update assistance action, conflict returns 409`() {
        givenAssistanceAction(testDate(1), testDate(5))
        val id2 = givenAssistanceAction(testDate(10), testDate(20))

        whenPutAssistanceActionThenExpectError(
            id2,
            AssistanceActionRequest(
                startDate = testDate(5),
                endDate = testDate(22)
            ),
            409
        )
    }

    @Test
    fun `delete assistance action`() {
        val id1 = givenAssistanceAction(testDate(1), testDate(5))
        val id2 = givenAssistanceAction(testDate(10), testDate(20))

        whenDeleteAssistanceActionThenExpectSuccess(id2)

        val assistanceActions = whenGetAssistanceActionsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceActions.size)
        assertEquals(id1, assistanceActions.first().id)
    }

    @Test
    fun `delete assistance action, not found responds 404`() {
        whenDeleteAssistanceActionThenExpectError(AssistanceActionId(UUID.randomUUID()), 404)
    }

    @Test
    fun `if child is in preschool, only show preschool assistance actions if employee is not an admin`() {
        val today = LocalDate.now().withMonth(8).withDayOfMonth(1)
        givenAssistanceAction(today.withMonth(1), today.withMonth(7), testChild_1.id)
        val assistanceActionDuringPreschool = givenAssistanceAction(today.withMonth(8), today.withMonth(12), testChild_1.id)

        // No preschool placement, so expect both
        assertEquals(2, whenGetAssistanceActionsThenExpectSuccess(testChild_1.id).size)

        // With a non preschool placement expect seeing both
        givenPlacement(today.withMonth(1), today.withMonth(7), PlacementType.DAYCARE)
        assertEquals(2, whenGetAssistanceActionsThenExpectSuccess(testChild_1.id).size)

        // With a preschool placement, expect only seeing the assistance need starting after the placement
        givenPlacement(today.withMonth(8), today.withMonth(12), PlacementType.PRESCHOOL)
        val assistanceActions = whenGetAssistanceActionsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceActions.size)

        with(assistanceActions[0]) {
            assertEquals(assistanceActionDuringPreschool, id)
        }

        // Admin sees all
        assertEquals(2, whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, admin).size)
    }

    private fun testDate(day: Int) = LocalDate.now().withMonth(1).withDayOfMonth(day)

    private fun givenAssistanceAction(startDate: LocalDate, endDate: LocalDate, childId: ChildId = testChild_1.id): AssistanceActionId {
        return db.transaction {
            it.insertTestAssistanceAction(
                DevAssistanceAction(
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

    private fun whenPostAssistanceActionThenExpectSuccess(request: AssistanceActionRequest): AssistanceAction {
        val (_, res, result) = http.post("/children/${testChild_1.id}/assistance-actions")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceAction>(objectMapper)

        assertEquals(201, res.statusCode)
        return result.get()
    }

    private fun whenPostAssistanceActionThenExpectError(request: AssistanceActionRequest, status: Int) {
        val (_, res, _) = http.post("/children/${testChild_1.id}/assistance-actions")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenGetAssistanceActionsThenExpectSuccess(childId: ChildId, user: AuthenticatedUser = assistanceWorker): List<AssistanceAction> {
        val (_, res, result) = http.get("/children/$childId/assistance-actions")
            .asUser(user)
            .responseObject<List<AssistanceAction>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceActionThenExpectSuccess(id: AssistanceActionId, request: AssistanceActionRequest): AssistanceAction {
        val (_, res, result) = http.put("/assistance-actions/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceAction>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceActionThenExpectError(id: AssistanceActionId, request: AssistanceActionRequest, status: Int) {
        val (_, res, _) = http.put("/assistance-actions/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenDeleteAssistanceActionThenExpectSuccess(id: AssistanceActionId) {
        val (_, res, _) = http.delete("/assistance-actions/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun whenDeleteAssistanceActionThenExpectError(id: AssistanceActionId, status: Int) {
        val (_, res, _) = http.delete("/assistance-actions/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }
}
