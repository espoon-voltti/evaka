// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VardaAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class VardaUpdateService(
    private val asyncJobRunner: AsyncJobRunner<VardaAsyncJob>,
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val mapper: ObjectMapper,
    private val vardaEnv: VardaEnv,
    private val evakaEnv: EvakaEnv
) {
    private val organizer = vardaEnv.organizer
    private val feeDecisionMinDate = evakaEnv.feeDecisionMinDate

    init {
        asyncJobRunner.registerHandler(::updateVardaChildByAsyncJob)
    }

    val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

    fun startVardaUpdate(db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

        logger.info("VardaUpdate: starting update process")

        updateOrganizer(db, client, organizer)
        updateUnits(db, client, organizer)

        planVardaChildrenUpdate(db)
    }

    /*
        1. Find all potentially changed service needs and the ones that have failed upload
            - For each deleted service need, delete all related data from varda
            - For each new service need, IF related fee data exists, add all related data to varda
            - For each modified service need, delete old related data from varda and add new
     */
    fun planVardaChildrenUpdate(db: Database.Connection) {
        val serviceNeedDiffsByChild = getChildrenToUpdate(db, feeDecisionMinDate)

        logger.info("VardaUpdate: will update ${serviceNeedDiffsByChild.entries.size} children")

        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                serviceNeedDiffsByChild.values.map {
                    VardaAsyncJob.UpdateVardaChild(it)
                },
                retryCount = 2,
                retryInterval = Duration.ofMinutes(10)
            )
        }
    }

    fun updateVardaChildByAsyncJob(db: Database.Connection, msg: VardaAsyncJob.UpdateVardaChild) {
        logger.info("VardaUpdate: starting to update child ${msg.serviceNeedDiffByChild.childId}")
        updateVardaChild(db, client, msg.serviceNeedDiffByChild, feeDecisionMinDate)
        logger.info("VardaUpdate: successfully updated child ${msg.serviceNeedDiffByChild.childId}")
    }
}

fun getChildrenToUpdate(db: Database.Connection, feeDecisionMinDate: LocalDate): Map<ChildId, VardaChildCalculatedServiceNeedChanges> {
    val includedChildIds = db.read { it.getSuccessfullyVardaResetEvakaChildIds() }
    val serviceNeedDiffsByChild = calculateEvakaVsVardaServiceNeedChangesByChild(db, feeDecisionMinDate)
        .filter { includedChildIds.contains(it.key) }
    return serviceNeedDiffsByChild
}

fun updateVardaChild(db: Database.Connection, client: VardaClient, serviceNeedDiffByChild: VardaChildCalculatedServiceNeedChanges, feeDecisionMinDate: LocalDate) {
    val evakaChildId = serviceNeedDiffByChild.childId
    try {
        serviceNeedDiffByChild.deletes.forEach { deletedServiceNeedId ->
            handleDeletedEvakaServiceNeed(db, client, deletedServiceNeedId)
        }

        serviceNeedDiffByChild.updates.forEach { updatedServiceNeedId ->
            handleUpdatedEvakaServiceNeed(db, client, updatedServiceNeedId, feeDecisionMinDate)
        }

        serviceNeedDiffByChild.additions.forEach { addedServiceNeedId ->
            handleNewEvakaServiceNeed(db, client, addedServiceNeedId, feeDecisionMinDate)
        }

        if (!db.read { it.hasVardaServiceNeeds(evakaChildId) }) {
            logger.info("VardaUpdate: deleting child for not having service needs ($evakaChildId)")
            deleteChildDataFromVardaAndDb(db, client, evakaChildId.raw)
        }
    } catch (e: Exception) {
        logger.info("VardaUpdate: failed to update child $evakaChildId: ${e.localizedMessage}")
    }
}

private fun handleDeletedEvakaServiceNeed(db: Database.Connection, client: VardaClient, evakaServiceNeedId: ServiceNeedId) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) }
            ?: error("VardaUpdate: cannot handle deleted service need $evakaServiceNeedId: varda service need missing")
        deleteServiceNeedDataFromVarda(client, vardaServiceNeed)
        db.transaction { it.deleteVardaServiceNeedByEvakaServiceNeed(evakaServiceNeedId) }
        logger.info("VardaUpdate: successfully deleted service need $evakaServiceNeedId")
    } catch (e: Exception) {
        db.transaction { it.markVardaServiceNeedUpdateFailed(evakaServiceNeedId, listOf(e.localizedMessage)) }
        logger.error("VardaUpdate: error while processing deleted service need $evakaServiceNeedId: ${e.localizedMessage}")
    }
}

