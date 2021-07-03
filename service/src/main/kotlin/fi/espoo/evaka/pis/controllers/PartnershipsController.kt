// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.noContent
import fi.espoo.evaka.pis.getPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.pis.service.PartnershipService
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.GenerateFinanceDecisions
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/partnerships")
class PartnershipsController(private val asyncJobRunner: AsyncJobRunner, private val partnershipService: PartnershipService) {
    @PostMapping
    fun createPartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: PartnershipRequest
    ): ResponseEntity<Partnership> {
        Audit.PartnerShipsCreate.log(targetId = body.personIds)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        with(body) {
            if (personIds.size != 2) throw BadRequest("Must have exactly two partners")
            val (personId1, personId2) = personIds.toList()
            return db
                .transaction { tx ->
                    val partnership = partnershipService.createPartnership(tx, personId1, personId2, startDate, endDate)
                    asyncJobRunner.plan(
                        tx,
                        listOf(GenerateFinanceDecisions.forAdult(personId2, DateRange(startDate, endDate)))
                    )
                    partnership
                }
                .also { asyncJobRunner.scheduleImmediateRun() }
                .let { ResponseEntity.created(URI.create("/partnerships/${it.id}")).body(it) }
        }
    }

    @GetMapping
    fun getPartnerships(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(name = "personId", required = true) personId: UUID
    ): ResponseEntity<List<Partnership>> {
        Audit.PartnerShipsRead.log(targetId = personId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        return db.read { it.getPartnershipsForPerson(personId, includeConflicts = true) }
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/{id}")
    fun getPartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: PartnershipId
    ): ResponseEntity<Partnership> {
        Audit.PartnerShipsRead.log(targetId = partnershipId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        return db.read { it.getPartnership(partnershipId) }
            ?.let { ResponseEntity.ok().body(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun updatePartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: PartnershipId,
        @RequestBody body: PartnershipUpdateRequest
    ): ResponseEntity<Partnership> {
        Audit.PartnerShipsUpdate.log(targetId = partnershipId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        return db
            .transaction { tx ->
                val partnership = partnershipService.updatePartnershipDuration(tx, partnershipId, body.startDate, body.endDate)
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        GenerateFinanceDecisions.forAdult(
                            partnership.partners.last().id,
                            DateRange(partnership.startDate, partnership.endDate)
                        )
                    )
                )
                partnership
            }
            .also { asyncJobRunner.scheduleImmediateRun() }
            .let { ResponseEntity.ok().body(it) }
    }

    @PutMapping("/{id}/retry")
    fun retryPartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: PartnershipId
    ): ResponseEntity<Unit> {
        Audit.PartnerShipsRetry.log(targetId = partnershipId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        db.transaction { tx ->
            partnershipService.retryPartnership(tx, partnershipId)?.let {
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        GenerateFinanceDecisions.forAdult(it.partners.first().id, DateRange(it.startDate, it.endDate))
                    )
                )
            }
        }
        asyncJobRunner.scheduleImmediateRun()
        return noContent()
    }

    @DeleteMapping("/{id}")
    fun deletePartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: PartnershipId
    ): ResponseEntity<Unit> {
        Audit.PartnerShipsDelete.log(targetId = partnershipId)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        db.transaction { tx ->
            partnershipService.deletePartnership(tx, partnershipId)?.also { partnership ->
                asyncJobRunner.plan(
                    tx,
                    partnership.partners.map {
                        GenerateFinanceDecisions.forAdult(it.id, DateRange(partnership.startDate, partnership.endDate))
                    }
                )
            }
        }
        asyncJobRunner.scheduleImmediateRun()
        return ResponseEntity.noContent().build()
    }

    data class PartnershipRequest(
        val personIds: Set<UUID>,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    data class PartnershipUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate?
    )
}
