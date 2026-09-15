// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.pis.NotificationCategory
import evaka.core.shared.CitizenPushSubscriptionId
import evaka.core.shared.PersonId
import evaka.core.shared.db.Database
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.user.DeviceClass
import evaka.core.user.ParsedUserAgent
import java.net.URI

data class CitizenPushDevice(
    val id: CitizenPushSubscriptionId,
    val installed: Boolean,
    val deviceClass: DeviceClass,
    val operatingSystemName: String,
    val agentName: String,
    val createdAt: HelsinkiDateTime,
    val lastSentAt: HelsinkiDateTime?,
)

fun Database.Read.getCitizenPushDevices(person: PersonId): List<CitizenPushDevice> = createQuery {
    sql(
        """
SELECT id, installed, device_class, operating_system_name, agent_name, created_at, last_sent_at
FROM citizen_push_subscription
WHERE person_id = ${bind(person)}
ORDER BY created_at
"""
    )
}
    .toList()

/**
 * Replaces whatever subscription the browser had, because an endpoint identifies one browser. If
 * another user subscribed earlier with the same browser, their notifications will not be delivered
 * anymore.
 */
fun Database.Transaction.insertCitizenPushSubscription(
    person: PersonId,
    subscription: WebPushSubscription,
    installed: Boolean,
    client: ParsedUserAgent,
): CitizenPushDevice {
    execute {
        sql(
            "DELETE FROM citizen_push_subscription WHERE endpoint = ${bind(subscription.endpoint.toString())}"
        )
    }
    return createUpdate {
        sql(
            """
INSERT INTO citizen_push_subscription (person_id, endpoint, auth_secret, ecdh_key, expires_at, installed, device_class, operating_system_name, agent_name)
VALUES (
    ${bind(person)},
    ${bind(subscription.endpoint.toString())},
    ${bind(subscription.authSecret.toByteArray())},
    ${bind(subscription.ecdhKey.toByteArray())},
    ${bind(subscription.expires)},
    ${bind(installed)},
    ${bind(client.deviceClass)},
    ${bind(client.operatingSystemName)},
    ${bind(client.agentName)}
)
RETURNING id, installed, device_class, operating_system_name, agent_name, created_at, last_sent_at
"""
        )
    }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
}

fun Database.Read.getCitizenPushSubscriptionId(
    person: PersonId,
    endpoint: URI,
): CitizenPushSubscriptionId? = createQuery {
    sql(
        """
SELECT id
FROM citizen_push_subscription
WHERE endpoint = ${bind(endpoint.toString())} AND person_id = ${bind(person)}
"""
    )
}
    .exactlyOneOrNull()

fun Database.Transaction.deleteForeignCitizenPushSubscription(
    person: PersonId,
    endpoint: URI,
): CitizenPushSubscriptionId? = createUpdate {
    sql(
        """
DELETE FROM citizen_push_subscription
WHERE endpoint = ${bind(endpoint.toString())} AND person_id <> ${bind(person)}
RETURNING id
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOneOrNull()

fun Database.Transaction.deleteCitizenPushDevice(
    person: PersonId,
    id: CitizenPushSubscriptionId,
): CitizenPushSubscriptionId? = createUpdate {
    sql(
        """
DELETE FROM citizen_push_subscription
WHERE id = ${bind(id)} AND person_id = ${bind(person)}
RETURNING id
"""
    )
}
    .executeAndReturnGeneratedKeys()
    .exactlyOneOrNull()

data class CitizenPushTarget(
    val endpoint: WebPushEndpoint,
    val disabledCategories: Set<NotificationCategory>,
)

fun Database.Read.getCitizenPushTarget(
    subscription: CitizenPushSubscriptionId
): CitizenPushTarget? = createQuery {
    sql(
        """
SELECT cps.endpoint, cps.auth_secret, cps.ecdh_key, p.disabled_push_types
FROM citizen_push_subscription cps
JOIN person p ON p.id = cps.person_id
WHERE cps.id = ${bind(subscription)}
"""
    )
}
    .exactlyOneOrNull {
        CitizenPushTarget(
            endpoint =
                WebPushEndpoint(
                    uri = column("endpoint"),
                    ecdhPublicKey = WebPushCrypto.decodePublicKey(column<ByteArray>("ecdh_key")),
                    authSecret = column("auth_secret"),
                ),
            disabledCategories = column("disabled_push_types"),
        )
    }

@IgnorableReturnValue
fun Database.Transaction.markCitizenPushSent(
    subscription: CitizenPushSubscriptionId,
    now: HelsinkiDateTime,
): Int = createUpdate {
    sql(
        "UPDATE citizen_push_subscription SET last_sent_at = ${bind(now)} WHERE id = ${bind(subscription)}"
    )
}
    .executeAndReturnCount()

@IgnorableReturnValue
fun Database.Transaction.deleteCitizenPushSubscription(
    subscription: CitizenPushSubscriptionId
): Int = createUpdate {
    sql("DELETE FROM citizen_push_subscription WHERE id = ${bind(subscription)}")
}
    .executeAndReturnCount()

fun Database.Read.citizenOwnsPushDevice(
    person: PersonId,
    id: CitizenPushSubscriptionId,
): Boolean = createQuery {
    sql(
        """
SELECT EXISTS(
    SELECT FROM citizen_push_subscription
    WHERE id = ${bind(id)} AND person_id = ${bind(person)}
)
"""
    )
}
    .exactlyOne()
