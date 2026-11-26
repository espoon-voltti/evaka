// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.family

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.shared.PartnershipId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.domain.maxEndDate
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
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
@RequestMapping("/employee/partnerships")
class PartnershipsController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val partnershipService: PartnershipService,
    private val accessControl: AccessControl,
) {
    @PostMapping
    fun createPartnership(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: PartnershipRequest,
    ) {
        val audit =
            AuditContext().add(body.person1Id).add(body.person2Id).observeDate(body.startDate)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.CREATE_PARTNERSHIP,
                        body.person1Id,
                    )
                    partnershipService
                        .createPartnership(
                            tx,
                            body.person1Id,
                            body.person2Id,
                            body.startDate,
                            body.endDate,
                            user.evakaUserId,
                            clock.now(),
                        )
                        .also { partnership ->
                            audit.add(partnership.id)
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.GenerateFinanceDecisions.forAdult(
                                        body.person1Id,
                                        DateRange(body.startDate, body.endDate),
                                    ),
                                ),
                                runAt = clock.now(),
                            )
                        }
                }
            }
            .also { audit.log(Audit.PartnerShipsCreate, clock) }
    }

    @GetMapping
    fun getPartnerships(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam personId: PersonId,
    ): List<PartnershipWithPermittedActions> {
        val audit = AuditContext().add(personId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Person.READ_PARTNERSHIPS,
                        personId,
                    )
                    val partnerships =
                        tx.getPartnershipsForPerson(personId, includeConflicts = true)
                    audit
                        .add(partnerships.map { it.id })
                        .add(partnerships.flatMap { p -> p.partners.map { it.id } })

                    val permittedActions =
                        accessControl.getPermittedActions<PartnershipId, Action.Partnership>(
                            tx,
                            user,
                            clock,
                            partnerships.map { it.id },
                        )

                    partnerships.map {
                        PartnershipWithPermittedActions(
                            data = it,
                            permittedActions = permittedActions[it.id] ?: emptySet(),
                        )
                    }
                }
            }
            .also { audit.log(Audit.PartnerShipsRead, clock) }
    }

    @PutMapping("/{partnershipId}")
    fun updatePartnership(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId,
        @RequestBody body: PartnershipUpdateRequest,
    ) {
        val audit =
            AuditContext()
                .add(partnershipId)
                .observeDate(body.startDate)
                .addMeta("startDate", body.startDate)
                .addMeta("endDate", body.endDate)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Partnership.UPDATE,
                        partnershipId,
                    )
                    val oldPartnership =
                        tx.getPartnership(partnershipId)
                            ?: throw NotFound("No partnership found with id $partnershipId")
                    audit
                        .add(oldPartnership.partners.map { it.id })
                        .observeDate(oldPartnership.startDate)
                    partnershipService.updatePartnershipDuration(
                        tx,
                        partnershipId,
                        body.startDate,
                        body.endDate,
                        user.evakaUserId,
                        clock.now(),
                    )
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forAdult(
                                oldPartnership.partners.first().id,
                                DateRange(
                                    minOf(oldPartnership.startDate, body.startDate),
                                    maxEndDate(oldPartnership.endDate, body.endDate),
                                ),
                            ),
                        ),
                        runAt = clock.now(),
                    )
                }
            }
            .also { audit.log(Audit.PartnerShipsUpdate, clock) }
    }

    @PutMapping("/{partnershipId}/retry")
    fun retryPartnership(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId,
    ) {
        val audit = AuditContext().add(partnershipId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Partnership.RETRY,
                        partnershipId,
                    )
                    partnershipService
                        .retryPartnership(tx, partnershipId, user.evakaUserId, clock.now())
                        ?.let {
                            audit.add(it.partners.map { p -> p.id }).observeDate(it.startDate)
                            asyncJobRunner.plan(
                                tx,
                                listOf(
                                    AsyncJob.GenerateFinanceDecisions.forAdult(
                                        it.partners.first().id,
                                        DateRange(it.startDate, it.endDate),
                                    ),
                                ),
                                runAt = clock.now(),
                            )
                        }
                }
            }
            .also { audit.log(Audit.PartnerShipsRetry, clock) }
    }

    @DeleteMapping("/{partnershipId}")
    fun deletePartnership(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable partnershipId: PartnershipId,
    ) {
        val audit = AuditContext().add(partnershipId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Partnership.DELETE,
                        partnershipId,
                    )
                    partnershipService.deletePartnership(tx, partnershipId)?.also { partnership ->
                        audit
                            .add(partnership.partners.map { it.id })
                            .observeDate(partnership.startDate)
                            .addMeta("startDate", partnership.startDate)
                            .addMeta("endDate", partnership.endDate)
                        asyncJobRunner.plan(
                            tx,
                            partnership.partners.map {
                                AsyncJob.GenerateFinanceDecisions.forAdult(
                                    it.id,
                                    DateRange(partnership.startDate, partnership.endDate),
                                )
                            },
                            runAt = clock.now(),
                        )
                    }
                }
            }
            .also { audit.log(Audit.PartnerShipsDelete, clock) }
    }

    data class PartnershipRequest(
        val person1Id: PersonId,
        val person2Id: PersonId,
        val startDate: LocalDate,
        val endDate: LocalDate?,
    )

    data class PartnershipUpdateRequest(val startDate: LocalDate, val endDate: LocalDate?)

    data class PartnershipWithPermittedActions(
        val data: Partnership,
        val permittedActions: Set<Action.Partnership>,
    )
}
