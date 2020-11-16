// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.lang.reflect.UndeclaredThrowableException
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }

@Service
class VardaUpdateService(
    private val jdbi: Jdbi,
    private val tokenProvider: VardaTokenProvider,
    private val env: Environment,
    private val mapper: ObjectMapper,
    private val personService: PersonService
) {
    private val forceSync = env.getProperty("fi.espoo.varda.force.sync", Boolean::class.java, false)
    private val organizer = env.getProperty("fi.espoo.varda.organizer", String::class.java, "Espoo")

    fun updateAll() {
        val client = VardaClient(tokenProvider, env, mapper)
        if (forceSync) {
            updateAll(Database(jdbi), client, mapper, personService, organizer)
        } else {
            thread {
                try {
                    updateAll(Database(jdbi), client, mapper, personService, organizer)
                } catch (e: Throwable) {
                    val exception = (e as? UndeclaredThrowableException)?.cause ?: e
                    logger.error(exception) { "Failed to run Varda update" }
                }
            }
        }
    }

    // This is left for backwards compatibility, but should be removed later
    fun updateUnits() = updateAll()
}

fun updateAll(
    db: Database,
    client: VardaClient,
    mapper: ObjectMapper,
    personService: PersonService,
    organizer: String
) {
    db.transaction { removeMarkedFeeDataFromVarda(it.handle, client) }
    db.transaction { removeMarkedPlacementsFromVarda(it.handle, client) }
    db.transaction { removeMarkedDecisionsFromVarda(it.handle, client) }
    db.transaction { updateOrganizer(it.handle, client, organizer) }
    db.transaction { updateUnits(it.handle, client, organizer) }
    db.transaction { updateChildren(it.handle, client, organizer) }
    db.transaction { updateDecisions(it.handle, client) }
    db.transaction { updatePlacements(it.handle, client) }
    db.transaction { updateFeeData(it, client, mapper, personService) }
}
