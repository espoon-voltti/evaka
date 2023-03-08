// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.db.Database

fun Database.Transaction.upsertPushSubscription(
    device: MobileDeviceId,
    subscription: WebPushSubscription
) =
    createUpdate<Any> {
            sql(
                """
INSERT INTO mobile_device_push_subscription (device, endpoint, expires, auth_secret, ecdh_key)
VALUES (
    ${bind(device)},
    ${bind(subscription.endpoint)},
    ${bind(subscription.expires)},
    ${bind(subscription.authSecret.toByteArray())},
    ${bind(subscription.ecdhKey.toByteArray())}
)
ON CONFLICT (device) DO UPDATE SET
    endpoint = EXCLUDED.endpoint,
    expires = EXCLUDED.expires,
    auth_secret = EXCLUDED.auth_secret,
    ecdh_key = EXCLUDED.ecdh_key
"""
            )
        }
        .execute()
