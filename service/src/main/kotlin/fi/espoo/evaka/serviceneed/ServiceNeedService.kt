// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ServiceNeedService {
    fun createServiceNeed(db: Database.Connection, user: AuthenticatedUser, childId: UUID, data: ServiceNeedRequest): ServiceNeed {
        try {
            return db.transaction { tx ->
                tx.shortenOverlappingServiceNeed(user, childId, data.startDate, data.endDate)
                tx.insertServiceNeed(user, childId, data)
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
                tx.updateServiceNeed(user, id, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteServiceNeed(db: Database.Connection, id: UUID) {
        db.transaction { tx ->
            tx.deleteServiceNeed(id)
        }
    }
}
