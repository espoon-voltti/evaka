// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.invoicing.data.getFeeDecisionsByIds
import fi.espoo.evaka.invoicing.data.getVoucherValueDecision
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionDetailed
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.minEndDate
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VardaService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val mapper: JsonMapper,
    private val vardaEnv: VardaEnv,
    private val evakaEnv: EvakaEnv,
    private val ophEnv: OphEnv
) {
    private val feeDecisionMinDate = evakaEnv.feeDecisionMinDate
    private val ophMunicipalityCode = ophEnv.municipalityCode
    private val municipalOrganizerOid = ophEnv.organizerOid
    private val ophMunicipalOrganizerIdUrl =
        "${vardaEnv.url}/v1/vakajarjestajat/$municipalOrganizerOid/"

    val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

    init {
        asyncJobRunner.registerHandler(::resetVardaChildByAsyncJob)
        asyncJobRunner.registerHandler(::deleteVardaChildByAsyncJob)
    }

    fun startVardaUpdate(db: Database.Connection, clock: EvakaClock) {
        val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

        logger.info("VardaService: starting update process")

        updateUnits(db, clock, client, ophMunicipalityCode, ophMunicipalOrganizerIdUrl)

        planVardaUpdate(db, clock)
    }

    fun planVardaUpdate(db: Database.Connection, clock: EvakaClock) {
        // Children with changes in service need data
        val serviceNeedDiffsByChild = getChildrenToUpdate(db, clock, feeDecisionMinDate)
        // Children already marked to be reset to varda AND possible new children who have started
        // in evaka
        val existingOrNewChildsToBeReset =
            db.transaction { it.getVardaChildrenToReset(limit = 1000, addNewChildren = true) }

        val allChildrenToUpdate =
            serviceNeedDiffsByChild.keys.toSet() + existingOrNewChildsToBeReset.toSet()

        logger.info("VardaService: will update ${allChildrenToUpdate.size} children")

        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                allChildrenToUpdate.map { AsyncJob.ResetVardaChild(it) },
                runAt = clock.now(),
                retryCount = 2,
                retryInterval = Duration.ofMinutes(10)
            )
        }
    }

    fun resetVardaChildByAsyncJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.ResetVardaChild
    ) {
        logger.info("VardaService: starting to reset child ${msg.childId}")
        resetVardaChild(db, clock, client, msg.childId, feeDecisionMinDate, municipalOrganizerOid)
    }

    fun deleteVardaChildByAsyncJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.DeleteVardaChild
    ) {
        logger.info("VardaService: starting to delete child ${msg.vardaChildId}")
        deleteChildDataFromVardaAndDbByVardaId(db, client, msg.vardaChildId)
    }
}

fun getChildrenToUpdate(
    db: Database.Connection,
    clock: EvakaClock,
    feeDecisionMinDate: LocalDate
): Map<ChildId, VardaChildCalculatedServiceNeedChanges> {
    val includedChildIds = db.read { it.getSuccessfullyVardaResetEvakaChildIds() }
    val serviceNeedDiffsByChild =
        calculateEvakaVsVardaServiceNeedChangesByChild(db, clock, feeDecisionMinDate).filter {
            includedChildIds.contains(it.key)
        }
    return serviceNeedDiffsByChild
}

fun resetVardaChild(
    db: Database.Connection,
    clock: EvakaClock,
    client: VardaClient,
    childId: ChildId,
    feeDecisionMinDate: LocalDate,
    municipalOrganizerOid: String
) {
    if (deleteChildDataFromVardaAndDb(db, client, childId)) {
        try {
            val childServiceNeeds = db.read { it.getServiceNeedsForVardaByChild(clock, childId) }
            logger.info(
                "VardaService: found ${childServiceNeeds.size} service needs for child $childId to be sent"
            )
            childServiceNeeds.forEachIndexed { idx, serviceNeedId ->
                logger.info(
                    "VardaService: sending service need $serviceNeedId for child $childId (${idx + 1}/${childServiceNeeds.size})"
                )
                val evakaServiceNeed = db.read { it.getEvakaServiceNeedInfoForVarda(serviceNeedId) }
                val newVardaServiceNeed = evakaServiceNeed.toVardaServiceNeed()
                addServiceNeedDataToVarda(
                    db,
                    client,
                    evakaServiceNeed,
                    newVardaServiceNeed,
                    feeDecisionMinDate,
                    municipalOrganizerOid
                )
            }
            db.transaction { it.setVardaResetChildResetTimestamp(childId, Instant.now()) }
            logger.info(
                "VardaService: successfully sent ${childServiceNeeds.size} service needs for $childId"
            )
        } catch (e: Exception) {
            logger.warn("VardaService: failed to reset child $childId: ${e.message}", e)
        }
    }
}

