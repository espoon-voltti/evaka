// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import java.net.URI
import java.time.Duration
import okhttp3.OkHttpClient
import tools.jackson.databind.json.JsonMapper

class ConfiguredHttpClient(val client: OkHttpClient, val rootUrl: URI?, val jsonMapper: JsonMapper?)

object OkHttpClientFactory {

    data class TimeoutConfig(
        val connectTimeout: Duration = Duration.ofSeconds(30),
        val readTimeout: Duration = Duration.ofSeconds(30),
        val writeTimeout: Duration = Duration.ofSeconds(30),
        val callTimeout: Duration? = null,
    )

    class Builder {
        private var timeoutConfig = TimeoutConfig()
        private val interceptors = mutableListOf<okhttp3.Interceptor>()
        private var rootUrl: URI? = null
        private var jsonMapper: JsonMapper? = null

        fun timeouts(
            connectTimeout: Duration = Duration.ofSeconds(30),
            readTimeout: Duration = Duration.ofSeconds(30),
            writeTimeout: Duration = Duration.ofSeconds(30),
            callTimeout: Duration? = null,
        ) = apply {
            this.timeoutConfig =
                TimeoutConfig(connectTimeout, readTimeout, writeTimeout, callTimeout)
        }

        fun rootUrl(url: URI) = apply { this.rootUrl = url }

        fun jsonMapper(mapper: JsonMapper) = apply { this.jsonMapper = mapper }

        fun addInterceptor(interceptor: okhttp3.Interceptor) = apply {
            this.interceptors.add(interceptor)
        }

        fun build(): ConfiguredHttpClient {
            val builder =
                OkHttpClient.Builder()
                    .connectTimeout(timeoutConfig.connectTimeout)
                    .readTimeout(timeoutConfig.readTimeout)
                    .writeTimeout(timeoutConfig.writeTimeout)

            timeoutConfig.callTimeout?.let { builder.callTimeout(it) }

            interceptors.forEach { builder.addInterceptor(it) }

            return ConfiguredHttpClient(
                client = builder.build(),
                rootUrl = rootUrl,
                jsonMapper = jsonMapper,
            )
        }
    }

    fun builder(): Builder = Builder()
}
