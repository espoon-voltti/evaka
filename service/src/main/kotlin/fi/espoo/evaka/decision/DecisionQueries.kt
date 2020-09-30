// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.decision

import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.net.URI
import java.sql.ResultSet
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

// language=SQL
private val decisionSelector =
    """
        SELECT 
            d.id, d.type, d.start_date, d.end_date, d.document_uri, d.other_guardian_document_uri, d.number, d.sent_date, d.status, d.unit_id, d.application_id, d.requested_start_date, d.resolved,
            u.name, u.decision_daycare_name, u.decision_preschool_name, u.decision_handler, u.decision_handler_address, u.provider_type,
            u.street_address, u.postal_code, u.post_office,
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
    id = UUID.fromString(rs.getString("id")),
    createdBy = "${rs.getString("first_name")} ${rs.getString("last_name")}",
    type = DecisionType.valueOf(rs.getString("type")),
    startDate = rs.getDate("start_date").toLocalDate(),
    endDate = rs.getDate("end_date").toLocalDate(),
    documentUri = rs.getString("document_uri"),
    otherGuardianDocumentUri = rs.getString("other_guardian_document_uri"),
    decisionNumber = rs.getLong("number"),
    sentDate = rs.getDate("sent_date").toLocalDate(),
    status = DecisionStatus.valueOf(rs.getString("status")),
    requestedStartDate = rs.getDate("requested_start_date")?.toLocalDate(),
    resolved = rs.getDate("resolved")?.toLocalDate(),
    unit = DecisionUnit(
        id = UUID.fromString(rs.getString("unit_id")),
        name = rs.getString("name"),
        daycareDecisionName = rs.getString("decision_daycare_name"),
        preschoolDecisionName = rs.getString("decision_preschool_name"),
        manager = rs.getString("manager"),
        streetAddress = rs.getString("street_address"),
        postalCode = rs.getString("postal_code"),
        postOffice = rs.getString("post_office"),
        decisionHandler = rs.getString("decision_handler"),
        decisionHandlerAddress = rs.getString("decision_handler_address"),
        providerType = rs.getEnum("provider_type")
    ),
    applicationId = UUID.fromString(rs.getString("application_id")),
    childId = UUID.fromString(rs.getString("child_id")),
    childName = "${rs.getString("child_first_name")} ${rs.getString("child_last_name")}"
)

fun getDecision(h: Handle, decisionId: UUID): Decision? {
    return h.createQuery("$decisionSelector WHERE d.id = :id AND d.sent_date IS NOT NULL").bind("id", decisionId)
        .map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.first()
}

fun getDecisionsByChild(h: Handle, childId: UUID, authorizedUnits: AclAuthorization): List<Decision> {
    val units = authorizedUnits.ids
    val sql = "$decisionSelector WHERE ap.child_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id IN (<units>)" } ?: ""}"
    val query = h.createQuery(sql).bind("id", childId)
    units?.let { query.bindList("units", units) }
    return query.map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.list()
}

fun getDecisionsByApplication(h: Handle, applicationId: UUID, authorizedUnits: AclAuthorization): List<Decision> {
    val units = authorizedUnits.ids
    val sql =
        "$decisionSelector WHERE d.application_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id IN (<units>)" } ?: ""}"
    val query = h.createQuery(sql).bind("id", applicationId)
    units?.let { query.bindList("units", units) }

    return query.map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.list()
}

fun getDecisionsByGuardian(h: Handle, guardianId: UUID, authorizedUnits: AclAuthorization): List<Decision> {
    val units = authorizedUnits.ids
    val sql = "$decisionSelector WHERE ap.guardian_id = :id AND d.sent_date IS NOT NULL ${units?.let { " AND d.unit_id IN (<units>)" } ?: ""}"
    val query = h.createQuery(sql).bind("id", guardianId)
    units?.let { query.bindList("units", units) }

    return query.map { rs: ResultSet, _: StatementContext -> decisionFromResultSet(rs) }.list()
}

fun fetchDecisionDrafts(h: Handle, applicationId: UUID): List<DecisionDraft> {
    // language=sql
    val sql =
        """
            SELECT id, unit_id, type, start_date, end_date, planned
            FROM decision
            WHERE application_id = :applicationId AND sent_date IS NULL
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("applicationId", applicationId)
        .mapTo<DecisionDraft>()
        .list()
}

fun finalizeDecisions(
    h: Handle,
    applicationId: UUID
): List<UUID> {
    // discard unplanned drafts
    h.createUpdate(
        //language=SQL
        """
            DELETE FROM decision WHERE sent_date IS NULL AND application_id = :applicationId AND planned = false
        """.trimIndent()
    )
        .bind("applicationId", applicationId)
        .execute()

    // confirm planned drafts
    return h.createQuery(
        //language=SQL
        """
            UPDATE decision SET sent_date = :sentDate 
            WHERE sent_date IS NULL AND application_id = :applicationId AND planned = true
            RETURNING id
        """.trimIndent()
    )
        .bind("applicationId", applicationId)
        .bind("sentDate", LocalDate.now(ZoneId.of("Europe/Helsinki")))
        .mapTo<UUID>()
        .list()
}

fun insertDecision(
    h: Handle,
    decisionId: UUID,
    userId: UUID,
    sentDate: LocalDate,
    applicationId: UUID,
    unitId: UUID,
    decisionType: String,
    startDate: LocalDate,
    endDate: LocalDate
) {
    h.createUpdate(
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

fun updateDecisionGuardianDocumentUri(h: Handle, decisionId: UUID, pdfUri: URI) {
    // language=SQL
    h.createUpdate("UPDATE decision SET document_uri = :uri WHERE id = :id")
        .bind("id", decisionId)
        .bind("uri", pdfUri.toString())
        .execute()
}

fun updateDecisionOtherGuardianDocumentUri(h: Handle, decisionId: UUID, pdfUri: URI) {
    // language=SQL
    h.createUpdate("UPDATE decision SET other_guardian_document_uri = :uri WHERE id = :id")
        .bind("id", decisionId)
        .bind("uri", pdfUri.toString())
        .execute()
}

fun isDecisionBlocked(h: Handle, decisionId: UUID): Boolean {
    return h.createQuery(
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

fun getDecisionLanguage(h: Handle, decisionId: UUID): String {
    return h.createQuery(
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

fun markDecisionAccepted(h: Handle, user: AuthenticatedUser, decisionId: UUID, requestedStartDate: LocalDate) {
    if (isDecisionBlocked(h, decisionId)) {
        throw BadRequest("Cannot accept decision that is blocked by a pending primary decision")
    }
    h.createUpdate(
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

fun markDecisionRejected(h: Handle, user: AuthenticatedUser, decisionId: UUID) {
    if (isDecisionBlocked(h, decisionId)) {
        throw BadRequest("Cannot reject decision that is blocked by a pending primary decision")
    }
    h.createUpdate(
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
