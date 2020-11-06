// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.invoicing.domain.FeeAlterationWithEffect
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun updateFeeData(
    h: Handle,
    client: VardaClient,
    mapper: ObjectMapper,
    personService: PersonService
) {
    deleteEntriesFromAnnulledFeeDecision(h, client)
    deleteEntriesFromDeletedPlacements(h, client)
    updateEntriesFromModifiedPlacements(h, client, mapper, personService)
    uploadNewFeeData(h, client, mapper, personService)
}

fun removeMarkedFeeDataFromVarda(h: Handle, client: VardaClient) {
    val feeDataIds: List<Int> = getFeeDataToDelete(h)
    feeDataIds.forEach { id ->
        if (client.deleteFeeData(id)) {
            softDeleteFeeData(h, id)
        }
    }
}

fun deleteEntriesFromAnnulledFeeDecision(h: Handle, client: VardaClient) {
    getAnnulledFeeDecisions(h)
        .forEach { vardaId ->
            if (client.deleteFeeData(vardaId)) {
                deleteVardaFeeDecision(vardaId, h)
            }
        }
}

fun deleteEntriesFromDeletedPlacements(h: Handle, client: VardaClient) {
    // also removes legacy fee data entries since their placementId is null
    getDeletedPlacements(h)
        .forEach { vardaId ->
            if (client.deleteFeeData(vardaId)) {
                deleteVardaFeeDecision(vardaId, h)
            }
        }
}

fun updateEntriesFromModifiedPlacements(h: Handle, client: VardaClient, mapper: ObjectMapper, personService: PersonService) {
    getModifiedFeeData(h, mapper)
        .forEach { (vardaId, feeDataBase) ->
            val feeData = baseToFeeData(h, feeDataBase, personService, client)
            if (feeData != null && client.updateFeeData(vardaId, feeData)) {
                insertFeeDataUpload(h, feeDataBase, vardaId)
            }
        }
}

fun uploadNewFeeData(
    h: Handle,
    client: VardaClient,
    mapper: ObjectMapper,
    personService: PersonService
) {
    val feeData = getStaleFeeData(h, mapper)
    feeData.forEach { feeDataBase ->
        val newFeeData = baseToFeeData(h, feeDataBase, personService, client)
        if (newFeeData != null) {
            val response = client.createFeeData(newFeeData)
            if (response != null) {
                insertFeeDataUpload(h, feeDataBase, response.vardaId)
            }
        } else {
            logger.info { "Not sending Varda fee data because child has no guardians in VTJ (child: ${feeDataBase.evakaChildId})" }
        }
    }
}

fun getFeeDataToDelete(h: Handle): List<Int> {
    return h.createQuery(
        // language=SQL
        """
SELECT varda_fee_data_id 
FROM varda_fee_data
WHERE should_be_deleted = true
AND deleted_at IS NULL
"""
    )
        .mapTo(Int::class.java)
        .toList()
}

fun softDeleteFeeData(h: Handle, vardaFeeDataId: Int) {
    h.createUpdate("UPDATE varda_fee_data SET deleted_at = NOW() WHERE varda_fee_data_id = :vardaFeeDataId")
        .bind("vardaFeeDataId", vardaFeeDataId)
        .execute()
}

