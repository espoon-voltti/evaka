// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.koski

import evaka.core.KoskiEnv
import evaka.core.OphEnv
import evaka.core.shared.KoskiStudyRightId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.buildHttpClient
import evaka.core.shared.config.defaultJsonMapperBuilder
import evaka.core.shared.db.Database
import evaka.core.shared.utils.basicAuthInterceptor
import evaka.core.shared.utils.headerInterceptor
import evaka.core.shared.utils.post
import evaka.core.shared.utils.put
import fi.espoo.voltti.logging.loggers.error
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URI
import java.time.LocalDate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.readValue

private val logger = KotlinLogging.logger {}

class KoskiClient(
    private val env: KoskiEnv,
    private val ophEnv: OphEnv,
    asyncJobRunner: AsyncJobRunner<AsyncJob>?,
) {
    // Use a local Jackson instance so the configuration doesn't get changed accidentally if the
    // global defaults change.
    // This is important, because our payload diffing mechanism relies on the serialization format
    private val jsonMapper =
        defaultJsonMapperBuilder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

    private val httpClient =
        buildHttpClient(
            rootUrl = URI(env.url),
            jsonMapper = jsonMapper,
            interceptors =
                listOf(
                    basicAuthInterceptor(env.user, env.secret.value),
                    headerInterceptor("Accept", "application/json"),
                ),
        )

    init {
        asyncJobRunner?.registerHandler { db, clock, msg: AsyncJob.UploadToKoski ->
            uploadToKoski(db, msg, clock.today())
        }
    }

    private class UploadException(val statusCode: Int, message: String) :
        RuntimeException(message) {
        val isClientError: Boolean
            get() = statusCode in 400..499
    }

    data class Error(val key: String, val message: String) {
        fun isNotFound() = key == "notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia"
    }

    fun uploadToKoski(db: Database.Connection, msg: AsyncJob.UploadToKoski, today: LocalDate) =
        try {
            db.transaction { tx -> uploadToKoski(tx, msg, today) }
        } catch (error: UploadException) {
            if (error.isClientError) {
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
        today: LocalDate,
    ) {
        logger.info { "Koski upload ${msg.key}: starting" }
        val data =
            tx.beginKoskiUpload(
                env.sourceSystem,
                ophEnv.organizerOid,
                ophEnv.municipalityCode,
                msg.key,
                today,
                env.syncRangeStart,
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
            val callerIdHeaders =
                mapOf("Caller-Id" to "${data.organizationOid}.${env.municipalityCallerId}")

            val body = payload.toRequestBody("application/json".toMediaType())
            val response: HenkilönOpiskeluoikeusVersiot? =
                if (data.operation == KoskiOperation.CREATE) {
                    httpClient.post(
                        "oppija",
                        body = body,
                        headers = callerIdHeaders,
                        responseHandler = { handleUploadResponse(it, data, msg.key, payload) },
                    )
                } else {
                    httpClient.put(
                        "oppija",
                        body = body,
                        headers = callerIdHeaders,
                        responseHandler = { handleUploadResponse(it, data, msg.key, payload) },
                    )
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
                            ),
                    )
                )
            }
            logger.info { "Koski upload ${msg.key} ${data.operation}: finished" }
        }
    }

    private fun handleUploadResponse(
        httpResponse: okhttp3.Response,
        data: KoskiData,
        key: KoskiStudyRightKey,
        payload: String,
    ): HenkilönOpiskeluoikeusVersiot? {
        if (httpResponse.isSuccessful) {
            return jsonMapper.readValue<HenkilönOpiskeluoikeusVersiot>(httpResponse.body.string())
        }
        val statusCode = httpResponse.code
        val errorBody = httpResponse.body.string()
        val errors: List<Error>? =
            try {
                jsonMapper.readValue(errorBody)
            } catch (_: Exception) {
                null
            }
        if (data.operation == KoskiOperation.VOID && errors?.any { it.isNotFound() } == true) {
            logger.warn {
                "Koski upload $key ${data.operation}: 404 not found -> assuming study right is already voided and nothing needs to be done"
            }
            return null
        }
        val meta =
            mapOf(
                "method" to httpResponse.request.method,
                "url" to httpResponse.request.url.toString(),
                "body" to payload,
                "errorMessage" to errorBody,
            )
        val uploadException =
            UploadException(
                statusCode,
                "Koski upload $key ${data.operation}: failed, status $statusCode",
            )
        logger.error(uploadException, meta) {
            "Koski upload $key ${data.operation}: failed, status $statusCode"
        }
        throw uploadException
    }
}