private fun handleUpdatedEvakaServiceNeed(db: Database.Connection, client: VardaClient, evakaServiceNeedId: ServiceNeedId, feeDecisionMinDate: LocalDate) {
    try {
        val vardaServiceNeed = db.read { it.getVardaServiceNeedByEvakaServiceNeedId(evakaServiceNeedId) }
            ?: error("VardaUpdate: cannot handle updated service need $evakaServiceNeedId: varda service need missing")
        deleteServiceNeedDataFromVarda(client, vardaServiceNeed)
        val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(evakaServiceNeedId) }
        addServiceNeedDataToVarda(db, client, evakaServiceNeed, vardaServiceNeed, feeDecisionMinDate)
        logger.info("VardaUpdate: successfully updated service need $evakaServiceNeedId")
    } catch (e: Exception) {
        db.transaction { it.markVardaServiceNeedUpdateFailed(evakaServiceNeedId, listOf(e.localizedMessage)) }
        logger.error("VardaUpdate: error while processing updated service need $evakaServiceNeedId: ${e.localizedMessage}")
    }
}

fun handleNewEvakaServiceNeed(
    db: Database.Connection,
    client: VardaClient,
    evakaServiceNeedId: ServiceNeedId,
    feeDecisionMinDate: LocalDate
) {
    logger.info("VardaUpdate: creating a new service need from $evakaServiceNeedId")
    val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(evakaServiceNeedId) }
    val newVardaServiceNeed = evakaServiceNeed.toVardaServiceNeed()
    addServiceNeedDataToVarda(db, client, evakaServiceNeed, newVardaServiceNeed, feeDecisionMinDate)
    logger.info("VardaUpdate: successfully created new service need from $evakaServiceNeedId")
}

// Delete decision, placement and related fee data from Varda by stored id's
private fun deleteServiceNeedDataFromVarda(vardaClient: VardaClient, vardaServiceNeed: VardaServiceNeed) {
    vardaServiceNeed.vardaFeeDataIds.forEach { feeDataId ->
        vardaClient.deleteFeeData(feeDataId)
    }
    vardaServiceNeed.vardaPlacementId?.let { vardaClient.deletePlacement(it) }
    vardaServiceNeed.vardaDecisionId?.let { vardaClient.deleteDecision(it) }
}

// Add child if missing, service need, placement and mandatory fee decision(s)
private fun addServiceNeedDataToVarda(
    db: Database.Connection,
    vardaClient: VardaClient,
    evakaServiceNeed: EvakaServiceNeedInfoForVarda,
    vardaServiceNeed: VardaServiceNeed,
    feeDecisionMinDate: LocalDate
) {
    try {
        logger.info("VardaUpdate: adding service need ${evakaServiceNeed.id}")
        val serviceNeedFeeData = db.read { it.getServiceNeedFeeData(evakaServiceNeed.id, FeeDecisionStatus.SENT, VoucherValueDecisionStatus.SENT) }

        // Service need should have fee data if unit is invoiced by evaka, and if so, if service need is in effect after evaka started invoicing
        val shouldHaveFeeData = if (!db.read { it.serviceNeedIsInvoicedByMunicipality(evakaServiceNeed.id) }) false else
            !evakaServiceNeed.endDate.isBefore(feeDecisionMinDate)

        val hasFeeData: Boolean = serviceNeedFeeData.firstOrNull().let { it != null && it.hasFeeData() }

        if (evakaServiceNeed.hoursPerWeek < 1) logger.info("VardaUpdate: refusing to send service need ${evakaServiceNeed.id} because hours per week is ${evakaServiceNeed.hoursPerWeek} ")
        else if (shouldHaveFeeData && !hasFeeData) logger.info("VardaUpdate: refusing to send service need ${evakaServiceNeed.id} because mandatory fee data is missing")
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
        val errors = listOf("VardaUpdate: error adding service need ${evakaServiceNeed.id} to Varda: ${e.message}")
        db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed, errors) }
        error(errors)
    }
}

private fun sendDecisionToVarda(client: VardaClient, vardaChildId: Long, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    val res = client.createDecision(evakaServiceNeedInfoForVarda.toVardaDecisionForChild(client.getChildUrl(vardaChildId), client.sourceSystem))
    return res.vardaDecisionId
}

