// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun updateFeeData(
    db: Database.Connection,
    client: VardaClient,
    personService: PersonService
) {
    deleteAnnulledFeeData(db, client)
    createAndUpdateFeeData(db, client, personService)
}

fun removeMarkedFeeDataFromVarda(db: Database.Connection, client: VardaClient) {
    val markedFeeData = db.read { getMarkedFeeData(it) }
    logger.info { "Varda: Deleting ${markedFeeData.size} marked fee data" }
    markedFeeData.forEach { vardaId ->
        if (client.deleteFeeData(vardaId)) {
            db.transaction { deleteVardaFeeData(it, vardaId) }
        }
    }
}

fun deleteAnnulledFeeData(db: Database.Connection, client: VardaClient) {
    val vardaIds = db.read { getAnnulledDecisions(it) }
    logger.info { "Varda: Deleting ${vardaIds.size} annulled fee data" }
    deleteFeeData(db, client, vardaIds)
}

fun deleteFeeData(db: Database.Connection, client: VardaClient, vardaIds: List<Long>) {
    logger.info { "Varda: Deleting ${vardaIds.size} fee data records" }
    vardaIds.forEach { vardaId ->
        if (client.deleteFeeData(vardaId)) {
            logger.info { "Varda: Deleting fee data from db by id $vardaId" }
            db.transaction { deleteVardaFeeData(it, vardaId) }
        }
    }
}

fun createAndUpdateFeeData(db: Database.Connection, client: VardaClient, personService: PersonService) {
    val children = db.read { getChildrenWithOutdatedFeeData(it) }
    children.forEach { (childId, childVardaId) ->
        val vardaDecisionPeriods = client.getChildDecisions(childVardaId)

        val outdatedFeeData = vardaDecisionPeriods.flatMap { decisionPeriod ->
            db.read { getOutdatedFeeData(it, decisionPeriod) }
        }
        logger.info { "Varda: Deleting ${outdatedFeeData.size} outdated fee data" }
        deleteFeeData(db, client, outdatedFeeData)

        val guardians = db.transaction { personService.getEvakaOrVtjGuardians(it, AuthenticatedUser.SystemInternalUser, childId) }
            .filter { (it.firstName + it.lastName).isNotBlank() }
        if (guardians.isEmpty()) return@forEach

        val (newFeeData, updatedFeeData) = vardaDecisionPeriods
            .flatMap { decisionPeriod ->
                db.read {
                    getDecisionFeeData(it, childVardaId, decisionPeriod)
                        .map { data -> decisionPeriod.id to data }
                }
            }
            .partition { (_, data) -> data.vardaId == null }

        logger.info { "Varda: Updating ${updatedFeeData.size} updated fee data" }
        updatedFeeData.forEach { (_, data) ->
            val richData = baseToFeeData(data, guardians, client.getChildUrl(data.vardaChildId), client.sourceSystem)
            client.updateFeeData(data.vardaId!!, richData).let { success ->
                if (success) db.transaction { updateFeeDataUploadedAt(it, data.vardaId, Instant.now()) }
            }
        }

        logger.info { "Varda: Sending ${newFeeData.size} new fee data" }
        newFeeData.forEach { (vardaDecisionId, data) ->
            val richData = baseToFeeData(data, guardians, client.getChildUrl(data.vardaChildId), client.sourceSystem)
            client.createFeeData(richData)?.let { (vardaId) ->
                db.transaction {
                    insertFeeDataUpload(it, vardaId, data.feeDecisionId, data.voucherValueDecisionId, vardaDecisionId, childVardaId)
                }
            }
        }
    }
}

fun getMarkedFeeData(tx: Database.Read): List<Long> {
    return tx
        .createQuery("SELECT varda_id FROM varda_fee_data WHERE should_be_deleted = true")
        .mapTo<Long>()
        .toList()
}

private fun insertFeeDataUpload(
    tx: Database.Transaction,
    vardaId: Long,
    feeDecisionId: UUID?,
    voucherValueDecisionId: UUID?,
    vardaDecisionId: Long,
    vardaChildId: Long
) {
    tx.createUpdate(
        """
INSERT INTO varda_fee_data (varda_id, evaka_fee_decision_id, evaka_voucher_value_decision_id, varda_decision_id, varda_child_id, uploaded_at)
VALUES (
    :vardaId,
    :feeDecisionId,
    :evakaVoucherValueDecisionId,
    (SELECT id FROM varda_decision WHERE varda_decision_id = :vardaDecisionId),
    (SELECT id FROM varda_child WHERE varda_child_id = :vardaChildId),
    :uploadedAt
)
"""
    )
        .bind("vardaId", vardaId)
        .bind("feeDecisionId", feeDecisionId)
        .bind("evakaVoucherValueDecisionId", voucherValueDecisionId)
        .bind("vardaDecisionId", vardaDecisionId)
        .bind("vardaChildId", vardaChildId)
        .bind("uploadedAt", Instant.now())
        .execute()
}

