package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.attachment.associateAttachments
import fi.espoo.evaka.attachment.dissociateAllAttachments
import fi.espoo.evaka.shared.IncomeStatementId
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen/income-statements")
class IncomeStatementControllerCitizen {
    @GetMapping
    fun getIncomeStatements(
        db: Database.Connection,
        user: AuthenticatedUser
    ): List<IncomeStatement> {
        Audit.IncomeStatementsOfPerson.log(user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { tx ->
            tx.readIncomeStatementsForPerson(user.id)
        }
    }

    @GetMapping("/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        Audit.IncomeStatementOfPerson.log(incomeStatementId, user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        return db.read { tx ->
            tx.readIncomeStatementForPerson(user.id, incomeStatementId) ?: throw NotFound("No such income statement")
        }
    }

    @PostMapping
    fun createIncomeStatement(
        db: Database.Connection,
        user: AuthenticatedUser,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementCreate.log(user.id)
        user.requireOneOfRoles(UserRole.END_USER)
        if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid income statement")
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
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: IncomeStatementBody
    ) {
        Audit.IncomeStatementUpdate.log(incomeStatementId)
        user.requireOneOfRoles(UserRole.END_USER)
        if (!validateIncomeStatementBody(body)) throw BadRequest("Invalid income statement body")
        return db.transaction { tx ->
            tx.updateIncomeStatement(user.id, incomeStatementId, body).also { success ->
                if (success) {
                    tx.dissociateAllAttachments(user.id, incomeStatementId)
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
        user: AuthenticatedUser,
        @PathVariable id: IncomeStatementId
    ) {
        Audit.IncomeStatementDelete.log(id)
        user.requireOneOfRoles(UserRole.END_USER)
        return db.transaction { tx ->
            tx.readIncomeStatementForPerson(user.id, id)
                ?.also { if (it.handlerName != null) throw Forbidden("Handled income statement cannot be removed") }
                ?: throw NotFound("Income statement not found")
            tx.removeIncomeStatement(id)
        }
    }
}
