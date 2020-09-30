// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.noContent
import fi.espoo.evaka.identity.VolttiIdentifier
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.pis.service.PartnershipService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.domain.BadRequest
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
class PartnershipsController(private val partnershipService: PartnershipService) {
    @PostMapping
    fun createPartnership(
        user: AuthenticatedUser,
        @RequestBody body: PartnershipRequest
    ): ResponseEntity<Partnership> {
        Audit.PartnerShipsCreate.log(targetId = body.personIds)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        with(body) {
            if (personIds.size != 2) throw BadRequest("Must have exactly two partners")
            val (personId1, personId2) = personIds.toList()
            return partnershipService.createPartnership(personId1, personId2, startDate, endDate)
                .let { ResponseEntity.created(URI.create("/partnerships/${it.id}")).body(it) }
        }
    }

    @GetMapping
    fun getPartnerships(
        user: AuthenticatedUser,
        @RequestParam(name = "personId", required = true) personId: VolttiIdentifier
    ): ResponseEntity<Set<Partnership>> {
        Audit.PartnerShipsRead.log(targetId = personId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return partnershipService.getPartnershipsForPerson(personId, includeConflicts = true)
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/{id}")
    fun getPartnership(
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: UUID
    ): ResponseEntity<Partnership> {
        Audit.PartnerShipsRead.log(targetId = partnershipId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return partnershipService.getPartnership(partnershipId)
            ?.let { ResponseEntity.ok().body(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun updatePartnership(
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: UUID,
        @RequestBody body: PartnershipUpdateRequest
    ): ResponseEntity<Partnership> {
        Audit.PartnerShipsUpdate.log(targetId = partnershipId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return partnershipService.updatePartnershipDuration(partnershipId, body.startDate, body.endDate)
            .let { ResponseEntity.ok().body(it) }
    }

    @PutMapping("/{id}/retry")
    fun retryPartnership(
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: UUID
    ): ResponseEntity<Unit> {
        Audit.PartnerShipsRetry.log(targetId = partnershipId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        partnershipService.retryPartnership(partnershipId)
        return noContent()
    }

    @DeleteMapping("/{id}")
    fun deletePartnership(
        user: AuthenticatedUser,
        @PathVariable(value = "id") partnershipId: UUID
    ): ResponseEntity<Unit> {
        Audit.PartnerShipsDelete.log(targetId = partnershipId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        partnershipService.deletePartnership(partnershipId)
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
