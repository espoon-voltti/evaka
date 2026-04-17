// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reservations

import evaka.core.FullApplicationTest
import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.daycare.domain.ProviderType
import evaka.core.emailclient.MockEmailClient
import evaka.core.pis.service.blockGuardian
import evaka.core.placement.PlacementType
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.dev.DevAbsence
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevFosterParent
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevHolidayPeriod
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.DevReservation
import evaka.core.shared.dev.insert
import evaka.core.shared.dev.insertServiceNeedOption
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.shared.job.ScheduledJobs
import evaka.core.shared.security.PilotFeature
import evaka.core.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MissingHolidayReservationsRemindersTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var scheduledJobs: ScheduledJobs
    @Autowired private lateinit var asyncJobRunner: AsyncJobRunner<AsyncJob>

    private val clockToday =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 3, 11), LocalTime.of(22, 0)))

    private val holidayPeriod: FiniteDateRange =
        FiniteDateRange(clockToday.today().plusDays(2), clockToday.today().plusDays(3))
    private val guardianEmail = "guardian@example.com"
    private val guardian = DevPerson(email = guardianEmail)
    private val area = DevCareArea()
    private val daycare =
        DevDaycare(areaId = area.id, enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS))
    private val child = DevPerson()
    private val employee = DevEmployee()

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
                DevHolidayPeriod(
                    period = holidayPeriod,
                    reservationsOpenOn = clockToday.today(),
                    reservationDeadline = clockToday.today().plusDays(2),
                )
            )
        }
    }

    @Test
    fun `Missing holiday reminder is sent 2 days before if there is a placement with a missing reservation`() {
        db.transaction {
            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycare.id,
                    startDate = holidayPeriod.start,
                    endDate = holidayPeriod.end,
                    type = PlacementType.DAYCARE,
                )
            )
        }

        assertEquals(listOf(guardianEmail), getHolidayReminderRecipients())

        db.transaction {
            it.insert(
                DevReservation(
                    childId = child.id,
                    date = holidayPeriod.start,
                    startTime = LocalTime.of(8, 0),
                    endTime = LocalTime.of(16, 0),
                    createdBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                )
            )
        }

        // 1/2 holiday days still misses a reservation / absence so a reminder is sent
        assertEquals(listOf(guardianEmail), getHolidayReminderRecipients())

        db.transaction {
            it.insert(
                DevAbsence(
                    childId = child.id,
                    absenceType = AbsenceType.PLANNED_ABSENCE,
                    date = holidayPeriod.end,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
        }

        // 2/2 holiday days has a reservation / absence so a reminder is not sent
        assertEquals(emptyList(), getHolidayReminderRecipients())
    }

    @Test
    fun `Missing holiday reminder is not sent if child is in unit with disabled reservations`() {
        db.transaction {
            val voucherDaycare =
                it.insert(
                    DevDaycare(
                        areaId = area.id,
                        // enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                        providerType = ProviderType.MUNICIPAL,
                    )
                )

            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = voucherDaycare,
                    startDate = holidayPeriod.start,
                    endDate = holidayPeriod.end,
                    type = PlacementType.DAYCARE,
                )
            )
        }

        assertEquals(emptyList(), getHolidayReminderRecipients())
    }

    @Test
    fun `Missing holiday reminder is sent to foster parent if the child guardian is blocked`() {
        db.transaction {
            val voucherDaycare =
                it.insert(
                    DevDaycare(
                        areaId = area.id,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                        providerType = ProviderType.MUNICIPAL,
                    )
                )

            it.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = voucherDaycare,
                    startDate = holidayPeriod.start,
                    endDate = holidayPeriod.end,
                    type = PlacementType.DAYCARE,
                )
            )

            it.insert(employee)

            val fosterParentId =
                it.insert(DevPerson(email = "fosterparent@test.com"), DevPersonType.ADULT)
            it.insert(
                DevFosterParent(
                    childId = child.id,
                    parentId = fosterParentId,
                    validDuring = DateRange(clockToday.today(), clockToday.today()),
                    modifiedAt = clockToday.now(),
                    modifiedBy = employee.evakaUserId,
                )
            )
            it.blockGuardian(childId = child.id, guardianId = guardian.id)
        }
        assertEquals(listOf("fosterparent@test.com"), getHolidayReminderRecipients())
    }

    private fun getHolidayReminderRecipients(): List<String> {
        scheduledJobs.sendMissingHolidayReservationReminders(db, clockToday)
        asyncJobRunner.runPendingJobsSync(clockToday)
        val emails = MockEmailClient.emails.map { it.toAddress }
        MockEmailClient.clear()
        return emails
    }
}
