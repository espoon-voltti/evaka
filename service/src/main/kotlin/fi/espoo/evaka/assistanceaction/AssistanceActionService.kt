// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service

@Service
class AssistanceActionService {
    fun createAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        childId: ChildId,
        data: AssistanceActionRequest
    ): AssistanceAction {
        try {
            return db.transaction { tx ->
                validateActions(data, tx.getAssistanceActionOptions().map { it.value })
                tx.shortenOverlappingAssistanceAction(user, childId, data.startDate)
                tx.insertAssistanceAction(user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun updateAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        id: AssistanceActionId,
        data: AssistanceActionRequest
    ): AssistanceAction {
        try {
            return db.transaction { tx ->
                validateActions(data, tx.getAssistanceActionOptions().map { it.value })
                tx.updateAssistanceAction(user, id, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceAction(db: Database.Connection, id: AssistanceActionId) {
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
