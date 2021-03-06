// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.dvv.DvvModificationsRefresh
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.voltti.logging.MdcKey
import fi.espoo.voltti.logging.loggers.error
import fi.espoo.voltti.logging.loggers.info
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }

private const val threadPoolSize = 1
private const val defaultRetryCount = 24 * 60 / 5 // 24h when used with default 5 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(5)

private val noHandler = { _: Database, msg: Any -> logger.warn("No job handler configured for $msg") }

class AsyncJobRunner(
    private val jdbi: Jdbi,
    private val syncMode: Boolean = false
) : AutoCloseable {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(threadPoolSize)
    private var periodicRunner: ScheduledFuture<*>? = null
    private val runningCount: AtomicInteger = AtomicInteger(0)

    @Volatile
    var notifyDecisionCreated: (db: Database, msg: NotifyDecisionCreated) -> Unit = noHandler

    @Volatile
    var sendDecision: (db: Database, msg: SendDecision) -> Unit = noHandler

    @Volatile
    var notifyFeeDecisionApproved: (db: Database, msg: NotifyFeeDecisionApproved) -> Unit = noHandler

    @Volatile
    var notifyFeeDecisionPdfGenerated: (db: Database, msg: NotifyFeeDecisionPdfGenerated) -> Unit = noHandler

    @Volatile
    var notifyVoucherValueDecisionApproved: (db: Database, msg: NotifyVoucherValueDecisionApproved) -> Unit = noHandler

    @Volatile
    var notifyVoucherValueDecisionPdfGenerated: (db: Database, msg: NotifyVoucherValueDecisionPdfGenerated) -> Unit =
        noHandler

    @Volatile
    var initializeFamilyFromApplication: (db: Database, msg: InitializeFamilyFromApplication) -> Unit = noHandler

    @Volatile
    var vtjRefresh: (db: Database, msg: VTJRefresh) -> Unit = noHandler

    @Volatile
    var dvvModificationsRefresh: (db: Database, msg: DvvModificationsRefresh) -> Unit = noHandler

    @Volatile
    var scheduleKoskiUploads: (db: Database, msg: ScheduleKoskiUploads) -> Unit = noHandler

    @Volatile
    var uploadToKoski: (db: Database, msg: UploadToKoski) -> Unit = noHandler

    @Volatile
    var sendApplicationEmail: (db: Database, msg: SendApplicationEmail) -> Unit = noHandler

    @Volatile
    var garbageCollectPairing: (db: Database, msg: GarbageCollectPairing) -> Unit = noHandler

    @Volatile
    var vardaUpdate: (db: Database, msg: VardaUpdate) -> Unit = noHandler

    @Volatile
    var vardaUpdateV2: (db: Database, msg: VardaUpdateV2) -> Unit = noHandler

    @Volatile
    var sendPendingDecisionEmail: (db: Database, msg: SendPendingDecisionEmail) -> Unit = noHandler

    @Volatile
    var sendMessageNotificationEmail: (db: Database, msg: SendMessageNotificationEmail) -> Unit = noHandler

    @Volatile
    var runScheduledJob: (db: Database, msg: RunScheduledJob) -> Unit = noHandler

    @Volatile
    var notifyFeeThresholdsUpdated: (db: Database, msg: NotifyFeeThresholdsUpdated) -> Unit = noHandler

    @Volatile
    var generateFinanceDecisions: (db: Database, msg: GenerateFinanceDecisions) -> Unit = noHandler

    fun plan(
        tx: Database.Transaction,
        payloads: Iterable<AsyncJobPayload>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: HelsinkiDateTime = HelsinkiDateTime.now()
    ) {
        payloads.forEach { payload ->
            tx.insertJob(
                JobParams(
                    payload = payload,
                    retryCount = retryCount,
                    retryInterval = retryInterval,
                    runAt = runAt
                )
            )
        }
    }

    fun scheduleImmediateRun(maxCount: Int = 1_000) {
        if (syncMode) {
            logger.info("Skipping scheduleImmediateRun in sync mode")
            return
        }
        executor.execute { this.runPendingJobs(maxCount) }
    }

    fun schedulePeriodicRun(pollingInterval: Duration, maxCount: Int = 1_000) {
        if (syncMode) {
            logger.info("Skipping schedulePeriodicRun in sync mode")
            return
        }
        this.periodicRunner?.cancel(false)
        if (!pollingInterval.isZero && !pollingInterval.isNegative) {
            this.periodicRunner =
                this.executor.scheduleWithFixedDelay(
                    { this.runPendingJobs(maxCount) },
                    0,
                    pollingInterval.toNanos(),
                    TimeUnit.NANOSECONDS
                )
        } else {
            this.periodicRunner = null
        }
    }

    fun runPendingJobsSync(maxCount: Int = 1_000) {
        this.executor.submit { this.runPendingJobs(maxCount) }.get()
    }

    fun getRunningCount(): Int = runningCount.get()

    fun getPendingJobCount(types: Collection<AsyncJobType> = AsyncJobType.values().toList()): Int =
        Database(jdbi).read { it.getPendingJobCount(types) }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (getRunningCount() == 0) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    private fun runPendingJobs(maxCount: Int) = Database(jdbi).connect { db ->
        var remaining = maxCount
        do {
            val job = db.transaction { it.claimJob() }
            if (job != null) {
                runPendingJob(db, job)
            }
            remaining -= 1
        } while (job != null && remaining > 0)
    }

    @Suppress("DEPRECATION")
    private fun runPendingJob(db: Database.Connection, job: ClaimedJobRef) {
        val logMeta = mapOf(
            "jobId" to job.jobId,
            "jobType" to job.jobType,
            "remainingAttempts" to job.remainingAttempts
        )
        try {
            MdcKey.TRACE_ID.set(job.jobId.toString())
            MdcKey.SPAN_ID.set(job.jobId.toString())
            runningCount.incrementAndGet()
            logger.info(logMeta) { "Running async job $job" }
            val completed = db.transaction {
                it.setLockTimeout(Duration.ofSeconds(5))
                when (job.jobType) {
                    AsyncJobType.DECISION_CREATED -> it.runJob(job, this.notifyDecisionCreated)
                    AsyncJobType.SEND_DECISION -> it.runJob(job, this.sendDecision)
                    AsyncJobType.FEE_DECISION_APPROVED -> it.runJob(job, this.notifyFeeDecisionApproved)
                    AsyncJobType.FEE_DECISION_PDF_GENERATED -> it.runJob(job, this.notifyFeeDecisionPdfGenerated)
                    AsyncJobType.VOUCHER_VALUE_DECISION_APPROVED -> it.runJob(
                        job,
                        this.notifyVoucherValueDecisionApproved
                    )
                    AsyncJobType.VOUCHER_VALUE_DECISION_PDF_GENERATED ->
                        it.runJob(job, this.notifyVoucherValueDecisionPdfGenerated)
                    AsyncJobType.INITIALIZE_FAMILY_FROM_APPLICATION -> it.runJob(
                        job,
                        this.initializeFamilyFromApplication
                    )
                    AsyncJobType.VTJ_REFRESH -> it.runJob(job, this.vtjRefresh)
                    AsyncJobType.DVV_MODIFICATIONS_REFRESH -> it.runJob(job, this.dvvModificationsRefresh)
                    AsyncJobType.UPLOAD_TO_KOSKI -> it.runJob(job, this.uploadToKoski)
                    AsyncJobType.SEND_APPLICATION_EMAIL -> it.runJob(job, this.sendApplicationEmail)
                    AsyncJobType.GARBAGE_COLLECT_PAIRING -> it.runJob(job, this.garbageCollectPairing)
                    AsyncJobType.VARDA_UPDATE -> it.runJob(job, this.vardaUpdate)
                    AsyncJobType.VARDA_UPDATE_V2 -> it.runJob(job, this.vardaUpdateV2)
                    AsyncJobType.SCHEDULE_KOSKI_UPLOADS -> it.runJob(job, this.scheduleKoskiUploads)
                    AsyncJobType.SEND_PENDING_DECISION_EMAIL -> it.runJob(job, this.sendPendingDecisionEmail)
                    AsyncJobType.SEND_UNREAD_MESSAGE_NOTIFICATION -> it.runJob(job, this.sendMessageNotificationEmail)
                    AsyncJobType.RUN_SCHEDULED_JOB -> it.runJob(job, this.runScheduledJob)
                    AsyncJobType.FEE_THRESHOLDS_UPDATED -> it.runJob(job, this.notifyFeeThresholdsUpdated)
                    AsyncJobType.GENERATE_FINANCE_DECISIONS -> it.runJob(job, this.generateFinanceDecisions)

                    // Deprecated, delete when no more jobs of this type in the database
                    AsyncJobType.PLACEMENT_PLAN_APPLIED -> it.runJob(job) { db, msg: NotifyPlacementPlanApplied ->
                        this.generateFinanceDecisions(
                            db,
                            GenerateFinanceDecisions.forChild(msg.childId, DateRange(msg.startDate, msg.endDate))
                        )
                    }
                    AsyncJobType.SERVICE_NEED_UPDATED -> it.runJob(job) { db, msg: NotifyServiceNeedUpdated ->
                        this.generateFinanceDecisions(
                            db,
                            GenerateFinanceDecisions.forChild(msg.childId, DateRange(msg.startDate, msg.endDate))
                        )
                    }
                    AsyncJobType.FAMILY_UPDATED -> it.runJob(job) { db, msg: NotifyFamilyUpdated ->
                        this.generateFinanceDecisions(
                            db,
                            GenerateFinanceDecisions.forAdult(msg.adultId, DateRange(msg.startDate, msg.endDate))
                        )
                    }
                    AsyncJobType.FEE_ALTERATION_UPDATED -> it.runJob(job) { db, msg: NotifyFeeAlterationUpdated ->
                        this.generateFinanceDecisions(
                            db,
                            GenerateFinanceDecisions.forChild(msg.personId, DateRange(msg.startDate, msg.endDate))
                        )
                    }
                    AsyncJobType.INCOME_UPDATED -> it.runJob(job) { db, msg: NotifyIncomeUpdated ->
                        this.generateFinanceDecisions(
                            db,
                            GenerateFinanceDecisions.forAdult(msg.personId, DateRange(msg.startDate, msg.endDate))
                        )
                    }
                }.exhaust()
            }
            if (completed) {
                logger.info(logMeta) { "Completed async job $job" }
            } else {
                logger.info(logMeta) { "Skipped async job $job due to contention" }
            }
        } catch (e: Throwable) {
            val exception = (e as? UndeclaredThrowableException)?.cause ?: e
            logger.error(exception, logMeta) { "Failed to run async job $job" }
        } finally {
            runningCount.decrementAndGet()
            MdcKey.SPAN_ID.unset()
            MdcKey.TRACE_ID.unset()
            MdcKey.USER_ID.unset()
        }
    }

    private inline fun <reified T : AsyncJobPayload> Database.Transaction.runJob(
        job: ClaimedJobRef,
        crossinline f: (db: Database, msg: T) -> Unit
    ): Boolean {
        val msg = startJob(job, T::class.java) ?: return false
        msg.user?.let { MdcKey.USER_ID.set(it.id.toString()) }
        f(Database(jdbi), msg)
        completeJob(job)
        return true
    }

    override fun close() {
        this.executor.shutdown()
        this.executor.awaitTermination(10, TimeUnit.SECONDS)
        this.executor.shutdownNow()
    }
}

private fun Database.Transaction.setLockTimeout(duration: Duration) = createUpdate(
    "SET LOCAL lock_timeout = <durationInMs>"
).define("durationInMs", "'${duration.toMillis()}ms'").execute()
