// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.JdbiException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AssistanceNeedService {
    fun createAssistanceNeed(db: Database.Connection, user: AuthenticatedUser, childId: UUID, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return db.transaction {
                validateBases(data, it.getAssistanceBasisOptions().map { it.value })
                it.shortenOverlappingAssistanceNeed(user, childId, data.startDate, data.endDate)
                it.insertAssistanceNeed(user, childId, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun getAssistanceNeedsByChildId(db: Database.Connection, childId: UUID): List<AssistanceNeed> {
        return db.transaction { it.getAssistanceNeedsByChild(childId) }
    }

    fun updateAssistanceNeed(db: Database.Connection, user: AuthenticatedUser, id: AssistanceNeedId, data: AssistanceNeedRequest): AssistanceNeed {
        try {
            return db.transaction {
                validateBases(data, it.getAssistanceBasisOptions().map { it.value })
                it.updateAssistanceNeed(user, id, data)
            }
        } catch (e: JdbiException) {
            throw mapPSQLException(e)
        }
    }

    fun deleteAssistanceNeed(db: Database.Connection, id: AssistanceNeedId) {
        db.transaction { it.deleteAssistanceNeed(id) }
    }

    fun getAssistanceBasisOptions(db: Database.Connection): List<AssistanceBasisOption> {
        return db.transaction { it.getAssistanceBasisOptions() }
    }

    private fun validateBases(data: AssistanceNeedRequest, options: List<String>) {
        data.bases.forEach { basis ->
            if (!options.contains(basis)) {
                throw BadRequest("Basis $basis is not valid option, all options: $options")
            }
        }
    }
}
