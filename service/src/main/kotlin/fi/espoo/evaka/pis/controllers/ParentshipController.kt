// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.noContent
import fi.espoo.evaka.pis.getParentship
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.service.Parentship
import fi.espoo.evaka.pis.service.ParentshipService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
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
class ParentshipController(
    private val parentshipService: ParentshipService,
    private val asyncJobRunner: AsyncJobRunner
) {
    @PostMapping
    fun createParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: ParentshipRequest
    ): ResponseEntity<Parentship> {
        Audit.ParentShipsCreate.log(targetId = body.headOfChildId, objectId = body.childId)
        user.requireOneOfRoles(
            UserRole.ADMIN,
            UserRole.SERVICE_WORKER,
            UserRole.UNIT_SUPERVISOR,
            UserRole.FINANCE_ADMIN
        )

        with(body) {
            return db.transaction {
                parentshipService.createParentship(it, childId, headOfChildId, startDate, endDate)
            }
                .also { asyncJobRunner.scheduleImmediateRun() }
                .let { ResponseEntity.created(URI.create("/parentships/${it.id}")).body(it) }
        }
    }

    @GetMapping
    fun getParentships(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam(value = "headOfChildId", required = false) headOfChildId: UUID? = null,
        @RequestParam(value = "childId", required = false) childId: UUID? = null
    ): ResponseEntity<List<Parentship>> {
        Audit.ParentShipsRead.log(targetId = listOf(headOfChildId, childId))
        user.requireOneOfRoles(
            UserRole.ADMIN,
            UserRole.SERVICE_WORKER,
            UserRole.UNIT_SUPERVISOR,
            UserRole.FINANCE_ADMIN
        )

        return db.read {
            it.handle.getParentships(
                headOfChildId = headOfChildId,
                childId = childId,
                includeConflicts = true
            )
        }
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/{id}")
    fun getParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID
    ): ResponseEntity<Parentship> {
        Audit.ParentShipsRead.log(targetId = id)
        user.requireOneOfRoles(UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR, UserRole.FINANCE_ADMIN)

        return db.read {
            it.handle.getParentship(id)
        }
            ?.let { ResponseEntity.ok().body(it) }
            ?: ResponseEntity.notFound().build()
    }

    @PutMapping("/{id}")
    fun updateParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID,
        @RequestBody body: ParentshipUpdateRequest
    ): ResponseEntity<Parentship> {
        Audit.ParentShipsUpdate.log(targetId = id)
        user.requireOneOfRoles(
            UserRole.ADMIN,
            UserRole.SERVICE_WORKER,
            UserRole.UNIT_SUPERVISOR,
            UserRole.FINANCE_ADMIN
        )

        return db.transaction {
            parentshipService.updateParentshipDuration(it, id, body.startDate, body.endDate)
        }
            .also { asyncJobRunner.scheduleImmediateRun() }
            .let { ResponseEntity.ok().body(it) }
    }

    @PutMapping("/{id}/retry")
    fun retryPartnership(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") parentshipId: UUID
    ): ResponseEntity<Unit> {
        Audit.ParentShipsRetry.log(targetId = parentshipId)
        user.requireOneOfRoles(
            UserRole.ADMIN,
            UserRole.SERVICE_WORKER,
            UserRole.UNIT_SUPERVISOR,
            UserRole.FINANCE_ADMIN
        )

        db.transaction { parentshipService.retryParentship(it, parentshipId) }
        asyncJobRunner.scheduleImmediateRun()
        return noContent()
    }

    @DeleteMapping("/{id}")
    fun deleteParentship(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable(value = "id") id: UUID
    ): ResponseEntity<Unit> {
        Audit.ParentShipsDelete.log(targetId = id)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN, UserRole.UNIT_SUPERVISOR)

        db.transaction {
            if (it.handle.getParentship(id)?.conflict == false) {
                user.requireOneOfRoles(UserRole.ADMIN, UserRole.FINANCE_ADMIN)
            }

            parentshipService.deleteParentship(it, id)
        }

        asyncJobRunner.scheduleImmediateRun()

        return ResponseEntity.noContent().build()
    }

    data class ParentshipRequest(
        val headOfChildId: UUID,
        val childId: UUID,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    data class ParentshipUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate
    )
}
