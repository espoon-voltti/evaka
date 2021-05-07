// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.application.Attachment
import fi.espoo.evaka.application.AttachmentType
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertAttachment(
    id: UUID,
    name: String,
    contentType: String,
    applicationId: UUID,
    uploadedByEnduser: UUID?,
    uploadedByEmployee: UUID?,
    type: AttachmentType
) {
    // language=sql
    val sql =
        """
        INSERT INTO attachment (id, name, content_type, application_id, uploaded_by_person, uploaded_by_employee, type)
        VALUES (:id, :name, :contentType, :applicationId, :uploadedByEnduser, :uploadedByEmployee, :type)
        """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", name)
        .bind("contentType", contentType)
        .bind("applicationId", applicationId)
        .bind("uploadedByEnduser", uploadedByEnduser)
        .bind("uploadedByEmployee", uploadedByEmployee)
        .bind("type", type)
        .execute()
}

fun Database.Read.getAttachment(id: UUID): Attachment? = this
    .createQuery("SELECT * FROM attachment WHERE id = :id")
    .bind("id", id).mapTo<Attachment>()
    .first()

fun Database.Read.isOwnAttachment(attachmentId: UUID, user: AuthenticatedUser): Boolean {
    val sql =
        """
        SELECT EXISTS 
            (SELECT 1 FROM attachment 
             WHERE id = :attachmentId 
             AND application_id IN (SELECT id FROM application WHERE guardian_id = :guardianId))
        """.trimIndent()

    return this.createQuery(sql)
        .bind("attachmentId", attachmentId)
        .bind("guardianId", user.id)
        .mapTo<Boolean>()
        .first()
}

fun Database.Read.isSelfUploadedAttachment(attachmentId: UUID, user: AuthenticatedUser): Boolean {
    val sql =
        """
        SELECT EXISTS
            (SELECT 1 FROM attachment
             WHERE id = :attachmentId
             AND (uploaded_by_employee = :userId OR uploaded_by_person = :userId))
        """.trimIndent()

    return this.createQuery(sql)
        .bind("attachmentId", attachmentId)
        .bind("userId", user.id)
        .mapTo<Boolean>()
        .first()
}

fun Database.Transaction.deleteAttachment(id: UUID) {
    this.createUpdate("DELETE FROM attachment WHERE id = :id")
        .bind("id", id)
        .execute()
}

fun Database.Transaction.deleteAttachmentsByApplicationAndType(applicationId: UUID, type: AttachmentType): List<UUID> {
    return this.createQuery("DELETE FROM attachment WHERE application_id = :applicationId AND type = :type RETURNING id")
        .bind("applicationId", applicationId)
        .bind("type", type)
        .mapTo<UUID>()
        .toList()
}
