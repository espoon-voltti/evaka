package fi.espoo.evaka.attachment

import org.jdbi.v3.core.Handle
import java.util.UUID

fun Handle.insertAttachment(id: UUID, name: String, contentType: String, applicationId: UUID) {
    // language=sql
    val sql =
        """
        INSERT INTO attachment (id, name, content_type, application_id)
        VALUES (:id, :name, :contentType, :applicationId)
        """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", name)
        .bind("contentType", contentType)
        .bind("applicationId", applicationId)
        .execute()
}
