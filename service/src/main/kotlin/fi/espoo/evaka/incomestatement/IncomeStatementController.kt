package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.daycare.controllers.utils.notFound
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/income-statements")
class IncomeStatementController {
    @GetMapping
    fun getIncomeStatements(
        db: Database.Connection,
        user: AuthenticatedUser
    ): ResponseEntity<List<IncomeStatement>> {
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { tx ->
            ResponseEntity.ok(tx.readIncomeStatementsForPerson(user.id))
        }
    }

    @PostMapping
    fun createIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: IncomeStatementBody
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.END_USER)
        db.transaction { tx ->
            tx.createIncomeStatement(user.id, body)
        }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{incomeStatementId}")
    fun updateIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody
    ): ResponseEntity<Unit> {
        user.requireOneOfRoles(UserRole.END_USER)
        return db.transaction { tx ->
            tx.updateIncomeStatement(user.id, incomeStatementId, body)
        }.let { success ->
            if (success) ResponseEntity.noContent().build()
            else notFound()
        }
    }
}
