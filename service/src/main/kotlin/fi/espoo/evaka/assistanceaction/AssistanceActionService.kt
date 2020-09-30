// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceaction

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistanceActionService(private val jdbi: Jdbi) {
    fun createAssistanceAction(user: AuthenticatedUser, childId: UUID, data: AssistanceActionRequest): AssistanceAction {
        try {
            return jdbi.transaction { h ->
                shortenOverlappingAssistanceAction(h, user, childId, data.startDate, data.endDate)
                insertAssistanceAction(h, user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceActionsByChildId(childId: UUID): List<AssistanceAction> {
        return jdbi.transaction { h -> getAssistanceActionsByChild(h.setReadOnly(true), childId) }
    }

    fun updateAssistanceAction(user: AuthenticatedUser, id: UUID, data: AssistanceActionRequest): AssistanceAction {
        try {
            return jdbi.transaction { h -> updateAssistanceAction(h, user, id, data) }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceAction(id: UUID) {
        jdbi.transaction { h -> deleteAssistanceAction(h, id) }
    }
}
