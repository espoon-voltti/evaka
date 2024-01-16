// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.controller

import fi.espoo.evaka.Audit
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.associateOrphanAttachments
import fi.espoo.evaka.attachment.deleteAttachment
import fi.espoo.evaka.invoicing.data.deleteFeeAlteration
import fi.espoo.evaka.invoicing.data.getFeeAlteration
import fi.espoo.evaka.invoicing.data.getFeeAlterationsForPerson
import fi.espoo.evaka.invoicing.data.upsertFeeAlteration
import fi.espoo.evaka.invoicing.domain.FeeAlteration
import fi.espoo.evaka.s3.DocumentService
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
@RequestMapping("/fee-alterations")
class FeeAlterationController(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val accessControl: AccessControl,
    private val documentClient: DocumentService,
    bucketEnv: BucketEnv
) {
    private val filesBucket = bucketEnv.attachments

    @GetMapping
    fun getFeeAlterations(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestParam personId: PersonId
    ): List<FeeAlterationWithPermittedActions> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Child.READ_FEE_ALTERATIONS,
                        personId
                    )
                    val feeAlterations = tx.getFeeAlterationsForPerson(personId)
                    val permittedActions =
                        accessControl.getPermittedActions<FeeAlterationId, Action.FeeAlteration>(
                            tx,
                            user,
                            clock,
                            feeAlterations.mapNotNull { it.id }
                        )
                    feeAlterations.map {
                        FeeAlterationWithPermittedActions(it, permittedActions[it.id] ?: emptySet())
                    }
                }
            }
            .also {
                Audit.ChildFeeAlterationsRead.log(
                    targetId = personId,
                    meta = mapOf("count" to it.size)
                )
            }
    }

    data class FeeAlterationWithPermittedActions(
        val data: FeeAlteration,
        val permittedActions: Set<Action.FeeAlteration>
    )

    @PostMapping
    fun createFeeAlteration(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @RequestBody feeAlteration: FeeAlteration
    ) {
        val id = FeeAlterationId(UUID.randomUUID())
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Child.CREATE_FEE_ALTERATION,
                    feeAlteration.personId
                )
                tx.upsertFeeAlteration(
                    clock,
                    feeAlteration.copy(id = id, updatedBy = user.evakaUserId)
                )
                tx.associateOrphanAttachments(
                    user.evakaUserId,
                    AttachmentParent.FeeAlteration(id),
                    feeAlteration.attachments.map { it.id }
                )
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
        Audit.ChildFeeAlterationsCreate.log(targetId = feeAlteration.personId, objectId = id)
    }

    @PutMapping("/{feeAlterationId}")
    fun updateFeeAlteration(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable feeAlterationId: FeeAlterationId,
        @RequestBody feeAlteration: FeeAlteration
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeAlteration.UPDATE,
                    feeAlterationId
                )
                val existing = tx.getFeeAlteration(feeAlterationId)
                tx.upsertFeeAlteration(
                    clock,
                    feeAlteration.copy(id = feeAlterationId, updatedBy = user.evakaUserId)
                )

                val expandedPeriod =
                    existing?.let {
                        DateRange(
                            minOf(it.validFrom, feeAlteration.validFrom),
                            maxEndDate(it.validTo, feeAlteration.validTo)
                        )
                    } ?: DateRange(feeAlteration.validFrom, feeAlteration.validTo)

                asyncJobRunner.plan(
                    tx,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forChild(
                            feeAlteration.personId,
                            expandedPeriod
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
        Audit.ChildFeeAlterationsUpdate.log(targetId = feeAlterationId)
    }

    @DeleteMapping("/{feeAlterationId}")
    fun deleteFeeAlteration(
        db: Database,
        user: AuthenticatedUser,
        clock: EvakaClock,
        @PathVariable feeAlterationId: FeeAlterationId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.FeeAlteration.DELETE,
                    feeAlterationId
                )
                val existing = tx.getFeeAlteration(feeAlterationId)

                existing?.let { feeAlteration ->
                    feeAlteration.attachments.map {
                        tx.deleteAttachment(it.id)
                        documentClient.delete(filesBucket, "${it.id}")
                    }
                }

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
        Audit.ChildFeeAlterationsDelete.log(targetId = feeAlterationId)
    }
}