fun deleteChildDataFromVardaAndDb(
    db: Database.Connection,
    vardaClient: VardaClient,
    evakaChildId: ChildId
): Boolean {
    val vardaChildIds = getVardaChildIdsByEvakaChildId(db, evakaChildId)

    logger.info(
        "VardaService: deleting all varda data for evaka child $evakaChildId, varda child ids: ${vardaChildIds.joinToString(",")}"
    )

    val successfulDeletes: List<Boolean> =
        vardaChildIds.map { vardaChildId ->
            try {
                deleteChildDataFromVardaAndDbByVardaId(db, vardaClient, vardaChildId)
                true
            } catch (e: Exception) {
                logger.warn(
                    "VardaService: failed to delete varda data for child $evakaChildId (varda id $vardaChildId): ${e.localizedMessage}",
                    e
                )
                false
            }
        }

    return successfulDeletes.all { it }
}

fun deleteChildDataFromVardaAndDbByVardaId(
    db: Database.Connection,
    vardaClient: VardaClient,
    vardaChildId: Long
) {
    logger.info("VardaService: deleting all child data from varda (child id: $vardaChildId)")
    try {
        vardaClient.deleteChildAllData(vardaChildId)
    } catch (e: Exception) {
        // MI015 = unknown child so delete can be considered being a success
        if (e.message?.contains("MI015") != true) throw e
    }

    db.transaction { it.deleteVardaOrganizerChildByVardaChildId(vardaChildId) }

    logger.info("VardaService: deleting from varda_service_need for child $vardaChildId")
    db.transaction { it.deleteVardaServiceNeedByVardaChildId(vardaChildId) }

    logger.info("VardaService: successfully deleted data for child $vardaChildId")
}

private fun getVardaChildIdsByEvakaChildId(
    db: Database.Connection,
    evakaChildId: ChildId
): List<Long> {
    return db.read {
        @Suppress("DEPRECATION")
        it.createQuery(
                """
            select varda_child_id from varda_service_need where evaka_child_id = :evakaChildId and varda_child_id is not null 
            union
            select varda_child_id from varda_organizer_child where evaka_person_id = :evakaChildId
            """
                    .trimIndent()
            )
            .bind("evakaChildId", evakaChildId)
            .toList<Long>()
    }
}

