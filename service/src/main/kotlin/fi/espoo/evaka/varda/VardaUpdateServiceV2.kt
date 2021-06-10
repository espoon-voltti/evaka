// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun updateAllVardaData(
    db: Database.Connection,
    client: VardaClient,
    organizer: String
) {
    updateOrganizer(db, client, organizer)
    updateUnits(db, client, organizer)
    updateChildData(db, client, HelsinkiDateTime.now().minusHours(24))
}

/*
    0. If there are any existing failed service need updates, try to delete and readd the service need data to varda
    1. Find out all changed service needs.
        - For each deleted service need delete all related data from varda
        - For each new service need IF related fee dataexists, add all related data to varda
        - For each modified service need delete old related data from varda and add new
    2. Find out all changed evaka fee data affecting service needs not yet updated above, and for each service need
       update all service need related data to varda
 */
// TODO: handle failed uploads first: delete service need, upload all data
fun updateChildData(db: Database.Connection, client: VardaClient, startingFrom: HelsinkiDateTime) {
    val serviceNeedDiffsByChild = calculateEvakaVsVardaServiceNeedChangesByChild(db, startingFrom)
    val feeAndVoucherDiffsByServiceNeed = db.read { it.getServiceNeedFeeData(startingFrom, null) }.groupBy { it.serviceNeedId }
    val uploadedServiceNeedIds = mutableSetOf<UUID>()

    serviceNeedDiffsByChild.entries.forEach { serviceNeedDiffByChild ->
        val childId = serviceNeedDiffByChild.value.childId

        serviceNeedDiffByChild.value.deletes.forEach { deletedServiceNeedId ->
            if (handleServiceNeedDelete(db, client, deletedServiceNeedId))
                uploadedServiceNeedIds.add(deletedServiceNeedId)
        }

        serviceNeedDiffByChild.value.updates.forEach { updatedServiceNeedId ->
            if (handleServiceNeedUpdate(db, client, updatedServiceNeedId, childId))
                uploadedServiceNeedIds.add(updatedServiceNeedId)
        }

        serviceNeedDiffByChild.value.additions.forEach { addedServiceNeedId ->
            if (handleServiceNeedAddition(db, client, addedServiceNeedId, childId))
                uploadedServiceNeedIds.add(addedServiceNeedId)
        }
    }

    // Handle fee data only -changes (no changes in service need). If there was any service need change, all related
    // fee data has already been sent to / deleted from varda, so we filter those service needs out here
    feeAndVoucherDiffsByServiceNeed.filterNot { uploadedServiceNeedIds.contains(it.key) }.entries.forEach {
        val serviceNeedId = it.key
        val diff = it.value
        val vardaServiceNeed = db.read { it.getVardaServiceNeed(serviceNeedId) }
        if (vardaServiceNeed != null) {
            handleServiceNeedUpdate(db, client, serviceNeedId, diff.first().evakaChildId)
        } else {
            handleServiceNeedAddition(db, client, serviceNeedId, diff.first().evakaChildId)
        }
    }
}

// Delete varda decision, placement and related varda fee decisions
fun handleServiceNeedDelete(db: Database.Connection, vardaClient: VardaClient, serviceNeedId: UUID): Boolean {
    val errors = mutableListOf<String>()
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeed(serviceNeedId) }
        if (vardaServiceNeed == null) {
            logger.info("VardaUpdate: no need to delete service need $serviceNeedId as it is not in varda")
        } else {
            errors.addAll(deleteServiceNeedAndRelatedDataFromVarda(vardaClient, vardaServiceNeed))
            if (errors.isEmpty()) {
                db.transaction { it.deleteVardaFeeDecision(serviceNeedId) }
            }
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: error deleting $serviceNeedId: ${e.localizedMessage}")
    }

    if (errors.isNotEmpty()) {
        val error = "VardaUpdate: could not delete service need $serviceNeedId and related data, marking it as failed"
        logger.error(error)
        errors.add(error)
        db.transaction { it.markServiceNeedUpdateFailed(serviceNeedId, errors) }
        return false
    } else {
        db.transaction { it.deleteVardaServiceNeed(serviceNeedId) }
        return true
    }
}

