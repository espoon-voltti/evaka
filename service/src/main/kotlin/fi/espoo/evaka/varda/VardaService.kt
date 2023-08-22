// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.VardaEnv
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import java.time.Duration
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
    private val ophMunicipalityOid = ophEnv.organizerOid
    private val ophMunicipalOrganizerIdUrl =
        "${vardaEnv.url}/v1/vakajarjestajat/${ophEnv.organizerId}/"

    val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

    fun startVardaUpdate(db: Database.Connection, clock: EvakaClock) {
        val client = VardaClient(tokenProvider, fuel, mapper, vardaEnv)

        logger.info("VardaService: starting update process")

        updateUnits(db, clock, client, ophMunicipalityCode, ophMunicipalOrganizerIdUrl)

        planVardaChildrenUpdate(db, clock)
    }

    fun planVardaChildrenUpdate(db: Database.Connection, clock: EvakaClock) {
        val serviceNeedDiffsByChild = getChildrenToUpdate(db, clock, feeDecisionMinDate)
        val childsToBeReset =
            db.transaction { it.getVardaChildrenToReset(limit = 1000, addNewChildren = true) }

        val allChildrenToUpdate = serviceNeedDiffsByChild.keys.toSet() + childsToBeReset.toSet()

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

    fun calculateEvakaVsVardaServiceNeedChangesByChild(
        db: Database.Connection,
        clock: EvakaClock,
        feeDecisionMinDate: LocalDate
    ): Map<ChildId, VardaChildCalculatedServiceNeedChanges> {
        val evakaServiceNeedDeletionsByChild =
            db.read { it.calculateDeletedChildServiceNeeds(clock) }

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

        // Above misses the case when only change for a child has been a service need delete from
        // evaka
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

    // Find out new varhaiskasvatuspaatos to be added for a child: any new service needs not found
    // from
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

    // Find out changed varhaiskasvatuspaatos for a child: any new service need with a different
    // update
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
}
