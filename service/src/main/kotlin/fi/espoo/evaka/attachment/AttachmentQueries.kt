// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.application.Attachment
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertAttachment(id: UUID, name: String, contentType: String, applicationId: UUID) {
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

fun Database.Transaction.deleteAttachment(id: UUID) {
    this.createUpdate("UPDATE attachment SET application_id = NULL WHERE id = :id")
        .bind("id", id)
        .execute()
}
