// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.message

import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.mapper.PropagateNull
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

data class Message(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val recipients: Set<MessageAccount>,
    val sentAt: HelsinkiDateTime,
    val content: String,
    val readAt: HelsinkiDateTime? = null
)

data class MessageThread(
    val id: UUID,
    val type: MessageType,
    val title: String,
    @Json
    val messages: List<Message>,
)

data class SentMessage(
    val contentId: UUID,
    val content: String,
    val sentAt: HelsinkiDateTime,
    val threadTitle: String,
    val type: MessageType,
    @Json
    val recipients: Set<MessageAccount>,
    val recipientNames: List<String>,
)

enum class MessageType {
    MESSAGE,
    BULLETIN
}

data class MessageReceiversResponse(
    val groupId: GroupId,
    val groupName: String,
    val receivers: List<MessageReceiver>
)

data class MessageReceiver(
    val childId: UUID,
    val childFirstName: String,
    val childLastName: String,
    val childDateOfBirth: LocalDate,
    val receiverPersons: List<MessageReceiverPerson>
)

data class MessageAccount(
    val id: UUID,
    val name: String,
)

data class Group(
    @PropagateNull
    val id: UUID,
    val name: String,
    val unitId: DaycareId,
    val unitName: String,
)

enum class AccountType {
    PERSONAL,
    GROUP
}

data class DetailedMessageAccount(
    val id: UUID,
    val name: String,
    @Nested("group_")
    val daycareGroup: Group?,
    val type: AccountType,
    val unreadCount: Int
)

data class MessageReceiverPerson(
    val accountId: UUID,
    val receiverFirstName: String,
    val receiverLastName: String
)
