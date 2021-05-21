// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistanceActionService {
    fun createAssistanceAction(db: Database.Connection, user: AuthenticatedUser, childId: UUID, data: AssistanceActionRequest): AssistanceAction {
        try {
            return db.transaction {
                validateActions(data, it.getAssistanceActionOptions().map { it.value })
                it.shortenOverlappingAssistanceAction(user, childId, data.startDate, data.endDate)
                it.insertAssistanceAction(user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceActionsByChildId(db: Database.Connection, childId: UUID): List<AssistanceAction> {
        return db.read { it.getAssistanceActionsByChild(childId) }
    }

    fun updateAssistanceAction(db: Database.Connection, user: AuthenticatedUser, id: UUID, data: AssistanceActionRequest): AssistanceAction {
        try {
            return db.transaction {
                validateActions(data, it.getAssistanceActionOptions().map { it.value })
                it.updateAssistanceAction(user, id, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceAction(db: Database.Connection, id: UUID) {
        db.transaction { it.deleteAssistanceAction(id) }
    }

    fun getAssistanceActionOptions(db: Database.Connection): List<AssistanceActionOption> {
        return db.transaction { it.getAssistanceActionOptions() }
    }

    private fun validateActions(data: AssistanceActionRequest, options: List<String>) {
        data.actions.forEach { action ->
            if (!options.contains(action)) {
                throw BadRequest("Action $action is not valid option, all options: $options")
            }
        }
    }
}
