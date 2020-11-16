// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistanceNeedService {
    fun createAssistanceNeed(db: Database, user: AuthenticatedUser, childId: UUID, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return db.transaction {
                shortenOverlappingAssistanceNeed(it.handle, user, childId, data.startDate, data.endDate)
                insertAssistanceNeed(it.handle, user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceNeedsByChildId(db: Database, childId: UUID): List<AssistanceNeed> {
        return db.transaction { getAssistanceNeedsByChild(it.handle, childId) }
    }

    fun updateAssistanceNeed(db: Database, user: AuthenticatedUser, id: UUID, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return db.transaction { updateAssistanceNeed(it.handle, user, id, data) }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceNeed(db: Database, id: UUID) {
        db.transaction { deleteAssistanceNeed(it.handle, id) }
    }
}