private fun getStaleFeeData(
    h: Handle,
    mapper: ObjectMapper
): List<VardaFeeDataBase> {
    val placementTypes = listOf(PlacementType.DAYCARE, PlacementType.FIVE_YEARS_OLD_DAYCARE, PlacementType.PRESCHOOL_WITH_DAYCARE, PlacementType.PREPARATORY_WITH_DAYCARE).toTypedArray()
    // language=SQL
    val sql =
        """    
        $feeDataQueryBase
        WHERE vfd.id IS NULL
            AND fdp.placement_type = ANY(:placementTypes)
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("sentStatus", FeeDecisionStatus.SENT)
        .bind("placementTypes", placementTypes)
        .map { rs, _ -> toVardaFeeDataBase(rs, mapper) }
        .toList()
}

private fun insertFeeDataUpload(h: Handle, feeDataBase: VardaFeeDataBase, vardaId: Long) {
    // language=SQL
    val sql =
        """
        INSERT INTO varda_fee_data (evaka_fee_decision_id, evaka_placement_id, varda_fee_data_id, uploaded_at)
        VALUES (:decisionId, :placementId, :vardaId, :checkTimestamp)
        ON CONFLICT ON CONSTRAINT varda_fee_data_evaka_fee_decision_id_evaka_placement_id_key
        DO UPDATE SET uploaded_at = :checkTimestamp
        """.trimIndent()

    h.createUpdate(sql)
        .bind("decisionId", feeDataBase.evakaFeeDecisionId)
        .bind("placementId", feeDataBase.placementId)
        .bind("vardaId", vardaId)
        .bind("checkTimestamp", feeDataBase.checkTimestamp)
        .execute()
}

private fun getAnnulledFeeDecisions(h: Handle): List<Int> {
    // language=SQL
    val sql =
        """
        SELECT varda_fee_data_id
        FROM varda_fee_data
            LEFT JOIN fee_decision ON fee_decision.id = varda_fee_data.evaka_fee_decision_id
        WHERE status = :annulledStatus
            AND varda_fee_data.id IS NOT NULL;
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("annulledStatus", FeeDecisionStatus.ANNULLED)
        .mapTo<Int>().list()
}

private fun deleteVardaFeeDecision(vardaId: Int, h: Handle) {
    // language=SQL
    val sql =
        """
        DELETE FROM varda_fee_data
        WHERE varda_fee_data_id = :vardaId
        """.trimIndent()

    h.createUpdate(sql)
        .bind("vardaId", vardaId)
        .execute()
}

private fun getDeletedPlacements(h: Handle): List<Int> {
    // language=SQL
    val sql =
        """
            SELECT varda_fee_data_id 
            FROM varda_fee_data
            WHERE evaka_placement_id IS NULL
        """.trimIndent()

    return h.createQuery(sql)
        .mapTo<Int>()
        .list()
}

private fun getModifiedFeeData(h: Handle, mapper: ObjectMapper): List<Pair<Long, VardaFeeDataBase>> {
    // language=SQL
    val sql =
        """
         $feeDataQueryBase
        WHERE vfd.varda_fee_data_id IS NOT NULL 
        AND vfd.uploaded_at < p.updated
        """.trimIndent()

    return h
        .createQuery(sql)
        .bind("sentStatus", FeeDecisionStatus.SENT)
        .map { rs, _ -> toVardaFeeDataBaseWithVardaId(rs, mapper) }
        .toList()
}

private val feeDataQueryBase =
    // language=SQL
    """
        SELECT fd.id                              AS fee_decision_id,
            vfd.varda_fee_data_id                 AS varda_id,
            p.id                                  AS placement_id,
            p.child_id                            AS evaka_child_id,
            vc.varda_child_id                     AS varda_child_id,
            GREATEST(fd.valid_from, p.start_date) AS start_date,
            LEAST(fd.valid_to, p.end_date)        AS end_date,
            fdp.fee,
            family_size,
            fdp.fee_alterations,
            fdp.placement_type,
            now()                               AS check_timestamp
        FROM varda_placement vp
            JOIN placement p ON vp.evaka_placement_id = p.id AND vp.deleted_at IS NULL
            LEFT JOIN varda_fee_data vfd ON p.id = vfd.evaka_placement_id AND vfd.deleted_at IS NULL
            JOIN fee_decision_part fdp ON fdp.child = p.child_id
            JOIN fee_decision fd ON (fdp.fee_decision_id = fd.id AND daterange(p.start_date, p.end_date, '[]') && daterange(fd.valid_from, fd.valid_to, '[]') AND fd.status = :sentStatus)
            JOIN daycare u ON p.unit_id = u.id
            JOIN varda_child vc ON p.child_id = vc.person_id AND vc.oph_organizer_oid = u.oph_organizer_oid
    """.trimIndent()

