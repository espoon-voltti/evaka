// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.placement.PlacementType.DAYCARE
import fi.espoo.evaka.placement.PlacementType.DAYCARE_PART_TIME
import fi.espoo.evaka.placement.PlacementType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.varda.integration.VardaClient
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun updatePlacements(h: Handle, client: VardaClient) {
    removeMarkedPlacements(h, client)
    removeDeletedPlacements(h, client)
    sendNewPlacements(h, client)
    sendUpdatedPlacements(h, client)
}

fun removeMarkedPlacements(h: Handle, client: VardaClient) {
    val placementIds: List<Long> = getPlacementsToDelete(h)
    placementIds.forEach { id ->
        if (client.deletePlacement(id)) {
            softDeletePlacement(h, id)
        }
    }
}

fun sendNewPlacements(h: Handle, client: VardaClient) {
    val newPlacements = getNewPlacements(h, client.getUnitUrl, client.getDecisionUrl)
    newPlacements.forEach { (decisionId, placementId, newPlacement) ->
        client.createPlacement(newPlacement)?.let { (vardaPlacementId) ->
            insertVardaPlacement(
                h,
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

fun sendUpdatedPlacements(h: Handle, client: VardaClient) {
    val updatedPlacements = getUpdatedPlacements(h, client.getUnitUrl, client.getDecisionUrl)
    updatedPlacements.forEach { (id, vardaPlacementId, updatedPlacement) ->
        client.updatePlacement(vardaPlacementId, updatedPlacement)?.let {
            updatePlacementUploadTimestamp(h, id)
        }
    }
}

fun removeDeletedPlacements(h: Handle, client: VardaClient) {
    val removedPlacements = getRemovedPlacements(h)
    removedPlacements.forEach { vardaPlacementId ->
        client.deletePlacement(vardaPlacementId).let { success ->
            if (success) deletePlacement(h, vardaPlacementId)
        }
    }
}

internal val daycarePlacementTypes = arrayOf(DAYCARE.name, DAYCARE_PART_TIME.name, PRESCHOOL_DAYCARE.name)

private val placementBaseQuery =
    """
WITH accepted_daycare_decision AS (
    $acceptedDaycareDecisionsQuery
), decision AS (
    SELECT d.child_id, d.unit_id, d.start_date, d.end_date, vd.id, vd.varda_decision_id
    FROM accepted_daycare_decision d
    JOIN varda_decision vd ON d.id = vd.evaka_decision_id
), derived_decision AS (
    SELECT p.child_id, p.unit_id, p.start_date, p.end_date, vd.id, vd.varda_decision_id
    FROM placement p
    JOIN varda_decision vd ON p.id = vd.evaka_placement_id
), sent_decision AS (
    SELECT * FROM decision
    UNION
    SELECT * FROM derived_decision
), daycare_placement AS (
    SELECT * FROM placement
    WHERE type = ANY(:types::placement_type[])
    AND start_date <= now()
)
SELECT
    vp.id,
    vp.varda_placement_id,
    p.id AS placement_id,
    d.id AS decision_id,
    d.varda_decision_id,
    vu.varda_unit_id,
    daycare.oph_unit_oid oph_unit_oid,
    p.start_date,
    p.end_date
FROM daycare_placement p
LEFT JOIN varda_placement vp ON p.id = vp.evaka_placement_id
JOIN varda_unit vu ON p.unit_id = vu.evaka_daycare_id
JOIN daycare ON vu.evaka_daycare_id = daycare.id AND daycare.upload_to_varda = true AND daycare.oph_unit_oid IS NOT NULL
JOIN sent_decision d
    ON p.child_id = d.child_id
    AND p.unit_id = d.unit_id
    AND daterange(p.start_date, p.end_date, '[]') && daterange(d.start_date, d.end_date, '[]')
    """.trimIndent()

fun getNewPlacements(
    h: Handle,
    getUnitUrl: (Long) -> String,
    getDecisionUrl: (Long) -> String
): List<Triple<UUID, UUID, VardaPlacement>> {
    val sql =
        """
$placementBaseQuery
WHERE vp.id IS NULL
        """.trimIndent()

    return h.createQuery(sql)
        .bind("types", daycarePlacementTypes)
        .map(toVardaPlacementWithDecisionAndPlacementId(getUnitUrl, getDecisionUrl))
        .toList()
}

fun insertVardaPlacement(h: Handle, placement: VardaPlacementTableRow) {
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

    h.createUpdate(sql)
        .bind("id", placement.id)
        .bind("vardaPlacementId", placement.vardaPlacementId)
        .bind("evakaPlacementId", placement.evakaPlacementId)
        .bind("decisionId", placement.decisionId)
        .bind("createdAt", placement.createdAt)
        .bind("uploadedAt", placement.uploadedAt)
        .execute()
}

private fun getUpdatedPlacements(
    h: Handle,
    getUnitUrl: (Long) -> String,
    getDecisionUrl: (Long) -> String
): List<Triple<UUID, Long, VardaPlacement>> {
    val sql =
        """
$placementBaseQuery
WHERE vp.uploaded_at < p.updated
        """.trimIndent()

    return h.createQuery(sql)
        .bind("types", daycarePlacementTypes)
        .map(toVardaPlacementWithIdAndVardaId(getUnitUrl, getDecisionUrl))
        .toList()
}

private fun updatePlacementUploadTimestamp(h: Handle, id: UUID, uploadedAt: Instant = Instant.now()) {
    val sql = "UPDATE varda_placement SET uploaded_at = :uploadedAt WHERE id = :id"
    h.createUpdate(sql)
        .bind("id", id)
        .bind("uploadedAt", uploadedAt)
        .execute()
}

fun getRemovedPlacements(h: Handle): List<Long> {
    return h.createQuery("SELECT varda_placement_id FROM varda_placement WHERE evaka_placement_id IS NULL")
        .mapTo(Long::class.java)
        .toList()
}

fun deletePlacement(h: Handle, vardaPlacementId: Long) {
    h.createUpdate("DELETE FROM varda_placement WHERE varda_placement_id = :id")
        .bind("id", vardaPlacementId)
        .execute()
}

fun softDeletePlacement(h: Handle, vardaPlacementId: Long) {
    h.createUpdate("UPDATE varda_placement SET deleted = NOW() WHERE varda_placement_id = :id")
        .bind("id", vardaPlacementId)
        .execute()
}

fun getPlacementsToDelete(h: Handle): List<Long> {
    return h.createQuery("SELECT varda_placement_id FROM varda_placement WHERE should_be_deleted = true AND deleted IS NULL")
        .mapTo(Long::class.java)
        .toList()
}

data class VardaPlacement(
    @JsonProperty("varhaiskasvatuspaatos")
    val decisionUrl: String,
    @JsonProperty("toimipaikka")
    val unitUrl: String,
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
    getUnitUrl: (Long) -> String,
    getDecisionUrl: (Long) -> String
): (ResultSet, StatementContext) -> Triple<UUID, UUID, VardaPlacement> =
    { rs, _ ->
        Triple(
            rs.getUUID("decision_id"),
            rs.getUUID("placement_id"),
            toVardaPlacement(rs, getUnitUrl, getDecisionUrl)
        )
    }

private fun toVardaPlacementWithIdAndVardaId(
    getUnitUrl: (Long) -> String,
    getDecisionUrl: (Long) -> String
): (ResultSet, StatementContext) -> Triple<UUID, Long, VardaPlacement> =
    { rs, _ ->
        Triple(
            rs.getUUID("id"),
            rs.getLong("varda_placement_id"),
            toVardaPlacement(rs, getUnitUrl, getDecisionUrl)
        )
    }

private fun toVardaPlacement(
    rs: ResultSet,
    getUnitUrl: (Long) -> String,
    getDecisionUrl: (Long) -> String
): VardaPlacement = VardaPlacement(
    getDecisionUrl(rs.getLong("varda_decision_id")),
    getUnitUrl(rs.getLong("varda_unit_id")),
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
