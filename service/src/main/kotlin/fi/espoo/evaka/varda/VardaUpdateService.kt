// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kittinunf.fuel.core.FuelManager
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.lang.reflect.UndeclaredThrowableException
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }

@Service
class VardaUpdateService(
    private val tokenProvider: VardaTokenProvider,
    private val fuel: FuelManager,
    private val env: Environment,
    private val mapper: ObjectMapper,
    private val personService: PersonService
) {
    private val forceSync = env.getProperty("fi.espoo.varda.force.sync", Boolean::class.java, false)
    private val organizer = env.getProperty("fi.espoo.varda.organizer", String::class.java, "Espoo")

    fun updateAll(db: Database.Connection) {
        val client = VardaClient(tokenProvider, fuel, env, mapper)
        if (forceSync) {
            updateAll(db, client, mapper, personService, organizer)
        } else {
            thread {
                try {
                    updateAll(db, client, mapper, personService, organizer)
                } catch (e: Throwable) {
                    val exception = (e as? UndeclaredThrowableException)?.cause ?: e
                    logger.error(exception) { "Failed to run Varda update" }
                }
            }
        }
    }

    // This is left for backwards compatibility, but should be removed later
    fun updateUnits(db: Database.Connection) = updateAll(db)
}

fun updateAll(
    db: Database.Connection,
    client: VardaClient,
    mapper: ObjectMapper,
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
    updateFeeData(db, client, mapper, personService)
}
