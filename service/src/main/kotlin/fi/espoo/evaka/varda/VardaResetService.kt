// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VardaAsyncJob
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class VardaResetService(
    private val asyncJobRunner: AsyncJobRunner<VardaAsyncJob>,
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val mapper: ObjectMapper,
    private val vardaEnv: VardaEnv,
    private val evakaEnv: EvakaEnv,
    private val ophEnv: OphEnv
) {
    private val feeDecisionMinDate = evakaEnv.feeDecisionMinDate
    private val municipalOrganizerOid = ophEnv.ophOrganizerOid

    init {
        asyncJobRunner.registerHandler(::resetVardaChildByAsyncJob)
        asyncJobRunner.registerHandler(::deleteVardaChildByAsyncJob)
    }

    val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

    fun planVardaReset(db: Database.Connection, addNewChildren: Boolean, maxChildren: Int = 1000) {
        logger.info("VardaUpdate: starting reset process")
        val resetChildIds = db.transaction { it.getVardaChildrenToReset(limit = maxChildren, addNewChildren = addNewChildren) }
        logger.info("VardaUpdate: will reset ${resetChildIds.size} children (max was $maxChildren)")

        db.transaction { tx ->
            asyncJobRunner.plan(
                tx, resetChildIds.map { VardaAsyncJob.ResetVardaChild(it) },
                retryCount = 2,
                retryInterval = Duration.ofMinutes(10)
            )
        }
    }

    fun resetVardaChildByAsyncJob(db: Database.Connection, msg: VardaAsyncJob.ResetVardaChild) {
        logger.info("VardaUpdate: starting to reset child ${msg.childId}")
        resetVardaChild(db, client, msg.childId, feeDecisionMinDate, municipalOrganizerOid)
    }

    fun deleteVardaChildByAsyncJob(db: Database.Connection, msg: VardaAsyncJob.DeleteVardaChild) {
        logger.info("VardaUpdate: starting to delete child ${msg.vardaChildId}")
        deleteChildDataFromVardaAndDbByVardaId(db, client, msg.vardaChildId)
    }

    fun planResetByVardaChildrenErrorReport(db: Database.Connection, organizerId: Long, limit: Int) {
        try {
            val errorReport = client.getVardaChildrenErrorReport(organizerId)
            logger.info("VardaUpdate: found ${errorReport.size} rows from error report, will limit to $limit rows")

            val mapper = db.read { it.getVardaChildToEvakaChild() }
            val (childrenWithId, childrenWithoutId) = errorReport.take(limit).map { it.lapsi_id }.partition {
                mapper[it] != null
            }

            logger.info("VardaUpdate: setting ${childrenWithId.size} children to be reset")
            db.transaction { tx -> tx.setToBeReset(childrenWithId.map { mapper[it]!! }) }

            logger.info("VardaUpdate: scheduling ${childrenWithoutId.size} children to be deleted")
            db.transaction { tx ->
                asyncJobRunner.plan(
                    tx, childrenWithoutId.map { VardaAsyncJob.DeleteVardaChild(it) },
                    retryCount = 2,
                    retryInterval = Duration.ofMinutes(1)
                )
            }
        } catch (e: Exception) {
            logger.info("VardaUpdate: failed to nuke varda children by report data: ${e.localizedMessage}")
        }
    }
}

private fun resetVardaChild(db: Database.Connection, client: VardaClient, childId: UUID, feeDecisionMinDate: LocalDate, municipalOrganizerOid: String) {
    if (deleteChildDataFromVardaAndDb(db, client, childId)) {
        try {
            val childServiceNeeds = db.read { it.getServiceNeedsForVardaByChild(childId) }
            logger.info("VardaUpdate: found ${childServiceNeeds.size} service needs for child $childId to be sent")
            childServiceNeeds.forEachIndexed { idx, serviceNeedId ->
                logger.info("VardaUpdate: sending service need $serviceNeedId for child $childId (${idx + 1}/${childServiceNeeds.size})")
                handleNewEvakaServiceNeed(db, client, serviceNeedId, feeDecisionMinDate, municipalOrganizerOid)
            }
            db.transaction { it.setVardaResetChildResetTimestamp(childId, Instant.now()) }
            logger.info("VardaUpdate: successfully sent ${childServiceNeeds.size} service needs for $childId")
        } catch (e: Exception) {
            logger.warn("VardaUpdate: failed to reset child $childId: ${e.message}")
        }
    }
}

