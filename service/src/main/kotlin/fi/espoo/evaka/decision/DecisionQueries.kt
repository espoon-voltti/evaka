// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.application.ApplicationDecisions
import fi.espoo.evaka.application.DecisionSummary
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

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
            p.first_name, p.last_name,
            c.first_name AS child_first_name, c.last_name AS child_last_name
        FROM decision d
        INNER JOIN daycare u on d.unit_id = u.id
        INNER JOIN application ap on d.application_id = ap.id 
        INNER JOIN employee p on d.created_by = p.id
        INNER JOIN person c on ap.child_id = c.id
        LEFT JOIN unit_manager m on u.unit_manager_id = m.id
    """.trimIndent()

private fun decisionFromResultSet(rs: ResultSet): Decision = Decision(
    id = DecisionId(rs.getUUID("id")),
    createdBy = "${rs.getString("first_name")} ${rs.getString("last_name")}",
    type = DecisionType.valueOf(rs.getString("type")),
    startDate = rs.getDate("start_date").toLocalDate(),
    endDate = rs.getDate("end_date").toLocalDate(),
    documentKey = rs.getString("document_key"),
    otherGuardianDocumentKey = rs.getString("other_guardian_document_key"),
    decisionNumber = rs.getLong("number"),
    sentDate = rs.getDate("sent_date").toLocalDate(),
    status = DecisionStatus.valueOf(rs.getString("status")),
    requestedStartDate = rs.getDate("requested_start_date")?.toLocalDate(),
    resolved = rs.getDate("resolved")?.toLocalDate(),
    unit = DecisionUnit(
        id = DaycareId(rs.getUUID("unit_id")),
        name = rs.getString("name"),
        daycareDecisionName = rs.getString("decision_daycare_name"),
        preschoolDecisionName = rs.getString("decision_preschool_name"),
        manager = rs.getString("manager"),
        streetAddress = rs.getString("street_address"),
        postalCode = rs.getString("postal_code"),
        postOffice = rs.getString("post_office"),
        phone = rs.getString("phone"),
        decisionHandler = rs.getString("decision_handler"),
        decisionHandlerAddress = rs.getString("decision_handler_address"),
        providerType = rs.getEnum("provider_type")
    ),
    applicationId = ApplicationId(rs.getUUID("application_id")),
    childId = UUID.fromString(rs.getString("child_id")),
    childName = "${rs.getString("child_first_name")} ${rs.getString("child_last_name")}"
)

fun Database.Read.getDecision(decisionId: DecisionId): Decision? {
    return createQuery("$decisionSelector WHERE d.id = :id AND d.sent_date IS NOT NULL").bind("id", decisionId)
        .map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.first()
}

fun Database.Read.getDecisionsByChild(childId: UUID, authorizedUnits: AclAuthorization): List<Decision> {
    val units = authorizedUnits.ids
    val sql = "$decisionSelector WHERE ap.child_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id IN (<units>)" } ?: ""}"
    val query = createQuery(sql).bind("id", childId)
    units?.let { query.bindList("units", units) }
    return query.map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.list()
}

fun Database.Read.getDecisionsByApplication(applicationId: ApplicationId, authorizedUnits: AclAuthorization): List<Decision> {
    val units = authorizedUnits.ids
    val sql =
        "$decisionSelector WHERE d.application_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id IN (<units>)" } ?: ""}"
    val query = createQuery(sql).bind("id", applicationId)
    units?.let { query.bindList("units", units) }

    return query.map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.list()
}

fun Database.Read.getDecisionsByGuardian(guardianId: UUID, authorizedUnits: AclAuthorization): List<Decision> {
    val units = authorizedUnits.ids
    val sql = "$decisionSelector WHERE ap.guardian_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id IN (<units>)" } ?: ""}"
    val query = createQuery(sql).bind("id", guardianId)
    units?.let { query.bindList("units", units) }

    return query.map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.list()
}

data class ApplicationDecisionRow(
    val applicationId: ApplicationId,
    val childName: String,
    val id: DecisionId,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?
)

fun Database.Read.getOwnDecisions(guardianId: UUID): List<ApplicationDecisions> {
    // language=sql
    val sql =
        """
        SELECT d.application_id, p.first_name || ' ' || p.last_name child_name,  d.id, d.type, d.status, d.sent_date, d.resolved::date
        FROM decision d
        JOIN application a ON d.application_id = a.id
        JOIN person p ON a.child_id = p.id
        WHERE a.guardian_id = :guardianId AND d.sent_date IS NOT NULL
        """.trimIndent()

    val rows = createQuery(sql)
        .bind("guardianId", guardianId)
        .mapTo<ApplicationDecisionRow>()

    return rows
        .groupBy { it.applicationId }
        .map { (applicationId, decisions) ->
            ApplicationDecisions(
                applicationId,
                decisions.first().childName,
                decisions.map { DecisionSummary(it.id, it.type, it.status, it.sentDate, it.resolved) }
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
        """.trimIndent()

    return createQuery(sql)
        .bind("applicationId", applicationId)
        .mapTo<DecisionDraft>()
        .list()
}

