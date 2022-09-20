// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.job

import fi.espoo.evaka.application.PendingDecisionEmailService
import fi.espoo.evaka.application.cancelOutdatedSentTransferApplications
import fi.espoo.evaka.application.removeOldDrafts
import fi.espoo.evaka.attachment.AttachmentsController
import fi.espoo.evaka.attendance.addMissingStaffAttendanceDepartures
import fi.espoo.evaka.dvv.DvvModificationsBatchRefreshService
import fi.espoo.evaka.koski.KoskiSearchParams
import fi.espoo.evaka.koski.KoskiUpdateService
import fi.espoo.evaka.note.child.daily.deleteExpiredNotes
import fi.espoo.evaka.pis.cleanUpInactivePeople
import fi.espoo.evaka.pis.clearRolesForInactiveEmployees
import fi.espoo.evaka.reports.freezeVoucherValueReportRows
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.removeOldAsyncJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.varda.VardaResetService
import fi.espoo.evaka.varda.VardaUpdateService
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.springframework.stereotype.Component

enum class ScheduledJob(val fn: (ScheduledJobs, Database.Connection, EvakaClock) -> Unit) {
    CancelOutdatedTransferApplications(ScheduledJobs::cancelOutdatedTransferApplications),
    DvvUpdate(ScheduledJobs::dvvUpdate),
    EndOfDayAttendanceUpkeep(ScheduledJobs::endOfDayAttendanceUpkeep),
    EndOfDayStaffAttendanceUpkeep(ScheduledJobs::endOfDayStaffAttendanceUpkeep),
    EndOfDayReservationUpkeep(ScheduledJobs::endOfDayReservationUpkeep),
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

private val logger = KotlinLogging.logger {}

@Component
class ScheduledJobs(
    private val vardaUpdateService: VardaUpdateService,
    private val vardaResetService: VardaResetService,
    private val dvvModificationsBatchRefreshService: DvvModificationsBatchRefreshService,
    private val attachmentsController: AttachmentsController,
    private val pendingDecisionEmailService: PendingDecisionEmailService,
    private val koskiUpdateService: KoskiUpdateService,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {

    init {
        asyncJobRunner.registerHandler { db, clock: EvakaClock, msg: AsyncJob.RunScheduledJob ->
            val logMeta = mapOf("jobName" to msg.job.name)
            logger.info(logMeta) { "Running scheduled job ${msg.job.name}" }
            msg.job.fn(this, db, clock)
        }
    }

    fun endOfDayAttendanceUpkeep(db: Database.Connection, clock: EvakaClock) {
        db.transaction {
            it.createUpdate(
                    // language=SQL
                    """
WITH attendances_to_end AS (
    SELECT ca.id
    FROM child_attendance ca
    JOIN daycare u ON u.id = ca.unit_id
    WHERE ca.end_time IS NULL AND (
        NOT u.round_the_clock OR
        -- No placement to this unit anymore, as of today
        NOT EXISTS (
            SELECT 1 FROM placement p
            LEFT JOIN backup_care bc ON ca.child_id = bc.child_id AND :today BETWEEN bc.start_date AND bc.end_date
            WHERE
                p.child_id = ca.child_id AND
                (p.unit_id = ca.unit_id OR bc.unit_id = ca.unit_id) AND
                :today BETWEEN p.start_date AND p.end_date
        )
    )
)
UPDATE child_attendance SET end_time = '23:59'::time
WHERE id IN (SELECT id FROM attendances_to_end)
                """.trimIndent(
                    )
                )
                .bind("today", clock.today())
                .execute()
        }
    }

    fun endOfDayStaffAttendanceUpkeep(db: Database.Connection, clock: EvakaClock) {
        db.transaction { it.addMissingStaffAttendanceDepartures(clock.now()) }
    }

    fun endOfDayReservationUpkeep(db: Database.Connection, clock: EvakaClock) {
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
                """.trimIndent(
                    )
                )
                .execute()
        }
    }

    fun dvvUpdate(db: Database.Connection, clock: EvakaClock) {
        dvvModificationsBatchRefreshService.scheduleBatch(db, clock)
    }

    fun koskiUpdate(db: Database.Connection, clock: EvakaClock) {
        koskiUpdateService.scheduleKoskiUploads(db, clock, KoskiSearchParams())
    }

    fun vardaUpdate(db: Database.Connection, clock: EvakaClock) {
        vardaUpdateService.startVardaUpdate(db, clock)
    }

    fun vardaReset(db: Database.Connection, clock: EvakaClock) {
        vardaResetService.planVardaReset(db, clock, addNewChildren = true)
    }

    fun removeOldDraftApplications(db: Database.Connection, clock: EvakaClock) {
        db.transaction { it.removeOldDrafts(clock, attachmentsController::deleteAttachment) }
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

    fun sendPendingDecisionReminderEmails(db: Database.Connection, clock: EvakaClock) {
        pendingDecisionEmailService.scheduleSendPendingDecisionsEmails(db, clock)
    }

    fun freezeVoucherValueReports(db: Database.Connection, clock: EvakaClock) {
        val now = clock.now()
        db.transaction { freezeVoucherValueReportRows(it, now.year, now.month.value, now) }
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
            val ids = it.clearRolesForInactiveEmployees(clock.now())
            logger.info { "Roles cleared for ${ids.size} inactive employees: $ids" }
        }
    }
}
