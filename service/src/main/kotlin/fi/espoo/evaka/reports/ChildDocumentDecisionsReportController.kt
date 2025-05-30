// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.Audit
import fi.espoo.evaka.document.ChildDocumentType
import fi.espoo.evaka.document.childdocument.ChildDocumentOrDecisionStatus
import fi.espoo.evaka.document.childdocument.ChildDocumentSummary
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.document.childdocument.getChildDocuments
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.toPredicate
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ChildDocumentDecisionsReportController(private val accessControl: AccessControl) {
    @GetMapping("/employee/reports/child-document-decisions")
    fun getChildDocumentDecisionsReport(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestParam statuses: Set<ChildDocumentOrDecisionStatus> = emptySet(),
        @RequestParam includeEnded: Boolean,
    ): List<ChildDocumentSummary> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Global.READ_CHILD_DOCUMENT_DECISIONS_REPORT,
                    )
                    it.setStatementTimeout(REPORT_STATEMENT_TIMEOUT)

                    val aclFilter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.ChildDocument.READ,
                        )

                    it.getReportRows(
                        today = clock.today(),
                        aclFilter = aclFilter,
                        statuses = statuses,
                        includeEnded = includeEnded,
                    )
                }
            }
            .also { Audit.ChildDocumentDecisionsReportRead.log() }
    }

    @GetMapping("/employee/reports/child-document-decisions/notification-count")
    fun getChildDocumentDecisionsReportNotificationCount(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
    ): Int {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.READ_CHILD_DOCUMENT_DECISIONS_REPORT,
                    )

                    tx.createQuery {
                            sql(
                                """
                    SELECT count(*)
                    FROM child_document
                    WHERE status = ${bind(DocumentStatus.DECISION_PROPOSAL)} 
                        AND decision_maker = ${bind(user.id)}
                """
                            )
                        }
                        .exactlyOne<Int>()
                }
            }
            .also { Audit.ChildDocumentDecisionsReportNotificationsRead.log() }
    }
}

private fun Database.Read.getReportRows(
    today: LocalDate,
    aclFilter: AccessControlFilter<ChildDocumentId>,
    statuses: Set<ChildDocumentOrDecisionStatus>,
    includeEnded: Boolean,
): List<ChildDocumentSummary> {
    val documentPredicate =
        Predicate { where("$it.type = ${bind(ChildDocumentType.OTHER_DECISION)}") }
            .and(aclFilter.toPredicate())

    val documentDecisionPredicate =
        if (includeEnded) {
            Predicate.alwaysTrue()
        } else {
            Predicate { where("$it.valid_to IS NULL OR $it.valid_to >= ${bind(today)}") }
        }

    return getChildDocuments(
        documentPredicate = documentPredicate,
        documentDecisionPredicate = documentDecisionPredicate,
        statuses = statuses,
    )
}
