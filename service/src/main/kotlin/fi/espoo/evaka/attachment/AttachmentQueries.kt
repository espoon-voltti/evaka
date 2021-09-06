// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.bindNullable
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

sealed interface AttachTo

data class AttachToApplication(val applicationId: ApplicationId) : AttachTo
data class AttachToIncomeStatement(val incomeStatementId: IncomeStatementId) : AttachTo
object AttachToNothing : AttachTo

fun Database.Transaction.insertAttachment(
    id: AttachmentId,
    name: String,
    contentType: String,
    attachTo: AttachTo,
    uploadedByEnduser: UUID?,
    uploadedByEmployee: UUID?,
    type: AttachmentType?
) {
    // language=sql
    val sql =
        """
        INSERT INTO attachment (id, name, content_type, application_id, income_statement_id, uploaded_by_person, uploaded_by_employee, type)
        VALUES (:id, :name, :contentType, :applicationId, :incomeStatementId, :uploadedByEnduser, :uploadedByEmployee, :type)
        """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", name)
        .bind("contentType", contentType)
        .let {
            when (attachTo) {
                is AttachToApplication ->
                    it
                        .bind("applicationId", attachTo.applicationId)
                        .bindNullable("incomeStatementId", null as IncomeStatementId?)
                is AttachToIncomeStatement ->
                    it
                        .bindNullable("applicationId", null as ApplicationId?)
                        .bind("incomeStatementId", attachTo.incomeStatementId)
                is AttachToNothing ->
                    it
                        .bindNullable("applicationId", null as ApplicationId?)
                        .bindNullable("incomeStatementId", null as IncomeStatementId?)
            }
        }
        .bind("uploadedByEnduser", uploadedByEnduser)
        .bind("uploadedByEmployee", uploadedByEmployee)
        .bind("type", type ?: "")
        .execute()
}

fun Database.Read.getAttachment(id: AttachmentId): Attachment? = this
    .createQuery(
        """
        SELECT id, name, content_type
        FROM attachment
        WHERE id = :id
        """
    )
    .bind("id", id)
    .mapTo<Attachment>()
    .firstOrNull()

fun Database.Read.isOwnAttachment(attachmentId: AttachmentId, user: AuthenticatedUser): Boolean {
    val sql =
        """
        SELECT EXISTS 
            (SELECT 1 FROM attachment 
             WHERE id = :attachmentId 
             AND (
               application_id IN (SELECT id FROM application WHERE guardian_id = :personId) OR 
               income_statement_id IN (SELECT id FROM income_statement WHERE person_id = :personId) OR
               (application_id IS NULL AND income_statement_id IS NULL AND uploaded_by_person = :personId)
             ))
        """.trimIndent()

    return this.createQuery(sql)
        .bind("attachmentId", attachmentId)
        .bind("personId", user.id)
        .mapTo<Boolean>()
        .first()
}

fun Database.Read.isSelfUploadedAttachment(attachmentId: AttachmentId, user: AuthenticatedUser): Boolean {
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

fun Database.Transaction.deleteAttachment(id: AttachmentId) {
    this.createUpdate("DELETE FROM attachment WHERE id = :id")
        .bind("id", id)
        .execute()
}

fun Database.Transaction.deleteAttachmentsByApplicationAndType(
    applicationId: ApplicationId,
    type: AttachmentType,
    userId: UUID
): List<AttachmentId> {
    return this.createQuery(
        """
            DELETE FROM attachment 
            WHERE application_id = :applicationId 
            AND type = :type 
            AND (uploaded_by_employee = :userId OR uploaded_by_person = :userId)
            RETURNING id
        """.trimIndent()
    )
        .bind("applicationId", applicationId)
        .bind("type", type)
        .bind("userId", userId)
        .mapTo<AttachmentId>()
        .toList()
}

fun Database.Transaction.associateAttachments(
    personId: UUID,
    incomeStatementId: IncomeStatementId,
    attachmentIds: List<AttachmentId>
) {
    val numRows = createUpdate(
        """
        UPDATE attachment SET income_statement_id = :incomeStatementId
        WHERE id = ANY(:attachmentIds)
          AND income_statement_id IS NULL and application_id IS NULL
          AND uploaded_by_person = :personId
        """.trimIndent()
    )
        .bind("incomeStatementId", incomeStatementId)
        .bind("attachmentIds", attachmentIds.toTypedArray())
        .bind("personId", personId)
        .execute()

    if (numRows != attachmentIds.size) {
        throw BadRequest("Cannot associate all requested attachments")
    }
}

fun Database.Transaction.dissociateAllPersonsAttachments(
    personId: UUID,
    incomeStatementId: IncomeStatementId,
) {
    createUpdate(
        """
        UPDATE attachment SET income_statement_id = NULL
        WHERE income_statement_id = :incomeStatementId
          AND uploaded_by_person = :personId
        """.trimIndent()
    )
        .bind("incomeStatementId", incomeStatementId)
        .bind("personId", personId)
        .execute()
}
