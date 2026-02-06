// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ReservationUpkeepIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs

    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val adult = DevPerson()
    private val child1 = DevPerson()
    private val child2 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(adult, DevPersonType.ADULT)
            listOf(child1, child2).forEach { tx.insert(it, DevPersonType.CHILD) }
        }
    }

    @Test
    fun `it works`() {
        val keepId =
            db.transaction { tx ->
                tx.insert(
                    DevPlacement(
                        childId = child1.id,
                        unitId = daycare.id,
                        startDate = LocalDate.of(2019, 1, 1),
                        endDate = LocalDate.of(2019, 12, 31),
                    )
                )

                // Before placement starts
                tx.insert(
                    DevReservation(
                        childId = child1.id,
                        date = LocalDate.of(2018, 12, 31),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = adult.evakaUserId(),
                    )
                )
                // After placement ends
                tx.insert(
                    DevReservation(
                        childId = child1.id,
                        date = LocalDate.of(2020, 1, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = adult.evakaUserId(),
                    )
                )
                // After placement ends, has no times
                tx.insert(
                    DevReservation(
                        childId = child1.id,
                        date = LocalDate.of(2020, 1, 2),
                        startTime = null,
                        endTime = null,
                        createdBy = adult.evakaUserId(),
                    )
                )
                // No placement at all
                tx.insert(
                    DevReservation(
                        childId = child2.id,
                        date = LocalDate.of(2019, 1, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = adult.evakaUserId(),
                    )
                )

                // Valid - will be kept
                tx.insert(
                    DevReservation(
                        childId = child1.id,
                        date = LocalDate.of(2019, 1, 2),
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(16, 0),
                        createdBy = adult.evakaUserId(),
                    )
                )
            }

        scheduledJobs.endOfDayReservationUpkeep(db, RealEvakaClock())

        val reservations =
            db.read { tx ->
                tx.createQuery { sql("SELECT id FROM attendance_reservation") }
                    .toSet<AttendanceReservationId>()
            }

        assertEquals(setOf(keepId), reservations)
    }
}
