package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.controllers.utils.Wrapper
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
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
        @PathVariable personId: PersonId
    ): List<IncomeStatement> {
        Audit.IncomeStatementsOfPerson.log(personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read { it.readIncomeStatementsForPerson(personId.raw, excludeEmployeeAttachments = false) }
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
        return db.read { it.readIncomeStatementForPerson(personId.raw, incomeStatementId, excludeEmployeeAttachments = false) } ?: throw NotFound("No such income statement")
    }

    @PostMapping("/{incomeStatementId}/handled")
    fun setHandled(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: Wrapper<Boolean>
    ) {
        Audit.IncomeStatementUpdateHandled.log(incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE_HANDLED, incomeStatementId)
        db.transaction { tx ->
            tx.setIncomeStatementHandler(
                if (body.data) EmployeeId(user.id) else null,
                incomeStatementId
            )
        }
    }

    @GetMapping("/awaiting-handler")
    fun getIncomeStatementsAwaitingHandler(
        db: Database.Connection,
        user: AuthenticatedUser
    ): List<IncomeStatementAwaitingHandler> {
        Audit.IncomeStatementsAwaitingHandler.log()
        accessControl.requirePermissionFor(user, Action.Global.FETCH_INCOME_STATEMENTS_AWAITING_HANDLER)
        return db.read { it.fetchIncomeStatementsAwaitingHandler() }
    }
}
