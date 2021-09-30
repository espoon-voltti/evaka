// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testRoundTheClockDaycare
import org.jdbi.v3.core.kotlin.mapTo
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

        val attendances = db.read {
            it.createQuery(
                // language=sql
                """
                    SELECT *
                    FROM child_attendance
                """.trimIndent()
            ).mapTo<ChildAttendance>().list()
        }

        assertEquals(1, attendances.size)
        val expectedDeparted = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(23, 59))
        assertEquals(expectedDeparted, attendances.first().departed)
    }

    @Test
    fun `null departures are not set in round the clock units`() {
        val arrived = HelsinkiDateTime.of(LocalDate.of(2020, 10, 25), LocalTime.of(9, 0))

        db.transaction {
            it.insertTestChildAttendance(
                childId = testChild_1.id,
                unitId = testRoundTheClockDaycare.id!!,
                arrived = arrived,
                departed = null
            )
        }

        scheduledJobs.endOfDayAttendanceUpkeep(db)

        val attendances = db.read {
            it.createQuery("SELECT * FROM child_attendance").mapTo<ChildAttendance>().list()
        }

        assertEquals(1, attendances.size)
        assertEquals(null, attendances.first().departed)
    }
}
