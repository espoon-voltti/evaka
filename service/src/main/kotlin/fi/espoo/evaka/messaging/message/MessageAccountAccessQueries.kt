// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import org.jdbi.v3.core.kotlin.mapTo

fun Database.Read.hasPermissionForMessageDraft(user: AuthenticatedUser.Employee, draftId: MessageDraftId): Boolean =
    this.createQuery(
        """
SELECT 1 
FROM message_draft draft
JOIN message_account_access_view access ON access.account_id = draft.account_id
WHERE
    draft.id = :draftId AND
    access.employee_id = :employeeId
        """.trimIndent()
    )
        .bind("employeeId", user.id)
        .bind("draftId", draftId)
        .mapTo<Int>()
        .any()

fun Database.Read.hasPermissionForAttachmentThroughMessageContent(user: AuthenticatedUser.Citizen, attachmentId: AttachmentId): Boolean =
    this.createQuery(
        """
SELECT 1 
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account ma ON ma.id = msg.sender_id OR ma.id = rec.recipient_id
WHERE att.id = :attachmentId AND ma.person_id = :personId
        """.trimIndent()
    )
        .bind("personId", user.id)
        .bind("attachmentId", attachmentId)
        .mapTo<Int>()
        .any()

fun Database.Read.hasPermissionForAttachmentThroughMessageContent(user: AuthenticatedUser.Employee, attachmentId: AttachmentId): Boolean =
    this.createQuery(
        """
SELECT 1 
FROM attachment att
JOIN message_content content ON att.message_content_id = content.id
JOIN message msg ON content.id = msg.content_id
JOIN message_recipients rec ON msg.id = rec.message_id
JOIN message_account_access_view access ON access.account_id = msg.sender_id OR access.account_id = rec.recipient_id
WHERE att.id = :attachmentId AND access.employee_id = :employeeId
        """.trimIndent()
    )
        .bind("employeeId", user.id)
        .bind("attachmentId", attachmentId)
        .mapTo<Int>()
        .any()

fun Database.Read.hasPermissionForAttachmentThroughMessageDraft(user: AuthenticatedUser.Employee, attachmentId: AttachmentId): Boolean =
    this.createQuery(
        """
SELECT 1
FROM attachment att
JOIN message_draft draft ON att.message_draft_id = draft.id
JOIN message_account_access_view access ON access.account_id = draft.account_id
WHERE att.id = :attachmentId AND access.employee_id = :employeeId
        """.trimIndent()
    )
        .bind("employeeId", user.id)
        .bind("attachmentId", attachmentId)
        .mapTo<Int>()
        .any()
