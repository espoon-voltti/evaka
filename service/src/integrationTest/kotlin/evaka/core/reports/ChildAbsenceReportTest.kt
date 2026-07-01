// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.placement.PlacementType
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevDaycareGroupPlacement
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ChildAbsenceReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var childAbsenceReport: ChildAbsenceReport

    private val today = LocalDate.of(2024, 3, 15)
    private val now = HelsinkiDateTime.of(today, LocalTime.of(12, 0))
    private val range = FiniteDateRange(today.minusDays(10), today)

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)

    private fun body() =
        ChildAbsenceReport.ChildAbsenceReportBody(
            range = FiniteDateRange(today.minusDays(20), today.plusDays(5)),
            areaId = null,
            unitId = daycare.id,
            groupId = null,
        )

    @Test
    fun `full-day absence requires all categories and takes its type from the nonbillable row`() {
        val childA = DevPerson(firstName = "Aaro", lastName = "Aalto")
        val childB = DevPerson(firstName = "Bertta", lastName = "Berg")

        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(childA, DevPersonType.CHILD)
            tx.insert(childB, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = childA.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    startDate = range.start,
                    endDate = range.end,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = childB.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    startDate = range.start,
                    endDate = range.end,
                )
            )

            // childA day 1: absent in both categories -> full day, SICKLEAVE
            tx.insert(
                DevAbsence(
                    childId = childA.id,
                    date = today.minusDays(3),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = childA.id,
                    date = today.minusDays(3),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
            // childA day 2: absent only in billable -> NOT a full day
            tx.insert(
                DevAbsence(
                    childId = childA.id,
                    date = today.minusDays(2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            // childA day 3: both categories, types differ -> full day, type from nonbillable
            // (SICKLEAVE)
            tx.insert(
                DevAbsence(
                    childId = childA.id,
                    date = today.minusDays(1),
                    absenceType = AbsenceType.OTHER_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = childA.id,
                    date = today.minusDays(1),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
            // childB: only a billable absence -> no full day -> excluded from results
            tx.insert(
                DevAbsence(
                    childId = childB.id,
                    date = today.minusDays(3),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        val results =
            childAbsenceReport.getChildAbsenceReport(
                dbInstance(),
                MockEvakaClock(now),
                admin.user,
                body(),
            )

        assertEquals(1, results.size)
        val row = results.first()
        assertEquals(childA.id, row.childId)
        assertEquals(PlacementType.DAYCARE_FIVE_YEAR_OLDS, row.placementType)
        assertEquals(mapOf(AbsenceType.SICKLEAVE to 2), row.absenceCountsByType)
    }

    @Test
    fun `future days are clamped out and single-category placements count`() {
        val child = DevPerson(firstName = "Cecilia", lastName = "Castren")
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE,
                    startDate = today.minusDays(20),
                    endDate = today.plusDays(20),
                )
            )
            // today: counts (DAYCARE has only BILLABLE category)
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today,
                    absenceType = AbsenceType.UNKNOWN_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            // tomorrow: future, excluded by clamp
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.plusDays(1),
                    absenceType = AbsenceType.UNKNOWN_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        val results =
            childAbsenceReport.getChildAbsenceReport(
                dbInstance(),
                MockEvakaClock(now),
                admin.user,
                body(),
            )

        assertEquals(1, results.size)
        assertEquals(mapOf(AbsenceType.UNKNOWN_ABSENCE to 1), results.first().absenceCountsByType)
    }

    @Test
    fun `full-day PLANNED_ABSENCE is reported as its own type`() {
        val child = DevPerson(firstName = "Dagmar", lastName = "Dahl")
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    startDate = range.start,
                    endDate = range.end,
                )
            )
            // Both categories absent with PLANNED_ABSENCE -> full day, reported as PLANNED_ABSENCE
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(3),
                    absenceType = AbsenceType.PLANNED_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(3),
                    absenceType = AbsenceType.PLANNED_ABSENCE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
        }

        val results =
            childAbsenceReport.getChildAbsenceReport(
                dbInstance(),
                MockEvakaClock(now),
                admin.user,
                body(),
            )

        assertEquals(1, results.size)
        assertEquals(mapOf(AbsenceType.PLANNED_ABSENCE to 1), results.first().absenceCountsByType)
    }

    @Test
    fun `group filter counts only absences in the queried group's date range`() {
        val child = DevPerson(firstName = "Erika", lastName = "Eriksson")
        val groupG = DevDaycareGroup(daycareId = daycare.id, name = "G")
        val groupH = DevDaycareGroup(daycareId = daycare.id, name = "H")
        // Child in G for days -10..-6, in H for days -5..0 (both within the range)
        val gStart = today.minusDays(10)
        val gEnd = today.minusDays(6)
        val hStart = today.minusDays(5)
        val hEnd = today

        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(groupG)
            tx.insert(groupH)
            tx.insert(child, DevPersonType.CHILD)
            val placement =
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    type = PlacementType.DAYCARE_FIVE_YEAR_OLDS,
                    startDate = gStart,
                    endDate = hEnd,
                )
            tx.insert(placement)
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = groupG.id,
                    startDate = gStart,
                    endDate = gEnd,
                )
            )
            tx.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placement.id,
                    daycareGroupId = groupH.id,
                    startDate = hStart,
                    endDate = hEnd,
                )
            )

            // Absence on a day inside the G period -> should be counted when filtering by G
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(8),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(8),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
            // Absence on a day inside the H period -> should NOT be counted when filtering by G
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )
        }

        val results =
            childAbsenceReport.getChildAbsenceReport(
                dbInstance(),
                MockEvakaClock(now),
                admin.user,
                ChildAbsenceReport.ChildAbsenceReportBody(
                    range = FiniteDateRange(today.minusDays(20), today.plusDays(5)),
                    areaId = null,
                    unitId = daycare.id,
                    groupId = groupG.id,
                ),
            )

        assertEquals(1, results.size)
        assertEquals(child.id, results.first().childId)
        // Only the G-period absence counted; H-period absence excluded
        assertEquals(mapOf(AbsenceType.SICKLEAVE to 1), results.first().absenceCountsByType)
    }
}
