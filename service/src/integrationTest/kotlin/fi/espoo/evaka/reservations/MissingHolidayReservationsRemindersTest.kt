// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.pis.service.blockGuardian
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevFosterParent
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevHolidayPeriod
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevReservation
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.job.ScheduledJobs
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
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
    private lateinit var guardian: PersonId
    private lateinit var child: ChildId
    private lateinit var daycareId: DaycareId
    private lateinit var areaId: AreaId
    private val employeeId = EmployeeId(UUID.randomUUID())
    private val admin = AuthenticatedUser.Employee(employeeId, setOf(UserRole.ADMIN))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            guardian = tx.insert(DevPerson(email = guardianEmail), DevPersonType.ADULT)
            areaId = tx.insert(DevCareArea())
            daycareId =
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                    )
                )
            tx.insertServiceNeedOption(snDefaultDaycare)
            child = tx.insert(DevPerson(), DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian, childId = child))
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
                    childId = child,
                    unitId = daycareId,
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
                    childId = child,
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
                    childId = child,
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
                        areaId = areaId,
                        // enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                        providerType = ProviderType.MUNICIPAL,
                    )
                )

            it.insert(
                DevPlacement(
                    childId = child,
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
                        areaId = areaId,
                        enabledPilotFeatures = setOf(PilotFeature.RESERVATIONS),
                        providerType = ProviderType.MUNICIPAL,
                    )
                )

            it.insert(
                DevPlacement(
                    childId = child,
                    unitId = voucherDaycare,
                    startDate = holidayPeriod.start,
                    endDate = holidayPeriod.end,
                    type = PlacementType.DAYCARE,
                )
            )

            it.insert(DevEmployee(id = employeeId))

            val fosterParentId =
                it.insert(DevPerson(email = "fosterparent@test.com"), DevPersonType.ADULT)
            it.insert(
                DevFosterParent(
                    childId = child,
                    parentId = fosterParentId,
                    validDuring = DateRange(clockToday.today(), clockToday.today()),
                    createdAt = clockToday.now(),
                    createdBy = admin.evakaUserId,
                )
            )
            it.blockGuardian(childId = child, guardianId = guardian)
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
