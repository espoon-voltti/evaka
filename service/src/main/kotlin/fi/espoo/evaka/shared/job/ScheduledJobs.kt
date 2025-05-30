// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.ScheduledJobsEnv
import fi.espoo.evaka.application.PendingDecisionEmailService
import fi.espoo.evaka.application.cancelOutdatedSentTransferApplications
import fi.espoo.evaka.application.removeOldDrafts
import fi.espoo.evaka.aromi.AromiService
import fi.espoo.evaka.assistance.endAssistanceFactorsWhichBelongToPastPlacements
import fi.espoo.evaka.assistanceneed.decision.endActiveDaycareAssistanceDecisions
import fi.espoo.evaka.assistanceneed.preschooldecision.endActivePreschoolAssistanceDecisions
import fi.espoo.evaka.assistanceneed.vouchercoefficient.endOutdatedAssistanceNeedVoucherCoefficients
import fi.espoo.evaka.attachment.AttachmentService
import fi.espoo.evaka.attendance.addMissingStaffAttendanceDepartures
import fi.espoo.evaka.calendarevent.CalendarEventNotificationService
import fi.espoo.evaka.daycare.controllers.removeDaycareAclForRole
import fi.espoo.evaka.document.childdocument.ChildDocumentService
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.invoicing.service.FinanceDecisionGenerator
import fi.espoo.evaka.invoicing.service.InvoiceGenerator
import fi.espoo.evaka.invoicing.service.NewCustomerIncomeNotification
import fi.espoo.evaka.invoicing.service.OutdatedIncomeNotifications
import fi.espoo.evaka.jamix.JamixService
import fi.espoo.evaka.koski.KoskiUpdateService
import fi.espoo.evaka.messaging.upsertEmployeeMessageAccount
import fi.espoo.evaka.nekku.NekkuService
import fi.espoo.evaka.note.child.daily.deleteExpiredNotes
import fi.espoo.evaka.pis.cleanUpInactivePeople
import fi.espoo.evaka.pis.deactivateInactiveEmployees
import fi.espoo.evaka.process.migrateProcessMetadata
import fi.espoo.evaka.reports.freezeVoucherValueReportRows
import fi.espoo.evaka.reservations.MissingHolidayReservationsReminders
import fi.espoo.evaka.reservations.MissingReservationsReminders
import fi.espoo.evaka.sficlient.SfiAsyncJobs
import fi.espoo.evaka.sficlient.SfiMessagesClient
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.removeOldAsyncJobs
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.PasswordBlacklist
import fi.espoo.evaka.shared.auth.getEndedDaycareAclRows
import fi.espoo.evaka.shared.auth.upsertAclRowsFromScheduled
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.runSanityChecks
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.titania.cleanTitaniaErrors
import fi.espoo.evaka.varda.VardaUpdateService
import io.github.oshai.kotlinlogging.KotlinLogging
import java.nio.file.Path
import java.time.LocalTime
import org.springframework.stereotype.Component

