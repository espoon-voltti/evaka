// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.insertServiceNeedOptions
import evaka.core.placement.PlacementType
import evaka.core.shared.EvakaUserId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevServiceNeed
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.snDefaultDaycare
import evaka.core.snDefaultPreschool
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class PlacementCountReportControllerTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var placementCountReportController: PlacementCountReportController

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    @Test
    fun `Care type filter works`() {
        val mockToday =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))
        db.transaction { tx ->
            tx.insertServiceNeedOptions()
            tx.insert(admin)
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId, openingDate = mockToday.today()))
            val preschoolChildId = tx.insert(DevPerson(), DevPersonType.CHILD)
            val preschoolPlacementId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = preschoolChildId,
                        unitId = unitId,
                        startDate = mockToday.today().minusMonths(1),
                        endDate = mockToday.today().plusMonths(1),
                    )
                )
            val period =
                FiniteDateRange(mockToday.today().minusMonths(1), mockToday.today().plusMonths(1))
            tx.insert(
                DevServiceNeed(
                    placementId = preschoolPlacementId,
                    startDate = period.start,
                    endDate = period.end,
                    optionId = snDefaultPreschool.id,
                    confirmedBy = EvakaUserId(admin.id.raw),
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )

            val daycareU3yChildId =
                tx.insert(
                    DevPerson(dateOfBirth = mockToday.today().minusYears(2)),
                    DevPersonType.CHILD,
                )
            val daycarePlacementId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE,
                        childId = daycareU3yChildId,
                        unitId = unitId,
                        startDate = mockToday.today().minusMonths(1),
                        endDate = mockToday.today().plusMonths(1),
                    )
                )
            val period1 =
                FiniteDateRange(mockToday.today().minusMonths(1), mockToday.today().plusMonths(1))
            tx.insert(
                DevServiceNeed(
                    placementId = daycarePlacementId,
                    startDate = period1.start,
                    endDate = period1.end,
                    optionId = snDefaultDaycare.id,
                    confirmedBy = EvakaUserId(admin.id.raw),
                    confirmedAt = HelsinkiDateTime.now(),
                )
            )
        }
        val reportAll =
            placementCountReportController.getPlacementCountReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                mockToday.today(),
                null,
                null,
            )
        assertEquals(2, reportAll.placementCount)
        assertEquals(1, reportAll.placementCount3vAndOver)
        assertEquals(1, reportAll.placementCountUnder3v)

        val reportPreschool =
            placementCountReportController.getPlacementCountReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                mockToday.today(),
                null,
                listOf(PlacementType.PRESCHOOL),
            )
        assertEquals(1, reportPreschool.placementCount)
        assertEquals(1, reportPreschool.placementCount3vAndOver)
        assertEquals(0, reportPreschool.placementCountUnder3v)
    }
}
