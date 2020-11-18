// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistanceActionService {
    fun createAssistanceAction(db: Database.Connection, user: AuthenticatedUser, childId: UUID, data: AssistanceActionRequest): AssistanceAction {
        try {
            return db.transaction {
                shortenOverlappingAssistanceAction(it.handle, user, childId, data.startDate, data.endDate)
                insertAssistanceAction(it.handle, user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceActionsByChildId(db: Database.Connection, childId: UUID): List<AssistanceAction> {
        return db.read { getAssistanceActionsByChild(it.handle, childId) }
    }

    fun updateAssistanceAction(db: Database.Connection, user: AuthenticatedUser, id: UUID, data: AssistanceActionRequest): AssistanceAction {
        try {
            return db.transaction { updateAssistanceAction(it.handle, user, id, data) }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceAction(db: Database.Connection, id: UUID) {
        db.transaction { deleteAssistanceAction(it.handle, id) }
    }
}
