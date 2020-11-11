// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.dvv.DvvModificationsRefresh
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.voltti.logging.MdcKey
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
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

private val noHandler = { msg: Any -> logger.warn("No job handler configured for $msg") }

class AsyncJobRunner(
    private val jdbi: Jdbi,
    private val syncMode: Boolean = false
) : AutoCloseable {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(threadPoolSize)
    private var periodicRunner: ScheduledFuture<*>? = null
    private val runningCount: AtomicInteger = AtomicInteger(0)

    @Volatile
    var notifyPlacementPlanApplied: (msg: NotifyPlacementPlanApplied) -> Unit = noHandler

    @Volatile
    var notifyServiceNeedUpdated: (msg: NotifyServiceNeedUpdated) -> Unit = noHandler

    @Volatile
    var notifyFamilyUpdated: (msg: NotifyFamilyUpdated) -> Unit = noHandler

    @Volatile
    var notifyFeeAlterationUpdated: (msg: NotifyFeeAlterationUpdated) -> Unit = noHandler

    @Volatile
    var notifyIncomeUpdated: (msg: NotifyIncomeUpdated) -> Unit = noHandler

    @Volatile
    var notifyDecisionCreated: (msg: NotifyDecisionCreated) -> Unit = noHandler

    @Volatile
    var sendDecision: (msg: SendDecision) -> Unit = noHandler

    @Volatile
    var notifyFeeDecisionApproved: (msg: NotifyFeeDecisionApproved) -> Unit = noHandler

    @Volatile
    var notifyFeeDecisionPdfGenerated: (msg: NotifyFeeDecisionPdfGenerated) -> Unit = noHandler

    @Volatile
    var notifyVoucherValueDecisionApproved: (msg: NotifyVoucherValueDecisionApproved) -> Unit = noHandler

    @Volatile
    var notifyVoucherValueDecisionPdfGenerated: (msg: NotifyVoucherValueDecisionPdfGenerated) -> Unit =
        noHandler

    @Volatile
    var initializeFamilyFromApplication: (msg: InitializeFamilyFromApplication) -> Unit = noHandler

    @Volatile
    var vtjRefresh: (msg: VTJRefresh) -> Unit = noHandler

    @Volatile
    var dvvModificationsRefresh: (msg: DvvModificationsRefresh) -> Unit = noHandler

    @Volatile
    var uploadToKoski: (msg: UploadToKoski) -> Unit = noHandler

    @Volatile
    var sendApplicationEmail: (msg: SendApplicationEmail) -> Unit = noHandler

    fun plan(
        h: Handle,
        payloads: Iterable<AsyncJobPayload>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: Instant = Instant.now()
    ) = h.transaction { plan(Database.Transaction.wrap(it), payloads, retryCount, retryInterval, runAt) }

    fun plan(
        tx: Database.Transaction,
        payloads: Iterable<AsyncJobPayload>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: Instant = Instant.now()
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

    private fun runPendingJob(db: Database.Connection, job: ClaimedJobRef) {
        try {
            MdcKey.TRACE_ID.set(job.jobId.toString())
            MdcKey.SPAN_ID.set(job.jobId.toString())
            runningCount.incrementAndGet()
            logger.info { "Running async job $job" }
            val completed = db.transaction {
                when (job.jobType) {
                    AsyncJobType.PLACEMENT_PLAN_APPLIED -> it.runJob(job, this.notifyPlacementPlanApplied)
                    AsyncJobType.SERVICE_NEED_UPDATED -> it.runJob(job, this.notifyServiceNeedUpdated)
                    AsyncJobType.FAMILY_UPDATED -> it.runJob(job, this.notifyFamilyUpdated)
                    AsyncJobType.FEE_ALTERATION_UPDATED -> it.runJob(job, this.notifyFeeAlterationUpdated)
                    AsyncJobType.INCOME_UPDATED -> it.runJob(job, this.notifyIncomeUpdated)
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
                }.exhaust()
            }
            if (completed) {
                logger.info { "Completed async job $job" }
            } else {
                logger.info { "Skipped async job $job due to contention" }
            }
        } catch (e: Throwable) {
            val exception = (e as? UndeclaredThrowableException)?.cause ?: e
            logger.error(exception) { "Failed to run async job $job" }
        } finally {
            runningCount.decrementAndGet()
            MdcKey.SPAN_ID.unset()
            MdcKey.TRACE_ID.unset()
            MdcKey.USER_ID.unset()
        }
    }

    override fun close() {
        this.executor.shutdown()
        this.executor.awaitTermination(10, TimeUnit.SECONDS)
        this.executor.shutdownNow()
    }
}

private inline fun <reified T : AsyncJobPayload> Database.Transaction.runJob(
    job: ClaimedJobRef,
    crossinline f: (msg: T) -> Unit
): Boolean {
    val msg = startJob(job, T::class.java) ?: return false
    msg.user?.let { MdcKey.USER_ID.set(it.id.toString()) }
    f(msg)
    completeJob(job)
    return true
}
