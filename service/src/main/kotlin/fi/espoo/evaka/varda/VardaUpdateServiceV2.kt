// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.getServiceNeedsByChild
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VoucherValueDecisionId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VardaUpdateV2
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
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
    private val mapper: ObjectMapper,
    private val env: VardaEnv
) {
    private val organizer = env.organizer

    init {
        asyncJobRunner.vardaUpdateV2 = ::updateAll
    }

    fun scheduleVardaUpdate(db: Database.Connection, runNow: Boolean = false) {
        if (runNow) {
            logger.info("VardaUpdate: running varda update immediately")
            val client = VardaClient(tokenProvider, fuel, mapper, env)
            updateAllVardaData(db, client, organizer)
        } else {
            logger.info("VardaUpdate: scheduling varda update")
            db.transaction { asyncJobRunner.plan(it, listOf(VardaUpdateV2()), retryCount = 1) }
        }
    }

    fun updateAll(db: Database, msg: VardaUpdateV2) {
        val client = VardaClient(tokenProvider, fuel, mapper, env)
        db.connect { updateAllVardaData(it, client, organizer) }
    }

    fun resetChildren(db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, mapper, env)

        val resetChildIds = db.read { it.getVardaChildrenToReset() }
        logger.info("VardaUpdate: will reset ${resetChildIds.size} children")

        resetChildIds.forEach { childId ->
            if (deleteChildDataFromVardaAndDb(db, client, childId)) {
                val childServiceNeeds = db.read { it.getServiceNeedsByChild(childId) }
                logger.info("VardaUpdate: will send ${childServiceNeeds.size} service needs for child $childId")
                try {
                    childServiceNeeds.forEach { serviceNeed ->
                        handleNewEvakaServiceNeed(db, client, serviceNeed.id)
                    }
                    db.transaction { it.setVardaResetChildResetTimestamp(childId, Instant.now()) }
                    logger.info("VardaUpdate: successfully sent ${childServiceNeeds.size} service needs for $childId")
                } catch (e: Exception) {
                    logger.warn("VardaUpdate: could not add service need for reset child $childId: ${e.message} - full reset will be retried next time")
                }
            } else {
                logger.warn("VardaUpdate: could not reset evaka child $childId from varda")
            }
        }

        logger.info("VardaUpdate: successfully reset ${resetChildIds.size} children")
    }

    fun clearAllExistingVardaChildDataFromVarda(db: Database.Connection, vardaChildId: Long) {
        val vardaClient = VardaClient(tokenProvider, fuel, mapper, env)

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

fun deleteChildDataFromVardaAndDb(db: Database.Connection, vardaClient: VardaClient, evakaChildId: UUID): Boolean {
    logger.info("VardaUpdate: resetting all varda data for evaka child $evakaChildId")

    val vardaChildIds = getVardaChildIdsByEvakaChildId(db, evakaChildId)

    val successfulDeletes: List<Boolean> = vardaChildIds.map { vardaChildId ->
        try {
            val decisionIds = vardaClient.getDecisionsByChild(vardaChildId)
            logger.info { "VardaUpdate: found ${decisionIds.size} decisions to be deleted for child $vardaChildId" }

            val placementIds = decisionIds.flatMap { vardaClient.getPlacementsByDecision(it) }
            logger.info { "VardaUpdate: found ${placementIds.size} placements to be deleted for child $vardaChildId" }

            val feeIds = placementIds.flatMap { vardaClient.getFeeDataByChild(it) }
            logger.info { "VardaUpdate: found ${feeIds.size} fee data to be deleted for child $vardaChildId" }

            feeIds.forEach { feeId ->
                if (vardaClient.deleteFeeDataV2(feeId)) {
                    logger.info { "VardaUpdate: Deleting fee data from db by id $feeId" }
                    db.transaction { deleteVardaFeeData(it, feeId) }
                }
            }

            placementIds.forEach { placementId ->
                if (vardaClient.deletePlacementV2(placementId)) {
                    logger.info { "VardaUpdate: Deleting placement data from db by id $placementId" }
                    db.transaction { deletePlacement(it, placementId) }
                }
            }

            decisionIds.forEach { decisionId ->
                if (vardaClient.deleteDecisionV2(decisionId)) {
                    logger.info { "VardaUpdate: Deleting decision data from db by id $decisionId" }
                    db.transaction { deleteDecision(it, decisionId) }
                }
            }

            logger.info { "VardaUpdate: Deleting from varda_service_need by child $vardaChildId" }
            db.transaction {
                it.deleteVardaServiceNeedByVardaChildId(vardaChildId)
            }

            logger.info { "VardaUpdate: successfully deleted data of child $vardaChildId" }
            return true
        } catch (e: Exception) {
            logger.info("VardaUpdate: couldn't reset varda child $vardaChildId: ${e.localizedMessage}")
            return false
        }
    }

    return successfulDeletes.all { it }
}

fun getVardaChildIdsByEvakaChildId(db: Database.Connection, evakaChildId: UUID): List<Long> {
    return db.read {
        it.createQuery(
            """
            select varda_child_id from varda_service_need where evaka_child_id = :evakaChildId
            union
            select varda_child_id from varda_organizer_child where evaka_person_id = :evakaChildId
            union
            select varda_child_id from varda_child where person_id = :evakaChildId
            """.trimIndent()
        )
            .bind("evakaChildId", evakaChildId)
            .mapTo<Long>()
            .toList()
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
    0. If there are any existing failed service need updates, try to delete and read the service need data to varda
    1. Find out all changed service needs.
        - For each deleted service need, delete all related data from varda
        - For each new service need, IF related fee data exists, add all related data to varda
        - For each modified service need, delete old related data from varda and add new
    2. Find out all changed evaka fee data affecting service needs not yet updated above, and for each service need
       update all service need related data to varda
 */
fun updateChildData(db: Database.Connection, client: VardaClient, startingFrom: HelsinkiDateTime) {
    val processedServiceNeedIds = retryUnsuccessfulServiceNeedVardaUpdates(db, client).toMutableSet()
    logger.info { "VardaUpdate: successfully processed ${processedServiceNeedIds.size} unsuccessful service needs" }

    val serviceNeedDiffsByChild = calculateEvakaVsVardaServiceNeedChangesByChild(db, startingFrom)
    val feeAndVoucherDiffsByServiceNeed = db.read { it.getAllChangedServiceNeedFeeDataSince(startingFrom) }.groupBy { it.serviceNeedId }

    logger.info("VardaUpdate: found ${serviceNeedDiffsByChild.entries.size} children with changed service need data")

    serviceNeedDiffsByChild.entries.forEach { serviceNeedDiffByChild ->
        serviceNeedDiffByChild.value.deletes.forEach { deletedServiceNeedId ->
            if (!processedServiceNeedIds.contains(deletedServiceNeedId)) {
                processedServiceNeedIds.add(deletedServiceNeedId)
                handleDeletedEvakaServiceNeed(db, client, deletedServiceNeedId)
            }
        }

        serviceNeedDiffByChild.value.updates.forEach { updatedServiceNeedId ->
            if (!processedServiceNeedIds.contains(updatedServiceNeedId)) {
                processedServiceNeedIds.add(updatedServiceNeedId)
                handleUpdatedEvakaServiceNeed(db, client, updatedServiceNeedId)
            }
        }

        serviceNeedDiffByChild.value.additions.forEach { addedServiceNeedId ->
            if (!processedServiceNeedIds.contains(addedServiceNeedId)) {
                processedServiceNeedIds.add(addedServiceNeedId)
                handleNewEvakaServiceNeed(db, client, addedServiceNeedId)
            }
        }
    }

    // Handle fee data only -changes (no changes in service need). If there was any service need change, all related
    // fee data has already been sent to / deleted from varda, so we filter those service needs out here
    val unprocessedChangedFeeData = feeAndVoucherDiffsByServiceNeed.filterNot { processedServiceNeedIds.contains(it.key) }
    if (unprocessedChangedFeeData.entries.isNotEmpty()) logger.info("VardaUpdate: found ${unprocessedChangedFeeData.entries.size} unprocessed fee data")

    unprocessedChangedFeeData.entries.forEach { it ->
        val serviceNeedId = it.key
        processedServiceNeedIds.add(serviceNeedId)
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(serviceNeedId) }
        if (vardaServiceNeed != null) {
            handleUpdatedEvakaServiceNeed(db, client, serviceNeedId)
        } else {
            handleNewEvakaServiceNeed(db, client, serviceNeedId)
        }
    }

    logger.info("VardaUpdate: processed ${processedServiceNeedIds.size} service needs")
}

fun handleDeletedEvakaServiceNeed(db: Database.Connection, client: VardaClient, serviceNeedId: ServiceNeedId) {
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

fun handleUpdatedEvakaServiceNeed(db: Database.Connection, client: VardaClient, evakaServiceNeedId: ServiceNeedId) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) } ?: error("VardaUpdate: cannot update service need $evakaServiceNeedId: varda service need missing")
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
            }
            db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed.copy(updateFailed = addErrors.isNotEmpty(), errors = addErrors.toMutableList())) }
        }
    } catch (e: Exception) {
        logger.error("VardaUpdate: manual check needed: something went wrong while trying to add varda service need $evakaServiceNeedId data: ${e.localizedMessage}")
    }
}

