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
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/partnerships")
class PartnershipsController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val partnershipService: PartnershipService,
    private val accessControl: AccessControl
) {
    @PostMapping
    fun createPartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody body: PartnershipRequest
    ) {
        Audit.PartnerShipsCreate.log(targetId = body.person1Id)
        accessControl.requirePermissionFor(
            user,
            clock,
            Action.Person.CREATE_PARTNERSHIP,
            body.person1Id
        )

        db.connect { dbc ->
            dbc.transaction { tx ->
                partnershipService.createPartnership(
                    tx,
                    body.person1Id,
                    body.person2Id,
                    body.startDate,
                    body.endDate
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            body.person1Id,
                            DateRange(body.startDate, body.endDate)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }

    @GetMapping
    fun getPartnerships(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam personId: PersonId
    ): List<Partnership> {
        Audit.PartnerShipsRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, clock, Action.Person.READ_PARTNERSHIPS, personId)

        return db.connect { dbc ->
            dbc.read { it.getPartnershipsForPerson(personId, includeConflicts = true) }
        }
    }

    @GetMapping("/{partnershipId}")
    fun getPartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId
    ): Partnership {
        Audit.PartnerShipsRead.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, clock, Action.Partnership.READ, partnershipId)

        return db.connect { dbc -> dbc.read { it.getPartnership(partnershipId) } }
            ?: throw NotFound()
    }

    @PutMapping("/{partnershipId}")
    fun updatePartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId,
        @RequestBody body: PartnershipUpdateRequest
    ) {
        Audit.PartnerShipsUpdate.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, clock, Action.Partnership.UPDATE, partnershipId)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                val partnership =
                    partnershipService.updatePartnershipDuration(
                        tx,
                        partnershipId,
                        body.startDate,
                        body.endDate
                    )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            partnership.partners.first().id,
                            DateRange(partnership.startDate, partnership.endDate)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }

    @PutMapping("/{partnershipId}/retry")
    fun retryPartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId
    ) {
        Audit.PartnerShipsRetry.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, clock, Action.Partnership.RETRY, partnershipId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                partnershipService.retryPartnership(tx, partnershipId)?.let {
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forAdult(
                                it.partners.first().id,
                                DateRange(it.startDate, it.endDate)
                            )
                        ),
                        runAt = clock.now()
                    )
                }
            }
        }
    }

    @DeleteMapping("/{partnershipId}")
    fun deletePartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId
    ) {
        Audit.PartnerShipsDelete.log(targetId = partnershipId)
        accessControl.requirePermissionFor(user, clock, Action.Partnership.DELETE, partnershipId)

        db.connect { dbc ->
            dbc.transaction { tx ->
                partnershipService.deletePartnership(tx, partnershipId)?.also { partnership ->
                    asyncJobRunner.plan(
                        tx,
                        partnership.partners.map {
                            AsyncJob.GenerateFinanceDecisions.forAdult(
                                it.id,
                                DateRange(partnership.startDate, partnership.endDate)
                            )
                        },
                        runAt = clock.now()
                    )
                }
            }
        }
    }

    data class PartnershipRequest(
        val person1Id: PersonId,
        val person2Id: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    data class PartnershipUpdateRequest(val startDate: LocalDate, val endDate: LocalDate?)
}
