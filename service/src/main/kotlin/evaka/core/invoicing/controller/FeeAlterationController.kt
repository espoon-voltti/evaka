// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.controller

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.associateOrphanAttachments
import evaka.core.invoicing.data.deleteFeeAlteration
import evaka.core.invoicing.data.getFeeAlteration
import evaka.core.invoicing.data.getFeeAlterationsForPerson
import evaka.core.invoicing.data.upsertFeeAlteration
import evaka.core.invoicing.domain.FeeAlteration
import evaka.core.shared.FeeAlterationId
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.maxEndDate
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.util.UUID
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
@RequestMapping("/employee/fee-alterations")
class FeeAlterationController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl,
) {
    @GetMapping
    fun getFeeAlterations(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam personId: PersonId,
    ): List<FeeAlterationWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_FEE_ALTERATIONS,
                        personId,
                    )
                    val feeAlterations = tx.getFeeAlterationsForPerson(personId)
                    val permittedActions =
                        accessControl.getPermittedActions<FeeAlterationId, Action.FeeAlteration>(
                            tx,
                            user,
                            clock,
                            feeAlterations.mapNotNull { it.id },
                        )
                    feeAlterations.map {
                        FeeAlterationWithPermittedActions(it, permittedActions[it.id] ?: emptySet())
                    }
                }
            }
            .also {
                Audit.ChildFeeAlterationsRead.log(
                    targetId = AuditId(personId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    data class FeeAlterationWithPermittedActions(
        val data: FeeAlteration,
        val permittedActions: Set<Action.FeeAlteration>,
    )

    @PostMapping
    fun createFeeAlteration(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody feeAlteration: FeeAlteration,
    ) {
        val id = FeeAlterationId(UUID.randomUUID())
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Child.CREATE_FEE_ALTERATION,
                    feeAlteration.personId,
                )
                tx.upsertFeeAlteration(
                    clock = clock,
                    modifiedBy = user.evakaUserId,
                    feeAlteration = feeAlteration.copy(id = id),
                )
                tx.associateOrphanAttachments(
                    user.evakaUserId,
                    AttachmentParent.FeeAlteration(id),
                    feeAlteration.attachments.map { it.id },
                )
                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            feeAlteration.personId,
                            DateRange(feeAlteration.validFrom, feeAlteration.validTo),
                        )
                    ),
                    runAt = clock.now(),
                )
            }
        }
        Audit.ChildFeeAlterationsCreate.log(
            targetId = AuditId(feeAlteration.personId),
            objectId = AuditId(id),
        )
    }

    @PutMapping("/{feeAlterationId}")
    fun updateFeeAlteration(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable feeAlterationId: FeeAlterationId,
        @RequestBody feeAlteration: FeeAlteration,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeAlteration.UPDATE,
                    feeAlterationId,
                )
                val existing = tx.getFeeAlteration(feeAlterationId)
                tx.upsertFeeAlteration(
                    clock = clock,
                    modifiedBy = user.evakaUserId,
                    feeAlteration = feeAlteration.copy(id = feeAlterationId),
                )

                val expandedPeriod =
                    existing?.let {
                        DateRange(
                            minOf(it.validFrom, feeAlteration.validFrom),
                            maxEndDate(it.validTo, feeAlteration.validTo),
                        )
                    } ?: DateRange(feeAlteration.validFrom, feeAlteration.validTo)

                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            feeAlteration.personId,
                            expandedPeriod,
                        )
                    ),
                    runAt = clock.now(),
                )
            }
        }
        Audit.ChildFeeAlterationsUpdate.log(targetId = AuditId(feeAlterationId))
    }

    @DeleteMapping("/{feeAlterationId}")
    fun deleteFeeAlteration(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable feeAlterationId: FeeAlterationId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeAlteration.DELETE,
                    feeAlterationId,
                )
                val existing = tx.getFeeAlteration(feeAlterationId)
                tx.deleteFeeAlteration(feeAlterationId)

                existing?.let {
                    asyncJobRunner.plan(
                        tx,
                        listOf(
                            AsyncJob.GenerateFinanceDecisions.forChild(
                                existing.personId,
                                DateRange(existing.validFrom, existing.validTo),
                            )
                        ),
                        runAt = clock.now(),
                    )
                }
            }
        }
        Audit.ChildFeeAlterationsDelete.log(targetId = AuditId(feeAlterationId))
    }
}
