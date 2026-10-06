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
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import kotlin.time.TimeSource
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DataRetentionService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    featureConfig: FeatureConfig,
) {
    private val schema = buildDataRetentionSchema(featureConfig.valueDecisionCapacityFactorEnabled)

    init {
        asyncJobRunner.registerHandler(::deleteExpiredPersonData)
    }

    fun deleteExpiredPersonData(
        dbc: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteExpiredPersonData,
    ) {
        runDataRetention(
            dbc,
            clock,
            asyncJobRunner,
            schema,
            msg.personId,
            dryRun = msg.dryRun,
        )
    }
}

fun runDataRetention(
    dbc: Database.Connection,
    clock: EvakaClock,
    asyncJobRunner: AsyncJobRunner<AsyncJob>,
    schema: SchemaDefinition,
    personId: PersonId,
    dryRun: Boolean,
) {
    val started = TimeSource.Monotonic.markNow()
    val deletionResult =
        dbc.transaction { tx ->
            val graph = tx.loadPersonGraph(schema, personId)
            if (graph == null) {
                logger.warn { "Data retention for person $personId: the person no longer exists" }
                return@transaction null
            }
            val plan = graph.evaluate(clock.today())
            if (dryRun) {
                logger.info {
                    "Dry run of data retention for person $personId: would ${plan.describe()}, evaluated in ${started.elapsedNow().inWholeMilliseconds} ms"
                }
                null
            } else {
                asyncJobRunner.plan(tx, plan.asyncJobs, runAt = clock.now())
                tx.executeDeletionPlan(plan, clock.now())
            }
        } ?: return

    logger.info {
        "Data retention for person $personId: deleted ${describeRowCounts(deletionResult.deletedRowCountsByTable)}, cleared ${describeRowCounts(deletionResult.clearedRowCountsByTable)}, froze ${describeFreezes(deletionResult.childrenFrozenForKoski, deletionResult.childrenFrozenForVarda)}, in ${started.elapsedNow().inWholeMilliseconds} ms"
    }

    // Audit logs
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
