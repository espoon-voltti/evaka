// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.application.PendingDecisionEmailService
import fi.espoo.evaka.application.cancelOutdatedSentTransferApplications
import fi.espoo.evaka.application.removeOldDrafts
import fi.espoo.evaka.assistance.endAssistanceFactorsWhichBelongToPastPlacements
import fi.espoo.evaka.assistanceneed.decision.endActiveDaycareAssistanceDecisions
import fi.espoo.evaka.assistanceneed.preschooldecision.endActivePreschoolAssistanceDecisions
import fi.espoo.evaka.assistanceneed.vouchercoefficient.endOutdatedAssistanceNeedVoucherCoefficients
import fi.espoo.evaka.attachment.AttachmentService
import fi.espoo.evaka.attendance.addMissingStaffAttendanceDepartures
import fi.espoo.evaka.calendarevent.CalendarEventNotificationService
import fi.espoo.evaka.document.childdocument.ChildDocumentService
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.invoicing.service.NewCustomerIncomeNotification
import fi.espoo.evaka.invoicing.service.OutdatedIncomeNotifications
import fi.espoo.evaka.jamix.JamixService
import fi.espoo.evaka.koski.KoskiUpdateService
import fi.espoo.evaka.note.child.daily.deleteExpiredNotes
import fi.espoo.evaka.pis.cleanUpInactivePeople
import fi.espoo.evaka.pis.deactivateInactiveEmployees
import fi.espoo.evaka.reports.freezeVoucherValueReportRows
import fi.espoo.evaka.reservations.MissingHolidayReservationsReminders
import fi.espoo.evaka.reservations.MissingReservationsReminders
import fi.espoo.evaka.sficlient.SfiMessagesClient
import fi.espoo.evaka.shared.async.removeOldAsyncJobs
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.runSanityChecks
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.varda.new.VardaUpdateServiceNew
import fi.espoo.evaka.varda.old.VardaResetService
import fi.espoo.evaka.varda.old.VardaUpdateService
import java.time.LocalTime
import mu.KotlinLogging
import org.springframework.stereotype.Component