// Delete varda decision, placement and related varda fee decisions and add new data
fun handleServiceNeedUpdate(db: Database.Connection, vardaClient: VardaClient, serviceNeedId: UUID, childId: UUID): Boolean {
    try {
        if (handleServiceNeedDelete(db, vardaClient, serviceNeedId))
            handleServiceNeedAddition(db, vardaClient, serviceNeedId, childId)
    } catch (e: Exception) {
        logger.error("VardaUpdate: could not update service need $serviceNeedId: ${e.localizedMessage}")
        return false
    }

    return true
}

// Add child if missing, service need, placement and mandatory fee decision(s)
fun handleServiceNeedAddition(db: Database.Connection, vardaClient: VardaClient, serviceNeedId: UUID, childId: UUID): Boolean {
    val errors = mutableListOf<String>()

    try {
        val serviceNeedFeeData = db.read { it.getServiceNeedFeeData(null, serviceNeedId) }.firstOrNull()

        // Todo: "nettopalvelu"-unit children do not have fee data
        if (serviceNeedFeeData != null && (serviceNeedFeeData.feeDecisionIds.isNotEmpty() || serviceNeedFeeData.voucherValueDecisionIds.isNotEmpty())) {
            val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(serviceNeedId) }
            val newVardaServiceNeed = evakaServiceNeedToVardaServiceNeed(childId, evakaServiceNeed)
            db.transaction { it.upsertVardaServiceNeed(newVardaServiceNeed) }

            if (evakaServiceNeed.ophOrganizerOid.isNullOrEmpty()) throw Exception("VardaUpdate: service need $serviceNeedId related oph_orginizer_id is null or empty")

            newVardaServiceNeed.vardaChildId = getOrCreateVardaChildByOrganizer(db, vardaClient, childId, evakaServiceNeed.ophOrganizerOid, vardaClient.sourceSystem)
            newVardaServiceNeed.vardaDecisionId = createDecisionToVarda(vardaClient, newVardaServiceNeed.vardaChildId, evakaServiceNeed)
            newVardaServiceNeed.vardaPlacementId = createPlacementToVarda(vardaClient, newVardaServiceNeed.vardaDecisionId!!, evakaServiceNeed)
            newVardaServiceNeed.vardaFeeDataIds = serviceNeedFeeData.feeDecisionIds.map { fdId ->
                val feeDecision = db.read { it.getFeeDecisionsByIds(listOf(fdId)) }.first()
                createFeeDataToVardaFromFeeDecision(db, vardaClient, newVardaServiceNeed.vardaChildId!!, evakaServiceNeed, feeDecision)
            }.plus(
                serviceNeedFeeData.voucherValueDecisionIds.map { vvdId ->
                    val voucherValueDecision = db.read { it.getVoucherValueDecision(vvdId) }
                        ?: throw Exception("VardaUpdate: cannot create voucher fee data: voucher $vvdId not found for $serviceNeedId")
                    createFeeDataToVardaFromVoucherValueDecision(db, vardaClient, newVardaServiceNeed.vardaChildId!!, evakaServiceNeed, voucherValueDecision)
                }
            )

            db.transaction { it.upsertVardaServiceNeed(newVardaServiceNeed) }
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: error creating a new varda decision for service need $serviceNeedId: ${e.localizedMessage}")
    }

    if (errors.isNotEmpty()) {
        logger.error(errors.joinToString(", "))
        db.transaction {
            it.markServiceNeedUpdateFailed(serviceNeedId, errors)
        }
        return false
    }

    return true
}

fun createDecisionToVarda(client: VardaClient, vardaChildId: Long?, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    if (vardaChildId == null) throw Exception("VardaUpdate: cannot create decision for ${evakaServiceNeedInfoForVarda.id}: child varda id missing")
    val res: VardaDecisionResponse?
    try {
        res = client.createDecision(evakaServiceNeedInfoForVarda.toVardaDecisionForChild(client.getChildUrl(vardaChildId), client.sourceSystem))
    } catch (e: Exception) {
        throw Exception("VardaUpdate: cannot create decision for ${evakaServiceNeedInfoForVarda.id}: varda client threw ${e.localizedMessage}")
    }

    if (res == null) throw Exception("VardaUpdate: cannot create decision for ${evakaServiceNeedInfoForVarda.id}: create varda decision response is null")
    return res.vardaDecisionId
}

