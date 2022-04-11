// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.resetDatabase
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testRoundTheClockDaycare
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals

class AttendanceUpkeepIntegrationTest : FullApplicationTest() {
    @Autowired
    private lateinit var scheduledJobs: ScheduledJobs

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.resetDatabase()
            tx.insertGeneralTestFixtures()
        }
    }

    @Test
    fun `null departures are set to 2359 of arrival day`() {
        // test with daylight saving time change on 25.10.2020
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                arrived = arrived,
                departed = null
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(LocalTime.of(23, 59), attendanceEndTimes.first())
    }

    @Test
    fun `null departures are not set in round the clock units if there's an active placement`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                startDate = LocalDate.now().minusDays(1),
                endDate = LocalDate.now().plusDays(1),
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                arrived = arrived,
                departed = null
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(null, attendanceEndTimes.first())
    }

    @Test
    fun `null departures are set to 2359 of arrival day in round the clock units if placement has ended`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            // Placement to the attendance's unit has ended
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                startDate = LocalDate.now().minusDays(2),
                endDate = LocalDate.now().minusDays(1),
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                arrived = arrived,
                departed = null
            )

            // A placement to another unit is active
            it.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(1),
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(LocalTime.of(23, 59), attendanceEndTimes.first())
    }

    private fun getAttendanceEndTimes() =
        db.read {
            it.createQuery("SELECT id, end_time FROM child_attendance")
                .map { rv -> rv.mapColumn<LocalTime?>("end_time") }
                .toList()
        }
}
