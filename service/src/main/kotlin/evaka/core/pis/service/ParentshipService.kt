// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.pis.service

import evaka.core.pis.CreationModificationMetadata
import evaka.core.pis.Creator
import evaka.core.pis.Modifier
import evaka.core.pis.createParentship
import evaka.core.pis.deleteParentship
import evaka.core.pis.getParentship
import evaka.core.pis.getPersonById
import evaka.core.pis.retryParentship
import evaka.core.pis.updateParentshipDuration
import evaka.core.shared.ChildId
import evaka.core.shared.ParentshipId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.db.mapPSQLException
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import org.springframework.stereotype.Service

@Service
class ParentshipService(private val asyncJobRunner: AsyncJobRunner<AsyncJob>) {
    private val logger = KotlinLogging.logger {}

    fun createParentship(
        tx: Database.Transaction,
        clock: EvakaClock,
        childId: ChildId,
        headOfChildId: PersonId,
        startDate: LocalDate,
        endDate: LocalDate,
        creator: Creator,
    ): Parentship {
        tx.getPersonById(childId)?.let { child ->
            validateDates(child.dateOfBirth, startDate, endDate)
        }
        return try {
            tx.createParentship(childId, headOfChildId, startDate, endDate, creator, false).also {
                tx.sendFamilyUpdatedMessage(clock, headOfChildId, startDate, endDate)
            }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun updateParentshipDuration(
        tx: Database.Transaction,
        clock: EvakaClock,
        user: AuthenticatedUser.Employee,
        id: ParentshipId,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Parentship {
        val oldParentship =
            tx.getParentship(id) ?: throw NotFound("No parentship found with id $id")
        validateDates(oldParentship.child.dateOfBirth, startDate, endDate)
        try {
            val success =
                tx.updateParentshipDuration(
                    id = id,
                    startDate = startDate,
                    endDate = endDate,
                    now = clock.now(),
                    modifier = Modifier.User(user.evakaUserId),
                )
            if (!success) throw NotFound("No parentship found with id $id")
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }

        tx.sendFamilyUpdatedMessage(
            clock,
            oldParentship.headOfChildId,
            minOf(startDate, oldParentship.startDate),
            maxOf(endDate, oldParentship.endDate),
        )

        return oldParentship.copy(startDate = startDate, endDate = endDate)
    }

    fun retryParentship(
        tx: Database.Transaction,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        id: ParentshipId,
    ) {
        try {
            tx.getParentship(id)
                ?.takeIf { it.conflict }
                ?.let {
                    tx.retryParentship(id = it.id, now = clock.now(), userId = user.evakaUserId)
                    tx.sendFamilyUpdatedMessage(
                        clock,
                        adultId = it.headOfChildId,
                        startDate = it.startDate,
                        endDate = it.endDate,
                    )
                }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun deleteParentship(tx: Database.Transaction, clock: EvakaClock, id: ParentshipId) {
        val parentship = tx.getParentship(id)
        val success = tx.deleteParentship(id)
        if (parentship == null || !success) throw NotFound("No parentship found with id $id")

        with(parentship) { tx.sendFamilyUpdatedMessage(clock, headOfChildId, startDate, endDate) }
    }

    private fun Database.Transaction.sendFamilyUpdatedMessage(
        clock: EvakaClock,
        adultId: PersonId,
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        logger.info { "Sending update family message with adult $adultId" }
        asyncJobRunner.plan(
            this,
            listOf(
                AsyncJob.GenerateFinanceDecisions.forAdult(adultId, DateRange(startDate, endDate))
            ),
            runAt = clock.now(),
        )
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
    val childId: ChildId,
    val child: PersonJSON,
    val headOfChildId: PersonId,
    val headOfChild: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val conflict: Boolean = false,
)

data class ParentshipDetailed(
    val id: ParentshipId,
    val childId: ChildId,
    val child: PersonJSON,
    val headOfChildId: PersonId,
    val headOfChild: PersonJSON,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val conflict: Boolean = false,
    val creationModificationMetadata: CreationModificationMetadata,
) {
    fun withoutDetails() =
        Parentship(
            id = id,
            childId = childId,
            child = child,
            headOfChildId = headOfChildId,
            headOfChild = headOfChild,
            startDate = startDate,
            endDate = endDate,
            conflict = conflict,
        )
}
