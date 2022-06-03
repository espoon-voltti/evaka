// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.withMockedTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MobileRealtimeStaffAttendanceControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val mobileUser2 = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val today = LocalDate.of(2022, 2, 3)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 5, 1))

    @BeforeEach
    fun beforeEach() {
        val groupId = GroupId(UUID.randomUUID())
        val groupId2 = GroupId(UUID.randomUUID())
        val groupName = "Group in daycare 1"
        val groupName2 = "Group in daycare 2"

        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId2, daycareId = testDaycare2.id, name = groupName2))

            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)
            tx.createMobileDeviceToUnit(mobileUser2.id, testDaycare2.id)

            FixtureBuilder(tx)
                .addEmployee()
                .withName("Pekka", "in both units")
                .withGroupAccess(testDaycare.id, groupId)
                .withGroupAccess(testDaycare2.id, groupId2)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .withScopedRole(UserRole.STAFF, testDaycare2.id)
                .saveAnd {
                    tx.markStaffArrival(
                        employeeId,
                        groupId,
                        HelsinkiDateTime.of(today, LocalTime.of(8, 0, 0)),
                        BigDecimal(7.0)
                    )
                }
        }
    }

    @Test
    fun `Employee present in one daycare unit should not be present in another`() {
        val attnInDaycare1 = fetchRealtimeStaffAttendances(testDaycare.id, mobileUser)
        val attnInDaycare2 = fetchRealtimeStaffAttendances(testDaycare2.id, mobileUser2)

        assertNotNull(attnInDaycare1.staff.first().present)
        assertNull(attnInDaycare2.staff.first().present)
    }

    private fun fetchRealtimeStaffAttendances(unitId: DaycareId, user: AuthenticatedUser.MobileDevice): CurrentDayStaffAttendanceResponse {
        val (_, res, result) = http.get(
            "/mobile/realtime-staff-attendances",
            listOf(Pair("unitId", unitId))
        )
            .asUser(user)
            .withMockedTime(now)
            .responseObject<CurrentDayStaffAttendanceResponse>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }
}
