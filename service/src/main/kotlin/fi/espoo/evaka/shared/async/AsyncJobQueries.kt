// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.json.Json
import java.util.UUID

private val logger = KotlinLogging.logger { }

fun Database.Transaction.insertJob(jobParams: JobParams<*>): UUID = createUpdate(
    // language=SQL
    """
INSERT INTO async_job (type, retry_count, retry_interval, run_at, payload)
VALUES (:jobType, :retryCount, :retryInterval, :runAt, :payload)
RETURNING id
"""
)
    .bind("jobType", AsyncJobType.ofPayload(jobParams.payload).name)
    .bind("retryCount", jobParams.retryCount)
    .bind("retryInterval", jobParams.retryInterval)
    .bind("runAt", jobParams.runAt)
    .bindByType("payload", jobParams.payload, QualifiedType.of(jobParams.payload.javaClass).with(Json::class.java))
    .executeAndReturnGeneratedKeys()
    .mapTo<UUID>()
    .one()

fun Database.Read.getPendingJobCount(jobTypes: Collection<AsyncJobType<*>>): Int =
    createQuery(
        // language=SQL
        """
SELECT count(*)
FROM async_job
WHERE completed_at IS NULL
AND type = ANY(:jobTypes)
"""
    ).bind("jobTypes", jobTypes.flatMap { jobType -> jobType.getAllNames() }.toTypedArray())
        .mapTo<Int>()
        .one()

fun <T : AsyncJobPayload> Database.Transaction.claimJob(jobTypes: Collection<AsyncJobType<out T>>): ClaimedJobRef<out T>? =
    createUpdate(
        // language=SQL
        """
WITH claimed_job AS (
  SELECT id
  FROM async_job
  WHERE run_at < now()
  AND retry_count > 0
  AND completed_at IS NULL
  AND type = ANY(:jobTypes)
  ORDER BY run_at ASC
  LIMIT 1
  FOR UPDATE SKIP LOCKED
)
UPDATE async_job
SET
  retry_count = greatest(0, retry_count - 1),
  run_at = now() + retry_interval,
  claimed_at = now(),
  claimed_by = txid_current()
WHERE id = (SELECT id FROM claimed_job)
RETURNING id AS jobId, type AS jobType, txid_current() AS txId, retry_count AS remainingAttempts
"""
    ).bind("jobTypes", jobTypes.flatMap { jobType -> jobType.getAllNames() }.toTypedArray())
        .executeAndReturnGeneratedKeys()
        .map { row ->
            ClaimedJobRef(
                jobId = row.mapColumn("jobId"),
                jobType = row.mapColumn<String>("jobType").let { jobType ->
                    jobTypes.find { it.getAllNames().contains(jobType) }
                }!!,
                txId = row.mapColumn("txId"),
                remainingAttempts = row.mapColumn("remainingAttempts")
            )
        }
        .findOne()
        .orElse(null)

fun <T : AsyncJobPayload> Database.Transaction.startJob(job: ClaimedJobRef<T>): T? = createUpdate(
    // language=SQL
    """
WITH started_job AS (
  SELECT id
  FROM async_job
  WHERE id = :jobId
  AND claimed_by = :txId
  FOR UPDATE
)
UPDATE async_job
SET started_at = clock_timestamp()
WHERE id = (SELECT id FROM started_job)
RETURNING payload
"""
).bindKotlin(job)
    .executeAndReturnGeneratedKeys()
    .map { row -> row.getColumn("payload", QualifiedType.of(job.jobType.payloadClass.java).with(Json::class.java)) }
    .findOne()
    .orElse(null)

fun Database.Transaction.completeJob(job: ClaimedJobRef<*>) = createUpdate(
    // language=SQL
    """
UPDATE async_job
SET completed_at = clock_timestamp()
WHERE id = :jobId
"""
).bindKotlin(job)
    .execute()

fun Database.Transaction.removeCompletedJobs(completedBefore: HelsinkiDateTime): Int = createUpdate(
    """
DELETE FROM async_job
WHERE completed_at < :completedBefore
"""
).bind("completedBefore", completedBefore)
    .execute()

fun Database.Transaction.removeJobs(runBefore: HelsinkiDateTime): Int = createUpdate(
    """
DELETE FROM async_job
WHERE run_at < :runBefore
"""
).bind("runBefore", runBefore)
    .execute()

fun Database.Connection.removeOldAsyncJobs(now: HelsinkiDateTime) {
    val completedBefore = now.minusMonths(1)
    val completedCount = transaction { it.removeCompletedJobs(completedBefore) }
    logger.info { "Removed $completedCount async jobs completed before $completedBefore" }

    val runBefore = now.minusMonths(6)
    val oldCount = transaction { it.removeJobs(runBefore = runBefore) }
    logger.info("Removed $oldCount async jobs originally planned to be run before $runBefore")
}
