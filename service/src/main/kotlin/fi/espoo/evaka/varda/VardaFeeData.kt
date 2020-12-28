// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.PlacementType
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.varda.integration.VardaClient
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun updateFeeData(
    db: Database.Connection,
    client: VardaClient,
    personService: PersonService
) {
    deleteAnnulledFeeData(db, client)
    createAndUpdateFeeData(db, client, personService)
}

fun removeMarkedFeeDataFromVarda(db: Database.Connection, client: VardaClient) {
    db.read { getMarkedFeeData(it) }.forEach { vardaId ->
        if (client.deleteFeeData(vardaId)) {
            db.transaction { deleteVardaFeeData(it, vardaId) }
        }
    }
}

fun deleteAnnulledFeeData(db: Database.Connection, client: VardaClient) {
    val vardaIds = db.read { getAnnulledFeeDecisions(it) }
    deleteFeeData(db, client, vardaIds)
}

fun deleteFeeData(db: Database.Connection, client: VardaClient, vardaIds: List<Long>) {
    vardaIds.forEach { vardaId ->
        if (client.deleteFeeData(vardaId)) {
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
        deleteFeeData(db, client, outdatedFeeData)

        val guardians = db.transaction { personService.getEvakaOrVtjGuardians(it, AuthenticatedUser.machineUser, childId) }
            .filter { (it.firstName + it.lastName).isNotBlank() }
        if (guardians.isEmpty()) return@forEach

        val feeData = vardaDecisionPeriods.flatMap { decisionPeriod ->
            db.read {
                getDecisionFeeData(it, childVardaId, decisionPeriod)
                    .map { data -> decisionPeriod.id to data }
            }
        }

        feeData.forEach { (vardaDecisionId, data) ->
            val richData = baseToFeeData(data, guardians, client.getChildUrl(data.vardaChildId))
            when (data.vardaId) {
                is Long -> {
                    client.updateFeeData(data.vardaId, richData).let { success ->
                        if (success) db.transaction { updateFeeDataUploadedAt(it, data.vardaId, Instant.now()) }
                    }
                }
                else -> {
                    client.createFeeData(richData)?.let { (vardaId) ->
                        db.transaction {
                            insertFeeDataUpload(it, vardaId, data.feeDecisionId, vardaDecisionId, childVardaId)
                        }
                    }
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
    feeDecisionId: UUID,
    vardaDecisionId: Long,
    vardaChildId: Long
) {
    tx.createUpdate(
        """
INSERT INTO varda_fee_data (varda_id, evaka_fee_decision_id, varda_decision_id, varda_child_id, uploaded_at)
VALUES (
    :vardaId,
    :feeDecisionId,
    (SELECT id FROM varda_decision WHERE varda_decision_id = :vardaDecisionId),
    (SELECT id FROM varda_child WHERE varda_child_id = :vardaChildId),
    :uploadedAt
)
"""
    )
        .bind("vardaId", vardaId)
        .bind("feeDecisionId", feeDecisionId)
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

private fun getAnnulledFeeDecisions(tx: Database.Read): List<Long> {
    return tx
        .createQuery(
            """
SELECT varda_id
FROM varda_fee_data
JOIN fee_decision ON fee_decision.id = varda_fee_data.evaka_fee_decision_id AND fee_decision.status = :annulled
"""
        )
        .bind("annulled", FeeDecisionStatus.ANNULLED)
        .mapTo<Long>()
        .toList()
}

private fun getChildrenWithOutdatedFeeData(tx: Database.Read): List<Pair<UUID, Long>> {
    return tx.createQuery(
        """
SELECT varda_child.person_id, varda_child.varda_child_id
FROM varda_child
JOIN LATERAL (
    SELECT MAX(fd.sent_at) sent_at
    FROM fee_decision fd
    JOIN fee_decision_part p ON fd.id = p.fee_decision_id AND p.child = varda_child.person_id
    WHERE fd.status = :sent
) latest_fee_decision ON true
JOIN LATERAL (
    SELECT MAX(uploaded_at) uploaded_at
    FROM varda_fee_data
    WHERE varda_child_id = varda_child.id
) latest_fee_data ON true
WHERE varda_child.varda_child_id IS NOT NULL
AND COALESCE(latest_fee_data.uploaded_at <= COALESCE(latest_fee_decision.sent_at, '-infinity'), true)
"""
    )
        .bind("sent", FeeDecisionStatus.SENT)
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
JOIN fee_decision d ON vfd.evaka_fee_decision_id = d.id AND NOT daterange(d.valid_from, d.valid_to, '[]') && :decisionPeriod
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
    vc.varda_child_id AS varda_child_id,
    vfd.varda_id AS varda_id,
    GREATEST(d.valid_from, :decisionStartDate) AS start_date,
    LEAST(d.valid_to, :decisionEndDate) AS end_date,
    (p.fee + COALESCE(alterations.sum, 0)) / 100.0 AS fee,
    d.family_size AS family_size,
    p.placement_type AS placement_type
FROM varda_child vc
JOIN fee_decision_part p ON vc.person_id = p.child
JOIN fee_decision d ON p.fee_decision_id = d.id AND daterange(d.valid_from, d.valid_to, '[]') && :decisionPeriod AND d.status = :sent
LEFT JOIN (
    SELECT fee_decision_part.id, SUM(effects.effect) sum
    FROM fee_decision_part
    JOIN (
        SELECT id, (jsonb_array_elements(fee_alterations)->>'effect')::integer effect
        FROM fee_decision_part
    ) effects ON fee_decision_part.id = effects.id
    GROUP BY fee_decision_part.id
) alterations ON p.id = alterations.id
JOIN varda_decision vd ON vd.varda_decision_id = :decisionId
LEFT JOIN varda_fee_data vfd ON d.id = vfd.evaka_fee_decision_id AND vd.id = vfd.varda_decision_id AND vc.id = vfd.varda_child_id
WHERE vc.varda_child_id = :childVardaId
"""
        )
        .bind("childVardaId", childVardaId)
        .bind("decisionId", decisionPeriod.id)
        .bind("decisionStartDate", decisionPeriod.alkamis_pvm)
        .bind("decisionEndDate", decisionPeriod.paattymis_pvm)
        .bind("decisionPeriod", FiniteDateRange(decisionPeriod.alkamis_pvm, decisionPeriod.paattymis_pvm))
        .bind("sent", FeeDecisionStatus.SENT)
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
    childUrl: String
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
        palveluseteli_arvo = 0.0,
        maksun_peruste_koodi =
        if (feeDataBase.placementType == PlacementType.FIVE_YEARS_OLD_DAYCARE)
            FeeBasisCode.FIVE_YEAR_OLDS_DAYCARE.code
        else
            FeeBasisCode.DAYCARE.code,
        asiakasmaksu = feeDataBase.fee,
        perheen_koko = feeDataBase.familySize,
        alkamis_pvm = feeDataBase.startDate,
        paattymis_pvm = feeDataBase.endDate
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
    val feeDecisionId: UUID,
    val vardaChildId: Long,
    val vardaId: Long?,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val fee: Double,
    val familySize: Int,
    val placementType: PlacementType
)

data class VardaFeeData(
    val huoltajat: List<VardaGuardian>,
    val lapsi: String,
    val maksun_peruste_koodi: String,
    val palveluseteli_arvo: Double,
    val asiakasmaksu: Double,
    val perheen_koko: Int,
    val alkamis_pvm: LocalDate,
    val paattymis_pvm: LocalDate?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaFeeDataResponse(
    val id: Long
)