fun handleNewEvakaServiceNeed(db: Database.Connection, client: VardaClient, serviceNeedId: ServiceNeedId) {
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

fun retryUnsuccessfulServiceNeedVardaUpdates(db: Database.Connection, vardaClient: VardaClient): List<ServiceNeedId> {
    val unsuccessfullyUploadedServiceNeeds = db.read { it.getUnsuccessfullyUploadVardaServiceNeeds() }

    if (unsuccessfullyUploadedServiceNeeds.isNotEmpty())
        logger.info("VardaUpdate: retrying failed varda uploads: ${unsuccessfullyUploadedServiceNeeds.size}")

    return unsuccessfullyUploadedServiceNeeds.map {
        try {
            if (it.existsInEvaka && it.childId != null)
                handleUpdatedEvakaServiceNeed(db, vardaClient, it.evakaServiceNeedId)
            else
                handleDeletedEvakaServiceNeed(db, vardaClient, it.evakaServiceNeedId)
            logger.info("VardaUpdate: successfully processed unsuccessful service need ${it.evakaServiceNeedId}")
        } catch (e: Exception) {
            logger.error("VardaUpdate: got an error while processing an unsuccessful service need: ${e.localizedMessage}")
        }
        it.evakaServiceNeedId
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
        check(!evakaServiceNeed.ophOrganizerOid.isNullOrBlank()) {
            "VardaUpdate: service need daycare oph_organizer_oid is null or empty"
        }

        // Todo: "nettopalvelu"-unit children do not have fee data
        val serviceNeedFeeData = db.read { it.getServiceNeedFeeData(evakaServiceNeed.id, FeeDecisionStatus.SENT, VoucherValueDecisionStatus.SENT) }.firstOrNull()

        if (serviceNeedFeeData != null && (serviceNeedFeeData.feeDecisionIds.isNotEmpty() || serviceNeedFeeData.voucherValueDecisionIds.isNotEmpty())) {
            newVardaServiceNeed.vardaChildId = getOrCreateVardaChildByOrganizer(db, vardaClient, evakaServiceNeed.childId, evakaServiceNeed.ophOrganizerOid, vardaClient.sourceSystem)
            newVardaServiceNeed.vardaDecisionId = sendDecisionToVarda(vardaClient, newVardaServiceNeed.vardaChildId, evakaServiceNeed)
            newVardaServiceNeed.vardaPlacementId = sendPlacementToVarda(vardaClient, newVardaServiceNeed.vardaDecisionId!!, evakaServiceNeed)
            newVardaServiceNeed.vardaFeeDataIds = sendFeeDataToVarda(vardaClient, db, newVardaServiceNeed, evakaServiceNeed, serviceNeedFeeData)
        }
    } catch (e: Exception) {
        errors.add("VardaUpdate: error creating a new varda decision for service need ${evakaServiceNeed.id}: ${e.message}")
        // TODO: remove once everything works
        logger.error("VardaUpdate: new varda decision errored with: ".plus(e.stackTrace.joinToString(",")))
    }

    return errors
}

fun sendDecisionToVarda(client: VardaClient, vardaChildId: Long?, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    if (vardaChildId == null) error("VardaUpdate: cannot create decision for ${evakaServiceNeedInfoForVarda.id}: child varda id missing")
    val res: VardaDecisionResponse?
    try {
        res = client.createDecision(evakaServiceNeedInfoForVarda.toVardaDecisionForChild(client.getChildUrl(vardaChildId), client.sourceSystem))
    } catch (e: Exception) {
        error("VardaUpdate: cannot create decision for ${evakaServiceNeedInfoForVarda.id}: varda client threw ${e.localizedMessage}")
    }

    if (res == null) error("VardaUpdate: cannot create decision for ${evakaServiceNeedInfoForVarda.id}: create varda decision response is null")
    return res.vardaDecisionId
}

fun sendPlacementToVarda(client: VardaClient, vardaDecisionId: Long, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    val res: VardaPlacementResponse = try {
        client.createPlacement(evakaServiceNeedInfoForVarda.toVardaPlacement(client.getDecisionUrl(vardaDecisionId), client.sourceSystem))
    } catch (e: Exception) {
        error("VardaUpdate: cannot create placement for ${evakaServiceNeedInfoForVarda.id}: varda client threw ${e.localizedMessage}")
    } ?: error("VardaUpdate: cannot create placement for ${evakaServiceNeedInfoForVarda.id}: create varda placement response is null")

    return res.vardaPlacementId
}

private fun guardiansLiveInSameAddress(guardians: List<VardaGuardianWithId>): Boolean {
    val validResidenceCodes = guardians.map { g -> g.asuinpaikantunnus }
        .filterNot { it.isNullOrBlank() }
    return validResidenceCodes.size == guardians.size && validResidenceCodes.toSet().size == 1
}

fun sendFeeDataToVarda(vardaClient: VardaClient, db: Database.Connection, newVardaServiceNeed: VardaServiceNeed, evakaServiceNeed: EvakaServiceNeedInfoForVarda, feeDataByServiceNeed: FeeDataByServiceNeed): List<Long> {
    val guardians = getChildVardaGuardians(db, evakaServiceNeed.childId)

    if (guardians.isEmpty()) {
        logger.info("VardaUpdate: will not create fee data for service need ${evakaServiceNeed.id}: child has no guardians")
        return emptyList()
    }

    val feeResponseIds: List<Long> = feeDataByServiceNeed.feeDecisionIds.mapNotNull { feeId ->
        try {
            logger.info { "VardaUpdate: trying to send fee data $feeId" }
            val decision = db.read { it.getFeeDecisionsByIds(listOf(feeId)) }.first()

            // If head of family is not a guardian, fee data is not supposed to be sent to varda (https://wiki.eduuni.fi/display/OPHPALV/Huoltajan+tiedot)
            if (guardians.none { it.id == decision.headOfFamily.id }) {
                logger.info { "VardaUpdate: won't send fee data for fee decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}" }
                null
            } else sendFeeDecisionToVarda(
                client = vardaClient,
                decision = decision,
                vardaChildId = newVardaServiceNeed.vardaChildId!!,
                evakaServiceNeedInfoForVarda = evakaServiceNeed,
                guardians = if (guardiansLiveInSameAddress(guardians)) guardians else guardians.filter { guardian -> guardian.id == decision.headOfFamily.id }
            ) ?: error("VardaUpdate: client failed fee request for $feeId - see logs for details")
        } catch (e: Exception) {
            error { "VardaUpdate: failed to send fee decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}" }
        }
    }

    val voucherResponseIds = feeDataByServiceNeed.voucherValueDecisionIds.mapNotNull { feeId ->
        try {
            logger.info { "VardaUpdate: trying to send voucher data $feeId" }
            val decision = db.read { it.getVoucherValueDecision(feeId) }!!

            if (guardians.none { it.id == decision.headOfFamily.id }) {
                logger.info { "VardaUpdate: won't send fee data for voucher decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}" }
                null
            } else sendVoucherDecisionToVarda(
                client = vardaClient,
                decision = decision,
                vardaChildId = newVardaServiceNeed.vardaChildId!!,
                evakaServiceNeedInfoForVarda = evakaServiceNeed,
                guardians = guardians
            ) ?: error("VardaUpdate: client failed voucher request for $feeId - see logs for details")
        } catch (e: Exception) {
            error { "VardaUpdate: failed to send voucher decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}" }
        }
    }

    return feeResponseIds + voucherResponseIds
}

data class VardaGuardianWithId(
    val id: UUID,
    val henkilotunnus: String?,
    val henkilo_oid: String?,
    val etunimet: String,
    val sukunimi: String,
    val asuinpaikantunnus: String?
) {
    fun toVardaGuardian(): VardaGuardian = VardaGuardian(
        henkilotunnus = henkilotunnus,
        henkilo_oid = henkilo_oid,
        etunimet = etunimet,
        sukunimi = sukunimi
    )
}

fun getChildVardaGuardians(db: Database.Connection, childId: UUID): List<VardaGuardianWithId> {
    return db.read {
        it.createQuery("select id, first_name AS etunimet, last_name as sukunimi, social_security_number AS henkilotunnus, oph_person_oid AS henkilo_oid, residence_code AS asuinpaikantunnus FROM person where id IN (SELECT guardian_id FROM guardian WHERE child_id = :id)")
            .bind("id", childId)
            .mapTo<VardaGuardianWithId>()
            .list()
    }
}

fun getGuardianFromVarda(client: VardaClient, ssn: String?, oid: String?): VardaGuardian {
    val person = client.getPersonFromVardaBySsnOrOid(VardaClient.VardaPersonSearchRequest(ssn, oid))
        ?: error("VardaUpdate: couldn't fetch guardian from Varda")
    return VardaGuardian(
        henkilotunnus = ssn,
        henkilo_oid = person.personOid,
        etunimet = person.firstName,
        sukunimi = person.lastName
    )
}

fun sendFeeDecisionToVarda(
    client: VardaClient,
    decision: FeeDecision,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    guardians: List<VardaGuardianWithId>
): Long? {
    val childPart = decision.children.find { part -> part.child.id == evakaServiceNeedInfoForVarda.childId }
        ?: error("VardaUpdate: could not create fee data for ${evakaServiceNeedInfoForVarda.id}: fee ${decision.id} has no part for child ${evakaServiceNeedInfoForVarda.childId}")

    val requestPayload = VardaFeeData(
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

    return client.createFeeData(requestPayload)?.id
        ?: client.createFeeData(requestPayload.copy(huoltajat = guardians.map { getGuardianFromVarda(client, it.henkilotunnus, it.henkilo_oid) }))?.id
}

fun sendVoucherDecisionToVarda(
    client: VardaClient,
    decision: VoucherValueDecisionDetailed,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    guardians: List<VardaGuardianWithId>
): Long? {
    val requestPayload = VardaFeeData(
        huoltajat = guardians.map { guardian -> guardian.toVardaGuardian() },
        lapsi = client.getChildUrl(vardaChildId),
        maksun_peruste_koodi = vardaFeeBasisByPlacementType(decision.placement.type),
        asiakasmaksu = decision.finalCoPayment.div(100.0),
        palveluseteli_arvo = decision.voucherValue.div(100.0),
        perheen_koko = decision.familySize,
        alkamis_pvm = calculateVardaFeeDataStartDate(decision.validFrom, evakaServiceNeedInfoForVarda.asPeriod),
        paattymis_pvm = minEndDate(evakaServiceNeedInfoForVarda.endDate, decision.validTo),
        lahdejarjestelma = client.sourceSystem
    )

    return client.createFeeData(requestPayload)?.id
        ?: client.createFeeData(requestPayload.copy(huoltajat = guardians.map { getGuardianFromVarda(client, it.henkilotunnus, it.henkilo_oid) }))?.id
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
    val additions: List<ServiceNeedId>,
    val updates: List<ServiceNeedId>,
    val deletes: List<ServiceNeedId>
)

// Find out new varhaiskasvatuspaatos to be added for a child: any new service needs not found from child's full varda history
private fun calculateNewChildServiceNeeds(evakaServiceNeedChangesForChild: List<VardaServiceNeed>, vardaChildServiceNeeds: List<VardaServiceNeed>): List<ServiceNeedId> {
    return evakaServiceNeedChangesForChild.filter { newServiceNeedChange ->
        vardaChildServiceNeeds.none { vardaChildServiceNeedChange ->
            vardaChildServiceNeedChange.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId
        }
    }.map {
        it.evakaServiceNeedId
    }
}

// Find out changed varhaiskasvatuspaatos for a child: any new service need with a different update timestamp in history
private fun calculateUpdatedChildServiceNeeds(evakaServiceNeedChangesForChild: List<VardaServiceNeed>, vardaServiceNeedsForChild: List<VardaServiceNeed>): List<ServiceNeedId> {
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
INSERT INTO varda_service_need (evaka_service_need_id, evaka_service_need_option_id, evaka_service_need_updated, evaka_service_need_option_updated, evaka_child_id, varda_child_id, varda_decision_id, varda_placement_id, varda_fee_data_ids, update_failed, errors) 
VALUES (:evakaServiceNeedId, :evakaServiceNeedOptionId, :evakaServiceNeedUpdated, :evakaServiceNeedOptionUpdated, :evakaChildId, :vardaChildId, :vardaDecisionId, :vardaPlacementId, :vardaFeeDataIds, :updateFailed, :errors)
ON CONFLICT (evaka_service_need_id) DO UPDATE 
    SET evaka_service_need_option_id = :evakaServiceNeedOptionId, 
        evaka_service_need_updated = :evakaServiceNeedUpdated, 
        evaka_service_need_option_updated = :evakaServiceNeedOptionUpdated, 
        evaka_child_id = :evakaChildId, 
        varda_child_id = :vardaChildId,
        varda_decision_id = :vardaDecisionId, 
        varda_placement_id = :vardaPlacementId, 
        varda_fee_data_ids = :vardaFeeDataIds,
        update_failed = :updateFailed,
        errors = :errors
"""
).bindKotlin(vardaServiceNeed)
    .execute()

fun Database.Transaction.deleteVardaServiceNeedByEvakaServiceNeed(serviceNeedId: ServiceNeedId) = createUpdate(
    """
DELETE FROM varda_service_need
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .execute()

fun Database.Transaction.deleteVardaServiceNeedByVardaChildId(vardaChildId: Long) = createUpdate(
    """
DELETE FROM varda_service_need
WHERE varda_child_id = :vardaChildId
        """
).bind("vardaChildId", vardaChildId)
    .execute()

fun Database.Transaction.markVardaServiceNeedUpdateFailed(serviceNeedId: ServiceNeedId, errors: List<String>) = createUpdate(
    """
UPDATE varda_service_need
SET update_failed = true, errors = :errors
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .bind("errors", errors.toTypedArray())
    .execute()

private fun calculateDeletedChildServiceNeeds(db: Database.Connection): Map<UUID, List<ServiceNeedId>> {
    return db.read { it ->
        it.createQuery(
            """
SELECT evaka_child_id AS child_id, array_agg(evaka_service_need_id::uuid) AS service_need_ids
FROM varda_service_need
WHERE evaka_service_need_id NOT IN (
    SELECT id FROM service_need
)
GROUP BY evaka_child_id"""
        )
            .map { row -> row.mapColumn<UUID>("child_id") to row.mapColumn<Array<ServiceNeedId>>("service_need_ids").toList() }
            .toMap()
    }
}

data class VardaServiceNeed(
    val evakaChildId: UUID,
    val evakaServiceNeedId: ServiceNeedId,
    val evakaServiceNeedOptionId: ServiceNeedOptionId,
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

fun Database.Read.getVardaServiceNeedByEvakaServiceNeedId(eVakaServiceNeedId: ServiceNeedId): VardaServiceNeed? =
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

fun Database.Read.getAllChangedServiceNeedFeeDataSince(startingFrom: HelsinkiDateTime?): List<FeeDataByServiceNeed> {
    return getServiceNeedFeeDataQuery(startingFrom, null, null, null)
}

fun Database.Read.getServiceNeedFeeData(serviceNeedId: ServiceNeedId?, feeDecisionStatus: FeeDecisionStatus, voucherValueDecisionStatus: VoucherValueDecisionStatus): List<FeeDataByServiceNeed> {
    return getServiceNeedFeeDataQuery(null, serviceNeedId, feeDecisionStatus, voucherValueDecisionStatus)
}

private fun Database.Read.getServiceNeedFeeDataQuery(startingFrom: HelsinkiDateTime?, serviceNeedId: ServiceNeedId?, feeDecisionStatus: FeeDecisionStatus?, voucherValueDecisionStatus: VoucherValueDecisionStatus?): List<FeeDataByServiceNeed> =
    createQuery(
        """
WITH child_fees AS (
  SELECT
    fd.id AS fee_decision_id,
    fdc.child_id,
    fd.valid_during
  FROM fee_decision fd JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id 
  ${if (startingFrom != null) " WHERE fd.sent_at >= :startingFrom" else ""}
  ${if (feeDecisionStatus != null) " WHERE fd.status = :feeDecisionStatus" else ""}  
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
${if (voucherValueDecisionStatus != null) " WHERE vvd.status = :voucherValueDecisionStatus" else ""}  
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
        .bind("feeDecisionStatus", feeDecisionStatus)
        .bind("voucherValueDecisionStatus", voucherValueDecisionStatus)
        .mapTo<FeeDataByServiceNeed>()
        .list()

data class FeeDataByServiceNeed(
    val evakaChildId: UUID,
    val serviceNeedId: ServiceNeedId,
    val feeDecisionIds: List<FeeDecisionId> = emptyList(),
    val voucherValueDecisionIds: List<VoucherValueDecisionId> = emptyList()
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
    val id: ServiceNeedId,
    val optionId: ServiceNeedOptionId,
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
        unitOid = this.ophUnitOid ?: error("VardaUpdate: varda placement cannot be created for service need ${this.id}: unitOid cannot be null"),
        startDate = this.startDate,
        endDate = this.endDate,
        sourceSystem = sourceSystem
    )
}

fun Database.Read.getEvakaServiceNeedInfoForVarda(id: ServiceNeedId): EvakaServiceNeedInfoForVarda {
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
            sno.id AS option_id
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
    val evakaServiceNeedId: ServiceNeedId,
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

fun Database.Read.getVardaChildrenToReset(): List<UUID> =
    createQuery("SELECT evaka_child_id FROM varda_reset_child WHERE reset_timestamp IS NULL")
        .mapTo<UUID>()
        .list()

fun Database.Transaction.setVardaResetChildResetTimestamp(evakaChildId: UUID, resetTimestamp: Instant) = createUpdate(
    """
UPDATE varda_reset_child SET reset_timestamp = :resetTimestamp
WHERE evaka_child_id = :evakaChildId
        """
).bind("evakaChildId", evakaChildId)
    .bind("resetTimestamp", resetTimestamp)
    .execute()
