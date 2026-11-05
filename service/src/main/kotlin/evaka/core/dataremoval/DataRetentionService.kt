// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.shared.FeatureConfig
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.MockEvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.util.UUID
import kotlin.time.TimeSource
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DataRetentionService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig,
    overrides: DataRetentionOverrides,
) {
    val schema =
        buildDataRetentionSchema(featureConfig.valueDecisionCapacityFactorEnabled, overrides)

    init {
        asyncJobRunner.registerHandler(::deleteExpiredPersonData)
    }

    fun deleteExpiredPersonData(
        dbc: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteExpiredPersonData,
    ) {
        require(msg.dryRun || msg.mockNow == null) { "A mock time is only allowed in a dry run" }
        if (msg.skipIfStartedAfter != null && clock.now() > msg.skipIfStartedAfter) {
            // The rest of the night's jobs share the deadline, so they would be skipped too
            dbc.transaction {
                it.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.DeleteExpiredPersonData::class)))
            }
            logger.info {
                "Data retention for person ${msg.personId} skipped and the remaining jobs of the night removed: the time allocation of the night ended at ${msg.skipIfStartedAfter}"
            }
            return
        }
        // A run with a mock time does not tell whether the person's data is expired now, so it must
        // not postpone the person's next nightly run
        if (msg.mockNow == null) {
            // Recorded in its own transaction so that a failed run counts as an attempt too
            dbc.transaction { it.setDataRetentionLastAttemptedAt(msg.personId, clock.now()) }
        }
        runDataRetention(
            dbc,
            msg.mockNow?.let { MockEvakaClock(it) } ?: clock,
            asyncJobRunner,
            schema,
            msg.personId,
            dryRun = msg.dryRun,
            optimized = msg.optimized ?: true,
        )
    }
}

fun planDataRetentionRuns(
    db: Database.Connection,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    personCount: Int,
    maxDuration: Duration,
) {
    val now = clock.now()
    val skipIfStartedAfter = now + maxDuration
    val personIds = db.transaction { tx ->
        val personIds = tx.getPersonsWithOldestDataRetentionAttempt(personCount)
        asyncJobRunner.plan(
            tx,
            personIds.map { personId ->
                AsyncJob.DeleteExpiredPersonData(
                    personId,
                    dryRun = true,
                    optimized = true,
                    skipIfStartedAfter = skipIfStartedAfter,
                )
            },
            retryCount = 3,
            runAt = now,
        )
        personIds
    }
    logger.info {
        "Planned data retention dry runs for ${personIds.size} persons, skipped if started after $skipIfStartedAfter"
    }
}

fun runDataRetention(
    dbc: Database.Connection,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    schema: SchemaDefinition,
    personId: PersonId,
    dryRun: Boolean,
    optimized: Boolean,
) {
    val started = TimeSource.Monotonic.markNow()
    val today = clock.today()
    val (clearedLink, deletionResult) =
        dbc.transaction { tx ->
            tx.lockDataRetention()
            val duplicateLink = tx.getDuplicateLink(personId)
            if (duplicateLink != null && !duplicateLink.mayBeCleared(today)) {
                logger.info {
                    "Data retention for person $personId skipped: it is still linked to its duplicate pair ${duplicateLink.otherThan(personId)}"
                }
                return@transaction null
            }
            val graph = tx.loadPersonGraph(schema, personId, today, optimized)
            if (graph == null) {
                logger.warn { "Data retention for person $personId: the person no longer exists" }
                return@transaction null
            }
            val plan = graph.evaluate(today)
            if (dryRun) {
                logger.info {
                    "Dry run of data retention for person $personId as of $today: would ${duplicateLink?.let { "clear the duplicate link to ${it.otherThan(personId)}, " }.orEmpty()}${plan.describe()}, evaluated in ${started.elapsedNow().inWholeMilliseconds} ms (${if (optimized) "optimized" else "not optimized"})"
                }
                null
            } else {
                val childrenFrozenWithLink =
                    duplicateLink
                        ?.let { link ->
                            // The other person no longer waits for this one
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.DeleteExpiredPersonData(
                                        link.otherThan(personId),
                                        dryRun = false,
                                        optimized = optimized,
                                    )
                                ),
                                retryCount = 3,
                                runAt = clock.now(),
                            )
                            tx.clearDuplicateLink(link, clock.now())
                        }
                        .orEmpty()
                asyncJobRunner.plan(tx, plan.asyncJobs, runAt = clock.now())
                val result = tx.executeDeletionPlan(plan, clock.now())
                Pair(
                    duplicateLink,
                    result.copy(
                        childrenFrozenForKoski =
                            childrenFrozenWithLink + result.childrenFrozenForKoski
                    ),
                )
            }
        } ?: return

    logger.info {
        "Data retention for person $personId: ${clearedLink?.let { "cleared the duplicate link to ${it.otherThan(personId)}, " }.orEmpty()}deleted ${describeRowCounts(deletionResult.deletedRowCountsByTable)}, cleared ${describeRowCounts(deletionResult.clearedRowCountsByTable)}, froze ${describeFreezes(deletionResult.childrenFrozenForKoski, deletionResult.childrenFrozenForVarda)}, in ${started.elapsedNow().inWholeMilliseconds} ms (${if (optimized) "optimized" else "not optimized"})"
    }

    // Audit logs
    if (clearedLink != null) {
        AuditContext()
            .add(listOf(clearedLink.duplicateId, clearedLink.originalId))
            .log(Audit.DataRemovalDuplicateLinkCleared, clock)
    }
    if (deletionResult.deletedRowsByTable.isNotEmpty()) {
        val audit =
            AuditContext()
                .add(personId)
                .addMeta("rowsByTable", deletionResult.deletedRowCountsByTable)
        val compositeRowIdsByTable = mutableMapOf<String, List<Map<String, UUID>>>()
        deletionResult.deletedRowsByTable.forEach { (table, deleted) ->
            if (deleted.auditIdType != null) {
                audit.add(deleted.auditIdType, deleted.ids.map { it.single() })
            } else {
                compositeRowIdsByTable[table] = deleted.ids.distinct().map { it.idByColumn }
            }
        }
        if (compositeRowIdsByTable.isNotEmpty()) {
            audit.addMeta("compositeRowIdsByTable", compositeRowIdsByTable)
        }
        audit.log(Audit.DataRemovalExpiredDelete, clock)
    }
    if (deletionResult.clearedRowCountsByTable.isNotEmpty()) {
        AuditContext()
            .add(personId)
            .addMeta("rowsByTable", deletionResult.clearedRowCountsByTable)
            .log(Audit.DataRemovalExpiredUnset, clock)
    }
    if (deletionResult.childrenFrozenForKoski.isNotEmpty()) {
        AuditContext()
            .add(deletionResult.childrenFrozenForKoski)
            .addMeta("targetPersonId", personId.raw.toString())
            .log(Audit.DataRemovalKoskiSyncFrozen, clock)
    }
    if (deletionResult.childrenFrozenForVarda.isNotEmpty()) {
        AuditContext()
            .add(deletionResult.childrenFrozenForVarda)
            .addMeta("targetPersonId", personId.raw.toString())
            .log(Audit.DataRemovalVardaSyncFrozen, clock)
    }
}
