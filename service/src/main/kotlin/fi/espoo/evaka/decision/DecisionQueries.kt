// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDecisions
import fi.espoo.evaka.application.DecisionSummary
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.ZoneId
import org.jdbi.v3.core.result.RowView

// language=SQL
private val decisionSelector =
    """
        SELECT 
            d.id, d.type, d.start_date, d.end_date, d.document_key, d.other_guardian_document_key, d.number, d.sent_date, d.status, d.unit_id, d.application_id, d.requested_start_date, d.resolved,
            u.name, u.decision_daycare_name, u.decision_preschool_name, u.decision_handler, u.decision_handler_address, u.provider_type,
            u.street_address, u.postal_code, u.post_office,
            u.phone,
            m.name AS manager,
            ap.child_id, ap.guardian_id,
            (SELECT name FROM evaka_user WHERE id = d.created_by) AS created_by,
            c.first_name AS child_first_name, c.last_name AS child_last_name
        FROM decision d
        INNER JOIN daycare u on d.unit_id = u.id
        INNER JOIN application ap on d.application_id = ap.id 
        INNER JOIN person c on ap.child_id = c.id
        LEFT JOIN unit_manager m on u.unit_manager_id = m.id
    """.trimIndent(
    )

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

fun Database.Read.getDecision(decisionId: DecisionId): Decision? {
    return createQuery("$decisionSelector WHERE d.id = :id AND d.sent_date IS NOT NULL")
        .bind("id", decisionId)
        .map(::decisionFromResultSet)
        .first()
}

fun Database.Read.getDecisionsByChild(
    childId: ChildId,
    authorizedUnits: AclAuthorization
): List<Decision> {
    val units = authorizedUnits.ids
    val sql =
        "$decisionSelector WHERE ap.child_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id = ANY(:units)" } ?: ""}"
    val query = createQuery(sql).bind("id", childId)
    units?.let { query.bind("units", units) }
    return query.map(::decisionFromResultSet).list()
}

fun Database.Read.getDecisionsByApplication(
    applicationId: ApplicationId,
    authorizedUnits: AclAuthorization
): List<Decision> {
    val units = authorizedUnits.ids
    val sql =
        "$decisionSelector WHERE d.application_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id = ANY(:units)" } ?: ""}"
    val query = createQuery(sql).bind("id", applicationId)
    units?.let { query.bind("units", units) }

    return query.map(::decisionFromResultSet).list()
}

fun Database.Read.getDecisionsByGuardian(
    guardianId: PersonId,
    authorizedUnits: AclAuthorization
): List<Decision> {
    val units = authorizedUnits.ids
    val sql =
        "$decisionSelector WHERE ap.guardian_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id = ANY(:units)" } ?: ""}"
    val query = createQuery(sql).bind("id", guardianId)
    units?.let { query.bind("units", units) }

    return query.map(::decisionFromResultSet).list()
}

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
        SELECT d.application_id, p.id child_id, p.first_name || ' ' || p.last_name child_name,  d.id, d.type, d.status, d.sent_date, d.resolved::date
        FROM decision d
        JOIN application a ON d.application_id = a.id
        JOIN person p ON a.child_id = p.id
        WHERE a.guardian_id = :guardianId AND d.sent_date IS NOT NULL
        """.trimIndent(
        )

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
        """.trimIndent(
        )

    return createQuery(sql).bind("applicationId", applicationId).mapTo<DecisionDraft>().list()
}

fun Database.Transaction.finalizeDecisions(applicationId: ApplicationId): List<DecisionId> {
    // discard unplanned drafts
    createUpdate(
            // language=SQL
            """
            DELETE FROM decision WHERE sent_date IS NULL AND application_id = :applicationId AND planned = false
        """.trimIndent(
            )
        )
        .bind("applicationId", applicationId)
        .execute()

    // confirm planned drafts
    return createQuery(
            // language=SQL
            """
            UPDATE decision SET sent_date = :sentDate 
            WHERE sent_date IS NULL AND application_id = :applicationId AND planned = true
            RETURNING id
        """.trimIndent(
            )
        )
        .bind("applicationId", applicationId)
        .bind("sentDate", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<DecisionId>()
        .list()
}

fun Database.Transaction.insertDecision(
    decisionId: DecisionId,
    userId: EvakaUserId,
    sentDate: LocalDate,
    applicationId: ApplicationId,
    unitId: DaycareId,
    decisionType: String,
    startDate: LocalDate,
    endDate: LocalDate
) {
    createUpdate(
            // language=SQL
            """
            INSERT INTO decision (id, created_by, sent_date, unit_id, application_id, type, start_date, end_date) 
            VALUES (:id, :createdBy, :sentDate, :unitId, :applicationId, :type::decision_type, :startDate, :endDate)
        """.trimIndent(
            )
        )
        .bind("id", decisionId)
        .bind("createdBy", userId)
        .bind("sentDate", sentDate)
        .bind("applicationId", applicationId)
        .bind("unitId", unitId)
        .bind("type", decisionType)
        .bind("startDate", startDate)
        .bind("endDate", endDate)
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
        .first()
        ?: false
}

fun Database.Read.getDecisionLanguage(decisionId: DecisionId): String {
    return createQuery(
            // language=SQL
            """
            SELECT daycare.language
            FROM decision
                INNER JOIN daycare ON unit_id = daycare.id
            WHERE decision.id = :id
        """
                .trimIndent()
                .trimIndent()
        )
        .bind("id", decisionId)
        .mapTo<String>()
        .first()
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
        """.trimIndent(
            )
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
        """.trimIndent(
            )
        )
        .bind("id", decisionId)
        .bind("now", clock.now())
        .bind("userId", user.evakaUserId)
        .execute()
}
