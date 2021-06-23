// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VardaUpdateV2
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class VardaUpdateServiceV2(
    private val asyncJobRunner: AsyncJobRunner,
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val env: Environment,
    private val mapper: ObjectMapper,
) {
    private val organizer = env.getProperty("fi.espoo.varda.organizer", String::class.java, "Espoo")

    init {
        asyncJobRunner.vardaUpdateV2 = ::updateAll
    }

    fun scheduleVardaUpdate(db: Database.Connection, runNow: Boolean = false) {
        if (runNow) {
            logger.info("VardaUpdate: running varda update immediately")
            val client = VardaClient(tokenProvider, fuel, env, mapper)
            updateAllVardaData(db, client, organizer)
        } else {
            logger.info("VardaUpdate: scheduling varda update")
            db.transaction { asyncJobRunner.plan(it, listOf(VardaUpdateV2()), retryCount = 1) }
        }
    }

    fun updateAll(db: Database, msg: VardaUpdateV2) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        db.connect { updateAllVardaData(it, client, organizer) }
    }

    fun clearAllExistingVardaChildDataFromVarda(db: Database.Connection, vardaChildId: Long) {
        val vardaClient = VardaClient(tokenProvider, fuel, env, mapper)

        try {
            val feeDataIds = vardaClient.getFeeDataByChild(vardaChildId)
            deleteFeeData(db, vardaClient, feeDataIds)

            val decisionIds = vardaClient.getDecisionsByChild(vardaChildId)
            val placementIds = decisionIds.flatMap {
                vardaClient.getPlacementsByDecision(it)
            }
            deletePlacements(db, vardaClient, placementIds)
            deleteDecisions(db, vardaClient, decisionIds)
            deleteChild(db, vardaClient, vardaChildId)
        } catch (e: Exception) {
            logger.error("VardaUpdate: could not delete old varda data for child $vardaChildId: ${e.localizedMessage}")
        }
    }
}

fun updateAllVardaData(
    db: Database.Connection,
    client: VardaClient,
    organizer: String
) {
    val since = HelsinkiDateTime.now().minusHours(24)
    logger.info("VardaUpdate: running varda update for data modified since $since")
    updateOrganizer(db, client, organizer)
    updateUnits(db, client, organizer)
    updateChildData(db, client, since)
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
fun updateChildData(db: Database.Connection, client: VardaClient, startingFrom: HelsinkiDateTime) {
    retryUnsuccessfulServiceNeedVardaUpdates(db, client)

    val serviceNeedDiffsByChild = calculateEvakaVsVardaServiceNeedChangesByChild(db, startingFrom)
    val feeAndVoucherDiffsByServiceNeed = db.read { it.getServiceNeedFeeData(startingFrom, null) }.groupBy { it.serviceNeedId }
    val processedServiceNeedIds = mutableSetOf<UUID>()

    logger.info("VardaUpdate: found ${serviceNeedDiffsByChild.entries.size} children with changed service need data")

    serviceNeedDiffsByChild.entries.forEach { serviceNeedDiffByChild ->
        serviceNeedDiffByChild.value.deletes.forEach { deletedServiceNeedId ->
            processedServiceNeedIds.add(deletedServiceNeedId)
            handleDeletedEvakaServiceNeed(db, client, deletedServiceNeedId)
        }

        serviceNeedDiffByChild.value.updates.forEach { updatedServiceNeedId ->
            processedServiceNeedIds.add(updatedServiceNeedId)
            handleUpdatedEvakaServiceNeed(db, client, updatedServiceNeedId)
        }

        serviceNeedDiffByChild.value.additions.forEach { addedServiceNeedId ->
            processedServiceNeedIds.add(addedServiceNeedId)
            handleNewEvakaServiceNeed(db, client, addedServiceNeedId)
        }
    }

    // Handle fee data only -changes (no changes in service need). If there was any service need change, all related
    // fee data has already been sent to / deleted from varda, so we filter those service needs out here
    val unprocessedChangedFeeData = feeAndVoucherDiffsByServiceNeed.filterNot { processedServiceNeedIds.contains(it.key) }
    if (unprocessedChangedFeeData.entries.size > 0) logger.info("VardaUpdate: found ${unprocessedChangedFeeData.entries.size} unprocessed fee data")

    unprocessedChangedFeeData.entries.forEach {
        val serviceNeedId = it.key
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(serviceNeedId) }
        if (vardaServiceNeed != null) {
            handleUpdatedEvakaServiceNeed(db, client, serviceNeedId)
        } else {
            handleNewEvakaServiceNeed(db, client, serviceNeedId)
        }
    }
}

