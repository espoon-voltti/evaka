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
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service

@Service
class AssistanceActionService {
    fun createAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        childId: ChildId,
        data: AssistanceActionRequest,
    ): AssistanceAction {
        try {
            return db.transaction { tx ->
                validateActions(data, tx.getAssistanceActionOptions())
                tx.shortenOverlappingAssistanceAction(user, now, childId, data.startDate)
                tx.insertAssistanceAction(user, now, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun updateAssistanceAction(
        db: Database.Connection,
        user: AuthenticatedUser,
        now: HelsinkiDateTime,
        id: AssistanceActionId,
        data: AssistanceActionRequest,
    ): AssistanceAction {
        try {
            return db.transaction { tx ->
                validateActions(data, tx.getAssistanceActionOptions())
                tx.updateAssistanceAction(user, now, id, data)
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

    private fun validateActions(
        data: AssistanceActionRequest,
        options: List<AssistanceActionOption>,
    ) {
        data.actions.forEach { action ->
            val option =
                options.find { it.value == action }
                    ?: throw BadRequest(
                        "Action $action is not a recognized option, all options: ${options.map { it.value }}"
                    )

            if (option.validFrom != null && data.startDate < option.validFrom) {
                throw BadRequest("Action $action cannot be used before ${option.validFrom}")
            }
            if (option.validTo != null && data.endDate > option.validTo) {
                throw BadRequest("Action $action cannot be used after ${option.validTo}")
            }
        }
    }
}