fun createPlacementToVarda(client: VardaClient, vardaDecisionId: Long, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    val res: VardaPlacementResponse?
    try {
        res = client.createPlacement(evakaServiceNeedInfoForVarda.toVardaPlacement(client.getDecisionUrl(vardaDecisionId), client.sourceSystem))
    } catch (e: Exception) {
        throw Exception("VardaUpdate: cannot create placement for ${evakaServiceNeedInfoForVarda.id}: varda client threw ${e.localizedMessage}")
    }

    if (res == null) throw Exception("VardaUpdate: cannot create placement for ${evakaServiceNeedInfoForVarda.id}: create varda placement response is null")
    return res.vardaPlacementId
}

fun createFeeDataToVardaFromFeeDecision(
    db: Database.Connection,
    client: VardaClient,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    decision: FeeDecision
): Long {
    val guardians = db.read { listOfNotNull(it.getPersonById(decision.headOfFamily.id), if (decision.partner?.id != null) it.getPersonById(decision.partner.id) else null) }
    if (guardians.isEmpty()) throw Exception("VardaUpdate: could not create fee data for ${evakaServiceNeedInfoForVarda.id}: child has no guardians")
    val childPart = decision.children.find { part -> part.child.id == evakaServiceNeedInfoForVarda.childId }
        ?: throw Exception("VardaUpdate: could not create fee data for ${evakaServiceNeedInfoForVarda.id}: fee ${decision.id} has no part for child ${evakaServiceNeedInfoForVarda.childId}")

    val res: VardaFeeDataResponse?
    try {
        res = client.createFeeData(
            VardaFeeData(
                huoltajat = guardians.map { guardian -> VardaGuardian(guardian.identity.toString(), guardian.firstName ?: "", guardian.lastName ?: "") },
                lapsi = client.getChildUrl(vardaChildId),
                maksun_peruste_koodi = vardaFeeBasisByPlacementType(childPart.placement.type),
                asiakasmaksu = childPart.finalFee.div(100.0),
                palveluseteli_arvo = 0.0,
                perheen_koko = decision.familySize,
                alkamis_pvm = calculateVardaFeeDataStartDate(decision.validFrom, evakaServiceNeedInfoForVarda.asPeriod),
                paattymis_pvm = minEndDate(evakaServiceNeedInfoForVarda.endDate, decision.validTo),
                lahdejarjestelma = client.sourceSystem
            )
        )
    } catch (e: Exception) {
        throw Exception("VardaUpdate: cannot create fee data ${decision.id} for service need ${evakaServiceNeedInfoForVarda.id}: varda client threw ${e.localizedMessage}")
    }

    if (res == null) throw Exception("VardaUpdate: cannot create fee data ${decision.id} for service need ${evakaServiceNeedInfoForVarda.id}: create varda placement response is null")
    return res.id
}

fun createFeeDataToVardaFromVoucherValueDecision(
    db: Database.Connection,
    client: VardaClient,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    decision: VoucherValueDecisionDetailed
): Long {
    val guardians = db.read { listOfNotNull(it.getPersonById(decision.headOfFamily.id), if (decision.partner?.id != null) it.getPersonById(decision.partner.id) else null) }
    if (guardians.isEmpty()) throw Exception("VardaUpdate: could not create fee data for ${evakaServiceNeedInfoForVarda.id}: child has no guardians")

    val res: VardaFeeDataResponse?
    try {
        res = client.createFeeData(
            VardaFeeData(
                huoltajat = guardians.map { guardian -> VardaGuardian(guardian.identity.toString(), guardian.firstName ?: "", guardian.lastName ?: "") },
                lapsi = client.getChildUrl(vardaChildId),
                maksun_peruste_koodi = vardaFeeBasisByPlacementType(decision.placement.type),
                asiakasmaksu = decision.finalCoPayment.div(100.0),
                palveluseteli_arvo = decision.voucherValue.div(100.0),
                perheen_koko = decision.familySize,
                alkamis_pvm = calculateVardaFeeDataStartDate(decision.validFrom, evakaServiceNeedInfoForVarda.asPeriod),
                paattymis_pvm = minEndDate(evakaServiceNeedInfoForVarda.endDate, decision.validTo),
                lahdejarjestelma = client.sourceSystem
            )
        )
    } catch (e: Exception) {
        throw Exception("VardaUpdate: cannot create voucher fee data ${decision.id} for service need ${evakaServiceNeedInfoForVarda.id}: varda client threw ${e.localizedMessage}")
    }

    if (res == null) throw Exception("VardaUpdate: cannot create voucher fee data ${decision.id} for service need ${evakaServiceNeedInfoForVarda.id}: create varda placement response is null")
    return res.id
}

