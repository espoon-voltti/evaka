// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testRoundTheClockDaycare
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AttendanceUpkeepIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    private val clock = MockEvakaClock(2020, 10, 26, 0, 10)

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testRoundTheClockDaycare)
            tx.insert(testChild_1, DevPersonType.CHILD)
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
                departed = null,
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db, clock)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(LocalTime.of(23, 59), attendanceEndTimes.first())
    }

    @Test
    fun `null departures are not set in round the clock units if there's an active placement`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testRoundTheClockDaycare.id,
                    startDate = clock.now().toLocalDate().minusDays(1),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                arrived = arrived,
                departed = null,
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db, clock)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(null, attendanceEndTimes.first())
    }

    @Test
    fun `null departures are set to 2359 of arrival day in round the clock units if placement has ended`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            // Placement to the attendance's unit has ended
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testRoundTheClockDaycare.id,
                    startDate = clock.now().toLocalDate().minusDays(2),
                    endDate = clock.now().toLocalDate().minusDays(1),
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                arrived = arrived,
                departed = null,
            )

            // A placement to another unit is active
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.now().toLocalDate(),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db, clock)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(LocalTime.of(23, 59), attendanceEndTimes.first())
    }

    @Test
    fun `attendances are not ended in round the clock units if a backup placement is still active`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            // Placement to another unit is active
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.now().toLocalDate().minusDays(2),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
            // Backup placement to attendance's unit is active
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testRoundTheClockDaycare.id,
                    groupId = null,
                    period =
                        FiniteDateRange(
                            clock.now().toLocalDate().minusDays(2),
                            clock.now().toLocalDate().plusDays(1),
                        ),
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                arrived = arrived,
                departed = null,
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db, clock)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(null, attendanceEndTimes.first())
    }

    @Test
    fun `null departures are set to 2359 of arrival day in round the clock units if backup placement to attendance unit has ended`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            // Placement to another unit is active
            it.insert(
                DevPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = clock.now().toLocalDate().minusDays(2),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
            // Backup placement to attendance's unit has ended
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testRoundTheClockDaycare.id,
                    groupId = null,
                    period =
                        FiniteDateRange(
                            clock.now().toLocalDate().minusDays(2),
                            clock.now().toLocalDate().minusDays(1),
                        ),
                )
            )
            // Backup placement to another unit has ended
            it.insert(
                DevBackupCare(
                    childId = testChild_1.id,
                    unitId = testDaycare2.id,
                    groupId = null,
                    period =
                        FiniteDateRange(
                            clock.now().toLocalDate(),
                            clock.now().toLocalDate().plusDays(1),
                        ),
                )
            )
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id,
                arrived = arrived,
                departed = null,
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db, clock)

        val attendanceEndTimes = getAttendanceEndTimes()
        assertEquals(1, attendanceEndTimes.size)
        assertEquals(LocalTime.of(23, 59), attendanceEndTimes.first())
    }

    private fun getAttendanceEndTimes() =
        db.read {
            it.createQuery { sql("SELECT id, end_time FROM child_attendance") }
                .toList { column<LocalTime?>("end_time") }
        }
}
