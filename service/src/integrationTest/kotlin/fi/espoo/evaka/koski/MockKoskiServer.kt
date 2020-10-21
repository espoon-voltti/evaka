// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.shared.config.defaultObjectMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.Context
import mu.KotlinLogging
import java.util.concurrent.locks.ReentrantLock
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import kotlin.concurrent.withLock
import kotlin.random.Random

private typealias Oid = String
private typealias Ssn = String

data class MockStudyRight(val version: Int, val opiskeluoikeus: Opiskeluoikeus)

class MockKoskiServer(private val objectMapper: ObjectMapper, port: Int) : AutoCloseable {
    private val app = Javalin.create().start(port)
    private val logger = KotlinLogging.logger {}

    private val lock = ReentrantLock()
    private val persons = HashMap<Ssn, Oid>()
    private val studyRights = HashMap<Oid, MockStudyRight>()
    private var personOid = Random.nextInt(1_000_000)
    private var studyRightOid = Random.nextInt(1_000_000)

    val port get() = app.port()

    init {
        app.routes {
            put("/oppija", ::oppija)
            post("/oppija", ::oppija)
        }
    }

    fun getStudyRights(): HashMap<Oid, MockStudyRight> = lock.withLock { HashMap(studyRights) }

    fun clearData() = lock.withLock {
        persons.clear()
        studyRights.clear()
    }

    private fun oppija(ctx: Context) {
        logger.info { "Mock Koski received ${ctx.method()} body: ${ctx.body()}" }
        val oppija = objectMapper.readValue(ctx.body(), Oppija::class.java)

        when (ctx.method()) {
            "POST" -> if (oppija.opiskeluoikeudet.any { it.oid != null }) {
                ctx.contentType("text/plain").status(SC_BAD_REQUEST).result("Trying to create a study right with OID")
                return
            }
            "PUT" -> if (oppija.opiskeluoikeudet.mapNotNull { it.oid }.any { !studyRights.containsKey(it) }) {
                ctx.contentType("text/plain").status(SC_BAD_REQUEST)
                    .result("Can't update a study right that doesn't exist")
                return
            }
        }

        val response = lock.withLock {
            val ssn = oppija.henkilö.hetu
            val personOid = persons.getOrPut(ssn) { "1.2.246.562.24.${personOid++}" }
            // Raw Jackson databind API is used to generate the response, because we want to add some fields
            // that are missing in our data class -based representation to simulate a real response more accurately
            objectMapper.createObjectNode().apply {
                with("henkilö").apply {
                    put("oid", personOid)
                }
                withArray("opiskeluoikeudet").apply {
                    addAll(
                        oppija.opiskeluoikeudet.map {
                            val studyRightOid = it.oid ?: "1.2.246.562.15.${studyRightOid++}"
                            val version = studyRights[studyRightOid]?.version?.let { v -> v + 1 } ?: 0

                            if (it.tila.opiskeluoikeusjaksot.any { jakso -> jakso.tila.koodiarvo == OpiskeluoikeusjaksonTilaKoodi.VOIDED }) {
                                studyRights.remove(studyRightOid)
                            } else {
                                studyRights[studyRightOid] =
                                    MockStudyRight(version, opiskeluoikeus = it.copy(oid = studyRightOid))
                            }

                            objectMapper.createObjectNode().apply {
                                put("oid", studyRightOid)
                                put("versionumero", version)
                                with("lähdejärjestelmänId").apply {
                                    put("id", it.lähdejärjestelmänId.id.toString())
                                    with("lähdejärjestelmä").apply {
                                        put("koodiarvo", it.lähdejärjestelmänId.lähdejärjestelmä.koodiarvo)
                                        put("koodistoUri", it.lähdejärjestelmänId.lähdejärjestelmä.koodistoUri)
                                        put("koodistoVersio", 1)
                                        with("nimi").apply {
                                            put("fi", "EvakaEspoo")
                                        }
                                        with("lyhytNimi").apply {
                                            put("fi", "EvakaEspoo")
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        ctx.contentType("application/json").result(objectMapper.writeValueAsString(response))
    }

    override fun close() {
        app.stop()
    }

    companion object {
        fun start(): MockKoskiServer {
            return MockKoskiServer(defaultObjectMapper(), port = 0)
        }
    }
}
