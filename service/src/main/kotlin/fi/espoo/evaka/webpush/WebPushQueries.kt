// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

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
    ${bind(subscription.endpoint.toString())},
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

fun Database.Transaction.upsertPushGroup(
    now: HelsinkiDateTime,
    device: MobileDeviceId,
    group: GroupId
) =
    createUpdate<Any> {
            sql(
                """
INSERT INTO mobile_device_push_group (device, daycare_group, created_at)
VALUES (${bind(device)}, ${bind(group)}, ${bind(now)})
"""
            )
        }
        .execute()

fun Database.Transaction.deletePushSubscription(device: MobileDeviceId) =
    createUpdate<Any> {
            sql("""
DELETE FROM mobile_device_push_subscription WHERE device = ${bind(device)}
""")
        }
        .execute()

fun Database.Transaction.getOrRefreshToken(
    newToken: VapidJwt,
    minValidThreshold: HelsinkiDateTime
): VapidJwt {
    // Try to save a new token if there's none or it doesn't match our min valid threshold
    // requirement
    val savedNewToken =
        createQuery<Any> {
                sql(
                    """
INSERT INTO vapid_jwt (origin, public_key, jwt, expires_at)
VALUES (${bind(newToken.origin)}, ${bind(newToken.publicKey)}, ${bind(newToken.jwt)}, ${bind(newToken.expiresAt)})
ON CONFLICT (origin, public_key) DO UPDATE SET
    jwt = excluded.jwt,
    expires_at = excluded.expires_at
WHERE vapid_jwt.expires_at < ${bind(minValidThreshold)}
RETURNING origin, public_key, jwt, expires_at
"""
                )
            }
            .exactlyOneOrNull<VapidJwt?>()
    return savedNewToken
    // We didn't save anything -> there must be a valid token in the db
    ?: createQuery<Any> {
                sql(
                    """
SELECT origin, public_key, jwt, expires_at
FROM vapid_jwt
WHERE (origin, public_key) = (${bind(newToken.origin)}, ${bind(newToken.publicKey)})
"""
                )
            }
            .exactlyOne<VapidJwt>()
}
