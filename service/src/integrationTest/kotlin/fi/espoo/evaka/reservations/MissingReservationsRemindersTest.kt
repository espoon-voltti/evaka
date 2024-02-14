// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDefaultDaycare
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            null
        )
    private lateinit var guardian: PersonId
    private lateinit var child: ChildId

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
            guardian = tx.insert(DevPerson(email = guardianEmail), DevPersonType.ADULT)
            val areaId = tx.insert(DevCareArea())
            val daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        operationTimes = operationTimes,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS)
                    )
                )
            tx.insertServiceNeedOption(snDefaultDaycare)
            child = tx.insert(DevPerson(), DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian, childId = child))
            tx.insert(
                DevPlacement(
                    childId = child,
                    unitId = daycareId,
                    startDate = checkedRange.start,
                    endDate = checkedRange.end,
                    type = PlacementType.DAYCARE
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
                tx.insertTestAbsence(
                    childId = child,
                    date = it,
                    category = AbsenceCategory.NONBILLABLE
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
            tx.createUpdate("UPDATE placement SET type = :type")
                .bind("type", placementType)
                .execute()
        }
        assertEquals(emptyList(), getReminderRecipients())
    }

    private fun Database.Transaction.createReservation(
        date: LocalDate,
        startTime: LocalTime? = LocalTime.of(8, 0),
        endTime: LocalTime? = LocalTime.of(16, 0)
    ) =
        insert(
            DevReservation(
                childId = child,
                date = date,
                startTime = startTime,
                endTime = endTime,
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId
            )
        )

    private fun getReminderRecipients(): List<String> {
        scheduledJobs.sendMissingReservationReminders(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        return MockEmailClient.emails.map { it.toAddress }
    }
}
