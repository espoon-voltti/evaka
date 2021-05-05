// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyServiceNeedUpdated
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ServiceNeedService(private val asyncJobRunner: AsyncJobRunner) {
    fun createServiceNeed(db: Database.Connection, user: AuthenticatedUser, childId: UUID, data: ServiceNeedRequest): ServiceNeed {
        try {
            return db.transaction { tx ->
                tx.shortenOverlappingServiceNeed(user, childId, data.startDate, data.endDate)
                tx.insertServiceNeed(user, childId, data).also {
                    tx.notifyServiceNeedUpdated(it)
                }
            }.also {
                asyncJobRunner.scheduleImmediateRun()
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getServiceNeedsByChildId(db: Database.Connection, childId: UUID): List<ServiceNeed> {
        return db.read { tx -> tx.getServiceNeedsByChild(childId) }
    }

    fun updateServiceNeed(db: Database.Connection, user: AuthenticatedUser, id: UUID, data: ServiceNeedRequest): ServiceNeed {
        try {
            return db.transaction { tx ->
                tx.updateServiceNeed(user, id, data).also {
                    tx.notifyServiceNeedUpdated(it)
                }
            }.also {
                asyncJobRunner.scheduleImmediateRun()
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteServiceNeed(db: Database.Connection, id: UUID) {
        db.transaction { tx ->
            tx.deleteServiceNeed(id).also {
                tx.notifyServiceNeedUpdated(it)
            }
        }
        asyncJobRunner.scheduleImmediateRun()
    }

    private fun Database.Transaction.notifyServiceNeedUpdated(sn: ServiceNeed) = asyncJobRunner.plan(this, listOf(NotifyServiceNeedUpdated(sn.childId, sn.startDate, sn.endDate)))
}
