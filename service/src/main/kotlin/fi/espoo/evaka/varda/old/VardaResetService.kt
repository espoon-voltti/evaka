// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda.old

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.varda.deleteVardaOrganizerChildByVardaChildId
import fi.espoo.evaka.varda.deleteVardaServiceNeedByEvakaChildId
import fi.espoo.evaka.varda.getServiceNeedsForVardaByChild
import fi.espoo.evaka.varda.getVardaChildrenToReset
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import fi.espoo.evaka.varda.setVardaResetChildResetTimestamp
import java.time.Instant
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class VardaResetService(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val mapper: JsonMapper,
    private val vardaEnv: VardaEnv,
    private val evakaEnv: EvakaEnv,
    private val ophEnv: OphEnv
) {
    private val feeDecisionMinDate = evakaEnv.feeDecisionMinDate
    private val municipalOrganizerOid = ophEnv.organizerOid

    init {
        asyncJobRunner.registerHandler(::oldResetVardaChildByAsyncJob)
    }

    val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

    fun planVardaReset(
        db: Database.Connection,
        clock: EvakaClock,
        addNewChildren: Boolean,
        maxChildren: Int = 1000
    ) {
        logger.info("VardaUpdate: starting reset process")
        val resetChildIds =
            db.transaction {
                it.getVardaChildrenToReset(limit = maxChildren, addNewChildren = addNewChildren)
            }
        logger.info("VardaUpdate: will reset ${resetChildIds.size} children (max was $maxChildren)")

        db.transaction { tx ->
            asyncJobRunner.plan(
                tx,
                resetChildIds.map { AsyncJob.ResetVardaChildOld(it) },
                retryCount = 1,
                runAt = clock.now()
            )
        }
    }

    fun oldResetVardaChildByAsyncJob(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.ResetVardaChildOld
    ) {
        logger.info("VardaUpdate (old): starting to reset child ${msg.childId}")
        resetVardaChild(db, clock, client, msg.childId, feeDecisionMinDate, municipalOrganizerOid)
    }
}

private fun resetVardaChild(
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
                "VardaUpdate: found ${childServiceNeeds.size} service needs for child $childId to be sent"
            )
            childServiceNeeds.forEachIndexed { idx, serviceNeedId ->
                logger.info(
                    "VardaUpdate: sending service need $serviceNeedId for child $childId (${idx + 1}/${childServiceNeeds.size})"
                )
                handleNewEvakaServiceNeed(
                    db,
                    client,
                    serviceNeedId,
                    feeDecisionMinDate,
                    municipalOrganizerOid
                )
            }
            db.transaction { it.setVardaResetChildResetTimestamp(childId, Instant.now()) }
            logger.info(
                "VardaUpdate: successfully sent ${childServiceNeeds.size} service needs for $childId"
            )
        } catch (e: Exception) {
            logger.warn("VardaUpdate: failed to reset child $childId: ${e.message}", e)
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
        "VardaUpdate: deleting all varda data for evaka child $evakaChildId, varda child ids: ${vardaChildIds.joinToString(",")}"
    )

    logger.info("VardaUpdate: deleting all varda service needs for $evakaChildId")
    db.transaction { it.deleteVardaServiceNeedByEvakaChildId(evakaChildId) }

    val successfulDeletes: List<Boolean> =
        vardaChildIds.map { vardaChildId ->
            try {
                deleteChildDataFromVardaAndDbByVardaId(db, vardaClient, vardaChildId)
                true
            } catch (e: Exception) {
                logger.warn(
                    "VardaUpdate: failed to delete varda data for child $evakaChildId (varda id $vardaChildId): ${e.localizedMessage}",
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
    logger.info("VardaUpdate: deleting all child data from varda (child id: $vardaChildId)")
    try {
        vardaClient.deleteChildAllData(vardaChildId)
    } catch (e: Exception) {
        // MI015 = unknown child so delete can be considered being a success
        if (e.message?.contains("MI015") != true) throw e
    }

    logger.info("VardaUpdate: deleting all evaka side varda data of child $vardaChildId")
    db.transaction { it.deleteVardaOrganizerChildByVardaChildId(vardaChildId) }

    logger.info("VardaUpdate: successfully deleted evaka side varda data for child $vardaChildId")
}

private fun getVardaChildIdsByEvakaChildId(
    db: Database.Connection,
    evakaChildId: ChildId
): List<Long> {
    return db.read {
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
