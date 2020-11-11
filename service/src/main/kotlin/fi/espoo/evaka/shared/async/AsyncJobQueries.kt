// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.async

import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.qualifier.QualifiedType
import org.jdbi.v3.json.Json
import java.util.UUID

fun <T : AsyncJobPayload> Database.Transaction.insertJob(jobParams: JobParams<T>): UUID = createUpdate(
    // language=SQL
    """
INSERT INTO async_job (type, retry_count, retry_interval, run_at, payload)
VALUES (:jobType, :retryCount, :retryInterval, :runAt, :payload)
RETURNING id
"""
)
    .bind("jobType", jobParams.payload.asyncJobType)
    .bind("retryCount", jobParams.retryCount)
    .bind("retryInterval", jobParams.retryInterval)
    .bind("runAt", jobParams.runAt)
    .bindByType("payload", jobParams.payload, QualifiedType.of(jobParams.payload.javaClass).with(Json::class.java))
    .executeAndReturnGeneratedKeys()
    .mapTo(UUID::class.java)
    .one()

fun Database.Read.getPendingJobCount(jobTypes: Collection<AsyncJobType> = AsyncJobType.values().toList()): Int =
    createQuery(
        // language=SQL
        """
SELECT count(*)
FROM async_job
WHERE completed_at IS NULL
AND type = ANY(:jobTypes)
"""
    ).bind("jobTypes", jobTypes.toTypedArray())
        .mapTo<Int>()
        .one()

fun Database.Transaction.claimJob(jobTypes: Collection<AsyncJobType> = AsyncJobType.values().toList()): ClaimedJobRef? =
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
RETURNING id AS jobId, type AS jobType, txid_current() AS txId
"""
    ).bind("jobTypes", jobTypes.toTypedArray())
        .executeAndReturnGeneratedKeys()
        .mapTo<ClaimedJobRef>()
        .findOne()
        .orElse(null)

fun <T : AsyncJobPayload> Database.Transaction.startJob(job: ClaimedJobRef, payloadClass: Class<T>): T? = createUpdate(
    // language=SQL
    """
WITH started_job AS (
  SELECT id
  FROM async_job
  WHERE id = :jobId
  AND claimed_by = :txId
  FOR UPDATE SKIP LOCKED
)
UPDATE async_job
SET started_at = clock_timestamp()
WHERE id = (SELECT id FROM started_job)
RETURNING payload
"""
).bindKotlin(job)
    .executeAndReturnGeneratedKeys()
    .map { row -> row.getColumn("payload", QualifiedType.of(payloadClass).with(Json::class.java)) }
    .findOne()
    .orElse(null)

fun Database.Transaction.completeJob(job: ClaimedJobRef) = createUpdate(
    // language=SQL
    """
UPDATE async_job
SET completed_at = clock_timestamp()
WHERE id = :jobId
"""
).bindKotlin(job)
    .execute()
