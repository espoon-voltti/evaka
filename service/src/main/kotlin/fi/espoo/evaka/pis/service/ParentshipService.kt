// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.deleteParentship
import fi.espoo.evaka.pis.getParentship
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.retryParentship
import fi.espoo.evaka.pis.updateParentshipDuration
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GenerateFinanceDecisions
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapPSQLException
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ParentshipService(private val asyncJobRunner: AsyncJobRunner) {
    private val logger = KotlinLogging.logger { }

    fun createParentship(
        tx: Database.Transaction,
        childId: UUID,
        headOfChildId: UUID,
        startDate: LocalDate,
        endDate: LocalDate
    ): Parentship {
        tx.getPersonById(childId)?.let { child -> validateDates(child.dateOfBirth, startDate, endDate) }
        return try {
            tx.createParentship(childId, headOfChildId, startDate, endDate, false)
                .also { tx.sendFamilyUpdatedMessage(headOfChildId, startDate, endDate) }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun updateParentshipDuration(tx: Database.Transaction, id: ParentshipId, startDate: LocalDate, endDate: LocalDate): Parentship {
        val oldParentship = tx.getParentship(id) ?: throw NotFound("No parentship found with id $id")
        validateDates(oldParentship.child.dateOfBirth, startDate, endDate)
        try {
            val success = tx.updateParentshipDuration(id, startDate, endDate)
            if (!success) throw NotFound("No parentship found with id $id")
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }

        tx.sendFamilyUpdatedMessage(
            oldParentship.headOfChildId,
            minOf(startDate, oldParentship.startDate),
            maxOf(endDate, oldParentship.endDate)
        )

        return oldParentship.copy(startDate = startDate, endDate = endDate)
    }

    fun retryParentship(tx: Database.Transaction, id: ParentshipId) {
        try {
            tx.getParentship(id)
                ?.takeIf { it.conflict }
                ?.let {
                    tx.retryParentship(it.id)
                    tx.sendFamilyUpdatedMessage(
                        adultId = it.headOfChildId,
                        startDate = it.startDate,
                        endDate = it.endDate
                    )
                }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun deleteParentship(tx: Database.Transaction, id: ParentshipId) {
        val parentship = tx.getParentship(id)
        val success = tx.deleteParentship(id)
        if (parentship == null || !success) throw NotFound("No parentship found with id $id")

        with(parentship) {
            tx.sendFamilyUpdatedMessage(headOfChildId, startDate, endDate)
        }
    }

    private fun Database.Transaction.sendFamilyUpdatedMessage(adultId: UUID, startDate: LocalDate, endDate: LocalDate) {
        logger.info("Sending update family message with adult $adultId")
        asyncJobRunner.plan(this, listOf(GenerateFinanceDecisions.forAdult(adultId, DateRange(startDate, endDate))))
    }
}

private fun validateDates(childDateOfBirth: LocalDate, startDate: LocalDate, endDate: LocalDate) {
    if (startDate < childDateOfBirth) {
        throw BadRequest("Parentship start date cannot be before child's date of birth")
    }

    if (childDateOfBirth.plusYears(18) <= endDate) {
        throw BadRequest("Parentship end date cannot be at or after child's 18th birthday")
    }
}

data class Parentship(
    val id: ParentshipId,
    val childId: UUID,
    val child: PersonJSON,
    val headOfChildId: UUID,
    val headOfChild: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val conflict: Boolean = false
)
