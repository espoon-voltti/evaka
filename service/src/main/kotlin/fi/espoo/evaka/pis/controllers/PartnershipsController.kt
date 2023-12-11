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
import fi.espoo.evaka.shared.domain.maxEndDate
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
        val partnership =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_PARTNERSHIP,
                        body.person1Id
                    )
                    partnershipService
                        .createPartnership(
                            tx,
                            body.person1Id,
                            body.person2Id,
                            body.startDate,
                            body.endDate,
                            user.evakaUserId,
                            clock.now()
                        )
                        .also {
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
        Audit.PartnerShipsCreate.log(
            targetId = listOf(body.person1Id, body.person2Id),
            objectId = partnership.id
        )
    }

    @GetMapping
    fun getPartnerships(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam personId: PersonId
    ): List<PartnershipWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_PARTNERSHIPS,
                        personId
                    )
                    val partnerships =
                        tx.getPartnershipsForPerson(personId, includeConflicts = true)

                    val permittedActions =
                        accessControl.getPermittedActions<PartnershipId, Action.Partnership>(
                            tx,
                            user,
                            clock,
                            partnerships.map { it.id }
                        )

                    partnerships.map {
                        PartnershipWithPermittedActions(
                            data = it,
                            permittedActions = permittedActions[it.id] ?: emptySet()
                        )
                    }
                }
            }
            .also {
                Audit.PartnerShipsRead.log(targetId = personId, meta = mapOf("count" to it.size))
            }
    }

    @GetMapping("/{partnershipId}")
    fun getPartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId
    ): Partnership {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Partnership.READ,
                        partnershipId
                    )
                    it.getPartnership(partnershipId)
                } ?: throw NotFound()
            }
            .also { Audit.PartnerShipsRead.log(targetId = partnershipId) }
    }

    @PutMapping("/{partnershipId}")
    fun updatePartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId,
        @RequestBody body: PartnershipUpdateRequest
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Partnership.UPDATE,
                    partnershipId
                )
                val oldPartnership =
                    tx.getPartnership(partnershipId)
                        ?: throw NotFound("No partnership found with id $partnershipId")
                partnershipService.updatePartnershipDuration(
                    tx,
                    partnershipId,
                    body.startDate,
                    body.endDate,
                    user.evakaUserId,
                    clock.now()
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            oldPartnership.partners.first().id,
                            DateRange(
                                minOf(oldPartnership.startDate, body.startDate),
                                maxEndDate(oldPartnership.endDate, body.endDate)
                            )
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
        Audit.PartnerShipsUpdate.log(
            targetId = partnershipId,
            meta = mapOf("startDate" to body.startDate, "endDate" to body.endDate)
        )
    }

    @PutMapping("/{partnershipId}/retry")
    fun retryPartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Partnership.RETRY,
                    partnershipId
                )
                partnershipService
                    .retryPartnership(tx, partnershipId, user.evakaUserId, clock.now())
                    ?.let {
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
        Audit.PartnerShipsRetry.log(targetId = partnershipId)
    }

    @DeleteMapping("/{partnershipId}")
    fun deletePartnership(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Partnership.DELETE,
                    partnershipId
                )
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
        Audit.PartnerShipsDelete.log(targetId = partnershipId)
    }

    data class PartnershipRequest(
        val person1Id: PersonId,
        val person2Id: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    data class PartnershipUpdateRequest(val startDate: LocalDate, val endDate: LocalDate?)

    data class PartnershipWithPermittedActions(
        val data: Partnership,
        val permittedActions: Set<Action.Partnership>
    )
}
