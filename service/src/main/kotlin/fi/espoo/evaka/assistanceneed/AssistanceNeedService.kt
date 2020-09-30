// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistanceNeedService(private val jdbi: Jdbi) {
    fun createAssistanceNeed(user: AuthenticatedUser, childId: UUID, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return jdbi.transaction { h ->
                shortenOverlappingAssistanceNeed(h, user, childId, data.startDate, data.endDate)
                insertAssistanceNeed(h, user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceNeedsByChildId(childId: UUID): List<AssistanceNeed> {
        return jdbi.transaction { h -> getAssistanceNeedsByChild(h.setReadOnly(true), childId) }
    }

    fun updateAssistanceNeed(user: AuthenticatedUser, id: UUID, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return jdbi.transaction { h -> updateAssistanceNeed(h, user, id, data) }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceNeed(id: UUID) {
        jdbi.transaction { h -> deleteAssistanceNeed(h, id) }
    }
}
