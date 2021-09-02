// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.deletePartnership
import fi.espoo.evaka.pis.getPartnership
import fi.espoo.evaka.pis.retryPartnership
import fi.espoo.evaka.pis.updatePartnershipDuration
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class PartnershipService {
    fun createPartnership(
        tx: Database.Transaction,
        personId1: UUID,
        personId2: UUID,
        startDate: LocalDate,
        endDate: LocalDate?
    ): Partnership {
        return try {
            tx.createPartnership(personId1, personId2, startDate, endDate)
        } catch (e: UnableToExecuteStatementException) {
            throw mapPSQLException(e)
        }
    }

    fun updatePartnershipDuration(
        tx: Database.Transaction,
        partnershipId: PartnershipId,
        startDate: LocalDate,
        endDate: LocalDate?
    ): Partnership {
        val partnership = tx.getPartnership(partnershipId)
            ?: throw NotFound("No partnership found with id $partnershipId")
        try {
            val success = tx.updatePartnershipDuration(partnershipId, startDate, endDate)
            if (!success) throw NotFound("No partnership found with id $partnershipId")
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }

        return partnership.copy(startDate = startDate, endDate = endDate)
    }

    fun retryPartnership(tx: Database.Transaction, partnershipId: PartnershipId): Partnership? {
        return try {
            tx.getPartnership(partnershipId)
                ?.takeIf { it.conflict }
                ?.also { tx.retryPartnership(partnershipId) }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun deletePartnership(tx: Database.Transaction, partnershipId: PartnershipId): Partnership? {
        return tx.getPartnership(partnershipId)?.also {
            tx.deletePartnership(partnershipId)
        }
    }
}

data class Partnership(
    val id: PartnershipId,
    val partners: Set<PersonJSON>,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false
)

data class Partner(
    val partnershipId: PartnershipId,
    val person: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false
)
