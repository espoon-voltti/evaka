// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.job.ScheduledJobs
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class AttendanceUpkeepIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    private val clock = MockEvakaClock(2020, 10, 26, 0, 10)
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val daycare2 = DevDaycare(areaId = area.id)
    private val allDayTimeRange = TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59))
    private val roundTheClockDaycare =
        DevDaycare(
            areaId = area.id,
            shiftCareOperationTimes = List(7) { allDayTimeRange },
            shiftCareOpenOnHolidays = true,
        )
    private val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(roundTheClockDaycare)
            tx.insert(child, DevPersonType.CHILD)
        }
    }

    @Test
    fun `null departures are set to 2359 of arrival day`() {
        // test with daylight saving time change on 25.10.2020
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = daycare.id,
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
                    childId = child.id,
                    unitId = roundTheClockDaycare.id,
                    startDate = clock.now().toLocalDate().minusDays(1),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = roundTheClockDaycare.id,
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
                    childId = child.id,
                    unitId = roundTheClockDaycare.id,
                    startDate = clock.now().toLocalDate().minusDays(2),
                    endDate = clock.now().toLocalDate().minusDays(1),
                )
            )
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = roundTheClockDaycare.id,
                arrived = arrived,
                departed = null,
            )

            // A placement to another unit is active
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.now().toLocalDate().minusDays(2),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
            // Backup placement to attendance's unit is active
            it.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = roundTheClockDaycare.id,
                    groupId = null,
                    period =
                        FiniteDateRange(
                            clock.now().toLocalDate().minusDays(2),
                            clock.now().toLocalDate().plusDays(1),
                        ),
                )
            )
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = roundTheClockDaycare.id,
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
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = clock.now().toLocalDate().minusDays(2),
                    endDate = clock.now().toLocalDate().plusDays(1),
                )
            )
            // Backup placement to attendance's unit has ended
            it.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = roundTheClockDaycare.id,
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
                    childId = child.id,
                    unitId = daycare2.id,
                    groupId = null,
                    period =
                        FiniteDateRange(
                            clock.now().toLocalDate(),
                            clock.now().toLocalDate().plusDays(1),
                        ),
                )
            )
            it.insertTestChildAttendance(
                childId = child.id,
                unitId = roundTheClockDaycare.id,
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
