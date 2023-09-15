// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PlacementGuaranteeReportControllerTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired
    private lateinit var placementGuaranteeReportController: PlacementGuaranteeReportController

    @Test
    fun `getPlacementGuaranteeReport smoke`() {
        val today = LocalDate.of(2023, 9, 12)
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val clock = MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(8, 8)))
        val unitId =
            db.transaction { tx ->
                val areaId = tx.insertTestCareArea(DevCareArea())
                tx.insertTestDaycare(DevDaycare(areaId = areaId))
            }
        db.transaction { tx ->
            tx.insertSingleDatePlacement(unitId, yesterday, false)
            tx.insertSingleDatePlacement(unitId, today, false)
            tx.insertSingleDatePlacement(unitId, tomorrow, false)
        }
        val childYesterday =
            db.transaction { tx -> tx.insertSingleDatePlacement(unitId, yesterday, true) }
        val childToday = db.transaction { tx -> tx.insertSingleDatePlacement(unitId, today, true) }
        val childTomorrow =
            db.transaction { tx -> tx.insertSingleDatePlacement(unitId, tomorrow, true) }

        val yesterdayReport =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                user,
                clock,
                yesterday
            )
        assertThat(yesterdayReport)
            .extracting({ it.childId }, { it.placementStartDate }, { it.placementEndDate })
            .containsExactly(Tuple(childYesterday, yesterday, yesterday))

        val todayReport =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                user,
                clock,
                today
            )
        assertThat(todayReport)
            .extracting({ it.childId }, { it.placementStartDate }, { it.placementEndDate })
            .containsExactly(Tuple(childToday, today, today))

        val tomorrowReport =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                user,
                clock,
                tomorrow
            )
        assertThat(tomorrowReport)
            .extracting({ it.childId }, { it.placementStartDate }, { it.placementEndDate })
            .containsExactly(Tuple(childTomorrow, tomorrow, tomorrow))
    }

    @Test
    fun `getPlacementGuaranteeReport unit supervisor`() {
        val date = LocalDate.of(2023, 9, 12)
        val areaId = db.transaction { tx -> tx.insertTestCareArea(DevCareArea()) }
        val unitId1 = db.transaction { tx -> tx.insertTestDaycare(DevDaycare(areaId = areaId)) }
        val unitId2 = db.transaction { tx -> tx.insertTestDaycare(DevDaycare(areaId = areaId)) }
        val childId = db.transaction { tx -> tx.insertSingleDatePlacement(unitId1, date, true) }
        db.transaction { tx -> tx.insertSingleDatePlacement(unitId2, date, true) }
        val employeeId =
            db.transaction { tx ->
                val employeeId = tx.insertTestEmployee(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = unitId1,
                    employeeId = employeeId,
                    role = UserRole.UNIT_SUPERVISOR
                )
                employeeId
            }

        val unit1Rows =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                AuthenticatedUser.Employee(employeeId, setOf()),
                MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(9, 12))),
                date,
                unitId1
            )

        assertThat(unit1Rows).extracting({ it.childId }).containsExactly(Tuple(childId))

        val unit2Rows =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                AuthenticatedUser.Employee(employeeId, setOf()),
                MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(9, 12))),
                date,
                unitId2
            )

        assertThat(unit2Rows).isEmpty()
    }
}

private fun Database.Transaction.insertSingleDatePlacement(
    unitId: DaycareId,
    date: LocalDate,
    placementGuarantee: Boolean
): PersonId {
    val childId = insertTestPerson(DevPerson())
    insertTestChild(DevChild(id = childId))
    insertTestPlacement(
        DevPlacement(
            childId = childId,
            unitId = unitId,
            startDate = date,
            endDate = date,
            placeGuarantee = placementGuarantee
        )
    )
    return childId
}
