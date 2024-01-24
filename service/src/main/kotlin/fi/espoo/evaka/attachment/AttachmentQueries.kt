// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import kotlin.reflect.full.declaredMemberProperties

data class AttachmentForeignKeys(
    val applicationId: ApplicationId?,
    val feeAlterationId: FeeAlterationId?,
    val incomeId: IncomeId?,
    val incomeStatementId: IncomeStatementId?,
    val messageContentId: MessageContentId?,
    val messageDraftId: MessageDraftId?,
    val pedagogicalDocumentId: PedagogicalDocumentId?,
) {
    constructor(
        parent: AttachmentParent
    ) : this(
        applicationId = (parent as? AttachmentParent.Application)?.applicationId,
        feeAlterationId = (parent as? AttachmentParent.FeeAlteration)?.feeAlterationId,
        incomeId = (parent as? AttachmentParent.Income)?.incomeId,
        incomeStatementId = (parent as? AttachmentParent.IncomeStatement)?.incomeStatementId,
        messageContentId = (parent as? AttachmentParent.MessageContent)?.messageContentId,
        messageDraftId = (parent as? AttachmentParent.MessageDraft)?.draftId,
        pedagogicalDocumentId =
            (parent as? AttachmentParent.PedagogicalDocument)?.pedagogicalDocumentId,
    )

    init {
        require(allIds().filterNotNull().size <= 1) { "Expected 0-1 ids, got $this" }
    }

    private fun allIds(): List<Id<*>?> =
        listOf(
            applicationId,
            feeAlterationId,
            incomeId,
            incomeStatementId,
            messageContentId,
            messageDraftId,
            pedagogicalDocumentId
        )

    fun parent(): AttachmentParent =
        when {
            applicationId != null -> AttachmentParent.Application(applicationId)
            feeAlterationId != null -> AttachmentParent.FeeAlteration(feeAlterationId)
            incomeId != null -> AttachmentParent.Income(incomeId)
            incomeStatementId != null -> AttachmentParent.IncomeStatement(incomeStatementId)
            messageContentId != null -> AttachmentParent.MessageContent(messageContentId)
            messageDraftId != null -> AttachmentParent.MessageDraft(messageDraftId)
            pedagogicalDocumentId != null ->
                AttachmentParent.PedagogicalDocument(pedagogicalDocumentId)
            allIds().all { it == null } -> AttachmentParent.None
            else -> error("Unhandled AttachmentParent type")
        }

    companion object {
        val idFieldCount = AttachmentForeignKeys(AttachmentParent.None).allIds().size

        init {
            check(AttachmentForeignKeys::class.declaredMemberProperties.size == idFieldCount) {
                "AttachmentForeignKeys.allIds() must include all id fields"
            }
        }
    }
}

fun Database.Transaction.insertAttachment(
    user: AuthenticatedUser,
    now: HelsinkiDateTime,
    name: String,
    contentType: String,
    attachTo: AttachmentParent,
    type: AttachmentType?
): AttachmentId {
    check(AttachmentForeignKeys.idFieldCount == 7) {
        "Unexpected AttachmentForeignKeys field count"
    }
    require(attachTo !is AttachmentParent.MessageContent) { "attachments are saved via draft" }
    val sql =
        """
        INSERT INTO attachment (created, name, content_type, application_id, income_statement_id, income_id, message_draft_id, pedagogical_document_id, fee_alteration_id, uploaded_by, type)
        VALUES (:now, :name, :contentType, :applicationId, :incomeStatementId, :incomeId, :messageDraftId, :pedagogicalDocumentId, :feeAlterationId, :userId, :type)
        RETURNING id
        """
    return this.createUpdate(sql)
        .bind("now", now)
        .bind("name", name)
        .bind("contentType", contentType)
        .bindKotlin(AttachmentForeignKeys(attachTo))
        .bind("userId", user.evakaUserId)
        .bind("type", type ?: "")
        .executeAndReturnGeneratedKeys()
        .exactlyOne<AttachmentId>()
}

fun Database.Read.getAttachment(id: AttachmentId): Attachment? =
    createQuery<Any> {
            check(AttachmentForeignKeys.idFieldCount == 7) {
                "Unexpected AttachmentForeignKeys field count"
            }
            sql(
                """
        SELECT
            id, name, content_type, uploaded_by,
            application_id,
            fee_alteration_id,
            income_id,
            income_statement_id,
            message_content_id,
            message_draft_id,
            pedagogical_document_id
        FROM attachment
        WHERE id = ${bind(id)}
        """
            )
        }
        .exactlyOneOrNull {
            Attachment(
                id = column("id"),
                name = column("name"),
                contentType = column("content_type"),
                attachedTo = row<AttachmentForeignKeys>().parent()
            )
        }

