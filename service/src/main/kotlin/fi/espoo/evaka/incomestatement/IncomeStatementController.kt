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
import org.springframework.http.ResponseEntity
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
    ): ResponseEntity<List<IncomeStatement>> {
        Audit.IncomeStatementsOfPerson.log(personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read { tx ->
            ResponseEntity.ok(tx.readIncomeStatementsForPerson(personId.raw))
        }
    }

    @GetMapping("/person/{personId}/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): ResponseEntity<IncomeStatement> {
        Audit.IncomeStatementOfPerson.log(incomeStatementId, personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read { tx ->
            val incomeStatement = tx.readIncomeStatementForPerson(personId.raw, incomeStatementId)
            if (incomeStatement == null) throw NotFound("No such income statement")
            else ResponseEntity.ok(incomeStatement)
        }
    }

    @PostMapping("/{incomeStatementId}/handled")
    fun setHandled(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: Wrapper<Boolean>
    ): ResponseEntity<Unit> {
        Audit.IncomeStatementUpdateHandled.log(incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE_HANDLED, incomeStatementId)
        db.transaction { tx ->
            tx.setIncomeStatementHandler(
                if (body.data) EmployeeId(user.id) else null,
                incomeStatementId
            )
        }
        return ResponseEntity.noContent().build()
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
