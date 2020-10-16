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
    private val forceSync = env.getProperty("fi.espoo.varda.force.sync", Boolean::class.java, false)
    private val organizer = env.getProperty("fi.espoo.varda.organizer", String::class.java, "Espoo")

    @PostMapping("/update")
    fun updateAll(): ResponseEntity<Unit> {
        val client = VardaClient(tokenProvider, env, mapper)
        if (forceSync) {
            updateAll(jdbi, client, mapper, personService, organizer)
        } else {
            thread {
                try {
                    updateAll(jdbi, client, mapper, personService, organizer)
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
    mapper: ObjectMapper,
    personService: PersonService,
    organizer: String
) {
    jdbi.handle { h ->
        updateOrganizer(h, client, organizer)
        updateUnits(h, client, organizer)
        updateChildren(h, client, organizer)
        updateDecisions(h, client)
        updatePlacements(h, client)
        updateFeeData(h, client, mapper, personService)
        removeMarkedPlacements(h, client)
        removeMarkedDecisions(h, client)
    }
}
