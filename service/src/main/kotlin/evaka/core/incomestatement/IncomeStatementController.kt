// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.incomestatement

import evaka.core.Audit
import evaka.core.AuditId
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.controller.SortDirection
import evaka.core.shared.DaycareId
import evaka.core.shared.IncomeStatementId
import evaka.core.shared.PersonId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/employee/income-statements")
class IncomeStatementController(private val accessControl: AccessControl) {
    @GetMapping("/person/{personId}")
    fun getIncomeStatements(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable personId: PersonId,
        @RequestParam page: Int,
    ): PagedIncomeStatements {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_INCOME_STATEMENTS,
                        personId,
                    )
                    it.readIncomeStatementsForPerson(
                        user = user,
                        personId = personId,
                        page = page,
                        pageSize = 10,
                    )
                }
            }
            .also {
                Audit.IncomeStatementsOfPerson.log(
                    targetId = AuditId(personId),
                    meta = mapOf("total" to it.total),
                )
            }
    }

    @GetMapping("/{incomeStatementId}")
    fun getIncomeStatement(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable incomeStatementId: IncomeStatementId,
    ): IncomeStatement {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.IncomeStatement.READ,
                        incomeStatementId,
                    )
                    tx.readIncomeStatement(user = user, incomeStatementId = incomeStatementId)
                } ?: throw NotFound("No such income statement")
            }
            .also { Audit.IncomeStatementRead.log(targetId = AuditId(incomeStatementId)) }
    }

    data class SetIncomeStatementHandledBody(
        val status: IncomeStatementStatus,
        val handlerNote: String,
    )

    @PostMapping("/{incomeStatementId}/handled")
    fun setIncomeStatementHandled(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable incomeStatementId: IncomeStatementId,
        @RequestBody body: SetIncomeStatementHandledBody,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.IncomeStatement.UPDATE_HANDLED,
                    incomeStatementId,
                )

                if (body.status == IncomeStatementStatus.DRAFT) {
                    throw BadRequest("Cannot set income statement status to DRAFT")
                }

                tx.updateIncomeStatementHandled(
                    user,
                    clock.now(),
                    incomeStatementId,
                    body.handlerNote,
                    body.status,
                )
            }
        }
        Audit.IncomeStatementUpdateHandled.log(targetId = AuditId(incomeStatementId))
    }

    @PostMapping("/awaiting-handler")
    fun getIncomeStatementsAwaitingHandler(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchIncomeStatementsRequest,
    ): PagedIncomeStatementsAwaitingHandler {
        return db.connect { dbc ->
                dbc.read { it ->
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.FETCH_INCOME_STATEMENTS_AWAITING_HANDLER,
                    )

                    if (
                        body.status?.any {
                            it == IncomeStatementStatus.DRAFT || it == IncomeStatementStatus.HANDLED
                        } ?: false
                    ) {
                        throw BadRequest("Invalid status filters")
                    }

                    it.fetchIncomeStatementsAwaitingHandler(
                        clock.now().toLocalDate(),
                        body.areas ?: emptyList(),
                        body.unit,
                        body.providerTypes ?: emptyList(),
                        body.sentStartDate,
                        body.sentEndDate,
                        body.placementValidDate,
                        body.status ?: emptyList(),
                        body.page,
                        pageSize = 50,
                        body.sortBy ?: IncomeStatementSortParam.SENT_AT,
                        body.sortDirection ?: SortDirection.ASC,
                    )
                }
            }
            .also { Audit.IncomeStatementsAwaitingHandler.log(meta = mapOf("total" to it.total)) }
    }

    @GetMapping("/guardian/{guardianId}/children")
    fun getIncomeStatementChildren(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable guardianId: PersonId,
    ): List<ChildBasicInfo> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_INCOME_STATEMENTS,
                        guardianId,
                    )
                    it.getIncomeStatementChildrenByGuardian(guardianId, clock.today())
                }
            }
            .also {
                Audit.GuardianChildrenRead.log(
                    targetId = AuditId(guardianId),
                    meta = mapOf("count" to it.size),
                )
            }
    }
}

data class SearchIncomeStatementsRequest(
    val page: Int = 1,
    val sortBy: IncomeStatementSortParam? = null,
    val sortDirection: SortDirection? = null,
    val areas: List<String>? = emptyList(),
    val unit: DaycareId? = null,
    val providerTypes: List<ProviderType>? = emptyList(),
    val sentStartDate: LocalDate? = null,
    val sentEndDate: LocalDate? = null,
    val placementValidDate: LocalDate? = null,
    val status: List<IncomeStatementStatus>? = emptyList(),
)

enum class IncomeStatementSortParam {
    SENT_AT,
    START_DATE,
    INCOME_END_DATE,
    TYPE,
    HANDLER_NOTE,
    PERSON_NAME,
    CITIZEN_MODIFIED_AT,
}
