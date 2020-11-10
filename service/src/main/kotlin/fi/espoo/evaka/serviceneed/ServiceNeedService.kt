// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyServiceNeedUpdated
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ServiceNeedService(
    private val jdbi: Jdbi,
    private val asyncJobRunner: AsyncJobRunner
) {
    fun createServiceNeed(user: AuthenticatedUser, childId: UUID, data: ServiceNeedRequest): ServiceNeed {
        try {
            return jdbi.transaction { h ->
                shortenOverlappingServiceNeed(h, user, childId, data.startDate, data.endDate)
                insertServiceNeed(h, user, childId, data).also {
                    notifyServiceNeedUpdated(h, it)
                }
            }.also {
                asyncJobRunner.scheduleImmediateRun()
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getServiceNeedsByChildId(childId: UUID): List<ServiceNeed> {
        return jdbi.handle { h -> getServiceNeedsByChild(h.setReadOnly(true), childId) }
    }

    fun updateServiceNeed(user: AuthenticatedUser, id: UUID, data: ServiceNeedRequest): ServiceNeed {
        try {
            return jdbi.transaction { h ->
                updateServiceNeed(h, user, id, data).also {
                    notifyServiceNeedUpdated(h, it)
                }
            }.also {
                asyncJobRunner.scheduleImmediateRun()
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteServiceNeed(id: UUID) {
        jdbi.transaction { h ->
            deleteServiceNeed(h, id).also {
                notifyServiceNeedUpdated(h, it)
            }
        }
        asyncJobRunner.scheduleImmediateRun()
    }

    fun notifyServiceNeedUpdated(h: Handle, sn: ServiceNeed) = asyncJobRunner.plan(h, listOf(NotifyServiceNeedUpdated(sn.childId, sn.startDate, sn.endDate)))
}
