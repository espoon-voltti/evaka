// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.invoicing.data.deleteFeeAlteration
import fi.espoo.evaka.invoicing.data.getFeeAlteration
import fi.espoo.evaka.invoicing.data.getFeeAlterationsForPerson
import fi.espoo.evaka.invoicing.data.upsertFeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.maxEndDate
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
import java.util.UUID

@RestController
@RequestMapping("/fee-alterations")
class FeeAlterationController(private val asyncJobRunner: AsyncJobRunner<AsyncJob>, private val accessControl: AccessControl) {
    @GetMapping
    fun getFeeAlterations(db: Database, user: AuthenticatedUser, clock: EvakaClock, @RequestParam personId: PersonId): List<FeeAlterationWithPermittedActions> {
        Audit.ChildFeeAlterationsRead.log(targetId = personId)
        accessControl.requirePermissionFor(user, clock, Action.Child.READ_FEE_ALTERATIONS, personId)
        return db.connect { dbc ->
            dbc.read { tx ->
                val feeAlterations = tx.getFeeAlterationsForPerson(personId)
                val permittedActions =
                    accessControl.getPermittedActions<FeeAlterationId, Action.FeeAlteration>(
                        tx, user, clock,
                        feeAlterations.mapNotNull { it.id }
                    )
                feeAlterations.map { FeeAlterationWithPermittedActions(it, permittedActions[it.id] ?: emptySet()) }
            }
        }
    }

    data class FeeAlterationWithPermittedActions(
        val data: FeeAlteration,
        val permittedActions: Set<Action.FeeAlteration>
    )

    @PostMapping
    fun createFeeAlteration(db: Database, user: AuthenticatedUser, clock: EvakaClock, @RequestBody feeAlteration: FeeAlteration) {
        Audit.ChildFeeAlterationsCreate.log(targetId = feeAlteration.personId)
        accessControl.requirePermissionFor(user, clock, Action.Child.CREATE_FEE_ALTERATION, feeAlteration.personId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.upsertFeeAlteration(clock, feeAlteration.copy(id = FeeAlterationId(UUID.randomUUID()), updatedBy = user.evakaUserId))
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            feeAlteration.personId,
                            DateRange(feeAlteration.validFrom, feeAlteration.validTo)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }

    @PutMapping("/{feeAlterationId}")
    fun updateFeeAlteration(db: Database, user: AuthenticatedUser, clock: EvakaClock, @PathVariable feeAlterationId: FeeAlterationId, @RequestBody feeAlteration: FeeAlteration) {
        Audit.ChildFeeAlterationsUpdate.log(targetId = feeAlterationId)
        accessControl.requirePermissionFor(user, clock, Action.FeeAlteration.UPDATE, feeAlterationId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val existing = tx.getFeeAlteration(feeAlterationId)
                tx.upsertFeeAlteration(clock, feeAlteration.copy(id = feeAlterationId, updatedBy = user.evakaUserId))

                val expandedPeriod = existing?.let {
                    DateRange(minOf(it.validFrom, feeAlteration.validFrom), maxEndDate(it.validTo, feeAlteration.validTo))
                } ?: DateRange(feeAlteration.validFrom, feeAlteration.validTo)

                asyncJobRunner.plan(tx, listOf(AsyncJob.GenerateFinanceDecisions.forChild(feeAlteration.personId, expandedPeriod)), runAt = clock.now())
            }
        }
    }

    @DeleteMapping("/{feeAlterationId}")
    fun deleteFeeAlteration(db: Database, user: AuthenticatedUser, clock: EvakaClock, @PathVariable feeAlterationId: FeeAlterationId) {
        Audit.ChildFeeAlterationsDelete.log(targetId = feeAlterationId)
        accessControl.requirePermissionFor(user, clock, Action.FeeAlteration.DELETE, feeAlterationId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val existing = tx.getFeeAlteration(feeAlterationId)
                tx.deleteFeeAlteration(feeAlterationId)

                existing?.let {
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(
                                existing.personId,
                                DateRange(existing.validFrom, existing.validTo)
                            )
                        ),
                        runAt = clock.now()
                    )
                }
            }
        }
    }
}
