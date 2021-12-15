// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.controllers

import fi.espoo.evaka.Audit
import fi.espoo.evaka.pis.getPartnership
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.pis.service.Partnership
import fi.espoo.evaka.pis.service.PartnershipService
import fi.espoo.evaka.shared.PartnershipId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/partnerships")
class PartnershipsController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val partnershipService: PartnershipService,
    private val accessControl: AccessControl
) {
    @PostMapping
    fun createPartnership(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @RequestBody body: PartnershipRequest
    ) {
        Audit.PartnerShipsCreate.log(targetId = body.person1Id)
        accessControl.requirePermissionFor(user, Action.Person.CREATE_PARTNERSHIP, body.person1Id)

        db
            .transaction { tx ->
                partnershipService.createPartnership(
                    tx,
                    body.person1Id.raw,
                    body.person2Id.raw,
                    body.startDate,
                    body.endDate
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            body.person1Id.raw,
                            DateRange(body.startDate, body.endDate)
                        )
                    )
                )
            }
    }

    @GetMapping
    fun getPartnerships(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @RequestParam personId: PersonId
    ): List<Partnership> {
        Audit.PartnerShipsRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_PARTNERSHIPS, personId)

        return db.read { it.getPartnershipsForPerson(personId.raw, includeConflicts = true) }
    }

    @GetMapping("/{partnershipId}")
    fun getPartnership(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable partnershipId: PartnershipId
    ): Partnership {
        Audit.PartnerShipsRead.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, Action.Partnership.READ, partnershipId)

        return db.read { it.getPartnership(partnershipId) }
            ?: throw NotFound()
    }

    @PutMapping("/{partnershipId}")
    fun updatePartnership(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable partnershipId: PartnershipId,
        @RequestBody body: PartnershipUpdateRequest
    ) {
        Audit.PartnerShipsUpdate.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, Action.Partnership.UPDATE, partnershipId)

        return db
            .transaction { tx ->
                val partnership =
                    partnershipService.updatePartnershipDuration(tx, partnershipId, body.startDate, body.endDate)
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            partnership.partners.first().id,
                            DateRange(partnership.startDate, partnership.endDate)
                        )
                    )
                )
            }
    }

    @PutMapping("/{partnershipId}/retry")
    fun retryPartnership(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable partnershipId: PartnershipId
    ) {
        Audit.PartnerShipsRetry.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, Action.Partnership.RETRY, partnershipId)

        db.transaction { tx ->
            partnershipService.retryPartnership(tx, partnershipId)?.let {
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            it.partners.first().id,
                            DateRange(it.startDate, it.endDate)
                        )
                    )
                )
            }
        }
    }

    @DeleteMapping("/{partnershipId}")
    fun deletePartnership(
        db: Database.DeprecatedConnection,
        user: AuthenticatedUser,
        @PathVariable partnershipId: PartnershipId
    ) {
        Audit.PartnerShipsDelete.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, Action.Partnership.DELETE, partnershipId)

        db.transaction { tx ->
            partnershipService.deletePartnership(tx, partnershipId)?.also { partnership ->
                asyncJobRunner.plan(
                    tx,
                    partnership.partners.map {
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            it.id,
                            DateRange(partnership.startDate, partnership.endDate)
                        )
                    }
                )
            }
        }
    }

    data class PartnershipRequest(
        val person1Id: PersonId,
        val person2Id: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    data class PartnershipUpdateRequest(
        val startDate: LocalDate,
        val endDate: LocalDate?
    )
}
