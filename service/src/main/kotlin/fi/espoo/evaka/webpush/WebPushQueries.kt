// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.webpush

import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.actionrule.forTable

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

fun Database.Read.getPushCategories(device: MobileDeviceId): Set<PushNotificationCategory> =
    createQuery<Any> {
            sql(
                """
SELECT unnest(push_notification_categories)
FROM mobile_device
WHERE id = ${bind(device)}
"""
            )
        }
        .toSet<PushNotificationCategory>()

fun Database.Transaction.setPushCategories(
    device: MobileDeviceId,
    categories: Set<PushNotificationCategory>
) {
    createUpdate<Any> {
            sql(
                """
UPDATE mobile_device SET push_notification_categories = ${bind(categories)}
WHERE id = ${bind(device)}
"""
            )
        }
        .execute()
}

fun Database.Read.getPushGroups(
    filter: AccessControlFilter<GroupId>,
    device: MobileDeviceId
): Set<GroupId> =
    createQuery<Any> {
            sql(
                """
SELECT mdpg.daycare_group
FROM mobile_device_push_group mdpg
JOIN daycare_group dg ON mdpg.daycare_group = dg.id
WHERE device = ${bind(device)}
AND ${predicate(filter.forTable("dg"))}
"""
            )
        }
        .toSet<GroupId>()

fun Database.Transaction.setPushGroups(
    now: HelsinkiDateTime,
    device: MobileDeviceId,
    groups: Set<GroupId>
) {
    createUpdate<Any> {
            sql(
                """
WITH deleted AS (
    DELETE FROM mobile_device_push_group mdpg
    WHERE device = ${bind(device)}
    AND NOT daycare_group = ANY(${bind(groups)})
)
INSERT INTO mobile_device_push_group (device, daycare_group, created_at)
SELECT ${bind(device)}, daycare_group, ${bind(now)}
FROM unnest(${bind(groups)}) AS daycare_group
ON CONFLICT (device, daycare_group) DO NOTHING
"""
            )
        }
        .execute()
}
