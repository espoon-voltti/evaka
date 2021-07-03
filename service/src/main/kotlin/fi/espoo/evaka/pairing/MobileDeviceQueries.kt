// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Read.getDevice(id: UUID): MobileDevice {
    // language=sql
    val sql = "SELECT * FROM mobile_device WHERE id = :id AND deleted = false"
    return createQuery(sql)
        .bind("id", id)
        .mapTo<MobileDevice>()
        .firstOrNull() ?: throw NotFound("Device $id not found")
}

fun Database.Read.getDeviceByToken(token: UUID): MobileDeviceIdentity = createQuery(
    "SELECT id, long_term_token FROM mobile_device WHERE long_term_token = :token AND deleted = false"
).bind("token", token).mapTo<MobileDeviceIdentity>().singleOrNull()
    ?: throw NotFound("Device not found with token $token")

fun Database.Read.listDevices(unitId: DaycareId): List<MobileDevice> {
    // language=sql
    val sql = "SELECT * FROM mobile_device WHERE unit_id = :unitId AND deleted = false"
    return createQuery(sql)
        .bind("unitId", unitId)
        .mapTo<MobileDevice>()
        .list()
}

fun Database.Transaction.renameDevice(id: UUID, name: String) {
    // language=sql
    val deviceUpdate = "UPDATE mobile_device SET name = :name WHERE id = :id"
    createUpdate(deviceUpdate)
        .bind("id", id)
        .bind("name", name)
        .updateExactlyOne(notFoundMsg = "Device $id not found")

    // language=sql
    val employeeUpdate = "UPDATE employee SET first_name = :name WHERE id = :id"
    createUpdate(employeeUpdate)
        .bind("id", id)
        .bind("name", name)
        .execute()
}

fun Database.Transaction.softDeleteDevice(id: UUID) {
    // language=sql
    val sql = "UPDATE mobile_device SET deleted = true WHERE id = :id"
    createUpdate(sql).bind("id", id).execute()
}
