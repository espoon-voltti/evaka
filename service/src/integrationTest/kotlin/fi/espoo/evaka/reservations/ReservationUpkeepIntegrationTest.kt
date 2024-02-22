// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.testAdult_1
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testDaycare
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ReservationUpkeepIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx -> tx.insertGeneralTestFixtures() }
    }

    @Test
    fun `it works`() {
        val keepId =
            db.transaction { tx ->
                tx.insertTestPlacement(
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = LocalDate.of(2019, 1, 1),
                    endDate = LocalDate.of(2019, 12, 31)
                )

                // Before placement starts
                tx.insert(
                    DevReservation(
                        childId = testChild_1.id,
                        date = LocalDate.of(2018, 12, 31),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testAdult_1.id.raw)
                    )
                )
                // After placement ends
                tx.insert(
                    DevReservation(
                        childId = testChild_1.id,
                        date = LocalDate.of(2020, 1, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testAdult_1.id.raw)
                    )
                )
                // After placement ends, has no times
                tx.insert(
                    DevReservation(
                        childId = testChild_1.id,
                        date = LocalDate.of(2020, 1, 2),
                        startTime = null,
                        endTime = null,
                        createdBy = EvakaUserId(testAdult_1.id.raw)
                    )
                )
                // No placement at all
                tx.insert(
                    DevReservation(
                        childId = testChild_2.id,
                        date = LocalDate.of(2019, 1, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testAdult_1.id.raw)
                    )
                )

                // Valid - will be kept
                tx.insert(
                    DevReservation(
                        childId = testChild_1.id,
                        date = LocalDate.of(2019, 1, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = EvakaUserId(testAdult_1.id.raw)
                    )
                )
            }

        scheduledJobs.endOfDayReservationUpkeep(db, RealEvakaClock())

        val reservations =
            db.read { tx ->
                @Suppress("DEPRECATION")
                tx.createQuery("""SELECT id FROM attendance_reservation""")
                    .toSet<AttendanceReservationId>()
            }

        assertEquals(setOf(keepId), reservations)
    }
}