// Add child if missing, service need, placement and mandatory fee decision(s)
private fun addServiceNeedDataToVarda(
    db: Database.Connection,
    vardaClient: VardaClient,
    evakaServiceNeed: EvakaServiceNeedInfoForVarda,
    vardaServiceNeed: VardaServiceNeed,
    feeDecisionMinDate: LocalDate,
    municipalOrganizerOid: String
) {
    try {
        logger.info("VardaUpdate: adding service need ${evakaServiceNeed.id}")
        val serviceNeedFeeData =
            db.read {
                it.getServiceNeedFeeData(
                    evakaServiceNeed.id,
                    FeeDecisionStatus.SENT,
                    VoucherValueDecisionStatus.SENT
                )
            }

        // Service need should have fee data if unit is invoiced by evaka, and if so, if service
        // need is in effect after evaka started invoicing
        val shouldHaveFeeData =
            if (!db.read { it.serviceNeedIsInvoicedByMunicipality(evakaServiceNeed.id) }) {
                false
            } else {
                !evakaServiceNeed.endDate.isBefore(feeDecisionMinDate)
            }

        val hasFeeData: Boolean =
            serviceNeedFeeData.firstOrNull().let { it != null && it.hasFeeData() }

        if (evakaServiceNeed.hoursPerWeek < 1) {
            logger.info(
                "VardaUpdate: refusing to send service need ${evakaServiceNeed.id} because hours per week is ${evakaServiceNeed.hoursPerWeek} "
            )
        } else if (shouldHaveFeeData && !hasFeeData) {
            logger.info(
                "VardaUpdate: refusing to send service need ${evakaServiceNeed.id} because mandatory fee data is missing"
            )
        } else {
            check(!evakaServiceNeed.ophOrganizerOid.isNullOrBlank()) {
                "VardaUpdate: service need daycare oph_organizer_oid is null or blank"
            }
            check(!evakaServiceNeed.ophUnitOid.isNullOrBlank()) {
                "VardaUpdate: service need daycare oph unit oid is null or blank"
            }

            vardaServiceNeed.evakaServiceNeedUpdated =
                HelsinkiDateTime.from(evakaServiceNeed.serviceNeedUpdated)

            val vardaChildId =
                getOrCreateVardaChildByOrganizer(
                    db,
                    vardaClient,
                    evakaServiceNeed.childId,
                    evakaServiceNeed.ophOrganizerOid,
                    vardaClient.sourceSystem,
                    municipalOrganizerOid
                )
            vardaServiceNeed.vardaChildId = vardaChildId

            val vardaDecisionId = sendDecisionToVarda(vardaClient, vardaChildId, evakaServiceNeed)
            vardaServiceNeed.vardaDecisionId = vardaDecisionId

            val vardaPlacementId =
                sendPlacementToVarda(vardaClient, vardaDecisionId, evakaServiceNeed)
            vardaServiceNeed.vardaPlacementId = vardaPlacementId

            if (hasFeeData)
                vardaServiceNeed.vardaFeeDataIds =
                    sendFeeDataToVarda(
                        vardaClient,
                        db,
                        vardaServiceNeed,
                        evakaServiceNeed,
                        serviceNeedFeeData.first()
                    )
            db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed) }
            logger.info(
                "VardaService: successfully created new service need from ${evakaServiceNeed.id}"
            )
        }
    } catch (e: Exception) {
        val errors =
            listOf(
                "VardaUpdate: error adding service need ${evakaServiceNeed.id} to Varda: ${e.message}"
            )
        db.transaction { it.upsertVardaServiceNeed(vardaServiceNeed, errors) }
        // MA003: varda needs a moment before retry
        // PT010: service need belongs to externally managed PAOS unit, we have no right to update
        // PO003: There is no active PaosOikeus
        val skipErrorCodes = listOf("MA003", "PT010", "PO003")

        if (skipErrorCodes.any { skipError -> e.message?.contains(skipError) == true }) {
            logger.info(
                "VardaUpdate: service need ${evakaServiceNeed.id} error response contained non critical error so continuing"
            )
        } else error(errors) // Fatal error
    }
}

private fun sendDecisionToVarda(
    client: VardaClient,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda
): Long {
    val res =
        client.createDecision(
            evakaServiceNeedInfoForVarda.toVardaDecisionForChild(
                client.getChildUrl(vardaChildId),
                client.sourceSystem
            )
        )
    return res.vardaDecisionId
}

private fun sendPlacementToVarda(
    client: VardaClient,
    vardaDecisionId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda
): Long {
    val res =
        client.createPlacement(
            evakaServiceNeedInfoForVarda.toVardaPlacement(
                client.getDecisionUrl(vardaDecisionId),
                client.sourceSystem
            )
        )
    return res.vardaPlacementId
}

private fun guardiansLiveInSameAddress(guardians: List<VardaGuardianWithId>): Boolean {
    val validResidenceCodes =
        guardians.map { g -> g.asuinpaikantunnus }.filterNot { it.isNullOrBlank() }
    return validResidenceCodes.size == guardians.size && validResidenceCodes.toSet().size == 1
}

private fun guardiansResponsibleForFeeData(
    decisionHeadOfFamilyId: PersonId,
    allGuardians: List<VardaGuardianWithId>
): List<VardaGuardianWithId> {
    return if (guardiansLiveInSameAddress(allGuardians)) allGuardians
    else allGuardians.filter { guardian -> guardian.id == decisionHeadOfFamilyId }
}

