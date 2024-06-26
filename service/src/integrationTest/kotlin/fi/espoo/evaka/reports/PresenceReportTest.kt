// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceCategory.BILLABLE
import fi.espoo.evaka.absence.AbsenceCategory.NONBILLABLE
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PresenceReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insert(testChild_1, DevPersonType.CHILD)
            groupId = tx.insert(DevDaycareGroup(daycareId = testDaycare.id))
        }
    }

    private lateinit var groupId: GroupId
    private val reportStart = LocalDate.of(2022, 1, 3)
    private val reportEnd = LocalDate.of(2022, 1, 9)

    @Test
    fun `child with no absences is present`() {
        createPlacement(DAYCARE)

        val report = getReport()
        assertEquals(5, report.size)
        assertEquals(true, report.all { it.present == true })
    }

    @Test
    fun `daycare child with a billable absence is not present`() {
        createPlacement(DAYCARE)
        addAbsence(reportStart, BILLABLE)

        val report = getReport()
        assertEquals(5, report.size)
        assertEquals(false, report.find { it.date == reportStart }?.present)
        assertEquals(true, report.filterNot { it.date == reportStart }.all { it.present == true })
    }

    @Test
    fun `preschool daycare child with only a billable absence is present`() {
        createPlacement(PRESCHOOL_DAYCARE)
        addAbsence(reportStart, BILLABLE)

        val report = getReport()
        assertEquals(5, report.size)
        assertEquals(true, report.all { it.present == true })
    }

    @Test
    fun `preschool daycare child with a billable and a non-billable absence is not present`() {
        createPlacement(PRESCHOOL_DAYCARE)
        addAbsence(reportStart, BILLABLE)
        addAbsence(reportStart, NONBILLABLE)

        val report = getReport()
        assertEquals(5, report.size)
        assertEquals(false, report.find { it.date == reportStart }?.present)
        assertEquals(true, report.filterNot { it.date == reportStart }.all { it.present == true })
    }

    @Test
    fun `preschool child with a non-billable absence is not present`() {
        createPlacement(PRESCHOOL)
        addAbsence(reportStart, NONBILLABLE)

        val report = getReport()
        assertEquals(5, report.size)
        assertEquals(false, report.find { it.date == reportStart }?.present)
        assertEquals(true, report.filterNot { it.date == reportStart }.all { it.present == true })
    }

    private fun getReport(): List<PresenceReportRow> {
        return db.read { it.getPresenceRows(reportStart, reportEnd) }
    }

    private fun createPlacement(type: PlacementType) {
        db.transaction {
            val placementId =
                it.insert(
                    DevPlacement(
                        type = type,
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        startDate = reportStart,
                        endDate = reportEnd
                    )
                )
            it.insert(
                DevDaycareGroupPlacement(
                    daycarePlacementId = placementId,
                    daycareGroupId = groupId,
                    startDate = reportStart,
                    endDate = reportEnd
                )
            )
        }
    }

    private fun addAbsence(date: LocalDate, category: AbsenceCategory) {
        db.transaction {
            it.insert(
                DevAbsence(
                    childId = testChild_1.id,
                    date = date,
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = category
                )
            )
        }
    }
}
