// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.FeeDecisionId
import fi.espoo.evaka.shared.ServiceNeedId
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
    private val vardaEnv: VardaEnv,
    private val evakaEnv: EvakaEnv
) {
    private val organizer = vardaEnv.organizer
    private val feeDecisionMinDate = evakaEnv.feeDecisionMinDate

    init {
        asyncJobRunner.vardaUpdateV2 = ::updateAll
    }

    fun scheduleVardaUpdate(db: Database.Connection, runNow: Boolean = false) {
        if (runNow) {
            logger.info("VardaUpdate: running varda update immediately")
            val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)
            updateAllVardaData(db, client, organizer, feeDecisionMinDate)
        } else {
            logger.info("VardaUpdate: scheduling varda update")
            db.transaction { asyncJobRunner.plan(it, listOf(VardaUpdateV2()), retryCount = 1) }
        }
    }

    fun updateAll(db: Database, msg: VardaUpdateV2) {
        val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)
        db.connect { updateAllVardaData(it, client, organizer, feeDecisionMinDate) }
    }

    fun resetChildren(db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

        val resetChildIds = db.read { it.getVardaChildrenToReset() }
        logger.info("VardaUpdate: will reset ${resetChildIds.size} children")

        val successfulResets: List<UUID> = resetChildIds.foldIndexed(listOf()) { index, successfulResets, childId ->
            logger.info { "VardaUpdate: resetting child $childId (${index + 1}/${resetChildIds.size})" }
            if (deleteChildDataFromVardaAndDb(db, client, childId)) {
                try {
                    val childServiceNeeds = db.read { it.getServiceNeedsForVardaByChild(childId) }
                    logger.info("VardaUpdate: found ${childServiceNeeds.size} service needs for child $childId to be sent")
                    childServiceNeeds.forEachIndexed { idx, serviceNeedId ->
                        logger.info { "VardaUpdate: sending service need $serviceNeedId for child $childId (${idx + 1}/${childServiceNeeds.size})" }
                        if (!handleNewEvakaServiceNeed(db, client, serviceNeedId, feeDecisionMinDate))
                            error("VardaUpdate: failed to send service need for child $childId")
                    }
                    db.transaction { it.setVardaResetChildResetTimestamp(childId, Instant.now()) }
                    logger.info("VardaUpdate: successfully sent ${childServiceNeeds.size} service needs for $childId")
                    successfulResets.plus(childId)
                } catch (e: Exception) {
                    logger.warn("VardaUpdate: could not add service need for child $childId while doing reset - full reset will be retried next time: ${e.localizedMessage}")
                    successfulResets
                }
            } else successfulResets
        }

        logger.info("VardaUpdate: successfully reset ${successfulResets.size} children (failed ${resetChildIds.size - successfulResets.size} children)")
    }

    fun clearAllExistingVardaChildDataFromVarda(db: Database.Connection, vardaChildId: Long) {
        val vardaClient = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

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
    val vardaChildIds = getVardaChildIdsByEvakaChildId(db, evakaChildId)

    logger.info("VardaUpdate: deleting all varda data for evaka child $evakaChildId, varda child ids: ${vardaChildIds.joinToString(",")}")

    val successfulDeletes: List<Boolean> = vardaChildIds.map { vardaChildId ->
        try {
            logger.info { "VardaUpdate: deleting varda data for evaka child $evakaChildId, varda child $vardaChildId" }

            val decisionIds = vardaClient.getDecisionsByChild(vardaChildId)
            logger.info { "VardaUpdate: found ${decisionIds.size} decisions to be deleted for child $evakaChildId (varda id $vardaChildId)" }

            val placementIds = decisionIds.flatMap { vardaClient.getPlacementsByDecision(it) }
            logger.info { "VardaUpdate: found ${placementIds.size} placements to be deleted for child $evakaChildId (varda id $vardaChildId)" }

            val feeIds = vardaClient.getFeeDataByChild(vardaChildId)
            logger.info { "VardaUpdate: found ${feeIds.size} fee data to be deleted for child $evakaChildId (varda id $vardaChildId)" }

            feeIds.forEachIndexed { index, feeId ->
                logger.info { "VardaUpdate: deleting fee data $feeId for child $vardaChildId (${index + 1} / ${feeIds.size})" }
                if (vardaClient.deleteFeeDataV2(feeId)) {
                    logger.info { "VardaUpdate: deleting fee data from db for child $evakaChildId (varda id $vardaChildId) by id $feeId" }
                    db.transaction { deleteVardaFeeData(it, feeId) }
                }
            }

            placementIds.forEachIndexed { index, placementId ->
                logger.info { "VardaUpdate: deleting placement $placementId for child $vardaChildId (${index + 1} / ${placementIds.size})" }
                if (vardaClient.deletePlacementV2(placementId)) {
                    logger.info { "VardaUpdate: deleting placement data from db for child $evakaChildId (varda id $vardaChildId) by id $placementId" }
                    db.transaction { deletePlacement(it, placementId) }
                }
            }

            decisionIds.forEachIndexed { index, decisionId ->
                logger.info { "VardaUpdate: deleting decision $decisionId for child $vardaChildId (${index + 1} / ${decisionIds.size})" }
                if (vardaClient.deleteDecisionV2(decisionId)) {
                    logger.info { "VardaUpdate: deleting decision data from db for child $evakaChildId (varda id $vardaChildId) by id $decisionId" }
                    db.transaction { deleteDecision(it, decisionId) }
                }
            }

            logger.info { "VardaUpdate: deleting from varda_service_need for child $evakaChildId (varda id $vardaChildId)" }
            db.transaction { it.deleteVardaServiceNeedByVardaChildId(vardaChildId) }

            logger.info { "VardaUpdate: successfully deleted data for child $evakaChildId (varda id $vardaChildId)" }
            true
        } catch (e: Exception) {
            logger.warn("VardaUpdate: failed to delete varda data for child $evakaChildId (varda id $vardaChildId): ${e.localizedMessage}")
            false
        }
    }

    return successfulDeletes.all { it }
}

