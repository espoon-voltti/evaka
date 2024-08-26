// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
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
        val date = LocalDate.of(2023, 9, 12)
        val user = AuthenticatedUser.Employee(EmployeeId(UUID.randomUUID()), setOf(UserRole.ADMIN))
        val clock = MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(8, 8)))
        val unitId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(DevDaycare(areaId = areaId))
            }
        val childId =
            db.transaction { tx ->
                val childId = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = date.minusDays(1),
                        endDate = date.minusDays(1),
                        placeGuarantee = false,
                    )
                )
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = date.plusDays(1),
                        endDate = date.plusDays(1),
                        placeGuarantee = true,
                    )
                )
                childId
            }

        val yesterdayReport =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                user,
                clock,
                date.minusDays(1),
            )
        assertThat(yesterdayReport).isEmpty()

        val todayReport =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                user,
                clock,
                date,
            )
        assertThat(todayReport)
            .extracting({ it.childId }, { it.placementStartDate }, { it.placementEndDate })
            .containsExactly(Tuple(childId, date.plusDays(1), date.plusDays(1)))

        val tomorrowReport =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                user,
                clock,
                date.plusDays(1),
            )
        assertThat(tomorrowReport).isEmpty()
    }

    @Test
    fun `getPlacementGuaranteeReport unit supervisor`() {
        val date = LocalDate.of(2023, 9, 12)
        val areaId = db.transaction { tx -> tx.insert(DevCareArea()) }
        val unitId1 = db.transaction { tx -> tx.insert(DevDaycare(areaId = areaId)) }
        val unitId2 = db.transaction { tx -> tx.insert(DevDaycare(areaId = areaId)) }
        val childId =
            db.transaction { tx ->
                val childId1 = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId1,
                        unitId = unitId1,
                        startDate = date.minusDays(1),
                        endDate = date.minusDays(1),
                        placeGuarantee = false,
                    )
                )
                tx.insert(
                    DevPlacement(
                        childId = childId1,
                        unitId = unitId1,
                        startDate = date.plusDays(1),
                        endDate = date.plusDays(1),
                        placeGuarantee = true,
                    )
                )
                val childId2 = tx.insert(DevPerson(), DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(
                        childId = childId2,
                        unitId = unitId2,
                        startDate = date.minusDays(1),
                        endDate = date.minusDays(1),
                        placeGuarantee = false,
                    )
                )
                tx.insert(
                    DevPlacement(
                        childId = childId2,
                        unitId = unitId2,
                        startDate = date.plusDays(1),
                        endDate = date.plusDays(1),
                        placeGuarantee = true,
                    )
                )
                childId1
            }
        val employeeId =
            db.transaction { tx ->
                val employeeId = tx.insert(DevEmployee())
                tx.insertDaycareAclRow(
                    daycareId = unitId1,
                    employeeId = employeeId,
                    role = UserRole.UNIT_SUPERVISOR,
                )
                employeeId
            }

        val unit1Rows =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                AuthenticatedUser.Employee(employeeId, setOf()),
                MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(9, 12))),
                date,
                unitId1,
            )

        assertThat(unit1Rows).extracting({ it.childId }).containsExactly(Tuple(childId))

        val unit2Rows =
            placementGuaranteeReportController.getPlacementGuaranteeReport(
                dbInstance(),
                AuthenticatedUser.Employee(employeeId, setOf()),
                MockEvakaClock(HelsinkiDateTime.of(date, LocalTime.of(9, 12))),
                date,
                unitId2,
            )

        assertThat(unit2Rows).isEmpty()
    }
}
