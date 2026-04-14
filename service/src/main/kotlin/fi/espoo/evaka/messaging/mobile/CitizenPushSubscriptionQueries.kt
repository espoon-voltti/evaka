// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.shared.CitizenPushSubscriptionDeviceId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database

data class CitizenPushSubscription(
    val citizenId: PersonId,
    val deviceId: CitizenPushSubscriptionDeviceId,
    val expoPushToken: String,
)

fun Database.Transaction.upsertCitizenPushSubscription(
    citizenId: PersonId,
    deviceId: CitizenPushSubscriptionDeviceId,
    expoPushToken: String,
) {
    createUpdate {
            sql(
                """
                INSERT INTO citizen_push_subscription (citizen_id, device_id, expo_push_token)
                VALUES (${bind(citizenId)}, ${bind(deviceId)}, ${bind(expoPushToken)})
                ON CONFLICT (citizen_id, device_id)
                    DO UPDATE SET expo_push_token = EXCLUDED.expo_push_token
                """
            )
        }
        .execute()
}

fun Database.Transaction.deleteCitizenPushSubscription(
    citizenId: PersonId,
    deviceId: CitizenPushSubscriptionDeviceId,
) {
    createUpdate {
            sql(
                """
                DELETE FROM citizen_push_subscription
                WHERE citizen_id = ${bind(citizenId)} AND device_id = ${bind(deviceId)}
                """
            )
        }
        .execute()
}

fun Database.Read.getCitizenPushSubscription(
    citizenId: PersonId,
    deviceId: CitizenPushSubscriptionDeviceId,
): CitizenPushSubscription? =
    createQuery {
            sql(
                """
                SELECT citizen_id, device_id, expo_push_token
                FROM citizen_push_subscription
                WHERE citizen_id = ${bind(citizenId)} AND device_id = ${bind(deviceId)}
                """
            )
        }
        .exactlyOneOrNull<CitizenPushSubscription>()
