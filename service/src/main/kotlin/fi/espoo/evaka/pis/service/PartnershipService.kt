// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.dao.PartnershipDAO
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.db.runAfterCommit
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.maxEndDate
import mu.KotlinLogging
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class PartnershipService(
    private val dao: PartnershipDAO,
    private val asyncJobRunner: AsyncJobRunner,
    private val txm: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createPartnership(
        personId1: UUID,
        personId2: UUID,
        startDate: LocalDate,
        endDate: LocalDate?,
        allowConflicts: Boolean = false
    ): Partnership {
        return try {
            dao
                .createPartnership(personId1, personId2, startDate, endDate)
                .also { sendFamilyUpdatedMessage(personId2, startDate, endDate) }
        } catch (e: UnableToExecuteStatementException) {
            if (allowConflicts) {
                withSpringTx(txm, requiresNew = true) {
                    dao.createPartnership(personId1, personId2, startDate, endDate, conflict = true)
                }
            } else {
                throw mapPSQLException(e)
            }
        }
    }

    fun getPartnershipsForPerson(personId: UUID, includeConflicts: Boolean): Set<Partnership> {
        return dao.getPartnershipsForPerson(personId, includeConflicts)
    }

    fun getPartnersForPerson(personId: UUID, includeConflicts: Boolean = false): Set<Partner> {
        return dao.getPartnersForPerson(personId, includeConflicts)
    }

    fun getPartnership(partnershipId: UUID): Partnership? {
        return dao.getPartnership(partnershipId)
    }

    @Transactional
    fun updatePartnershipDuration(
        partnershipId: UUID,
        startDate: LocalDate,
        endDate: LocalDate?
    ): Partnership {
        val partnership =
            dao.getPartnership(partnershipId) ?: throw NotFound("No partnership found with id $partnershipId")
        try {
            val success = dao.updatePartnershipDuration(partnershipId, startDate, endDate)
            if (!success) throw NotFound("No partnership found with id $partnershipId")
        } catch (e: UnableToExecuteStatementException) {
            throw mapPSQLException(e)
        }

        sendFamilyUpdatedMessage(
            partnership.partners.last().id,
            minOf(startDate, partnership.startDate),
            maxEndDate(endDate, partnership.endDate)
        )

        return partnership.copy(startDate = startDate, endDate = endDate)
    }

    @Transactional
    fun retryPartnership(partnershipId: UUID) {
        try {
            getPartnership(partnershipId)
                ?.takeIf { it.conflict }
                ?.let { partnership ->
                    dao.retryPartnership(partnershipId)
                    partnership.partners.forEach { partner ->
                        sendFamilyUpdatedMessage(
                            adultId = partner.id,
                            startDate = partnership.startDate,
                            endDate = partnership.endDate
                        )
                    }
                }
            dao.retryPartnership(partnershipId)
        } catch (e: DataIntegrityViolationException) {
            throw mapPSQLException(e)
        }
    }

    @Transactional
    fun deletePartnership(partnershipId: UUID) {
        val partnership =
            dao.getPartnership(partnershipId) ?: throw NotFound("No partnership found with id $partnershipId")
        dao.deletePartnership(partnershipId)

        with(partnership) {
            partners.forEach {
                sendFamilyUpdatedMessage(
                    it.id,
                    startDate,
                    endDate
                )
            }
        }
    }

    private fun sendFamilyUpdatedMessage(adultId: UUID, startDate: LocalDate, endDate: LocalDate?) {
        logger.info("Sending update family message with adult $adultId")
        asyncJobRunner.plan(listOf(NotifyFamilyUpdated(adultId, startDate, endDate)))
        runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
    }
}

data class Partnership(
    val id: UUID,
    val partners: Set<PersonJSON>,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false
)

data class Partner(
    val partnershipId: UUID,
    val person: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false
)