fun getVardaChildIdsByEvakaChildId(db: Database.Connection, evakaChildId: UUID): List<Long> {
    return db.read {
        it.createQuery(
            """
            select varda_child_id from varda_service_need where evaka_child_id = :evakaChildId and varda_child_id is not null 
            union
            select varda_child_id from varda_organizer_child where evaka_person_id = :evakaChildId
            union
            select varda_child_id from varda_child where person_id = :evakaChildId and varda_child_id is not null
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
    organizer: String,
    feeDecisionMinDate: LocalDate
) {
    val since = calculateStartingFrom(db)
    logger.info("VardaUpdate: running varda update for data modified since $since")
    updateOrganizer(db, client, organizer)
    updateUnits(db, client, organizer)
    updateChildData(db, client, since, feeDecisionMinDate)
}

private fun calculateStartingFrom(db: Database.Connection): HelsinkiDateTime {
    return db.read { it.createQuery("SELECT COALESCE(MAX(evaka_service_need_updated), now()) - INTERVAL '1 day' FROM varda_service_need") }
        .mapTo<HelsinkiDateTime>().one()
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
fun updateChildData(db: Database.Connection, client: VardaClient, startingFrom: HelsinkiDateTime, feeDecisionMinDate: LocalDate) {
    val processedServiceNeedIds = retryUnsuccessfulServiceNeedVardaUpdates(db, client, feeDecisionMinDate).toMutableSet()
    logger.info { "VardaUpdate: successfully processed ${processedServiceNeedIds.size} unsuccessful service needs" }

    // This is used to control which children are ready for the new varda update service
    // (e.g. old bogus info has been removed from varda and historical info has been successfully uploaded)
    val includedChildIds = db.read { it.getSuccessfullyVardaResetEvakaChildIds() }

    val serviceNeedDiffsByChild = calculateEvakaVsVardaServiceNeedChangesByChild(db, startingFrom)
        .filter { includedChildIds.contains(it.key) }

    val feeAndVoucherDiffsByServiceNeed = db.read { it.getAllChangedServiceNeedFeeDataSince(startingFrom) }
        .filter { includedChildIds.contains(it.evakaChildId) }
        .groupBy { it.serviceNeedId }

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
                handleUpdatedEvakaServiceNeed(db, client, updatedServiceNeedId, feeDecisionMinDate)
            }
        }

        serviceNeedDiffByChild.value.additions.forEach { addedServiceNeedId ->
            if (!processedServiceNeedIds.contains(addedServiceNeedId)) {
                processedServiceNeedIds.add(addedServiceNeedId)
                handleNewEvakaServiceNeed(db, client, addedServiceNeedId, feeDecisionMinDate)
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
            handleUpdatedEvakaServiceNeed(db, client, serviceNeedId, feeDecisionMinDate)
        } else {
            handleNewEvakaServiceNeed(db, client, serviceNeedId, feeDecisionMinDate)
        }
    }

    logger.info("VardaUpdate: processed ${processedServiceNeedIds.size} service needs")
}

fun handleDeletedEvakaServiceNeed(db: Database.Connection, client: VardaClient, evakaServiceNeedId: ServiceNeedId) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) }
            ?: error("VardaUpdate: cannot handle deleted service need $evakaServiceNeedId: varda service need missing")
        deleteServiceNeedDataFromVarda(client, vardaServiceNeed)
        db.transaction { it.deleteVardaServiceNeedByEvakaServiceNeed(evakaServiceNeedId) }
    } catch (e: Exception) {
        db.transaction { it.markVardaServiceNeedUpdateFailed(evakaServiceNeedId, listOf(e.localizedMessage)) }
        logger.error("VardaUpdate: error while processing deleted service need $evakaServiceNeedId: ${e.localizedMessage}")
    }
}

fun handleUpdatedEvakaServiceNeed(db: Database.Connection, client: VardaClient, evakaServiceNeedId: ServiceNeedId, feeDecisionMinDate: LocalDate) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) }
            ?: error("VardaUpdate: cannot handle updated service need $evakaServiceNeedId: varda service need missing")
        deleteServiceNeedDataFromVarda(client, vardaServiceNeed)
        val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(evakaServiceNeedId) }
        addServiceNeedDataToVarda(db, client, evakaServiceNeed, vardaServiceNeed, feeDecisionMinDate)
    } catch (e: Exception) {
        db.transaction { it.markVardaServiceNeedUpdateFailed(evakaServiceNeedId, listOf(e.localizedMessage)) }
        logger.error("VardaUpdate: error while processing updated service need $evakaServiceNeedId: ${e.localizedMessage}")
    }
}

fun handleNewEvakaServiceNeed(db: Database.Connection, client: VardaClient, serviceNeedId: ServiceNeedId, feeDecisionMinDate: LocalDate): Boolean {
    try {
        val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(serviceNeedId) }
        val newVardaServiceNeed = evakaServiceNeed.toVardaServiceNeed()
        addServiceNeedDataToVarda(db, client, evakaServiceNeed, newVardaServiceNeed, feeDecisionMinDate)
    } catch (e: Exception) {
        logger.error("VardaUpdate: error while processing new service need $serviceNeedId data: ${e.localizedMessage}")
        return false
    }

    return true
}

fun retryUnsuccessfulServiceNeedVardaUpdates(db: Database.Connection, vardaClient: VardaClient, feeDecisionMinDate: LocalDate): List<ServiceNeedId> {
    val unsuccessfullyUploadedServiceNeeds = db.read { it.getUnsuccessfullyUploadVardaServiceNeeds() }

    if (unsuccessfullyUploadedServiceNeeds.isNotEmpty())
        logger.info("VardaUpdate: retrying failed varda uploads: ${unsuccessfullyUploadedServiceNeeds.size}")

    return unsuccessfullyUploadedServiceNeeds.map {
        try {
            if (it.existsInEvaka && it.childId != null)
                handleUpdatedEvakaServiceNeed(db, vardaClient, it.evakaServiceNeedId, feeDecisionMinDate)
            else
                handleDeletedEvakaServiceNeed(db, vardaClient, it.evakaServiceNeedId)
            logger.info("VardaUpdate: successfully processed unsuccessful service need ${it.evakaServiceNeedId}")
        } catch (e: Exception) {
            logger.error("VardaUpdate: got an error while processing an unsuccessful service need: ${e.localizedMessage}")
        }
        it.evakaServiceNeedId
    }
}

// Delete decision, placement and related fee data from Varda by stored id's
fun deleteServiceNeedDataFromVarda(vardaClient: VardaClient, vardaServiceNeed: VardaServiceNeed) {
    vardaServiceNeed.vardaFeeDataIds.forEach { feeDataId ->
        vardaClient.deleteFeeDataV2(feeDataId)
    }
    vardaServiceNeed.vardaPlacementId?.let { vardaClient.deletePlacementV2(it) }
    vardaServiceNeed.vardaDecisionId?.let { vardaClient.deleteDecisionV2(it) }
}

// Add child if missing, service need, placement and mandatory fee decision(s)
fun addServiceNeedDataToVarda(db: Database.Connection, vardaClient: VardaClient, evakaServiceNeed: EvakaServiceNeedInfoForVarda, vardaServiceNeed: VardaServiceNeed, feeDecisionMinDate: LocalDate) {
    try {
        // Todo: "nettopalvelu"-unit children do not have fee data
        val serviceNeedFeeData = db.read { it.getServiceNeedFeeData(evakaServiceNeed.id, FeeDecisionStatus.SENT, VoucherValueDecisionStatus.SENT) }

        // Service need should have fee data if unit is invoiced by evaka, and if so, if service need is in effect after evaka started invoicing
        val shouldHaveFeeData = if (!db.read { it.serviceNeedIsInvoicedByMunicipality(evakaServiceNeed.id) }) false else
            !evakaServiceNeed.endDate.isBefore(feeDecisionMinDate)

        val hasFeeData: Boolean = serviceNeedFeeData.firstOrNull().let { it != null && it.hasFeeData() }

        if (shouldHaveFeeData && !hasFeeData) logger.info("VardaUpdate: refusing to send service need ${evakaServiceNeed.id} because mandatory fee data is missing")
        else {
            check(!evakaServiceNeed.ophOrganizerOid.isNullOrBlank()) {
                "VardaUpdate: service need daycare oph_organizer_oid is null or blank"
            }
            check(!evakaServiceNeed.ophUnitOid.isNullOrBlank()) {
                "VardaUpdate: service need daycare oph unit oid is null or blank"
            }

            vardaServiceNeed.evakaServiceNeedUpdated = HelsinkiDateTime.from(evakaServiceNeed.serviceNeedUpdated)

            val vardaChildId = getOrCreateVardaChildByOrganizer(db, vardaClient, evakaServiceNeed.childId, evakaServiceNeed.ophOrganizerOid, vardaClient.sourceSystem)
            vardaServiceNeed.vardaChildId = vardaChildId

            val vardaDecisionId = sendDecisionToVarda(vardaClient, vardaChildId, evakaServiceNeed)
            vardaServiceNeed.vardaDecisionId = vardaDecisionId

            val vardaPlacementId = sendPlacementToVarda(vardaClient, vardaDecisionId, evakaServiceNeed)
            vardaServiceNeed.vardaPlacementId = vardaPlacementId

            if (hasFeeData) vardaServiceNeed.vardaFeeDataIds = sendFeeDataToVarda(vardaClient, db, vardaServiceNeed, evakaServiceNeed, serviceNeedFeeData.first())
            db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed) }
        }
    } catch (e: Exception) {
        val errors = listOf("VardaUpdate: error adding service need ${evakaServiceNeed.id} to Varda: ${e.localizedMessage}")
        db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed, errors) }
        error(errors)
    }
}

fun sendDecisionToVarda(client: VardaClient, vardaChildId: Long, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    val res = client.createDecisionV2(evakaServiceNeedInfoForVarda.toVardaDecisionForChild(client.getChildUrl(vardaChildId), client.sourceSystem))
    return res.vardaDecisionId
}

fun sendPlacementToVarda(client: VardaClient, vardaDecisionId: Long, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    val res = client.createPlacementV2(evakaServiceNeedInfoForVarda.toVardaPlacement(client.getDecisionUrl(vardaDecisionId), client.sourceSystem))
    return res.vardaPlacementId
}

private fun guardiansLiveInSameAddress(guardians: List<VardaGuardianWithId>): Boolean {
    val validResidenceCodes = guardians.map { g -> g.asuinpaikantunnus }
        .filterNot { it.isNullOrBlank() }
    return validResidenceCodes.size == guardians.size && validResidenceCodes.toSet().size == 1
}

private fun guardiansResponsibleForFeeData(decisionHeadOfFamilyId: UUID, allGuardians: List<VardaGuardianWithId>): List<VardaGuardianWithId> {
    return if (guardiansLiveInSameAddress(allGuardians)) allGuardians else allGuardians.filter { guardian -> guardian.id == decisionHeadOfFamilyId }
}

fun sendFeeDataToVarda(vardaClient: VardaClient, db: Database.Connection, newVardaServiceNeed: VardaServiceNeed, evakaServiceNeed: EvakaServiceNeedInfoForVarda, feeDataByServiceNeed: FeeDataByServiceNeed): List<Long> {
    val guardians = getChildVardaGuardians(db, evakaServiceNeed.childId)

    if (guardians.isEmpty()) {
        logger.info("VardaUpdate: refusing to create fee data for service need ${evakaServiceNeed.id}: child has no guardians")
        return emptyList()
    }

    val feeResponseIds: List<Long> = feeDataByServiceNeed.feeDecisionIds.mapNotNull { feeId ->
        try {
            logger.info { "VardaUpdate: trying to send fee data $feeId" }
            val decision = db.read { it.getFeeDecisionsByIds(listOf(feeId)) }.first()

            // If head of family is not a guardian, fee data is not supposed to be sent to varda (https://wiki.eduuni.fi/display/OPHPALV/Huoltajan+tiedot)
            if (guardians.none { it.id == decision.headOfFamily.id }) {
                logger.info { "VardaUpdate: refusing to send fee data for fee decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}" }
                null
            } else sendFeeDecisionToVarda(
                client = vardaClient,
                decision = decision,
                vardaChildId = newVardaServiceNeed.vardaChildId!!,
                evakaServiceNeedInfoForVarda = evakaServiceNeed,
                guardians = guardiansResponsibleForFeeData(decision.headOfFamily.id, guardians)
            )
        } catch (e: Exception) {
            error { "VardaUpdate: failed to send fee decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}" }
        }
    }

    val voucherResponseIds = feeDataByServiceNeed.voucherValueDecisionIds.mapNotNull { feeId ->
        try {
            logger.info { "VardaUpdate: trying to send voucher data $feeId" }
            val decision = db.read { it.getVoucherValueDecision(feeId) }!!

            if (guardians.none { it.id == decision.headOfFamily.id }) {
                logger.info { "VardaUpdate: refusing to send fee data for voucher decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}" }
                null
            } else sendVoucherDecisionToVarda(
                client = vardaClient,
                decision = decision,
                vardaChildId = newVardaServiceNeed.vardaChildId!!,
                evakaServiceNeedInfoForVarda = evakaServiceNeed,
                guardians = guardiansResponsibleForFeeData(decision.headOfFamily.id, guardians)
            )
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

fun sendFeeDecisionToVarda(
    client: VardaClient,
    decision: FeeDecision,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    guardians: List<VardaGuardianWithId>
): Long {
    val childPart = decision.children.find { part -> part.child.id == evakaServiceNeedInfoForVarda.childId }

    check(childPart != null) {
        "VardaUpdate: fee decision ${decision.id} has no part for child ${evakaServiceNeedInfoForVarda.childId}, service need ${evakaServiceNeedInfoForVarda.id}"
    }

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

    return client.createFeeDataV2(requestPayload).id
}

fun sendVoucherDecisionToVarda(
    client: VardaClient,
    decision: VoucherValueDecisionDetailed,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    guardians: List<VardaGuardianWithId>
): Long {
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

    return client.createFeeDataV2(requestPayload).id
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
        match != null && newServiceNeedChange.evakaServiceNeedUpdated != match.evakaServiceNeedUpdated
    }.map {
        it.evakaServiceNeedId
    }
}

private fun deleteServiceNeedAndRelatedDataFromVarda(vardaClient: VardaClient, vardaServiceNeed: VardaServiceNeed) {
    vardaServiceNeed.vardaFeeDataIds.forEach { feeDataId ->
        vardaClient.deleteFeeDataV2(feeDataId)
    }
    vardaServiceNeed.vardaPlacementId?.let { vardaClient.deletePlacementV2(it) }
    vardaServiceNeed.vardaDecisionId?.let { vardaClient.deleteDecision(it) }
}

fun Database.Transaction.upsertVardaServiceNeed(vardaServiceNeed: VardaServiceNeed, upsertErrors: List<String> = listOf()) = createUpdate(
    """
INSERT INTO varda_service_need (
    evaka_service_need_id, 
    evaka_service_need_updated, 
    evaka_child_id, 
    varda_child_id, 
    varda_decision_id, 
    varda_placement_id, 
    varda_fee_data_ids, 
    update_failed, 
    errors) 
VALUES (
    :evakaServiceNeedId, 
    :evakaServiceNeedUpdated, 
    :evakaChildId, 
    :vardaChildId, 
    :vardaDecisionId, 
    :vardaPlacementId, 
    :vardaFeeDataIds, 
    :errorsNotEmpty, 
    :upsertErrors
) ON CONFLICT (evaka_service_need_id) DO UPDATE 
    SET evaka_service_need_updated = :evakaServiceNeedUpdated, 
        evaka_child_id = :evakaChildId, 
        varda_child_id = :vardaChildId,
        varda_decision_id = :vardaDecisionId, 
        varda_placement_id = :vardaPlacementId, 
        varda_fee_data_ids = :vardaFeeDataIds,
        update_failed = :errorsNotEmpty,
        errors = :upsertErrors
"""
).bindKotlin(vardaServiceNeed)
    .bind("upsertErrors", upsertErrors.toTypedArray())
    .bind("errorsNotEmpty", upsertErrors.isNotEmpty())
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
    var evakaServiceNeedUpdated: HelsinkiDateTime? = null,
    var vardaChildId: Long? = null,
    var vardaDecisionId: Long? = null,
    var vardaPlacementId: Long? = null,
    var vardaFeeDataIds: List<Long> = listOf(),
    var updateFailed: Boolean = false,
    val errors: MutableList<String> = mutableListOf()
)

fun Database.Read.getEvakaServiceNeedChanges(startingFrom: HelsinkiDateTime): List<VardaServiceNeed> =
    createQuery(
        """
SELECT 
    placement.child_id AS evakaChildId,
    sn.id AS evakaServiceNeedId,
    sn.updated AS evakaServiceNeedUpdated
FROM service_need sn
LEFT JOIN service_need_option sno ON sn.option_id = sno.id
LEFT JOIN placement ON sn.placement_id = placement.id
LEFT JOIN daycare ON daycare.id = placement.unit_id
LEFT JOIN varda_service_need vsn ON vsn.evaka_service_need_id = sn.id
WHERE sn.updated >= :startingFrom
AND sno.daycare_hours_per_week > 0
AND placement.type = ANY(:vardaPlacementTypes::placement_type[])
AND daycare.upload_children_to_varda = true
AND (vsn.evaka_service_need_updated IS NULL OR sn.updated > vsn.evaka_service_need_updated)
"""
    )
        .bind("startingFrom", startingFrom)
        .bind("vardaPlacementTypes", vardaPlacementTypes)
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
    JOIN daycare d ON d.id = p.unit_id
    JOIN child_fees ON p.child_id = child_fees.child_id 
      AND child_fees.valid_during && daterange(sn.start_date, sn.end_date, '[]')
  WHERE d.upload_children_to_varda = true AND d.invoiced_by_municipality = true
  GROUP BY service_need_id, p.child_id
), service_need_vouchers AS (
SELECT
    sn.id AS service_need_id,
    p.child_id AS child_id,
    array_agg(vvd.id) AS voucher_value_decision_ids
FROM service_need sn JOIN placement p ON p.id = sn.placement_id
  JOIN daycare d ON d.id = p.unit_id  
  JOIN voucher_value_decision vvd ON p.child_id = vvd.child_id 
    AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange(sn.start_date, sn.end_date, '[]')
WHERE d.upload_children_to_varda = true AND d.invoiced_by_municipality = true
${if (startingFrom != null) " AND vvd.sent_at >= :startingFrom" else ""}      
${if (voucherValueDecisionStatus != null) " AND vvd.status = :voucherValueDecisionStatus" else ""}  
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
) {
    fun hasFeeData() = feeDecisionIds.isNotEmpty() || voucherValueDecisionIds.isNotEmpty()
}

data class EvakaServiceNeedInfoForVarda(
    val id: ServiceNeedId,
    val serviceNeedUpdated: Instant,
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

    fun toVardaServiceNeed(): VardaServiceNeed =
        VardaServiceNeed(
            evakaChildId = this.childId,
            evakaServiceNeedId = this.id,
            evakaServiceNeedUpdated = HelsinkiDateTime.from(this.serviceNeedUpdated)
        )
}

fun Database.Read.getEvakaServiceNeedInfoForVarda(id: ServiceNeedId): EvakaServiceNeedInfoForVarda {
    // language=sql
    val sql = """
        SELECT
            sn.id,
            p.child_id AS child_id,
            sn.start_date,
            sn.end_date,
            LEAST(COALESCE(application_match.sentdate, application_match.created::date), sn.start_date) AS application_date,            
            COALESCE((af.document ->> 'urgent') :: BOOLEAN, false) AS urgent,
            sno.daycare_hours_per_week AS hours_per_week,
            CASE 
                WHEN sno.valid_placement_type = ANY(:vardaTemporaryPlacementTypes::placement_type[]) THEN true
                ELSE false
            END AS temporary,
            NOT(sno.part_week) AS daily,
            sn.shift_care,
            d.provider_type,
            d.oph_organizer_oid,
            d.oph_unit_oid,
            sn.updated AS service_need_updated
        FROM service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN employee e on e.id = sn.confirmed_by
        JOIN placement p ON p.id = sn.placement_id
        JOIN daycare d ON p.unit_id = d.id
        LEFT JOIN LATERAL (
            SELECT a.id, a.sentdate, a.created
            FROM application a
            WHERE child_id = p.child_id
              AND a.status IN ('ACTIVE')
              AND EXISTS (
                    SELECT 1
                    FROM placement_plan pp
                    WHERE pp.unit_id = p.unit_id AND pp.application_id = a.id
                      AND daterange(pp.start_date, pp.end_date, '[]') && daterange(sn.start_date, sn.end_date, '[]')
                      AND unit_confirmation_status = 'ACCEPTED'
                )
            ORDER BY a.sentdate, a.id
            LIMIT 1
            ) application_match ON true
        LEFT JOIN application_form af ON af.application_id = application_match.id AND af.latest IS TRUE        
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bind("vardaTemporaryPlacementTypes", vardaTemporaryPlacementTypes)
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

fun Database.Read.serviceNeedIsInvoicedByMunicipality(serviceNeedId: ServiceNeedId): Boolean = createQuery(
    """
            SELECT true
            FROM service_need sn 
                LEFT JOIN placement p ON sn.placement_id = p.id
                LEFT JOIN daycare d ON d.id = p.unit_id
            WHERE
                sn.id = :serviceNeedId 
                AND d.invoiced_by_municipality = true
    """.trimIndent()
)
    .bind("serviceNeedId", serviceNeedId)
    .mapTo<Boolean>()
    .list().isNotEmpty()

fun Database.Read.getServiceNeedsForVardaByChild(
    childId: UUID
): List<ServiceNeedId> {
    // language=SQL
    val sql =
        """
        SELECT sn.id
        FROM service_need sn
        JOIN placement pl ON pl.id = sn.placement_id
        JOIN daycare d ON d.id = pl.unit_id
        WHERE pl.child_id = :childId
        AND pl.type = ANY(:vardaPlacementTypes::placement_type[])
        AND d.upload_children_to_varda = true
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("vardaPlacementTypes", vardaPlacementTypes)
        .mapTo<ServiceNeedId>()
        .toList()
}

fun Database.Read.getSuccessfullyVardaResetEvakaChildIds(): List<UUID> = createQuery("SELECT evaka_child_id FROM varda_reset_child WHERE reset_timestamp IS NOT NULL")
    .mapTo<UUID>()
    .list()
