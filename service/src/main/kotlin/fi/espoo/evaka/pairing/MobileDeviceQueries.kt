package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.updateExactlyOne
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun getDevice(tx: Database.Read, id: UUID): MobileDevice {
    // language=sql
    val sql = "SELECT * FROM mobile_device WHERE id = :id AND deleted = false"
    return tx.createQuery(sql)
        .bind("id", id)
        .mapTo<MobileDevice>()
        .firstOrNull() ?: throw NotFound("Device $id not found")
}

fun listDevices(tx: Database.Read, unitId: UUID): List<MobileDevice> {
    // language=sql
    val sql = "SELECT * FROM mobile_device WHERE unit_id = :unitId AND deleted = false"
    return tx.createQuery(sql)
        .bind("unitId", unitId)
        .mapTo<MobileDevice>()
        .list()
}

fun renameDevice(tx: Database.Transaction, id: UUID, name: String) {
    // language=sql
    val deviceUpdate = "UPDATE mobile_device SET name = :name WHERE id = :id"
    tx.createUpdate(deviceUpdate)
        .bind("id", id)
        .bind("name", name)
        .updateExactlyOne(notFoundMsg = "Device $id not found")

    // language=sql
    val employeeUpdate = "UPDATE employee SET first_name = :name WHERE id = :id"
    tx.createUpdate(employeeUpdate)
        .bind("id", id)
        .bind("name", name)
        .execute()
}

fun softDeleteDevice(tx: Database.Transaction, id: UUID) {
    // language=sql
    val sql = "UPDATE mobile_device SET deleted = true WHERE id = :id"
    tx.createUpdate(sql).bind("id", id).execute()
}