enum class ScheduledJob(
    val fn: (ScheduledJobs, Database.Connection, EvakaClock) -> Unit,
    val defaultSettings: ScheduledJobSettings,
) {
    CancelOutdatedTransferApplications(
        ScheduledJobs::cancelOutdatedTransferApplications,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    CompleteChildDocumentsWithExpiredTemplate(
        ScheduledJobs::completeChildDocumentsWithExpiredTemplate,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    DvvUpdate(
        ScheduledJobs::dvvUpdate,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    EndAssistanceFactorsWhichBelongToPastPlacements(
        ScheduledJobs::endAssistanceFactorsWhichBelongToPastPlacements,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    EndOutdatedAssistanceNeedVoucherValueCoefficients(
        ScheduledJobs::endOutdatedAssistanceNeedVoucherValueCoefficients,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    EndActiveDaycareAssistanceDecisions(
        ScheduledJobs::endActiveDaycareAssistanceDecisions,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    EndActivePreschoolAssistanceDecisions(
        ScheduledJobs::endActivePreschoolAssistanceDecisions,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    EndOfDayAttendanceUpkeep(
        ScheduledJobs::endOfDayAttendanceUpkeep,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    EndOfDayStaffAttendanceUpkeep(
        ScheduledJobs::endOfDayStaffAttendanceUpkeep,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    EndOfDayReservationUpkeep(
        ScheduledJobs::endOfDayReservationUpkeep,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
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
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    KoskiUpdate(
        ScheduledJobs::koskiUpdate,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly(), retryCount = 1),
    ),
    RemoveOldAsyncJobs(
        ScheduledJobs::removeOldAsyncJobs,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    RemoveOldDaycareDailyNotes(
        ScheduledJobs::removeExpiredNotes,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.daily(LocalTime.of(6, 30))),
    ),
    RemoveOldDraftApplications(
        ScheduledJobs::removeOldDraftApplications,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
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
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.cron("0 */10 7-17 * * *")),
    ),
    SyncNekkuCustomers(
        ScheduledJobs::syncNekkuCustomers,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 */10 7-17 * * *"),
            retryCount = 1,
        ),
    ),
    SyncNekkuSpecialDiets(
        ScheduledJobs::syncNekkuSpecialDiets,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 */10 7-17 * * *"),
            retryCount = 1,
        ),
    ),
    SyncNekkuProducts(
        ScheduledJobs::syncNekkuProducts,
        // change to better schedule
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 */10 7-17 * * *"),
            retryCount = 1,
        ),
    ),
    SendNekkuOrders(
        ScheduledJobs::sendNekkuOrders,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 0 3 * * 1"), // mon @ 3:00
        ),
    ),
    SendNekkuDailyOrders(
        ScheduledJobs::sendNekkuDailyOrders,
        ScheduledJobSettings(
            enabled = false,
            schedule = JobSchedule.cron("0 30 9 * * *"), // daily 9:30 am
        ),
    ),
    SendAromiOrders(
        ScheduledJobs::sendAromiOrders,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    SendPendingDecisionReminderEmails(
        ScheduledJobs::sendPendingDecisionReminderEmails,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
    VardaUpdate(
        ScheduledJobs::vardaUpdate,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly(), retryCount = 1),
    ),
    RemoveGuardiansFromAdults(
        ScheduledJobs::removeGuardiansFromAdults,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly(), retryCount = 1),
    ),
    InactivePeopleCleanup(
        ScheduledJobs::inactivePeopleCleanup,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly(), retryCount = 1),
    ),
    InactiveEmployeesRoleReset(
        ScheduledJobs::inactiveEmployeesRoleReset,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
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
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
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
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    DatabaseSanityChecks(
        ScheduledJobs::databaseSanityChecks,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
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
    GetSfiEvents(
        ScheduledJobs::getSfiEvents,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    CleanTitaniaErrors(
        ScheduledJobs::cleanTitaniaErrors,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    GenerateReplacementInvoices(
        ScheduledJobs::generateReplacementDraftInvoices,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    ImportPasswordBlacklists(
        ScheduledJobs::importPasswordBlacklists,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    DeleteEndedAcl(
        ScheduledJobs::syncAclRows,
        ScheduledJobSettings(enabled = true, schedule = JobSchedule.nightly()),
    ),
    MigrateMetadata(
        ScheduledJobs::migrateMetadata,
        ScheduledJobSettings(enabled = false, schedule = JobSchedule.nightly()),
    ),
}

private val logger = KotlinLogging.logger {}

@Component
class ScheduledJobs(
    private val vardaUpdateService: VardaUpdateService,
    private val evakaEnv: EvakaEnv,
    private val featureConfig: FeatureConfig,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService,
    private val pendingDecisionEmailService: PendingDecisionEmailService,
    private val invoiceGenerator: InvoiceGenerator,
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
    private val nekkuService: NekkuService,
    private val aromiService: AromiService,
    private val sfiMessagesClient: SfiMessagesClient?,
    private val sfiAsyncJobs: SfiAsyncJobs,
    private val passwordBlacklist: PasswordBlacklist,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
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
        db.transaction {
            it.addMissingStaffAttendanceDepartures(
                clock.now(),
                AuthenticatedUser.SystemInternalUser.evakaUserId,
            )
        }
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
        vardaUpdateService.updateUnits(db, clock)
        vardaUpdateService.planChildrenUpdate(db, clock)
    }

    fun removeOldDraftApplications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { it.removeOldDrafts(clock) }
    }

    fun cancelOutdatedTransferApplications(db: Database.Connection, clock: EvakaClock) {
        val canceledApplications =
            db.transaction {
                val applicationIds =
                    it.cancelOutdatedSentTransferApplications(
                        clock,
                        AuthenticatedUser.SystemInternalUser.evakaUserId,
                    )
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

    fun syncNekkuCustomers(db: Database.Connection, clock: EvakaClock) {
        nekkuService.planNekkuCustomersSync(db, clock)
    }

    fun syncNekkuProducts(db: Database.Connection, clock: EvakaClock) {
        nekkuService.planNekkuProductsSync(db, clock)
    }

    fun sendNekkuOrders(db: Database.Connection, clock: EvakaClock) {
        nekkuService.planNekkuOrders(db, clock)
    }

    fun sendNekkuDailyOrders(db: Database.Connection, clock: EvakaClock) {
        nekkuService.planNekkuDailyOrders(db, clock)
    }

    fun sendAromiOrders(db: Database.Connection, clock: EvakaClock) {
        aromiService.sendOrders(db, clock)
    }

    fun syncNekkuSpecialDiets(db: Database.Connection, clock: EvakaClock) {
        nekkuService.planNekkuSpecialDietsSync(db, clock)
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

    fun removeGuardiansFromAdults(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            tx.execute {
                sql(
                    """
                DELETE FROM guardian g
                WHERE EXISTS(
                    SELECT FROM person ch 
                    WHERE g.child_id = ch.id AND ch.date_of_birth <= ${bind(clock.today().minusYears(18))}
                )
            """
                )
            }
        }
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
            logger.info { "Scheduled $count reminders about missing reservations" }
        }
    }

    fun sendMissingHolidayReservationReminders(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = missingHolidayReservationsReminders.scheduleReminders(tx, clock)
            logger.info { "Scheduled $count reminders about missing holiday reservations" }
        }
    }

    fun sendOutdatedIncomeNotifications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = outdatedIncomeNotifications.scheduleNotifications(tx, clock)
            logger.info { "Scheduled $count notifications about outdated income" }
        }
    }

    fun sendNewCustomerIncomeNotifications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { tx ->
            val count = newCustomerIncomeNotification.scheduleNotifications(tx, clock)
            logger.info { "Scheduled $count notifications for new customer about income" }
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

    fun cleanTitaniaErrors(db: Database.Connection, clock: EvakaClock) =
        db.transaction { it.cleanTitaniaErrors(clock.now()) }

    fun generateReplacementDraftInvoices(db: Database.Connection, clock: EvakaClock) =
        invoiceGenerator.generateAllReplacementDraftInvoices(db, clock.today())

    fun importPasswordBlacklists(db: Database.Connection, clock: EvakaClock) =
        evakaEnv.passwordBlacklistDirectory?.let { directory ->
            passwordBlacklist.importBlacklists(db, Path.of(directory))
        }

    fun syncAclRows(db: Database.Connection, clock: EvakaClock) =
        db.transaction { tx ->
            val now = clock.now()
            val today = now.toLocalDate()

            tx.getEndedDaycareAclRows(today).forEach {
                removeDaycareAclForRole(
                    tx,
                    asyncJobRunner,
                    now,
                    it.daycareId,
                    it.employeeId,
                    it.role,
                )
            }

            val employeeIds = tx.upsertAclRowsFromScheduled(today)
            employeeIds.forEach { tx.upsertEmployeeMessageAccount(it) }
        }

    fun getSfiEvents(db: Database.Connection, clock: EvakaClock) {
        sfiAsyncJobs.getEvents(db, clock)
    }

    fun migrateMetadata(db: Database.Connection, clock: EvakaClock) {
        migrateProcessMetadata(db, clock, featureConfig)
    }
}
