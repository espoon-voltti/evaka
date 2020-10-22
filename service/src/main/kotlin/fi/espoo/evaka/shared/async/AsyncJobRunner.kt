// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.application.utils.exhaust
import fi.espoo.evaka.dvv.DvvModificationsRefresh
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.voltti.logging.MdcKey
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.transaction.PlatformTransactionManager
import java.lang.reflect.UndeclaredThrowableException
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

private val logger = KotlinLogging.logger { }

private const val threadPoolSize = 1
private const val defaultRetryCount = 24 * 60 / 5 // 24h when used with default 5 minute retry interval
private val defaultRetryInterval = Duration.ofMinutes(5)

private val noHandler = { msg: Any -> logger.warn("No job handler configured for $msg") }

class AsyncJobRunner(
    private val jdbi: Jdbi,
    private val dataSource: DataSource,
    private val platformTransactionManager: PlatformTransactionManager,
    private val syncMode: Boolean = false
) : AutoCloseable {
    private val executor: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(threadPoolSize)
    private var periodicRunner: ScheduledFuture<*>? = null
    private val runningCount: AtomicInteger = AtomicInteger(0)

    @Volatile
    var notifyDecisionCreated: (msg: NotifyDecisionCreated) -> Unit = noHandler

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
    var sendDecision: (msg: SendDecision) -> Unit = noHandler

    @Volatile
    var notifyDecision2Created: (msg: NotifyDecision2Created) -> Unit = noHandler

    @Volatile
    var sendDecision2: (msg: SendDecision2) -> Unit = noHandler

    @Volatile
    var notifyFeeDecisionApproved: (msg: NotifyFeeDecisionApproved) -> Unit = noHandler

    @Volatile
    var notifyFeeDecisionPdfGenerated: (msg: NotifyFeeDecisionPdfGenerated) -> Unit = noHandler

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
        payloads: Iterable<AsyncJobPayload>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: Instant = Instant.now()
    ) {
        withSpringHandle(dataSource) { h -> plan(h, payloads, retryCount, retryInterval, runAt) }
    }

    fun plan(
        h: Handle,
        payloads: Iterable<AsyncJobPayload>,
        retryCount: Int = defaultRetryCount,
        retryInterval: Duration = defaultRetryInterval,
        runAt: Instant = Instant.now()
    ) {
        payloads.forEach { payload ->
            insertJob(
                h,
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
        jdbi.handle { h -> getPendingJobCount(h, types) }

    fun waitUntilNoRunningJobs(timeout: Duration = Duration.ofSeconds(10)) {
        val start = Instant.now()
        do {
            if (getRunningCount() == 0) return
            TimeUnit.MILLISECONDS.sleep(100)
        } while (Duration.between(start, Instant.now()).abs() < timeout)
        error { "Timed out while waiting for running jobs to finish" }
    }

    private fun runPendingJobs(maxCount: Int) {
        jdbi.handle { h ->
            var remaining = maxCount
            do {
                val job = h.transaction { tx -> claimJob(tx) }?.also(this::runPendingJob)
                remaining -= 1
            } while (job != null && remaining > 0)
        }
    }

    private fun runPendingJob(job: ClaimedJobRef) {
        try {
            MdcKey.TRACE_ID.set(job.jobId.toString())
            MdcKey.SPAN_ID.set(job.jobId.toString())
            runningCount.incrementAndGet()
            logger.info { "Running async job $job" }
            val completed = when (job.jobType) {
                AsyncJobType.DECISION_CREATED -> runJob(job, this.notifyDecisionCreated)
                AsyncJobType.PLACEMENT_PLAN_APPLIED -> runJob(job, this.notifyPlacementPlanApplied)
                AsyncJobType.SERVICE_NEED_UPDATED -> runJob(job, this.notifyServiceNeedUpdated)
                AsyncJobType.FAMILY_UPDATED -> runJob(job, this.notifyFamilyUpdated)
                AsyncJobType.FEE_ALTERATION_UPDATED -> runJob(job, this.notifyFeeAlterationUpdated)
                AsyncJobType.INCOME_UPDATED -> runJob(job, this.notifyIncomeUpdated)
                AsyncJobType.SEND_DECISION -> runJob(job, this.sendDecision)
                AsyncJobType.DECISION2_CREATED -> runJob(job, this.notifyDecision2Created)
                AsyncJobType.SEND_DECISION2 -> runJob(job, this.sendDecision2)
                AsyncJobType.FEE_DECISION_APPROVED -> runJob(job, this.notifyFeeDecisionApproved)
                AsyncJobType.FEE_DECISION_PDF_GENERATED -> runJob(job, this.notifyFeeDecisionPdfGenerated)
                AsyncJobType.INITIALIZE_FAMILY_FROM_APPLICATION -> runJob(job, this.initializeFamilyFromApplication)
                AsyncJobType.VTJ_REFRESH -> runJob(job, this.vtjRefresh)
                AsyncJobType.DVV_MODIFICATIONS_REFRESH -> runJob(job, this.dvvModificationsRefresh)
                AsyncJobType.UPLOAD_TO_KOSKI -> runJob(job, this.uploadToKoski)
                AsyncJobType.SEND_APPLICATION_EMAIL -> runJob(job, this.sendApplicationEmail)
            }.exhaust()
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

    private inline fun <reified T : AsyncJobPayload> runJob(job: ClaimedJobRef, crossinline f: (msg: T) -> Unit) =
        withSpringTx(platformTransactionManager, requiresNew = true) {
            withSpringHandle(dataSource) { tx ->
                startJob(tx, job, T::class.java)?.let { msg ->
                    msg.user?.let { MdcKey.USER_ID.set(it.id.toString()) }
                    f(msg)
                    completeJob(tx, job)
                    true
                } ?: false
            }
        }

    override fun close() {
        this.executor.shutdown()
        this.executor.awaitTermination(10, TimeUnit.SECONDS)
        this.executor.shutdownNow()
    }
}
