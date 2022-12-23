// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID
import mu.KotlinLogging
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.json.Json

private val logger = KotlinLogging.logger {}

fun Database.Transaction.insertJob(jobParams: JobParams<*>): UUID =
    createUpdate(
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
        .bindByType(
            "payload",
            jobParams.payload,
            QualifiedType.of(jobParams.payload.javaClass).with(Json::class.java)
        )
        .executeAndReturnGeneratedKeys()
        .mapTo<UUID>()
        .one()

fun <T : AsyncJobPayload> Database.Transaction.claimJob(
    now: HelsinkiDateTime,
    jobTypes: Collection<AsyncJobType<out T>>
): ClaimedJobRef<out T>? =
    createUpdate<Any> {
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
        }
        .executeAndReturnGeneratedKeys()
        .map { row ->
            ClaimedJobRef(
                jobId = row.mapColumn("jobId"),
                jobType =
                    row.mapColumn<String>("jobType").let { jobType ->
                        jobTypes.find { it.name == jobType }
                    }!!,
                txId = row.mapColumn("txId"),
                remainingAttempts = row.mapColumn("remainingAttempts")
            )
        }
        .findOne()
        .orElse(null)

fun <T : AsyncJobPayload> Database.Transaction.startJob(
    job: ClaimedJobRef<T>,
    now: HelsinkiDateTime
): T? =
    createUpdate(
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
SET started_at = :now
WHERE id = (SELECT id FROM started_job)
RETURNING payload
"""
        )
        .bindKotlin(job)
        .bind("now", now)
        .executeAndReturnGeneratedKeys()
        .map { row ->
            row.getColumn(
                "payload",
                QualifiedType.of(job.jobType.payloadClass.java).with(Json::class.java)
            )
        }
        .findOne()
        .orElse(null)

fun Database.Transaction.completeJob(job: ClaimedJobRef<*>, now: HelsinkiDateTime) =
    createUpdate(
            // language=SQL
            """
UPDATE async_job
SET completed_at = :now
WHERE id = :jobId
"""
        )
        .bindKotlin(job)
        .bind("now", now)
        .execute()

fun Database.Transaction.removeCompletedJobs(completedBefore: HelsinkiDateTime): Int =
    createUpdate("""
DELETE FROM async_job
WHERE completed_at < :completedBefore
""")
        .bind("completedBefore", completedBefore)
        .execute()

fun Database.Transaction.removeUnclaimedJobs(jobTypes: Collection<AsyncJobType<*>>): Int =
    createUpdate<DatabaseTable> {
            sql(
                """
DELETE FROM async_job
WHERE completed_at IS NULL
AND claimed_at IS NULL
AND type = ANY(${bind(jobTypes.map { it.name })})
    """
                    .trimIndent()
            )
        }
        .execute()

fun Database.Transaction.removeJobs(runBefore: HelsinkiDateTime): Int =
    createUpdate("""
DELETE FROM async_job
WHERE run_at < :runBefore
""")
        .bind("runBefore", runBefore)
        .execute()

fun Database.Connection.removeOldAsyncJobs(now: HelsinkiDateTime) {
    val completedBefore = now.minusMonths(6)
    val completedCount = transaction { it.removeCompletedJobs(completedBefore) }
    logger.info { "Removed $completedCount async jobs completed before $completedBefore" }

    val runBefore = now.minusMonths(6)
    val oldCount = transaction { it.removeJobs(runBefore = runBefore) }
    logger.info("Removed $oldCount async jobs originally planned to be run before $runBefore")
}
