// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestGuardian
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.dev.insertTestReservation
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.upsertCitizenUser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MissingReservationsRemindersTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val guardianEmail = "guardian@example.com"
    private val operationDays: Set<Int> = setOf(1, 2, 3, 4, 5)
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
        MockEmailClient.clear()
        db.transaction { tx ->
            guardian = tx.insertTestPerson(DevPerson(email = guardianEmail))
            tx.upsertCitizenUser(guardian)
            val areaId = tx.insertTestCareArea(DevCareArea())
            val daycareId =
                tx.insertTestDaycare(
                    DevDaycare(
                        areaId = areaId,
                        operationDays = operationDays,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS)
                    )
                )
            child = tx.insertTestPerson(DevPerson()).also { tx.insertTestChild(DevChild(it)) }
            tx.insertTestGuardian(DevGuardian(guardianId = guardian, childId = child))
            tx.insertTestPlacement(
                DevPlacement(
                    childId = child,
                    unitId = daycareId,
                    startDate = checkedRange.start,
                    endDate = checkedRange.end
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

    private fun Database.Transaction.createReservation(date: LocalDate) =
        insertTestReservation(
            DevReservation(
                childId = child,
                date = date,
                startTime = LocalTime.of(8, 0),
                endTime = LocalTime.of(16, 0),
                createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId
            )
        )
    private fun getReminderRecipients(): List<String> {
        scheduledJobs.sendMissingReservationReminders(db, clock)
        asyncJobRunner.runPendingJobsSync(clock)
        return MockEmailClient.emails.map { it.toAddress }
    }
}
