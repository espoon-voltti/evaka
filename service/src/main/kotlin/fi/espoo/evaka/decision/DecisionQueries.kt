// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.DecisionSummary
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.Row
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.OfficialLanguage
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate

private fun Database.Read.createDecisionQuery(
    decision: Predicate = Predicate.alwaysTrue(),
    application: Predicate = Predicate.alwaysTrue(),
) = createQuery {
    sql(
        """
        SELECT
            d.id, d.type, d.start_date, d.end_date, d.document_key, d.number, d.sent_date, d.status, d.unit_id, d.application_id, d.requested_start_date, d.resolved, d.document_contains_contact_info,
            u.name, u.decision_daycare_name, u.decision_preschool_name, u.decision_handler, u.decision_handler_address, u.provider_type,
            u.street_address, u.postal_code, u.post_office,
            u.phone,
            unit_manager_name AS manager,
            ap.child_id, ap.guardian_id,
            (SELECT name FROM evaka_user WHERE id = d.created_by) AS created_by,
            c.first_name AS child_first_name, c.last_name AS child_last_name,
            eu.name AS resolved_by_name
        FROM decision d
        INNER JOIN daycare u on d.unit_id = u.id
        INNER JOIN application ap on d.application_id = ap.id
        INNER JOIN person c on ap.child_id = c.id
        LEFT JOIN evaka_user eu on d.resolved_by = eu.id
        WHERE ${predicate(decision.forTable("d"))}
        AND ${predicate(application.forTable("ap"))}
    """
    )
}

private fun Row.decisionFromResultSet(): Decision =
    Decision(
        id = column("id"),
        createdBy = column("created_by"),
        type = column("type"),
        startDate = column("start_date"),
        endDate = column("end_date"),
        documentKey = column("document_key"),
        decisionNumber = column("number"),
        sentDate = column("sent_date"),
        status = column("status"),
        requestedStartDate = column("requested_start_date"),
        resolved = column<HelsinkiDateTime?>("resolved")?.toLocalDate(),
        resolvedByName = column<String?>("resolved_by_name"),
        unit =
            DecisionUnit(
                id = column("unit_id"),
                name = column("name"),
                daycareDecisionName = column("decision_daycare_name"),
                preschoolDecisionName = column("decision_preschool_name"),
                manager = column("manager"),
                streetAddress = column("street_address"),
                postalCode = column("postal_code"),
                postOffice = column("post_office"),
                phone = column("phone"),
                decisionHandler = column("decision_handler"),
                decisionHandlerAddress = column("decision_handler_address"),
                providerType = column("provider_type")
            ),
        applicationId = column("application_id"),
        childId = column("child_id"),
        childName = "${column<String>("child_last_name")} ${column<String>("child_first_name")}",
        documentContainsContactInfo = column("document_contains_contact_info"),
    )

fun Database.Read.getDecision(decisionId: DecisionId): Decision? =
    createDecisionQuery(decision = Predicate { where("$it.id = ${bind(decisionId)}") })
        .exactlyOneOrNull(Row::decisionFromResultSet)

fun Database.Read.getSentDecision(decisionId: DecisionId): Decision? =
    createDecisionQuery(
            decision =
                Predicate { where("$it.sent_date IS NOT NULL AND $it.id = ${bind(decisionId)}") }
        )
        .exactlyOneOrNull(Row::decisionFromResultSet)

fun Database.Read.getDecisionsByChild(
    childId: ChildId,
    filter: AccessControlFilter<DecisionId>
): List<Decision> =
    createDecisionQuery(
            decision =
                Predicate {
                    where("$it.sent_date IS NOT NULL AND ${predicate(filter.forTable(it))}")
                },
            application = Predicate { where("$it.child_id = ${bind(childId)}") }
        )
        .toList(Row::decisionFromResultSet)

fun Database.Read.getDecisionsByApplication(
    applicationId: ApplicationId,
    filter: AccessControlFilter<DecisionId>
): List<Decision> =
    createDecisionQuery(
            decision =
                Predicate {
                    where(
                        "$it.application_id = ${bind(applicationId)} AND ${predicate(filter.forTable(it))}"
                    )
                }
        )
        .toList(Row::decisionFromResultSet)

fun Database.Read.getSentDecisionsByApplication(
    applicationId: ApplicationId,
    filter: AccessControlFilter<DecisionId>
): List<Decision> =
    createDecisionQuery(
            decision =
                Predicate {
                    where(
                        """
                            $it.application_id = ${bind(applicationId)}
                            AND $it.sent_date IS NOT NULL
                            AND ${predicate(filter.forTable(it))}
                        """
                    )
                }
        )
        .toList(Row::decisionFromResultSet)

fun Database.Read.getDecisionsByGuardian(
    guardianId: PersonId,
    filter: AccessControlFilter<DecisionId>
): List<Decision> =
    createDecisionQuery(
            decision =
                Predicate {
                    where("$it.sent_date IS NOT NULL AND ${predicate(filter.forTable(it))}")
                },
            application =
                Predicate {
                    where(
                        """
                $it.guardian_id = ${bind(guardianId)} OR
                ($it.allow_other_guardian_access AND EXISTS (
                    SELECT FROM application_other_guardian aog
                    WHERE aog.application_id = $it.id
                    AND aog.guardian_id = ${bind(guardianId)}
                ))
"""
                    )
                }
        )
        .toList(Row::decisionFromResultSet)

