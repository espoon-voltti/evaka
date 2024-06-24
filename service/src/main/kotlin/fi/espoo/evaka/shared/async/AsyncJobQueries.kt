// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID
import mu.KotlinLogging
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.json.Json

private val logger = KotlinLogging.logger {}

fun Database.Transaction.insertJob(jobParams: JobParams<*>): UUID =
    createUpdate {
        sql(
            """
INSERT INTO async_job (type, retry_count, retry_interval, run_at, payload)
VALUES (
    ${bind(AsyncJobType.ofPayload(jobParams.payload).name)},
    ${bind(jobParams.retryCount)},
    ${bind(jobParams.retryInterval)},
    ${bind(jobParams.runAt)},
    ${bindJson(jobParams.payload)}
)
RETURNING id
"""
        )
    }.executeAndReturnGeneratedKeys()
        .exactlyOne<UUID>()

fun Database.Transaction.upsertPermit(pool: AsyncJobPool.Id<*>) {
    createUpdate {
        sql(
            """
INSERT INTO async_job_work_permit (pool_id, available_at)
VALUES (${bind(pool.toString())}, '-infinity')
ON CONFLICT DO NOTHING
"""
        )
    }.execute()
}

fun Database.Transaction.claimPermit(pool: AsyncJobPool.Id<*>): WorkPermit =
    createQuery {
        sql(
            """
SELECT available_at
FROM async_job_work_permit
WHERE pool_id = ${bind(pool.toString())}
FOR UPDATE
"""
        )
    }.exactlyOne()

fun Database.Transaction.updatePermit(
    pool: AsyncJobPool.Id<*>,
    availableAt: HelsinkiDateTime
) = createUpdate {
    sql(
        """
UPDATE async_job_work_permit
SET available_at = ${bind(availableAt)}
WHERE pool_id = ${bind(pool.toString())}
"""
    )
}.updateExactlyOne()

fun <T : AsyncJobPayload> Database.Transaction.claimJob(
    now: HelsinkiDateTime,
    jobTypes: Collection<AsyncJobType<out T>>
): ClaimedJobRef<out T>? =
    createUpdate {
        sql(
            """
WITH claimed_job AS (
  SELECT id
  FROM async_job
  WHERE run_at <= ${bind(now)}
  AND retry_count > 0
  AND completed_at IS NULL
  AND type = ANY(${bind(jobTypes.map { it.name })})
  ORDER BY run_at ASC
  LIMIT 1
  FOR UPDATE SKIP LOCKED
)
UPDATE async_job
SET
  retry_count = greatest(0, retry_count - 1),
  run_at = ${bind(now)} + retry_interval,
  claimed_at = ${bind(now)},
  claimed_by = txid_current()
WHERE id = (SELECT id FROM claimed_job)
RETURNING id AS jobId, type AS jobType, txid_current() AS txId, retry_count AS remainingAttempts
        """
        )
    }.executeAndReturnGeneratedKeys()
        .exactlyOneOrNull {
            ClaimedJobRef(
                jobId = column("jobId"),
                jobType =
                    column<String>("jobType").let { jobType ->
                        jobTypes.find { it.name == jobType }
                    }!!,
                txId = column("txId"),
                remainingAttempts = column("remainingAttempts")
            )
        }

fun <T : AsyncJobPayload> Database.Transaction.startJob(
    job: ClaimedJobRef<T>,
    now: HelsinkiDateTime
): T? =
    createUpdate {
        sql(
            """
WITH started_job AS (
  SELECT id
  FROM async_job
  WHERE id = ${bind(job.jobId)}
  AND claimed_by = ${bind(job.txId)}
  FOR UPDATE
)
UPDATE async_job
SET started_at = ${bind(now)}
WHERE id = (SELECT id FROM started_job)
RETURNING payload
"""
        )
    }.executeAndReturnGeneratedKeys()
        .exactlyOneOrNull {
            column(
                "payload",
                QualifiedType.of(job.jobType.payloadClass.java).with(Json::class.java)
            )
        }

fun Database.Transaction.completeJob(
    job: ClaimedJobRef<*>,
    now: HelsinkiDateTime
) = createUpdate {
    sql(
        """
UPDATE async_job
SET completed_at = ${bind(now)}
WHERE id = ${bind(job.jobId)}
"""
    )
}.execute()

fun Database.Transaction.removeCompletedJobs(completedBefore: HelsinkiDateTime): Int =
    createUpdate {
        sql(
            """
DELETE FROM async_job
WHERE completed_at < ${bind(completedBefore)}
"""
        )
    }.execute()

fun Database.Transaction.removeUnclaimedJobs(jobTypes: Collection<AsyncJobType<*>>): Int =
    createUpdate {
        sql(
            """
DELETE FROM async_job
WHERE completed_at IS NULL
AND claimed_at IS NULL
AND type = ANY(${bind(jobTypes.map { it.name })})
    """
        )
    }.execute()

fun Database.Transaction.removeUncompletedJobs(runBefore: HelsinkiDateTime): Int =
    createUpdate {
        sql(
            """
DELETE FROM async_job
WHERE completed_at IS NULL
AND run_at < ${bind(runBefore)}
"""
        )
    }.execute()

fun Database.Connection.removeOldAsyncJobs(now: HelsinkiDateTime) {
    val completedBefore = now.minusMonths(6)
    val completedCount = transaction { it.removeCompletedJobs(completedBefore) }
    logger.info { "Removed $completedCount async jobs completed before $completedBefore" }

    val runBefore = now.minusMonths(6)
    val oldCount = transaction { it.removeUncompletedJobs(runBefore = runBefore) }
    logger.info("Removed $oldCount async jobs originally planned to be run before $runBefore")
}
