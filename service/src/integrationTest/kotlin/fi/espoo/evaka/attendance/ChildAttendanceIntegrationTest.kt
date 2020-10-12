package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.daycare.service.CareType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.updateDaycareAclWithEmployee
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDecisionMaker_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

class ChildAttendanceIntegrationTest : FullApplicationTest() {
    private val staffUser = AuthenticatedUser(testDecisionMaker_1.id, emptySet())
    private val groupId = UUID.randomUUID()
    private val daycarePlacementId = UUID.randomUUID()
    private val placementStart = LocalDate.now().minusDays(30)
    private val placementEnd = LocalDate.now().plusDays(30)

    @BeforeEach
    fun beforeEach() {
        jdbi.handle { h ->
            resetDatabase(h)
            insertGeneralTestFixtures(h)
            h.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id))
            insertTestPlacement(
                h = h,
                id = daycarePlacementId,
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = placementStart,
                endDate = placementEnd
            )
            insertTestDaycareGroupPlacement(
                h = h,
                daycarePlacementId = daycarePlacementId,
                groupId = groupId,
                startDate = placementStart,
                endDate = placementEnd
            )
            updateDaycareAclWithEmployee(h, testDaycare.id, staffUser.id, UserRole.STAFF)
        }
    }

    @Test
    fun `get group children - child is coming`() {
        jdbi.handle {
            insertTestChildAttendance(
                h = it,
                childId = testChild_1.id,
                arrived = OffsetDateTime.now().minusDays(1).minusHours(8),
                departed = OffsetDateTime.now().minusDays(1)
            )
        }
        assertOneChildWithStatus(AttendanceStatus.COMING)
    }

    @Test
    fun `get group children - child is present`() {
        jdbi.handle {
            insertTestChildAttendance(
                h = it,
                childId = testChild_1.id,
                arrived = OffsetDateTime.now().minusHours(3),
                departed = null
            )
        }
        assertOneChildWithStatus(AttendanceStatus.PRESENT)
    }

    @Test
    fun `get group children - child has departed`() {
        jdbi.handle {
            insertTestChildAttendance(
                h = it,
                childId = testChild_1.id,
                arrived = OffsetDateTime.now().minusHours(8),
                departed = OffsetDateTime.now().minusMinutes(1)
            )
        }
        assertOneChildWithStatus(AttendanceStatus.DEPARTED)
    }

    @Test
    fun `get group children - child is absent`() {
        jdbi.handle {
            insertTestAbsence(
                h = it,
                childId = testChild_1.id,
                careType = CareType.DAYCARE,
                date = LocalDate.now(),
                absenceType = AbsenceType.SICKLEAVE
            )
        }
        assertOneChildWithStatus(AttendanceStatus.ABSENT)
    }

    @Test
    fun `marking child arrived`() {
        markArrived(ArrivalRequest(childId = testChild_1.id))
        assertOneChildWithStatus(AttendanceStatus.PRESENT)
    }

    @Test
    fun `marking child departed`() {
        jdbi.handle {
            insertTestChildAttendance(
                h = it,
                childId = testChild_1.id,
                arrived = OffsetDateTime.now().minusHours(3),
                departed = null
            )
        }
        markDeparted(DepartureRequest(childId = testChild_1.id))
        assertOneChildWithStatus(AttendanceStatus.DEPARTED)
    }

    private fun fetchChildrenInGroup(): List<ChildInGroup> {
        val (_, res, result) = http.get("/child-attendances/current?groupId=$groupId")
            .asUser(staffUser)
            .responseObject<List<ChildInGroup>>(objectMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun markArrived(request: ArrivalRequest) {
        val (_, res, _) = http.post("/child-attendances/arrive")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(staffUser)
            .responseObject<Unit>()

        assertEquals(200, res.statusCode)
    }

    private fun markDeparted(request: DepartureRequest) {
        val (_, res, _) = http.post("/child-attendances/depart")
            .jsonBody(objectMapper.writeValueAsString(request))
            .asUser(staffUser)
            .responseObject<Unit>()

        assertEquals(200, res.statusCode)
    }

    private fun assertOneChildWithStatus(status: AttendanceStatus) {
        val children = fetchChildrenInGroup()
        assertEquals(1, children.size)
        assertEquals(status, children.first().status)
    }
}