private fun sendFeeDataToVarda(
    vardaClient: VardaClient,
    db: Database.Connection,
    newVardaServiceNeed: VardaServiceNeed,
    evakaServiceNeed: EvakaServiceNeedInfoForVarda,
    feeDataByServiceNeed: FeeDataByServiceNeed
): List<Long> {
    val guardians = getChildVardaGuardians(db, evakaServiceNeed.childId)

    if (guardians.isEmpty()) {
        logger.info(
            "VardaUpdate: refusing to create fee data for service need ${evakaServiceNeed.id}: child has no guardians"
        )
        return emptyList()
    }

    val feeResponseIds: List<Long> =
        feeDataByServiceNeed.feeDecisionIds.mapNotNull { feeId ->
            try {
                logger.info("VardaUpdate: trying to send fee data $feeId")
                val decision = db.read { it.getFeeDecisionsByIds(listOf(feeId)) }.first()

                // If head of family is not a guardian, fee data is not supposed to be sent to varda
                // (https://wiki.eduuni.fi/display/OPHPALV/Huoltajan+tiedot)
                if (guardians.none { it.id == decision.headOfFamilyId }) {
                    logger.info(
                        "VardaUpdate: refusing to send fee data for fee decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}"
                    )
                    null
                } else {
                    sendFeeDecisionToVarda(
                        client = vardaClient,
                        decision = decision,
                        vardaChildId = newVardaServiceNeed.vardaChildId!!,
                        evakaServiceNeedInfoForVarda = evakaServiceNeed,
                        guardians =
                            guardiansResponsibleForFeeData(decision.headOfFamilyId, guardians)
                    )
                }
            } catch (e: Exception) {
                error(
                    "VardaUpdate: failed to send fee decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}"
                )
            }
        }

    val voucherResponseIds =
        feeDataByServiceNeed.voucherValueDecisionIds.mapNotNull { feeId ->
            try {
                logger.info("VardaUpdate: trying to send voucher data $feeId")
                val decision = db.read { it.getVoucherValueDecision(feeId) }!!

                if (guardians.none { it.id == decision.headOfFamily.id }) {
                    logger.info(
                        "VardaUpdate: refusing to send fee data for voucher decision $feeId - head of family is not a guardian of ${evakaServiceNeed.childId}"
                    )
                    null
                } else {
                    sendVoucherDecisionToVarda(
                        client = vardaClient,
                        decision = decision,
                        vardaChildId = newVardaServiceNeed.vardaChildId!!,
                        evakaServiceNeedInfoForVarda = evakaServiceNeed,
                        guardians =
                            guardiansResponsibleForFeeData(decision.headOfFamily.id, guardians)
                    )
                }
            } catch (e: Exception) {
                error(
                    "VardaUpdate: failed to send voucher decision data for service need ${evakaServiceNeed.id}: ${e.localizedMessage}"
                )
            }
        }

    return feeResponseIds + voucherResponseIds
}

