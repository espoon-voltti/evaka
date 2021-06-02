package fi.espoo.evaka.varda

import fi.espoo.evaka.serviceneednew.NewServiceNeed
import fi.espoo.evaka.serviceneednew.getNewServiceNeed
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.util.UUID

private val logger = KotlinLogging.logger {}

class VardaUpdateServiceV2 {
    fun updateAll(
        db: Database.Connection,
        client: VardaClient,
        organizer: String
    ) {
        updateOrganizer(db, client, organizer)
        updateUnits(db, client, organizer)
        updateChildData(db, client, HelsinkiDateTime.now().minusHours(24))
    }
}

fun updateChildData(db: Database.Connection, client: VardaClient, startingFrom: HelsinkiDateTime) {
    val serviceNeedDiffsByChild = calculateEvakaVsVardaServiceNeedChangesByChild(db, startingFrom)
    val feeAndVoucherDiffByChild = db.read { it.calculateEvakaFeeDataChangesByServiceNeed(startingFrom) }.groupBy { it.evakaChildId }
    // val feeAndVoucherDiffByServiceNeed = db.read { it.calculateEvakaFeeDataChangesByServiceNeed(startingFrom) }.groupBy { it.serviceNeedId }

    serviceNeedDiffsByChild.entries.forEach { serviceNeedDiffByChild ->
        val childId = serviceNeedDiffByChild.value.childId
        serviceNeedDiffByChild.value.deletes.forEach { deletedServiceNeedId ->
            handleServiceNeedDelete(db, client, deletedServiceNeedId)
        }

        serviceNeedDiffByChild.value.updates.forEach { updatedServiceNeedId ->
            handleServiceNeedUpdate(db, updatedServiceNeedId, childId)
        }

        serviceNeedDiffByChild.value.additions.forEach { addedServiceNeedId ->
            handleServiceNeedAddition(db, client, addedServiceNeedId, childId)
        }

        // Todo: ne service needit joita EI päivitetty yllä ^ pitää vielä käsitellä
        // feeAndVoucherDiffByChild.get(childId).forEach { feeDataChange -> feeDataChange. }
    }

    logger.debug("Got $serviceNeedDiffsByChild AND $feeAndVoucherDiffByChild AND $client")
}

// Delete varda decision, placement and related varda fee decisions
fun handleServiceNeedDelete(db: Database.Connection, vardaClient: VardaClient, serviceNeedId: UUID) {
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
    }
}

// Delete varda decision, placement and related varda fee decisions and add new (use
// the function below!)
fun handleServiceNeedUpdate(db: Database.Connection, serviceNeedId: UUID, childId: UUID) {
    if (!db.isConnected()) logger.info("silence lint")

    logger.info("TODO: handle $serviceNeedId update for $childId")
}

