// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import com.github.kittinunf.fuel.jackson.responseObject
import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.asUser
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.createMobileDeviceToUnit
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.TimeInterval
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.withMockedTime
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MobileUnitControllerIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    private val mobileUser = AuthenticatedUser.MobileDevice(MobileDeviceId(UUID.randomUUID()))
    private val today = LocalDate.of(2022, 2, 3)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 5, 1))
    private val placementStart = today.minusDays(30)
    private val placementEnd = today.plusDays(30)

    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            enabledPilotFeatures =
                setOf(
                    PilotFeature.MESSAGING,
                    PilotFeature.MOBILE,
                    PilotFeature.RESERVATIONS,
                    PilotFeature.PLACEMENT_TERMINATION,
                    PilotFeature.REALTIME_STAFF_ATTENDANCE,
                ),
        )
    private val daycare2 =
        DevDaycare(
            areaId = area.id,
            name = "Test Daycare 2",
            enabledPilotFeatures = setOf(PilotFeature.MESSAGING),
        )

    // dateOfBirth values affect occupancy coefficients and utilization
    private val child1 = DevPerson(dateOfBirth = LocalDate.of(2017, 6, 1))
    private val child2 = DevPerson(dateOfBirth = LocalDate.of(2016, 3, 1))
    private val child3 = DevPerson(dateOfBirth = LocalDate.of(2018, 9, 1))
    private val child4 = DevPerson(dateOfBirth = LocalDate.of(2019, 3, 2))
    private val child5 = DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13))
    private val child6 = DevPerson(dateOfBirth = LocalDate.of(2018, 11, 13))
    private val child7 = DevPerson(dateOfBirth = LocalDate.of(2018, 7, 28))

    private lateinit var groupId: GroupId

    @BeforeEach
    fun beforeEach() {
        val groupId2 = GroupId(UUID.randomUUID())

        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            listOf(child1, child2, child3, child4, child5, child6, child7).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insertServiceNeedOptions()
            groupId = tx.insert(DevDaycareGroup(daycareId = daycare.id, name = "Testaajat"))
            tx.insert(DevDaycareGroup(id = groupId2, daycareId = daycare.id, name = "Tyhjä"))
            listOf(child1, child2, child3, child4, child5).forEach { child ->
                val placement =
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = child.id,
                        unitId = daycare.id,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                tx.insert(placement)
                tx.insert(
                    DevDaycareGroupPlacement(
                        daycarePlacementId = placement.id,
                        daycareGroupId = groupId,
                        startDate = placementStart,
                        endDate = placementEnd,
                    )
                )
            }

            // child6 has multiple group placements
            val child6Placement =
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child6.id,
                    unitId = daycare.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                )
            tx.insert(child6Placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child6Placement.id,
                    daycareGroupId = groupId2,
                    startDate = placementStart,
                    endDate = today.minusDays(1),
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = child6Placement.id,
                    daycareGroupId = groupId,
                    startDate = today,
                    endDate = placementEnd,
                )
            )

            tx.createMobileDeviceToUnit(mobileUser.id, daycare.id)

            tx.insertAttendance(
                child1.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(8, 30), null),
                now,
                mobileUser.evakaUserId,
            )
            tx.insertAttendance(
                child2.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(9, 0), null),
                now,
                mobileUser.evakaUserId,
            )
            tx.insertAttendance(
                child3.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(9, 30), null),
                now,
                mobileUser.evakaUserId,
            )
            tx.insertAttendance(
                child4.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(10, 0), null),
                now,
                mobileUser.evakaUserId,
            )
            tx.insertAttendance(
                child5.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(10, 15), null),
                now,
                mobileUser.evakaUserId,
            )
            tx.insertAttendance(
                child6.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(10, 30), null),
                now,
                mobileUser.evakaUserId,
            )

            val employee1 = DevEmployee(firstName = "One", lastName = "in group 1")
            tx.insert(
                employee1,
                mapOf(daycare.id to UserRole.STAFF),
                mapOf(daycare.id to listOf(groupId)),
            )
            tx.markStaffArrival(
                employee1.id,
                groupId,
                now.minusDays(1),
                BigDecimal(7.0),
                now,
                mobileUser.evakaUserId,
            )

            val employee2 = DevEmployee(firstName = "Two", lastName = "in group 1")
            tx.insert(
                employee2,
                mapOf(daycare.id to UserRole.STAFF),
                mapOf(daycare.id to listOf(groupId)),
            )
            tx.markStaffArrival(
                employee2.id,
                groupId,
                now.minusDays(1),
                BigDecimal(7.0),
                now,
                mobileUser.evakaUserId,
            )

            val employee3 = DevEmployee(firstName = "Three", lastName = "in group 2")
            tx.insert(
                employee3,
                mapOf(daycare.id to UserRole.STAFF),
                mapOf(daycare.id to listOf(groupId2)),
            )
            tx.markStaffArrival(
                employee3.id,
                groupId2,
                now.minusDays(1),
                BigDecimal(7.0),
                now,
                mobileUser.evakaUserId,
            )
        }
    }

    @Test
    fun `unit utilization is same in both unit info and stats`() {
        val unitInfo = fetchUnitInfo(daycare.id)
        val allUnitStats = fetchUnitStats(listOf(daycare.id))

        assertEquals(32.1, unitInfo.utilization)
        assertEquals(unitInfo.utilization, allUnitStats.first().utilization)
    }

    @Test
    fun `unit utilization is same in both unit info and stats when backup care is involved`() {
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = child7.id,
                    unitId = daycare2.id,
                    startDate = placementStart,
                    endDate = placementEnd,
                    type = PlacementType.PRESCHOOL_DAYCARE,
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = child7.id,
                    unitId = daycare.id,
                    period = FiniteDateRange(placementStart, placementEnd),
                    groupId = groupId,
                )
            )
            tx.insertAttendance(
                child7.id,
                daycare.id,
                today,
                TimeInterval(LocalTime.of(6, 0), null),
                now,
                mobileUser.evakaUserId,
            )
        }
        val unitInfo = fetchUnitInfo(daycare.id)
        val allUnitStats = fetchUnitStats(listOf(daycare.id))

        assertEquals(36.9, unitInfo.utilization)
        assertEquals(unitInfo.utilization, allUnitStats.first().utilization)
    }

    private fun fetchUnitInfo(unitId: DaycareId): UnitInfo {
        val (_, res, result) =
            http
                .get("/employee-mobile/units/$unitId")
                .asUser(mobileUser)
                .withMockedTime(now)
                .responseObject<UnitInfo>(jackson2JsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }

    private fun fetchUnitStats(unitIds: List<DaycareId>): List<UnitStats> {
        val (_, res, result) =
            http
                .get(
                    "/employee-mobile/units/stats",
                    listOf(Pair("unitIds", unitIds.joinToString { it.toString() })),
                )
                .asUser(mobileUser)
                .withMockedTime(now)
                .responseObject<List<UnitStats>>(jackson2JsonMapper)

        assertEquals(200, res.statusCode)
        return result.get()
    }
}