fun Database.Transaction.finalizeDecisions(
    applicationId: ApplicationId
): List<DecisionId> {
    // discard unplanned drafts
    createUpdate(
        //language=SQL
        """
            DELETE FROM decision WHERE sent_date IS NULL AND application_id = :applicationId AND planned = false
        """.trimIndent()
    )
        .bind("applicationId", applicationId)
        .execute()

    // confirm planned drafts
    return createQuery(
        //language=SQL
        """
            UPDATE decision SET sent_date = :sentDate 
            WHERE sent_date IS NULL AND application_id = :applicationId AND planned = true
            RETURNING id
        """.trimIndent()
    )
        .bind("applicationId", applicationId)
        .bind("sentDate", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<DecisionId>()
        .list()
}

fun Database.Transaction.insertDecision(
    decisionId: DecisionId,
    userId: UUID,
    sentDate: LocalDate,
    applicationId: ApplicationId,
    unitId: DaycareId,
    decisionType: String,
    startDate: LocalDate,
    endDate: LocalDate
) {
    createUpdate(
        //language=SQL
        """
            INSERT INTO decision (id, created_by, sent_date, unit_id, application_id, type, start_date, end_date) 
            VALUES (:id, :createdBy, :sentDate, :unitId, :applicationId, :type::decision_type, :startDate, :endDate)
        """.trimIndent()
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

fun Database.Transaction.updateDecisionGuardianDocumentKey(decisionId: DecisionId, documentKey: String) {
    // language=SQL
    createUpdate("UPDATE decision SET document_key = :documentKey WHERE id = :id")
        .bind("id", decisionId)
        .bind("documentKey", documentKey)
        .execute()
}

fun Database.Transaction.updateDecisionOtherGuardianDocumentKey(decisionId: DecisionId, documentKey: String) {
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
    ).bind("id", decisionId)
        .map { rs: ResultSet, _: StatementContext -> rs.getBoolean("blocked") }.first() ?: false
}

fun Database.Read.getDecisionLanguage(decisionId: DecisionId): String {
    return createQuery(
        //language=SQL
        """
            SELECT daycare.language
            FROM decision
                INNER JOIN daycare ON unit_id = daycare.id
            WHERE decision.id = :id
        """.trimIndent().trimIndent()
    )
        .bind("id", decisionId)
        .map { rs -> rs.getColumn("language", String::class.java) }.first()
}

fun Database.Transaction.markDecisionAccepted(user: AuthenticatedUser, decisionId: DecisionId, requestedStartDate: LocalDate) {
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
  resolved = now()
WHERE id = :id
AND status = 'PENDING'
        """.trimIndent()
    )
        .bind("id", decisionId)
        .bind("userId", user.id)
        .bind("requestedStartDate", requestedStartDate)
        .execute()
}

fun Database.Transaction.markDecisionRejected(user: AuthenticatedUser, decisionId: DecisionId) {
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
  resolved = now()
WHERE id = :id
AND status = 'PENDING'
        """.trimIndent()
    )
        .bind("id", decisionId)
        .bind("userId", user.id)
        .execute()
}
