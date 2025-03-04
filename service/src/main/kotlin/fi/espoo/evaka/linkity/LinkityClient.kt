// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.linkity

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.espoo.evaka.LinkityEnv
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.varda.ensureTrailingSlash
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface LinkityClient {
    fun getShifts(period: FiniteDateRange): List<Shift>

    fun postWorkLogs(workLogs: Collection<WorkLog>)
}

class LinkityHttpClient(private val env: LinkityEnv, private val jsonMapper: JsonMapper) :
    LinkityClient {
    private val logger = KotlinLogging.logger {}

    private val httpClient =
        OkHttpClient.Builder()
            .connectTimeout(Duration.ofMinutes(1))
            .readTimeout(Duration.ofMinutes(1))
            .writeTimeout(Duration.ofMinutes(1))
            .build()

    override fun getShifts(period: FiniteDateRange): List<Shift> {
        val url =
            env.url
                .ensureTrailingSlash()
                .resolve("v1/shifts")
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?.addQueryParameter("fromDate", period.start.toString())
                ?.addQueryParameter("toDate", period.end.toString())
                ?.build() ?: throw IllegalArgumentException("Invalid Linkity URL")

        logger.debug { "Getting shifts from Linkity URL: $url" }

        val req = Request.Builder().url(url).get().build()

        return httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Failed to fetch shifts from Linkity. Status: ${response.code}"
                )
            }
            response.body?.string()?.let { json -> jsonMapper.readValue(json) }
                ?: throw IllegalStateException(
                    "Failed to fetch shifts from Linkity: empty response"
                )
        }
    }

    override fun postWorkLogs(workLogs: Collection<WorkLog>) {
        val url =
            env.url
                .ensureTrailingSlash()
                .resolve("v1/worklogs")
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?.build() ?: throw IllegalArgumentException("Invalid Linkity URL")

        logger.debug { "Posting work logs to Linkity URL: $url" }

        val req =
            Request.Builder()
                .url(url)
                .post(
                    jsonMapper
                        .writeValueAsString(workLogs)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

        return httpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException(
                    "Failed to send work logs to Linkity. Status: ${response.code}"
                )
            }
        }
    }
}
