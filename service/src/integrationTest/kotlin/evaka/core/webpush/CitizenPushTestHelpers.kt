// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.db.Database
import evaka.core.user.DeviceClass
import evaka.core.user.ParsedUserAgent
import java.net.URI
import java.security.SecureRandom

fun mockWebPushEndpoint(httpPort: Int, id: String = "1234"): URI =
    URI("http://localhost:$httpPort/public/mock-web-push/subscription/$id")

fun Database.Transaction.insertTestCitizenPushSubscription(
    person: PersonId,
    endpoint: URI,
): CitizenPushDevice =
    insertCitizenPushSubscription(
        person,
        WebPushSubscription(
            endpoint = endpoint,
            expires = null,
            authSecret = listOf(0x00, 0x11, 0x22, 0x33),
            ecdhKey =
                WebPushCrypto.encode(WebPushCrypto.generateKeyPair(SecureRandom()).publicKey)
                    .toList(),
        ),
        installed = true,
        client = ParsedUserAgent(DeviceClass.PHONE, "iOS", "Safari"),
    )

/** The notifications planned so far, whether or not their jobs have run */
fun Database.Read.getPlannedCitizenPushNotifications(): List<CitizenPushNotification> =
    createQuery {
        sql(
            "SELECT payload FROM async_job WHERE type = 'SendCitizenPushNotification' ORDER BY submitted_at, id"
        )
    }
    .map { jsonColumn<AsyncJob.SendCitizenPushNotification>("payload").notification }
    .toList()