private fun baseToFeeData(h: Handle, feeDataBase: VardaFeeDataBase, personService: PersonService, client: VardaClient): VardaFeeData? {
    val guardians = personService
        .getGuardians(h, AuthenticatedUser.anonymous, feeDataBase.evakaChildId)
        .map { guardian ->
            VardaGuardian(
                ssn = guardian.identity.toString(),
                firstName = guardian.firstName!!,
                lastName = guardian.lastName!!
            )
        }

    return when (guardians.isNotEmpty()) {
        true -> VardaFeeData(
            guardians = guardians,
            childUrl = client.getChildUrl(feeDataBase.vardaChildId),
            voucherAmount = 0.0,
            feeCode = if (feeDataBase.placementType == PlacementType.FIVE_YEARS_OLD_DAYCARE) "MP02" else "MP03",
            feeAmount = feeDataBase.finalFee(),
            familySize = feeDataBase.familySize,
            startDate = feeDataBase.startDate,
            endDate = feeDataBase.endDate
        )
        false -> null
    }
}

private fun toVardaFeeDataBase(rs: ResultSet, mapper: ObjectMapper) = VardaFeeDataBase(
    evakaFeeDecisionId = rs.getUUID("fee_decision_id"),
    placementId = rs.getUUID("placement_id"),
    evakaChildId = rs.getUUID("evaka_child_id"),
    vardaChildId = rs.getLong("varda_child_id"),
    startDate = rs.getDate("start_date").toLocalDate(),
    endDate = rs.getDate("end_date").toLocalDate(),
    fee = rs.getInt("fee"),
    familySize = rs.getInt("family_size"),
    feeAlterations = mapper.readValue(rs.getString("fee_alterations")),
    placementType = PlacementType.valueOf(rs.getString("placement_type")),
    checkTimestamp = rs.getTimestamp("check_timestamp").toInstant()
)

private fun toVardaFeeDataBaseWithVardaId(rs: ResultSet, mapper: ObjectMapper): Pair<Long, VardaFeeDataBase> =
    rs.getLong("varda_id") to toVardaFeeDataBase(rs, mapper)

data class VardaGuardian(
    @JsonProperty("henkilotunnus")
    val ssn: String,
    @JsonProperty("etunimet")
    val firstName: String,
    @JsonProperty("sukunimi")
    val lastName: String
)

data class VardaFeeDataBase(
    val evakaFeeDecisionId: UUID,
    val placementId: UUID,
    val evakaChildId: UUID,
    val vardaChildId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val fee: Int,
    val familySize: Int,
    val feeAlterations: List<FeeAlterationWithEffect> = listOf(),
    val placementType: PlacementType,
    val checkTimestamp: Instant
) {
    fun finalFee(): Double = (fee + feeAlterations.sumBy { it.effect }) / 100.0
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaFeeData(
    @JsonProperty("huoltajat")
    val guardians: List<VardaGuardian>,
    @JsonProperty("lapsi")
    val childUrl: String,
    @JsonProperty("maksun_peruste_koodi")
    val feeCode: String,
    @JsonProperty("palveluseteli_arvo")
    val voucherAmount: Double,
    @JsonProperty("asiakasmaksu")
    val feeAmount: Double,
    @JsonProperty("perheen_koko")
    val familySize: Int,
    @JsonProperty("alkamis_pvm")
    val startDate: LocalDate,
    @JsonProperty("paattymis_pvm")
    val endDate: LocalDate?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaFeeDataResponse(
    @JsonProperty("id")
    val vardaId: Long
)

data class VardaFeeDataRow(
    val id: UUID,
    val evakaFeeDecisionId: UUID,
    val evakaPlacementId: UUID?,
    val vardaFeeDataId: Long?,
    val createdAt: Instant,
    val uploadedAt: Instant
)
