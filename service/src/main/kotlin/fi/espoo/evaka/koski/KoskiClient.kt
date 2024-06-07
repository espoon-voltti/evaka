// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.core.isClientError
import fi.espoo.evaka.KoskiEnv
import fi.espoo.evaka.OphEnv
import fi.espoo.evaka.shared.KoskiStudyRightId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.config.defaultJsonMapperBuilder
import fi.espoo.evaka.shared.db.Database
import fi.espoo.voltti.logging.loggers.error
import java.time.LocalDate
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class KoskiClient(
    private val env: KoskiEnv,
    private val ophEnv: OphEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>?
) {
    // Use a local Jackson instance so the configuration doesn't get changed accidentally if the
    // global defaults change.
    // This is important, because our payload diffing mechanism relies on the serialization format
    private val jsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    private val fuel = FuelManager()

    init {
        asyncJobRunner?.registerHandler { db, clock, msg: AsyncJob.UploadToKoski ->
            uploadToKoski(db, msg, clock.today())
        }
    }

    data class Error(val key: String, val message: String) {
        fun isNotFound() = key == "notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia"
    }

    fun uploadToKoski(db: Database.Connection, msg: AsyncJob.UploadToKoski, today: LocalDate) =
        try {
            db.transaction { tx -> uploadToKoski(tx, msg, today) }
        } catch (error: FuelError) {
            if (error.response.isClientError) {
                // No need to trigger alerts since this error will be visible in the Koski UI, and
                // we'll automatically retry again tomorrow.
                // The transaction has been rolled back, and since we don't propagate the exception,
                // the async job will be marked completed.
            } else {
                throw error
            }
        }

    private fun uploadToKoski(
        tx: Database.Transaction,
        msg: AsyncJob.UploadToKoski,
        today: LocalDate
    ) {
        logger.info { "Koski upload ${msg.key}: starting" }
        val data =
            tx.beginKoskiUpload(
                env.sourceSystem,
                ophEnv.organizerOid,
                ophEnv.municipalityCode,
                msg.key,
                today
            )
        if (data == null) {
            logger.info { "Koski upload ${msg.key}: no data -> skipping" }
            return
        }
        val payload = jsonMapper.writeValueAsString(data.oppija)
        if (!tx.isPayloadChanged(msg.key, payload)) {
            logger.info {
                "Koski upload ${msg.key} ${data.operation}: no change in payload -> skipping"
            }
        } else {
            val (request, _, result) =
                fuel
                    .request(
                        method =
                            if (data.operation == KoskiOperation.CREATE) Method.POST
                            else Method.PUT,
                        path = "${env.url}/oppija"
                    )
                    .authentication()
                    .basic(env.user, env.secret.value)
                    .header(Headers.ACCEPT, "application/json")
                    .header("Caller-Id", "${data.organizationOid}.${env.municipalityCallerId}")
                    .jsonBody(payload)
                    .response()

            val response: HenkilönOpiskeluoikeusVersiot? =
                try {
                    jsonMapper.readValue<HenkilönOpiskeluoikeusVersiot>(result.get())
                } catch (error: FuelError) {
                    val errors: List<Error>? =
                        try {
                            jsonMapper.readValue(error.errorData)
                        } catch (err: Exception) {
                            null
                        }
                    if (
                        data.operation == KoskiOperation.VOID &&
                            errors?.any { it.isNotFound() } == true
                    ) {
                        logger.warn(
                            "Koski upload ${msg.key} ${data.operation}: 404 not found -> assuming study right is already voided and nothing needs to be done"
                        )
                        null
                    } else {
                        val meta =
                            mapOf(
                                "method" to request.method,
                                "url" to request.url,
                                "body" to request.body.asString("application/json"),
                                "errorMessage" to error.errorData.decodeToString()
                            )
                        logger.error(error, meta) {
                            "Koski upload ${msg.key} ${data.operation}: failed, status ${error.response.statusCode}"
                        }
                        throw error
                    }
                }
            if (response != null) {
                tx.finishKoskiUpload(
                    KoskiUploadResponse(
                        id = KoskiStudyRightId(response.opiskeluoikeudet[0].lähdejärjestelmänId.id),
                        studyRightOid = response.opiskeluoikeudet[0].oid,
                        personOid = response.henkilö.oid,
                        version = response.opiskeluoikeudet[0].versionumero,
                        // We need to apply the returned OID back to the payload, or
                        // `isPayloadChanged` would always
                        // consider a freshly created study right as "outdated" because the
                        // original payload did not have the
                        // OID field
                        payload =
                            jsonMapper.writeValueAsString(
                                data.oppija.copy(
                                    opiskeluoikeudet =
                                        data.oppija.opiskeluoikeudet
                                            .zip(response.opiskeluoikeudet)
                                            .map { it.first.copy(oid = it.second.oid) }
                                )
                            )
                    )
                )
            }
            logger.info { "Koski upload ${msg.key} ${data.operation}: finished" }
        }
    }
}
