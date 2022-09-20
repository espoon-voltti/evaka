// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import java.lang.IllegalArgumentException

fun Database.Transaction.insertAttachment(
    user: AuthenticatedUser,
    id: AttachmentId,
    name: String,
    contentType: String,
    attachTo: AttachmentParent,
    type: AttachmentType?
) {
    data class AttachmentParentColumn(
        val applicationId: ApplicationId? = null,
        val incomeStatementId: IncomeStatementId? = null,
        val incomeId: IncomeId? = null,
        val messageDraftId: MessageDraftId? = null,
        val pedagogicalDocumentId: PedagogicalDocumentId? = null
    )

    // language=sql
    val sql =
        """
        INSERT INTO attachment (id, name, content_type, application_id, income_statement_id, income_id, message_draft_id, pedagogical_document_id, uploaded_by, type)
        VALUES (:id, :name, :contentType, :applicationId, :incomeStatementId, :incomeId, :messageDraftId, :pedagogicalDocumentId, :userId, :type)
        """.trimIndent(
        )

    this.createUpdate(sql)
        .bind("id", id)
        .bind("name", name)
        .bind("contentType", contentType)
        .bindKotlin(
            when (attachTo) {
                is AttachmentParent.Application ->
                    AttachmentParentColumn(applicationId = attachTo.applicationId)
                is AttachmentParent.IncomeStatement ->
                    AttachmentParentColumn(incomeStatementId = attachTo.incomeStatementId)
                is AttachmentParent.Income -> AttachmentParentColumn(incomeId = attachTo.incomeId)
                is AttachmentParent.MessageDraft ->
                    AttachmentParentColumn(messageDraftId = attachTo.draftId)
                is AttachmentParent.None -> AttachmentParentColumn()
                is AttachmentParent.MessageContent ->
                    throw IllegalArgumentException("attachments are saved via draft")
                is AttachmentParent.PedagogicalDocument ->
                    AttachmentParentColumn(pedagogicalDocumentId = attachTo.pedagogicalDocumentId)
            }
        )
        .bind("userId", user.evakaUserId)
        .bind("type", type ?: "")
        .execute()
}

fun Database.Read.getAttachment(id: AttachmentId): Attachment? =
    this.createQuery(
            """
        SELECT id, name, content_type, uploaded_by, application_id, income_statement_id, message_draft_id, message_content_id, pedagogical_document_id
        FROM attachment
        WHERE id = :id
        """
        )
        .bind("id", id)
        .map { row ->
            val applicationId = row.mapColumn<ApplicationId?>("application_id")
            val incomeStatementId = row.mapColumn<IncomeStatementId?>("income_statement_id")
            val messageDraftId = row.mapColumn<MessageDraftId?>("message_draft_id")
            val messageContentId = row.mapColumn<MessageContentId?>("message_content_id")
            val pedagogicalDocumentId =
                row.mapColumn<PedagogicalDocumentId?>("pedagogical_document_id")
            val attachedTo =
                if (applicationId != null) AttachmentParent.Application(applicationId)
                else if (incomeStatementId != null)
                    AttachmentParent.IncomeStatement(incomeStatementId)
                else if (messageDraftId != null) AttachmentParent.MessageDraft(messageDraftId)
                else if (messageContentId != null) AttachmentParent.MessageContent(messageContentId)
                else if (pedagogicalDocumentId != null)
                    AttachmentParent.PedagogicalDocument(pedagogicalDocumentId)
                else AttachmentParent.None

            Attachment(
                id = row.mapColumn("id"),
                name = row.mapColumn("name"),
                contentType = row.mapColumn("content_type"),
                attachedTo = attachedTo,
            )
        }
        .firstOrNull()

fun Database.Transaction.deleteAttachment(id: AttachmentId) {
    this.createUpdate("DELETE FROM attachment WHERE id = :id").bind("id", id).execute()
}

