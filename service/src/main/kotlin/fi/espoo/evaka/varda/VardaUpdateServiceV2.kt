package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.varda.integration.VardaClient
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

class VardaUpdateServiceV2 {
    fun updateAll(
        db: Database.Connection,
        client: VardaClient,
        organizer: String,
//            startingFrom: HelsinkiDateTime
    ) {
        updateOrganizer(db, client, organizer)
        updateUnits(db, client, organizer)

        // updateChildData(db, client, organizer)
    }
}

fun calculateEvakaVsVardaServiceNeedChangesByChild(db: Database.Connection, startingFrom: HelsinkiDateTime): Map<UUID, VardaChildCalculatedServiceNeedChanges> {
    val evakaServiceNeedDeletionsByChild = calculateDeletedChildServiceNeeds(db)
    val evakaServiceNeedChangesByChild = db.read { it.getEvakaServiceNeedChanges(startingFrom) }
        .groupBy { it.evakaChildId }

    val additionsAndChangesToVardaByChild = evakaServiceNeedChangesByChild.entries.associate { evakaServiceNeedChangesForChild ->
        val vardaServiceNeedsForChild = db.read { it.getChildVardaServiceNeeds(evakaServiceNeedChangesForChild.key) }

        evakaServiceNeedChangesForChild.key to VardaChildCalculatedServiceNeedChanges(
            childId = evakaServiceNeedChangesForChild.key,
            additions = calculateNewChildServiceNeeds(evakaServiceNeedChangesForChild.value, vardaServiceNeedsForChild),
            updates = calculateUpdatedChildServiceNeeds(evakaServiceNeedChangesForChild.value, vardaServiceNeedsForChild),
            deletes = evakaServiceNeedDeletionsByChild.getOrDefault(evakaServiceNeedChangesForChild.key, emptyList())
        )
    }

    return additionsAndChangesToVardaByChild.plus(
        evakaServiceNeedDeletionsByChild.filterNot {
            additionsAndChangesToVardaByChild.containsKey(it.key)
        }.entries.associate { childServiceNeedDeletions ->
            childServiceNeedDeletions.key to
                VardaChildCalculatedServiceNeedChanges(
                    childId = childServiceNeedDeletions.key,
                    additions = emptyList(),
                    updates = emptyList(),
                    deletes = childServiceNeedDeletions.value
                )
        }
    )
}

data class VardaChildCalculatedServiceNeedChanges(
    val childId: UUID,
    val additions: List<UUID>,
    val updates: List<UUID>,
    val deletes: List<UUID>
)

// Find out new varhaiskasvatuspaatos to be added for a child: any new service needs not found from child's full varda history
private fun calculateNewChildServiceNeeds(evakaServiceNeedChangesForChild: List<VardaServiceNeed>, vardaChildServiceNeeds: List<VardaServiceNeed>): List<UUID> {
    return evakaServiceNeedChangesForChild.filter { newServiceNeedChange ->
        vardaChildServiceNeeds.none { vardaChildServiceNeedChange ->
            vardaChildServiceNeedChange.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId
        }
    }.map {
        it.evakaServiceNeedId
    }
}

// Find out changed varhaiskasvatuspaatos for a child: any new service need with a different update timestamp in history
private fun calculateUpdatedChildServiceNeeds(evakaServiceNeedChangesForChild: List<VardaServiceNeed>, vardaServiceNeedsForChild: List<VardaServiceNeed>): List<UUID> {
    return evakaServiceNeedChangesForChild.filter { newServiceNeedChange ->
        val match = vardaServiceNeedsForChild.find { it.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId }
        match != null && (
            (
                newServiceNeedChange.evakaServiceNeedUpdated != match.evakaServiceNeedUpdated ||
                    newServiceNeedChange.evakaServiceNeedOptionUpdated != match.evakaServiceNeedOptionUpdated
                )
            )
    }.map {
        it.evakaServiceNeedId
    }
}

private fun calculateDeletedChildServiceNeeds(db: Database.Connection): Map<UUID, List<UUID>> {
    return db.read {
        it.createQuery(
            """
SELECT evaka_child_id AS child_id, array_agg(evaka_service_need_id::uuid) AS service_need_ids
FROM varda_service_need
WHERE evaka_service_need_id NOT IN (
    SELECT id FROM new_service_need
)
GROUP BY evaka_child_id"""
        )
            .map { rs, _ -> rs.getObject("child_id", UUID::class.java) to (rs.getArray("service_need_ids").array as Array<*>).map { UUID.fromString(it.toString()) } }
            .toMap()
    }
}

data class VardaServiceNeed(
    val evakaChildId: UUID,
    val evakaServiceNeedId: UUID,
    val evakaServiceNeedOptionId: UUID,
    val evakaServiceNeedUpdated: HelsinkiDateTime,
    val evakaServiceNeedOptionUpdated: HelsinkiDateTime,
    val vardaDecisionId: String? = null,
    val vardaRelationId: String? = null
) {
    val evakaLastUpdated = if (evakaServiceNeedUpdated.isAfter(evakaServiceNeedOptionUpdated)) evakaServiceNeedUpdated else evakaServiceNeedOptionUpdated
}