private fun vardaFeeBasisByPlacementType(type: PlacementType): String {
    return if (type == PlacementType.DAYCARE_FIVE_YEAR_OLDS || type == PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS)
        FeeBasisCode.FIVE_YEAR_OLDS_DAYCARE.code
    else
        FeeBasisCode.DAYCARE.code
}

private fun calculateVardaFeeDataStartDate(fdStartDate: LocalDate, serviceNeedDates: DateRange): LocalDate {
    return if (serviceNeedDates.includes(fdStartDate)) fdStartDate else serviceNeedDates.start
}

fun getOrCreateVardaChildForServiceNeedOrganization(db: Database.Connection, childId: UUID, ophOrganizerOid: String?): Long {
    if (!ophOrganizerOid.isNullOrEmpty()) {
        return db.read { it.getVardaChildIdByEvakaPersonIdAndOphUnitOid(childId, ophOrganizerOid) } ?: throw Exception("VardaUpdate: TODO: Creating a varda child not yet supported")
    } else {
        throw Exception("VardaUpdate: Cannot get or create varda child $childId: unit does not have oph_organizer_id")
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

private fun deleteServiceNeedAndRelatedDataFromVarda(vardaClient: VardaClient, vardaServiceNeed: VardaServiceNeed): List<String> {
    val errors = mutableListOf<String>()
    try {
        vardaServiceNeed.vardaFeeDataIds.forEach { feeDataId ->
            if (!vardaClient.deleteFeeData(feeDataId)) {
                errors.add("VardaUpdate: deleting fee data $feeDataId failed, continuing (maybe this is a retry?)")
            }
        }
        if (vardaServiceNeed.vardaPlacementId != null) {
            if (!vardaClient.deletePlacement(vardaServiceNeed.vardaPlacementId!!)) {
                errors.add("VardaUpdate: deleting placement ${vardaServiceNeed.vardaPlacementId} failed, continuing (maybe this is a retry?)")
            }
        }
        if (vardaServiceNeed.vardaDecisionId != null) {
            if (!vardaClient.deleteDecision(vardaServiceNeed.vardaDecisionId!!)) {
                errors.add("VardaUpdate: deleting decision ${vardaServiceNeed.vardaDecisionId} failed, continuing (maybe this is a retry?)")
            }
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: could not delete varda service need $vardaServiceNeed related data: ${e.localizedMessage}")
    }
    return errors
}

/*
 evaka_service_need_id             | uuid                     |           | not null |
 evaka_service_need_option_id      | uuid                     |           | not null |
 evaka_service_need_updated        | timestamp with time zone |           | not null |
 evaka_service_need_option_updated | timestamp with time zone |           | not null |
 evaka_child_id                    | uuid                     |           | not null |
 varda_decision_id                 | bigint                   |           |          |
 varda_placement_id                | bigint                   |           |          |
 varda_fee_data_ids                | bigint[]                 |           |          |
 update_failed                     | boolean                  |           |          |
 errors                            | text[]                   |           |          |
 */
fun Database.Transaction.upsertVardaServiceNeed(vardaServiceNeed: VardaServiceNeed) = createUpdate(
    """
INSERT INTO varda_service_need (evaka_service_need_id, evaka_service_need_option_id, evaka_service_need_updated, evaka_service_need_option_updated, evaka_child_id, varda_decision_id, varda_placement_id, varda_fee_data_ids) 
VALUES (:evakaServiceNeedId, :evakaServiceNeedOptionId, :evakaServiceNeedUpdated, :evakaServiceNeedOptionUpdated, :evakaChildId, :vardaDecisionId, :vardaPlacementId, :vardaFeeDataIds)
ON CONFLICT (evaka_service_need_id) DO UPDATE 
    SET evaka_service_need_option_id = :evakaServiceNeedOptionId, 
        evaka_service_need_updated = :evakaServiceNeedUpdated, 
        evaka_service_need_option_updated = :evakaServiceNeedOptionUpdated, 
        evaka_child_id = :evakaChildId, 
        varda_decision_id = :vardaDecisionId, 
        varda_placement_id = :vardaPlacementId, 
        varda_fee_data_ids = :vardaFeeDataIds
"""
).bindKotlin(vardaServiceNeed)
    .execute()

fun Database.Transaction.deleteVardaServiceNeed(serviceNeedId: UUID) = createUpdate(
    """
DELETE FROM varda_service_need
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .execute()

fun Database.Transaction.markServiceNeedUpdateFailed(serviceNeedId: UUID, errors: List<String>) = createUpdate(
    """
UPDATE varda_service_need
SET update_failed = true, errors = :errors
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .bind("errors", errors.toTypedArray())
    .execute()

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
    var vardaChildId: Long? = null,
    var vardaDecisionId: Long? = null,
    var vardaPlacementId: Long? = null,
    var vardaFeeDataIds: List<Long> = listOf(),
    var updateFailed: Boolean = false,
    val errors: MutableList<String> = mutableListOf()
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

fun Database.Read.getVardaServiceNeed(eVakaServiceNeedId: UUID): VardaServiceNeed? =
    createQuery(
        """
SELECT *
FROM varda_service_need
WHERE evaka_service_need_id = :eVakaServiceNeedId
"""
    )
        .bind("eVakaServiceNeedId", eVakaServiceNeedId)
        .mapTo<VardaServiceNeed>()
        .firstOrNull()

fun Database.Transaction.deleteVardaFeeDecision(eVakaServiceNeedId: UUID) =
    createUpdate(
        """
DELETE FROM varda_service_need WHERE evaka_service_need_id = :eVakaServiceNeedId
"""
    )
        .bind("eVakaServiceNeedId", eVakaServiceNeedId)
        .execute()

fun Database.Read.getServiceNeedFeeData(startingFrom: HelsinkiDateTime?, serviceNeedId: UUID?): List<FeeDataByServiceNeed> =
    createQuery(
        """
WITH child_fees AS (
  SELECT
    fd.id AS fee_decision_id,
    fdc.child_id,
    fd.valid_during
  FROM new_fee_decision fd LEFT JOIN new_fee_decision_child fdc ON fd.id = fdc.fee_decision_id 
  ${if (startingFrom != null) " WHERE fd.sent_at >= :startingFrom" else ""}  
), service_need_fees AS (
  SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(child_fees.fee_decision_id) AS fee_decision_ids
  FROM new_service_need sn JOIN placement p ON p.id = sn.placement_id
    JOIN child_fees ON p.child_id = child_fees.child_id 
      AND child_fees.valid_during && daterange(sn.start_date, sn.end_date, '[]')
  GROUP BY service_need_id, p.child_id
), service_need_vouchers AS (
SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(vvd.id) AS voucher_value_decision_ids
FROM new_service_need sn JOIN placement p ON p.id = sn.placement_id
  JOIN voucher_value_decision vvd ON p.child_id = vvd.child_id 
    AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange(sn.start_date, sn.end_date, '[]')
GROUP BY service_need_id, p.child_id
)
SELECT
    COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) AS service_need_id,
    COALESCE(service_need_fees.child_id, service_need_vouchers.child_id) AS evaka_child_id,
    service_need_fees.fee_decision_ids,
    service_need_vouchers.voucher_value_decision_ids
FROM service_need_fees 
    FULL OUTER JOIN service_need_vouchers ON 
        service_need_fees.service_need_id = service_need_vouchers.service_need_id
${ if (serviceNeedId != null) "WHERE COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) = :serviceNeedId" else ""}
        """
    )
        .bind("startingFrom", startingFrom)
        .bind("serviceNeedId", serviceNeedId)
        .mapTo<FeeDataByServiceNeed>()
        .list()

data class FeeDataByServiceNeed(
    val evakaChildId: UUID,
    val serviceNeedId: UUID,
    val feeDecisionIds: List<UUID> = emptyList(),
    val voucherValueDecisionIds: List<UUID> = emptyList()
)

fun Database.Read.getVardaChildIdByEvakaPersonIdAndOphUnitOid(evakaChildId: UUID, organizerOid: String): Long? = createQuery(
    """
SELECT varda_child_id
FROM varda_child
WHERE person_id = :evakaChildId AND oph_organizer_oid = :organizerOid
"""
).bind("evakaChildId", evakaChildId)
    .bind("organizerOid", organizerOid)
    .mapTo<Long>()
    .first()

fun Database.Read.getVardaChildByEvakaPersonIdAndOphUnitOid(evakaChildId: UUID, organizerOid: String): VardaChild? = createQuery(
    """
SELECT *
FROM varda_child
WHERE person_id = :evakaChildId AND oph_organizer_oid = :organizerOid
"""
).bind("evakaChildId", evakaChildId)
    .bind("organizerOid", organizerOid)
    .mapTo<VardaChild>()
    .first()

data class VardaChild(
    val id: UUID,
    val personId: UUID,
    val vardaPersonId: Long,
    val vardaPersonOid: String,
    val vardaChildId: Long?,
    val ophOrganizerOid: String,
    val createdAt: Instant?,
    val modifiedAt: Instant?,
    val uploadedAt: Instant?
)

private fun evakaServiceNeedToVardaServiceNeed(childId: UUID, evakaServiceNeed: EvakaServiceNeedInfoForVarda): VardaServiceNeed =
    VardaServiceNeed(
        evakaChildId = childId,
        evakaServiceNeedId = evakaServiceNeed.id,
        evakaServiceNeedOptionId = evakaServiceNeed.optionId,
        evakaServiceNeedUpdated = HelsinkiDateTime.from(evakaServiceNeed.serviceNeedUpdated),
        evakaServiceNeedOptionUpdated = HelsinkiDateTime.from(evakaServiceNeed.serviceNeedOptionUpdated)
    )

data class EvakaServiceNeedInfoForVarda(
    val id: UUID,
    val optionId: UUID,
    val serviceNeedUpdated: Instant,
    val serviceNeedOptionUpdated: Instant,
    val childId: UUID,
    val applicationDate: LocalDate,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val urgent: Boolean,
    val hoursPerWeek: Double,
    val temporary: Boolean,
    val daily: Boolean,
    val shiftCare: Boolean,
    val providerType: ProviderType,
    val ophOrganizerOid: String?,
    val ophUnitOid: String?
) {
    val providerTypeCode = VardaUnitProviderType.valueOf(providerType.toString()).vardaCode

    val asPeriod = DateRange(startDate, endDate)

    fun toVardaDecisionForChild(vardaChildUrl: String, sourceSystem: String): VardaDecision = VardaDecision(
        childUrl = vardaChildUrl,
        applicationDate = this.applicationDate,
        startDate = this.startDate,
        endDate = this.endDate,
        urgent = this.urgent,
        hoursPerWeek = this.hoursPerWeek,
        temporary = this.temporary,
        daily = this.daily,
        shiftCare = this.shiftCare,
        providerTypeCode = this.providerTypeCode,
        sourceSystem = sourceSystem
    )

    fun toVardaPlacement(vardaDecisionUrl: String, sourceSystem: String): VardaPlacement = VardaPlacement(
        decisionUrl = vardaDecisionUrl,
        unitOid = this.ophUnitOid ?: throw Exception("VardaUpdate: varda placement cannot be created for service need ${this.id}: unitOid cannot be null"),
        startDate = this.startDate,
        endDate = this.endDate,
        sourceSystem = sourceSystem
    )
}

fun Database.Read.getEvakaServiceNeedInfoForVarda(id: UUID): EvakaServiceNeedInfoForVarda {
    // language=sql
    val sql = """
        SELECT
            sn.id, p.child_id AS child_id, COALESCE(a.preferredstartdate, sn.start_date) AS application_date, sn.start_date, sn.end_date,
            COALESCE(a.urgent, false) AS urgent, sno.daycare_hours_per_week AS hours_per_week,
            CASE 
                WHEN sno.valid_placement_type = 'TEMPORARY_DAYCARE' OR sno.valid_placement_type = 'TEMPORARY_DAYCARE_PART_DAY' THEN true
                ELSE false
            END AS temporary,
            NOT(sno.part_week) AS daily,
            sn.shift_care,
            d.provider_type,
            d.oph_organizer_oid,
            d.oph_unit_oid,
            sn.updated AS service_need_updated,
            sno.updated AS service_need_option_updated,
            sno.id AS optionId
        FROM new_service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN employee e on e.id = sn.confirmed_by
        JOIN placement p ON p.id = sn.placement_id
        JOIN daycare d ON p.unit_id = d.id
        LEFT JOIN application_view a ON daterange(sn.start_date, sn.end_date, '[]') @> a.preferredstartdate AND a.preferredstartdate=(select max(preferredstartdate) from application_view a where daterange(sn.start_date, sn.end_date, '[]') @> a.preferredstartdate)
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .mapTo<EvakaServiceNeedInfoForVarda>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}
