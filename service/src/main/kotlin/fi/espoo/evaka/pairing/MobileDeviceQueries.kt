// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.pis.SystemController
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.util.UUID

fun Database.Read.getDevice(id: MobileDeviceId): MobileDeviceDetails {
    return createQuery {
            sql(
                """
SELECT
    md.id, md.name, md.employee_id,
    CASE
        WHEN md.unit_id IS NOT NULL THEN ARRAY[md.unit_id]
        ELSE coalesce(array_agg(acl.daycare_id) FILTER (WHERE acl.daycare_id IS NOT NULL), '{}')
    END AS unit_ids,
    md.employee_id IS NOT NULL AS personal_device
FROM mobile_device md
LEFT JOIN daycare_acl acl ON md.employee_id = acl.employee_id
WHERE id = ${bind(id)}
GROUP BY md.id, md.name, md.employee_id
"""
            )
        }
        .exactlyOneOrNull<MobileDeviceDetails>() ?: throw NotFound("Device $id not found")
}

fun Database.Read.getDeviceByToken(token: UUID): MobileDeviceIdentity =
    createQuery {
            sql(
                "SELECT id, long_term_token FROM mobile_device WHERE long_term_token = ${bind(token)}"
            )
        }
        .exactlyOneOrNull<MobileDeviceIdentity>()
        ?: throw NotFound("Device not found with token $token")

fun Database.Read.listSharedDevices(unitId: DaycareId): List<MobileDevice> {
    return createQuery { sql("SELECT id, name FROM mobile_device WHERE unit_id = ${bind(unitId)}") }
        .toList<MobileDevice>()
}

fun Database.Read.listPersonalDevices(employeeId: EmployeeId): List<MobileDevice> {
    return createQuery {
            sql("SELECT id, name FROM mobile_device WHERE employee_id = ${bind(employeeId)}")
        }
        .toList<MobileDevice>()
}

fun Database.Transaction.updateDeviceTracking(
    id: MobileDeviceId,
    lastSeen: HelsinkiDateTime,
    tracking: SystemController.MobileDeviceTracking,
) =
    createUpdate {
            sql(
                """
UPDATE mobile_device
SET last_seen = ${bind(lastSeen)}, user_agent = ${bind(tracking.userAgent)}
WHERE id = ${bind(id)}
"""
            )
        }
        .execute()

fun Database.Transaction.renameDevice(id: MobileDeviceId, name: String) {
    createUpdate { sql("UPDATE mobile_device SET name = ${bind(name)} WHERE id = ${bind(id)}") }
        .updateExactlyOne(notFoundMsg = "Device $id not found")
}

fun Database.Transaction.deleteDevice(id: MobileDeviceId) =
    createUpdate { sql("DELETE FROM mobile_device WHERE id = ${bind(id)}") }.execute()