fun handleDeletedEvakaServiceNeed(db: Database.Connection, client: VardaClient, serviceNeedId: UUID) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(serviceNeedId) }
        if (vardaServiceNeed != null) {
            val errors = deleteServiceNeedDataFromVarda(client, vardaServiceNeed)
            if (errors.isNotEmpty()) {
                logger.error(errors.joinToString(","))
                db.transaction { it.markVardaServiceNeedUpdateFailed(serviceNeedId, errors) }
            } else {
                db.transaction { it.deleteVardaServiceNeedByEvakaServiceNeed(serviceNeedId) }
            }
        }
    } catch (e: Exception) {
        logger.error("VardaUpdate: manual check needed: something went wrong while trying to delete varda service need $serviceNeedId data: ${e.localizedMessage}")
    }
}

fun handleUpdatedEvakaServiceNeed(db: Database.Connection, client: VardaClient, evakaServiceNeedId: UUID) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) } ?: throw Exception("VardaUpdate: cannot update service need $evakaServiceNeedId: varda service need missing")
        val deleteErrors = deleteServiceNeedDataFromVarda(client, vardaServiceNeed)
        if (deleteErrors.isNotEmpty()) {
            logger.error(deleteErrors.joinToString(","))
            db.transaction {
                it.markVardaServiceNeedUpdateFailed(evakaServiceNeedId, deleteErrors)
            }
        } else {
            val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(evakaServiceNeedId) }
            val addErrors = addServiceNeedDataToVarda(db, client, evakaServiceNeed, vardaServiceNeed)
            if (addErrors.isNotEmpty()) {
                logger.error(addErrors.joinToString(","))
                db.transaction {
                    it.markVardaServiceNeedUpdateFailed(evakaServiceNeedId, addErrors)
                }
            } else {
                db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed.copy(updateFailed = false, errors = mutableListOf())) }
            }
        }
    } catch (e: Exception) {
        logger.error("VardaUpdate: manual check needed: something went wrong while trying to add varda service need $evakaServiceNeedId data: ${e.localizedMessage}")
    }
}

fun handleNewEvakaServiceNeed(db: Database.Connection, client: VardaClient, serviceNeedId: UUID) {
    try {
        val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(serviceNeedId) }
        val newVardaServiceNeed = evakaServiceNeedToVardaServiceNeed(evakaServiceNeed.childId, evakaServiceNeed)
        val errors = addServiceNeedDataToVarda(db, client, evakaServiceNeed, newVardaServiceNeed)
        db.transaction { it.upsertVardaServiceNeed(newVardaServiceNeed) }

        if (errors.isNotEmpty()) {
            logger.error(errors.joinToString(","))
            db.transaction {
                it.markVardaServiceNeedUpdateFailed(serviceNeedId, errors)
            }
        }
    } catch (e: Exception) {
        logger.error("VardaUpdate: manual check needed: something went wrong while trying to add varda service need $serviceNeedId data: ${e.localizedMessage}")
    }
}

