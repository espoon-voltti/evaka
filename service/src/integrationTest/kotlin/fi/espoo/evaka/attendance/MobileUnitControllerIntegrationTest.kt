// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.withMockedTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals

class MobileUnitControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val today = LocalDate.of(2022, 2, 3)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 5, 1))

    @BeforeEach
    fun beforeEach() {
        val groupId = GroupId(UUID.randomUUID())
        val groupName = "Testaajat"
        val placementStart = now.toLocalDate().minusDays(30)
        val placementEnd = now.toLocalDate().plusDays(30)

        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestDaycareGroup(DevDaycareGroup(id = groupId, daycareId = testDaycare.id, name = groupName))
            listOf(testChild_1, testChild_2, testChild_3, testChild_4, testChild_5).forEach { child ->
                val daycarePlacementId = PlacementId(UUID.randomUUID())
                tx.insertTestPlacement(
                    id = daycarePlacementId,
                    childId = child.id,
                    unitId = testDaycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                    type = PlacementType.PRESCHOOL_DAYCARE
                )
                tx.insertTestDaycareGroupPlacement(
                    daycarePlacementId = daycarePlacementId,
                    groupId = groupId,
                    startDate = placementStart,
                    endDate = placementEnd
                )
            }
            tx.createMobileDeviceToUnit(mobileUser.id, testDaycare.id)

            tx.insertAttendance(testChild_1.id, testDaycare.id, today, LocalTime.of(8, 30, 0))
            tx.insertAttendance(testChild_2.id, testDaycare.id, today, LocalTime.of(9, 0, 0))
            tx.insertAttendance(testChild_3.id, testDaycare.id, today, LocalTime.of(9, 30, 0))
            tx.insertAttendance(testChild_4.id, testDaycare.id, today, LocalTime.of(10, 0, 0))
            tx.insertAttendance(testChild_5.id, testDaycare.id, today, LocalTime.of(10, 30, 0))

            FixtureBuilder(tx)
                .addEmployee()
                .withName("One", "in group 1")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .saveAnd {
                    tx.markStaffArrival(employeeId, groupId, now.minusDays(1), BigDecimal(7.0))
                }
                .addEmployee()
                .withName("Two", "in group 1")
                .withGroupAccess(testDaycare.id, groupId)
                .withScopedRole(UserRole.STAFF, testDaycare.id)
                .saveAnd {
                    tx.markStaffArrival(employeeId, groupId, now.minusDays(1), BigDecimal(7.0))
                }
        }
    }

    @Test
    fun `unit utilization is same in both unit info and stats`() {
        val unitInfo = fetchUnitInfo(testDaycare.id)
        val allUnitStats = fetchUnitStats(listOf(testDaycare.id))

        assertEquals(unitInfo.utilization, allUnitStats.first().utilization)
    }

    private fun fetchUnitInfo(unitId: DaycareId): UnitInfo {
        val (_, res, result) = http.get(
            "/mobile/units/$unitId",
            listOf(Pair("useRealtimeStaffAttendance", true))
        )
            .asUser(mobileUser)
            .withMockedTime(now)
            .responseObject<UnitInfo>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun fetchUnitStats(unitIds: List<DaycareId>): List<UnitStats> {
        val (_, res, result) = http.get(
            "/mobile/units/stats",
            listOf(Pair("unitIds", unitIds.joinToString { it.toString() }), Pair("useRealtimeStaffAttendance", true))
        )
            .asUser(mobileUser)
            .withMockedTime(now)
            .responseObject<List<UnitStats>>(jsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }
}