data class ApplicationDecisionRow(
    val applicationId: ApplicationId,
    val childId: ChildId,
    val id: DecisionId,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?
)

fun Database.Read.getOwnDecisions(
    guardianId: PersonId,
    children: Collection<ChildId>,
    filter: AccessControlFilter<DecisionId>
): List<DecisionSummary> =
    createQuery {
            sql(
                """
        SELECT
            d.application_id,
            a.child_id,
            d.id,
            d.type,
            d.status,
            d.sent_date,
            d.resolved::date
        FROM decision d
        JOIN application a ON d.application_id = a.id
        WHERE (
            a.guardian_id = ${bind(guardianId)}
            OR (a.allow_other_guardian_access AND EXISTS (
                SELECT FROM application_other_guardian aog
                WHERE aog.application_id = a.id
                AND aog.guardian_id = ${bind(guardianId)}
            ))
        )
        AND a.child_id = ANY(${bind(children)})
        AND ${predicate(filter.forTable("d"))}
        AND d.sent_date IS NOT NULL
        AND a.status IN ('WAITING_CONFIRMATION', 'ACTIVE', 'REJECTED')
        """
            )
        }
        .toList()

fun Database.Read.fetchDecisionDrafts(applicationId: ApplicationId): List<DecisionDraft> =
    createQuery {
            sql(
                """
SELECT id, unit_id, type, start_date, end_date, planned
FROM decision
WHERE application_id = ${bind(applicationId)} AND sent_date IS NULL
"""
            )
        }
        .toList()

fun Database.Transaction.finalizeDecisions(
    applicationId: ApplicationId,
    today: LocalDate
): List<DecisionId> {
    // discard unplanned drafts
    execute {
        sql(
            "DELETE FROM decision WHERE sent_date IS NULL AND application_id = ${bind(applicationId)} AND planned = false"
        )
    }

    return createQuery {
            sql(
                "UPDATE decision SET sent_date = ${bind(today)} WHERE application_id = ${bind(applicationId)} RETURNING id"
            )
        }
        .toList<DecisionId>()
}

fun Database.Transaction.markApplicationDecisionsSent(
    applicationId: ApplicationId,
    sentDate: LocalDate
) {
    execute {
        sql(
            """
UPDATE decision SET sent_date = ${bind(sentDate)}
WHERE sent_date IS NULL AND application_id = ${bind(applicationId)} AND planned = true
"""
        )
    }
}

fun Database.Transaction.markDecisionSent(decisionId: DecisionId, sentDate: LocalDate) {
    execute {
        sql(
            """
UPDATE decision SET sent_date = ${bind(sentDate)}
WHERE sent_date IS NULL AND id = ${bind(decisionId)} AND planned = true
"""
        )
    }
}

fun Database.Transaction.updateDecisionGuardianDocumentKey(
    decisionId: DecisionId,
    documentKey: String
) {
    execute {
        sql(
            "UPDATE decision SET document_key = ${bind(documentKey)} WHERE id = ${bind(decisionId)}"
        )
    }
}

fun Database.Read.isDecisionBlocked(decisionId: DecisionId): Boolean =
    createQuery {
            sql(
                """
SELECT count(*) > 0 AS blocked
FROM decision
WHERE status = 'PENDING' AND type = 'PRESCHOOL' AND id != ${bind(decisionId)}
AND application_id = (SELECT application_id FROM decision WHERE id = ${bind(decisionId)})
"""
            )
        }
        .exactlyOne()

fun Database.Read.getDecisionLanguage(decisionId: DecisionId): OfficialLanguage =
    createQuery {
            sql(
                """
            SELECT daycare.language
            FROM decision
                INNER JOIN daycare ON unit_id = daycare.id
            WHERE decision.id = ${bind(decisionId)}
        """
            )
        }
        .exactlyOne<OfficialLanguage>()

fun Database.Transaction.markDecisionAccepted(
    user: AuthenticatedUser,
    clock: EvakaClock,
    decisionId: DecisionId,
    requestedStartDate: LocalDate
) {
    if (isDecisionBlocked(decisionId)) {
        throw BadRequest("Cannot accept decision that is blocked by a pending primary decision")
    }
    execute {
        sql(
            """
UPDATE decision
SET
  status = 'ACCEPTED',
  requested_start_date = ${bind(requestedStartDate)},
  resolved_by = ${bind(user.evakaUserId)},
  resolved = ${bind(clock.now())}
WHERE id = ${bind(decisionId)}
AND status = 'PENDING'
"""
        )
    }
}

fun Database.Transaction.markDecisionRejected(
    user: AuthenticatedUser,
    clock: EvakaClock,
    decisionId: DecisionId
) {
    if (isDecisionBlocked(decisionId)) {
        throw BadRequest("Cannot reject decision that is blocked by a pending primary decision")
    }
    execute {
        sql(
            """
UPDATE decision
SET
  status = 'REJECTED',
  resolved_by = ${bind(user.evakaUserId)},
  resolved = ${bind(clock.now())}
WHERE id = ${bind(decisionId)}
AND status = 'PENDING'
    """
        )
    }
}
