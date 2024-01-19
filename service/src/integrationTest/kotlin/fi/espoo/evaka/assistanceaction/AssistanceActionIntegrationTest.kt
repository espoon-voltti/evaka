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
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAssistanceAction
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestUser
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
    private lateinit var child: ChildId
    private lateinit var admin: AuthenticatedUser.Employee

    private val clock = MockEvakaClock(2023, 1, 1, 12, 0)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            val area = tx.insert(DevCareArea())
            daycare = tx.insert(DevDaycare(areaId = area))
            child = tx.insert(DevPerson(), DevPersonType.CHILD)
            admin = tx.insertTestUser(DevEmployee(roles = setOf(UserRole.ADMIN)))
        }
    }

    @Test
    fun `post first assistance action, no action types`() {
        val assistanceAction =
            createAssistanceAction(
                admin,
                child,
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
                admin,
                child,
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
            admin,
            child,
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
                admin,
                child,
                AssistanceActionRequest(startDate = testDate(1), endDate = testDate(30))
            )
        }
    }

    @Test
    fun `post assistance action, starts on same day, ends later - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        assertThrows<Conflict> {
            createAssistanceAction(
                admin,
                child,
                AssistanceActionRequest(startDate = testDate(10), endDate = testDate(25))
            )
        }
    }

    @Test
    fun `post assistance action, overlaps start of previous - responds 409`() {
        givenAssistanceAction(testDate(10), testDate(20))
        assertThrows<Conflict> {
            createAssistanceAction(
                admin,
                child,
                AssistanceActionRequest(startDate = testDate(1), endDate = testDate(10))
            )
        }
    }

    @Test
    fun `post assistance action, overlaps end of previous - previous gets shortened`() {
        givenAssistanceAction(testDate(10), testDate(20))
        createAssistanceAction(
            admin,
            child,
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
            admin,
            child,
            AssistanceActionRequest(startDate = testDate(11), endDate = testDate(15)),
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
            db.transaction {
                it.insertTestUser(
                    DevEmployee(),
                    unitRoles = mapOf(daycare to UserRole.SPECIAL_EDUCATION_TEACHER)
                )
            }
        val child2 = db.transaction { it.insert(DevPerson(), DevPersonType.CHILD) }
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

        val assistanceActions = getAssistanceActions(veoInPlacementUnit, child)
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
            db.transaction {
                it.insertTestUser(
                    DevEmployee(),
                    unitRoles = mapOf(daycare to UserRole.SPECIAL_EDUCATION_TEACHER)
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
                veoInPlacementUnit,
                id2,
                AssistanceActionRequest(
                    startDate = placementStartDate.plusDays(9),
                    endDate = placementStartDate.plusDays(22)
                )
            )

        assertEquals(id2, updated.id)
        assertEquals(placementStartDate.plusDays(9), updated.startDate)
        assertEquals(placementStartDate.plusDays(22), updated.endDate)

        val fetched = getAssistanceActions(veoInPlacementUnit, child)
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
                admin,
                AssistanceActionId(UUID.randomUUID()),
                AssistanceActionRequest(startDate = testDate(9), endDate = testDate(22))
            )
        }
    }

    @Test
    fun `update assistance action, conflict returns 409`() {
        givenAssistanceAction(testDate(1), testDate(5))
        val id2 = givenAssistanceAction(testDate(10), testDate(20))

        assertThrows<Conflict> {
            updateAssistanceAction(
                admin,
                id2,
                AssistanceActionRequest(startDate = testDate(5), endDate = testDate(22))
            )
        }
    }

    @Test
    fun `delete assistance action`() {
        val veoInPlacementUnit =
            db.transaction {
                it.insertTestUser(
                    DevEmployee(),
                    unitRoles = mapOf(daycare to UserRole.SPECIAL_EDUCATION_TEACHER)
                )
            }
        val placementStartDate = clock.today()
        val placementEndDate = placementStartDate.plusYears(1)
        givenPlacement(placementStartDate, placementEndDate, PlacementType.DAYCARE)
        val id1 = givenAssistanceAction(placementStartDate, placementStartDate.plusMonths(1))
        val id2 = givenAssistanceAction(placementStartDate.plusMonths(2), placementEndDate)

        deleteAssistanceAction(veoInPlacementUnit, id2)

        val assistanceActions = getAssistanceActions(veoInPlacementUnit, child)
        assertEquals(1, assistanceActions.size)
        assertEquals(id1, assistanceActions.first().id)
    }

    @Test
    fun `delete assistance action, not found responds 404`() {
        assertThrows<NotFound> {
            deleteAssistanceAction(admin, AssistanceActionId(UUID.randomUUID()))
        }
    }

    @Test
    fun `if child is in preschool, show VEO all assistance info`() {
        val veoEmployee =
            db.transaction {
                it.insertTestUser(
                    DevEmployee(),
                    unitRoles = mapOf(daycare to UserRole.SPECIAL_EDUCATION_TEACHER)
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
            givenAssistanceAction(daycarePlacementStart, daycarePlacementEnd.minusDays(1), child)

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

        val assistanceActions = getAssistanceActions(veoEmployee, child)
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
        assertEquals(3, getAssistanceActions(admin, child).size)
    }

    @Test
    fun `before preschool show assistance action only to employees in same unit as child`() {
        val (sameUnitEmployee, differentUnitEmployee) =
            db.transaction { tx ->
                val area2 = tx.insert(DevCareArea(name = "Test Area 2", shortName = "test_area2"))
                val daycare2 = tx.insert(DevDaycare(areaId = area2))

                Pair(
                    tx.insertTestUser(DevEmployee(), unitRoles = mapOf(daycare to UserRole.STAFF)),
                    tx.insertTestUser(DevEmployee(), unitRoles = mapOf(daycare2 to UserRole.STAFF))
                )
            }

        val today = clock.today()
        givenAssistanceAction(today, today, child)

        givenPlacement(today, today, PlacementType.DAYCARE)
        val sameUnitAssistanceActions = getAssistanceActions(sameUnitEmployee, child)
        assertEquals(1, sameUnitAssistanceActions.size)

        assertThrows<Forbidden> { getAssistanceActions(differentUnitEmployee, child) }
    }

    @Test
    fun `if child will be in preschool, show pre preschool assistance actions`() {
        val veoInPlacementUnit =
            db.transaction {
                it.insertTestUser(
                    DevEmployee(),
                    unitRoles = mapOf(daycare to UserRole.SPECIAL_EDUCATION_TEACHER)
                )
            }
        val today = clock.today()
        givenPlacement(today, today, PlacementType.DAYCARE)

        givenAssistanceAction(today, today, child)

        givenPlacement(today.plusDays(1), today.plusDays(1), PlacementType.PRESCHOOL)
        val assistanceActions = getAssistanceActions(veoInPlacementUnit, child)
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
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId
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

    private fun createAssistanceAction(
        user: AuthenticatedUser,
        child: ChildId,
        request: AssistanceActionRequest
    ): AssistanceAction =
        controller.createAssistanceAction(dbInstance(), user, clock, child, request)

    private fun getAssistanceActions(user: AuthenticatedUser, child: ChildId) =
        controller.getChildAssistance(dbInstance(), user, clock, child).assistanceActions.map {
            it.action
        }

    private fun updateAssistanceAction(
        user: AuthenticatedUser,
        id: AssistanceActionId,
        request: AssistanceActionRequest
    ): AssistanceAction = controller.updateAssistanceAction(dbInstance(), user, clock, id, request)

    private fun deleteAssistanceAction(user: AuthenticatedUser, id: AssistanceActionId) =
        controller.deleteAssistanceAction(dbInstance(), user, clock, id)
}
