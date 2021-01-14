// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType.DAYCARE
import fi.espoo.evaka.decision.DecisionType.DAYCARE_PART_TIME
import fi.espoo.evaka.decision.DecisionType.PRESCHOOL_DAYCARE
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getEnum
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

fun updateDecisions(db: Database.Connection, client: VardaClient) {
    removeDeletedDecisions(db, client)
    sendNewDecisions(db, client)
    sendUpdatedDecisions(db, client)
}

fun removeMarkedDecisionsFromVarda(db: Database.Connection, client: VardaClient) {
    val decisionIds: List<Long> = db.read { getDecisionsToDelete(it) }
    decisionIds.forEach { id ->
        if (client.deleteDecision(id)) {
            db.transaction { softDeleteDecision(it, id) }
        }
    }
}

fun sendNewDecisions(db: Database.Connection, client: VardaClient) {
    val newDecisions = db.read { getNewDecisions(it, client.getChildUrl) }
    newDecisions.forEach { (decisionId, newDecision) ->
        if (validateDecision(decisionId, newDecision)) {
            client.createDecision(newDecision)?.let { (vardaDecisionId) ->
                db.transaction {
                    insertVardaDecision(
                        it,
                        VardaDecisionTableRow(
                            id = UUID.randomUUID(),
                            vardaDecisionId = vardaDecisionId,
                            evakaDecisionId = decisionId,
                            evakaPlacementId = null,
                            createdAt = Instant.now(),
                            uploadedAt = Instant.now()
                        )
                    )
                }
            }
        }
    }

    val newDerivedDecisions = db.read { getNewDerivedDecisions(it, client.getChildUrl) }
    newDerivedDecisions.forEach { (placementId, newDecision) ->
        if (validateDecision(placementId, newDecision)) {
            client.createDecision(newDecision)?.let { (vardaDecisionId) ->
                db.transaction {
                    insertVardaDecision(
                        it,
                        VardaDecisionTableRow(
                            id = UUID.randomUUID(),
                            vardaDecisionId = vardaDecisionId,
                            evakaDecisionId = null,
                            evakaPlacementId = placementId,
                            createdAt = Instant.now(),
                            uploadedAt = Instant.now()
                        )
                    )
                }
            }
        }
    }

    val newTemporaryDecisions = db.read { getNewTemporaryDecisions(it, client.getChildUrl) }
    newTemporaryDecisions.forEach { (placementId, newDecision) ->
        if (validateDecision(placementId, newDecision)) {
            client.createDecision(newDecision)?.let { (vardaDecisionId) ->
                db.transaction {
                    insertVardaDecision(
                        it,
                        VardaDecisionTableRow(
                            id = UUID.randomUUID(),
                            vardaDecisionId = vardaDecisionId,
                            evakaDecisionId = null,
                            evakaPlacementId = placementId,
                            createdAt = Instant.now(),
                            uploadedAt = Instant.now()
                        )
                    )
                }
            }
        }
    }
}

fun sendUpdatedDecisions(db: Database.Connection, client: VardaClient) {
    val updatedDecisions = db.read { getUpdatedDecisions(it, client.getChildUrl) }
    val updatedDerivedDecisions = db.read { getUpdatedDerivedDecisions(it, client.getChildUrl) }
    val updatedTemporaryDecisions = db.read { getUpdatedTemporaryDecisions(it, client.getChildUrl) }
    val decisionIds = updatedDecisions.map { it.first } + updatedDerivedDecisions.map { it.first } + updatedTemporaryDecisions.map { it.first }
    cleanUpDecisionRelatedData(db, client, decisionIds)
    (updatedDecisions + updatedDerivedDecisions + updatedTemporaryDecisions)
        .forEach { (id, vardaDecisionId, updatedDecision) ->
            if (validateDecision(id, updatedDecision)) {
                client.updateDecision(vardaDecisionId, updatedDecision)?.let {
                    db.transaction { updateDecisionUploadTimestamp(it, id) }
                }
            }
        }
}

fun cleanUpDecisionRelatedData(db: Database.Connection, client: VardaClient, decisionIds: List<UUID>) {
    // Delete placements and fee data related to these decisions before decision update or delete
    // since Varda does not clean them up automatically
    deleteDecisionPlacements(db, client, decisionIds)
    deleteDecisionFeeData(db, client, decisionIds)
}

