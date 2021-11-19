// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getDevice(id: MobileDeviceId): MobileDevice {
    return createQuery(
        """
SELECT
    md.id, md.name, md.employee_id,
    CASE
        WHEN md.unit_id IS NOT NULL THEN ARRAY[md.unit_id]
        ELSE coalesce(array_agg(acv.daycare_id) FILTER (WHERE acv.daycare_id IS NOT NULL), '{}')
    END AS unit_ids,
    md.employee_id IS NOT NULL AS personal_device
FROM mobile_device md
LEFT JOIN daycare_acl_view acv ON md.employee_id = acv.employee_id
WHERE id = :id AND deleted = false
GROUP BY md.id, md.name, md.employee_id
"""
    )
        .bind("id", id)
        .mapTo<MobileDevice>()
        .firstOrNull() ?: throw NotFound("Device $id not found")
}

fun Database.Read.getDeviceByToken(token: UUID): MobileDeviceIdentity = createQuery(
    "SELECT id, long_term_token FROM mobile_device WHERE long_term_token = :token AND deleted = false"
).bind("token", token).mapTo<MobileDeviceIdentity>().singleOrNull()
    ?: throw NotFound("Device not found with token $token")

fun Database.Read.listSharedDevices(unitId: DaycareId): List<SharedMobileDevice> {
    return createQuery("SELECT id, name FROM mobile_device WHERE unit_id = :unitId AND deleted = false")
        .bind("unitId", unitId)
        .mapTo<SharedMobileDevice>()
        .list()
}

fun Database.Transaction.renameDevice(id: MobileDeviceId, name: String) {
    // language=sql
    val deviceUpdate = "UPDATE mobile_device SET name = :name WHERE id = :id"
    createUpdate(deviceUpdate)
        .bind("id", id)
        .bind("name", name)
        .updateExactlyOne(notFoundMsg = "Device $id not found")
}

fun Database.Transaction.softDeleteDevice(id: MobileDeviceId) {
    // language=sql
    val sql = "UPDATE mobile_device SET deleted = true WHERE id = :id"
    createUpdate(sql).bind("id", id).execute()
}
