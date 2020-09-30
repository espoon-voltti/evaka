// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.dao.ParentshipDAO
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.db.runAfterCommit
import fi.espoo.evaka.shared.db.withSpringTx
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.maxEndDate
import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional(readOnly = true)
class ParentshipService(
    private val dao: ParentshipDAO,
    private val asyncJobRunner: AsyncJobRunner,
    private val txm: PlatformTransactionManager
) {
    private val logger = KotlinLogging.logger { }

    @Transactional
    fun createParentship(
        childId: UUID,
        headOfChildId: UUID,
        startDate: LocalDate,
        endDate: LocalDate?,
        allowConflicts: Boolean = false
    ): Parentship {
        return try {
            dao
                .createParentship(childId, headOfChildId, startDate, endDate)
                .also { sendFamilyUpdatedMessage(headOfChildId, startDate, endDate) }
        } catch (e: DataIntegrityViolationException) {
            if (allowConflicts) {
                withSpringTx(txm, requiresNew = true) {
                    dao.createParentship(childId, headOfChildId, startDate, endDate, conflict = true)
                }
            } else {
                throw mapPSQLException(e)
            }
        }
    }

    fun getParentships(headOfChildId: UUID?, childId: UUID?, includeConflicts: Boolean = false): Set<Parentship> {
        return dao.getParentships(headOfChildId = headOfChildId, childId = childId, includeConflicts = includeConflicts)
    }

    fun getParentshipsByHeadOfChildId(headOfChildId: UUID, includeConflicts: Boolean = false): Set<Parentship> {
        return dao.getParentships(headOfChildId = headOfChildId, childId = null, includeConflicts = includeConflicts)
    }

    fun getParentshipsByChildId(childId: UUID, includeConflicts: Boolean = false): Set<Parentship> {
        return dao.getParentships(headOfChildId = null, childId = childId, includeConflicts = includeConflicts)
    }

    fun getParentship(id: UUID): Parentship? {
        return dao.getParentship(id)
    }

    @Transactional
    fun updateParentshipDuration(id: UUID, startDate: LocalDate, endDate: LocalDate?): Parentship {
        val oldParentship = dao.getParentship(id) ?: throw NotFound("No parentship found with id $id")
        try {
            val success = dao.updateParentshipDuration(id, startDate, endDate)
            if (!success) throw NotFound("No parentship found with id $id")
        } catch (e: DataIntegrityViolationException) {
            throw mapPSQLException(e)
        }

        sendFamilyUpdatedMessage(
            oldParentship.headOfChildId,
            minOf(startDate, oldParentship.startDate),
            maxEndDate(endDate, oldParentship.endDate)
        )

        return oldParentship.copy(startDate = startDate, endDate = endDate)
    }

    @Transactional
    fun retryParentship(id: UUID) {
        try {
            getParentship(id)
                ?.takeIf { it.conflict }
                ?.let {
                    dao.retryParentship(it.id)
                    sendFamilyUpdatedMessage(
                        adultId = it.headOfChildId,
                        startDate = it.startDate,
                        endDate = it.endDate
                    )
                }
        } catch (e: DataIntegrityViolationException) {
            throw mapPSQLException(e)
        }
    }

    @Transactional
    fun deleteParentship(id: UUID) {
        val parentship = dao.getParentship(id)
        val success = dao.deleteParentship(id)
        if (parentship == null || !success) throw NotFound("No parentship found with id $id")

        with(parentship) {
            sendFamilyUpdatedMessage(
                headOfChildId,
                startDate,
                endDate
            )
        }
    }

    private fun sendFamilyUpdatedMessage(adultId: UUID, startDate: LocalDate, endDate: LocalDate?) {
        logger.info("Sending update family message with adult $adultId")
        asyncJobRunner.plan(listOf(NotifyFamilyUpdated(adultId, startDate, endDate)))
        runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
    }
}

data class Parentship(
    val id: UUID,
    val childId: UUID,
    val child: PersonJSON,
    val headOfChildId: UUID,
    val headOfChild: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val conflict: Boolean = false
)