fun deleteDecisionPlacements(db: Database.Connection, client: VardaClient, decisionIds: List<UUID>) {
    val decisionPlacementIds = db.read {
        it.createQuery("SELECT varda_placement_id FROM varda_placement WHERE decision_id = ANY(:decisionIds)")
            .bind("decisionIds", decisionIds.toTypedArray())
            .mapTo<Long>()
            .toList()
    }
    decisionPlacementIds.forEach { id ->
        if (client.deletePlacement(id)) {
            db.transaction { deletePlacement(it, id) }
        }
    }
}

fun deleteDecisionFeeData(db: Database.Connection, client: VardaClient, decisionIds: List<UUID>) {
    val decisionFeeDataIds = db.read {
        it.createQuery("SELECT varda_id FROM varda_fee_data WHERE varda_decision_id = ANY(:decisionIds)")
            .bind("decisionIds", decisionIds.toTypedArray())
            .mapTo<Long>()
            .toList()
    }
    decisionFeeDataIds.forEach { id ->
        if (client.deleteFeeData(id)) {
            db.transaction { deleteVardaFeeData(it, id) }
        }
    }
}

fun removeDeletedDecisions(db: Database.Connection, client: VardaClient) {
    val removedDecisions = db.read { getRemovedDecisions(it) + getRemovedDerivedDecisions(it) }
    val decisionIds = removedDecisions.map { (id, _) -> id }
    cleanUpDecisionRelatedData(db, client, decisionIds)
    removedDecisions.forEach { (_, vardaDecisionId) ->
        client.deleteDecision(vardaDecisionId).let { success ->
            if (success) db.transaction { deleteDecision(it, vardaDecisionId) }
        }
    }
}

private val acceptedStatus = DecisionStatus.ACCEPTED.name
private val daycareDecisionTypes = arrayOf(DAYCARE.name, DAYCARE_PART_TIME.name, PRESCHOOL_DAYCARE.name)

val acceptedDaycareDecisionsQuery =
    """
SELECT 
    d.id,
    a.childid AS child_id,
    d.unit_id,
    d.application_id,
    d.type,
    d.start_date,
    coalesce((next_decision.start_date - interval '1 day')::date, d.end_date) AS end_date,
    a.sentdate AS application_date,
    a.urgent,
    greatest(d.updated, d.resolved, next_decision.updated) AS updated
FROM decision d
JOIN application_view a ON d.application_id = a.id
LEFT JOIN LATERAL (
    SELECT start_date, greatest(updated, resolved) AS updated FROM decision d2
    WHERE d2.application_id IN (SELECT id FROM application WHERE child_id = a.childid)
    AND d2.status = '$acceptedStatus'::decision_status
    AND d2.type IN (${daycareDecisionTypes.joinToString(", ") { type -> "'$type'::decision_type" }})
    AND d2.id != d.id
    AND d2.resolved > d.resolved
    AND daterange(d.start_date, d.end_date, '[]') && daterange(d2.start_date, d2.end_date, '[]')
    ORDER BY start_date ASC LIMIT 1
) next_decision ON TRUE
WHERE d.status = '$acceptedStatus'::decision_status
AND d.type IN (${daycareDecisionTypes.joinToString(", ") { type -> "'$type'::decision_type" }})
AND (d.start_date < next_decision.start_date OR next_decision.start_date IS NULL)
AND d.start_date < current_date
    """.trimIndent()