private fun updateFeeDataUploadedAt(tx: Database.Transaction, vardaId: Long, timestamp: Instant) {
    tx.createUpdate("UPDATE varda_fee_data SET uploaded_at = :timestamp WHERE varda_id = :vardaId")
        .bind("vardaId", vardaId)
        .bind("timestamp", timestamp)
        .execute()
}

private fun getAnnulledDecisions(tx: Database.Read): List<Long> {
    return tx
        .createQuery(
            """
SELECT varda_id
FROM varda_fee_data
JOIN new_fee_decision ON new_fee_decision.id = varda_fee_data.evaka_fee_decision_id
    AND new_fee_decision.status = :annulledFeeDecision::fee_decision_status

UNION ALL

SELECT varda_id
FROM varda_fee_data
JOIN voucher_value_decision ON voucher_value_decision.id = varda_fee_data.evaka_voucher_value_decision_id
    AND voucher_value_decision.status = :annulledVoucherDecision::voucher_value_decision_status
"""
        )
        .bind("annulledFeeDecision", FeeDecisionStatus.ANNULLED)
        .bind("annulledVoucherDecision", VoucherValueDecisionStatus.ANNULLED)
        .mapTo<Long>()
        .toList()
}

private fun getChildrenWithOutdatedFeeData(tx: Database.Read): List<Pair<UUID, Long>> {
    return tx.createQuery(
        """
SELECT varda_child.person_id, varda_child.varda_child_id
FROM varda_child
JOIN LATERAL (
    SELECT MAX(approved_at) approved_at
    FROM (
        SELECT approved_at
        FROM new_fee_decision d
        JOIN new_fee_decision_child p ON d.id = p.fee_decision_id AND p.child_id = varda_child.person_id
        WHERE d.status = ANY(:effectiveFeeDecision::fee_decision_status[])
        UNION ALL
        SELECT approved_at
        FROM voucher_value_decision d
        WHERE d.child_id = varda_child.person_id AND d.status = ANY(:effectiveVoucherValueDecision::voucher_value_decision_status[])
    ) decisions
) latest_finance_decision ON true
JOIN LATERAL (
    SELECT MAX(uploaded_at) uploaded_at
    FROM varda_fee_data
    WHERE varda_child_id = varda_child.id
) latest_fee_data ON true
WHERE varda_child.varda_child_id IS NOT NULL
AND COALESCE(latest_fee_data.uploaded_at <= COALESCE(latest_finance_decision.approved_at, '-infinity'), true)
"""
    )
        .bind("effectiveFeeDecision", FeeDecisionStatus.effective)
        .bind("effectiveVoucherValueDecision", VoucherValueDecisionStatus.effective)
        .map { rs, _ -> rs.getUUID("person_id") to rs.getLong("varda_child_id") }
        .toList()
}

fun getOutdatedFeeData(tx: Database.Read, decisionPeriod: VardaClient.DecisionPeriod): List<Long> {
    return tx
        .createQuery(
            """
SELECT vfd.varda_id
FROM varda_decision vd
JOIN varda_fee_data vfd ON vd.id = vfd.varda_decision_id
JOIN new_fee_decision d ON vfd.evaka_fee_decision_id = d.id AND NOT d.valid_during && :decisionPeriod
WHERE vd.varda_decision_id = :decisionId

UNION ALL

SELECT vfd.varda_id
FROM varda_decision vd
JOIN varda_fee_data vfd ON vd.id = vfd.varda_decision_id
JOIN voucher_value_decision d ON vfd.evaka_voucher_value_decision_id = d.id AND NOT daterange(d.valid_from, d.valid_to, '[]') && :decisionPeriod
WHERE vd.varda_decision_id = :decisionId
"""
        )
        .bind("decisionId", decisionPeriod.id)
        .bind("decisionPeriod", FiniteDateRange(decisionPeriod.alkamis_pvm, decisionPeriod.paattymis_pvm))
        .mapTo<Long>()
        .toList()
}

