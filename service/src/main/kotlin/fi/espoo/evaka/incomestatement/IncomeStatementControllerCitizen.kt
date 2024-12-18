// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.associateOrphanAttachments
import fi.espoo.evaka.attachment.dissociateAttachmentsOfParent
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.Forbidden
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
@RequestMapping("/citizen/income-statements")
class IncomeStatementControllerCitizen(private val accessControl: AccessControl) {
    @GetMapping
    fun getIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestParam page: Int,
    ): PagedIncomeStatements {
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
                }
            }
            .also {
                Audit.IncomeStatementsOfPerson.log(
                    targetId = AuditId(user.id),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    @GetMapping("/child/{childId}")
    fun getChildIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @RequestParam page: Int,
    ): PagedIncomeStatements {
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
                }
            }
            .also {
                Audit.IncomeStatementsOfChild.log(
                    targetId = AuditId(childId),
                    meta = mapOf("total" to it.total),
                )
            }
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
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.IncomeStatement.READ,
                        incomeStatementId,
                    )
                    tx.readIncomeStatementForPerson(
                        user = user,
                        personId = user.id,
                        incomeStatementId = incomeStatementId,
                    ) ?: throw NotFound("No such income statement")
                }
            }
            .also { Audit.IncomeStatementReadOfPerson.log(targetId = AuditId(incomeStatementId)) }
    }

    @GetMapping("/child/{childId}/{incomeStatementId}")
    fun getChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.IncomeStatement.READ,
                        incomeStatementId,
                    )
                    tx.readIncomeStatementForPerson(
                        user = user,
                        personId = childId,
                        incomeStatementId = incomeStatementId,
                    ) ?: throw NotFound("No such child income statement")
                }
            }
            .also { Audit.IncomeStatementReadOfChild.log(targetId = AuditId(incomeStatementId)) }
    }

    @PostMapping
    fun createIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: IncomeStatementBody,
        @RequestParam draft: Boolean,
    ) {
        val id =
            db.connect { dbc ->
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
        val id =
            db.connect { dbc ->
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
                    tx.readIncomeStatementForPerson(user, user.id, incomeStatementId)
                        ?: throw NotFound("Income statement not found")

                verifyIncomeStatementUpdateAllowed(original, body)

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
                    is IncomeStatementBody.Income ->
                        tx.associateOrphanAttachments(user.evakaUserId, parent, body.attachmentIds)
                    else -> Unit
                }
            }
        }
        Audit.IncomeStatementUpdate.log(targetId = AuditId(incomeStatementId))
    }

    @PutMapping("/child/{childId}/{incomeStatementId}")
    fun updateChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody,
        @RequestParam draft: Boolean,
    ) {
        if (!draft && !validateIncomeStatementBody(body))
            throw BadRequest("Invalid child income statement body")

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
                    tx.readIncomeStatementForPerson(user, childId, incomeStatementId)
                        ?: throw NotFound("Income statement not found")

                verifyIncomeStatementUpdateAllowed(original, body)

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
                    is IncomeStatementBody.ChildIncome ->
                        tx.associateOrphanAttachments(user.evakaUserId, parent, body.attachmentIds)
                    else -> Unit
                }
            }
        }
        Audit.IncomeStatementUpdateForChild.log(targetId = AuditId(incomeStatementId))
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
                verifyIncomeStatementDeletionAllowed(tx, user, user.id, id)
                tx.removeIncomeStatement(id)
            }
        }
        Audit.IncomeStatementDelete.log(targetId = AuditId(id))
    }

    @DeleteMapping("/child/{childId}/{id}")
    fun removeChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
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
                verifyIncomeStatementDeletionAllowed(tx, user, childId, id)
                tx.removeIncomeStatement(id)
            }
        }
        Audit.IncomeStatementDeleteOfChild.log(targetId = AuditId(id))
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

    private fun verifyIncomeStatementUpdateAllowed(
        original: IncomeStatement,
        update: IncomeStatementBody,
    ) {
        if (original.status == IncomeStatementStatus.HANDLED) {
            throw Forbidden("Handled income statement cannot be modified")
        }

        if (original.status == IncomeStatementStatus.SENT) {
            // Convert the original income statement into IncomeStatementBody for comparison.
            // Copy otherInfo and attachmentIds from update because these are allowed to be updated.
            // Everything else must remain equal.
            val originalBody =
                when {
                    original is IncomeStatement.ChildIncome &&
                        update is IncomeStatementBody.ChildIncome ->
                        IncomeStatementBody.ChildIncome(
                            startDate = original.startDate,
                            endDate = original.endDate,
                            otherInfo = update.otherInfo,
                            attachmentIds = update.attachmentIds,
                        )
                    original is IncomeStatement.Income && update is IncomeStatementBody.Income ->
                        IncomeStatementBody.Income(
                            startDate = original.startDate,
                            endDate = original.endDate,
                            gross = original.gross,
                            entrepreneur = original.entrepreneur,
                            student = original.student,
                            alimonyPayer = original.alimonyPayer,
                            otherInfo = update.otherInfo,
                            attachmentIds = update.attachmentIds,
                        )
                    original is IncomeStatement.HighestFee &&
                        update is IncomeStatementBody.HighestFee ->
                        IncomeStatementBody.HighestFee(
                            startDate = original.startDate,
                            endDate = original.endDate,
                        )
                    else -> throw BadRequest("Income statement type cannot be changed anymore")
                }

            if (originalBody != update) {
                throw BadRequest("Only attachments and otherInfo can be updated after sending")
            }
        }
    }

    private fun verifyIncomeStatementDeletionAllowed(
        tx: Database.Transaction,
        user: AuthenticatedUser.Citizen,
        personId: PersonId,
        id: IncomeStatementId,
    ) {
        val incomeStatement =
            tx.readIncomeStatementForPerson(user, personId, id)
                ?: throw NotFound("Income statement not found")
        if (incomeStatement.status == IncomeStatementStatus.HANDLED) {
            throw Forbidden("Handled income statement cannot be removed")
        }
    }
}
