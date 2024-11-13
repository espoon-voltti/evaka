// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChildAttendance
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.snDaycareHours120
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ExceededServiceNeedsReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    lateinit var exceededServiceNeedsReportController: ExceededServiceNeedsReportController

    @Test
    fun `it works`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 1, 31)
        val today = end.plusDays(1)

        val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val shiftDaycare =
            DevDaycare(
                areaId = area.id,
                shiftCareOperationTimes =
                    (1..7).map { TimeRange(LocalTime.of(6, 0), LocalTime.of(22, 0)) },
            )

        val child1 = DevPerson()
        val child2 = DevPerson()
        val child3 = DevPerson()
        val child4 = DevPerson()

        val daycareGroup = DevDaycareGroup(daycareId = daycare.id, startDate = start, endDate = end)

        db.transaction { tx ->
            tx.insertServiceNeedOption(snDaycareHours120)
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(shiftDaycare)
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(child3, DevPersonType.CHILD)
            tx.insert(child4, DevPersonType.CHILD)

            tx.insert(daycareGroup)

            tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = start,
                        endDate = end,
                    )
                )
                .also { placementId ->
                    val period = FiniteDateRange(start, end)
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period.start,
                            endDate = period.end,
                            optionId = snDaycareHours120.id,
                            confirmedBy = admin.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                    tx.insert(
                        DevDaycareGroupPlacement(
                            daycarePlacementId = placementId,
                            daycareGroupId = daycareGroup.id,
                            startDate = start,
                            endDate = end,
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = child2.id,
                        unitId = daycare.id,
                        startDate = start,
                        endDate = end,
                    )
                )
                .also { placementId ->
                    val period = FiniteDateRange(start, end)
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period.start,
                            endDate = period.end,
                            optionId = snDaycareHours120.id,
                            confirmedBy = admin.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = child3.id,
                        unitId = daycare.id,
                        startDate = start,
                        endDate = end,
                    )
                )
                .also { placementId ->
                    val period = FiniteDateRange(start, end)
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period.start,
                            endDate = period.end,
                            optionId = snDaycareHours120.id,
                            confirmedBy = admin.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                }

            tx.insert(
                    DevPlacement(
                        childId = child4.id,
                        unitId = daycare.id,
                        startDate = start,
                        endDate = end,
                    )
                )
                .also { placementId ->
                    val period = FiniteDateRange(start, end)
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = period.start,
                            endDate = period.end,
                            optionId = snDaycareHours120.id,
                            shiftCare = ShiftCareType.FULL,
                            confirmedBy = admin.evakaUserId,
                            confirmedAt = HelsinkiDateTime.now(),
                        )
                    )
                }

            // 22 operation days
            FiniteDateRange(start, end)
                .dates()
                .filterNot { it.isWeekend() }
                .forEach { date ->
                    // child1: 22 * 8 = 176 > 120
                    tx.insert(
                        DevChildAttendance(
                            child1.id,
                            daycare.id,
                            date,
                            LocalTime.of(8, 0),
                            LocalTime.of(16, 0),
                        )
                    )
                    // child2: 22 * 4 = 88 < 120
                    tx.insert(
                        DevChildAttendance(
                            child2.id,
                            daycare.id,
                            date,
                            LocalTime.of(8, 0),
                            LocalTime.of(12, 0),
                        )
                    )
                    // child3: free absence on all weekdays -> no excess hours
                    tx.insert(
                        DevAbsence(
                            childId = child3.id,
                            date = date,
                            absenceCategory = AbsenceCategory.BILLABLE,
                            absenceType = AbsenceType.FORCE_MAJEURE,
                        )
                    )
                }
            FiniteDateRange(start, end).dates().forEach { date ->
                // child4: shift care + free absence on all days -> no excess hours
                tx.insert(
                    DevAbsence(
                        childId = child4.id,
                        date = date,
                        absenceCategory = AbsenceCategory.BILLABLE,
                        absenceType = AbsenceType.FORCE_MAJEURE,
                    )
                )
            }
        }

        val rows =
            exceededServiceNeedsReportController.getExceededServiceNeedReportRows(
                dbInstance(),
                admin.user,
                MockEvakaClock(HelsinkiDateTime.of(today, LocalTime.of(12, 0))),
                daycare.id,
                start.year,
                start.monthValue,
            )

        assertEquals(
            listOf(
                ExceededServiceNeedReportRow(
                    childId = child1.id,
                    childFirstName = child1.firstName,
                    childLastName = child1.lastName,
                    unitId = daycare.id,
                    groupId = daycareGroup.id,
                    groupName = daycareGroup.name,
                    serviceNeedHoursPerMonth = 120,
                    usedServiceHours = 176,
                    excessHours = 56,
                )
            ),
            rows,
        )
    }
}
