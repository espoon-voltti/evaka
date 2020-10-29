// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.core.extensions.jsonBody
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.UploadToKoski
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

@Component
class KoskiClient(
    private val jdbi: Jdbi,
    private val env: Environment,
    private val baseUrl: String = env.getRequiredProperty("fi.espoo.integration.koski.url"),
    private val sourceSystem: String = env.getRequiredProperty("fi.espoo.integration.koski.source_system"),
    private val koskiUser: String = env.getRequiredProperty("fi.espoo.integration.koski.user"),
    private val koskiSecret: String = env.getRequiredProperty("fi.espoo.integration.koski.secret"),
    asyncJobRunner: AsyncJobRunner?
) {
    // Use a local Jackson instance so the configuration doesn't get changed accidentally if the global defaults change.
    // This is important, because our payload diffing mechanism relies on the serialization format
    private val objectMapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(JaxbAnnotationModule())
        .registerModule(Jdk8Module())
        .registerModule(ParameterNamesModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    init {
        asyncJobRunner?.uploadToKoski = { h, msg -> uploadToKoski(h, msg, today = LocalDate.now()) }
    }

    fun uploadToKoski(h: Handle, msg: UploadToKoski, today: LocalDate) {
        logger.info { "Koski upload ${msg.key}: starting" }
        val data = h.beginKoskiUpload(sourceSystem, msg.key, today)
        if (data == null) {
            logger.info { "Koski upload ${msg.key}: no data -> skipping" }
            return
        }
        val payload = objectMapper.writeValueAsString(data.oppija)
        if (!h.isPayloadChanged(msg.key, payload)) {
            logger.info { "Koski upload ${msg.key} ${data.operation}: no change in payload -> skipping" }
        } else {
            val (_, _, result) = Fuel.request(
                method = if (data.operation == KoskiOperation.CREATE) Method.POST else Method.PUT,
                path = "$baseUrl/oppija"
            )
                .authentication()
                .basic(koskiUser, koskiSecret)
                .header(Headers.ACCEPT, "application/json")
                .header("Caller-Id", "${data.organizerOid}.espooevaka")
                .jsonBody(payload)
                .responseString()

            val response: HenkilönOpiskeluoikeusVersiot = try {
                objectMapper.readValue(result.get())
            } catch (error: FuelError) {
                logger.error(error) { "Koski upload ${msg.key} ${data.operation} failed: ${error.response}" }
                throw error
            }
            h.finishKoskiUpload(
                KoskiUploadResponse(
                    id = response.opiskeluoikeudet[0].lähdejärjestelmänId.id,
                    studyRightOid = response.opiskeluoikeudet[0].oid,
                    personOid = response.henkilö.oid,
                    version = response.opiskeluoikeudet[0].versionumero,
                    // We need to apply the returned OID back to the payload, or `isPayloadChanged` would always
                    // consider a freshly created study right as "outdated" because the original payload did not have the
                    // OID field
                    payload = objectMapper.writeValueAsString(
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