fun Database.Transaction.deleteAttachmentsByApplicationAndType(
    applicationId: ApplicationId,
    type: AttachmentType,
    userId: EvakaUserId
): List<AttachmentId> {
    return this.createQuery(
            """
            DELETE FROM attachment 
            WHERE application_id = :applicationId 
            AND type = :type 
            AND uploaded_by = :userId
            RETURNING id
        """.trimIndent(
            )
        )
        .bind("applicationId", applicationId)
        .bind("type", type)
        .bind("userId", userId)
        .mapTo<AttachmentId>()
        .toList()
}

fun Database.Transaction.associateAttachments(
    personId: PersonId,
    incomeStatementId: IncomeStatementId,
    attachmentIds: List<AttachmentId>
) {
    val numRows =
        createUpdate(
                """
        UPDATE attachment SET income_statement_id = :incomeStatementId
        WHERE id = ANY(:attachmentIds)
          AND income_statement_id IS NULL and application_id IS NULL
          AND uploaded_by = :personId
        """.trimIndent(
                )
            )
            .bind("incomeStatementId", incomeStatementId)
            .bind("attachmentIds", attachmentIds)
            .bind("personId", personId)
            .execute()

    if (numRows != attachmentIds.size) {
        throw BadRequest("Cannot associate all requested attachments")
    }
}

fun Database.Transaction.associateIncomeAttachments(
    personId: EvakaUserId,
    incomeId: IncomeId,
    attachmentIds: List<AttachmentId>
) {
    val numRows =
        createUpdate(
                """
        UPDATE attachment SET income_id = :incomeId
        WHERE id = ANY(:attachmentIds)
          AND income_id IS NULL
          AND uploaded_by = :personId
        """.trimIndent(
                )
            )
            .bind("incomeId", incomeId)
            .bind("attachmentIds", attachmentIds)
            .bind("personId", personId)
            .execute()

    if (numRows < attachmentIds.size) {
        throw BadRequest("Cannot associate all requested attachments")
    }
}

fun Database.Transaction.dissociateAllPersonsAttachments(
    personId: PersonId,
    incomeStatementId: IncomeStatementId,
) {
    createUpdate(
            """
        UPDATE attachment SET income_statement_id = NULL
        WHERE income_statement_id = :incomeStatementId
          AND uploaded_by = :personId
        """.trimIndent(
            )
        )
        .bind("incomeStatementId", incomeStatementId)
        .bind("personId", personId)
        .execute()
}

fun Database.Read.userUnparentedAttachmentCount(userId: EvakaUserId): Int {
    return this.createQuery(
            """
        SELECT COUNT(*) FROM attachment
        WHERE application_id IS NULL
          AND income_statement_id IS NULL
          AND uploaded_by = :userId
        """
        )
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userApplicationAttachmentCount(
    applicationId: ApplicationId,
    userId: EvakaUserId
): Int {
    return this.createQuery(
            "SELECT COUNT(*) FROM attachment WHERE application_id = :applicationId AND uploaded_by = :userId"
        )
        .bind("applicationId", applicationId)
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userIncomeStatementAttachmentCount(
    incomeStatementId: IncomeStatementId,
    userId: EvakaUserId
): Int {
    return this.createQuery(
            "SELECT COUNT(*) FROM attachment WHERE income_statement_id = :incomeStatementId AND uploaded_by = :userId"
        )
        .bind("incomeStatementId", incomeStatementId)
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userIncomeAttachmentCount(incomeId: IncomeId, userId: EvakaUserId): Int {
    return this.createQuery(
            "SELECT COUNT(*) FROM attachment WHERE income_id = :incomeId AND uploaded_by = :userId"
        )
        .bind("incomeId", incomeId)
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}

fun Database.Read.userPedagogicalDocumentCount(
    pedagogicalDocumentId: PedagogicalDocumentId,
    userId: EvakaUserId
): Int {
    return this.createQuery(
            "SELECT COUNT(*) FROM attachment WHERE pedagogical_document_id = :pedagogicalDocumentId AND uploaded_by = :userId"
        )
        .bind("pedagogicalDocumentId", pedagogicalDocumentId)
        .bind("userId", userId)
        .mapTo<Int>()
        .first()
}