private fun sendPlacementToVarda(client: VardaClient, vardaDecisionId: Long, evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda): Long {
    val res = client.createPlacement(evakaServiceNeedInfoForVarda.toVardaPlacement(client.getDecisionUrl(vardaDecisionId), client.sourceSystem))
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

private fun sendFeeDataToVarda(vardaClient: VardaClient, db: Database.Connection, newVardaServiceNeed: VardaServiceNeed, evakaServiceNeed: EvakaServiceNeedInfoForVarda, feeDataByServiceNeed: FeeDataByServiceNeed): List<Long> {
    val guardians = getChildVardaGuardians(db, evakaServiceNeed.childId)

    if (guardians.isEmpty()) {
        logger.info("VardaUpdate: refusing to create fee data for service need ${evakaServiceNeed.id}: child has no guardians")
        return emptyList()
    }

    val feeResponseIds: List<Long> = feeDataByServiceNeed.feeDecisionIds.mapNotNull { feeId ->
        try {
            logger.info("VardaUpdate: trying to send fee data $feeId")
            val decision = db.read { it.getFeeDecisionsByIds(listOf(feeId)) }.first()

            // If head of family is not a guardian, fee data is not supposed to be sent to varda (https://wiki.eduuni.fi/display/OPHPALV/Huoltajan+tiedot)
            if (guardians.none { it.id == decision.headOfFamily.id }) {
                logger.info("VardaUpdate: refusing to send fee data for fee decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}")
                null
            } else sendFeeDecisionToVarda(
                client = vardaClient,
                decision = decision,
                vardaChildId = newVardaServiceNeed.vardaChildId!!,
                evakaServiceNeedInfoForVarda = evakaServiceNeed,
                guardians = guardiansResponsibleForFeeData(decision.headOfFamily.id, guardians)
            )
        } catch (e: Exception) {
            error("VardaUpdate: failed to send fee decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}")
        }
    }

    val voucherResponseIds = feeDataByServiceNeed.voucherValueDecisionIds.mapNotNull { feeId ->
        try {
            logger.info("VardaUpdate: trying to send voucher data $feeId")
            val decision = db.read { it.getVoucherValueDecision(feeId) }!!

            if (guardians.none { it.id == decision.headOfFamily.id }) {
                logger.info("VardaUpdate: refusing to send fee data for voucher decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}")
                null
            } else sendVoucherDecisionToVarda(
                client = vardaClient,
                decision = decision,
                vardaChildId = newVardaServiceNeed.vardaChildId!!,
                evakaServiceNeedInfoForVarda = evakaServiceNeed,
                guardians = guardiansResponsibleForFeeData(decision.headOfFamily.id, guardians)
            )
        } catch (e: Exception) {
            error("VardaUpdate: failed to send voucher decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}")
        }
    }

    return feeResponseIds + voucherResponseIds
}

private fun getChildVardaGuardians(db: Database.Connection, childId: ChildId): List<VardaGuardianWithId> {
    return db.read {
        it.createQuery(
            """
            SELECT
                id,
                first_name AS etunimet,
                last_name as sukunimi,
                social_security_number AS henkilotunnus,
                nullif(trim(oph_person_oid), '') AS henkilo_oid,
                residence_code AS asuinpaikantunnus
            FROM person 
            WHERE id IN (SELECT guardian_id FROM guardian WHERE child_id = :id)
            """.trimIndent()
        )
            .bind("id", childId)
            .mapTo<VardaGuardianWithId>()
            .list()
    }
}

private fun sendFeeDecisionToVarda(
    client: VardaClient,
    decision: FeeDecision,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    guardians: List<VardaGuardianWithId>
): Long {
    val childPart = decision.children.find { part -> part.child.id == evakaServiceNeedInfoForVarda.childId.raw }

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

    return client.createFeeData(requestPayload).id
}

private fun sendVoucherDecisionToVarda(
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

    return client.createFeeData(requestPayload).id
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

fun calculateEvakaVsVardaServiceNeedChangesByChild(db: Database.Connection, feeDecisionMinDate: LocalDate): Map<ChildId, VardaChildCalculatedServiceNeedChanges> {
    val evakaServiceNeedDeletionsByChild = db.read { it.calculateDeletedChildServiceNeeds() }

    val evakaServiceNeedChangesByChild = db.read { it.getEvakaServiceNeedChanges(feeDecisionMinDate) }
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

// Find out new varhaiskasvatuspaatos to be added for a child: any new service needs not found from child's full varda history
private fun calculateNewChildServiceNeeds(evakaServiceNeedChangesForChild: List<ChangedChildServiceNeed>, vardaChildServiceNeeds: List<VardaServiceNeed>): List<ServiceNeedId> {
    return evakaServiceNeedChangesForChild.filter { newServiceNeedChange ->
        vardaChildServiceNeeds.none { vardaChildServiceNeedChange ->
            vardaChildServiceNeedChange.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId
        }
    }.map {
        it.evakaServiceNeedId
    }
}

// Find out changed varhaiskasvatuspaatos for a child: any new service need with a different update timestamp in history
private fun calculateUpdatedChildServiceNeeds(evakaServiceNeedChangesForChild: List<ChangedChildServiceNeed>, vardaServiceNeedsForChild: List<VardaServiceNeed>): List<ServiceNeedId> {
    return evakaServiceNeedChangesForChild.mapNotNull { newServiceNeedChange ->
        vardaServiceNeedsForChild.find { it.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId }?.evakaServiceNeedId
    }
}
