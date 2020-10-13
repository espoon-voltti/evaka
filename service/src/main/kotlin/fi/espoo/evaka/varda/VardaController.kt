// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.VardaTokenProvider
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.springframework.core.env.Environment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.UndeclaredThrowableException
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }

@RestController
@RequestMapping("/varda")
class VardaController(
    private val jdbi: Jdbi,
    private val tokenProvider: VardaTokenProvider,
    private val env: Environment,
    private val mapper: ObjectMapper,
    private val personService: PersonService
) {
    private val uploadChildren = env.getProperty("fi.espoo.varda.upload.children", Boolean::class.java, false)
    private val forceSync = env.getProperty("fi.espoo.varda.force.sync", Boolean::class.java, false)

    @PostMapping("/update")
    fun updateAll(): ResponseEntity<Unit> {
        val client = VardaClient(tokenProvider, env, mapper)
        if (forceSync) {
            updateAll(jdbi, client, uploadChildren, mapper, personService)
        } else {
            thread {
                try {
                    updateAll(jdbi, client, uploadChildren, mapper, personService)
                } catch (e: Throwable) {
                    val exception = (e as? UndeclaredThrowableException)?.cause ?: e
                    logger.error(exception) { "Failed to run Varda update" }
                }
            }
        }
        return ResponseEntity.noContent().build()
    }

    // This path is left for backwards compatibility, but should be removed later
    @PostMapping("/units/update-units")
    fun updateUnits(): ResponseEntity<Unit> = updateAll()
}

fun updateAll(
    jdbi: Jdbi,
    client: VardaClient,
    uploadChildren: Boolean,
    mapper: ObjectMapper,
    personService: PersonService
) {
    jdbi.handle { h ->
        logger.debug { "Updating Varda organizer" }
        updateOrganizer(h, client)
        logger.debug { "Updating Varda units" }
        updateUnits(h, client)
        if (uploadChildren) {
            logger.debug { "Updating Varda children" }
            updateChildren(h, client)
            logger.debug { "Updating Varda decisions" }
            updateDecisions(h, client)
            logger.debug { "Updating Varda placements" }
            updatePlacements(h, client)
            logger.debug { "Updating Varda fee decisions" }
            updateFeeData(h, client, mapper, personService)
        }
        logger.debug { "Varda update finished" }
    }
}

fun clearPlacementsAndDecisions(
    jdbi: Jdbi,
    client: VardaClient
) {
    jdbi.handle { h ->
        val placementIds: List<Long> = getPlacementsToDelete(h)
        placementIds.forEach { id ->
            client.deletePlacement(id)
            softDeletePlacement(h, id)
        }

        val decisionIds: List<Long> = getDecisionsToDelete(h)
        decisionIds.forEach { id ->
            client.deleteDecision(id)
            softDeleteDecision(h, id)
        }
    }
}
