// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.noContent
import fi.espoo.evaka.identity.VolttiIdentifier
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.ParentshipService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles.FINANCE_ADMIN
import fi.espoo.evaka.shared.config.Roles.SERVICE_WORKER
import fi.espoo.evaka.shared.config.Roles.UNIT_SUPERVISOR
import fi.espoo.evaka.shared.db.transaction
import org.jdbi.v3.core.Jdbi
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
@RequestMapping("/parentships")
class ParentshipController(private val parentshipService: ParentshipService, private val jdbi: Jdbi) {
    @PostMapping
    fun createParentship(
        user: AuthenticatedUser,
        @RequestBody body: ParentshipRequest
    ): ResponseEntity<Parentship> {
        Audit.ParentShipsCreate.log(targetId = body.headOfChildId, objectId = body.childId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        with(body) {
            return jdbi.transaction {
                parentshipService.createParentship(it, childId, headOfChildId, startDate, endDate)
                    .let { ResponseEntity.created(URI.create("/parentships/${it.id}")).body(it) }
            }
        }
    }

    @GetMapping
    fun getParentships(
        user: AuthenticatedUser,
        @RequestParam(value = "headOfChildId", required = false) headOfChildId: VolttiIdentifier? = null,
        @RequestParam(value = "childId", required = false) childId: VolttiIdentifier? = null
    ): ResponseEntity<Set<Parentship>> {
        Audit.ParentShipsRead.log(targetId = listOf(headOfChildId, childId))
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return jdbi.transaction {
            parentshipService.getParentships(
                it,
                headOfChildId = headOfChildId,
                childId = childId,
                includeConflicts = true
            )
                .let { ResponseEntity.ok().body(it) }
        }
    }

    @GetMapping("/{id}")
    fun getParentship(user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Parentship> {
        Audit.ParentShipsRead.log(targetId = id)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return jdbi.transaction {
            parentshipService.getParentship(it, id)
                ?.let { ResponseEntity.ok().body(it) }
                ?: ResponseEntity.notFound().build()
        }
    }

    @PutMapping("/{id}")
    fun updateParentship(
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID,
        @RequestBody body: ParentshipUpdateRequest
    ): ResponseEntity<Parentship> {
        Audit.ParentShipsUpdate.log(targetId = id)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        return jdbi.transaction {
            parentshipService.updateParentshipDuration(it, id, body.startDate, body.endDate)
                .let { ResponseEntity.ok().body(it) }
        }
    }

    @PutMapping("/{id}/retry")
    fun retryPartnership(
        user: AuthenticatedUser,
        @PathVariable(value = "id") parentshipId: UUID
    ): ResponseEntity<Unit> {
        Audit.ParentShipsRetry.log(targetId = parentshipId)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        jdbi.transaction { parentshipService.retryParentship(it, parentshipId) }
        return noContent()
    }

    @DeleteMapping("/{id}")
    fun deleteParentship(user: AuthenticatedUser, @PathVariable(value = "id") id: UUID): ResponseEntity<Unit> {
        Audit.ParentShipsDelete.log(targetId = id)
        user.requireOneOfRoles(SERVICE_WORKER, UNIT_SUPERVISOR, FINANCE_ADMIN)

        jdbi.transaction { parentshipService.deleteParentship(it, id) }

        return ResponseEntity.noContent().build()
    }

    data class ParentshipRequest(
        val headOfChildId: UUID,
        val childId: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    data class ParentshipUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate?
    )
}
