// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDecisions
import fi.espoo.evaka.application.DecisionSummary
import fi.espoo.evaka.invoicing.service.DocumentLang
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable
import java.time.LocalDate
import org.jdbi.v3.core.result.RowView

private fun Database.Read.createDecisionQuery(
    decision: Predicate<DatabaseTable.Decision> = Predicate.alwaysTrue(),
    application: Predicate<DatabaseTable.Application> = Predicate.alwaysTrue(),
) =
    createQuery<Any> {
        sql(
            """
        SELECT
            d.id, d.type, d.start_date, d.end_date, d.document_key, d.other_guardian_document_key, d.number, d.sent_date, d.status, d.unit_id, d.application_id, d.requested_start_date, d.resolved, 
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
                .trimIndent()
        )
    }

private fun decisionFromResultSet(row: RowView): Decision =
    Decision(
        id = row.mapColumn("id"),
        createdBy = row.mapColumn("created_by"),
        type = row.mapColumn("type"),
        startDate = row.mapColumn("start_date"),
        endDate = row.mapColumn("end_date"),
        documentKey = row.mapColumn("document_key"),
        otherGuardianDocumentKey = row.mapColumn("other_guardian_document_key"),
        decisionNumber = row.mapColumn("number"),
        sentDate = row.mapColumn("sent_date"),
        status = row.mapColumn("status"),
        requestedStartDate = row.mapColumn("requested_start_date"),
        resolved = row.mapColumn<HelsinkiDateTime?>("resolved")?.toLocalDate(),
        resolvedByName = row.mapColumn<String?>("resolved_by_name"),
        unit =
            DecisionUnit(
                id = row.mapColumn("unit_id"),
                name = row.mapColumn("name"),
                daycareDecisionName = row.mapColumn("decision_daycare_name"),
                preschoolDecisionName = row.mapColumn("decision_preschool_name"),
                manager = row.mapColumn("manager"),
                streetAddress = row.mapColumn("street_address"),
                postalCode = row.mapColumn("postal_code"),
                postOffice = row.mapColumn("post_office"),
                phone = row.mapColumn("phone"),
                decisionHandler = row.mapColumn("decision_handler"),
                decisionHandlerAddress = row.mapColumn("decision_handler_address"),
                providerType = row.mapColumn("provider_type")
            ),
        applicationId = row.mapColumn("application_id"),
        childId = row.mapColumn("child_id"),
        childName =
            "${row.mapColumn<String>("child_last_name")} ${row.mapColumn<String>("child_first_name")}"
    )

fun Database.Read.getDecision(decisionId: DecisionId): Decision? =
    createDecisionQuery(decision = Predicate { where("$it.id = ${bind(decisionId)}") })
        .map(::decisionFromResultSet)
        .firstOrNull()

fun Database.Read.getSentDecision(decisionId: DecisionId): Decision? =
    createDecisionQuery(
            decision =
                Predicate { where("$it.sent_date IS NOT NULL AND $it.id = ${bind(decisionId)}") }
        )
        .map(::decisionFromResultSet)
        .firstOrNull()

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
        .map(::decisionFromResultSet)
        .toList()

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
        .map(::decisionFromResultSet)
        .toList()

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
                            .trimIndent()
                    )
                }
        )
        .map(::decisionFromResultSet)
        .toList()

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
                        "$it.guardian_id = ${bind(guardianId)} OR $it.other_guardian_id = ${bind(guardianId)}"
                    )
                }
        )
        .map(::decisionFromResultSet)
        .toList()

data class ApplicationDecisionRow(
    val applicationId: ApplicationId,
    val childId: ChildId,
    val childName: String,
    val id: DecisionId,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?
)

fun Database.Read.getOwnDecisions(guardianId: PersonId): List<ApplicationDecisions> {
    // language=sql
    val sql =
        """
        SELECT
            d.application_id,
            p.id AS child_id,
            p.first_name || ' ' || p.last_name AS child_name,
            d.id,
            d.type,
            d.status,
            d.sent_date,
            d.resolved::date
        FROM decision d
        JOIN application a ON d.application_id = a.id
        JOIN person p ON a.child_id = p.id
        WHERE a.guardian_id = :guardianId
        AND NOT EXISTS (
            SELECT 1 FROM guardian_blocklist b
            WHERE b.child_id = a.child_id
            AND b.guardian_id = :guardianId
        )
        AND d.sent_date IS NOT NULL
        AND a.status != 'WAITING_MAILING'::application_status_type
        """
            .trimIndent()

    val rows = createQuery(sql).bind("guardianId", guardianId).mapTo<ApplicationDecisionRow>()

    return rows
        .groupBy { it.applicationId }
        .map { (applicationId, decisions) ->
            ApplicationDecisions(
                applicationId,
                decisions.first().childId,
                decisions.first().childName,
                decisions.map {
                    DecisionSummary(it.id, it.type, it.status, it.sentDate, it.resolved)
                }
            )
        }
}

fun Database.Read.fetchDecisionDrafts(applicationId: ApplicationId): List<DecisionDraft> {
    // language=sql
    val sql =
        """
            SELECT id, unit_id, type, start_date, end_date, planned
            FROM decision
            WHERE application_id = :applicationId AND sent_date IS NULL
        """
            .trimIndent()

    return createQuery(sql).bind("applicationId", applicationId).mapTo<DecisionDraft>().list()
}

fun Database.Transaction.finalizeDecisions(
    applicationId: ApplicationId,
    today: LocalDate
): List<DecisionId> {
    // discard unplanned drafts
    createUpdate(
            "DELETE FROM decision WHERE sent_date IS NULL AND application_id = :applicationId AND planned = false"
        )
        .bind("applicationId", applicationId)
        .execute()

    // confirm planned drafts
    return createQuery(
            "UPDATE decision SET sent_date = :today WHERE application_id = :applicationId RETURNING id"
        )
        .bind("applicationId", applicationId)
        .bind("today", today)
        .mapTo<DecisionId>()
        .list()
}

fun Database.Transaction.markApplicationDecisionsSent(
    applicationId: ApplicationId,
    sentDate: LocalDate
) {
    createUpdate(
            """
UPDATE decision SET sent_date = :sentDate
WHERE sent_date IS NULL AND application_id = :applicationId AND planned = true
"""
        )
        .bind("applicationId", applicationId)
        .bind("sentDate", sentDate)
        .execute()
}

fun Database.Transaction.markDecisionSent(decisionId: DecisionId, sentDate: LocalDate) {
    createUpdate(
            """
UPDATE decision SET sent_date = :sentDate
WHERE sent_date IS NULL AND id = :decisionId AND planned = true
"""
        )
        .bind("decisionId", decisionId)
        .bind("sentDate", sentDate)
        .execute()
}

fun Database.Transaction.updateDecisionGuardianDocumentKey(
    decisionId: DecisionId,
    documentKey: String
) {
    // language=SQL
    createUpdate("UPDATE decision SET document_key = :documentKey WHERE id = :id")
        .bind("id", decisionId)
        .bind("documentKey", documentKey)
        .execute()
}

fun Database.Transaction.updateDecisionOtherGuardianDocumentKey(
    decisionId: DecisionId,
    documentKey: String
) {
    // language=SQL
    createUpdate("UPDATE decision SET other_guardian_document_key = :documentKey WHERE id = :id")
        .bind("id", decisionId)
        .bind("documentKey", documentKey)
        .execute()
}

fun Database.Read.isDecisionBlocked(decisionId: DecisionId): Boolean {
    return createQuery(
            // language=SQL
            """
SELECT count(*) > 0 AS blocked
FROM decision
WHERE status = 'PENDING' AND type = 'PRESCHOOL' AND id != :id
AND application_id = (SELECT application_id FROM decision WHERE id = :id)
"""
        )
        .bind("id", decisionId)
        .mapTo<Boolean>()
        .exactlyOne()
}

fun Database.Read.getDecisionLanguage(decisionId: DecisionId): DocumentLang {
    return createQuery(
            // language=SQL
            """
            SELECT daycare.language
            FROM decision
                INNER JOIN daycare ON unit_id = daycare.id
            WHERE decision.id = :id
        """
                .trimIndent()
        )
        .bind("id", decisionId)
        .mapTo<DocumentLang>()
        .exactlyOne()
}

fun Database.Transaction.markDecisionAccepted(
    user: AuthenticatedUser,
    clock: EvakaClock,
    decisionId: DecisionId,
    requestedStartDate: LocalDate
) {
    if (isDecisionBlocked(decisionId)) {
        throw BadRequest("Cannot accept decision that is blocked by a pending primary decision")
    }
    createUpdate(
            // language=SQL
            """
UPDATE decision
SET
  status = 'ACCEPTED',
  requested_start_date = :requestedStartDate,
  resolved_by = :userId,
  resolved = :now
WHERE id = :id
AND status = 'PENDING'
        """
                .trimIndent()
        )
        .bind("id", decisionId)
        .bind("now", clock.now())
        .bind("userId", user.evakaUserId)
        .bind("requestedStartDate", requestedStartDate)
        .execute()
}

fun Database.Transaction.markDecisionRejected(
    user: AuthenticatedUser,
    clock: EvakaClock,
    decisionId: DecisionId
) {
    if (isDecisionBlocked(decisionId)) {
        throw BadRequest("Cannot reject decision that is blocked by a pending primary decision")
    }
    createUpdate(
            // language=SQL
            """
UPDATE decision
SET
  status = 'REJECTED',
  resolved_by = :userId,
  resolved = :now
WHERE id = :id
AND status = 'PENDING'
        """
                .trimIndent()
        )
        .bind("id", decisionId)
        .bind("now", clock.now())
        .bind("userId", user.evakaUserId)
        .execute()
}
