// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dataremoval

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.time.TimeSource
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class DataRetentionService(private val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    init {
        asyncJobRunner.registerHandler(::deleteExpiredPersonData)
    }

    fun deleteExpiredPersonData(
        dbc: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteExpiredPersonData,
    ) {
        val personId = msg.personId
        val started = TimeSource.Monotonic.markNow()
        val deletionResult =
            dbc.transaction { tx ->
                val graph = tx.loadPersonGraph(dataRetentionSchema, personId)
                if (graph == null) {
                    logger.warn {
                        "Data retention for person $personId: the person no longer exists"
                    }
                    return@transaction null
                }
                val plan = graph.evaluate(clock.today())
                if (msg.dryRun) {
                    logger.info {
                        "Dry run of data retention for person $personId: would ${plan.describe()}, evaluated in ${started.elapsedNow().inWholeMilliseconds} ms"
                    }
                    null
                } else {
                    asyncJobRunner.plan(tx, plan.asyncJobs, runAt = clock.now())
                    tx.executeDeletionPlan(plan, clock.now())
                }
            } ?: return

        if (msg.dryRun) {
            logger.info {
                "Data retention dry run for person $personId took ${started.elapsedNow().inWholeMilliseconds} ms"
            }
        } else {
            logger.info {
                "Data retention for person $personId: deleted ${describeRowCounts(deletionResult.deletedRowCountsByTable)}, cleared ${describeRowCounts(deletionResult.clearedRowCountsByColumn.mapKeys { "${it.key.first}.${it.key.second}" })}, froze ${describeFreezes(deletionResult.childrenFrozenForKoski, deletionResult.childrenFrozenForVarda)}, in ${started.elapsedNow().inWholeMilliseconds} ms"
            }
        }

        // Audit logs
        deletionResult.deletedRowCountsByTable.forEach { (table, rowCount) ->
            AuditContext()
                .add(personId)
                .addMeta("entity", table)
                .addMeta("rows", rowCount)
                .log(Audit.DataRemovalExpiredDelete, clock)
        }
        deletionResult.clearedRowCountsByColumn.forEach { (cleared, rowCount) ->
            val table = cleared.first
            val column = cleared.second
            val reference =
                dataRetentionSchema.tablesByName.getValue(table).optionalReferences.first {
                    it.referenceColumn == column
                }
            AuditContext()
                .add(personId)
                .addMeta("entity", table)
                .addMeta("clearedColumns", listOf(column) + reference.alsoNull)
                .addMeta("rows", rowCount)
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
}
