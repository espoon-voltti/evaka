// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.shared.MessageAccountId
import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime

data class MobileThreadListItem(
    val id: MessageThreadId,
    val title: String,
    val lastMessagePreview: String,
    val lastMessageAt: HelsinkiDateTime,
    val unreadCount: Int,
    val senderName: String,
)

data class MobileThreadListResponse(
    val data: List<MobileThreadListItem>,
    val hasMore: Boolean,
)

data class MobileThread(
    val id: MessageThreadId,
    val title: String,
    val messages: List<MobileMessage>,
)

data class MobileMessage(
    val id: MessageId,
    val senderName: String,
    val senderAccountId: MessageAccountId,
    val content: String,
    val sentAt: HelsinkiDateTime,
    val readAt: HelsinkiDateTime?,
)

data class MobileMyAccount(
    val accountId: MessageAccountId,
    val messageAttachmentsAllowed: Boolean,
)