private val decisionQueryBase =
    // language=SQL
    """
WITH accepted_daycare_decision AS (
    $acceptedDaycareDecisionsQuery
)
SELECT
    vd.id,
    vd.varda_decision_id,
    d.id AS evaka_id,
    vc.varda_child_id,
    least(d.start_date, d.application_date, first_placement.start_date) AS application_date,
    least(d.start_date, first_placement.start_date) AS start_date,
    greatest(d.end_date, last_placement.end_date) AS end_date,
    d.urgent,
    (CASE WHEN last_placement.type = 'PREPARATORY_DAYCARE'
    THEN latest_sn.hours_per_week - 25
    WHEN last_placement.type = 'PRESCHOOL_DAYCARE'
    THEN latest_sn.hours_per_week - 20
    ELSE latest_sn.hours_per_week END) 
    AS hours_per_week,
    false AS temporary, -- temporary placements are handled separately
    NOT latest_sn.part_week AS daily,
    latest_sn.shift_care AS shift_care,
    u.provider_type AS provider_type
FROM accepted_daycare_decision d
JOIN daycare u ON (d.unit_id = u.id AND u.upload_to_varda = true)
JOIN varda_child vc ON d.child_id = vc.person_id AND u.oph_organizer_oid = vc.oph_organizer_oid
LEFT JOIN varda_decision vd ON d.id = vd.evaka_decision_id AND vd.deleted_at IS NULL
LEFT JOIN LATERAL (
    SELECT * FROM placement p 
    WHERE p.child_id = vc.person_id 
    AND daterange(d.start_date, d.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
    ORDER BY p.start_date ASC
    LIMIT 1
) first_placement ON TRUE
LEFT JOIN LATERAL (
    SELECT * FROM placement p 
    WHERE p.child_id = vc.person_id 
    AND daterange(d.start_date, d.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
    ORDER BY p.end_date DESC
    LIMIT 1
) last_placement ON TRUE
JOIN LATERAL (
    SELECT * FROM service_need sn
    WHERE sn.child_id = d.child_id
    AND daterange(sn.start_date, sn.end_date, '[]') && daterange(d.start_date, d.end_date, '[]')
    AND NOT temporary
    ORDER BY sn.start_date DESC
    LIMIT 1
) latest_sn ON TRUE
    """.trimIndent()

