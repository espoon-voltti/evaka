// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.dao.mapPSQLException
import fi.espoo.evaka.pis.deleteParentship
import fi.espoo.evaka.pis.getParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.retryParentship
import fi.espoo.evaka.pis.updateParentshipDuration
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyFamilyUpdated
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.maxEndDate
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ParentshipService(private val asyncJobRunner: AsyncJobRunner) {
    private val logger = KotlinLogging.logger { }

    fun createParentship(
        h: Handle,
        childId: UUID,
        headOfChildId: UUID,
        startDate: LocalDate,
        endDate: LocalDate?
    ): Parentship {
        return try {
            h.createParentship(childId, headOfChildId, startDate, endDate, false)
                .also { sendFamilyUpdatedMessage(h, headOfChildId, startDate, endDate) }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun getParentships(
        h: Handle,
        headOfChildId: UUID?,
        childId: UUID?,
        includeConflicts: Boolean = false
    ): Set<Parentship> {
        return h.getParentships(headOfChildId = headOfChildId, childId = childId, includeConflicts = includeConflicts)
            .toSet()
    }

    fun getParentshipsByHeadOfChildId(
        h: Handle,
        headOfChildId: UUID,
        includeConflicts: Boolean = false
    ): Set<Parentship> {
        return h.getParentships(headOfChildId = headOfChildId, childId = null, includeConflicts = includeConflicts)
            .toSet()
    }

    fun getParentshipsByChildId(h: Handle, childId: UUID, includeConflicts: Boolean = false): Set<Parentship> {
        return h.getParentships(headOfChildId = null, childId = childId, includeConflicts = includeConflicts).toSet()
    }

    fun getParentship(h: Handle, id: UUID): Parentship? {
        return h.getParentship(id)
    }

    fun updateParentshipDuration(h: Handle, id: UUID, startDate: LocalDate, endDate: LocalDate?): Parentship {
        val oldParentship = h.getParentship(id) ?: throw NotFound("No parentship found with id $id")
        try {
            val success = h.updateParentshipDuration(id, startDate, endDate)
            if (!success) throw NotFound("No parentship found with id $id")
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }

        sendFamilyUpdatedMessage(
            h,
            oldParentship.headOfChildId,
            minOf(startDate, oldParentship.startDate),
            maxEndDate(endDate, oldParentship.endDate)
        )

        return oldParentship.copy(startDate = startDate, endDate = endDate)
    }

    fun retryParentship(h: Handle, id: UUID) {
        try {
            h.getParentship(id)
                ?.takeIf { it.conflict }
                ?.let {
                    h.retryParentship(it.id)
                    sendFamilyUpdatedMessage(
                        h,
                        adultId = it.headOfChildId,
                        startDate = it.startDate,
                        endDate = it.endDate
                    )
                }
        } catch (e: Exception) {
            throw mapPSQLException(e)
        }
    }

    fun deleteParentship(h: Handle, id: UUID) {
        val parentship = h.getParentship(id)
        val success = h.deleteParentship(id)
        if (parentship == null || !success) throw NotFound("No parentship found with id $id")

        with(parentship) {
            sendFamilyUpdatedMessage(
                h,
                headOfChildId,
                startDate,
                endDate
            )
        }
    }

    private fun sendFamilyUpdatedMessage(h: Handle, adultId: UUID, startDate: LocalDate, endDate: LocalDate?) {
        logger.info("Sending update family message with adult $adultId")
        asyncJobRunner.plan(h, listOf(NotifyFamilyUpdated(adultId, startDate, endDate)))
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
