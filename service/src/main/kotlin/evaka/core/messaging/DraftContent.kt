// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.messaging

import evaka.core.attachment.Attachment
import evaka.core.shared.Id
import evaka.core.shared.MessageDraftId
import evaka.core.shared.domain.HelsinkiDateTime
import org.jdbi.v3.json.Json

data class DraftRecipient(val accountId: Id<*>, val starter: Boolean)

data class DraftContent(
    val id: MessageDraftId,
    val createdAt: HelsinkiDateTime,
    val type: MessageType,
    val title: String,
    val content: String,
    val urgent: Boolean,
    val sensitive: Boolean,
    @Json val recipients: Set<DraftRecipient>,
    val recipientNames: List<String>,
    @Json val attachments: List<Attachment>,
)

data class UpdatableDraftContent(
    val type: MessageType,
    val title: String,
    val content: String,
    val urgent: Boolean,
    val sensitive: Boolean,
    val recipients: Set<DraftRecipient>,
    val recipientNames: List<String>,
)
