// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.json.Json
import java.util.UUID

data class Message(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val sentAt: HelsinkiDateTime,
    val content: String,
)

data class MessageThread(
    val id: UUID,
    val type: MessageType,
    val title: String,
    @Json
    val messages: List<Message>
)

enum class MessageType {
    MESSAGE,
    BULLETIN
}

data class MessageAccount(
    val id: UUID,
    val name: String
)