fun Database.Read.getEvakaServiceNeedChanges(startingFrom: HelsinkiDateTime): List<VardaServiceNeed> =
    createQuery(
        """
SELECT 
    placement.child_id AS evakaChildId,
    sn.id AS evakaServiceNeedId,
    option.id AS evakaServiceNeedOptionId,
    sn.updated AS evakaServiceNeedUpdated,
    option.updated AS evakaServiceNeedOptionUpdated
FROM new_service_need sn
LEFT JOIN service_need_option option ON sn.option_id = option.id
LEFT JOIN placement ON sn.placement_id = placement.id
WHERE (sn.updated >= :startingFrom OR option.updated >= :startingFrom)
"""
    )
        .bind("startingFrom", startingFrom)
        .mapTo<VardaServiceNeed>()
        .list()

fun Database.Read.getChildVardaServiceNeeds(evakaChildId: UUID): List<VardaServiceNeed> =
    createQuery(
        """
SELECT *
FROM varda_service_need
WHERE evaka_child_id = :evakaChildId
"""
    )
        .bind("evakaChildId", evakaChildId)
        .mapTo<VardaServiceNeed>()
        .list()

fun Database.Read.getEvakaFeeDataChangesByChild(startingFrom: HelsinkiDateTime): List<EvakaFeeDataChangesByChild> =
    createQuery(
        """
WITH child_new_fee_decisions AS (
SELECT
    fdc.child_id AS child_id,
    array_agg(fdc.id::uuid) AS fee_decision_child_ids,
    true AS is_fee_decision
FROM new_fee_decision fd
    LEFT JOIN new_fee_decision_child fdc ON fd.id = fdc.fee_decision_id
    WHERE fd.sent_at >= :startingFrom
GROUP BY fdc.child_id),
child_new_voucher_value_decisions AS (
SELECT
    vvd.child_id AS child_id,
    array_agg(vvd.id) AS voucher_value_decision_ids,
    false AS is_fee_decision
FROM voucher_value_decision vvd
WHERE vvd.sent_at >= :startingFrom
GROUP BY vvd.child_id)
SELECT
    COALESCE(fd.child_id, vvd.child_id) AS child_id,
    fee_decision_child_ids,
    voucher_value_decision_ids
FROM child_new_fee_decisions fd FULL OUTER JOIN child_new_voucher_value_decisions vvd
    ON fd.child_id = vvd.child_id;            
        """
    )
        .bind("startingFrom", startingFrom)
        .mapTo<EvakaFeeDataChangesByChild>()
        .list()

data class EvakaFeeDataChangesByChild(
    val childId: UUID,
    val feeDecisionChildIds: List<UUID> = emptyList(),
    val voucherValueDecisionIds: List<UUID> = emptyList()
)

fun Database.Read.calculateEvakaFeeDataChangesByServiceNeed(startingFrom: HelsinkiDateTime): List<EvakaFeeDataChangesByServiceNeed> =
    createQuery(
        """
WITH changed_fees AS (
  SELECT
    fdc.id AS fee_decision_child_id,
    fdc.child_id,
    fd.valid_during
  FROM new_fee_decision fd LEFT JOIN new_fee_decision_child fdc ON fd.id = fdc.fee_decision_id 
  WHERE fd.sent_at >= :startingFrom
), service_need_fees AS (
  SELECT
    sn.id AS service_need_id,
    array_agg(changed_fees.fee_decision_child_id) AS fee_decision_ids
  FROM new_service_need sn JOIN placement p ON p.id = sn.placement_id
    JOIN changed_fees ON p.child_id = changed_fees.child_id 
      AND changed_fees.valid_during && daterange(sn.start_date, sn.end_date, '[]')
  GROUP BY service_need_id
), service_need_vouchers AS (
SELECT
    sn.id AS service_need_id,
    array_agg(vvd.id) AS voucher_value_decision_ids
FROM new_service_need sn JOIN placement p ON p.id = sn.placement_id
  JOIN voucher_value_decision vvd ON p.child_id = vvd.child_id 
    AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange(sn.start_date, sn.end_date, '[]')
  WHERE vvd.sent_at >= :startingFrom  
GROUP BY service_need_id
)
SELECT
    COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) AS service_need_id,
    service_need_fees.fee_decision_ids,
    service_need_vouchers.voucher_value_decision_ids
FROM service_need_fees 
    FULL OUTER JOIN service_need_vouchers ON 
        service_need_fees.service_need_id = service_need_vouchers.service_need_id;        
        """
    )
        .bind("startingFrom", startingFrom)
        .mapTo<EvakaFeeDataChangesByServiceNeed>()
        .list()

data class EvakaFeeDataChangesByServiceNeed(
    val serviceNeedId: UUID,
    val feeDecisionIds: List<UUID> = emptyList(),
    val voucherValueDecisionIds: List<UUID> = emptyList()
)
