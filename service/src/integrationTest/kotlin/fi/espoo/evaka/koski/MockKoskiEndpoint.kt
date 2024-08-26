// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.databind.json.JsonMapper
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.random.Random
import mu.KotlinLogging
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

private typealias PersonOid = String

private typealias StudyRightOid = String

private typealias Ssn = String

data class MockStudyRight(val version: Int, val opiskeluoikeus: Opiskeluoikeus)

@RestController
@RequestMapping("/public/mock-koski")
class MockKoskiEndpoint(private val jsonMapper: JsonMapper) {
    private val logger = KotlinLogging.logger {}
    private val lock = ReentrantLock()
    private val persons = HashMap<Ssn, PersonOid>()
    private val studyRights = HashMap<StudyRightOid, MockStudyRight>()
    private val personStudyRights: SetMultimap<PersonOid, StudyRightOid> =
        Multimaps.newSetMultimap(HashMap()) { HashSet() }
    private var personOid = Random.nextInt(1_000_000)
    private var studyRightOid = Random.nextInt(1_000_000)

    @RequestMapping("/oppija", method = [RequestMethod.PUT, RequestMethod.POST])
    fun oppija(method: HttpMethod, @RequestBody oppija: Oppija): ResponseEntity<Any> {
        logger.info { "Mock Koski received $method body: $oppija" }

        when (method) {
            HttpMethod.POST -> {
                if (oppija.opiskeluoikeudet.any { it.oid != null }) {
                    return ResponseEntity.badRequest()
                        .body("Trying to create a study right with OID")
                }
                oppija.opiskeluoikeudet.forEach { opiskeluOikeus ->
                    opiskeluOikeus.suoritukset.forEach {
                        if (it.toimipiste.oid == UNIT_OID_THAT_TRIGGERS_400) {
                            return ResponseEntity.badRequest().body("Simulated bad request")
                        }
                    }
                }
            }
            HttpMethod.PUT ->
                if (
                    oppija.opiskeluoikeudet
                        .mapNotNull { it.oid }
                        .any { !studyRights.containsKey(it) }
                ) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(
                            listOf(
                                KoskiClient.Error(
                                    key = "notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia",
                                    message = "Not found",
                                )
                            )
                        )
                }
            else -> error("Unsupported operation type $method")
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
                // some fields
                // that are missing in our data class -based representation to simulate a real
                // response more accurately
                jsonMapper.createObjectNode().apply {
                    putObject("henkilö").apply { put("oid", personOid) }
                    putArray("opiskeluoikeudet").apply {
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
                                            opiskeluoikeus = it.copy(oid = studyRightOid),
                                        )
                                    personStudyRights.get(personOid).add(studyRightOid)
                                }

                                jsonMapper.createObjectNode().apply {
                                    put("oid", studyRightOid)
                                    put("versionumero", version)
                                    putObject("lähdejärjestelmänId").apply {
                                        put("id", it.lähdejärjestelmänId.id.toString())
                                        putObject("lähdejärjestelmä").apply {
                                            put(
                                                "koodiarvo",
                                                it.lähdejärjestelmänId.lähdejärjestelmä.koodiarvo,
                                            )
                                            put(
                                                "koodistoUri",
                                                it.lähdejärjestelmänId.lähdejärjestelmä.koodistoUri,
                                            )
                                            put("koodistoVersio", 1)
                                            putObject("nimi").apply { put("fi", "EvakaEspoo") }
                                            putObject("lyhytNimi").apply { put("fi", "EvakaEspoo") }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        return ResponseEntity.ok(response)
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

    companion object {
        const val UNIT_OID_THAT_TRIGGERS_400 = "SIMULATE_BAD_REQUEST"
    }
}
