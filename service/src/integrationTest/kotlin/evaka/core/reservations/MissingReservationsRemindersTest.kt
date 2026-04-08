// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reservations

import evaka.core.FullApplicationTest
import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.emailclient.MockEmailClient
import evaka.core.placement.PlacementType
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevReservation
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.domain.TimeRange
import evaka.core.shared.job.ScheduledJobs
import evaka.core.shared.security.PilotFeature
import evaka.core.snDefaultDaycare
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MissingReservationsRemindersTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val guardianEmail = "guardian@example.com"
    private val operationTimes: List<TimeRange?> =
        listOf(
            TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
            TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
            TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
            TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
            TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59")),
            null,
            null,
        )
    private val guardian = DevPerson(email = guardianEmail)
    private val area = DevCareArea()
    private val daycare =
        DevDaycare(
            areaId = area.id,
            operationTimes = operationTimes,
            enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
        )
    private val child = DevPerson()

    private val checkedRange =
        FiniteDateRange(LocalDate.of(2022, 10, 31), LocalDate.of(2022, 11, 6))
    private val clock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 10, 23), LocalTime.of(21, 0)))

    init {
        assertEquals(DayOfWeek.SUNDAY, clock.today().dayOfWeek)
        assertEquals(DayOfWeek.MONDAY, checkedRange.start.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, checkedRange.end.dayOfWeek)
    }

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(area)
            tx.insert(daycare)
            tx.insertServiceNeedOption(snDefaultDaycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = child.id))
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = checkedRange.start,
                    endDate = checkedRange.end,
                    type = PlacementType.DAYCARE,
                )
            )
        }
    }

    @Test
    fun `reminder is sent when a placement exists but there are no reservations`() {
        assertEquals(listOf(guardianEmail), getReminderRecipients())
    }

    @Test
    fun `reminder is sent when only some days in range have reservations`() {
        db.transaction { it.createReservation(checkedRange.start) }
        assertEquals(listOf(guardianEmail), getReminderRecipients())
    }

    @Test
    fun `reminder is not sent when all days have reservations`() {
        db.transaction { tx -> checkedRange.dates().forEach { tx.createReservation(it) } }
        assertEquals(emptyList(), getReminderRecipients())
    }

    @Test
    fun `reminder is sent when all days have reservations without times`() {
        db.transaction { tx ->
            checkedRange.dates().forEach {
                tx.createReservation(it, startTime = null, endTime = null)
            }
        }
        assertEquals(listOf(guardianEmail), getReminderRecipients())
    }

    @Test
    fun `reminder is not sent when there are absences`() {
        db.transaction { tx ->
            checkedRange.dates().forEach {
                tx.insert(
                    DevAbsence(
                        childId = child.id,
                        date = it,
                        absenceType = AbsenceType.SICKLEAVE,
                        absenceCategory = AbsenceCategory.NONBILLABLE,
                    )
                )
            }
        }
        assertEquals(emptyList(), getReminderRecipients())
    }

    @Test
    fun `reminder is not sent when reservations are missing only from non-operational days`() {
        db.transaction { tx -> checkedRange.dates().take(5).forEach { tx.createReservation(it) } }
        assertEquals(emptyList(), getReminderRecipients())
    }

    @Test
    fun `reminder is not sent when child 's placement does not require reservations`() {
        val placementType = PlacementType.PRESCHOOL
        assertFalse(PlacementType.requiringAttendanceReservations.contains(placementType))

        db.transaction { tx ->
            tx.execute { sql("UPDATE placement SET type = ${bind(placementType)}") }
        }
        assertEquals(emptyList(), getReminderRecipients())
    }

    @Test
    fun `reminder is not sent for a far away future placement`() {
        val placementType = PlacementType.DAYCARE
        assertTrue(PlacementType.requiringAttendanceReservations.contains(placementType))

        val placementStart = clock.today().plusWeeks(4).plusDays(1)
        assertEquals(DayOfWeek.MONDAY, placementStart.dayOfWeek)

        val email = UUID.randomUUID().toString() + "@example.com"
        db.transaction { tx ->
            val guardianId = tx.insert(DevPerson(email = email), DevPersonType.ADULT)
            val childId2 = tx.insert(DevPerson(), DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardianId, childId = childId2))
            tx.insert(
                DevPlacement(
                    childId = childId2,
                    unitId = daycare.id,
                    startDate = placementStart,
                    endDate = placementStart.plusMonths(12),
                    type = placementType,
                )
            )
        }
        val recipients = getReminderRecipients()
        assertFalse(recipients.contains(email))
    }

    private fun Database.Transaction.createReservation(
        date: LocalDate,
        startTime: LocalTime? = LocalTime.of(8, 0),
        endTime: LocalTime? = LocalTime.of(16, 0),
    ) =
        insert(
            DevReservation(
                childId = child.id,
                date = date,
                startTime = startTime,
                endTime = endTime,
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
            )
        )

    private fun getReminderRecipients(): List<String> {
        scheduledJobs.sendMissingReservationReminders(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        return MockEmailClient.emails.map { it.toAddress }
    }
}
