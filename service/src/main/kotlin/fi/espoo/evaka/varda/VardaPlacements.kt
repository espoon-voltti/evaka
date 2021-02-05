// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun updatePlacements(db: Database.Connection, client: VardaClient) {
    removeDeletedPlacements(db, client)
    sendNewPlacements(db, client)
    sendUpdatedPlacements(db, client)
}

fun removeMarkedPlacementsFromVarda(db: Database.Connection, client: VardaClient) {
    val placementIds: List<Long> = db.read { getPlacementsToDelete(it) }
    logger.info { "Varda: Deleting ${placementIds.size} marked placements" }
    placementIds.forEach { id ->
        if (client.deletePlacement(id)) {
            db.transaction { softDeletePlacement(it, id) }
        }
    }
}

fun sendNewPlacements(db: Database.Connection, client: VardaClient) {
    val newPlacements = db.read { getNewPlacements(it, client.getDecisionUrl) }
    logger.info { "Varda: Creating ${newPlacements.size} new placements" }
    newPlacements.forEach { (decisionId, placementId, newPlacement) ->
        client.createPlacement(newPlacement)?.let { (vardaPlacementId) ->
            db.transaction {
                insertVardaPlacement(
                    it,
                    VardaPlacementTableRow(
                        id = UUID.randomUUID(),
                        vardaPlacementId = vardaPlacementId,
                        evakaPlacementId = placementId,
                        decisionId = decisionId,
                        createdAt = Instant.now(),
                        uploadedAt = Instant.now()
                    )
                )
            }
        }
    }
}

fun sendUpdatedPlacements(db: Database.Connection, client: VardaClient) {
    val updatedPlacements = db.read { getUpdatedPlacements(it, client.getDecisionUrl) }
    logger.info { "Varda: Updating ${updatedPlacements.size} updated placements" }
    updatedPlacements.forEach { (id, vardaPlacementId, updatedPlacement) ->
        client.updatePlacement(vardaPlacementId, updatedPlacement)?.let {
            db.transaction { updatePlacementUploadTimestamp(it, id) }
        }
    }
}

fun removeDeletedPlacements(db: Database.Connection, client: VardaClient) {
    val removedPlacements = db.read { getRemovedPlacements(it) }
    logger.info { "Varda: Deleting ${removedPlacements.size} removed placements" }
    removedPlacements.forEach { vardaPlacementId ->
        client.deletePlacement(vardaPlacementId).let { success ->
            if (success) db.transaction { deletePlacement(it, vardaPlacementId) }
        }
    }
}

private val placementBaseQuery =
    // language=SQL
    """
WITH accepted_daycare_decision AS (
    $acceptedDaycareDecisionsQuery
), decision AS (
    SELECT d.child_id, d.unit_id, d.start_date, d.end_date, vd.id, vd.varda_decision_id
    FROM accepted_daycare_decision d
    JOIN varda_decision vd ON d.id = vd.evaka_decision_id AND vd.deleted_at IS NULL
), derived_decision AS (
    SELECT p.child_id, p.unit_id, p.start_date, p.end_date, vd.id, vd.varda_decision_id
    FROM placement p
    JOIN varda_decision vd ON p.id = vd.evaka_placement_id AND vd.deleted_at IS NULL
), sent_decision AS (
    SELECT * FROM decision
    UNION
    SELECT * FROM derived_decision
), daycare_placement AS (
    SELECT * FROM placement
    WHERE type = ANY(:placementTypes::placement_type[])
    AND start_date <= now()
)
SELECT
    vp.id,
    vp.varda_placement_id,
    p.id AS placement_id,
    d.id AS decision_id,
    d.varda_decision_id,
    u.oph_unit_oid oph_unit_oid,
    p.start_date,
    LEAST(p.end_date, COALESCE(u.closing_date, 'infinity')) end_date
FROM daycare_placement p
LEFT JOIN varda_placement vp ON p.id = vp.evaka_placement_id AND vp.deleted_at IS NULL
JOIN daycare u ON p.unit_id = u.id AND u.upload_to_varda = true AND u.oph_unit_oid IS NOT NULL
JOIN sent_decision d
    ON p.child_id = d.child_id
    AND (p.unit_id = d.unit_id OR (SELECT ghost_unit FROM daycare WHERE id = d.unit_id))
    AND daterange(p.start_date, p.end_date, '[]') && daterange(d.start_date, d.end_date, '[]')
    """.trimIndent()

