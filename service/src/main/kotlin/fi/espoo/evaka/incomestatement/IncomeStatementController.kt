package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.attachment.associateAttachments
import fi.espoo.evaka.attachment.dissociateAllAttachments
import fi.espoo.evaka.daycare.controllers.utils.notFound
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/income-statements")
class IncomeStatementController(
    private val accessControl: AccessControl,
) {
    @GetMapping("/{personId}")
    fun getIncomeStatements(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId
    ): ResponseEntity<List<IncomeStatement>> {
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read { tx ->
            ResponseEntity.ok(tx.readIncomeStatementsForPerson(personId.raw))
        }
    }

    @GetMapping("/{personId}/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): ResponseEntity<IncomeStatement> {
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.read { tx ->
            val incomeStatement = tx.readIncomeStatementForPerson(personId.raw, incomeStatementId)
            if (incomeStatement == null) throw NotFound("No such income statement")
            else ResponseEntity.ok(incomeStatement)
        }
    }
}