// Add child if missing, service need, placement and mandatory fee decision(s)
fun handleServiceNeedAddition(db: Database.Connection, vardaClient: VardaClient, serviceNeedId: UUID, childId: UUID) {
    val errors = mutableListOf<String>()
    try {
        val serviceNeedFeeData = db.read { it.getServiceNeedFeeData(serviceNeedId) }
        if (serviceNeedFeeData.feeDecisionIds.isNotEmpty() || serviceNeedFeeData.voucherValueDecisionIds.isNotEmpty()) {
            val evakaServiceNeed = db.read { it.getNewServiceNeed(serviceNeedId) }
            val vardaChild = getOrCreateVardaChildForServiceNeedOrganization(db, childId, evakaServiceNeed)
            val vardaDecision = createDecisionToVarda()
            val vardaPlacement = createPlacementToVarda()
            val vardaFeeDecisions = createFeeDataToVarda()
            logger.info("$vardaChild $vardaDecision $vardaPlacement $vardaFeeDecisions $vardaClient")
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: error deleting $serviceNeedId: ${e.localizedMessage}")
    }

    if (errors.isNotEmpty()) {
        val error = "VardaUpdate: could not delete service need $serviceNeedId and related data, marking it as failed"
        logger.error(error)
        errors.add(error)
        db.transaction { it.markServiceNeedUpdateFailed(serviceNeedId, errors) }
    }
}

/*
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
    val providerTypeCode: String,
    @JsonProperty("lahdejarjestelma")
    val sourceSystem: String
 */
fun createDecisionToVarda(): VardaDecision? {
    return null
}

/*
data class VardaPlacement(
    @JsonProperty("varhaiskasvatuspaatos")
    val decisionUrl: String,
    @JsonProperty("toimipaikka_oid")
    val unitOid: String,
    @JsonProperty("alkamis_pvm")
    val startDate: LocalDate,
    @JsonProperty("paattymis_pvm")
    val endDate: LocalDate?,
    @JsonProperty("lahdejarjestelma")
    val sourceSystem: String
)
 */
fun createPlacementToVarda(): VardaPlacement? {
    return null
}

/*
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
 */
fun createFeeDataToVarda(): List<VardaFeeData> {
    return emptyList()
}

// Add service need, placement and mandatory fee decision(s)
fun handleServiceNeedFeeDataUpdate(db: Database.Connection, serviceNeedId: UUID, childId: UUID) {
    if (!db.isConnected()) logger.info("silence lint")

    logger.info("TODO: handle $serviceNeedId addition for $childId")
}

fun getOrCreateVardaChildForServiceNeedOrganization(db: Database.Connection, childId: UUID, serviceNeed: NewServiceNeed): VardaChild {
    val serviceNeedOrganizer = db.read { it.getServiceNeedOrganizer(serviceNeed.id) }
    if (serviceNeedOrganizer.ophUnitOid != null) {
        // TODO: if child for the service need unit organizer is not found, create it & person if not exists yet
        return db.read { it.getVardaChildByEvakaPersonIdAndOphUnitOid(childId, serviceNeedOrganizer.ophUnitOid) }
            ?: throw Exception("VardaUpdate: TODO: Creating a varda child not yet supported")
    } else {
        throw Exception("VardaUpdate: Cannot get or create varda child: unit ${serviceNeedOrganizer.id} does not have oph_organizer_id")
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
            if (!vardaClient.deletePlacement(vardaServiceNeed.vardaPlacementId)) {
                errors.add("VardaUpdate: deleting placement ${vardaServiceNeed.vardaPlacementId} failed, continuing (maybe this is a retry?)")
            }
        }
        if (vardaServiceNeed.vardaDecisionId != null) {
            if (!vardaClient.deleteDecision(vardaServiceNeed.vardaDecisionId)) {
                errors.add("VardaUpdate: deleting decision ${vardaServiceNeed.vardaDecisionId} failed, continuing (maybe this is a retry?)")
            }
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: could not delete varda service need related data: ${e.localizedMessage}")
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

fun Database.Transaction.markServiceNeedUpdateFailed(serviceNeedId: UUID, errors: List<String>) = createUpdate(
    """
UPDATE varda_service_need
SET update_failed = true, errors = :errors
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .bind("errors", errors)
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
    val vardaDecisionId: Long? = null,
    val vardaPlacementId: Long? = null,
    val vardaFeeDataIds: List<Long> = emptyList(),
    val updateFailed: Boolean = false,
    val errors: List<String> = emptyList()
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

fun Database.Read.calculateEvakaFeeDataChangesByServiceNeed(startingFrom: HelsinkiDateTime): List<FeeDataByServiceNeed> =
    createQuery(
        """
WITH changed_fees AS (
  SELECT
    fdc.id AS fee_decision_id,
    fdc.child_id,
    fd.valid_during
  FROM new_fee_decision fd LEFT JOIN new_fee_decision_child fdc ON fd.id = fdc.fee_decision_id 
  WHERE fd.sent_at >= :startingFrom
), service_need_fees AS (
  SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(changed_fees.fee_decision_id) AS fee_decision_ids
  FROM new_service_need sn JOIN placement p ON p.id = sn.placement_id
    JOIN changed_fees ON p.child_id = changed_fees.child_id 
      AND changed_fees.valid_during && daterange(sn.start_date, sn.end_date, '[]')
  GROUP BY service_need_id, p.child_id
), service_need_vouchers AS (
SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(vvd.id) AS voucher_value_decision_ids
FROM new_service_need sn JOIN placement p ON p.id = sn.placement_id
  JOIN voucher_value_decision vvd ON p.child_id = vvd.child_id 
    AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange(sn.start_date, sn.end_date, '[]')
  WHERE vvd.sent_at >= :startingFrom  
GROUP BY service_need_id, p.child_id
)
SELECT
    COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) AS service_need_id,
    COALESCE(service_need_fees.child_id, service_need_vouchers.child_id) AS evaka_child_id,
    service_need_fees.fee_decision_ids,
    service_need_vouchers.voucher_value_decision_ids
FROM service_need_fees 
    FULL OUTER JOIN service_need_vouchers ON 
        service_need_fees.service_need_id = service_need_vouchers.service_need_id;        
        """
    )
        .bind("startingFrom", startingFrom)
        .mapTo<FeeDataByServiceNeed>()
        .list()

fun Database.Read.getServiceNeedFeeData(serviceNeedId: UUID): FeeDataByServiceNeed =
    createQuery(
        """
WITH child_fees AS (
  SELECT
    fdc.id AS fee_decision_id,
    fdc.child_id,
    fd.valid_during
  FROM new_fee_decision fd LEFT JOIN new_fee_decision_child fdc ON fd.id = fdc.fee_decision_id 
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
WHERE service_need_id = :serviceNeedId
        """
    )
        .bind("serviceNeedId", serviceNeedId)
        .mapTo<FeeDataByServiceNeed>()
        .first()

data class FeeDataByServiceNeed(
    val evakaChildId: UUID,
    val serviceNeedId: UUID,
    val feeDecisionIds: List<UUID> = emptyList(),
    val voucherValueDecisionIds: List<UUID> = emptyList()
)

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

fun Database.Read.getServiceNeedOrganizer(serviceNeedId: UUID): ServiceNeedOrganizer = createQuery(
    """
SELECT d.id, d.oph_organization_oid, oph_organizer_oid, oph_unit_oid
FROM new_service_need sn 
    JOIN placement p ON p.id = sn.placement_id
    JOIN daycare d ON d.id = p.unit_id
WHERE sn.id = :serviceNeedId
        """
).bind("serviceNeedId", serviceNeedId)
    .mapTo<ServiceNeedOrganizer>()
    .first()

data class ServiceNeedOrganizer(
    val id: UUID,
    val ophOrganizerOid: String?,
    val ophOrganizationOid: String?,
    val ophUnitOid: String?
)

private fun evakaServiceNeedToVardaServiceNeed(childId: UUID, evakaServiceNeed: NewServiceNeed): VardaServiceNeed =
    VardaServiceNeed(
        evakaChildId = childId,
        evakaServiceNeedId = evakaServiceNeed.id,
        evakaServiceNeedOptionId = evakaServiceNeed.option.id,
        evakaServiceNeedUpdated = HelsinkiDateTime.from(evakaServiceNeed.updated),
        evakaServiceNeedOptionUpdated = HelsinkiDateTime.from(evakaServiceNeed.option.updated)
    )
