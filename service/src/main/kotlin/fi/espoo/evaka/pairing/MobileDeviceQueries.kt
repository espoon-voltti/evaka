package fi.espoo.evaka.pairing

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun insertDeviceData(tx: Database.Transaction, unitId: UUID): UUID {
    val defaultName = "Nimeämätön laite"

    // language=sql
    val employeeInsert =
        """
            INSERT INTO employee (first_name, last_name, email, aad_object_id)
            VALUES (:name, (SELECT name FROM daycare WHERE id = :unitId), null, null)
            RETURNING id
        """.trimIndent()
    val id = tx.createQuery(employeeInsert)
        .bind("unitId", unitId)
        .bind("name", defaultName)
        .mapTo<UUID>()
        .first()

    // language=sql
    val deviceInsert =
        """
            INSERT INTO mobile_device (id, unit_id, name) 
            VALUES (:id, :unitId, :name)
        """.trimIndent()
    tx.createUpdate(deviceInsert)
        .bind("id", id)
        .bind("unitId", unitId)
        .bind("name", defaultName)
        .execute()

    return id
}

fun listDevices(tx: Database.Read, unitId: UUID): List<MobileDevice> {
    // language=sql
    val sql = "SELECT * FROM mobile_device WHERE unit_id = :unitId AND deleted = false ORDER BY created"
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
        .execute().takeIf { it > 0 } ?: throw NotFound("Device $id not found")

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
