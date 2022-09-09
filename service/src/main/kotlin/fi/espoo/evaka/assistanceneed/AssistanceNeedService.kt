// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service

@Service
class AssistanceNeedService(val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    fun createAssistanceNeed(db: Database.Connection, user: AuthenticatedUser, clock: EvakaClock, childId: ChildId, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return db.transaction { tx ->
                validateBases(data, tx.getAssistanceBasisOptions().map { it.value })
                tx.shortenOverlappingAssistanceNeed(user, childId, data.startDate, data.endDate)
                tx.insertAssistanceNeed(user, childId, data).also {
                    notifyAssistanceNeedUpdated(
                        tx,
                        clock,
                        AssistanceNeedChildRange(childId, DateRange(data.startDate, data.endDate))
                    )
                }
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceNeedsByChildId(db: Database.Connection, childId: ChildId): List<AssistanceNeed> {
        return db.transaction { it.getAssistanceNeedsByChild(childId) }
    }

    fun updateAssistanceNeed(db: Database.Connection, user: AuthenticatedUser, clock: EvakaClock, id: AssistanceNeedId, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return db.transaction { tx ->
                validateBases(data, tx.getAssistanceBasisOptions().map { it.value })
                tx.updateAssistanceNeed(user, id, data).also {
                    notifyAssistanceNeedUpdated(
                        tx,
                        clock,
                        AssistanceNeedChildRange(it.childId, DateRange(it.startDate, it.endDate))
                    )
                }
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceNeed(db: Database.Connection, clock: EvakaClock, id: AssistanceNeedId) {
        db.transaction { tx ->
            val childRange = tx.deleteAssistanceNeed(id)
            notifyAssistanceNeedUpdated(tx, clock, childRange)
        }
    }

    fun getAssistanceBasisOptions(db: Database.Connection): List<AssistanceBasisOption> {
        return db.transaction { it.getAssistanceBasisOptions() }
    }

    private fun validateBases(data: AssistanceNeedRequest, options: List<String>) {
        data.bases.forEach { basis ->
            if (!options.contains(basis)) {
                throw BadRequest("Basis $basis is not valid option, all options: $options")
            }
        }
    }

    private fun notifyAssistanceNeedUpdated(tx: Database.Transaction, clock: EvakaClock, childRange: AssistanceNeedChildRange) {
        asyncJobRunner.plan(
            tx,
            listOf(AsyncJob.GenerateFinanceDecisions.forChild(childRange.childId, childRange.dateRange)),
            runAt = clock.now()
        )
    }
}
