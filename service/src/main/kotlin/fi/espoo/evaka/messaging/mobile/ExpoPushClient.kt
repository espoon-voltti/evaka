// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.springframework.stereotype.Service
import tools.jackson.databind.json.JsonMapper

interface ExpoPushClient {
    data class SendResult(val deviceNotRegistered: Boolean)

    fun send(to: String, title: String, body: String, data: Map<String, Any?>): SendResult
}

@Service
class RealExpoPushClient(private val jsonMapper: JsonMapper) : ExpoPushClient {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val endpoint = URI("https://exp.host/--/api/v2/push/send")

    override fun send(
        to: String,
        title: String,
        body: String,
        data: Map<String, Any?>,
    ): ExpoPushClient.SendResult {
        val payload =
            mapOf(
                "to" to to,
                "title" to title,
                "body" to body,
                "data" to data,
                "sound" to "default",
                "priority" to "high",
            )
        val req =
            HttpRequest.newBuilder(endpoint)
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(payload)))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build()
        val response = http.send(req, HttpResponse.BodyHandlers.ofString())
        val parsed = jsonMapper.readTree(response.body())
        val status = parsed.path("data").path("status").asString()
        val error = parsed.path("data").path("details").path("error").asString()
        return ExpoPushClient.SendResult(
            deviceNotRegistered = status == "error" && error == "DeviceNotRegistered"
        )
    }
}