fun getNewPlacements(tx: Database.Read, getDecisionUrl: (Long) -> String): List<Triple<UUID, UUID, VardaPlacement>> {
    val sql =
        """
$placementBaseQuery
WHERE vp.id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("decisionTypes", vardaDecisionTypes)
        .bind("placementTypes", vardaPlacementTypes)
        .map(toVardaPlacementWithDecisionAndPlacementId(getDecisionUrl))
        .toList()
}

fun insertVardaPlacement(tx: Database.Transaction, placement: VardaPlacementTableRow) {
    val sql =
        """
INSERT INTO varda_placement (
    id,
    varda_placement_id,
    evaka_placement_id,
    decision_id,
    created_at,
    uploaded_at
) VALUES (
    :id,
    :vardaPlacementId,
    :evakaPlacementId,
    :decisionId,
    :createdAt,
    :uploadedAt
)
        """.trimIndent()

    tx.createUpdate(sql)
        .bind("id", placement.id)
        .bind("vardaPlacementId", placement.vardaPlacementId)
        .bind("evakaPlacementId", placement.evakaPlacementId)
        .bind("decisionId", placement.decisionId)
        .bind("createdAt", placement.createdAt)
        .bind("uploadedAt", placement.uploadedAt)
        .execute()
}

private fun getUpdatedPlacements(
    tx: Database.Read,
    getDecisionUrl: (Long) -> String
): List<Triple<UUID, Long, VardaPlacement>> {
    val sql =
        """
$placementBaseQuery
WHERE vp.uploaded_at < GREATEST(p.updated, u.updated)
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("decisionTypes", vardaDecisionTypes)
        .bind("placementTypes", vardaPlacementTypes)
        .map(toVardaPlacementWithIdAndVardaId(getDecisionUrl))
        .toList()
}

private fun updatePlacementUploadTimestamp(tx: Database.Transaction, id: UUID, uploadedAt: Instant = Instant.now()) {
    val sql = "UPDATE varda_placement SET uploaded_at = :uploadedAt WHERE id = :id"
    tx.createUpdate(sql)
        .bind("id", id)
        .bind("uploadedAt", uploadedAt)
        .execute()
}

fun getRemovedPlacements(tx: Database.Read): List<Long> {
    return tx.createQuery("SELECT varda_placement_id FROM varda_placement WHERE evaka_placement_id IS NULL")
        .mapTo(Long::class.java)
        .toList()
}

fun deletePlacement(tx: Database.Transaction, vardaPlacementId: Long) {
    tx.createUpdate("DELETE FROM varda_placement WHERE varda_placement_id = :id")
        .bind("id", vardaPlacementId)
        .execute()
}

fun softDeletePlacement(tx: Database.Transaction, vardaPlacementId: Long) {
    tx.createUpdate("UPDATE varda_placement SET deleted_at = NOW() WHERE varda_placement_id = :id")
        .bind("id", vardaPlacementId)
        .execute()
}

fun getPlacementsToDelete(tx: Database.Read): List<Long> {
    return tx.createQuery("SELECT varda_placement_id FROM varda_placement WHERE should_be_deleted = true AND deleted_at IS NULL")
        .mapTo(Long::class.java)
        .toList()
}

data class VardaPlacement(
    @JsonProperty("varhaiskasvatuspaatos")
    val decisionUrl: String,
    @JsonProperty("toimipaikka_oid")
    val unitOid: String,
    @JsonProperty("alkamis_pvm")
    val startDate: LocalDate,
    @JsonProperty("paattymis_pvm")
    val endDate: LocalDate?
)

data class VardaPlacementResponse(
    @JsonProperty("id")
    val vardaPlacementId: Long
)

data class VardaPlacementTableRow(
    val id: UUID,
    val vardaPlacementId: Long,
    val evakaPlacementId: UUID,
    val decisionId: UUID,
    val createdAt: Instant,
    val uploadedAt: Instant
)

private fun toVardaPlacementWithDecisionAndPlacementId(
    getDecisionUrl: (Long) -> String
): (ResultSet, StatementContext) -> Triple<UUID, UUID, VardaPlacement> =
    { rs, _ ->
        Triple(
            rs.getUUID("decision_id"),
            rs.getUUID("placement_id"),
            toVardaPlacement(rs, getDecisionUrl)
        )
    }

private fun toVardaPlacementWithIdAndVardaId(
    getDecisionUrl: (Long) -> String
): (ResultSet, StatementContext) -> Triple<UUID, Long, VardaPlacement> =
    { rs, _ ->
        Triple(
            rs.getUUID("id"),
            rs.getLong("varda_placement_id"),
            toVardaPlacement(rs, getDecisionUrl)
        )
    }

private fun toVardaPlacement(
    rs: ResultSet,
    getDecisionUrl: (Long) -> String
): VardaPlacement = VardaPlacement(
    getDecisionUrl(rs.getLong("varda_decision_id")),
    rs.getString("oph_unit_oid"),
    rs.getDate("start_date").toLocalDate(),
    rs.getDate("end_date")?.toLocalDate()
)

val toVardaPlacementRow: (ResultSet, StatementContext) -> VardaPlacementTableRow = { rs, _ ->
    VardaPlacementTableRow(
        id = rs.getUUID("id"),
        vardaPlacementId = rs.getLong("varda_placement_id"),
        evakaPlacementId = rs.getUUID("evaka_placement_id"),
        decisionId = rs.getUUID("decision_id"),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        uploadedAt = rs.getTimestamp("uploaded_at").toInstant()
    )
}