fun retryUnsuccessfulServiceNeedVardaUpdates(db: Database.Connection, vardaClient: VardaClient) {
    val unsuccessfullyUploadedServiceNeeds = db.read { it.getUnsuccessfullyUploadVardaServiceNeeds() }

    if (unsuccessfullyUploadedServiceNeeds.size > 0)
        logger.info("VardaUpdate: retrying failed varda uploads: ${unsuccessfullyUploadedServiceNeeds.size}")

    unsuccessfullyUploadedServiceNeeds.forEach {
        try {
            if (it.existsInEvaka && it.childId != null)
                handleUpdatedEvakaServiceNeed(db, vardaClient, it.evakaServiceNeedId)
            else
                handleDeletedEvakaServiceNeed(db, vardaClient, it.evakaServiceNeedId)
        } catch (e: Exception) {
            logger.error("VardaUpdate: got an error while trying to redo a failed service need varda update: ${e.localizedMessage}")
        }
    }
}

// Delete varda decision, placement and related varda fee decisions
fun deleteServiceNeedDataFromVarda(vardaClient: VardaClient, vardaServiceNeed: VardaServiceNeed): List<String> {
    val errors = mutableListOf<String>()
    try {
        errors.addAll(deleteServiceNeedAndRelatedDataFromVarda(vardaClient, vardaServiceNeed))
    } catch (e: Exception) {
        errors.add("VardaUpdate: error deleting varda service need ${vardaServiceNeed.evakaServiceNeedId} related data: ${e.localizedMessage}")
    }
    return errors
}

