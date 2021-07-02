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
import fi.espoo.evaka.messaging.daycarydailynote.deleteExpiredDaycareDailyNotes
import fi.espoo.evaka.placement.deletePlacementPlans
import fi.espoo.evaka.reports.freezeVoucherValueReportRows
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.removeOldAsyncJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.utils.europeHelsinki
import fi.espoo.evaka.varda.VardaUpdateService
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate

enum class ScheduledJob(val fn: (ScheduledJobs, Database.Connection) -> Unit) {
    CancelOutdatedTransferApplications(ScheduledJobs::cancelOutdatedTransferApplications),
    DvvUpdate(ScheduledJobs::dvvUpdate),
    EndOfDayAttendanceUpkeep(ScheduledJobs::endOfDayAttendanceUpkeep),
    EndOutdatedVoucherValueDecisions(ScheduledJobs::endOutdatedVoucherValueDecisions),
    FreezeVoucherValueReports(ScheduledJobs::freezeVoucherValueReports),
    KoskiUpdate(ScheduledJobs::koskiUpdate),
    RemoveOldAsyncJobs(ScheduledJobs::removeOldAsyncJobs),
    RemoveOldDaycareDailyNotes(ScheduledJobs::removeOldDaycareDailyNotes),
    RemoveOldDraftApplications(ScheduledJobs::removeOldDraftApplications),
    SendPendingDecisionReminderEmails(ScheduledJobs::sendPendingDecisionReminderEmails),
    VardaUpdate(ScheduledJobs::vardaUpdate),
}

private val logger = KotlinLogging.logger { }

@Component
class ScheduledJobs(
    private val vardaUpdateService: VardaUpdateService,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService,
    private val attachmentsController: AttachmentsController,
    private val pendingDecisionEmailService: PendingDecisionEmailService,
    private val voucherValueDecisionService: VoucherValueDecisionService,
    private val koskiUpdateService: KoskiUpdateService,
    asyncJobRunner: AsyncJobRunner
) {

    init {
        asyncJobRunner.runScheduledJob = { db, msg ->
            val logMeta = mapOf("jobName" to msg.job.name)
            logger.info(logMeta) { "Running scheduled job ${msg.job.name}" }
            db.connect { msg.job.fn(this, it) }
        }
    }

    fun endOfDayAttendanceUpkeep(db: Database.Connection) {
        db.transaction {
            it.createUpdate(
                // language=SQL
                """
                    UPDATE child_attendance ca
                    SET departed = ((arrived AT TIME ZONE 'Europe/Helsinki')::date + time '23:59') AT TIME ZONE 'Europe/Helsinki'
                    WHERE departed IS NULL
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
        vardaUpdateService.scheduleVardaUpdate(db)
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
                Instant.now()
            )
        }
    }

    fun endOutdatedVoucherValueDecisions(db: Database.Connection) {
        val now = LocalDate.now()
        db.transaction { voucherValueDecisionService.endDecisionsWithEndedPlacements(it, now) }
    }

    fun removeOldDaycareDailyNotes(db: Database.Connection) {
        db.transaction { it.deleteExpiredDaycareDailyNotes(Instant.now()) }
    }

    fun removeOldAsyncJobs(db: Database.Connection) {
        val now = HelsinkiDateTime.now()
        db.removeOldAsyncJobs(now)
    }
}
