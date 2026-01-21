// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared

import java.net.URI
import java.time.Duration
import okhttp3.OkHttpClient
import tools.jackson.databind.json.JsonMapper

class ConfiguredHttpClient(val client: OkHttpClient, val rootUrl: URI?, val jsonMapper: JsonMapper?)

data class TimeoutConfig(
    val connectTimeout: Duration = Duration.ofSeconds(30),
    val readTimeout: Duration = Duration.ofSeconds(30),
    val writeTimeout: Duration = Duration.ofSeconds(30),
    val callTimeout: Duration? = null,
)

fun buildHttpClient(
    timeouts: TimeoutConfig = TimeoutConfig(),
    interceptors: List<okhttp3.Interceptor> = emptyList(),
    rootUrl: URI? = null,
    jsonMapper: JsonMapper? = null,
): ConfiguredHttpClient {
    val builder =
        OkHttpClient.Builder()
            .connectTimeout(timeouts.connectTimeout)
            .readTimeout(timeouts.readTimeout)
            .writeTimeout(timeouts.writeTimeout)

    timeouts.callTimeout?.let { builder.callTimeout(it) }

    interceptors.forEach { builder.addInterceptor(it) }

    return ConfiguredHttpClient(
        client = builder.build(),
        rootUrl = rootUrl,
        jsonMapper = jsonMapper,
    )
}
