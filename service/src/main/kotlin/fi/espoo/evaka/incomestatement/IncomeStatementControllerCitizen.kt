// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.attachment.associateAttachments
import fi.espoo.evaka.attachment.dissociateAllPersonsAttachments
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
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
@RequestMapping("/citizen/income-statements")
class IncomeStatementControllerCitizen(
    private val accessControl: AccessControl
) {
    @GetMapping
    fun getIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): Paged<IncomeStatement> {
        Audit.IncomeStatementsOfPerson.log(user.id)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.READ_ALL_OWN)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.readIncomeStatementsForPerson(PersonId(user.id), includeEmployeeContent = false, page = page, pageSize = pageSize)
            }
        }
    }

    @GetMapping("/child/{childId}")
    fun getChildIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): Paged<IncomeStatement> {
        Audit.IncomeStatementsOfChild.log(user.id, childId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.READ_ALL_CHILDS, childId.raw)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.readIncomeStatementsForPerson(PersonId(childId.raw), includeEmployeeContent = false, page = page, pageSize = pageSize)
            }
        }
    }

    @GetMapping("/child/start-dates/{childId}")
    fun getChildIncomeStatementStartDates(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId
    ): List<LocalDate> {
        Audit.IncomeStatementStartDatesOfChild.log(user.id, childId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.READ_CHILDS_START_DATES, childId.raw)

        return db.connect { dbc -> dbc.read { it.readIncomeStatementStartDates(PersonId(childId.raw)) } }
    }

    @GetMapping("/start-dates/")
    fun getIncomeStatementStartDates(
        db: Database,
        user: AuthenticatedUser.Citizen
    ): List<LocalDate> {
        Audit.IncomeStatementStartDates.log()
        accessControl.requirePermissionFor(user, Action.IncomeStatement.READ_START_DATES)
        return db.connect { dbc -> dbc.read { it.readIncomeStatementStartDates(PersonId(user.id)) } }
    }

    @GetMapping("/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        Audit.IncomeStatementReadOfPerson.log(incomeStatementId, user.id)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.READ)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.readIncomeStatementForPerson(PersonId(user.id), incomeStatementId, includeEmployeeContent = false)
                    ?: throw NotFound("No such income statement")
            }
        }
    }

    @GetMapping("/child/{childId}/{incomeStatementId}")
    fun getChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId,
        @PathVariable incomeStatementId: IncomeStatementId
    ): IncomeStatement {
        Audit.IncomeStatementReadOfChild.log(incomeStatementId, user.id)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.READ_CHILDS, incomeStatementId.raw)

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.readIncomeStatementForPerson(PersonId(childId.raw), incomeStatementId, includeEmployeeContent = false)
                    ?: throw NotFound("No such child income statement")
            }
        }
    }

    @PostMapping
    fun createIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementCreate.log(user.id)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.CREATE)
        db.connect { createIncomeStatement(it, PersonId(user.id), PersonId(user.id), body) }
    }

    @PostMapping("/child/{childId}")
    fun createChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementCreateForChild.log(user.id)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.CREATE_FOR_CHILD, childId.raw)
        db.connect { createIncomeStatement(it, PersonId(childId.raw), PersonId(user.id), body) }
    }

    @PutMapping("/{incomeStatementId}")
    fun updateIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementUpdate.log(incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE)

        if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid income statement body")
        return db.connect { dbc ->
            dbc.transaction { tx ->
                verifyIncomeStatementModificationsAllowed(tx, PersonId(user.id), incomeStatementId)
                tx.updateIncomeStatement(incomeStatementId, body).also { success ->
                    if (success) {
                        tx.dissociateAllPersonsAttachments(PersonId(user.id), incomeStatementId)
                        when (body) {
                            is IncomeStatementBody.Income ->
                                tx.associateAttachments(PersonId(user.id), incomeStatementId, body.attachmentIds)
                            else ->
                                Unit
                        }
                    }
                }
            }
        }.let { success -> if (!success) throw NotFound("Income statement not found") }
    }

    @PutMapping("/child/{childId}/{incomeStatementId}")
    fun updateChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementUpdateForChild.log(user.id, incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE_FOR_CHILD, childId.raw)

        if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid child income statement body")

        return db.connect { dbc ->
            dbc.transaction { tx ->
                verifyIncomeStatementModificationsAllowed(tx, PersonId(childId.raw), incomeStatementId)
                tx.updateIncomeStatement(incomeStatementId, body).also { success ->
                    if (success) {
                        tx.dissociateAllPersonsAttachments(PersonId(user.id), incomeStatementId)
                        when (body) {
                            is IncomeStatementBody.ChildIncome ->
                                tx.associateAttachments(PersonId(user.id), incomeStatementId, body.attachmentIds)
                            else ->
                                Unit
                        }
                    }
                }
            }
        }.let { success -> if (!success) throw NotFound("Income statement not found") }
    }

    @DeleteMapping("/{id}")
    fun removeIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable id: IncomeStatementId
    ) {
        Audit.IncomeStatementDelete.log(id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.connect { dbc ->
            dbc.transaction { tx ->
                verifyIncomeStatementModificationsAllowed(tx, PersonId(user.id), id)
                tx.removeIncomeStatement(id)
            }
        }
    }

    @DeleteMapping("/child/{childId}/{id}")
    fun removeChildIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Citizen,
        @PathVariable childId: ChildId,
        @PathVariable id: IncomeStatementId
    ) {
        Audit.IncomeStatementDeleteOfChild.log(user.id, id)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE_FOR_CHILD, childId.raw)

        return db.connect { dbc ->
            dbc.transaction { tx ->
                verifyIncomeStatementModificationsAllowed(tx, PersonId(childId.raw), id)
                tx.removeIncomeStatement(id)
            }
        }
    }

    @GetMapping("/children")
    fun getIncomeStatementChildren(db: Database, user: AuthenticatedUser.Citizen): List<ChildBasicInfo> {
        Audit.CitizenChildrenRead.log()
        accessControl.requirePermissionFor(user, Action.Global.READ_OWN_CHILDREN)
        return db.connect { dbc -> dbc.read { it.getIncomeStatementChildrenByGuardian(PersonId(user.id)) } }
    }

    private fun verifyIncomeStatementModificationsAllowed(
        tx: Database.Transaction,
        personId: PersonId,
        id: IncomeStatementId
    ) {
        val incomeStatement = tx.readIncomeStatementForPerson(personId, id, includeEmployeeContent = false)
            ?: throw NotFound("Income statement not found")
        if (incomeStatement.handled) {
            throw Forbidden("Handled income statement cannot be modified or removed")
        }
    }
}
