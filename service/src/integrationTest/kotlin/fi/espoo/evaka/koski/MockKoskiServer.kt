// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import fi.espoo.evaka.shared.config.defaultJsonMapper
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.post
import io.javalin.apibuilder.ApiBuilder.put
import io.javalin.http.Context
import java.util.concurrent.locks.ReentrantLock
import javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST
import javax.servlet.http.HttpServletResponse.SC_NOT_FOUND
import kotlin.concurrent.withLock
import kotlin.random.Random
import mu.KotlinLogging

private typealias PersonOid = String

private typealias StudyRightOid = String

private typealias Ssn = String

data class MockStudyRight(val version: Int, val opiskeluoikeus: Opiskeluoikeus)

class MockKoskiServer(private val jsonMapper: JsonMapper, port: Int) : AutoCloseable {
    private val app = Javalin.create().start(port)
    private val logger = KotlinLogging.logger {}

    private val lock = ReentrantLock()
    private val persons = HashMap<Ssn, PersonOid>()
    private val studyRights = HashMap<StudyRightOid, MockStudyRight>()
    private val personStudyRights: SetMultimap<PersonOid, StudyRightOid> =
        Multimaps.newSetMultimap(HashMap()) { HashSet() }
    private var personOid = Random.nextInt(1_000_000)
    private var studyRightOid = Random.nextInt(1_000_000)

    val port
        get() = app.port()

    init {
        app.routes {
            put("/oppija", ::oppija)
            post("/oppija", ::oppija)
        }
    }

    fun getStudyRights(): HashMap<StudyRightOid, MockStudyRight> =
        lock.withLock { HashMap(studyRights) }
    fun getPersonStudyRights(oid: PersonOid): Set<StudyRightOid> =
        lock.withLock { personStudyRights.get(oid) }

    fun clearData() =
        lock.withLock {
            persons.clear()
            studyRights.clear()
        }

    private fun oppija(ctx: Context) {
        logger.info { "Mock Koski received ${ctx.method()} body: ${ctx.body()}" }
        val oppija = jsonMapper.readValue(ctx.body(), Oppija::class.java)

        when (ctx.method()) {
            "POST" ->
                if (oppija.opiskeluoikeudet.any { it.oid != null }) {
                    ctx.contentType("text/plain")
                        .status(SC_BAD_REQUEST)
                        .result("Trying to create a study right with OID")
                    return
                }
            "PUT" ->
                if (
                    oppija.opiskeluoikeudet
                        .mapNotNull { it.oid }
                        .any { !studyRights.containsKey(it) }
                ) {
                    ctx.contentType("application/json")
                        .status(SC_NOT_FOUND)
                        .result(
                            jsonMapper.writeValueAsString(
                                listOf(
                                    KoskiClient.Error(
                                        key = "notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia",
                                        message = "Not found"
                                    )
                                )
                            )
                        )
                    return
                }
        }

        val response =
            lock.withLock {
                val personOid =
                    when (oppija.henkilö) {
                        is OidHenkilö -> (oppija.henkilö as OidHenkilö).oid
                        is UusiHenkilö -> {
                            val ssn = (oppija.henkilö as UusiHenkilö).hetu
                            persons.getOrPut(ssn) { "1.2.246.562.24.${personOid++}" }
                        }
                    }
                // Raw Jackson databind API is used to generate the response, because we want to add
                // some
                // fields
                // that are missing in our data class -based representation to simulate a real
                // response
                // more accurately
                jsonMapper.createObjectNode().apply {
                    with("henkilö").apply { put("oid", personOid) }
                    withArray("opiskeluoikeudet").apply {
                        addAll(
                            oppija.opiskeluoikeudet.map {
                                val studyRightOid = it.oid ?: "1.2.246.562.15.${studyRightOid++}"
                                val version =
                                    studyRights[studyRightOid]?.version?.let { v -> v + 1 } ?: 0

                                if (
                                    it.tila.opiskeluoikeusjaksot.any { jakso ->
                                        jakso.tila.koodiarvo == OpiskeluoikeusjaksonTilaKoodi.VOIDED
                                    }
                                ) {
                                    studyRights.remove(studyRightOid)
                                    personStudyRights.get(personOid).remove(studyRightOid)
                                } else {
                                    studyRights[studyRightOid] =
                                        MockStudyRight(
                                            version,
                                            opiskeluoikeus = it.copy(oid = studyRightOid)
                                        )
                                    personStudyRights.get(personOid).add(studyRightOid)
                                }

                                jsonMapper.createObjectNode().apply {
                                    put("oid", studyRightOid)
                                    put("versionumero", version)
                                    with("lähdejärjestelmänId").apply {
                                        put("id", it.lähdejärjestelmänId.id.toString())
                                        with("lähdejärjestelmä").apply {
                                            put(
                                                "koodiarvo",
                                                it.lähdejärjestelmänId.lähdejärjestelmä.koodiarvo
                                            )
                                            put(
                                                "koodistoUri",
                                                it.lähdejärjestelmänId.lähdejärjestelmä.koodistoUri
                                            )
                                            put("koodistoVersio", 1)
                                            with("nimi").apply { put("fi", "EvakaEspoo") }
                                            with("lyhytNimi").apply { put("fi", "EvakaEspoo") }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        ctx.contentType("application/json").result(jsonMapper.writeValueAsString(response))
    }

    override fun close() {
        app.stop()
    }

    companion object {
        fun start(): MockKoskiServer {
            return MockKoskiServer(defaultJsonMapper(), port = 0)
        }
    }
}
