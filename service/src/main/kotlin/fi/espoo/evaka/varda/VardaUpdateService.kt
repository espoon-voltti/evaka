// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VardaUpdate
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate

internal val vardaMinDate = LocalDate.of(2019, 1, 1)
internal val vardaPlacementTypes = arrayOf(
    PlacementType.DAYCARE,
    PlacementType.DAYCARE_PART_TIME,
    PlacementType.DAYCARE_FIVE_YEAR_OLDS,
    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS,
    PlacementType.PRESCHOOL_DAYCARE,
    PlacementType.PREPARATORY_DAYCARE
)
internal val vardaTemporaryPlacementTypes = arrayOf(
    PlacementType.TEMPORARY_DAYCARE,
    PlacementType.TEMPORARY_DAYCARE_PART_DAY
)
internal val vardaDecisionTypes = arrayOf(
    DecisionType.DAYCARE,
    DecisionType.DAYCARE_PART_TIME,
    DecisionType.PRESCHOOL_DAYCARE
)

@Service
class VardaUpdateService(
    private val asyncJobRunner: AsyncJobRunner,
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val env: Environment,
    private val mapper: ObjectMapper,
    private val personService: PersonService
) {
    private val forceSync = env.getProperty("fi.espoo.varda.force.sync", Boolean::class.java, false)
    private val organizer = env.getProperty("fi.espoo.varda.organizer", String::class.java, "Espoo")

    init {
        asyncJobRunner.vardaUpdate = ::updateAll
    }

    fun scheduleVardaUpdate(db: Database.Connection) {
        if (forceSync) {
            val client = VardaClient(tokenProvider, fuel, env, mapper)
            updateAll(db, client, personService, organizer)
        } else {
            db.transaction { asyncJobRunner.plan(it, listOf(VardaUpdate()), retryCount = 1) }
        }
    }

    fun updateAll(db: Database, msg: VardaUpdate) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        db.connect { updateAll(it, client, personService, organizer) }
    }

    fun deleteChild(vardaChildId: Long, db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        deleteChild(db, client, vardaChildId)
    }

    fun deleteFeeDataByChild(vardaChildId: Long, db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        val feeDataIds = client.getFeeDataByChild(vardaChildId)

        deleteFeeData(db, client, feeDataIds)
    }

    fun deletePlacementsByChild(vardaChildId: Long, db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        val placementIds = client.getPlacementsByChild(vardaChildId)

        deletePlacements(db, client, placementIds)
    }

    fun deleteDecisionsByChild(vardaChildId: Long, db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        val decisionIds = client.getDecisionsByChild(vardaChildId)

        deleteDecisions(db, client, decisionIds)
    }
}

fun updateAll(
    db: Database.Connection,
    client: VardaClient,
    personService: PersonService,
    organizer: String
) {
    removeMarkedFeeDataFromVarda(db, client)
    removeMarkedPlacementsFromVarda(db, client)
    removeMarkedDecisionsFromVarda(db, client)
    updateOrganizer(db, client, organizer)
    updateUnits(db, client, organizer)
    updateChildren(db, client, organizer)
    updateDecisions(db, client)
    updatePlacements(db, client)
    updateFeeData(db, client, personService)
}