fun deleteChildDataFromVardaAndDb(db: Database.Connection, vardaClient: VardaClient, evakaChildId: UUID): Boolean {
    val vardaChildIds = getVardaChildIdsByEvakaChildId(db, evakaChildId)

    logger.info("VardaUpdate: deleting all varda data for evaka child $evakaChildId, varda child ids: ${vardaChildIds.joinToString(",")}")

    val successfulDeletes: List<Boolean> = vardaChildIds.map { vardaChildId ->
        try {
            deleteChildDataFromVardaAndDbByVardaId(db, vardaClient, vardaChildId)
            true
        } catch (e: Exception) {
            logger.warn("VardaUpdate: failed to delete varda data for child $evakaChildId (varda id $vardaChildId): ${e.localizedMessage}")
            false
        }
    }

    return successfulDeletes.all { it }
}

fun deleteChildDataFromVardaAndDbByVardaId(db: Database.Connection, vardaClient: VardaClient, vardaChildId: Long) {
    val decisionIds = vardaClient.getDecisionsByChild(vardaChildId)
    logger.info("VardaUpdate: found ${decisionIds.size} decisions to be deleted for child $vardaChildId")

    val placementIds = decisionIds.flatMap { vardaClient.getPlacementsByDecision(it) }
    logger.info("VardaUpdate: found ${placementIds.size} placements to be deleted for child $vardaChildId")

    val feeIds = vardaClient.getFeeDataByChild(vardaChildId)
    logger.info("VardaUpdate: found ${feeIds.size} fee data to be deleted for child $vardaChildId")

    feeIds.forEachIndexed { index, feeId ->
        logger.info("VardaUpdate: deleting fee data $feeId for child $vardaChildId (${index + 1} / ${feeIds.size})")
        vardaClient.deleteFeeData(feeId)
    }

    placementIds.forEachIndexed { index, placementId ->
        logger.info("VardaUpdate: deleting placement $placementId for child $vardaChildId (${index + 1} / ${placementIds.size})")
        vardaClient.deletePlacement(placementId)
    }

    decisionIds.forEachIndexed { index, decisionId ->
        logger.info("VardaUpdate: deleting decision $decisionId for child $vardaChildId (${index + 1} / ${decisionIds.size})")
        vardaClient.deleteDecision(decisionId)
    }

    logger.info("VardaUpdate: deleting child $vardaChildId")
    vardaClient.deleteChild(vardaChildId)
    db.transaction { it.deleteVardaOrganizerChildByVardaChildId(vardaChildId) }

    logger.info("VardaUpdate: deleting from varda_service_need for child $vardaChildId")
    db.transaction { it.deleteVardaServiceNeedByVardaChildId(vardaChildId) }

    logger.info("VardaUpdate: successfully deleted data for child $vardaChildId")
}

private fun getVardaChildIdsByEvakaChildId(db: Database.Connection, evakaChildId: UUID): List<Long> {
    return db.read {
        it.createQuery(
            """
            select varda_child_id from varda_service_need where evaka_child_id = :evakaChildId and varda_child_id is not null 
            union
            select varda_child_id from varda_organizer_child where evaka_person_id = :evakaChildId
            """.trimIndent()
        )
            .bind("evakaChildId", evakaChildId)
            .mapTo<Long>()
            .toList()
    }
}
