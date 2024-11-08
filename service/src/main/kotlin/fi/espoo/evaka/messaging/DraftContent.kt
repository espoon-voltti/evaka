// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging

import fi.espoo.evaka.attachment.MessageAttachment
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.MessageDraftId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.json.Json

data class DraftContent(
    val id: MessageDraftId,
    val createdAt: HelsinkiDateTime,
    val type: MessageType,
    val title: String,
    val content: String,
    val urgent: Boolean,
    val sensitive: Boolean,
    val recipientIds: Set<Id<*>>,
    val recipientNames: List<String>,
    @Json val attachments: List<MessageAttachment>,
)

data class UpdatableDraftContent(
    val type: MessageType,
    val title: String,
    val content: String,
    val urgent: Boolean,
    val sensitive: Boolean,
    val recipientIds: Set<Id<*>>,
    val recipientNames: List<String>,
)
