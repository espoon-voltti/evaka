// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.application.PendingDecisionEmailService
import fi.espoo.evaka.application.cancelOutdatedTransferApplications
import fi.espoo.evaka.application.removeOldDrafts
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.decision.clearDecisionDrafts
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.invoicing.service.VoucherValueDecisionService
import fi.espoo.evaka.koski.KoskiSearchParams
import fi.espoo.evaka.koski.KoskiUpdateService
import fi.espoo.evaka.note.child.daily.deleteExpiredNotes
import fi.espoo.evaka.pis.cleanUpInactivePeople
import fi.espoo.evaka.pis.clearRolesForInactiveEmployees
import fi.espoo.evaka.placement.deletePlacementPlans
import fi.espoo.evaka.reports.freezeVoucherValueReportRows
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.removeOldAsyncJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.europeHelsinki
import fi.espoo.evaka.varda.VardaResetService
import fi.espoo.evaka.varda.VardaUpdateService
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

enum class ScheduledJob(val fn: (ScheduledJobs, Database.Connection) -> Unit) {
    CancelOutdatedTransferApplications(ScheduledJobs::cancelOutdatedTransferApplications),
    DvvUpdate(ScheduledJobs::dvvUpdate),
    EndOfDayAttendanceUpkeep(ScheduledJobs::endOfDayAttendanceUpkeep),
    EndOfDayStaffAttendanceUpkeep(ScheduledJobs::endOfDayStaffAttendanceUpkeep),
    EndOfDayReservationUpkeep(ScheduledJobs::endOfDayReservationUpkeep),
    EndOutdatedVoucherValueDecisions(ScheduledJobs::endOutdatedVoucherValueDecisions),
    FreezeVoucherValueReports(ScheduledJobs::freezeVoucherValueReports),
    KoskiUpdate(ScheduledJobs::koskiUpdate),
    RemoveOldAsyncJobs(ScheduledJobs::removeOldAsyncJobs),
    RemoveOldDaycareDailyNotes(ScheduledJobs::removeExpiredNotes),
    RemoveOldDraftApplications(ScheduledJobs::removeOldDraftApplications),
    SendPendingDecisionReminderEmails(ScheduledJobs::sendPendingDecisionReminderEmails),
    VardaUpdate(ScheduledJobs::vardaUpdate),
    VardaReset(ScheduledJobs::vardaReset),
    InactivePeopleCleanup(ScheduledJobs::inactivePeopleCleanup),
    InactiveEmployeesRoleReset(ScheduledJobs::inactiveEmployeesRoleReset)
}

private val logger = KotlinLogging.logger { }

@Component
class ScheduledJobs(
    private val vardaUpdateService: VardaUpdateService,
    private val vardaResetService: VardaResetService,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService,
    private val attachmentsController: AttachmentsController,
    private val pendingDecisionEmailService: PendingDecisionEmailService,
    private val voucherValueDecisionService: VoucherValueDecisionService,
    private val koskiUpdateService: KoskiUpdateService,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {

    init {
        asyncJobRunner.registerHandler { db, msg: AsyncJob.RunScheduledJob ->
            val logMeta = mapOf("jobName" to msg.job.name)
            logger.info(logMeta) { "Running scheduled job ${msg.job.name}" }
            msg.job.fn(this, db)
        }
    }

    fun endOfDayAttendanceUpkeep(db: Database.Connection) {
        db.transaction {
            it.createUpdate(
                // language=SQL
                """
UPDATE child_attendance ca
SET end_time = '23:59'::time
FROM daycare u
WHERE ca.unit_id = u.id AND NOT u.round_the_clock AND ca.end_time IS NULL
                """.trimIndent()
            ).execute()
        }
    }

    fun endOfDayStaffAttendanceUpkeep(db: Database.Connection) {
        db.transaction {
            it.createUpdate(
                // language=SQL
                """
                    UPDATE staff_attendance_realtime
                    SET departed = now()
                    WHERE departed IS NULL AND arrived + interval '1 day' < now()
                """.trimIndent()
            ).execute()
            it.createUpdate(
                // language=SQL
                """
                    UPDATE staff_attendance_external
                    SET departed = now()
                    WHERE departed IS NULL AND arrived + interval '1 day' < now()
                """.trimIndent()
            ).execute()
        }
    }

    fun endOfDayReservationUpkeep(db: Database.Connection) {
        db.transaction {
            it.createUpdate(
                // language=SQL
                """
                    DELETE FROM attendance_reservation ar
                    WHERE NOT EXISTS (
                        SELECT 1 FROM placement p
                        WHERE p.child_id = ar.child_id
                        AND ar.date BETWEEN p.start_date AND p.end_date
                    )
                """.trimIndent()
            ).execute()
        }
    }

    fun dvvUpdate(db: Database.Connection) {
        dvvModificationsBatchRefreshService.scheduleBatch(db)
    }

    fun koskiUpdate(db: Database.Connection) {
        koskiUpdateService.scheduleKoskiUploads(db, KoskiSearchParams())
    }

    fun vardaUpdate(db: Database.Connection) {
        vardaUpdateService.startVardaUpdate(db)
    }

    fun vardaReset(db: Database.Connection) {
        vardaResetService.planVardaReset(db, addNewChildren = true)
    }

    fun removeOldDraftApplications(db: Database.Connection) {
        db.transaction { it.removeOldDrafts(attachmentsController::deleteAttachment) }
    }

    fun cancelOutdatedTransferApplications(db: Database.Connection) {
        val canceledApplications = db.transaction {
            val applicationIds = it.cancelOutdatedTransferApplications()
            it.deletePlacementPlans(applicationIds)
            it.clearDecisionDrafts(applicationIds)
            applicationIds
        }
        logger.info {
            "Canceled ${canceledApplications.size} outdated transfer applications (ids: ${canceledApplications.joinToString(", ")})"
        }
    }

    fun sendPendingDecisionReminderEmails(db: Database.Connection) {
        pendingDecisionEmailService.scheduleSendPendingDecisionsEmails(db)
    }

    fun freezeVoucherValueReports(db: Database.Connection) {
        db.transaction {
            freezeVoucherValueReportRows(
                it,
                LocalDate.now(europeHelsinki).year,
                LocalDate.now(europeHelsinki).monthValue,
                HelsinkiDateTime.now()
            )
        }
    }

    fun endOutdatedVoucherValueDecisions(db: Database.Connection) {
        db.transaction { voucherValueDecisionService.endDecisionsWithEndedPlacements(it, HelsinkiDateTime.now()) }
    }

    fun removeExpiredNotes(db: Database.Connection) {
        db.transaction { it.deleteExpiredNotes(HelsinkiDateTime.now()) }
    }

    fun removeOldAsyncJobs(db: Database.Connection) {
        val now = HelsinkiDateTime.now()
        db.removeOldAsyncJobs(now)
    }

    fun inactivePeopleCleanup(db: Database.Connection) {
        db.transaction { cleanUpInactivePeople(it, LocalDate.now()) }
    }

    fun inactiveEmployeesRoleReset(db: Database.Connection) {
        db.transaction {
            val ids = it.clearRolesForInactiveEmployees(HelsinkiDateTime.now())
            logger.info { "Roles cleared for ${ids.size} inactive employees: $ids" }
        }
    }
}