// Add child if missing, service need, placement and mandatory fee decision(s)
fun addServiceNeedDataToVarda(db: Database.Connection, vardaClient: VardaClient, evakaServiceNeed: EvakaServiceNeedInfoForVarda, newVardaServiceNeed: VardaServiceNeed): List<String> {
    val errors = mutableListOf<String>()

    try {
        // Todo: "nettopalvelu"-unit children do not have fee data
        val serviceNeedFeeData = db.read { it.getServiceNeedFeeData(null, evakaServiceNeed.id) }.firstOrNull()
        if (serviceNeedFeeData != null && (serviceNeedFeeData.feeDecisionIds.isNotEmpty() || serviceNeedFeeData.voucherValueDecisionIds.isNotEmpty())) {
            if (evakaServiceNeed.ophOrganizerOid.isNullOrEmpty()) throw Exception("VardaUpdate: service need ${evakaServiceNeed.id} related oph_orginizer_id is null or empty")
            if (db.read { it.personHasSsn(evakaServiceNeed.childId) }.not()) throw Exception("VardaUpdate: cannot create service need ${evakaServiceNeed.id} for child ${evakaServiceNeed.childId} because child has no ssn")

            newVardaServiceNeed.vardaChildId = getOrCreateVardaChildByOrganizer(db, vardaClient, evakaServiceNeed.childId, evakaServiceNeed.ophOrganizerOid, vardaClient.sourceSystem)
            newVardaServiceNeed.vardaDecisionId = createDecisionToVarda(vardaClient, newVardaServiceNeed.vardaChildId, evakaServiceNeed)
            newVardaServiceNeed.vardaPlacementId = createPlacementToVarda(vardaClient, newVardaServiceNeed.vardaDecisionId!!, evakaServiceNeed)
            newVardaServiceNeed.vardaFeeDataIds = serviceNeedFeeData.feeDecisionIds.mapNotNull { fdId ->
                createFeeDataToVardaFromFeeDecision(db, vardaClient, newVardaServiceNeed.vardaChildId!!, evakaServiceNeed, fdId)
            }.plus(
                serviceNeedFeeData.voucherValueDecisionIds.map { vvdId ->
                    createFeeDataToVardaFromVoucherValueDecision(db, vardaClient, newVardaServiceNeed.vardaChildId!!, evakaServiceNeed, vvdId)
                }
            )
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: error creating a new varda decision for service need ${evakaServiceNeed.id}: ${e.message}")
        // TODO: remove once everything works
        logger.error("VardaUpdate: new varda decision errored with: ".plus(e.stackTrace.joinToString(",")))
    }

    return errors
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
    decisionId: UUID
): Long? {
    val decision = db.read { it.getFeeDecisionsByIds(listOf(decisionId)) }.first()
    val childPart = decision.children.find { part -> part.child.id == evakaServiceNeedInfoForVarda.childId }
        ?: throw Exception("VardaUpdate: could not create fee data for ${evakaServiceNeedInfoForVarda.id}: fee ${decision.id} has no part for child ${evakaServiceNeedInfoForVarda.childId}")

    val guardians = getChildVardaGuardians(db, evakaServiceNeedInfoForVarda.childId)
    if (guardians.isEmpty()) throw Exception("VardaUpdate: could not create fee data for ${evakaServiceNeedInfoForVarda.id}: child has no guardians")

    // If head of family is not a guardian, fee data is not supposed to be sent to varda (https://wiki.eduuni.fi/display/OPHPALV/Huoltajan+tiedot)
    if (guardians.none { it.id == decision.headOfFamily.id }) {
        logger.info("VardaUpdate: will not send fee data for service need ${evakaServiceNeedInfoForVarda.id} child ${evakaServiceNeedInfoForVarda.childId}: head of family is not a guardian")
        return null
    }

    val res: VardaFeeDataResponse?
    try {
        res = client.createFeeData(
            VardaFeeData(
                huoltajat = guardians.map { it.toVardaGuardian() },
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

    if (res == null) throw Exception("VardaUpdate: cannot create fee data ${decision.id} for service need ${evakaServiceNeedInfoForVarda.id}: create varda fee data response is null (response failed, see logs for the actual reason)")
    return res.id
}

data class VardaGuardianWithId(
    val id: UUID,
    val henkilotunnus: String,
    val etunimet: String,
    val sukunimi: String
) {
    fun toVardaGuardian(): VardaGuardian = VardaGuardian(
        henkilotunnus = henkilotunnus,
        etunimet = etunimet,
        sukunimi = sukunimi
    )
}

fun getChildVardaGuardians(db: Database.Connection, childId: UUID): List<VardaGuardianWithId> {
    return db.read {
        it.createQuery("select id, first_name AS etunimet, last_name as sukunimi, social_security_number AS henkilotunnus FROM person where id IN (SELECT guardian_id FROM guardian WHERE child_id = :id)")
            .bind("id", childId)
            .mapTo<VardaGuardianWithId>()
            .list()
    }
}

fun createFeeDataToVardaFromVoucherValueDecision(
    db: Database.Connection,
    client: VardaClient,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    id: UUID
): Long {
    val decision = db.read { it.getVoucherValueDecision(id) }
        ?: throw Exception("VardaUpdate: cannot create voucher fee data: voucher $id not found")

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

    if (res == null) throw Exception("VardaUpdate: cannot create voucher fee data ${decision.id} for service need ${evakaServiceNeedInfoForVarda.id}: create varda fee data response is null")
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

    // Above misses the case when only change for a child has been a service need delete from evaka
    val deletedEvakaServiceNeedsByChild = evakaServiceNeedDeletionsByChild.filterNot {
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

    return additionsAndChangesToVardaByChild.plus(deletedEvakaServiceNeedsByChild)
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

fun Database.Transaction.upsertVardaServiceNeed(vardaServiceNeed: VardaServiceNeed) = createUpdate(
    """
INSERT INTO varda_service_need (evaka_service_need_id, evaka_service_need_option_id, evaka_service_need_updated, evaka_service_need_option_updated, evaka_child_id, varda_decision_id, varda_placement_id, varda_fee_data_ids, update_failed, errors) 
VALUES (:evakaServiceNeedId, :evakaServiceNeedOptionId, :evakaServiceNeedUpdated, :evakaServiceNeedOptionUpdated, :evakaChildId, :vardaDecisionId, :vardaPlacementId, :vardaFeeDataIds, :updateFailed, :errors)
ON CONFLICT (evaka_service_need_id) DO UPDATE 
    SET evaka_service_need_option_id = :evakaServiceNeedOptionId, 
        evaka_service_need_updated = :evakaServiceNeedUpdated, 
        evaka_service_need_option_updated = :evakaServiceNeedOptionUpdated, 
        evaka_child_id = :evakaChildId, 
        varda_decision_id = :vardaDecisionId, 
        varda_placement_id = :vardaPlacementId, 
        varda_fee_data_ids = :vardaFeeDataIds,
        update_failed = :updateFailed,
        errors = :errors
"""
).bindKotlin(vardaServiceNeed)
    .execute()

fun Database.Transaction.deleteVardaServiceNeedByEvakaServiceNeed(serviceNeedId: UUID) = createUpdate(
    """
DELETE FROM varda_service_need
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .execute()

fun Database.Transaction.markVardaServiceNeedUpdateFailed(serviceNeedId: UUID, errors: List<String>) = createUpdate(
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
    SELECT id FROM service_need
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
FROM service_need sn
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

fun Database.Read.getVardaServiceNeedByEvakaServiceNeedId(eVakaServiceNeedId: UUID): VardaServiceNeed? =
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

fun Database.Read.getServiceNeedFeeData(startingFrom: HelsinkiDateTime?, serviceNeedId: UUID?): List<FeeDataByServiceNeed> =
    createQuery(
        """
WITH child_fees AS (
  SELECT
    fd.id AS fee_decision_id,
    fdc.child_id,
    fd.valid_during
  FROM fee_decision fd JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id 
  ${if (startingFrom != null) " WHERE fd.sent_at >= :startingFrom" else ""}  
), service_need_fees AS (
  SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(child_fees.fee_decision_id) AS fee_decision_ids
  FROM service_need sn JOIN placement p ON p.id = sn.placement_id
    JOIN child_fees ON p.child_id = child_fees.child_id 
      AND child_fees.valid_during && daterange(sn.start_date, sn.end_date, '[]')
  GROUP BY service_need_id, p.child_id
), service_need_vouchers AS (
SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(vvd.id) AS voucher_value_decision_ids
FROM service_need sn JOIN placement p ON p.id = sn.placement_id
  JOIN voucher_value_decision vvd ON p.child_id = vvd.child_id 
    AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange(sn.start_date, sn.end_date, '[]')
${if (startingFrom != null) " WHERE vvd.sent_at >= :startingFrom" else ""}      
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
            sn.id, p.child_id AS child_id, LEAST(COALESCE(a.sentdate, a.created::date), sn.start_date) AS application_date, sn.start_date, sn.end_date,
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
        FROM service_need sn
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

data class UnsuccessfullyUploadedServiceNeed(
    val evakaServiceNeedId: UUID,
    val existsInEvaka: Boolean,
    val childId: UUID?
)

fun Database.Read.getUnsuccessfullyUploadVardaServiceNeeds(): List<UnsuccessfullyUploadedServiceNeed> =
    createQuery(
        """
SELECT 
    vsn.evaka_service_need_id, 
    EXISTS(SELECT 1 FROM service_need sn WHERE sn.id = vsn.evaka_service_need_id ) AS exists_in_evaka,
    p.child_id
FROM varda_service_need vsn 
    LEFT JOIN service_need sn ON sn.id = vsn.evaka_service_need_id
    LEFT JOIN placement p ON p.id = sn.placement_id
WHERE update_failed = true"""
    )
        .mapTo<UnsuccessfullyUploadedServiceNeed>()
        .list()

fun Database.Read.personHasSsn(id: UUID): Boolean = createQuery("SELECT CASE WHEN (social_security_number IS NOT NULL AND social_security_number <> '') THEN 'True' ELSE 'False' END FROM person WHERE id = :id")
    .bind("id", id)
    .mapTo<Boolean>()
    .firstOrNull() ?: false
