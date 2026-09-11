// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.incomestatement

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.AuditId
import evaka.core.attachment.AttachmentParent
import evaka.core.attachment.associateOrphanAttachments
import evaka.core.attachment.dissociateAttachmentsOfParent
import evaka.core.shared.AttachmentId
import evaka.core.shared.ChildId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.Forbidden
import evaka.core.shared.domain.NotFound
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
@RequestMapping("/citizen/income-statements")
class IncomeStatementControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam page: Int,
    ): PagedIncomeStatements {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_INCOME_STATEMENTS,
                        user.id,
                    )
                    tx.readIncomeStatementsForPerson(
                            user = user,
                            personId = user.id,
                            page = page,
                            pageSize = 10,
                        )
                        .also { statements ->
                            statements.data.forEach {
                                audit.add(it.id).add(it.attachmentIds).observeDate(it.startDate)
                            }
                        }
                }
            }
            .also { audit.log(Audit.IncomeStatementsOfPerson, clock) }
    }

    @GetMapping("/child/{childId}")
    fun getChildIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestParam page: Int,
    ): PagedIncomeStatements {
        val audit = AuditContext().add(childId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_INCOME_STATEMENTS,
                        childId,
                    )
                    tx.readIncomeStatementsForPerson(
                            user = user,
                            personId = childId,
                            page = page,
                            pageSize = 10,
                        )
                        .also { statements ->
                            statements.data.forEach {
                                audit.add(it.id).add(it.attachmentIds).observeDate(it.startDate)
                            }
                        }
                }
            }
            .also { audit.log(Audit.IncomeStatementsOfChild, clock) }
    }

    data class PartnerIncomeStatementStatusResponse(val partner: PartnerIncomeStatementStatus?)

    @GetMapping("/partner")
    fun getPartnerIncomeStatementStatus(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): PartnerIncomeStatementStatusResponse {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_PARTNER_INCOME_STATEMENT_STATUS,
                        user.id,
                    )
                    PartnerIncomeStatementStatusResponse(
                        tx.getPartnerIncomeStatementStatus(user.id, clock.today())
                            ?.also { audit.add(it.partnerId) }
                            ?.status
                    )
                }
            }
            .also { audit.log(Audit.IncomeStatementStatusOfPartner, clock) }
    }

    @GetMapping("/child/start-dates/{childId}")
    fun getChildIncomeStatementStartDates(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<LocalDate> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Child.READ_INCOME_STATEMENTS,
                        childId,
                    )
                    it.readIncomeStatementStartDates(childId)
                }
            }
            .also {
                Audit.IncomeStatementStartDatesOfChild.log(
                    targetId = AuditId(childId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @GetMapping("/start-dates/")
    fun getIncomeStatementStartDates(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<LocalDate> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Person.READ_INCOME_STATEMENTS,
                        user.id,
                    )
                    it.readIncomeStatementStartDates(user.id)
                }
            }
            .also {
                Audit.IncomeStatementStartDates.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    @GetMapping("/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        val audit = AuditContext().add(incomeStatementId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.IncomeStatement.READ,
                        incomeStatementId,
                    )
                    tx.readIncomeStatement(user = user, incomeStatementId = incomeStatementId)
                        ?.also {
                            audit.add(it.personId).add(it.attachmentIds).observeDate(it.startDate)
                        } ?: throw NotFound("No such income statement")
                }
            }
            .also { audit.log(Audit.IncomeStatementRead, clock) }
    }

    @PostMapping
    fun createIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: IncomeStatementBody,
        @RequestParam draft: Boolean,
    ) {
        val id = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Person.CREATE_INCOME_STATEMENT,
                    user.id,
                )
                createValidatedIncomeStatement(
                    tx = tx,
                    user = user,
                    now = clock.now(),
                    personId = user.id,
                    body = body,
                    draft = draft,
                )
            }
        }
        Audit.IncomeStatementCreate.log(targetId = AuditId(user.id), objectId = AuditId(id))
    }

    @PostMapping("/child/{childId}")
    fun createChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestBody body: IncomeStatementBody,
        @RequestParam draft: Boolean?,
    ) {
        val id = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Child.CREATE_INCOME_STATEMENT,
                    childId,
                )
                createValidatedIncomeStatement(
                    tx = tx,
                    user = user,
                    now = clock.now(),
                    personId = childId,
                    body = body,
                    draft = draft ?: false,
                )
            }
        }
        Audit.IncomeStatementCreateForChild.log(targetId = AuditId(user.id), objectId = AuditId(id))
    }

    @PutMapping("/{incomeStatementId}")
    fun updateIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody,
        @RequestParam draft: Boolean,
    ) {
        if (!draft && !validateIncomeStatementBody(body))
            throw BadRequest("Invalid income statement body")

        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.IncomeStatement.UPDATE,
                    incomeStatementId,
                )

                val original =
                    tx.readIncomeStatement(user, incomeStatementId)
                        ?: throw NotFound("Income statement not found")

                if (original.status != IncomeStatementStatus.DRAFT) {
                    throw Forbidden("Only draft income statements can be updated")
                }

                tx.updateIncomeStatement(
                    user.evakaUserId,
                    clock.now(),
                    incomeStatementId,
                    body,
                    draft,
                )

                val parent = AttachmentParent.IncomeStatement(incomeStatementId)
                tx.dissociateAttachmentsOfParent(user.evakaUserId, parent)

                when (body) {
                    is IncomeStatementBody.Income -> body.attachmentIds
                    is IncomeStatementBody.ChildIncome -> body.attachmentIds
                    else -> null
                }?.also { attachmentIds ->
                    tx.associateOrphanAttachments(user.evakaUserId, parent, attachmentIds)
                }
            }
        }
        Audit.IncomeStatementUpdate.log(targetId = AuditId(incomeStatementId))
    }

    data class UpdateSentIncomeStatementBody(
        val otherInfo: String,
        val attachmentIds: List<AttachmentId>,
    )

    @PutMapping("/{incomeStatementId}/update-sent")
    fun updateSentIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: UpdateSentIncomeStatementBody,
    ) {
        val now = clock.now()
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.IncomeStatement.UPDATE,
                    incomeStatementId,
                )
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Attachment.READ_ORPHAN_ATTACHMENT,
                    body.attachmentIds,
                )

                val original =
                    tx.readIncomeStatement(user, incomeStatementId)
                        ?: throw NotFound("Income statement not found")

                if (
                    (original !is IncomeStatement.Income &&
                        original !is IncomeStatement.ChildIncome) ||
                        (original.status != IncomeStatementStatus.SENT &&
                            original.status != IncomeStatementStatus.HANDLING)
                ) {
                    throw Forbidden(
                        "Only income statements with status SENT or HANDLING can be updated"
                    )
                }

                tx.updateIncomeStatementOtherInfo(
                    incomeStatementId,
                    user.evakaUserId,
                    now,
                    body.otherInfo,
                )

                val parent = AttachmentParent.IncomeStatement(incomeStatementId)
                tx.dissociateAttachmentsOfParent(user.evakaUserId, parent)
                tx.associateOrphanAttachments(user.evakaUserId, parent, body.attachmentIds)
            }
        }
        Audit.IncomeStatementUpdate.log(targetId = AuditId(incomeStatementId))
    }

    @DeleteMapping("/{id}")
    fun deleteIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: IncomeStatementId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.IncomeStatement.DELETE,
                    id,
                )
                verifyIncomeStatementDeletionAllowed(tx, user, id)
                tx.removeIncomeStatement(id)
            }
        }
        Audit.IncomeStatementDelete.log(targetId = AuditId(id))
    }

    @GetMapping("/children")
    fun getIncomeStatementChildren(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<ChildBasicInfo> {
        val personId = user.id
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Person.READ_CHILDREN,
                        personId,
                    )
                    it.getIncomeStatementChildrenByGuardian(personId, clock.today())
                }
            }
            .also {
                Audit.CitizenChildrenRead.log(
                    targetId = AuditId(personId),
                    meta = mapOf("count" to it.size),
                )
            }
    }

    private fun verifyIncomeStatementDeletionAllowed(
        tx: Database.Transaction,
        user: AuthenticatedUser.Citizen,
        id: IncomeStatementId,
    ) {
        val incomeStatement =
            tx.readIncomeStatement(user, id) ?: throw NotFound("Income statement not found")
        if (incomeStatement.status == IncomeStatementStatus.HANDLING) {
            throw Forbidden("Income statement cannot be removed while being handled")
        }
        if (incomeStatement.status == IncomeStatementStatus.HANDLED) {
            throw Forbidden("Handled income statement cannot be removed")
        }
    }
}
