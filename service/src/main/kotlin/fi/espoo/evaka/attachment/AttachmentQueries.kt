// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

fun Database.Transaction.insertAttachment(
    id: AttachmentId,
    name: String,
    contentType: String,
    attachTo: AttachmentParent,
    uploadedByPerson: UUID?,
    uploadedByEmployee: UUID?,
    type: AttachmentType?
) {
    data class AttachmentParentColumn(
        val applicationId: ApplicationId? = null,
        val incomeStatementId: IncomeStatementId? = null,
        val messageDraftId: MessageDraftId? = null
    )

    // language=sql
    val sql =
        """
        INSERT INTO attachment (id, name, content_type, application_id, income_statement_id, message_draft_id, uploaded_by_person, uploaded_by_employee, type)
        VALUES (:id, :name, :contentType, :applicationId, :incomeStatementId, :messageDraftId, :uploadedByPerson, :uploadedByEmployee, :type)
        """.trimIndent()

    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", name)
        .bind("contentType", contentType)
        .bindKotlin(
            when (attachTo) {
                is AttachmentParent.Application -> AttachmentParentColumn(applicationId = attachTo.applicationId)
                is AttachmentParent.IncomeStatement -> AttachmentParentColumn(incomeStatementId = attachTo.incomeStatementId)
                is AttachmentParent.MessageDraft -> AttachmentParentColumn(messageDraftId = attachTo.draftId)
                is AttachmentParent.None -> AttachmentParentColumn()
            }
        )
        .bind("uploadedByPerson", uploadedByPerson)
        .bind("uploadedByEmployee", uploadedByEmployee)
        .bind("type", type ?: "")
        .execute()
}

fun Database.Read.getAttachment(id: AttachmentId): Attachment? = this
    .createQuery(
        """
        SELECT id, name, content_type, uploaded_by_employee, uploaded_by_person, application_id, income_statement_id
        FROM attachment
        WHERE id = :id
        """
    )
    .bind("id", id)
    .map { row ->
        val applicationId = row.mapColumn<ApplicationId?>("application_id")
        val incomeStatementId = row.mapColumn<IncomeStatementId?>("income_statement_id")
        val attachedTo =
            if (applicationId != null) AttachmentParent.Application(applicationId)
            else if (incomeStatementId != null) AttachmentParent.IncomeStatement(incomeStatementId)
            else AttachmentParent.None

        Attachment(
            id = row.mapColumn("id"),
            name = row.mapColumn("name"),
            contentType = row.mapColumn("content_type"),
            attachedTo = attachedTo,
        )
    }
    .firstOrNull()

fun Database.Read.isOwnAttachment(attachmentId: AttachmentId, user: AuthenticatedUser.Citizen): Boolean {
    val sql =
        """
        SELECT EXISTS 
            (SELECT 1 FROM attachment 
             WHERE id = :attachmentId 
             AND uploaded_by_person = :personId)
        """.trimIndent()

    return this.createQuery(sql)
        .bind("attachmentId", attachmentId)
        .bind("personId", user.id)
        .mapTo<Boolean>()
        .first()
}

fun Database.Read.isOwnAttachment(attachmentId: AttachmentId, user: AuthenticatedUser.Employee): Boolean {
    val sql =
        """
        SELECT EXISTS 
            (SELECT 1 FROM attachment 
             WHERE id = :attachmentId 
             AND uploaded_by_employee = :userId)
        """.trimIndent()

    return this.createQuery(sql)
        .bind("attachmentId", attachmentId)
        .bind("userId", user.id)
        .mapTo<Boolean>()
        .first()
}

fun Database.Read.wasUploadedByAnyEmployee(attachmentId: AttachmentId): Boolean {
    val sql =
        """
        SELECT EXISTS
            (SELECT 1 FROM attachment
             WHERE id = :attachmentId
             AND uploaded_by_employee IS NOT NULL)
        """.trimIndent()

    return this.createQuery(sql)
        .bind("attachmentId", attachmentId)
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

fun Database.Read.userUnparentedAttachmentCount(userId: UUID): Int {
    return this.createQuery(
        """
        SELECT COUNT(*) FROM attachment
        WHERE application_id IS NULL
          AND income_statement_id IS NULL
          AND uploaded_by_person = :userId
        """
    )
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userApplicationAttachmentCount(applicationId: ApplicationId, userId: UUID): Int {
    return this.createQuery("SELECT COUNT(*) FROM attachment WHERE application_id = :applicationId AND uploaded_by_person = :userId")
        .bind("applicationId", applicationId)
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userIncomeStatementAttachmentCount(incomeStatementId: IncomeStatementId, userId: UUID): Int {
    return this.createQuery("SELECT COUNT(*) FROM attachment WHERE income_statement_id = :incomeStatementId AND uploaded_by_person = :userId")
        .bind("incomeStatementId", incomeStatementId)
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}
