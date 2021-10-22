// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/income-statements")
class IncomeStatementController(
    private val accessControl: AccessControl,
) {
    @GetMapping("/person/{personId}")
    fun getIncomeStatements(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): Paged<IncomeStatement> {
        Audit.IncomeStatementsOfPerson.log(personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read {
            it.readIncomeStatementsForPerson(
                personId = personId.raw,
                includeEmployeeContent = true,
                page = page,
                pageSize = pageSize
            )
        }
    }

    @GetMapping("/person/{personId}/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        Audit.IncomeStatementOfPerson.log(incomeStatementId, personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read {
            it.readIncomeStatementForPerson(
                personId.raw,
                incomeStatementId,
                includeEmployeeContent = true
            )
        } ?: throw NotFound("No such income statement")
    }

    data class SetIncomeStatementHandledBody(val handled: Boolean, val handlerNote: String)

    @PostMapping("/{incomeStatementId}/handled")
    fun setHandled(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: SetIncomeStatementHandledBody
    ) {
        Audit.IncomeStatementUpdateHandled.log(incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE_HANDLED, incomeStatementId)
        db.transaction { tx ->
            tx.updateIncomeStatementHandled(
                incomeStatementId,
                body.handlerNote,
                if (body.handled) EmployeeId(user.id) else null,
            )
        }
    }

    @GetMapping("/awaiting-handler")
    fun getIncomeStatementsAwaitingHandler(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestParam areas: String,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): Paged<IncomeStatementAwaitingHandler> {
        Audit.IncomeStatementsAwaitingHandler.log()
        accessControl.requirePermissionFor(user, Action.Global.FETCH_INCOME_STATEMENTS_AWAITING_HANDLER)
        val areasList = areas.split(",").filter { it.isNotEmpty() }
        return db.read { it.fetchIncomeStatementsAwaitingHandler(areasList, page, pageSize) }
    }
}
