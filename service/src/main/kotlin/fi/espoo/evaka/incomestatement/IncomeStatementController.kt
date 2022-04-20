// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.incomestatement

import fi.espoo.evaka.Audit
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/income-statements")
class IncomeStatementController(
    private val accessControl: AccessControl,
) {
    @GetMapping("/person/{personId}")
    fun getIncomeStatements(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @RequestParam page: Int,
        @RequestParam pageSize: Int
    ): Paged<IncomeStatement> {
        Audit.IncomeStatementsOfPerson.log(personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.connect { dbc ->
            dbc.read {
                it.readIncomeStatementsForPerson(
                    personId = personId,
                    includeEmployeeContent = true,
                    page = page,
                    pageSize = pageSize
                )
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
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, PersonId(childId.raw))

        return db.connect { dbc ->
            dbc.read { tx ->
                tx.readIncomeStatementsForPerson(PersonId(childId.raw), includeEmployeeContent = true, page = page, pageSize = pageSize)
            }
        }
    }

    @GetMapping("/person/{personId}/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable personId: PersonId,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        Audit.IncomeStatementReadOfPerson.log(incomeStatementId, personId)
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, personId)
        return db.connect { dbc ->
            dbc.read {
                it.readIncomeStatementForPerson(
                    personId,
                    incomeStatementId,
                    includeEmployeeContent = true
                )
            }
        } ?: throw NotFound("No such income statement")
    }

    data class SetIncomeStatementHandledBody(val handled: Boolean, val handlerNote: String)

    @PostMapping("/{incomeStatementId}/handled")
    fun setHandled(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: SetIncomeStatementHandledBody
    ) {
        Audit.IncomeStatementUpdateHandled.log(incomeStatementId)
        accessControl.requirePermissionFor(user, Action.IncomeStatement.UPDATE_HANDLED, incomeStatementId)
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateIncomeStatementHandled(
                    incomeStatementId,
                    body.handlerNote,
                    if (body.handled) EmployeeId(user.id) else null,
                )
            }
        }
    }

    @PostMapping("/awaiting-handler")
    fun getIncomeStatementsAwaitingHandler(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: SearchIncomeStatementsRequest
    ): Paged<IncomeStatementAwaitingHandler> {
        Audit.IncomeStatementsAwaitingHandler.log()
        accessControl.requirePermissionFor(user, Action.Global.FETCH_INCOME_STATEMENTS_AWAITING_HANDLER)
        return db.connect { dbc ->
            dbc.read {
                it.fetchIncomeStatementsAwaitingHandler(
                    HelsinkiDateTime.now().toLocalDate(),
                    body.areas ?: emptyList(),
                    body.providerTypes ?: emptyList(),
                    body.sentStartDate?.let { LocalDate.parse(body.sentStartDate, DateTimeFormatter.ISO_DATE) },
                    body.sentEndDate?.let { LocalDate.parse(body.sentEndDate, DateTimeFormatter.ISO_DATE) },
                    body.page,
                    body.pageSize
                )
            }
        }
    }

    @GetMapping("/guardian/{guardianId}/children")
    fun getIncomeStatementChildren(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable guardianId: PersonId
    ): List<ChildBasicInfo> {
        Audit.IncomeStatementsOfChild.log()
        accessControl.requirePermissionFor(user, Action.Person.READ_INCOME_STATEMENTS, guardianId)
        return db.connect { dbc ->
            dbc.read {
                it.getIncomeStatementChildrenByGuardian(guardianId)
            }
        }
    }
}

data class SearchIncomeStatementsRequest(
    val page: Int = 1,
    val pageSize: Int = 50,
    val areas: List<String>? = emptyList(),
    val providerTypes: List<ProviderType>? = emptyList(),
    val sentStartDate: String? = null,
    val sentEndDate: String? = null,
)
