// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.attachment.associateAttachments
import fi.espoo.evaka.attachment.dissociateAllPersonsAttachments
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
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
class IncomeStatementControllerCitizen {
    @GetMapping
    fun getIncomeStatements(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): Paged<IncomeStatement> {
        Audit.IncomeStatementsOfPerson.log(user.id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { tx ->
            tx.readIncomeStatementsForPerson(user.id, includeEmployeeContent = false, page = page, pageSize = pageSize)
        }
    }

    @GetMapping("/start-dates")
    fun getIncomeStatementStartDates(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen
    ): List<LocalDate> {
        Audit.IncomeStatementStartDates.log()
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { it.readIncomeStatementStartDates(user) }
    }

    @GetMapping("/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        Audit.IncomeStatementOfPerson.log(incomeStatementId, user.id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { tx ->
            tx.readIncomeStatementForPerson(user.id, incomeStatementId, includeEmployeeContent = false)
                ?: throw NotFound("No such income statement")
        }
    }

    @PostMapping
    fun createIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementCreate.log(user.id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)

        if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid income statement")
        if (db.read { tx -> tx.incomeStatementExistsForStartDate(user, body.startDate) })
            throw BadRequest("An income statement for this start date already exists")

        db.transaction { tx ->
            val incomeStatementId = tx.createIncomeStatement(user.id, body)
            when (body) {
                is IncomeStatementBody.Income ->
                    tx.associateAttachments(user.id, incomeStatementId, body.attachmentIds)
                else ->
                    Unit
            }
        }
    }

    @PutMapping("/{incomeStatementId}")
    fun updateIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementUpdate.log(incomeStatementId)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid income statement body")
        return db.transaction { tx ->
            verifyIncomeStatementModificationsAllowed(tx, user, incomeStatementId)
            tx.updateIncomeStatement(user.id, incomeStatementId, body).also { success ->
                if (success) {
                    tx.dissociateAllPersonsAttachments(user.id, incomeStatementId)
                    when (body) {
                        is IncomeStatementBody.Income ->
                            tx.associateAttachments(user.id, incomeStatementId, body.attachmentIds)
                        else ->
                            Unit
                    }
                }
            }
        }.let { success -> if (!success) throw NotFound("Income statement not found") }
    }

    @DeleteMapping("/{id}")
    fun removeIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser.Citizen,
        @PathVariable id: IncomeStatementId
    ) {
        Audit.IncomeStatementDelete.log(id)
        @Suppress("DEPRECATION")
        user.requireOneOfRoles(UserRole.END_USER)
        return db.transaction { tx ->
            verifyIncomeStatementModificationsAllowed(tx, user, id)
            tx.removeIncomeStatement(id)
        }
    }

    private fun verifyIncomeStatementModificationsAllowed(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        id: IncomeStatementId
    ) {
        val incomeStatement = tx.readIncomeStatementForPerson(user.id, id, includeEmployeeContent = false)
            ?: throw NotFound("Income statement not found")
        if (incomeStatement.handled) {
            throw Forbidden("Handled income statement cannot be modified or removed")
        }
    }
}
