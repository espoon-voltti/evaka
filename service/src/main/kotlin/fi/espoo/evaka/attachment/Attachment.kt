// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.attachment

import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.FeeAlterationId
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeStatementId
import fi.espoo.evaka.shared.MessageContentId
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.PedagogicalDocumentId

/*
If you add a new parent type, remember to:
- add a foreign key constraint with ON DELETE SET NULL for the new id column
- update the check constraint to include the new id column
- update the orphan index to include the new id column in the WHERE condition
- add a *partial index* for the new id column
- update the orphan attachment deletion scheduled job so that it looks at the new id
 */
sealed class AttachmentParent {
    data class Application(val applicationId: ApplicationId) : AttachmentParent()

    data class IncomeStatement(val incomeStatementId: IncomeStatementId) : AttachmentParent()

    data class Income(val incomeId: IncomeId) : AttachmentParent()

    data class MessageDraft(val draftId: MessageDraftId) : AttachmentParent()

    data class MessageContent(val messageContentId: MessageContentId) : AttachmentParent()

    data class PedagogicalDocument(val pedagogicalDocumentId: PedagogicalDocumentId) :
        AttachmentParent()

    data class FeeAlteration(val feeAlterationId: FeeAlterationId) : AttachmentParent()

    data object None : AttachmentParent()
}

data class Attachment(
    val id: AttachmentId,
    val name: String,
    val contentType: String,
    val attachedTo: AttachmentParent
)

data class IncomeAttachment(val id: AttachmentId, val name: String, val contentType: String)

data class MessageAttachment(val id: AttachmentId, val name: String, val contentType: String)

enum class AttachmentType {
    URGENCY,
    EXTENDED_CARE
}
