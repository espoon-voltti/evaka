// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.shared.KoskiStudyRightId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.voltti.logging.loggers.error
import mu.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

class KoskiClient(
    private val env: KoskiEnv,
    private val ophEnv: OphEnv,
    private val fuel: FuelManager,
    asyncJobRunner: AsyncJobRunner<AsyncJob>?
) {
    // Use a local Jackson instance so the configuration doesn't get changed accidentally if the global defaults change.
    // This is important, because our payload diffing mechanism relies on the serialization format
    private val jsonMapper = jacksonMapperBuilder()
        .addModules(JavaTimeModule(), JaxbAnnotationModule(), Jdk8Module(), ParameterNamesModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .build()

    init {
        asyncJobRunner?.registerHandler { db, msg: AsyncJob.UploadToKoski -> uploadToKoski(db, msg, today = LocalDate.now()) }
    }

    fun uploadToKoski(db: Database.Connection, msg: AsyncJob.UploadToKoski, today: LocalDate) = db.transaction { tx ->
        logger.info { "Koski upload ${msg.key}: starting" }
        val data = tx.beginKoskiUpload(env.sourceSystem, ophEnv.organizerOid, msg.key, today)
        if (data == null) {
            logger.info { "Koski upload ${msg.key}: no data -> skipping" }
            return@transaction
        }
        val payload = jsonMapper.writeValueAsString(data.oppija)
        if (!tx.isPayloadChanged(msg.key, payload)) {
            logger.info { "Koski upload ${msg.key} ${data.operation}: no change in payload -> skipping" }
        } else {
            val (request, _, result) = fuel.request(
                method = if (data.operation == KoskiOperation.CREATE) Method.POST else Method.PUT,
                path = "${env.url}/oppija"
            )
                .authentication()
                .basic(env.user, env.secret.value)
                .header(Headers.ACCEPT, "application/json")
                .header("Caller-Id", "${data.organizationOid}.espooevaka")
                .jsonBody(payload)
                .responseString()

            val response: HenkilönOpiskeluoikeusVersiot = try {
                jsonMapper.readValue(result.get())
            } catch (error: FuelError) {
                val meta = mapOf(
                    "method" to request.method,
                    "url" to request.url,
                    "body" to request.body.asString("application/json"),
                    "errorMessage" to error.errorData.decodeToString()
                )
                logger.error(
                    error,
                    meta
                ) { "Koski upload ${msg.key} ${data.operation} failed, status ${error.response.statusCode}" }
                throw error
            }
            tx.finishKoskiUpload(
                KoskiUploadResponse(
                    id = KoskiStudyRightId(response.opiskeluoikeudet[0].lähdejärjestelmänId.id),
                    studyRightOid = response.opiskeluoikeudet[0].oid,
                    personOid = response.henkilö.oid,
                    version = response.opiskeluoikeudet[0].versionumero,
                    // We need to apply the returned OID back to the payload, or `isPayloadChanged` would always
                    // consider a freshly created study right as "outdated" because the original payload did not have the
                    // OID field
                    payload = jsonMapper.writeValueAsString(
                        data.oppija.copy(
                            opiskeluoikeudet = data.oppija.opiskeluoikeudet.zip(response.opiskeluoikeudet).map {
                                it.first.copy(
                                    oid = it.second.oid
                                )
                            }
                        )
                    )
                )
            )
            logger.info { "Koski upload ${msg.key} ${data.operation}: finished" }
        }
    }
}