private fun getChildVardaGuardians(
    db: Database.Connection,
    childId: ChildId
): List<VardaGuardianWithId> {
    return db.read {
        @Suppress("DEPRECATION")
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
            """
                    .trimIndent()
            )
            .bind("id", childId)
            .toList<VardaGuardianWithId>()
    }
}

private fun sendFeeDecisionToVarda(
    client: VardaClient,
    decision: FeeDecision,
    vardaChildId: Long,
    evakaServiceNeedInfoForVarda: EvakaServiceNeedInfoForVarda,
    guardians: List<VardaGuardianWithId>
): Long {
    val childPart =
        decision.children.find { part -> part.child.id == evakaServiceNeedInfoForVarda.childId }

    check(childPart != null) {
        "VardaUpdate: fee decision ${decision.id} has no part for child ${evakaServiceNeedInfoForVarda.childId}, service need ${evakaServiceNeedInfoForVarda.id}"
    }

    val requestPayload =
        VardaFeeData(
            huoltajat = guardians.map { it.toVardaGuardian() },
            lapsi = client.getChildUrl(vardaChildId),
            maksun_peruste_koodi = vardaFeeBasisByPlacementType(childPart.placement.type),
            asiakasmaksu = childPart.finalFee.div(100.0),
            palveluseteli_arvo = 0.0,
            perheen_koko = decision.familySize,
            alkamis_pvm =
                calculateVardaFeeDataStartDate(
                    decision.validFrom,
                    evakaServiceNeedInfoForVarda.asPeriod
                ),
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
    val requestPayload =
        VardaFeeData(
            huoltajat = guardians.map { guardian -> guardian.toVardaGuardian() },
            lapsi = client.getChildUrl(vardaChildId),
            maksun_peruste_koodi = vardaFeeBasisByPlacementType(decision.placement.type),
            asiakasmaksu = decision.finalCoPayment.div(100.0),
            palveluseteli_arvo = decision.voucherValue.div(100.0),
            perheen_koko = decision.familySize,
            alkamis_pvm =
                calculateVardaFeeDataStartDate(
                    decision.validFrom,
                    evakaServiceNeedInfoForVarda.asPeriod
                ),
            paattymis_pvm = minEndDate(evakaServiceNeedInfoForVarda.endDate, decision.validTo),
            lahdejarjestelma = client.sourceSystem
        )

    return client.createFeeData(requestPayload).id
}

private fun vardaFeeBasisByPlacementType(type: PlacementType): String {
    return if (
        type == PlacementType.DAYCARE_FIVE_YEAR_OLDS ||
            type == PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
    ) {
        FeeBasisCode.FIVE_YEAR_OLDS_DAYCARE.code
    } else {
        FeeBasisCode.DAYCARE.code
    }
}

private fun calculateVardaFeeDataStartDate(
    fdStartDate: LocalDate,
    serviceNeedDates: DateRange
): LocalDate {
    return if (serviceNeedDates.includes(fdStartDate)) fdStartDate else serviceNeedDates.start
}

fun calculateEvakaVsVardaServiceNeedChangesByChild(
    db: Database.Connection,
    clock: EvakaClock,
    feeDecisionMinDate: LocalDate
): Map<ChildId, VardaChildCalculatedServiceNeedChanges> {
    val evakaServiceNeedDeletionsByChild = db.read { it.calculateDeletedChildServiceNeeds(clock) }

    val evakaServiceNeedChangesByChild =
        db.read { it.getEvakaServiceNeedChanges(clock, feeDecisionMinDate) }
            .groupBy { it.evakaChildId }

    val additionsAndChangesToVardaByChild =
        evakaServiceNeedChangesByChild.entries.associate { evakaServiceNeedChangesForChild ->
            val vardaServiceNeedsForChild =
                db.read { it.getChildVardaServiceNeeds(evakaServiceNeedChangesForChild.key) }

            evakaServiceNeedChangesForChild.key to
                VardaChildCalculatedServiceNeedChanges(
                    childId = evakaServiceNeedChangesForChild.key,
                    additions =
                        calculateNewChildServiceNeeds(
                            evakaServiceNeedChangesForChild.value,
                            vardaServiceNeedsForChild
                        ),
                    updates =
                        calculateUpdatedChildServiceNeeds(
                            evakaServiceNeedChangesForChild.value,
                            vardaServiceNeedsForChild
                        ),
                    deletes =
                        evakaServiceNeedDeletionsByChild.getOrDefault(
                            evakaServiceNeedChangesForChild.key,
                            emptyList()
                        )
                )
        }

    // Above misses the case when only change for a child has been a service need delete from evaka
    val deletedEvakaServiceNeedsByChild =
        evakaServiceNeedDeletionsByChild
            .filterNot { additionsAndChangesToVardaByChild.containsKey(it.key) }
            .entries
            .associate { childServiceNeedDeletions ->
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

// Find out new varhaiskasvatuspaatos to be added for a child: any new service needs not found from
// child's full varda history
private fun calculateNewChildServiceNeeds(
    evakaServiceNeedChangesForChild: List<ChangedChildServiceNeed>,
    vardaChildServiceNeeds: List<VardaServiceNeed>
): List<ServiceNeedId> {
    return evakaServiceNeedChangesForChild
        .filter { newServiceNeedChange ->
            vardaChildServiceNeeds.none { vardaChildServiceNeedChange ->
                vardaChildServiceNeedChange.evakaServiceNeedId ==
                    newServiceNeedChange.evakaServiceNeedId
            }
        }
        .map { it.evakaServiceNeedId }
}

// Find out changed varhaiskasvatuspaatos for a child: any new service need with a different update
// timestamp in history
private fun calculateUpdatedChildServiceNeeds(
    evakaServiceNeedChangesForChild: List<ChangedChildServiceNeed>,
    vardaServiceNeedsForChild: List<VardaServiceNeed>
): List<ServiceNeedId> {
    return evakaServiceNeedChangesForChild.mapNotNull { newServiceNeedChange ->
        vardaServiceNeedsForChild
            .find { it.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId }
            ?.evakaServiceNeedId
    }
}