fun Database.Transaction.dissociateAttachmentsByApplicationAndType(
    applicationId: ApplicationId,
    type: AttachmentType,
    userId: EvakaUserId
): List<AttachmentId> =
    createQuery(
            """
            UPDATE attachment
            SET application_id = NULL
            WHERE application_id = :applicationId 
            AND type = :type 
            AND uploaded_by = :userId
            RETURNING id
        """
        )
        .bind("applicationId", applicationId)
        .bind("type", type)
        .bind("userId", userId)
        .toList<AttachmentId>()

/** Changes the parent of *all attachments* that match the given predicate */
private fun Database.Transaction.changeParent(
    newParent: AttachmentParent,
    predicate: Predicate<DatabaseTable.Attachment>
): Int =
    createUpdate<DatabaseTable.Attachment> {
            val fks = AttachmentForeignKeys(newParent)
            check(AttachmentForeignKeys.idFieldCount == 7) {
                "Unexpected AttachmentForeignKeys field count"
            }
            sql(
                """
UPDATE attachment
SET
    application_id = ${bind(fks.applicationId)},
    fee_alteration_id = ${bind(fks.feeAlterationId)},
    income_id = ${bind(fks.incomeId)},
    income_statement_id = ${bind(fks.incomeStatementId)},
    message_content_id = ${bind(fks.messageContentId)},
    message_draft_id = ${bind(fks.messageDraftId)},
    pedagogical_document_id = ${bind(fks.pedagogicalDocumentId)}
WHERE ${predicate(predicate.forTable("attachment"))}
"""
            )
        }
        .execute()

/**
 * Associates *orphan* attachments with a new parent.
 *
 * If any of the given attachments is not found or already has a parent, this function fails with an
 * error.
 */
fun Database.Transaction.associateOrphanAttachments(
    uploadedBy: EvakaUserId,
    newParent: AttachmentParent,
    attachments: Collection<AttachmentId>
) {
    val isOrphan = AttachmentParent.None.toPredicate()
    val numRows =
        changeParent(
            newParent = newParent,
            Predicate<DatabaseTable.Attachment> {
                    where(
                        "$it.uploaded_by = ${bind(uploadedBy)} AND $it.id = ANY(${bind(attachments)})"
                    )
                }
                .and(isOrphan)
        )
    if (numRows != attachments.size) {
        throw BadRequest("Cannot associate all requested attachments")
    }
}

/** Dissociates all attachments from the given parent, so that they become orphans. */
fun Database.Transaction.dissociateAttachmentsOfParent(
    uploadedBy: EvakaUserId,
    parent: AttachmentParent
): Int =
    changeParent(
        newParent = AttachmentParent.None,
        Predicate<DatabaseTable.Attachment> { where("$it.uploaded_by = ${bind(uploadedBy)}") }
            .and(parent.toPredicate())
    )

private fun AttachmentParent.toPredicate() =
    Predicate<DatabaseTable.Attachment> {
        check(AttachmentForeignKeys.idFieldCount == 7) {
            "Unexpected AttachmentForeignKeys field count"
        }
        when (this@toPredicate) {
            is AttachmentParent.Application -> where("$it.application_id = ${bind(applicationId)}")
            is AttachmentParent.FeeAlteration ->
                where("$it.fee_alteration_id = ${bind(feeAlterationId)}")
            is AttachmentParent.Income -> where("$it.income_id = ${bind(incomeId)}")
            is AttachmentParent.IncomeStatement ->
                where("$it.income_statement_id = ${bind(incomeStatementId)}")
            is AttachmentParent.MessageContent ->
                where("$it.message_content_id = ${bind(messageContentId)}")
            is AttachmentParent.MessageDraft -> where("$it.message_draft_id = ${bind(draftId)}")
            is AttachmentParent.PedagogicalDocument ->
                where("$it.pedagogical_document_id = ${bind(pedagogicalDocumentId)}")
            is AttachmentParent.None ->
                where(
                    """
$it.application_id IS NULL
AND $it.fee_alteration_id IS NULL
AND $it.income_id IS NULL
AND $it.income_statement_id IS NULL
AND $it.message_content_id IS NULL
AND $it.message_draft_id IS NULL
AND $it.pedagogical_document_id IS NULL
            """
                )
        }
    }

fun Database.Read.userAttachmentCount(userId: EvakaUserId, parent: AttachmentParent): Int =
    createQuery<Any> {
            sql(
                """
SELECT count(*)
FROM attachment
WHERE uploaded_by = ${bind(userId)}
AND ${predicate(parent.toPredicate().forTable("attachment"))}
"""
            )
        }
        .exactlyOne<Int>()

fun Database.Read.getOrphanAttachments(olderThan: HelsinkiDateTime): List<AttachmentId> =
    createQuery<DatabaseTable.Attachment> {
            sql(
                """
SELECT id
FROM attachment
WHERE created < ${bind(olderThan)}
AND ${predicate(AttachmentParent.None.toPredicate().forTable("attachment"))}
"""
            )
        }
        .toList()
