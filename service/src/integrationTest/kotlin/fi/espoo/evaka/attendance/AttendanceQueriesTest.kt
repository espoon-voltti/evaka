// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attendance

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AttendanceQueriesTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.of(12, 0, 0))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun empty() {
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertTrue(attendances.isEmpty())
    }

    @Test
    fun `ongoing attendance`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now,
                departed = null
            )
        }
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertEquals(
            mapOf(
                testChild_1.id to
                    listOf(
                        AttendanceTimes(
                            arrived = now,
                            departed = null
                        )
                    )
            ),
            attendances
        )
    }

    @Test
    fun `ended attendance`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.minusHours(1),
                departed = now
            )
        }
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertEquals(
            mapOf(
                testChild_1.id to
                    listOf(
                        AttendanceTimes(
                            arrived = now.minusHours(1),
                            departed = now
                        )
                    )
            ),
            attendances
        )
    }

    @Test
    fun `multiple children`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.minusHours(1),
                departed = now
            )
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_2.id,
                arrived = now.minusHours(2),
                departed = null
            )
        }
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertEquals(
            mapOf(
                testChild_1.id to
                    listOf(
                        AttendanceTimes(
                            arrived = now.minusHours(1),
                            departed = now
                        )
                    ),
                testChild_2.id to
                    listOf(
                        AttendanceTimes(
                            arrived = now.minusHours(2),
                            departed = null
                        )
                    )
            ),
            attendances
        )
    }

    @Test
    fun `multiple attendances per child`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.minusHours(3),
                departed = now.minusHours(2)
            )
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.minusHours(1),
                departed = now
            )
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_2.id,
                arrived = now.minusHours(4),
                departed = now.minusHours(3)
            )
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_2.id,
                arrived = now.minusHours(2),
                departed = null
            )
        }
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertEquals(
            mapOf(
                // Newest attendance is first
                testChild_1.id to
                    listOf(
                        AttendanceTimes(
                            arrived = now.minusHours(1),
                            departed = now
                        ),
                        AttendanceTimes(
                            arrived = now.minusHours(3),
                            departed = now.minusHours(2)
                        )
                    ),
                testChild_2.id to
                    listOf(
                        AttendanceTimes(
                            arrived = now.minusHours(2),
                            departed = null
                        ),
                        AttendanceTimes(
                            arrived = now.minusHours(4),
                            departed = now.minusHours(3)
                        )
                    )
            ),
            attendances
        )
    }

    @Test
    fun `ongoing attendance started yesterday`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.minusDays(1),
                departed = now.minusDays(1).atEndOfDay()
            )
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.atStartOfDay(),
                departed = null
            )
        }
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertEquals(
            mapOf(
                testChild_1.id to
                    listOf(AttendanceTimes(arrived = now.minusDays(1), departed = null))
            ),
            attendances
        )
    }

    @Test
    fun `attendance started yesterday`() {
        db.transaction { tx ->
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.minusDays(1),
                departed = now.minusDays(1).atEndOfDay()
            )
            tx.insertTestChildAttendance(
                unitId = testDaycare.id,
                childId = testChild_1.id,
                arrived = now.atStartOfDay(),
                departed = now.minusMinutes(45)
            )
        }
        val attendances = db.read { it.getUnitChildAttendances(testDaycare.id, now) }
        assertEquals(
            mapOf(
                testChild_1.id to
                    listOf(
                        AttendanceTimes(arrived = now.minusDays(1), departed = now.minusMinutes(45))
                    )
            ),
            attendances
        )
    }
}
