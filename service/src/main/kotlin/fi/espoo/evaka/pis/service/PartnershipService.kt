// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.deletePartnership
import fi.espoo.evaka.pis.getPartnership
import fi.espoo.evaka.pis.retryPartnership
import fi.espoo.evaka.pis.updatePartnershipDuration
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.springframework.stereotype.Service

@Service
class PartnershipService {
    fun createPartnership(
        tx: Database.Transaction,
        personId1: PersonId,
        personId2: PersonId,
        startDate: LocalDate,
        endDate: LocalDate?,
        creatorId: EvakaUserId?,
        createDate: HelsinkiDateTime
    ): Partnership {
        return try {
            tx.createPartnership(
                personId1,
                personId2,
                startDate,
                endDate,
                false,
                creatorId,
                createDate
            )
        } catch (e: UnableToExecuteStatementException) {
            throw mapPSQLException(e)
        }
    }

    fun updatePartnershipDuration(
        tx: Database.Transaction,
        partnershipId: PartnershipId,
        startDate: LocalDate,
        endDate: LocalDate?,
        modifiedById: EvakaUserId?,
        modificationDate: HelsinkiDateTime
    ) {
        try {
            val success =
                tx.updatePartnershipDuration(
                    partnershipId,
                    startDate,
                    endDate,
                    modifiedById,
                    modificationDate
                )
            if (!success) throw NotFound("No partnership found with id $partnershipId")
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun retryPartnership(
        tx: Database.Transaction,
        partnershipId: PartnershipId,
        modifiedById: EvakaUserId?,
        modificationDate: HelsinkiDateTime
    ): Partnership? {
        return try {
            tx.getPartnership(partnershipId)
                ?.takeIf { it.conflict }
                ?.also { tx.retryPartnership(partnershipId, modifiedById, modificationDate) }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun deletePartnership(tx: Database.Transaction, partnershipId: PartnershipId): Partnership? {
        return tx.getPartnership(partnershipId)?.also { tx.deletePartnership(partnershipId) }
    }
}

data class Partnership(
    val id: PartnershipId,
    val partners: Set<PersonJSON>,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false,
    val createdAt: HelsinkiDateTime,
    val createdBy: EvakaUserId?,
    val modifiedAt: HelsinkiDateTime,
    val modifiedBy: EvakaUserId?
)

data class Partner(
    val partnershipId: PartnershipId,
    val person: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false
)