private fun getNewDecisions(tx: Database.Read, getChildUrl: (Long) -> String): List<Pair<UUID, VardaDecision>> {
    val sql =
        """
$decisionQueryBase
WHERE vd.id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .map(toVardaDecisionWithDecisionId(getChildUrl))
        .toList()
}

fun insertVardaDecision(tx: Database.Transaction, decision: VardaDecisionTableRow) {
    val sql =
        """
INSERT INTO varda_decision (
    id,
    varda_decision_id,
    evaka_decision_id,
    evaka_placement_id,
    created_at,
    uploaded_at
) VALUES (
    :id,
    :vardaDecisionId,
    :evakaDecisionId,
    :evakaPlacementId,
    :createdAt,
    :uploadedAt
)
        """.trimIndent()

    tx.createUpdate(sql)
        .bind("id", decision.id)
        .bind("vardaDecisionId", decision.vardaDecisionId)
        .bind("evakaDecisionId", decision.evakaDecisionId)
        .bind("evakaPlacementId", decision.evakaPlacementId)
        .bind("createdAt", decision.createdAt)
        .bind("uploadedAt", decision.uploadedAt)
        .execute()
}

private fun getUpdatedDecisions(tx: Database.Read, getChildUrl: (Long) -> String): List<Triple<UUID, Long, VardaDecision>> {
    val sql =
        """
$decisionQueryBase
WHERE vd.uploaded_at < greatest(latest_sn.updated, d.updated, first_placement.updated, last_placement.updated)
        """.trimIndent()

    return tx.createQuery(sql)
        .map(toVardaDecisionWithIdAndVardaId(getChildUrl))
        .toList()
}

private fun updateDecisionUploadTimestamp(tx: Database.Transaction, id: UUID, uploadedAt: Instant = Instant.now()) {
    val sql = "UPDATE varda_decision SET uploaded_at = :uploadedAt WHERE id = :id"
    tx.createUpdate(sql)
        .bind("id", id)
        .bind("uploadedAt", uploadedAt)
        .execute()
}

private val derivedDecisionQueryBase =
    // language=SQL
    """
WITH placement_without_decision AS (
SELECT p.*
FROM placement p
WHERE NOT EXISTS(
    SELECT 1 FROM decision d
    LEFT JOIN application a ON d.application_id = a.id
    WHERE a.child_id = p.child_id
    AND daterange(p.start_date, p.end_date, '[]') && daterange(d.start_date, d.end_date, '[]')
    AND d.status = 'ACCEPTED'
)
AND p.type = ANY(:types::placement_type[])
AND p.start_date < current_date
)
SELECT
    vd.id,
    vd.varda_decision_id,
    p.id AS evaka_id,
    vc.varda_child_id,
    p.start_date - interval '4 months' AS application_date,
    p.start_date,
    p.end_date,
    false AS urgent,
    latest_sn.hours_per_week,
    false AS temporary, -- temporary placements are handled separately
    NOT latest_sn.part_week AS daily,
    latest_sn.shift_care AS shift_care,
    u.provider_type AS provider_type
FROM placement_without_decision p
LEFT JOIN varda_decision vd ON p.id = vd.evaka_placement_id AND vd.deleted_at IS NULL
JOIN daycare u ON p.unit_id = u.id
JOIN varda_child vc ON p.child_id = vc.person_id AND u.oph_organizer_oid = vc.oph_organizer_oid
JOIN LATERAL (
    SELECT * FROM service_need sn
    WHERE sn.child_id = p.child_id
    AND daterange(sn.start_date, sn.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
    AND NOT temporary
    ORDER BY sn.start_date DESC
    LIMIT 1
) latest_sn ON true
    """.trimIndent()

private val temporaryDecisionQueryBase =
    // language=sql
    """
SELECT
    vd.id,
    vd.varda_decision_id,
    p.id AS evaka_id,
    vc.varda_child_id,
    greatest(p.start_date, sn.start_date) - interval '4 months' AS application_date,
    greatest(p.start_date, sn.start_date) AS start_date,
    least(p.end_date, sn.end_date) AS end_date,
    false AS urgent,
    sn.hours_per_week,
    true AS temporary,
    NOT sn.part_week AS daily,
    sn.shift_care AS shift_care,
    u.provider_type AS provider_type
FROM placement p
LEFT JOIN varda_decision vd ON p.id = vd.evaka_placement_id AND vd.deleted_at IS NULL
JOIN daycare u ON p.unit_id = u.id
JOIN varda_child vc ON p.child_id = vc.person_id AND u.oph_organizer_oid = vc.oph_organizer_oid
JOIN service_need sn 
    ON sn.child_id = p.child_id 
    AND daterange(sn.start_date, sn.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
    AND sn.temporary
    """.trimIndent()

private fun getNewDerivedDecisions(tx: Database.Read, getChildUrl: (Long) -> String): List<Pair<UUID, VardaDecision>> {
    val sql =
        """
$derivedDecisionQueryBase
WHERE vd.id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("types", daycarePlacementTypes)
        .map(toVardaDecisionWithDecisionId(getChildUrl))
        .toList()
}

private fun getUpdatedDerivedDecisions(
    tx: Database.Read,
    getChildUrl: (Long) -> String
): List<Triple<UUID, Long, VardaDecision>> {
    val sql =
        """
$derivedDecisionQueryBase
WHERE vd.uploaded_at < greatest(latest_sn.updated, p.updated)
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("types", daycarePlacementTypes)
        .map(toVardaDecisionWithIdAndVardaId(getChildUrl))
        .toList()
}

private fun getRemovedDecisions(tx: Database.Read): List<Pair<UUID, Long>> {
    val sql =
        """
WITH accepted_daycare_decision AS (
    $acceptedDaycareDecisionsQuery
)
SELECT vd.id, vd.varda_decision_id FROM varda_decision vd
LEFT JOIN accepted_daycare_decision d ON vd.evaka_decision_id = d.id
WHERE vd.evaka_placement_id IS NULL
AND d.id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .map { rs, _ -> rs.getUUID("id") to rs.getLong("varda_decision_id") }
        .toList()
}

private fun getRemovedDerivedDecisions(tx: Database.Read): List<Pair<UUID, Long>> {
    val sql =
        """
WITH derived_decision AS (
    $derivedDecisionQueryBase
    UNION
    $temporaryDecisionQueryBase
)
SELECT vd.id, vd.varda_decision_id FROM varda_decision vd
LEFT JOIN derived_decision d ON vd.evaka_placement_id = d.evaka_id
WHERE vd.evaka_decision_id IS NULL
AND d.evaka_id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("types", daycarePlacementTypes)
        .map { rs, _ -> rs.getUUID("id") to rs.getLong("varda_decision_id") }
        .toList()
}

private fun getNewTemporaryDecisions(tx: Database.Read, getChildUrl: (Long) -> String): List<Pair<UUID, VardaDecision>> {
    val sql =
        """
$temporaryDecisionQueryBase
WHERE vd.id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .map(toVardaDecisionWithDecisionId(getChildUrl))
        .toList()
}

private fun getUpdatedTemporaryDecisions(
    tx: Database.Read,
    getChildUrl: (Long) -> String
): List<Triple<UUID, Long, VardaDecision>> {
    val sql =
        """
$temporaryDecisionQueryBase
WHERE vd.uploaded_at < greatest(sn.updated, p.updated)
        """.trimIndent()

    return tx.createQuery(sql)
        .map(toVardaDecisionWithIdAndVardaId(getChildUrl))
        .toList()
}

private fun deleteDecision(tx: Database.Transaction, vardaDecisionId: Long) {
    tx.createUpdate("DELETE FROM varda_decision WHERE varda_decision_id = :id")
        .bind("id", vardaDecisionId)
        .execute()
}

fun softDeleteDecision(tx: Database.Transaction, vardaDecisionId: Long) {
    tx.createUpdate("UPDATE varda_decision SET deleted_at = NOW() WHERE varda_decision_id = :vardaDecisionId")
        .bind("vardaDecisionId", vardaDecisionId)
        .execute()
}

fun getDecisionsToDelete(tx: Database.Read): List<Long> {
    return tx.createQuery(
        // language=SQL
        """
SELECT varda_decision_id 
FROM varda_decision
WHERE should_be_deleted = true
AND deleted_at IS NULL
"""
    )
        .mapTo(Long::class.java)
        .toList()
}

private fun validateDecision(id: UUID, decision: VardaDecision): Boolean {
    if (decision.hoursPerWeek < 1.0) {
        logger.info { "Won't create Varda decision (decision/placement id: $id, reason: hours per week < 1)" }
        return false
    }
    return true
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaDecision(
    @JsonProperty("lapsi")
    val childUrl: String,
    @JsonProperty("hakemus_pvm")
    val applicationDate: LocalDate,
    @JsonProperty("alkamis_pvm")
    val startDate: LocalDate,
    @JsonProperty("paattymis_pvm")
    val endDate: LocalDate,
    @JsonProperty("pikakasittely_kytkin")
    val urgent: Boolean,
    @JsonProperty("tuntimaara_viikossa")
    val hoursPerWeek: Double,
    @JsonProperty("tilapainen_vaka_kytkin")
    val temporary: Boolean,
    @JsonProperty("paivittainen_vaka_kytkin")
    val daily: Boolean,
    @JsonProperty("vuorohoito_kytkin")
    val shiftCare: Boolean,
    @JsonProperty("jarjestamismuoto_koodi")
    val providerTypeCode: String
) {
    @JsonProperty("kokopaivainen_vaka_kytkin")
    val fullDay: Boolean = hoursPerWeek >= 25
}

data class VardaDecisionResponse(
    @JsonProperty("id")
    val vardaDecisionId: Long
)

data class VardaDecisionTableRow(
    val id: UUID,
    val vardaDecisionId: Long,
    val evakaDecisionId: UUID?,
    val evakaPlacementId: UUID?,
    val createdAt: Instant,
    val uploadedAt: Instant
)

private fun toVardaDecisionWithDecisionId(getChildUrl: (Long) -> String): (ResultSet, StatementContext) -> Pair<UUID, VardaDecision> =
    { rs, _ -> rs.getUUID("evaka_id") to toVardaDecision(rs, getChildUrl) }

private fun toVardaDecisionWithIdAndVardaId(getChildUrl: (Long) -> String): (ResultSet, StatementContext) -> Triple<UUID, Long, VardaDecision> =
    { rs, _ ->
        Triple(
            rs.getUUID("id"),
            rs.getLong("varda_decision_id"),
            toVardaDecision(rs, getChildUrl)
        )
    }

private fun toVardaDecision(rs: ResultSet, getChildUrl: (Long) -> String) = VardaDecision(
    childUrl = getChildUrl(rs.getLong("varda_child_id")),
    applicationDate = rs.getDate("application_date").toLocalDate(),
    startDate = rs.getDate("start_date").toLocalDate(),
    endDate = rs.getDate("end_date").toLocalDate(),
    urgent = rs.getBoolean("urgent"),
    hoursPerWeek = rs.getDouble("hours_per_week"),
    temporary = rs.getBoolean("temporary"),
    daily = rs.getBoolean("daily"),
    shiftCare = rs.getBoolean("shift_care"),
    providerTypeCode = rs.getEnum<VardaUnitProviderType>("provider_type").vardaCode
)

val toVardaDecisionRow: (ResultSet, StatementContext) -> VardaDecisionTableRow = { rs, _ ->
    VardaDecisionTableRow(
        id = rs.getUUID("id"),
        vardaDecisionId = rs.getLong("varda_decision_id"),
        evakaDecisionId = rs.getString("evaka_decision_id")?.let(UUID::fromString),
        evakaPlacementId = rs.getString("evaka_placement_id")?.let(UUID::fromString),
        createdAt = rs.getTimestamp("created_at").toInstant(),
        uploadedAt = rs.getTimestamp("uploaded_at").toInstant()
    )
}