fun getDecisionFeeData(tx: Database.Read, childVardaId: Long, decisionPeriod: VardaClient.DecisionPeriod): List<VardaFeeDataBase> {
    return tx
        .createQuery(
            """
SELECT
    d.id AS fee_decision_id,
    NULL AS voucher_value_decision_id,
    vc.varda_child_id AS varda_child_id,
    vfd.varda_id AS varda_id,
    GREATEST(lower(d.valid_during), :decisionStartDate) AS start_date,
    LEAST(upper(d.valid_during) - interval '1 day', :decisionEndDate) AS end_date,
    p.final_fee / 100.0 AS fee,
    0.0 AS voucher_value,
    d.family_size AS family_size,
    p.placement_type = ANY(:feeDecisionFiveYearOldTypes::placement_type[]) AS is_five_year_old_daycare
FROM varda_child vc
JOIN new_fee_decision_child p ON vc.person_id = p.child_id
JOIN new_fee_decision d ON p.fee_decision_id = d.id
    AND d.valid_during && :decisionPeriod
    AND d.status = ANY(:effective::fee_decision_status[])
JOIN varda_decision vd ON vd.varda_decision_id = :decisionId
LEFT JOIN varda_fee_data vfd ON d.id = vfd.evaka_fee_decision_id AND vd.id = vfd.varda_decision_id AND vc.id = vfd.varda_child_id
WHERE vc.varda_child_id = :childVardaId

UNION ALL

SELECT
    NULL AS fee_decision_id,
    d.id AS voucher_value_decision_id,
    vc.varda_child_id AS varda_child_id,
    vfd.varda_id AS varda_id,
    GREATEST(d.valid_from, :decisionStartDate) AS start_date,
    LEAST(d.valid_to, :decisionEndDate) AS end_date,
    d.final_co_payment / 100.0 AS fee,
    d.voucher_value / 100.0 AS voucher_value,
    d.family_size AS family_size,
    d.placement_type = ANY(:valueDecisionFiveYearOldTypes::placement_type[]) AS is_five_year_old_daycare
FROM varda_child vc
JOIN voucher_value_decision d ON vc.person_id = d.child_id
    AND daterange(d.valid_from, d.valid_to, '[]') && :decisionPeriod
    AND d.status = ANY(:effective::voucher_value_decision_status[])
JOIN varda_decision vd ON vd.varda_decision_id = :decisionId
LEFT JOIN varda_fee_data vfd ON d.id = vfd.evaka_voucher_value_decision_id AND vd.id = vfd.varda_decision_id AND vc.id = vfd.varda_child_id
WHERE vc.varda_child_id = :childVardaId
"""
        )
        .bind("childVardaId", childVardaId)
        .bind("decisionId", decisionPeriod.id)
        .bind("decisionStartDate", decisionPeriod.alkamis_pvm)
        .bind("decisionEndDate", decisionPeriod.paattymis_pvm)
        .bind("decisionPeriod", FiniteDateRange(decisionPeriod.alkamis_pvm, decisionPeriod.paattymis_pvm))
        .bind("effective", FeeDecisionStatus.effective)
        .bind("feeDecisionFiveYearOldTypes", arrayOf(PlacementType.DAYCARE_FIVE_YEAR_OLDS, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS))
        .bind("valueDecisionFiveYearOldTypes", arrayOf(PlacementType.DAYCARE_FIVE_YEAR_OLDS, PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS))
        .mapTo<VardaFeeDataBase>()
        .toList()
}

fun deleteVardaFeeData(tx: Database.Transaction, vardaId: Long) {
    tx.createUpdate("DELETE FROM varda_fee_data WHERE varda_id = :vardaId")
        .bind("vardaId", vardaId)
        .execute()
}

private fun baseToFeeData(
    feeDataBase: VardaFeeDataBase,
    guardians: List<PersonDTO>,
    childUrl: String,
    sourceSystem: String
): VardaFeeData {
    val vardaGuardians = guardians
        .map { guardian ->
            VardaGuardian(
                henkilotunnus = guardian.identity.toString(),
                etunimet = guardian.firstName!!,
                sukunimi = guardian.lastName!!
            )
        }

    return VardaFeeData(
        huoltajat = vardaGuardians,
        lapsi = childUrl,
        maksun_peruste_koodi =
        if (feeDataBase.isFiveYearOldDaycare)
            FeeBasisCode.FIVE_YEAR_OLDS_DAYCARE.code
        else
            FeeBasisCode.DAYCARE.code,
        asiakasmaksu = feeDataBase.fee,
        palveluseteli_arvo = feeDataBase.voucherValue,
        perheen_koko = feeDataBase.familySize,
        alkamis_pvm = feeDataBase.startDate,
        paattymis_pvm = feeDataBase.endDate,
        lahdejarjestelma = sourceSystem
    )
}

enum class FeeBasisCode(val code: String) {
    FIVE_YEAR_OLDS_DAYCARE("MP02"),
    DAYCARE("MP03")
}

data class VardaGuardian(
    val henkilotunnus: String,
    val etunimet: String,
    val sukunimi: String
)

data class VardaFeeDataBase(
    val feeDecisionId: UUID?,
    val voucherValueDecisionId: UUID?,
    val vardaChildId: Long,
    val vardaId: Long?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val fee: Double,
    val voucherValue: Double,
    val familySize: Int,
    val isFiveYearOldDaycare: Boolean
)

data class VardaFeeData(
    val huoltajat: List<VardaGuardian>,
    val lapsi: String,
    val maksun_peruste_koodi: String,
    val palveluseteli_arvo: Double,
    val asiakasmaksu: Double,
    val perheen_koko: Int,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?,
    val lahdejarjestelma: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaFeeDataResponse(
    val id: Long
)
