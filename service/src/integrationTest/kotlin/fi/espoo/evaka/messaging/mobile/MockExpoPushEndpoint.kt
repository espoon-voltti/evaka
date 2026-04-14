// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class MockExpoPushEndpoint : ExpoPushClient {
    data class SentMessage(
        val to: String,
        val title: String,
        val body: String,
        val data: Map<String, Any?>,
    )

    val sent: MutableList<SentMessage> = mutableListOf()
    var nextResponseIsDeviceNotRegistered: Boolean = false

    fun reset() {
        sent.clear()
        nextResponseIsDeviceNotRegistered = false
    }

    override fun send(
        to: String,
        title: String,
        body: String,
        data: Map<String, Any?>,
    ): ExpoPushClient.SendResult {
        sent.add(SentMessage(to = to, title = title, body = body, data = data))
        val deviceNotRegistered = nextResponseIsDeviceNotRegistered
        nextResponseIsDeviceNotRegistered = false
        return ExpoPushClient.SendResult(deviceNotRegistered = deviceNotRegistered)
    }
}
