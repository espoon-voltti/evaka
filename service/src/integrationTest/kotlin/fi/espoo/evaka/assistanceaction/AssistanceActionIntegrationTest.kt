// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.insertTestAssistanceAction
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class AssistanceActionIntegrationTest : FullApplicationTest() {
    private val assistanceWorker = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))

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
        val allActionTypes = setOf(
            AssistanceActionType.ASSISTANCE_SERVICE_CHILD,
            AssistanceActionType.ASSISTANCE_SERVICE_UNIT,
            AssistanceActionType.SMALLER_GROUP,
            AssistanceActionType.SPECIAL_GROUP,
            AssistanceActionType.PERVASIVE_VEO_SUPPORT,
            AssistanceActionType.RESOURCE_PERSON,
            AssistanceActionType.RATIO_DECREASE,
            AssistanceActionType.PERIODICAL_VEO_SUPPORT,
            AssistanceActionType.OTHER
        )
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
        givenAssistanceAction(1, 15)
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
        givenAssistanceAction(10, 20)
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
        givenAssistanceAction(10, 20)
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
        givenAssistanceAction(10, 20)
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
        givenAssistanceAction(10, 20)
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
        givenAssistanceAction(10, 20)
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
        givenAssistanceAction(1, 5, testChild_1.id)
        givenAssistanceAction(25, 30, testChild_1.id)
        givenAssistanceAction(25, 30, testChild_2.id)

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
        val id1 = givenAssistanceAction(1, 5)
        val id2 = givenAssistanceAction(10, 20)
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
            UUID.randomUUID(),
            AssistanceActionRequest(
                startDate = testDate(9),
                endDate = testDate(22)
            ),
            404
        )
    }

    @Test
    fun `update assistance action, conflict returns 409`() {
        givenAssistanceAction(1, 5)
        val id2 = givenAssistanceAction(10, 20)

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
        val id1 = givenAssistanceAction(1, 5)
        val id2 = givenAssistanceAction(10, 20)

        whenDeleteAssistanceActionThenExpectSuccess(id2)

        val assistanceActions = whenGetAssistanceActionsThenExpectSuccess(testChild_1.id)
        assertEquals(1, assistanceActions.size)
        assertEquals(id1, assistanceActions.first().id)
    }

    @Test
    fun `delete assistance action, not found responds 404`() {
        whenDeleteAssistanceActionThenExpectError(UUID.randomUUID(), 404)
    }

    private fun testDate(day: Int) = LocalDate.of(2000, 1, day)

    private fun givenAssistanceAction(start: Int, end: Int, childId: UUID = testChild_1.id): UUID {
        return db.transaction {
            it.insertTestAssistanceAction(
                DevAssistanceAction(
                    childId = childId,
                    startDate = testDate(start),
                    endDate = testDate(end),
                    updatedBy = assistanceWorker.id
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

    private fun whenGetAssistanceActionsThenExpectSuccess(childId: UUID): List<AssistanceAction> {
        val (_, res, result) = http.get("/children/$childId/assistance-actions")
            .asUser(assistanceWorker)
            .responseObject<List<AssistanceAction>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceActionThenExpectSuccess(id: UUID, request: AssistanceActionRequest): AssistanceAction {
        val (_, res, result) = http.put("/assistance-actions/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .responseObject<AssistanceAction>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceActionThenExpectError(id: UUID, request: AssistanceActionRequest, status: Int) {
        val (_, res, _) = http.put("/assistance-actions/$id")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenDeleteAssistanceActionThenExpectSuccess(id: UUID) {
        val (_, res, _) = http.delete("/assistance-actions/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(204, res.statusCode)
    }

    private fun whenDeleteAssistanceActionThenExpectError(id: UUID, status: Int) {
        val (_, res, _) = http.delete("/assistance-actions/$id")
            .asUser(assistanceWorker)
            .response()

        assertEquals(status, res.statusCode)
    }
}
