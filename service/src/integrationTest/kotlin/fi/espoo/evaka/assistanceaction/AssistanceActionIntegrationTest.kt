// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistance.AssistanceController
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testDecisionMaker_2
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AssistanceActionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val assistanceWorker =
        AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.SERVICE_WORKER))
    private val admin = AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.ADMIN))
    private val testDaycareId = testDaycare.id

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `post first assistance action, no action types`() {
        val assistanceAction =
            whenPostAssistanceActionThenExpectSuccess(
                AssistanceActionRequest(startDate = testDate(10), endDate = testDate(20))
            )

        assertEquals(
            AssistanceAction(
                id = assistanceAction.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                actions = emptySet(),
                otherAction = ""
            ),
            assistanceAction
        )
    }

    @Test
    fun `post first assistance action, with action types`() {
        val allActionTypes =
            db.transaction { it.getAssistanceActionOptions() }.map { it.value }.toSet()

        val assistanceAction =
            whenPostAssistanceActionThenExpectSuccess(
                AssistanceActionRequest(
                    startDate = testDate(10),
                    endDate = testDate(20),
                    actions = allActionTypes,
                    otherAction = "foo",
                )
            )

        assertEquals(
            AssistanceAction(
                id = assistanceAction.id,
                childId = testChild_1.id,
                startDate = testDate(10),
                endDate = testDate(20),
                actions = allActionTypes,
                otherAction = "foo"
            ),
            assistanceAction
        )
    }

    @Test
    fun `post assistance action, no overlap`() {
        givenAssistanceAction(testDate(1), testDate(15))
        whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(startDate = testDate(16), endDate = testDate(30))
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(testChild_1.id) }
        assertEquals(2, assistanceActions.size)
        assertTrue(
            assistanceActions.any { it.startDate == testDate(1) && it.endDate == testDate(15) }
        )
        assertTrue(
            assistanceActions.any { it.startDate == testDate(16) && it.endDate == testDate(30) }
        )
    }

    @Test
    fun `post assistance action, fully encloses previous - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectError(
            AssistanceActionRequest(startDate = testDate(1), endDate = testDate(30)),
            409
        )
    }

    @Test
    fun `post assistance action, starts on same day, ends later - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectError(
            AssistanceActionRequest(startDate = testDate(10), endDate = testDate(25)),
            409
        )
    }

    @Test
    fun `post assistance action, overlaps start of previous - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectError(
            AssistanceActionRequest(startDate = testDate(1), endDate = testDate(10)),
            409
        )
    }

    @Test
    fun `post assistance action, overlaps end of previous - previous gets shortened`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(startDate = testDate(20), endDate = testDate(30))
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(testChild_1.id) }
        assertEquals(2, assistanceActions.size)
        assertTrue(
            assistanceActions.any { it.startDate == testDate(10) && it.endDate == testDate(19) }
        )
        assertTrue(
            assistanceActions.any { it.startDate == testDate(20) && it.endDate == testDate(30) }
        )
    }

    @Test
    fun `post assistance action, is within previous - previous gets shortened`() {
        givenAssistanceAction(testDate(10), testDate(20))
        whenPostAssistanceActionThenExpectSuccess(
            AssistanceActionRequest(startDate = testDate(11), endDate = testDate(15))
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(testChild_1.id) }
        assertEquals(2, assistanceActions.size)
        assertTrue(
            assistanceActions.any { it.startDate == testDate(10) && it.endDate == testDate(10) }
        )
        assertTrue(
            assistanceActions.any { it.startDate == testDate(11) && it.endDate == testDate(15) }
        )
    }

    @Test
    fun `get assistance actions`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                testDecisionMaker_1.id,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                testDaycareId,
                testDecisionMaker_1.id,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val placementStartDate = LocalDate.now()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)

        givenAssistanceAction(placementStartDate, placementStartDate.plusDays(5), testChild_1.id)
        givenAssistanceAction(
            placementStartDate.plusDays(25),
            placementStartDate.plusDays(30),
            testChild_1.id
        )
        givenAssistanceAction(
            placementStartDate.plusDays(25),
            placementStartDate.plusDays(30),
            testChild_2.id
        )

        val assistanceActions =
            whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, veoInPlacementUnit)
        assertEquals(2, assistanceActions.size)
        with(assistanceActions[0]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(placementStartDate.plusDays(25), startDate)
            assertEquals(placementStartDate.plusDays(30), endDate)
        }
        with(assistanceActions[1]) {
            assertEquals(testChild_1.id, childId)
            assertEquals(placementStartDate, startDate)
            assertEquals(placementStartDate.plusDays(5), endDate)
        }
    }

    @Test
    fun `update assistance action`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                testDecisionMaker_1.id,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                testDaycareId,
                testDecisionMaker_1.id,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val placementStartDate = LocalDate.now()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)

        val id1 = givenAssistanceAction(placementStartDate, placementStartDate.plusDays(5))
        val id2 =
            givenAssistanceAction(placementStartDate.plusDays(10), placementEndDate.plusDays(20))
        val updated =
            whenPutAssistanceActionThenExpectSuccess(
                id2,
                AssistanceActionRequest(
                    startDate = placementStartDate.plusDays(9),
                    endDate = placementStartDate.plusDays(22)
                ),
                veoInPlacementUnit
            )

        assertEquals(id2, updated.id)
        assertEquals(placementStartDate.plusDays(9), updated.startDate)
        assertEquals(placementStartDate.plusDays(22), updated.endDate)

        val fetched = whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, veoInPlacementUnit)
        assertEquals(2, fetched.size)
        assertTrue(
            fetched.any {
                it.id == id1 &&
                    it.startDate == placementStartDate &&
                    it.endDate == placementStartDate.plusDays(5)
            }
        )
        assertTrue(
            fetched.any {
                it.id == id2 &&
                    it.startDate == placementStartDate.plusDays(9) &&
                    it.endDate == placementStartDate.plusDays(22)
            }
        )
    }

    @Test
    fun `update assistance action, not found responds 404`() {
        whenPutAssistanceActionThenExpectError(
            AssistanceActionId(UUID.randomUUID()),
            AssistanceActionRequest(startDate = testDate(9), endDate = testDate(22)),
            404
        )
    }

    @Test
    fun `update assistance action, conflict returns 409`() {
        givenAssistanceAction(testDate(1), testDate(5))
        val id2 = givenAssistanceAction(testDate(10), testDate(20))

        whenPutAssistanceActionThenExpectError(
            id2,
            AssistanceActionRequest(startDate = testDate(5), endDate = testDate(22)),
            409
        )
    }

    @Test
    fun `delete assistance action`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                testDecisionMaker_1.id,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                testDaycareId,
                testDecisionMaker_1.id,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val placementStartDate = LocalDate.now()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)
        val id1 = givenAssistanceAction(placementStartDate, placementStartDate.plusMonths(1))
        val id2 = givenAssistanceAction(placementStartDate.plusMonths(2), placementEndDate)

        whenDeleteAssistanceActionThenExpectSuccess(id2)

        val assistanceActions =
            whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, veoInPlacementUnit)
        assertEquals(1, assistanceActions.size)
        assertEquals(id1, assistanceActions.first().id)
    }

    @Test
    fun `delete assistance action, not found responds 404`() {
        whenDeleteAssistanceActionThenExpectError(AssistanceActionId(UUID.randomUUID()), 404)
    }

    @Test
    fun `if child is in preschool, show VEO all assistance info`() {
        val veoEmployee =
            AuthenticatedUser.Employee(
                testDecisionMaker_1.id,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                testDaycareId,
                testDecisionMaker_1.id,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }

        val today = LocalDate.now()
        val daycarePlacementStart = today.minusYears(1)
        val daycarePlacementEnd = today.minusDays(1)
        val preschoolPeriodFirstDate = daycarePlacementEnd.plusDays(1)

        val daycarePlacement =
            givenPlacement(daycarePlacementStart, daycarePlacementEnd, PlacementType.DAYCARE)
        val preschoolPlacementFirst =
            givenPlacement(
                preschoolPeriodFirstDate,
                preschoolPeriodFirstDate,
                PlacementType.PRESCHOOL_DAYCARE
            )
        val preschoolPlacementSecond =
            givenPlacement(
                preschoolPeriodFirstDate.plusDays(1),
                preschoolPeriodFirstDate.plusYears(1),
                PlacementType.PRESCHOOL
            )

        // Completely before any preschool placement
        val assistanceBeforePreschool =
            givenAssistanceAction(
                daycarePlacementStart,
                daycarePlacementEnd.minusDays(1),
                testChild_1.id
            )

        // Overlaps first preschool placement
        val assistanceOverlappingPreschool =
            givenAssistanceAction(daycarePlacementEnd, preschoolPeriodFirstDate, testChild_1.id)

        // Completely after any daycare placement
        val assistanceDuringPreschool =
            givenAssistanceAction(
                preschoolPeriodFirstDate.plusDays(1),
                preschoolPeriodFirstDate.plusYears(1),
                testChild_1.id
            )

        val assistanceActions =
            whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, veoEmployee)
        assertEquals(3, assistanceActions.size)

        assertEquals(
            setOf(
                assistanceBeforePreschool,
                assistanceOverlappingPreschool,
                assistanceDuringPreschool
            ),
            assistanceActions.map { it.id }.toSet()
        )

        // Admin sees all
        assertEquals(3, whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, admin).size)
    }

    @Test
    fun `before preschool show assistance action only to employees in same unit as child`() {
        val sameUnitEmployee =
            AuthenticatedUser.Employee(testDecisionMaker_1.id, setOf(UserRole.STAFF))
        val differentUnitEmployee =
            AuthenticatedUser.Employee(testDecisionMaker_2.id, setOf(UserRole.STAFF))
        db.transaction {
            it.insertDaycareAclRow(testDaycareId, testDecisionMaker_1.id, UserRole.STAFF)
            it.insertDaycareAclRow(testDaycare2.id, testDecisionMaker_2.id, UserRole.STAFF)
        }

        val today = LocalDate.now()
        givenAssistanceAction(today, today, testChild_1.id)

        givenPlacement(today, today, PlacementType.DAYCARE)
        val sameUnitAssistanceActions =
            whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, sameUnitEmployee)
        assertEquals(1, sameUnitAssistanceActions.size)

        whenGetAssistanceActionsThenExpect403(testChild_1.id, differentUnitEmployee)
    }

    @Test
    fun `if child will be in preschool, show pre preschool assistance actions`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                testDecisionMaker_1.id,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                testDaycareId,
                testDecisionMaker_1.id,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val today = LocalDate.now()
        givenPlacement(today, today, PlacementType.DAYCARE)

        givenAssistanceAction(today, today, testChild_1.id)

        givenPlacement(today.plusDays(1), today.plusDays(1), PlacementType.PRESCHOOL)
        val assistanceActions =
            whenGetAssistanceActionsThenExpectSuccess(testChild_1.id, veoInPlacementUnit)
        assertEquals(1, assistanceActions.size)
    }

    private fun testDate(day: Int) = LocalDate.now().withMonth(1).withDayOfMonth(day)

    private fun givenAssistanceAction(
        startDate: LocalDate,
        endDate: LocalDate,
        childId: ChildId = testChild_1.id
    ): AssistanceActionId {
        return db.transaction {
            it.insert(
                DevAssistanceAction(
                    childId = childId,
                    startDate = startDate,
                    endDate = endDate,
                    updatedBy = assistanceWorker.evakaUserId
                )
            )
        }
    }

    private fun givenPlacement(
        startDate: LocalDate,
        endDate: LocalDate,
        type: PlacementType,
        childId: ChildId = testChild_1.id
    ): PlacementId {
        return db.transaction {
            it.insert(
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

    private fun whenPostAssistanceActionThenExpectSuccess(
        request: AssistanceActionRequest
    ): AssistanceAction {
        val (_, res, result) =
            http
                .post("/children/${testChild_1.id}/assistance-actions")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(assistanceWorker)
                .responseObject<AssistanceAction>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPostAssistanceActionThenExpectError(
        request: AssistanceActionRequest,
        status: Int
    ) {
        val (_, res, _) =
            http
                .post("/children/${testChild_1.id}/assistance-actions")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(assistanceWorker)
                .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenGetAssistanceActionsThenExpectSuccess(
        childId: ChildId,
        user: AuthenticatedUser = assistanceWorker
    ): List<AssistanceAction> {
        val (_, res, result) =
            http
                .get("/children/$childId/assistance")
                .asUser(user)
                .responseObject<AssistanceController.AssistanceResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get().assistanceActions.map { it.action }
    }

    private fun whenGetAssistanceActionsThenExpect403(
        childId: ChildId,
        user: AuthenticatedUser = assistanceWorker
    ) {
        val (_, res, result) =
            http
                .get("/children/$childId/assistance")
                .asUser(user)
                .responseObject<AssistanceController.AssistanceResponse>(jsonMapper)

        assertEquals(403, res.statusCode)
    }

    private fun whenPutAssistanceActionThenExpectSuccess(
        id: AssistanceActionId,
        request: AssistanceActionRequest,
        employee: AuthenticatedUser.Employee = assistanceWorker
    ): AssistanceAction {
        val (_, res, result) =
            http
                .put("/assistance-actions/$id")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(employee)
                .responseObject<AssistanceAction>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun whenPutAssistanceActionThenExpectError(
        id: AssistanceActionId,
        request: AssistanceActionRequest,
        status: Int
    ) {
        val (_, res, _) =
            http
                .put("/assistance-actions/$id")
                .jsonBody(jsonMapper.writeValueAsString(request))
                .asUser(assistanceWorker)
                .response()

        assertEquals(status, res.statusCode)
    }

    private fun whenDeleteAssistanceActionThenExpectSuccess(id: AssistanceActionId) {
        val (_, res, _) = http.delete("/assistance-actions/$id").asUser(assistanceWorker).response()

        assertEquals(200, res.statusCode)
    }

    private fun whenDeleteAssistanceActionThenExpectError(id: AssistanceActionId, status: Int) {
        val (_, res, _) = http.delete("/assistance-actions/$id").asUser(assistanceWorker).response()

        assertEquals(status, res.statusCode)
    }
}