enum class ScheduledJob(
    val fn: (ScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    CancelOutdatedTransferApplications(
        ScheduledJobs::cancelOutdatedTransferApplications,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(0, 35))),
    ),
    CompleteChildDocumentsWithExpiredTemplate(
        ScheduledJobs::completeChildDocumentsWithExpiredTemplate,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(0, 45))),
    ),
    DvvUpdate(
        ScheduledJobs::dvvUpdate,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(4, 0))),
    ),
    EndAssistanceFactorsWhichBelongToPastPlacements(
        ScheduledJobs::endAssistanceFactorsWhichBelongToPastPlacements,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
    EndOutdatedAssistanceNeedVoucherValueCoefficients(
        ScheduledJobs::endOutdatedAssistanceNeedVoucherValueCoefficients,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
    EndActiveDaycareAssistanceDecisions(
        ScheduledJobs::endActiveDaycareAssistanceDecisions,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
    EndActivePreschoolAssistanceDecisions(
        ScheduledJobs::endActivePreschoolAssistanceDecisions,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(1, 0))),
    ),
    EndOfDayAttendanceUpkeep(
        ScheduledJobs::endOfDayAttendanceUpkeep,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(0, 0))),
    ),
    EndOfDayStaffAttendanceUpkeep(
        ScheduledJobs::endOfDayStaffAttendanceUpkeep,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(0, 0))),
    ),
    EndOfDayReservationUpkeep(
        ScheduledJobs::endOfDayReservationUpkeep,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(0, 0))),
    ),
    FreezeVoucherValueReports(
        ScheduledJobs::freezeVoucherValueReports,
        ScheduledJobSettings(
            enabled = true,
            schedule = JobSchedule.cron("0 0 0 25 * ?"), // Monthly on 25th
        ),
    ),
    GenerateFinanceDecisions(
        ScheduledJobs::generateFinanceDecisions,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(2, 0))),
    ),
    KoskiUpdate(
        ScheduledJobs::koskiUpdate,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.daily(LocalTime.of(0, 0)),
            retryCount = 1,
        ),
    ),
    RemoveOldAsyncJobs(
        ScheduledJobs::removeOldAsyncJobs,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(3, 0))),
    ),
    RemoveOldDaycareDailyNotes(
        ScheduledJobs::removeExpiredNotes,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(6, 30))),
    ),
    RemoveOldDraftApplications(
        ScheduledJobs::removeOldDraftApplications,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(0, 30))),
    ),
    SendJamixOrders(
        ScheduledJobs::sendJamixOrders,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 25 2 * * 2"), // tue @ 2:25
        ),
    ),
    SyncJamixDiets(
        ScheduledJobs::syncJamixDiets,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.cron("0 */10 7-18 * * *")),
    ),
    SendPendingDecisionReminderEmails(
        ScheduledJobs::sendPendingDecisionReminderEmails,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(7, 0))),
    ),
    VardaUpdate(
        ScheduledJobs::vardaUpdate,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 0 23 * * 2,3,6,7"), // tue, wed, sat, sun @ 23 pm
            retryCount = 1,
        ),
    ),
    VardaReset(
        ScheduledJobs::vardaReset,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 0 23 * * 1,4"), // mon, thu @ 23 pm
            retryCount = 1,
        ),
    ),
    InactivePeopleCleanup(
        ScheduledJobs::inactivePeopleCleanup,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.daily(LocalTime.of(3, 30)),
            retryCount = 1,
        ),
    ),
    InactiveEmployeesRoleReset(
        ScheduledJobs::inactiveEmployeesRoleReset,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(3, 15))),
    ),
    SendMissingReservationReminders(
        ScheduledJobs::sendMissingReservationReminders,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 0 18 * * 0"), // Sunday 18:00
        ),
    ),
    SendMissingHolidayReservationReminders(
        ScheduledJobs::sendMissingHolidayReservationReminders,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(2, 45))),
    ),
    SendOutdatedIncomeNotifications(
        ScheduledJobs::sendOutdatedIncomeNotifications,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.daily(LocalTime.of(6, 45))),
    ),
    SendNewCustomerIncomeNotification(
        ScheduledJobs::sendNewCustomerIncomeNotifications,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 45 6 1 * *"), // first day of month, 6:45
        ),
    ),
    SendCalendarEventDigests(
        ScheduledJobs::sendCalendarEventDigests,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(18, 0))),
    ),
    ScheduleDiscussionSurveyDigests(
        ScheduledJobs::scheduleDiscussionSurveyDigests,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(18, 0))),
    ),
    ScheduleDiscussionTimeReservationReminders(
        ScheduledJobs::scheduleDiscussionTimeReservationReminders,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(19, 0))),
    ),
    ScheduleOrphanAttachmentDeletion(
        ScheduledJobs::scheduleOrphanAttachmentDeletion,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(0, 0))),
    ),
    DatabaseSanityChecks(
        ScheduledJobs::databaseSanityChecks,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(3, 45))),
    ),
    /**
     * Rotates the SFI messages REST password.
     *
     * If SFI messages is disabled or is still using the old SOAP interface, this job can be safely
     * enabled but does nothing
     */
    RotateSfiMessagesPassword(
        ScheduledJobs::rotateSfiMessagesPassword,
        ScheduledJobSettings(
            enabled = true,
            schedule = JobSchedule.cron("0 0 0 1 * *"), // first day of month
        ),
    ),
}

private val logger = KotlinLogging.logger {}

