// SPDX-FileCopyrightText: 2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.bi

import evaka.core.PureJdbiTest
import evaka.core.absence.AbsenceCategory
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevDaycareGroup
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevStaffAttendance
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.europeHelsinki
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class BiSnapshotQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val config =
        BiExportConfig(includePII = false, includeLegacyColumns = false, windowDays = 7)
    private val today = LocalDate.now(europeHelsinki)
    private val windowStart = today.minusDays(config.windowDays.toLong())
    private val yesterday = today.minusDays(1)

    @Test
    fun `getAbsencesSnapshot includes absences dated in the window`() {
        val child = DevPerson()
        // Modified long before the window, which a delta query would leave out
        val modifiedAt = HelsinkiDateTime.of(today.minusYears(1), LocalTime.NOON)
        fun absence(date: LocalDate) =
            DevAbsence(
                childId = child.id,
                date = date,
                absenceCategory = AbsenceCategory.BILLABLE,
                modifiedAt = modifiedAt,
            )
        val beforeWindow = absence(windowStart.minusDays(1))
        val firstDay = absence(windowStart)
        val lastDay = absence(yesterday)
        val exportDay = absence(today)
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            listOf(beforeWindow, firstDay, lastDay, exportDay).forEach { tx.insert(it) }
        }

        assertEquals(
            setOf(firstDay.id.toString(), lastDay.id.toString()),
            exportedIds(BiQueries.getAbsencesSnapshot),
        )
    }

    @Test
    fun `getStaffAttendanceRealtimeSnapshot includes attendances that arrived in the window in Helsinki time`() {
        val area = DevCareArea()
        val daycare = DevDaycare(areaId = area.id)
        val group = DevDaycareGroup(daycareId = daycare.id)
        val employee = DevEmployee()
        fun attendance(arrived: HelsinkiDateTime) =
            DevStaffAttendance(
                employeeId = employee.id,
                groupId = group.id,
                arrived = arrived,
                departed = arrived.plusMinutes(20),
                modifiedAt = arrived,
                modifiedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
            )
        // 00:30 in Helsinki is on the previous day in UTC
        val beforeWindow =
            attendance(HelsinkiDateTime.of(windowStart.minusDays(1), LocalTime.of(23, 30)))
        val firstDay = attendance(HelsinkiDateTime.of(windowStart, LocalTime.of(0, 30)))
        val lastDay = attendance(HelsinkiDateTime.of(yesterday, LocalTime.of(23, 30)))
        val exportDay = attendance(HelsinkiDateTime.of(today, LocalTime.of(0, 30)))
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(group)
            tx.insert(employee)
            listOf(beforeWindow, firstDay, lastDay, exportDay).forEach { tx.insert(it) }
        }

        assertEquals(
            setOf(firstDay.id.toString(), lastDay.id.toString()),
            exportedIds(BiQueries.getStaffAttendanceRealtimeSnapshot),
        )
    }

    private fun exportedIds(query: BiQueries.CsvQuery): Set<String> = db.read { tx ->
        query(tx, config) { records ->
            val rows = records.map { it.trimEnd().split(',') }.toList()
            val idColumn = rows.first().indexOf("id")
            rows.drop(1).map { it[idColumn].removeSurrounding("\"") }.toSet()
        }
    }
}
