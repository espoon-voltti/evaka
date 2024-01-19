// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.assistance.AssistanceController
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class AssistanceActionIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired lateinit var controller: AssistanceController

    private lateinit var daycare: DaycareId
    private lateinit var daycare2: DaycareId
    private lateinit var decisionMaker: EmployeeId
    private lateinit var decisionMaker2: EmployeeId
    private lateinit var child: ChildId
    private lateinit var child2: ChildId
    private lateinit var assistanceWorker: AuthenticatedUser.Employee
    private lateinit var admin: AuthenticatedUser.Employee

    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            val area = tx.insert(DevCareArea())
            val area2 = tx.insert(DevCareArea(name = "Test Area 2", shortName = "test_area2"))
            decisionMaker = tx.insert(DevEmployee())
            decisionMaker2 = tx.insert(DevEmployee())
            daycare = tx.insert(DevDaycare(areaId = area))
            daycare2 = tx.insert(DevDaycare(areaId = area2))
            child = tx.insert(DevPerson(), DevPersonType.CHILD)
            child2 = tx.insert(DevPerson(), DevPersonType.CHILD)
            assistanceWorker =
                AuthenticatedUser.Employee(decisionMaker, setOf(UserRole.SERVICE_WORKER))
            admin = AuthenticatedUser.Employee(decisionMaker, setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun `post first assistance action, no action types`() {
        val assistanceAction =
            createAssistanceAction(
                AssistanceActionRequest(startDate = testDate(10), endDate = testDate(20))
            )

        assertEquals(
            AssistanceAction(
                id = assistanceAction.id,
                childId = child,
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
            createAssistanceAction(
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
                childId = child,
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
        createAssistanceAction(
            AssistanceActionRequest(startDate = testDate(16), endDate = testDate(30))
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(child) }
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
        assertThrows<Conflict> {
            createAssistanceAction(
                AssistanceActionRequest(startDate = testDate(1), endDate = testDate(30))
            )
        }
    }

    @Test
    fun `post assistance action, starts on same day, ends later - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        assertThrows<Conflict> {
            createAssistanceAction(
                AssistanceActionRequest(startDate = testDate(10), endDate = testDate(25)),
            )
        }
    }

    @Test
    fun `post assistance action, overlaps start of previous - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        assertThrows<Conflict> {
            createAssistanceAction(
                AssistanceActionRequest(startDate = testDate(1), endDate = testDate(10)),
            )
        }
    }

    @Test
    fun `post assistance action, overlaps end of previous - previous gets shortened`() {
        givenAssistanceAction(testDate(10), testDate(20))
        createAssistanceAction(
            AssistanceActionRequest(startDate = testDate(20), endDate = testDate(30))
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(child) }
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
        createAssistanceAction(
            AssistanceActionRequest(startDate = testDate(11), endDate = testDate(15))
        )

        val assistanceActions = db.read { it.getAssistanceActionsByChild(child) }
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
                decisionMaker,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                daycare,
                decisionMaker,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val placementStartDate = clock.today()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)

        givenAssistanceAction(placementStartDate, placementStartDate.plusDays(5), child)
        givenAssistanceAction(
            placementStartDate.plusDays(25),
            placementStartDate.plusDays(30),
            child
        )
        givenAssistanceAction(
            placementStartDate.plusDays(25),
            placementStartDate.plusDays(30),
            child2
        )

        val assistanceActions = getAssistanceActions(child, veoInPlacementUnit)
        assertEquals(2, assistanceActions.size)
        with(assistanceActions[0]) {
            assertEquals(child, childId)
            assertEquals(placementStartDate.plusDays(25), startDate)
            assertEquals(placementStartDate.plusDays(30), endDate)
        }
        with(assistanceActions[1]) {
            assertEquals(child, childId)
            assertEquals(placementStartDate, startDate)
            assertEquals(placementStartDate.plusDays(5), endDate)
        }
    }

    @Test
    fun `update assistance action`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                decisionMaker,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                daycare,
                decisionMaker,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val placementStartDate = clock.today()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)

        val id1 = givenAssistanceAction(placementStartDate, placementStartDate.plusDays(5))
        val id2 =
            givenAssistanceAction(placementStartDate.plusDays(10), placementEndDate.plusDays(20))
        val updated =
            updateAssistanceAction(
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

        val fetched = getAssistanceActions(child, veoInPlacementUnit)
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
        assertThrows<NotFound> {
            updateAssistanceAction(
                AssistanceActionId(UUID.randomUUID()),
                AssistanceActionRequest(startDate = testDate(9), endDate = testDate(22)),
            )
        }
    }

    @Test
    fun `update assistance action, conflict returns 409`() {
        givenAssistanceAction(testDate(1), testDate(5))
        val id2 = givenAssistanceAction(testDate(10), testDate(20))

        assertThrows<Conflict> {
            updateAssistanceAction(
                id2,
                AssistanceActionRequest(startDate = testDate(5), endDate = testDate(22)),
            )
        }
    }

    @Test
    fun `delete assistance action`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                decisionMaker,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                daycare,
                decisionMaker,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val placementStartDate = clock.today()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)
        val id1 = givenAssistanceAction(placementStartDate, placementStartDate.plusMonths(1))
        val id2 = givenAssistanceAction(placementStartDate.plusMonths(2), placementEndDate)

        deleteAssistanceAction(id2)

        val assistanceActions = getAssistanceActions(child, veoInPlacementUnit)
        assertEquals(1, assistanceActions.size)
        assertEquals(id1, assistanceActions.first().id)
    }

    @Test
    fun `delete assistance action, not found responds 404`() {
        assertThrows<NotFound> { deleteAssistanceAction(AssistanceActionId(UUID.randomUUID())) }
    }

    @Test
    fun `if child is in preschool, show VEO all assistance info`() {
        val veoEmployee =
            AuthenticatedUser.Employee(
                decisionMaker,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                daycare,
                decisionMaker,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }

        val today = clock.today()
        val daycarePlacementStart = today.minusYears(1)
        val daycarePlacementEnd = today.minusDays(1)
        val preschoolPeriodFirstDate = daycarePlacementEnd.plusDays(1)

        givenPlacement(daycarePlacementStart, daycarePlacementEnd, PlacementType.DAYCARE)
        givenPlacement(
            preschoolPeriodFirstDate,
            preschoolPeriodFirstDate,
            PlacementType.PRESCHOOL_DAYCARE
        )
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
                child
            )

        // Overlaps first preschool placement
        val assistanceOverlappingPreschool =
            givenAssistanceAction(daycarePlacementEnd, preschoolPeriodFirstDate, child)

        // Completely after any daycare placement
        val assistanceDuringPreschool =
            givenAssistanceAction(
                preschoolPeriodFirstDate.plusDays(1),
                preschoolPeriodFirstDate.plusYears(1),
                child
            )

        val assistanceActions = getAssistanceActions(child, veoEmployee)
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
        assertEquals(3, getAssistanceActions(child, admin).size)
    }

    @Test
    fun `before preschool show assistance action only to employees in same unit as child`() {
        val sameUnitEmployee =
            AuthenticatedUser.Employee(decisionMaker, setOf(UserRole.STAFF))
        val differentUnitEmployee =
            AuthenticatedUser.Employee(decisionMaker2, setOf(UserRole.STAFF))
        db.transaction {
            it.insertDaycareAclRow(daycare, decisionMaker, UserRole.STAFF)
            it.insertDaycareAclRow(daycare2, decisionMaker2, UserRole.STAFF)
        }

        val today = clock.today()
        givenAssistanceAction(today, today, child)

        givenPlacement(today, today, PlacementType.DAYCARE)
        val sameUnitAssistanceActions = getAssistanceActions(child, sameUnitEmployee)
        assertEquals(1, sameUnitAssistanceActions.size)

        assertThrows<Forbidden> { getAssistanceActions(child, differentUnitEmployee) }
    }

    @Test
    fun `if child will be in preschool, show pre preschool assistance actions`() {
        val veoInPlacementUnit =
            AuthenticatedUser.Employee(
                decisionMaker,
                setOf(UserRole.SPECIAL_EDUCATION_TEACHER)
            )
        db.transaction {
            it.insertDaycareAclRow(
                daycare,
                decisionMaker,
                UserRole.SPECIAL_EDUCATION_TEACHER
            )
        }
        val today = clock.today()
        givenPlacement(today, today, PlacementType.DAYCARE)

        givenAssistanceAction(today, today, child)

        givenPlacement(today.plusDays(1), today.plusDays(1), PlacementType.PRESCHOOL)
        val assistanceActions = getAssistanceActions(child, veoInPlacementUnit)
        assertEquals(1, assistanceActions.size)
    }

    private fun testDate(day: Int) = clock.today().withMonth(1).withDayOfMonth(day)

    private fun givenAssistanceAction(
        startDate: LocalDate,
        endDate: LocalDate,
        childId: ChildId = child
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
        childId: ChildId = child
    ): PlacementId {
        return db.transaction {
            it.insert(
                DevPlacement(
                    childId = childId,
                    startDate = startDate,
                    endDate = endDate,
                    type = type,
                    unitId = daycare
                )
            )
        }
    }

    private fun createAssistanceAction(request: AssistanceActionRequest): AssistanceAction =
        controller.createAssistanceAction(
            dbInstance(),
            assistanceWorker,
            clock,
            child,
            request
        )

    private fun getAssistanceActions(child: ChildId, user: AuthenticatedUser = assistanceWorker) =
        controller.getChildAssistance(dbInstance(), user, clock, child).assistanceActions.map {
            it.action
        }

    private fun updateAssistanceAction(
        id: AssistanceActionId,
        request: AssistanceActionRequest,
        employee: AuthenticatedUser.Employee = assistanceWorker
    ): AssistanceAction =
        controller.updateAssistanceAction(dbInstance(), employee, clock, id, request)

    private fun deleteAssistanceAction(id: AssistanceActionId) =
        controller.deleteAssistanceAction(dbInstance(), assistanceWorker, clock, id)
}