@Component
class ScheduledJobs(
    // private val vardaService: VardaService, // Use this once varda fixes MA003 retry glitch
    private val vardaUpdateService: VardaUpdateService,
    private val vardaResetService: VardaResetService,
    private val vardaUpdateServiceNew: VardaUpdateServiceNew,
    private val vardaEnv: VardaEnv,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService,
    private val pendingDecisionEmailService: PendingDecisionEmailService,
    private val koskiUpdateService: KoskiUpdateService,
    private val missingReservationsReminders: MissingReservationsReminders,
    private val missingHolidayReservationsReminders: MissingHolidayReservationsReminders,
    private val outdatedIncomeNotifications: OutdatedIncomeNotifications,
    private val newCustomerIncomeNotification: NewCustomerIncomeNotification,
    private val calendarEventNotificationService: CalendarEventNotificationService,
    private val financeDecisionGenerator: FinanceDecisionGenerator,
    private val childDocumentService: ChildDocumentService,
    private val attachmentService: AttachmentService,
    private val jamixService: JamixService,
    private val sfiMessagesClient: SfiMessagesClient?,
    env: ScheduledJobsEnv<ScheduledJob>,
) : JobSchedule {
    override val jobs: List<ScheduledJobDefinition> =
        env.jobs.map {
            ScheduledJobDefinition(it.key, it.value) { db, clock -> it.key.fn(this, db, clock) }
        }

    fun endAssistanceFactorsWhichBelongToPastPlacements(
        db: Database.Connection,
        clock: EvakaClock,
    ) {
        db.transaction { tx -> tx.endAssistanceFactorsWhichBelongToPastPlacements(clock.today()) }
    }

    fun endOutdatedAssistanceNeedVoucherValueCoefficients(
        db: Database.Connection,
        clock: EvakaClock,
    ) {
        db.transaction { tx ->
            tx.endOutdatedAssistanceNeedVoucherCoefficients(
                user = AuthenticatedUser.SystemInternalUser,
                now = clock.now(),
            )
        }
    }

    fun endActiveDaycareAssistanceDecisions(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx -> tx.endActiveDaycareAssistanceDecisions(clock.today()) }
    }

    fun endActivePreschoolAssistanceDecisions(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx -> tx.endActivePreschoolAssistanceDecisions(clock.today()) }
    }

    fun endOfDayAttendanceUpkeep(db: Database.Connection, clock: EvakaClock) {
        val today = clock.today()
        db.transaction {
            it.createUpdate {
                    sql(
                        """
WITH attendances_to_end AS (
    SELECT ca.id
    FROM child_attendance ca
    JOIN daycare u ON u.id = ca.unit_id
    LEFT JOIN placement p ON p.child_id = ca.child_id AND daterange(p.start_date, p.end_date, '[]') @> ${bind(today)}
    LEFT JOIN service_need sn ON sn.placement_id = p.id AND daterange(sn.start_date, sn.end_date, '[]') @> ${bind(today)}
    LEFT JOIN backup_care bc ON ca.child_id = bc.child_id AND daterange(bc.start_date, bc.end_date, '[]') @> ${bind(today)}
    WHERE ca.end_time IS NULL AND (
        -- Unit is not open through midnight (checking until midnight but in practice no unit closes at midnight)
        u.shift_care_operation_times[extract(isodow FROM ca.date)].end IS DISTINCT FROM '23:59'::time OR
        -- Child does not have shift care in service need
        coalesce(sn.shift_care = 'NONE', FALSE) OR
        -- No (backup) placement to this unit anymore, as of today
        (p.unit_id IS DISTINCT FROM u.id AND bc.unit_id IS DISTINCT FROM u.id)
    )
)
UPDATE child_attendance SET end_time = '23:59'::time
WHERE id IN (SELECT id FROM attendances_to_end)
"""
                    )
                }
                .execute()
        }
    }

    fun endOfDayStaffAttendanceUpkeep(db: Database.Connection, clock: EvakaClock) {
        db.transaction { it.addMissingStaffAttendanceDepartures(clock.now()) }
    }

    fun endOfDayReservationUpkeep(db: Database.Connection, clock: EvakaClock) {
        db.transaction {
            it.createUpdate {
                    sql(
                        """
                        DELETE FROM attendance_reservation ar
                        WHERE NOT EXISTS (
                            SELECT 1 FROM placement p
                            WHERE p.child_id = ar.child_id
                            AND ar.date BETWEEN p.start_date AND p.end_date
                        )
                        """
                    )
                }
                .execute()
        }
    }

    fun dvvUpdate(db: Database.Connection, clock: EvakaClock) {
        dvvModificationsBatchRefreshService.scheduleBatch(db, clock)
    }

    fun koskiUpdate(db: Database.Connection, clock: EvakaClock) {
        koskiUpdateService.scheduleKoskiUploads(db, clock)
    }

    fun vardaUpdate(db: Database.Connection, clock: EvakaClock) {
        if (vardaEnv.newIntegrationEnabled) {
            vardaUpdateServiceNew.updateUnits(db, clock)
            vardaUpdateServiceNew.planChildrenUpdate(
                db,
                clock,
                vardaEnv.newIntegrationMigrationSpeed,
            )
        } else {
            vardaUpdateService.updateUnits(db, clock)
        }

        // Run old integration in addition to the new one. The old integration can be removed when
        // all children have been migrated to the new integration.
        vardaUpdateService.planVardaChildrenUpdate(db, clock)
    }

    fun vardaReset(db: Database.Connection, clock: EvakaClock) {
        vardaResetService.planVardaReset(
            db,
            clock,
            addNewChildren = !vardaEnv.newIntegrationEnabled,
        )
    }

    fun removeOldDraftApplications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { it.removeOldDrafts(clock) }
    }

    fun cancelOutdatedTransferApplications(db: Database.Connection, clock: EvakaClock) {
        val canceledApplications =
            db.transaction {
                val applicationIds = it.cancelOutdatedSentTransferApplications(clock)
                applicationIds
            }
        logger.info {
            "Canceled ${canceledApplications.size} outdated transfer applications (ids: ${canceledApplications.joinToString(", ")})"
        }
    }

    fun sendJamixOrders(db: Database.Connection, clock: EvakaClock) {
        jamixService.planOrders(db, clock)
    }

    fun syncJamixDiets(db: Database.Connection, clock: EvakaClock) {
        jamixService.planDietSync(db, clock)
    }

    fun sendPendingDecisionReminderEmails(db: Database.Connection, clock: EvakaClock) {
        pendingDecisionEmailService.scheduleSendPendingDecisionsEmails(db, clock)
    }

    fun freezeVoucherValueReports(db: Database.Connection, clock: EvakaClock) {
        val now = clock.now()
        db.transaction { freezeVoucherValueReportRows(it, now.year, now.month.value, now) }
    }

    fun generateFinanceDecisions(db: Database.Connection, clock: EvakaClock) {
        db.transaction { financeDecisionGenerator.scheduleBatchGeneration(it) }
    }

    fun removeExpiredNotes(db: Database.Connection, clock: EvakaClock) {
        db.transaction { it.deleteExpiredNotes(clock.now()) }
    }

    fun removeOldAsyncJobs(db: Database.Connection, clock: EvakaClock) {
        db.removeOldAsyncJobs(clock.now())
    }

    fun inactivePeopleCleanup(db: Database.Connection, clock: EvakaClock) {
        db.transaction { cleanUpInactivePeople(it, clock.today()) }
    }

    fun inactiveEmployeesRoleReset(db: Database.Connection, clock: EvakaClock) {
        db.transaction {
            val ids = it.deactivateInactiveEmployees(clock.now())
            logger.info { "Roles cleared for ${ids.size} inactive employees: $ids" }
        }
    }

    fun sendMissingReservationReminders(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = missingReservationsReminders.scheduleReminders(tx, clock)
            logger.info("Scheduled $count reminders about missing reservations")
        }
    }

    fun sendMissingHolidayReservationReminders(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = missingHolidayReservationsReminders.scheduleReminders(tx, clock)
            logger.info("Scheduled $count reminders about missing holiday reservations")
        }
    }

    fun sendOutdatedIncomeNotifications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = outdatedIncomeNotifications.scheduleNotifications(tx, clock)
            logger.info("Scheduled $count notifications about outdated income")
        }
    }

    fun sendNewCustomerIncomeNotifications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = newCustomerIncomeNotification.scheduleNotifications(tx, clock)
            logger.info("Scheduled $count notifications for new customer about income")
        }
    }

    fun completeChildDocumentsWithExpiredTemplate(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            childDocumentService.completeAndPublishChildDocumentsAtEndOfTerm(tx, clock.now())
        }
    }

    fun sendCalendarEventDigests(db: Database.Connection, clock: EvakaClock) {
        calendarEventNotificationService.scheduleCalendarEventDigestEmails(db, clock.now())
    }

    fun scheduleDiscussionSurveyDigests(db: Database.Connection, clock: EvakaClock) {
        calendarEventNotificationService.scheduleDiscussionSurveyDigests(db, clock.now())
    }

    fun scheduleDiscussionTimeReservationReminders(db: Database.Connection, clock: EvakaClock) {
        calendarEventNotificationService.scheduleDiscussionTimeReminders(db, clock.now())
    }

    fun scheduleOrphanAttachmentDeletion(db: Database.Connection, clock: EvakaClock) =
        db.transaction { attachmentService.scheduleOrphanAttachmentDeletion(it, clock) }

    fun databaseSanityChecks(db: Database.Connection, clock: EvakaClock) =
        db.transaction { runSanityChecks(it, clock) }

    fun rotateSfiMessagesPassword(db: Database.Connection, clock: EvakaClock) =
        sfiMessagesClient?.rotatePassword()
}
